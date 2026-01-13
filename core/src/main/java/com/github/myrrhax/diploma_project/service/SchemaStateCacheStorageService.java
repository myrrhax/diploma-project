package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
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
public class SchemaStateCacheStorageService {
    private final SchemeRepository schemeRepository;
    private final ConcurrentHashMap<Integer, SchemaStateMetadata> schemaStateCache = new ConcurrentHashMap<>();
    @Value("${app.cache.schema-ttl}")
    private Duration ttl = Duration.ofMinutes(15);

    @Transactional(readOnly = true)
    public void getSchemaState(Integer id) {
        schemaStateCache.computeIfAbsent(id, (schemeId) -> {
           var version = schemeRepository.findByIdLocking(schemeId)
                   .map(SchemeEntity::getCurrentVersion)
                   .orElseThrow(() -> new SchemaNotFoundException(schemeId));

           return version.getSchema();
        });
    }


    @Transactional
    public void flush(int id, boolean force) {
        SchemaStateMetadata state = schemaStateCache.get(id);
        if (state != null) {
            Lock lock = null;
            try {
                if (state != null) {
                    lock = state.getLock();
                    lock.lock();

                    schemeRepository.findByIdLocking(id)
                            .ifPresentOrElse(it -> {
                                it.getCurrentVersion().setSchema(state);
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
            SchemaStateMetadata state = schemaStateCache.get(key);
            if (state != null) {
                Lock lock = null;
                boolean locked = false;
                try {
                    lock = state.getLock();
                    if (lock.tryLock()) {
                        locked = true;
                        if (state.getLastModificationTime().plus(ttl).isBefore(Instant.now())) {
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
        SchemaStateMetadata state = schemaStateCache.get(id);
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
