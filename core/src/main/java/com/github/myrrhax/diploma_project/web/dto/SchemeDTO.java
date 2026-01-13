package com.github.myrrhax.diploma_project.web.dto;

public record SchemeDTO(
        Integer id,
        String name,
        UserDTO creator,
        VersionDTO currentVersion
) {
}
