package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractScriptFabric {
    protected static final String FK_TEMPLATE =
            """
            ALTER TABLE %s
            ADD CONSTRAINT %s
            FOREIGN KEY (%s)
            REFERENCES %s (%s)
            ON DELETE %s
            ON UPDATE %s;
            """;

    public static final Set<ColumnMetadata.ColumnType> lengthLimitedTypes = Set.of(
            ColumnMetadata.ColumnType.VARCHAR,
            ColumnMetadata.ColumnType.CHAR,
            ColumnMetadata.ColumnType.NUMERIC
    );

    public static final Set<ColumnMetadata.ColumnType> validAutoIncrementTypes = Set.of(
            ColumnMetadata.ColumnType.SMALLINT,
            ColumnMetadata.ColumnType.INT,
            ColumnMetadata.ColumnType.BIGINT
    );

    public static final Set<ColumnMetadata.ColumnType> minMaxableTypes = Set.of(
            ColumnMetadata.ColumnType.SMALLINT,
            ColumnMetadata.ColumnType.INT,
            ColumnMetadata.ColumnType.BIGINT,
            ColumnMetadata.ColumnType.NUMERIC,
            ColumnMetadata.ColumnType.DECIMAL,
            ColumnMetadata.ColumnType.FLOAT,
            ColumnMetadata.ColumnType.DOUBLE
    );

    public final String getReferenceDefinition(Map<UUID, TableMetadata> tables, ReferenceMetadata referenceMeta) {
        ReferenceMetadata.ReferenceKey key = referenceMeta.getKey();

        TableMetadata fromTable = tables.get(key.getFromTableId());
        TableMetadata toTable = tables.get(key.getToTableId());
        List<ColumnMetadata> fromColumns = Arrays.stream(key.getFromColumns())
                .map(k -> fromTable.getColumns().get(k))
                .toList();
        String[] fromColumnNames = fromColumns.stream()
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);

        List<ColumnMetadata> toColumns = Arrays.stream(key.getToColumns())
                .map(k -> toTable.getColumns().get(k))
                .toList();
        String[] toColumnNames = toColumns.stream()
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);

        if (referenceMeta.getName() == null) {
            referenceMeta.computeAndSetName();
        }

        return String.format(
                FK_TEMPLATE,
                fromTable.getName(),
                referenceMeta.getName(),
                String.join(", ", fromColumnNames),
                toTable.getName(),
                String.join(", ", toColumnNames),
                parseAction(referenceMeta.getOnDeleteAction()),
                parseAction(referenceMeta.getOnUpdateAction())
        );
    }

    public String getTableDefinition(TableMetadata tableMeta) {
        return String.format("CREATE TABLE IF NOT EXISTS %s (", tableMeta.getName());
    }

    protected String generateDecimalDefinition(ColumnMetadata metadata) {
        return "DECIMAL(" + metadata.getPrecision() + ", " + metadata.getScale() + ")";
    }

    protected String generateLengthLimitedDefinition(ColumnMetadata metadata) {
        int length = metadata.getLength();
        String type = getDefinitions().get(metadata.getColumnType());
        return type + "(" + length + ")";
    }

    protected final String parseAction(Enum<?> action) {
        String actionName = ReferenceMetadata.OnDeleteAction.NO_ACTION.name();
        if (action != null) {
            actionName = action.name();
        }
        return actionName.replace('_', ' ');
    }

    protected abstract String getMinMaxDefinition(ColumnMetadata column);
    public abstract String getColumnDefinition(ColumnMetadata columnMeta);
    public abstract String getIndexDefinition(TableMetadata tableMetadata, IndexMetadata indexMeta);
    protected abstract Map<ColumnMetadata.ColumnType, String> getDefinitions();
}