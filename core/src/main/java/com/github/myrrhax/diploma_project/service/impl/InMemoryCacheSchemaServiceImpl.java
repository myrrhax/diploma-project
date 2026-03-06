package com.github.myrrhax.diploma_project.service.impl;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.service.SchemaCacheStorage;
import com.github.myrrhax.diploma_project.service.SchemaService;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class InMemoryCacheSchemaServiceImpl extends SchemaService {
    private final SchemaCacheStorage schemaCacheStorage;

    public InMemoryCacheSchemaServiceImpl(SchemeRepository schemeRepository,
                                          UserRepository userRepository,
                                          AuthorityRepository authorityRepository,
                                          SchemaMapper schemaMapper,
                                          JsonSchemaStateMapper schemaStateMapper,
                                          SchemaCacheStorage schemaCacheStorage) {
        super(schemeRepository, userRepository, authorityRepository, schemaMapper, schemaStateMapper);
        this.schemaCacheStorage = schemaCacheStorage;
    }

    @Override
    public MetadataCommandProcessResult process(MetadataCommand command) {
        log.info("Processing command by InMemoryCachingProcessingService for schema {}", command.getSchemeId());
        SchemeDTO currentSchema = schemaCacheStorage.getSchema(command.getSchemeId());
        var version = currentSchema.currentVersion();
        if (version.getCurrentState() != null) {
            SchemaStateMetadata state = version.getCurrentState();
            try {
                state.getLock().lock();
                var difference = command.execute(state);
                state.setCacheVersion(state.getCacheVersion() + 1);
                state.setLastModificationTime(Instant.now());

                return new MetadataCommandProcessResult(state.getCacheVersion(), difference);
            } finally {
                state.getLock().unlock();
            }
        } else {
            throw new SchemaNotFoundException(command.getSchemeId());
        }
    }

    @Override
    public SchemeDTO getScheme(UUID schemeId) {
        SchemeDTO currentSchema = schemaCacheStorage.getSchema(schemeId);
        if (currentSchema == null) {
            throw new SchemaNotFoundException(schemeId);
        }

        return currentSchema;
    }

    @Override
    @Transactional
    public void deleteScheme(UUID schemeId) {
        if (!this.schemeRepository.existsById(schemeId)) {
            throw new SchemaNotFoundException(schemeId);
        }

        schemaCacheStorage.deleteFromCache(schemeId);
        schemeRepository.deleteById(schemeId);
    }
}
