package com.github.myrrhax.diploma_project.web.dto;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;

import java.util.UUID;

public record VersionDTO(
        UUID schemeId,
        long versionId,
        String tag,
        SchemaStateMetadata currentState,
        boolean isInitial,
        boolean isWorkingCopy
) { }