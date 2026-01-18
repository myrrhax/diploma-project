package com.github.myrrhax.diploma_project.model.dto;

import java.time.Instant;

public record AuthResultDTO(
        String accessToken,
        Instant expiresAt,
        UserDTO user
) { }