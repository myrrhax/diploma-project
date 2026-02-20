package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata implements Cloneable {
    @Setter
    @Builder.Default
    private UUID id = UUID.randomUUID();
    @Setter
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

    @Setter
    @JsonIgnore
    private SchemaStateMetadata schemaState;

    public Optional<ColumnMetadata> getColumn(UUID id) {
        return Optional.ofNullable(columns.get(id));
    }

    public Optional<ColumnMetadata> getColumn(String name) {
        return columns.values().stream()
                .filter(col -> col.getName().equals(name))
                .findFirst();
    }

    public void addColumn(ColumnMetadata columnMetadata) {
        columnMetadata.setSchema(schemaState);
        columnMetadata.setTable(this);
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

    public boolean containsColumn(String columnName) {
        return this.getColumn(columnName).isPresent();
    }

    public SchemaDifference removeColumn(ColumnMetadata column, SchemaStateMetadata schema) {
        SchemaDifference diff = new SchemaDifference();
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

        return diff;
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
            clone.setSchemaState(schemaState);
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

    public void setXCoord(Double xCoord) {
        if (xCoord != null) {
            this.xCoord = xCoord;
        }
    }

    public void setYCoord(Double yCoord) {
        if (yCoord != null) {
            this.yCoord = yCoord;
        }
    }

    public void setPrimaryKeyParts(List<UUID> primaryKeyParts) {
        if (primaryKeyParts != null) {
            this.primaryKeyParts = primaryKeyParts;
        }
    }

    public void setColumns(LinkedHashMap<UUID, ColumnMetadata> columns) {
        if (columns != null) {
            this.columns = columns;
        }
    }

    public void setIndexes(Map<UUID, IndexMetadata> indexes) {
        if (indexes != null) {
            this.indexes = indexes;
        }
    }

    public void setDescription(String description) {
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    public void linkChildren() {
        for (ColumnMetadata column : columns.values()) {
            column.setSchema(schemaState);
            column.setTable(this);
        }
    }
}
