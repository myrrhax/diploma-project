package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class MigrationProcessor {
    protected SchemaDifference calculateDifference(String name, VersionDTO from, VersionDTO to) {
        SchemaDifference difference = new SchemaDifference();
        if (from == to || Objects.equals(from.getHashSum(), to.getHashSum())) {
            return difference;
        }
        SchemaStateMetadata initialState = from.getCurrentState();
        SchemaStateMetadata finalState = to.getCurrentState();

        Collection<TableMetadata> initialTables = initialState.getTables().values();
        Collection<TableMetadata> finalTables = finalState.getTables().values();
        Set<UUID> processedTables = new HashSet<>();

        // New Tables
        for (TableMetadata table : finalTables) {
            if (processedTables.contains(table.getId())) {
                continue;
            }

            if (initialState.containsTable(table.getId())
                || initialState.containsTable(table.getName())) {
                // Same table, check columns
                Set<String> processedColumns = new HashSet<>();

                TableMetadata otherTable = initialState.getTable(table.getId())
                        .orElse(initialState.getTable(table.getName()).get());

                processedTables.add(otherTable.getId());

                for (ColumnMetadata column : table.getColumns().values()) {
                    if (otherTable.containsColumn(column.getId())
                        || otherTable.containsColumn(column.getName())) {
                        // Same column
                    }
                }

            } else {
                // New Table
            }

            processedTables.add(table.getId());
        }

        return difference;
    }

    private void handleAddTable(StringBuilder scriptBuilder, StringBuilder indexBuilder, TableMetadata table) {
        ScriptFabric fabric = getFabric();
        fabric.appendTableDefinition(scriptBuilder, table);

        for (ColumnMetadata column: table.getColumns().values()) {
            fabric.appendColumnDefinition(scriptBuilder, column);
            scriptBuilder.append("\n");
        }

        for (IndexMetadata idx : table.getIndexes().values()) {
            fabric.appendIndexDefinition(indexBuilder, idx);
            indexBuilder.append("\n");
        }
        onEndTableDefinition(scriptBuilder, table);
        fabric.appendEndTablePadding(scriptBuilder);
    }

    protected record GenericSchemaChanges<T>(
        T from,
        T to,
        DifferenceType differenceType
    ) {

    }

    protected enum DifferenceType {
        ADD,
        DROP,
        RENAME,
        UPDATE
    }


    protected abstract ScriptFabric getFabric();
    protected abstract void onEndTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
}
