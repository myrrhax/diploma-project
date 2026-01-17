package com.github.myrrhax.diploma_project.model.dto;

import java.util.UUID;

public record SchemeDTO(
        UUID id,
        String name,
        UserDTO creator,
        VersionDTO currentVersion
) {
}
