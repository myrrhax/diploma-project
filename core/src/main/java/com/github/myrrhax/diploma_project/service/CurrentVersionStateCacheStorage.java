package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.diploma_project.web.dto.VersionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentVersionStateCacheStorage {
    private final SchemeRepository schemeRepository;
    private final SchemaMapper schemaMapper;
    private JsonSchemaStateMapper schemaStateMapper;
    private final ConcurrentHashMap<Integer, VersionDTO> schemaStateCache = new ConcurrentHashMap<>();
    @Value("${app.cache.schema-ttl}")
    private Duration ttl = Duration.ofMinutes(15);

    @Transactional
    public VersionDTO getSchemaVersion(Integer id) {
        return schemaStateCache.computeIfAbsent(id, (schemeId) -> {
            log.info("Loading schema version from database for scheme {}", id);
            VersionDTO dto = schemeRepository.findByIdLocking(schemeId)
                .map(SchemeEntity::getCurrentVersion)
                .map(schemaMapper::toDto)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));

            dto.currentState().setLastModificationTime(Instant.now());
            return dto;
        });
    }

    @Transactional
    public void flush(int id, boolean force) {
        VersionDTO version = schemaStateCache.get(id);
        SchemaStateMetadata state = version.currentState();
        if (state != null) {
            Lock lock = null;
            try {
                if (state != null) {
                    lock = state.getLock();
                    lock.lock();

                    log.info("Flushing state for scheme {}", id);
                    schemeRepository.findByIdLocking(id)
                            .ifPresentOrElse(it -> {
                                it.getCurrentVersion().setSchema(schemaStateMapper.toJson(state));
                                schemeRepository.flush();
                                state.setLastModificationTime(Instant.now());
                            }, () -> {
                                throw new SchemaNotFoundException(id);
                            });

                    // Удаляем при не принудительном флаше
                    if (!force) {
                        deleteFromCache(id);
                    }
                }
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }

        }
    }

    @Scheduled(cron = "*/15 * * * *")
    public void evictCache() {
        for (int key : schemaStateCache.keySet()) {
            VersionDTO version = schemaStateCache.get(key);
            SchemaStateMetadata state = version.currentState();
            if (state != null) {
                Lock lock = null;
                boolean locked = false;
                try {
                    lock = state.getLock();
                    if (lock.tryLock()) {
                        locked = true;
                        if (state.getLastModificationTime().plus(ttl).isBefore(Instant.now())) {
                            log.info("Evicting schema version from database for scheme {}", key);
                            flush(key, false);
                        }
                    }
                } finally {
                    if (locked) {
                        lock.unlock();
                    }
                }
            }
        }
    }

    public void deleteFromCache(int id) {
        VersionDTO version = schemaStateCache.get(id);
        SchemaStateMetadata state = version.currentState();
        if (state != null) {
            Lock lock = null;
            try {
                lock = state.getLock();
                lock.lock();

                schemaStateCache.remove(id);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }
}
