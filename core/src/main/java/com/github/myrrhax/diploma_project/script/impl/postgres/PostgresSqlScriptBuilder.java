package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptBuilder;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component("postgresBuilder")
public class PostgresSqlScriptBuilder extends AbstractSqlScriptBuilder {
    private static final Map<ColumnMetadata.ColumnType, String> postgresMapping = new HashMap<>() {{
        put(ColumnMetadata.ColumnType.BOOLEAN, "boolean");
        put(ColumnMetadata.ColumnType.SMALLINT, "smallint");
        put(ColumnMetadata.ColumnType.INT, "integer");
        put(ColumnMetadata.ColumnType.BIGINT, "bigint");
        put(ColumnMetadata.ColumnType.NUMERIC, "numeric");
        put(ColumnMetadata.ColumnType.DECIMAL, "decimal");
        put(ColumnMetadata.ColumnType.FLOAT, "real");
        put(ColumnMetadata.ColumnType.DOUBLE, "double precision");
        put(ColumnMetadata.ColumnType.CHAR, "char");
        put(ColumnMetadata.ColumnType.VARCHAR, "varchar");
        put(ColumnMetadata.ColumnType.TEXT, "text");
        put(ColumnMetadata.ColumnType.UUID, "uuid");
        put(ColumnMetadata.ColumnType.TIME, "time");
        put(ColumnMetadata.ColumnType.DATE, "date");
        put(ColumnMetadata.ColumnType.DATETIME, "timestamp");
        put(ColumnMetadata.ColumnType.TIMESTAMP, "timestamp");
        put(ColumnMetadata.ColumnType.JSON, "jsonb");
    }};

    private static final Map<IndexMetadata.IndexType, String> indexMapping = Map.of(
        IndexMetadata.IndexType.B_TREE, "btree",
        IndexMetadata.IndexType.HASH, "hash"
    );

    private static final Map<ColumnMetadata.ColumnType, String> TIME_CURRENT_VALUES = Map.of(
        ColumnMetadata.ColumnType.DATE, "CURRENT_DATE",
        ColumnMetadata.ColumnType.TIME, "CURRENT_TIME",
        ColumnMetadata.ColumnType.DATETIME, "NOW()",
        ColumnMetadata.ColumnType.TIMESTAMP, "NOW()"
    );

    @Override
    public Map<ColumnMetadata.ColumnType, String> getDefinitions() {
        return postgresMapping;
    }

    @Override
    public void appendRenameTable(StringBuilder scriptBuilder, TableMetadata from, TableMetadata to) {
        scriptBuilder.append("ALTER TABLE ")
                .append(from.getName())
                .append(" RENAME TO ")
                .append(to.getName())
                .append(";\n");
    }

    @Override
    public void appendDropPkConstraint(StringBuilder scriptBuilder, TableMetadata toTable) {
        scriptBuilder.append("ALTER TABLE ")
                .append(toTable.getName())
                .append(" DROP CONSTRAINT ")
                .append("pk_").append(toTable.getName().toLowerCase())
                .append(";\n");
    }

    @Override
    public void appendAndPkConstraint(StringBuilder scriptBuilder, TableMetadata toTable) {
        scriptBuilder.append("ALTER TABLE ")
                .append(toTable.getName())
                .append(" ADD CONSTRAINT ")
                .append("pk_").append(toTable.getName().toLowerCase())
                .append(" PRIMARY KEY (")
                .append(toTable.getPkContated())
                .append(");\n");
    }

    @Override
    public void appendDropFK(ReferenceMetadata ref, StringBuilder scriptBuilder) {
        scriptBuilder.append("ALTER TABLE ")
                .append(ref.getBaseTable().getName())
                .append(" DROP CONSTRAINT ")
                .append(ref.getName())
                .append(";\n");
    }

