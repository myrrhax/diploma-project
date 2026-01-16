package com.github.myrrhax.diploma_project.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Token(
        UUID id,
        UUID userId,
        String subject,
        List<String> authorities,
        Instant createdAt,
        Instant expiresAt
) { }