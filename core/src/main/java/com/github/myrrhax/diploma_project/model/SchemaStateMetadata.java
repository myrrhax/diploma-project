package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import com.github.myrrhax.diploma_project.util.ReferenceKeyFromStringDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
@NoArgsConstructor
public class SchemaStateMetadata {
    private UUID schemaId;
    private Map<UUID, TableMetadata> tables = new HashMap<>();
    private Map<ReferenceMetadata.ReferenceKey, ReferenceMetadata> references = new HashMap<>();

    @JsonIgnore
    private Lock lock = new ReentrantLock();
    @JsonIgnore
    private Instant lastModificationTime = Instant.now();
    private AtomicInteger cacheVersion = new AtomicInteger(0);

    public void addTable(TableMetadata tableMetadata) {
        this.tables.putIfAbsent(tableMetadata.getId(), tableMetadata);
        tableMetadata.setSchemaState(this);
    }

    public void addReference(ReferenceMetadata referenceMetadata) {
        referenceMetadata.setSchemaState(this);
        this.references.putIfAbsent(referenceMetadata.getKey(), referenceMetadata);
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

    public SchemaDifference deleteInvalidReferences(TableMetadata changedTable) {
        List<ReferenceMetadata.ReferenceKey> cascadeReferences = references.values().stream()
                .filter(ref -> ref.getKey().getToTableId().equals(changedTable.getId())
                    || ref.getKey().getFromTableId().equals(changedTable.getId()))
                .filter(ref -> !MetadataTypeUtils.isRefValid(this, ref.getKey(), ref.getType()))
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
                .filter(ref -> !MetadataTypeUtils.isRefValid(this, ref.getKey(), ref.getType()))
                .map(ReferenceMetadata::getKey)
                .toList();

        SchemaDifference difference = new SchemaDifference();
        cascadeReferences.forEach(ref -> {
            difference.removeReference(ref);
            removeReference(ref);
        });

        return difference;
    }

//    public SchemaDifference deleteHangingReferences(ColumnMetadata changedColumn, boolean isDeleted) {
//        List<ReferenceMetadata.ReferenceKey> hangingFromRefs = references.values().stream()
//                .filter(ref -> changedColumn.getTableId().equals(ref.getKey().getFromTableId()))
//                .filter(ref -> Arrays.asList(ref.getKey().getFromColumns()).contains(changedColumn.getId()))
//                .filter(ref -> !MetadataTypeUtils.isRefValid(this, ref.getKey(), ref.getType()))
//                .map(ReferenceMetadata::getKey)
//                .toList();
//        List<ReferenceMetadata.ReferenceKey> hangingToRefs = references.values().stream()
//                .filter(ref -> changedColumn.getTableId().equals(ref.getKey().getToTableId()))
//                .filter(ref -> Arrays.asList(ref.getKey().getToColumns()).contains(changedColumn.getId()))
//                .filter(ref -> !MetadataTypeUtils.isRefValid(this, ref.getKey(), ref.getType()))
//                .map(ReferenceMetadata::getKey)
//                .toList();
//        SchemaDifference difference = new SchemaDifference();
//        hangingToRefs.stream()
//                .peek(difference::removeReference)
//                .forEach(this::removeReference);
//        // Колонка удалена
//        if (isDeleted) {
//            hangingFromRefs.stream()
//                    .peek(difference::removeReference)
//                    .forEach(this::removeReference);
//        }
//
//        return difference;
//    }
//
//    public SchemaDifference deleteHangingReferences(TableMetadata changedTable, boolean isDeleted) {
//        List<ReferenceMetadata.ReferenceKey> affectedFromReferences = references.keySet().stream()
//                .filter(key -> key.getFromTableId().equals(changedTable.getId()))
//                .toList();
//        List<ReferenceMetadata.ReferenceKey> affectedToReferences = references.keySet().stream()
//                .filter(key -> key.getToTableId().equals(changedTable.getId()))
//                .toList();
//
//        SchemaDifference difference = new SchemaDifference();
//        // Изменился ключ или индекс, или таблица удалена
//        affectedToReferences.stream()
//                .peek(difference::removeReference)
//                .forEach(this::removeReference);
//        // Таблица удалена
//        if (isDeleted) {
//            affectedFromReferences.stream()
//                    .peek(difference::removeReference)
//                    .forEach(this::removeReference);
//        }
//
//        return difference;
//    }

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
        }
    }

    public boolean containsTable(String tableName) {
        return tables.values().stream()
                .map(TableMetadata::getName)
                .anyMatch(name -> name.equals(tableName));
    }
}
