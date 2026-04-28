package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateScriptDto(
    @NotNull
    @Positive
    Long versionId,
    @NotNull
    ScriptType type
) { }
