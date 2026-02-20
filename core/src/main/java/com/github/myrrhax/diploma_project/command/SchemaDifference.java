package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class SchemaDifference {
    private final List<TableMetadata> upsertedTables = new LinkedList<>();
    private final List<ReferenceMetadata> upsertedReferences = new LinkedList<>();
    private final List<ColumnMetadata> upsertedColumns = new LinkedList<>();
    private final List<IndexMetadata> upsertedIndexes = new LinkedList<>();

    private final List<UUID> deletedTables = new LinkedList<>();
    private final Map<UUID, UUID> deletedColumns = new HashMap<>();
    private final Map<UUID, UUID> deletedIndexes = new HashMap<>();
    private final List<ReferenceMetadata.ReferenceKey> deletedReferences = new LinkedList<>();

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
