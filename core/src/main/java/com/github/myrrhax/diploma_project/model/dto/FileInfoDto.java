package com.github.myrrhax.diploma_project.model.dto;

import org.springframework.core.io.InputStreamResource;

public record FileInfoDto(
        InputStreamResource file,
        String preferredName,
        String mediaType
) { }
