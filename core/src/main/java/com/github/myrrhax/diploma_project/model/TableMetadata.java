package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.command.SchemaDifference;
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
public class TableMetadata implements Cloneable {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private String name;
    private String description;
    private double xCoord;
    private double yCoord;

    @Builder.Default
    private List<UUID> primaryKeyParts = new ArrayList<>();

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

    public void removeColumn(ColumnMetadata column, SchemaStateMetadata schema, SchemaDifference diff) {
        columns.remove(column.getId());
        diff.removeColumn(column);

        // Удаляем индексы каскадно
        List<IndexMetadata> cascadeIndexes = getIndexes().values().stream()
            .filter(idx -> idx.getColumnIds().contains(column.getId()))
            .peek(diff::removeIndex)
            .toList();
        cascadeIndexes.forEach(this::removeIndex);

        // Каскадно удаляем связи
        List<ReferenceMetadata.ReferenceKey> cascadeReferences = schema.getReferences().keySet().stream()
                .filter(key ->
                           key.getFromTableId().equals(this.id) && Arrays.stream(key.getFromColumns())
                                    .anyMatch(c -> c.equals(column.getId()))
                               || key.getToTableId().equals(this.id) && Arrays.stream(key.getToColumns())
                                    .anyMatch(c -> c.equals(column.getId()))
                )
                .peek(diff::removeReference)
                .toList();
        cascadeReferences.forEach(schema::removeReference);
    }

    public void updateColumn(ColumnMetadata columnMetadata) {
        this.columns.put(columnMetadata.getId(), columnMetadata);
    }

    public void addIndexes(IndexMetadata... indexes) {
        for (IndexMetadata index : indexes) {
            this.indexes.put(index.getId(), index);
        }
    }

    @Override
    public TableMetadata clone() {
        try {
            TableMetadata clone = (TableMetadata) super.clone();
            clone.setId(id);
            clone.setName(name);
            clone.setDescription(description);
            clone.setXCoord(xCoord);
            clone.setYCoord(yCoord);
            clone.setPrimaryKeyParts(new ArrayList<>(primaryKeyParts));
            clone.setColumns(new LinkedHashMap<>(columns));
            clone.setIndexes(new LinkedHashMap<>(indexes));
            
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
