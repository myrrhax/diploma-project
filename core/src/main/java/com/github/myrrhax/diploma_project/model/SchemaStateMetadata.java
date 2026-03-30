package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.util.ReferenceKeyFromStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaStateMetadata implements Cloneable {
    private UUID schemaId;
    private Map<UUID, TableMetadata> tables = new HashMap<>();
    private Map<ReferenceMetadata.ReferenceKey, ReferenceMetadata> references = new HashMap<>();

    @JsonIgnore
    private Lock lock = new ReentrantLock();
    @JsonIgnore
    private Instant lastModificationTime = Instant.now();
    private int cacheVersion;

    @JsonIgnore
    private Set<UUID> linkedColumns = new HashSet<>();

    public void addTable(TableMetadata tableMetadata) {
        this.tables.putIfAbsent(tableMetadata.getId(), tableMetadata);
        tableMetadata.setSchemaState(this);
    }

    public void addReference(ReferenceMetadata referenceMetadata) {
        referenceMetadata.setSchemaState(this);
        this.references.putIfAbsent(referenceMetadata.getKey(), referenceMetadata);
        linkReferenceColumn(referenceMetadata);
    }

    public void removeReference(ReferenceMetadata.ReferenceKey key) {
        this.references.remove(key);
    }

    public void removeTable(TableMetadata tableMetadata) {
        this.tables.remove(tableMetadata.getId());
    }

    public Optional<TableMetadata> getTable(UUID id) {
        return Optional.ofNullable(tables.get(id));
    }

    public Optional<TableMetadata> getTable(String name) {
        return tables.values().stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst();
    }

    public Optional<ReferenceMetadata> getReference(ReferenceMetadata.ReferenceKey key) {
        return Optional.ofNullable(this.references.get(key));
    }

    public boolean hasReference(String name) {
        return this.references.values().stream()
                .anyMatch(ref -> Objects.equals(name, ref.getName()));
    }

    public SchemaDifference deleteInvalidReferences(TableMetadata changedTable) {
        List<ReferenceMetadata.ReferenceKey> cascadeReferences = references.values().stream()
                .filter(ref -> ref.getKey().getToTableId().equals(changedTable.getId())
                    || ref.getKey().getFromTableId().equals(changedTable.getId()))
                .filter(ref -> !ref.checkIsRefValid())
                .map(ReferenceMetadata::getKey)
                .toList();

        SchemaDifference difference = new SchemaDifference();
        cascadeReferences.forEach(ref -> {
            difference.removeReference(ref);
            removeReference(ref);
        });

        return difference;
    }

    /**
     * Удаление невалидных связей каскадно по измененной колонке
     * @param changedColumn Измененная колонка
     * @return Разница между схемами
     */
    public SchemaDifference deleteInvalidReferences(ColumnMetadata changedColumn) {
        UUID tableId = changedColumn.getTableId();
        List<ReferenceMetadata.ReferenceKey> cascadeReferences = references.values().stream()
                .filter(ref -> (ref.getKey().getToTableId().equals(tableId)
                        && Arrays.asList(ref.getKey().getToColumns()).contains(changedColumn.getId()))
                    || (ref.getKey().getFromTableId().equals(tableId)
                        && Arrays.asList(ref.getKey().getFromColumns()).contains(changedColumn.getId())))
                .filter(ref -> !ref.checkIsRefValid())
                .map(ReferenceMetadata::getKey)
                .toList();

        SchemaDifference difference = new SchemaDifference();
        cascadeReferences.forEach(ref -> {
            difference.removeReference(ref);
            removeReference(ref);
        });

        return difference;
    }

    public SchemaDifference updateLinkedReferencesNames(TableMetadata changedTable) {
        SchemaDifference diff = new SchemaDifference();

        for (ReferenceMetadata.ReferenceKey key : references.keySet()) {
            if (key.getFromTableId().equals(changedTable.getId())
                    || key.getToTableId().equals(changedTable.getId())) {
                ReferenceMetadata ref = references.get(key);
                if (ref.isNameAutogenerated()) {
                    ref.computeAndSetName();
                    diff.upsertReference(ref);
                }
            }
        }

        return diff;
    }

    public SchemaDifference updateLinkedReferencesNames(ColumnMetadata changedColumn) {
        SchemaDifference diff = new SchemaDifference();

        for (ReferenceMetadata.ReferenceKey key : references.keySet()) {
            if (Arrays.asList(key.getFromColumns()).contains(changedColumn.getId())
                    || Arrays.asList(key.getToColumns()).contains(changedColumn.getId())) {
                ReferenceMetadata ref = references.get(key);
                if (ref.isNameAutogenerated()) {
                    ref.computeAndSetName();
                    diff.upsertReference(ref);
                }
            }
        }

        return diff;
    }

    public boolean checkDuplicate(ReferenceMetadata ref) {
        if (containsReference(ref.getKey())) {
            return true;
        }
        ReferenceMetadata reverseRef = ref.buildReverse();
        if (containsReference(reverseRef.getKey())) {
            ReferenceMetadata foundRef = references.get(reverseRef.getKey());
            if (foundRef.getType() == reverseRef.getType()) {
                return true;
            }
        }

        UUID[] columns = ref.getKey().getFromColumns();
        if (ref.getType() == ReferenceMetadata.ReferenceType.ONE_TO_MANY) {
            columns = ref.getKey().getToColumns();
        }

        for (UUID colId : columns) { // Колонки уже использовались для связей, тогда получится неоднозначность ссылок
            if (linkedColumns.contains(colId)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsReference(ReferenceMetadata.ReferenceKey key) {
        return references.containsKey(key);
    }

    @JsonDeserialize(keyUsing = ReferenceKeyFromStringDeserializer.class)
    public void setReferences(Map<ReferenceMetadata.ReferenceKey, ReferenceMetadata> references) {
        this.references = references;
    }

    public void updateTable(TableMetadata clone) {
        this.tables.put(clone.getId(), clone);
    }

    public void linkChildren() {
        for (TableMetadata table : tables.values()) {
            table.setSchemaState(this);
            table.linkChildren();
        }

        for (ReferenceMetadata reference : references.values()) {
            reference.setSchemaState(this);
            linkReferenceColumn(reference);
        }
    }

    public boolean containsTable(String tableName) {
        return tables.values().stream()
                .map(TableMetadata::getName)
                .anyMatch(name -> name.equals(tableName));
    }

    public SchemaStateMetadata clone() {
        return SchemaStateMetadata.builder()
                .schemaId(this.schemaId)
                .tables(new LinkedHashMap<>(this.tables))
                .references(new LinkedHashMap<>(this.references))
                .linkedColumns(new HashSet<>(this.linkedColumns))
                .build();
    }

    private void linkReferenceColumn(ReferenceMetadata ref) {
        if (ref.getType() == ReferenceMetadata.ReferenceType.MANY_TO_MANY) {
            return;
        }

        UUID[] columns;
        var key = ref.getKey();

        if (ref.getType() != ReferenceMetadata.ReferenceType.ONE_TO_MANY) {
            columns = key.getFromColumns();
        } else {
            columns = key.getToColumns();
        }

        linkedColumns.addAll(Arrays.asList(columns));
    }
}
