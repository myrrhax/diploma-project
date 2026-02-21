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
    private Map<UUID, List<ColumnMetadata>> upsertedColumns = new HashMap<>();
    private Map<UUID, List<IndexMetadata>> upsertedIndexes = new HashMap<>();

    private List<UUID> deletedTables = new LinkedList<>();
    private Map<UUID, List<UUID>> deletedColumns = new HashMap<>();
    private Map<UUID, List<UUID>> deletedIndexes = new HashMap<>();
    private List<ReferenceMetadata.ReferenceKey> deletedReferences = new LinkedList<>();

    public void applyDifference(SchemaDifference difference) {
        upsertedColumns = MetadataTypeUtils.joinUniqueFlat(upsertedColumns, difference.getUpsertedColumns());
        upsertedReferences = MetadataTypeUtils.joinUnique(upsertedReferences, difference.getUpsertedReferences());
        upsertedTables = MetadataTypeUtils.joinUnique(upsertedTables, difference.getUpsertedTables());
        upsertedIndexes = MetadataTypeUtils.joinUniqueFlat(upsertedIndexes, difference.getUpsertedIndexes());

        deletedTables = MetadataTypeUtils.joinUnique(deletedTables, difference.getDeletedTables());
        deletedReferences = MetadataTypeUtils.joinUnique(deletedReferences, difference.getDeletedReferences());
        deletedColumns = MetadataTypeUtils.joinUniqueFlat(deletedColumns, difference.getDeletedColumns());
        deletedIndexes = MetadataTypeUtils.joinUniqueFlat(deletedIndexes, difference.getDeletedIndexes());
    }

    public void upsertTable(TableMetadata table) {
        this.upsertedTables.add(table);
    }

    public void upsertReference(ReferenceMetadata reference) {
        this.upsertedReferences.add(reference);
    }

    public void upsertColumn(ColumnMetadata column) {
        if (!this.upsertedColumns.containsKey(column.getTableId())) {
            this.upsertedColumns.put(column.getTableId(), new LinkedList<>());
        }
        this.upsertedColumns.get(column.getTableId()).add(column);
    }

    public void removeTable(UUID tableId) {
        this.deletedTables.add(tableId);
    }

    public void removeReference(ReferenceMetadata.ReferenceKey key) {
        this.deletedReferences.add(key);
    }

    public void removeColumn(ColumnMetadata column) {
        if (!this.deletedColumns.containsKey(column.getTableId())) {
            this.deletedColumns.put(column.getTableId(), new LinkedList<>());
        }
        this.deletedColumns.get(column.getTableId()).add(column.getId());
    }

    public void upsertIndex(IndexMetadata index) {
        if (!this.upsertedIndexes.containsKey(index.getTableId())) {
            this.upsertedIndexes.put(index.getTableId(), new LinkedList<>());
        }
        this.upsertedIndexes.get(index.getTableId()).add(index);
    }

    public void removeIndex(IndexMetadata index) {
        if (!this.deletedIndexes.containsKey(index.getTableId())) {
            this.deletedIndexes.put(index.getTableId(), new LinkedList<>());
        }
        this.deletedIndexes.get(index.getTableId()).add(index.getId());
    }
}
