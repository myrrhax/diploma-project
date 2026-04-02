package com.github.myrrhax.diploma_project.script.impl.liquibase;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.script.ScriptProcessor;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class LiquibaseYamlScriptProcessor extends ScriptProcessor {
    private static final int TABLE_PADDING_LEVEL = 3;
    private static final int INDEX_PADDING_LEVEL = 3;
    private static final int COLUMN_PADDING_LEVEL = 5;
    private static final int INDEX_COLUMN_PADDING_LEVEL = 5;

    @Override
    public boolean supports(ScriptType type) {
        return ScriptType.LIQUIBASE.equals(type);
    }

    @Override
    protected String generateContent(SchemaStateMetadata metadata, List<TableMetadata> tablesToProcess, List<ReferenceMetadata> referencesToProcess) {
        StringBuilder scriptBuilder = new StringBuilder();
        StringBuilder indexesBuilder = new StringBuilder();
        addHeader(scriptBuilder);

        for (TableMetadata table : tablesToProcess) {
            appendLine(scriptBuilder, "- createTable:", TABLE_PADDING_LEVEL);
            appendLine(scriptBuilder, "tableName: ", table.getName(), TABLE_PADDING_LEVEL + 1);
            appendLine(scriptBuilder, "columns:", TABLE_PADDING_LEVEL + 1);

            for (ColumnMetadata column: table.getColumns().values()) {
                appendLine(scriptBuilder, "- column:", COLUMN_PADDING_LEVEL);
                appendLine(scriptBuilder, "name: ", column.getName(), COLUMN_PADDING_LEVEL + 1);
                String type = column.getColumnType().name();
                if (MetadataTypeUtils.lengthLimitedTypes.contains(column.getColumnType())
                    && column.getLength() != null) {
                    type = getLimitedType(column);
                } else if (column.getColumnType() == ColumnMetadata.ColumnType.DECIMAL) {
                    type = getDecimalDefinition(column);
                }

                appendLine(scriptBuilder, "type: ", type, COLUMN_PADDING_LEVEL + 1);
                if (column.getDefaultValue() != null) {
                    appendLine(scriptBuilder, "defaultValue: ", column.getDefaultValue(), COLUMN_PADDING_LEVEL + 1);
                }

                if (MetadataTypeUtils.isValidAutoincrement(column)
                    && Objects.equals(column.getAutoIncrement(), Boolean.TRUE)) {
                    appendLine(scriptBuilder, "autoIncrement: ", "true", COLUMN_PADDING_LEVEL + 1);
                }

                if (column.isPkPart()
                        || column.getMin() != null
                        || column.getMax() != null
                        || !column.getConstraints().isEmpty()) {
                    appendLine(scriptBuilder, "constraints:", COLUMN_PADDING_LEVEL + 1);
                    if (column.isPkPart()) {
                        appendLine(scriptBuilder, "primaryKey: ", "true", COLUMN_PADDING_LEVEL + 2);
                    }
                    if (!column.getConstraints().isEmpty()) {
                        addConstraintsDefinition(column, scriptBuilder);
                    }
                    if (column.getMin() != null || column.getMax() != null) {
                        addMinMaxDefinition(column, scriptBuilder);
                    }
                }
            }

            for (IndexMetadata index : table.getIndexes().values()) {
                buildIndex(indexesBuilder, index, metadata);
            }
        }
        buildReferences(tablesToProcess, referencesToProcess, scriptBuilder);
        scriptBuilder.append(indexesBuilder);

        return scriptBuilder.toString();
    }

    private void buildIndex(StringBuilder indexesBuilder, IndexMetadata index, SchemaStateMetadata metadata) {
        appendLine(indexesBuilder, "- createIndex:", INDEX_PADDING_LEVEL);
        if (index.getIndexName() == null) {
            index.computeAndSetName();
        }
        TableMetadata table = index.getTable();
        String[] columnNames = index.getColumnIds().stream()
                        .map(col -> table.getColumn(col).orElseThrow(
                                () -> new ApplicationException("error.column.notfound")
                            ).getName())
                        .toArray(String[]::new);


        appendLine(indexesBuilder, "indexName: ", index.getIndexName(), INDEX_PADDING_LEVEL + 1);
        appendLine(indexesBuilder, "tableName: ", table.getName(), INDEX_PADDING_LEVEL + 1);
        appendLine(indexesBuilder, "columns:", INDEX_PADDING_LEVEL + 1);
        for (String colName: columnNames) {
            appendLine(indexesBuilder, "- column:", INDEX_COLUMN_PADDING_LEVEL);
            appendLine(indexesBuilder, "name: ", colName, INDEX_COLUMN_PADDING_LEVEL + 1);
        }
    }

    private void buildReferences(List<TableMetadata> tablesToProcess, List<ReferenceMetadata> referencesToProcess, StringBuilder scriptBuilder) {
        for (ReferenceMetadata ref : referencesToProcess) {
            if (ref.getName() == null) {
                ref.computeAndSetName();
            }
            TableMetadata baseTable = tablesToProcess.stream()
                    .filter(table -> table.getId().equals(ref.getKey().getFromTableId()))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException("error.table.notfound"));

            TableMetadata referencedTable = tablesToProcess.stream()
                    .filter(table -> table.getId().equals(ref.getKey().getToTableId()))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException("error.table.notfound"));

            String[] baseColumnNames = Arrays.stream(ref.getKey().getFromColumns())
                    .map(colId -> baseTable.getColumn(colId)
                            .map(ColumnMetadata::getName)
                            .orElseThrow(() -> new ApplicationException("error.column.notfound")))
                    .toArray(String[]::new);
            String[] referencedColumnNames = Arrays.stream(ref.getKey().getToColumns())
                    .map(colId -> referencedTable.getColumn(colId)
                            .map(ColumnMetadata::getName)
                            .orElseThrow(() -> new ApplicationException("error.column.notfound")))
                    .toArray(String[]::new);

            appendLine(scriptBuilder, "- addForeignKeyConstraint:", TABLE_PADDING_LEVEL);
            appendLine(scriptBuilder, "constraintName: ", ref.getName(), TABLE_PADDING_LEVEL + 1);
            appendLine(scriptBuilder, "baseTableName: ", baseTable.getName(), TABLE_PADDING_LEVEL + 1);
            appendLine(scriptBuilder, "baseColumnNames: ", String.join(", ", baseColumnNames), TABLE_PADDING_LEVEL + 1);
            appendLine(scriptBuilder, "referencedTableName: ", referencedTable.getName(), TABLE_PADDING_LEVEL + 1);
            appendLine(scriptBuilder, "referencedColumnNames: ", String.join(", ", referencedColumnNames), TABLE_PADDING_LEVEL + 1);
        }
    }

    private String getDecimalDefinition(ColumnMetadata column) {
        return "DECIMAL(" + column.getPrecision()  + "," + column.getScale() + ")";
    }

    private String getLimitedType(ColumnMetadata metadata) {
        Integer length = metadata.getLength();
        if (length == null) {
            return "";
        }
        String type = metadata.getColumnType().name();
        return type + "(" + length + ")";
    }

    private void writeValue(StringBuilder builder, String data) {
        builder.append("\"").append(data).append("\"");
    }

    private void addMinMaxDefinition(ColumnMetadata column, StringBuilder scriptBuilder) {
        StringBuilder valueBuilder = new StringBuilder();
        boolean isBetween = column.getMin() != null && column.getMax() != null;
        if (column.getMin() != null) {
            valueBuilder.append(column.getName()).append(">=").append(column.getMin());
        }
        if (isBetween) {
            valueBuilder.append(" AND ");
        }
        if (column.getMax() != null) {
            valueBuilder.append(column.getName()).append("<=").append(column.getMax());
        }

        appendLine(scriptBuilder, "checkConstraint: ", valueBuilder.toString(), COLUMN_PADDING_LEVEL + 2);
    }

    private void addConstraintsDefinition(ColumnMetadata column, StringBuilder scriptBuilder) {
        if (column.getConstraints().contains(ColumnMetadata.ConstraintType.NOT_NULL)) {
            appendLine(scriptBuilder, "nullable: ", "false", COLUMN_PADDING_LEVEL + 2);
        }
        if (column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)) {
            appendLine(scriptBuilder, "unique: ", "true", COLUMN_PADDING_LEVEL + 2);
        }
    }

    private void addHeader(StringBuilder scriptBuilder) {
        appendLine(scriptBuilder, "databaseChangeLog:", 0);
        appendLine(scriptBuilder, "- changeSet:", 1);
        appendLine(scriptBuilder, "id: 1", 2);
        appendLine(scriptBuilder, "author: Generated by ERMDev", 2);
        appendLine(scriptBuilder, "changes:", 2);
    }

    private void appendLine(StringBuilder scriptBuilder, String text, int level) {
        scriptBuilder.append(getPadding(level)).append(text).append("\n");
    }

    private void appendLine(StringBuilder scriptBuilder, String key, String value, int level) {
        scriptBuilder.append(getPadding(level)).append(key);
        writeValue(scriptBuilder, value);
        scriptBuilder.append("\n");
    }

    private String getPadding(int level) {
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < level; i++) {
            padding.append('\t');
        }

        return padding.toString();
    }
}
