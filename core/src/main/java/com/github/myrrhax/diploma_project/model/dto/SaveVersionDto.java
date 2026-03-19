package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveVersionDto(
        @NotBlank(message = "{error.validation.null-value}")
        String tag
) { }
