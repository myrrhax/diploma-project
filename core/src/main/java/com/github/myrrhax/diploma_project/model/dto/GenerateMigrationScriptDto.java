package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateMigrationScriptDto(
        @NotNull
        @Positive
        Long versionId,
        @NotNull
        @Positive
        Long fromVersionId,
        @NotNull
        ScriptType type
) {
}
