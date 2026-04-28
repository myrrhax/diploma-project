package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record AuthRequestDTO(
        @NotNull(message = "{error.validation.null-value}")
        @Email(message = "{error.validation.email}")
        String email,

        @NotNull(message = "{error.validation.null-value}")
        @Length(min = 6, message = "{error.validation.min-password-length}")
        String password
) { }