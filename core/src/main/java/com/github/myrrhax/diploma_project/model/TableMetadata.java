package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private String name;
    private String description;

    @Builder.Default
    private List<ColumnMetadata> primaryKeyParts = new ArrayList<>();

    @Builder.Default
    private LinkedHashMap<UUID, ColumnMetadata> columns = new LinkedHashMap<>();

    @Builder.Default
    private List<IndexMetadata> indexes = new ArrayList<>();

    public Optional<ColumnMetadata> getColumn(UUID id) {
        return Optional.ofNullable(columns.get(id));
    }

    public Optional<ColumnMetadata> getColumn(String name) {
        return columns.values().stream()
                .filter(col -> col.getName().equals(name))
                .findFirst();
    }

    public void addColumn(ColumnMetadata columnMetadata) {
        columns.put(columnMetadata.getId(), columnMetadata);
    }

    public void addColumns(ColumnMetadata... columns) {
        for (ColumnMetadata column : columns) {
            this.columns.put(column.getId(), column);
        }
    }

    public void addIndexes(IndexMetadata... indexes) {
        this.indexes.addAll(Arrays.asList(indexes));
    }
}
