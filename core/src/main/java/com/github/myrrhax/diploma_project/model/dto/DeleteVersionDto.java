package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.NotNull;

public record DeleteVersionDto(
        @NotNull(message = "{error.validation.null-value}")
        Long versionId
) { }
