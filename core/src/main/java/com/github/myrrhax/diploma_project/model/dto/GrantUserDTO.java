package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GrantUserDTO(
        @NotNull(message = "{error.validation.null-value}")
        UUID userId,
        @NotNull(message = "{error.validation.null-value}")
        UUID schemeId,
        @NotNull(message = "{error.validation.null-value}")
        @NotEmpty(message = "{error.validation.null-value}")
        List<AuthorityType> authorities
) { }