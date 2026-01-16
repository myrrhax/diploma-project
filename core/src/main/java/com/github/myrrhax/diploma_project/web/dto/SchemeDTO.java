package com.github.myrrhax.diploma_project.web.dto;

import java.util.UUID;

public record SchemeDTO(
        UUID id,
        String name,
        UserDTO creator,
        VersionDTO currentVersion
) {
}
