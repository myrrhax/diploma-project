package com.github.myrrhax.diploma_project.model.dto;

import java.util.UUID;

public record UserKickedPayload(UUID userId, UUID schemaId) {
}
