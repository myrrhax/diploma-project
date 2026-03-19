package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeHeadVersionDto(
        @NotNull(message = "{error.validation.null-value}")
        Long currentVersionId,
        @NotNull(message = "{error.validation.null-value}")
        Long toVersionId
) { }
