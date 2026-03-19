package com.github.myrrhax.diploma_project.model.dto;

import java.util.UUID;

public record ConnectionChangedPayload(UUID schemaId, UserDTO user, ConnectionChangeType type) {
    public enum ConnectionChangeType {
        CONNECTED,
        DISCONNECTED
    }
}
