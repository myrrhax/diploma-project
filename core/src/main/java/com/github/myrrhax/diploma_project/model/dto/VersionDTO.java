package com.github.myrrhax.diploma_project.model.dto;

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