package com.github.myrrhax.diploma_project.web.dto;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;

public record VersionDTO(
        int schemeId,
        int versionId,
        String tag,
        SchemaStateMetadata currentState,
        Boolean isInitial,
        Boolean isWorkingCopy
) { }