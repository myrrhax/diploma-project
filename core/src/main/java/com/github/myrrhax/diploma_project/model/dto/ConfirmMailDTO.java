package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmMailDTO(
        @NotNull(message = "{error.validation.null-value}")
        @Pattern(regexp = "^\\d{6}$", message = "{error.validation.invalid-code-format}")
        String confirmationCode
) { }