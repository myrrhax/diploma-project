package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class SchemaDifference {
    private List<TableMetadata> upsertedTables = new LinkedList<>();
    private List<ReferenceMetadata> upsertedReferences = new LinkedList<>();
    private List<ColumnMetadata> upsertedColumns = new LinkedList<>();
    private List<IndexMetadata> upsertedIndexes = new LinkedList<>();

    private List<UUID> deletedTables = new LinkedList<>();
    private Map<UUID, UUID> deletedColumns = new HashMap<>();
    private Map<UUID, UUID> deletedIndexes = new HashMap<>();
    private List<ReferenceMetadata.ReferenceKey> deletedReferences = new LinkedList<>();

    public void applyDifference(SchemaDifference difference) {
        upsertedColumns = MetadataTypeUtils.joinUnique(upsertedColumns, difference.getUpsertedColumns());
        upsertedReferences = MetadataTypeUtils.joinUnique(upsertedReferences, difference.getUpsertedReferences());
        upsertedTables = MetadataTypeUtils.joinUnique(upsertedTables, difference.getUpsertedTables());
        upsertedIndexes = MetadataTypeUtils.joinUnique(upsertedIndexes, difference.getUpsertedIndexes());

        deletedTables = MetadataTypeUtils.joinUnique(deletedTables, difference.getDeletedTables());
        deletedReferences = MetadataTypeUtils.joinUnique(deletedReferences, difference.getDeletedReferences());
        deletedColumns = MetadataTypeUtils.joinUnique(deletedColumns, difference.getDeletedColumns());
        deletedIndexes = MetadataTypeUtils.joinUnique(deletedIndexes, difference.getDeletedColumns());
    }

    public void upsertTable(TableMetadata table) {
        this.upsertedTables.add(table);
    }

    public void upsertReference(ReferenceMetadata reference) {
        this.upsertedReferences.add(reference);
    }

    public void upsertColumn(ColumnMetadata column) {
        this.upsertedColumns.add(column);
    }

    public void removeTable(UUID tableId) {
        this.deletedTables.add(tableId);
    }

    public void removeReference(ReferenceMetadata.ReferenceKey key) {
        this.deletedReferences.add(key);
    }

    public void removeColumn(ColumnMetadata column) {
        this.deletedColumns.put(column.getTableId(), column.getId());
    }

    public void upsertIndex(IndexMetadata index) {
        this.upsertedIndexes.add(index);
    }

    public void removeIndex(IndexMetadata index) {
        this.deletedIndexes.put(index.getTableId(), index.getId());
    }
}
