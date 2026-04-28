package com.github.myrrhax.diploma_project.model.dto;

import java.util.UUID;

public record UserDeletePayload(UUID userId, UUID schemaId) {
}
