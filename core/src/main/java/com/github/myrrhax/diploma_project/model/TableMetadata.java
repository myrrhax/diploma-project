package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private double xCoord;
    private double yCoord;

    public TableMetadata(String name, double xCoord, double yCoord) {
        this.name = name;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    @Builder.Default
    private List<ColumnMetadata> primaryKeyParts = new ArrayList<>();

    @Builder.Default
    private LinkedHashMap<UUID, ColumnMetadata> columns = new LinkedHashMap<>();

    @Builder.Default
    private Map<UUID, IndexMetadata> indexes = new HashMap<>();

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

    public void removeIndex(IndexMetadata index) {
        indexes.remove(index.getId());
    }

    public void removeColumn(ColumnMetadata column, SchemaStateMetadata schema) {
        columns.remove(column.getId());
        getIndexes().entrySet().removeIf(idx ->
                idx.getValue().getColumnIds().contains(column.getId()));
        getPrimaryKeyParts().removeIf(pk ->
                pk.getId().equals(column.getId()));

        schema.getReferences().entrySet().removeIf(ref -> {
            var key = ref.getKey();
            return (key.getFromTableId().equals(getId())
                    && Arrays.stream(key.getFromColumns())
                        .anyMatch(fc -> fc.equals(column.getId())))
                    || (key.getToTableId().equals(getId())
                        && Arrays.stream(key.getToColumns()).anyMatch(fc -> fc.equals(column.getId())));
        });
    }

    public void addIndexes(IndexMetadata... indexes) {
        for (IndexMetadata index : indexes) {
            this.indexes.put(index.getId(), index);
        }
    }
}
