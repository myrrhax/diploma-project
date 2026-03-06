package com.github.myrrhax.diploma_project.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.util.ViewMarkers;

import java.util.UUID;

public record VersionDTO(
        @JsonView(ViewMarkers.Basic.class)
        UUID schemeId,
        @JsonView(ViewMarkers.Basic.class)
        long versionId,
        @JsonView(ViewMarkers.Basic.class)
        String tag,
        @JsonView(ViewMarkers.Stateful.class)
        SchemaStateMetadata currentState,
        @JsonView(ViewMarkers.Basic.class)
        boolean isInitial,
        @JsonView(ViewMarkers.Basic.class)
        boolean isWorkingCopy,
        @JsonView(ViewMarkers.Basic.class)
        String hashSum
) { }