package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;

import java.util.UUID;

public record SchemeDTO(
        UUID id,
        String name,
        UserDTO creator,
        VersionDTO currentVersion
) {
    public SchemaStateMetadata currentState() {
        return currentVersion.currentState();
    }
}
