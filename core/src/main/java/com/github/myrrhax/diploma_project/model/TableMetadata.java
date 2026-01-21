package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    private UUID id;
    private String name;
    private String description;

    @Builder.Default
    private List<ColumnMetadata> primaryKeyParts = new ArrayList<>();

    @Builder.Default
    private LinkedHashMap<UUID, ColumnMetadata> columns = new LinkedHashMap<>();

    @Builder.Default
    private List<IndexMetadata> indexes = new ArrayList<>();
}
