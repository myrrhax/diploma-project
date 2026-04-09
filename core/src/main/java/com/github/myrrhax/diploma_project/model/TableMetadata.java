package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.enums.MetadataType;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata implements Cloneable, AbstractMetadata<UUID> {
    @Setter
    @Builder.Default
    private UUID id = UUID.randomUUID();
    @Setter
    private String name;
    private String description;
    private double x;
    private double y;

    @Builder.Default
    private Set<UUID> primaryKeyParts = new HashSet<>();

    @Builder.Default
    private LinkedHashMap<UUID, ColumnMetadata> columns = new LinkedHashMap<>();

    @Builder.Default
    private Map<UUID, IndexMetadata> indexes = new LinkedHashMap<>();

    @Setter
    @JsonIgnore
    private SchemaStateMetadata schemaState;

    @Setter
    private UUID autoIncrementedColumn;

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

    public SchemaDifference removeIndex(IndexMetadata index) {
        SchemaDifference diff = new SchemaDifference();
        indexes.remove(index.getId());
        diff.removeIndex(index);

        List<ReferenceMetadata.ReferenceKey> cascadeReferences = schemaState.getReferences().keySet().stream()
                .filter(key -> key.getFromTableId().equals(this.id)
                            && MetadataTypeUtils.isFullEquals(Arrays.asList(key.getFromColumns()), index.getColumnIds())
                        || key.getToTableId().equals(this.id)
                            && MetadataTypeUtils.isFullEquals(Arrays.asList(key.getToColumns()), index.getColumnIds()))
                .peek(diff::removeReference)
                .toList();
        cascadeReferences.forEach(schemaState::removeReference);

        return diff;
    }

    public boolean containsColumn(String columnName) {
        return this.getColumn(columnName).isPresent();
    }

    public SchemaDifference removeColumn(ColumnMetadata column) {
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
        List<ReferenceMetadata.ReferenceKey> cascadeReferences = schemaState.getReferences().keySet().stream()
                .filter(key ->
                           key.getFromTableId().equals(this.id) && Arrays.stream(key.getFromColumns())
                                    .anyMatch(c -> c.equals(column.getId()))
                               || key.getToTableId().equals(this.id) && Arrays.stream(key.getToColumns())
                                    .anyMatch(c -> c.equals(column.getId()))
                )
                .peek(diff::removeReference)
                .toList();
        cascadeReferences.forEach(schemaState::removeReference);

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
            clone.setX(x);
            clone.setY(y);
            clone.setPrimaryKeyParts(new HashSet<>(primaryKeyParts));
            clone.setColumns(new LinkedHashMap<>(columns));
            clone.setIndexes(new LinkedHashMap<>(indexes));
            
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void addPkPart(UUID columnId) {
        primaryKeyParts.add(columnId);
    }

    public void setX(Double xCoord) {
        if (xCoord != null) {
            this.x = xCoord;
        }
    }

    public void setY(Double yCoord) {
        if (yCoord != null) {
            this.y = yCoord;
        }
    }

    public void setPrimaryKeyParts(Set<UUID> primaryKeyParts) {
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

        for (IndexMetadata index : indexes.values()) {
            index.setSchemaState(this.schemaState);
            index.setTable(this);
        }
    }

    public void removePkPart(UUID id) {
        primaryKeyParts.remove(id);
    }

    public Optional<IndexMetadata> getIndex(@NotNull UUID indexId) {
        return Optional.ofNullable(indexes.get(indexId));
    }

    public Optional<IndexMetadata> getIndex(String name) {
        return indexes.values().stream()
                .filter(idx -> idx.getName().equals(name))
                .findFirst();
    }

    public boolean containsIndex(UUID id) {
        return indexes.containsKey(id);
    }

    public boolean containsIndex(String name) {
        return indexes.values().stream()
                .anyMatch(idx -> Objects.equals(name, idx.getName()));
    }

    public boolean containsColumn(UUID id) {
        return this.columns.containsKey(id);
    }

    @JsonIgnore
    public String getPkContated() {
        return this.getPrimaryKeyParts().stream()
            .map(id -> this.getColumn(id).orElseThrow().getName())
            .collect(Collectors.joining(", "));
    }

    @Override
    @JsonIgnore
    public MetadataType getMetadataType() {
        return MetadataType.TABLE;
    }

    @Override
    public boolean contentEquals(AbstractMetadata<UUID> that) {
        if (that instanceof TableMetadata otherTable) {
            if (MetadataTypeUtils.isFullEquals(this.primaryKeyParts, otherTable.primaryKeyParts)) {
                return true;
            }
            if (this.primaryKeyParts.size() != otherTable.primaryKeyParts.size()) {
                return false;
            }
            var thisPk = this.primaryKeyParts.stream()
                    .map(col -> this.getColumn(col).orElseThrow().getName())
                    .toList();
            var otherPk = otherTable.primaryKeyParts.stream()
                    .map(col -> otherTable.getColumn(col).orElseThrow().getName())
                    .toList();

            return MetadataTypeUtils.isFullEquals(thisPk, otherPk);
        }
        return false;
    }
}
