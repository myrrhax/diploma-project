package com.github.myrrhax.diploma_project.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KickUserDto(
        @NotNull(message = "{error.validation.null-value}")
        UUID kickedUserID
) { }