    @Override
    public void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index) {
        TableMetadata tableMetadata = index.getTable();
        String[] affectedCols = index.getColumnIds().stream()
                .map(col -> tableMetadata.getColumns().get(col))
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);

        indexBuilder.append("CREATE ");
        if (index.isUnique()) {
            indexBuilder.append("UNIQUE ");
        }
        indexBuilder.append("INDEX ");
        if (index.getName() == null) {
            index.computeAndSetName();
        }
        indexBuilder.append(index.getName());
        indexBuilder.append(" ON ");
        indexBuilder.append(tableMetadata.getName());
        indexBuilder.append(" USING ");
        indexBuilder.append(indexMapping.get(index.getIndexType()));
        indexBuilder.append(" (");
        indexBuilder.append(String.join(", ", affectedCols));
        indexBuilder.append(");\n");
    }

    @Override
    protected String getSuitableType(ColumnMetadata metadata) {
        ColumnMetadata.ColumnType type = metadata.getColumnType();
        if (!Objects.requireNonNullElse(metadata.getAutoIncrement(), false)
            && !(MetadataTypeUtils.lengthLimitedTypes.contains(type) && metadata.getLength() != null)) {
            return postgresMapping.get(type);
        }

        if (MetadataTypeUtils.lengthLimitedTypes.contains(type) ) {
            if (type != ColumnMetadata.ColumnType.DECIMAL) {
                return getLengthLimitedType(metadata);
            }
            return getDecimalDefinition(metadata);
        }

        return switch (type) {
            case SMALLINT -> "smallserial";
            case INT -> "serial";
            case BIGINT -> "bigserial";
            default -> throw new RuntimeException("Unsupported autoincrement type: " + type);
        };
    }

    @Override
    public void appendAddUnique(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ADD CONSTRAINT ")
                .append(UQ_CONSTRAINT_PATTERN.formatted(column.getTable().getName().toLowerCase(),
                        column.getName().toLowerCase()))
                .append(" UNIQUE ")
                .append(column.getName())
                .append(";\n");
    }

    @Override
    public void addDefaultValue(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" SET DEFAULT ");
        String defaultValue = column.getDefaultValue();
        if (MetadataTypeUtils.timeTypes.contains(column.getColumnType()) && defaultValue.equals("now")) {
            defaultValue = getDefaultValueForTimeType(column);
        }
        scriptBuilder.append(defaultValue)
                .append(";\n");
    }

    @Override
    public void dropDefaultValue(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" DROP DEFAULT;\n");
    }

    @Override
    public void appendDropMinMax(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn) {
        scriptBuilder.append("ALTER TABLE ")
                .append(newColumn.getTable().getName())
                .append(" DROP CONSTRAINT ");
        String constraint = CHECK_CONSTRAINT_PATTERN.formatted(newColumn.getTable().getName().toLowerCase(),
                newColumn.getName().toLowerCase());
        scriptBuilder.append(constraint)
                .append(";\n");
    }

    @Override
    public void appendMinMaxConstraint(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ADD CONSTRAINT ");
        String constraint = CHECK_CONSTRAINT_PATTERN.formatted(column.getTable().getName().toLowerCase(),
                column.getName().toLowerCase());
        scriptBuilder.append(constraint)
                .append(' ');
        scriptBuilder.append(getMinMaxDefinition(column))
                .append(";\n");
    }

    @Override
    public void appendDropAutoIncrement(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" DROP IDENTITY IF EXISTS;\n");
    }

    @Override
    public void appendAddAutoIncrement(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" ADD GENERATED BY DEFAULT AS IDENTITY;\n");
    }

    @Override
    public void appendDropIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx) {
        scriptBuilder.append("DROP INDEX ")
                .append(idx.getName())
                .append(";\n");
    }

    @Override
    public void appendRenameIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx, IndexMetadata toIdx) {
        scriptBuilder.append("ALTER INDEX ")
                .append(idx.getName())
                .append(" RENAME TO ")
                .append(toIdx.getName())
                .append(";\n");
    }

    @Override
    public void appendChangeColumnType(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" TYPE ")
                .append(getSuitableType(column))
                .append(";\n");
    }

    @Override
    public void appendNotNullConstraint(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" SET NOT NULL;\n");
    }

    @Override
    public void appendDropNotNull(StringBuilder scriptBuilder, ColumnMetadata fromColumn, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ALTER COLUMN ")
                .append(column.getName())
                .append(" DROP NOT NULL;\n");
    }

    @Override
    public void appendDropUnique(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn) {
        scriptBuilder.append("ALTER TABLE ")
                .append(newColumn.getTable().getName())
                .append(" DROP CONSTRAINT ");
        String constraintName = UQ_CONSTRAINT_PATTERN.formatted(newColumn.getTable().getName().toLowerCase(),
                newColumn.getName().toLowerCase());
        scriptBuilder.append(constraintName)
                .append(";\n");
    }

    @Override
    protected String getDefaultValueForTimeType(ColumnMetadata column) {
        return TIME_CURRENT_VALUES.getOrDefault(column.getColumnType(), "now()");
    }
}
