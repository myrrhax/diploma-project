package com.github.myrrhax.diploma_project.service.impl;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.mapper.VersionMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.diploma_project.service.SchemaService;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Primary
public class SchemaServiceImpl extends SchemaService {
    private final VersionRepository versionRepository;
    private final VersionMapper versionMapper;

    public SchemaServiceImpl(SchemeRepository schemeRepository,
                             UserRepository userRepository,
                             AuthorityRepository authorityRepository,
                             SchemaMapper schemaMapper,
                             JsonSchemaStateMapper schemaStateMapper,
                             VersionRepository versionRepository,
                             VersionMapper versionMapper) {
        super(schemeRepository, userRepository, authorityRepository, schemaMapper, schemaStateMapper);
        this.versionRepository = versionRepository;
        this.versionMapper = versionMapper;
    }


    @Override
    @Transactional
    public MetadataCommandProcessResult process(MetadataCommand command) {
        // ToDo сереализовать команду
        log.info("Processing command for schema {}", command.getSchemeId());
        var schema = schemeRepository.findByIdLocking(command.getSchemeId())
                .orElseThrow(() -> new  SchemaNotFoundException(command.getSchemeId()));
        VersionEntity currentVersion = schema.getCurrentVersion();
        SchemaStateMetadata state;
        try {
            state = schemaStateMapper.toMetadata(currentVersion.getSchema());
        } catch (Exception e) {
            log.error("Failed to parse schema state for schema {}", command.getSchemeId(), e);
            throw new ApplicationException("Failed to parse schema", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        SchemaDifference diff = command.execute(state);
        state.setCacheVersion(state.getCacheVersion() + 1);
        currentVersion.setSchema(schemaStateMapper.toJson(state));

        return new MetadataCommandProcessResult(state.getCacheVersion(), diff);
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeDTO getScheme(UUID schemeId) {
        return schemeRepository.findById(schemeId)
                    .map(schemaMapper::toDto)
                    .orElseThrow(() -> new SchemaNotFoundException(schemeId));
    }

    @Override
    @Transactional
    public void deleteScheme(UUID schemeId) {
        if (!schemeRepository.existsById(schemeId)) {
            throw new SchemaNotFoundException(schemeId);
        }
        schemeRepository.deleteById(schemeId);
    }
}
