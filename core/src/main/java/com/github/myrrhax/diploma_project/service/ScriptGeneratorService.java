package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.ScriptMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.ScriptRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.diploma_project.script.FullScriptProcessor;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScriptGeneratorService {
    private final ScriptRepository scriptRepository;
    private final VersionRepository versionRepository;
    private final ScriptMapper scriptMapper;
    private final JsonSchemaStateMapper stateMapper;
    private final List<FullScriptProcessor> scriptProcessors;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ScriptDto> getScriptsForVersion(Long versionId) {
        return scriptRepository.findByVersionId(versionId).stream()
                .map(scriptMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScriptDto getScriptById(UUID scriptId) {
        return scriptRepository.findById(scriptId)
                .map(scriptMapper::toDto)
                .orElseThrow(() -> new ApplicationException("error.script.notfound", HttpStatus.NOT_FOUND));
    }

    public ScriptDto generateScript(Long versionId, ScriptType type) {
        log.info("Processing generate script for version {} with type {}", versionId, type);
        if (scriptRepository.existsByTypeAndGeneratedTypeAndVersionId(type, GeneratedScriptType.FULL, versionId)) {
            throw new ApplicationException("error.script.already-exists");
        }

        FullScriptProcessor processor = getScriptProcessor(type);
        VersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ApplicationException("error.version.notfound"));
        if (version.getIsWorkingCopy()) {
            throw new ApplicationException("error.version.generating-on-working-copy");
        }
        try {
            SchemaStateMetadata schema = stateMapper.toMetadata(version.getSchema());
            String script = processor.process(version.getTag(), schema);
            byte[] scriptBytes = script.getBytes(StandardCharsets.UTF_8);
            UUID fileId = fileStorageService.saveFile(version.getTag(),
                    scriptBytes,
                    getMediaTypeForScript(type),
                    schema.getSchemaId());
            DDLScriptEntity ddl = new DDLScriptEntity(version, fileId, type);
            scriptRepository.save(ddl);

            return scriptMapper.toDto(ddl);
        } catch (Exception e) {
            log.error("Failed to convert schema from JSON", e);
            throw new ApplicationException("Failed to convert schema from JSON", e);
        }
    }

    private String getMediaTypeForScript(ScriptType type) {
        return switch (type) {
            case POSTGRES, MYSQL -> "application/sql";
            case LIQUIBASE -> "application/yaml";
        };
    }

    private FullScriptProcessor getScriptProcessor(ScriptType type) {
        return scriptProcessors.stream()
                .filter(processor -> processor.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find script processor for type " + type));
    }
}
