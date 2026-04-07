package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptBuilder;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component("mysqlBuilder")
public class MysqlScriptBuilder extends AbstractSqlScriptBuilder {

    private static final Map<ColumnMetadata.ColumnType, String> mysqlMapping = new HashMap<>() {{
        put(ColumnMetadata.ColumnType.BOOLEAN, "boolean");
        put(ColumnMetadata.ColumnType.SMALLINT, "smallint");
        put(ColumnMetadata.ColumnType.INT, "int");
        put(ColumnMetadata.ColumnType.BIGINT, "bigint");
        put(ColumnMetadata.ColumnType.NUMERIC, "numeric");
        put(ColumnMetadata.ColumnType.DECIMAL, "decimal");
        put(ColumnMetadata.ColumnType.FLOAT, "float");
        put(ColumnMetadata.ColumnType.DOUBLE, "double");
        put(ColumnMetadata.ColumnType.CHAR, "char");
        put(ColumnMetadata.ColumnType.VARCHAR, "varchar");
        put(ColumnMetadata.ColumnType.TEXT, "text");
        put(ColumnMetadata.ColumnType.UUID, "varchar(36)");
        put(ColumnMetadata.ColumnType.TIME, "time");
        put(ColumnMetadata.ColumnType.DATE, "date");
        put(ColumnMetadata.ColumnType.DATETIME, "datetime");
        put(ColumnMetadata.ColumnType.TIMESTAMP, "timestamp");
        put(ColumnMetadata.ColumnType.JSON, "json");
    }};

    private static final Map<IndexMetadata.IndexType, String> indexMapping = Map.of(
            IndexMetadata.IndexType.B_TREE, "BTREE",
            IndexMetadata.IndexType.HASH, "HASH"
    );

    private static final Map<ColumnMetadata.ColumnType, String> MYSQL_TIME_CURRENT_VALUES = Map.of(
            ColumnMetadata.ColumnType.DATE, "(CURRENT_DATE)",
            ColumnMetadata.ColumnType.TIME, "(CURRENT_TIME)",
            ColumnMetadata.ColumnType.DATETIME, "CURRENT_TIMESTAMP",
            ColumnMetadata.ColumnType.TIMESTAMP, "CURRENT_TIMESTAMP"
    );

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
                .append(" DROP PRIMARY KEY;\n");
    }

    @Override
    public void appendAndPkConstraint(StringBuilder scriptBuilder, TableMetadata toTable) {
        scriptBuilder.append("ALTER TABLE ")
                .append(toTable.getName())
                .append(" ADD PRIMARY KEY (")
                .append(toTable.getPkContated())
                .append(");\n");
    }

    @Override
    public void appendDropFK(ReferenceMetadata ref, StringBuilder scriptBuilder) {
        scriptBuilder.append("ALTER TABLE ")
                .append(ref.getBaseTable().getName())
                .append(" DROP FOREIGN KEY ")
                .append(ref.getName())
                .append(";\n");
    }

    @Override
    protected String getSuitableType(ColumnMetadata metadata) {
        ColumnMetadata.ColumnType type = metadata.getColumnType();
        String baseType;

        if (MetadataTypeUtils.lengthLimitedTypes.contains(type) && metadata.getLength() != null) {
            if (type != ColumnMetadata.ColumnType.DECIMAL) {
                baseType = getLengthLimitedType(metadata);
            } else {
                baseType = getDecimalDefinition(metadata);
            }
        } else {
            baseType = mysqlMapping.get(type);
        }

        if (Objects.requireNonNullElse(metadata.getAutoIncrement(), false)) {
            if (type == ColumnMetadata.ColumnType.SMALLINT ||
                    type == ColumnMetadata.ColumnType.INT ||
                    type == ColumnMetadata.ColumnType.BIGINT) {
                return baseType + " AUTO_INCREMENT";
            } else {
                throw new ApplicationException("Unsupported autoincrement type for MySQL: " + type);
            }
        }

        return baseType;
    }

    @Override
    public void appendAddUnique(StringBuilder scriptBuilder, ColumnMetadata column) {
        String constraintName = UQ_CONSTRAINT_PATTERN.formatted(column.getTable().getName().toLowerCase(),
                column.getName().toLowerCase());
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ADD CONSTRAINT ")
                .append(constraintName)
                .append(" UNIQUE (")
                .append(column.getName())
                .append(");\n");
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

        scriptBuilder.append(defaultValue).append(";\n");
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
        String constraintName = CHECK_CONSTRAINT_PATTERN.formatted(newColumn.getTable().getName().toLowerCase(),
                newColumn.getName().toLowerCase());
        scriptBuilder.append("ALTER TABLE ")
                .append(newColumn.getTable().getName())
                .append(" DROP CHECK ")
                .append(constraintName)
                .append(";\n");
    }

    @Override
    public void appendMinMaxConstraint(StringBuilder scriptBuilder, ColumnMetadata column) {
        String constraintName = CHECK_CONSTRAINT_PATTERN.formatted(column.getTable().getName().toLowerCase(),
                column.getName().toLowerCase());
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" ADD CONSTRAINT ")
                .append(constraintName)
                .append(getMinMaxDefinition(column))
                .append(";\n");
    }


    @Override
    public Map<ColumnMetadata.ColumnType, String> getDefinitions() {
        return mysqlMapping;
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

        indexBuilder.append(" (");
        indexBuilder.append(String.join(", ", affectedCols));
        indexBuilder.append(")");

        indexBuilder.append(" USING ");
        indexBuilder.append(indexMapping.get(index.getIndexType()));
        indexBuilder.append(";\n");
    }

    @Override
    public void appendDropIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx) {
        scriptBuilder.append("ALTER TABLE ")
                .append(idx.getTable().getName())
                .append(" DROP INDEX ")
                .append(idx.getName())
                .append(";\n");
    }

    @Override
    public void appendRenameIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx, IndexMetadata toIdx) {
        scriptBuilder.append("ALTER TABLE ")
                .append(idx.getTable().getName())
                .append(" RENAME INDEX ")
                .append(idx.getName())
                .append(" TO ")
                .append(toIdx.getName())
                .append(";\n");
    }

    @Override
    public void appendChangeColumnType(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" MODIFY COLUMN ")
                .append(column.getName())
                .append(' ');
        appendColumnDefinition(scriptBuilder, column, true);
        scriptBuilder.append(";\n");
    }

    @Override
    public void appendNotNullConstraint(StringBuilder scriptBuilder, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" MODIFY COLUMN ")
                .append(column.getName())
                .append(' ')
                .append(getSuitableType(column))
                .append(" NOT NULL;\n");
    }

    @Override
    public void appendDropNotNull(StringBuilder scriptBuilder, ColumnMetadata fromColumn, ColumnMetadata column) {
        scriptBuilder.append("ALTER TABLE ")
                .append(column.getTable().getName())
                .append(" MODIFY COLUMN ")
                .append(column.getName())
                .append(' ')
                .append(getSuitableType(column))
                .append(" NULL;\n");
    }

    @Override
    public void appendDropUnique(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn) {
        String constraintName = UQ_CONSTRAINT_PATTERN.formatted(newColumn.getTable().getName().toLowerCase(),
                newColumn.getName().toLowerCase());
        scriptBuilder.append("ALTER TABLE ")
                .append(oldColumn.getTable().getName())
                .append(" DROP INDEX ")
                .append(constraintName)
                .append(";\n");
    }

    @Override
    protected String getDefaultValueForTimeType(ColumnMetadata column) {
        return MYSQL_TIME_CURRENT_VALUES.getOrDefault(column.getColumnType(), "now()");
    }
}