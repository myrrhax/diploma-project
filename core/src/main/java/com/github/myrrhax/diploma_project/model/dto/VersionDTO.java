package com.github.myrrhax.diploma_project.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VersionDTO implements Serializable {
        @JsonView(ViewMarkers.Basic.class)
        UUID schemeId;
        @JsonView(ViewMarkers.Basic.class)
        long versionId;
        @JsonView(ViewMarkers.Basic.class)
        String tag;
        @JsonView(ViewMarkers.Stateful.class)
        transient SchemaStateMetadata currentState;
        @JsonView(ViewMarkers.Basic.class)
        boolean isInitial;
        @JsonView(ViewMarkers.Basic.class)
        boolean isWorkingCopy;
        @JsonView(ViewMarkers.Basic.class)
        String hashSum;
        @JsonView(ViewMarkers.Basic.class)
        LocalDateTime versionedAt;
        @JsonView(ViewMarkers.Basic.class)
        long parentId;
}