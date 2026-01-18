package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.Pattern;

public record ConfirmMailDTO(
        @Pattern(regexp = "^\\d{6}$")
        String confirmationCode
) { }