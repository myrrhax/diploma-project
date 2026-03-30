package com.github.myrrhax.diploma_project.script.impl;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.ScriptProcessor;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SqlScriptProcessor extends ScriptProcessor {
    @Override
    protected String generateContent(SchemaStateMetadata metadata,
                                     List<TableMetadata> tablesToProcess,
                                     List<ReferenceMetadata> referencesToProcess) {
        StringBuilder scriptBuilder = new StringBuilder();
        StringBuilder indexesBuilder = new StringBuilder();

        AbstractSqlScriptFabric scriptFabric = getScriptFabric();
        for (TableMetadata tableMetadata : tablesToProcess) {
            scriptBuilder.append(scriptFabric.getTableDefinition(tableMetadata));
            List<ColumnMetadata> columns = tableMetadata.getColumns().values().stream().toList();

            Set<UUID> primaryKeyParts = tableMetadata.getPrimaryKeyParts();
            if (primaryKeyParts.isEmpty()) {
                throw new ApplicationException("Table must contain primary key", HttpStatus.BAD_REQUEST);
            }

            buildColumnsPart(columns, scriptBuilder);
            buildPrimaryKeyConstraint(tableMetadata, primaryKeyParts.stream().toList(), scriptBuilder);

            scriptBuilder.append(");\n");

            Collection<IndexMetadata> indexes = tableMetadata.getIndexes().values();
            buildIndexPart(tableMetadata, indexes, indexesBuilder);
        }

        Map<UUID, TableMetadata> tableMap = tablesToProcess.stream()
                .collect(Collectors.toMap(TableMetadata::getId, Function.identity()));

        buildReferencePart(tableMap, referencesToProcess, scriptBuilder);
        scriptBuilder.append(indexesBuilder);

        return scriptBuilder.toString();
    }
    protected abstract AbstractSqlScriptFabric getScriptFabric();

    private void buildPrimaryKeyConstraint(TableMetadata table, List<UUID> primaryKeyParts, StringBuilder scriptBuilder) {
        scriptBuilder.append("\tPRIMARY KEY (");
        for (int i = 0; i < primaryKeyParts.size(); i++) {
            ColumnMetadata column = table.getColumn(primaryKeyParts.get(i)).orElseThrow();
            scriptBuilder.append(column.getName());
            if (i < primaryKeyParts.size() - 1) {
                scriptBuilder.append(", ");
            }
        }
        scriptBuilder.append(")\n");
    }

    private void buildReferencePart(Map<UUID, TableMetadata> tables,
                                    List<ReferenceMetadata> refs,
                                    StringBuilder sqlBuilder) {
        for (ReferenceMetadata ref : refs) {
            sqlBuilder.append(getScriptFabric().getReferenceDefinition(tables, ref));
            sqlBuilder.append("\n");
        }
    }

    private void buildIndexPart(TableMetadata tableMetadata, Collection<IndexMetadata> indexes, StringBuilder indexBuilder) {
        for (IndexMetadata indexMetadata : indexes) {
            indexBuilder.append(getScriptFabric().getIndexDefinition(tableMetadata, indexMetadata));
            indexBuilder.append("\n");
        }
    }

    private void buildColumnsPart(List<ColumnMetadata> columns, StringBuilder scriptBuilder) {
        for (ColumnMetadata columnMeta : columns) {
            String definition = getScriptFabric().getColumnDefinition(columnMeta);
            scriptBuilder.append('\t');
            scriptBuilder.append(definition);
            scriptBuilder.append(',');
            scriptBuilder.append('\n');
        }
    }
}
