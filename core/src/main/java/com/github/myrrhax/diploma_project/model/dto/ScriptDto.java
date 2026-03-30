package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;

import java.util.UUID;

public record ScriptDto(
        UUID id,
        VersionDTO version,
        String script,
        ScriptType type,
        GeneratedScriptType generatedType,
        VersionDTO fromVersion
) { }