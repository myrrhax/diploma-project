package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.ScriptMapper;
import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.repository.ScriptRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScriptGeneratorService {
    private final ScriptRepository scriptRepository;
    private final VersionRepository versionRepository;
    private final ScriptMapper scriptMapper;

    public List<ScriptDto> getScriptsForVersion(Long versionId) {
        return scriptRepository.findByVersionId(versionId).stream()
                .map(scriptMapper::toDto)
                .toList();
    }

    public ScriptDto generateScript(Long versionId, GeneratedScriptType generatedType) {
        return null;
    }
}
