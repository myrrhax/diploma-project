package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentVersionStateCacheStorage {
    private final SchemeRepository schemeRepository;
    private final SchemaMapper schemaMapper;
    private final JsonSchemaStateMapper schemaStateMapper;
    private final ConcurrentHashMap<UUID, VersionDTO> schemaStateCache = new ConcurrentHashMap<>();
    @Value("${app.cache.schema-ttl}")
    private Duration ttl = Duration.ofMinutes(15);

    @Transactional
    public VersionDTO getSchemaVersion(UUID id) {
        return schemaStateCache.computeIfAbsent(id, (schemeId) -> {
            log.info("Loading schema version from database for scheme {}", id);
            VersionDTO dto = schemeRepository.findByIdLocking(schemeId)
                .map(SchemeEntity::getCurrentVersion)
                .map(it -> schemaMapper.toVersionDTO(it, schemaStateMapper.schemaStateMetadata(it.getSchema())))
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));

            dto.currentState().setLastModificationTime(Instant.now());
            return dto;
        });
    }

    @Transactional
    public void flush(UUID id, boolean force) {
        VersionDTO version = schemaStateCache.get(id);
        SchemaStateMetadata state = version.currentState();
        if (state != null) {
            Lock lock = null;
            try {
                lock = state.getLock();
                lock.lock();
                if (schemaStateCache.containsKey(id)) {
                    log.info("Flushing state for scheme {}", id);
                    schemeRepository.findByIdLocking(id)
                            .ifPresentOrElse(it -> {
                                it.getCurrentVersion().setSchema(schemaStateMapper.toJson(state));
                                schemeRepository.flush();
                                state.setLastModificationTime(Instant.now());
                            }, () -> {
                                throw new SchemaNotFoundException(id);
                            });
                    schemeRepository.flush();

                    // Удаляем при не принудительном флаше
                    if (!force) {
                        deleteFromCache(id);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to flush state for scheme {}", id, e);

                throw new RuntimeException(e);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }

        }
    }

    @Scheduled(cron = "*/15 * * * *")
    public void evictCache() {
        for (UUID key : schemaStateCache.keySet()) {
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
                } catch (Exception e) {
                    log.error("Unable to evict schema version from database for scheme {}", key, e);

                    throw new RuntimeException(e);
                } finally {
                    if (locked) {
                        lock.unlock();
                    }
                }
            }
        }
    }

    public void deleteFromCache(UUID id) {
        VersionDTO version = schemaStateCache.get(id);
        SchemaStateMetadata state = version.currentState();
        if (state != null) {
            Lock lock = null;
            try {
                lock = state.getLock();
                lock.lock();

                log.info("Deleting scheme info from cache for scheme {}", id);
                schemaStateCache.remove(id);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }
}
