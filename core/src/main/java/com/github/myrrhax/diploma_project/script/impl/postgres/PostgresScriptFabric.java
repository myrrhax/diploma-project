package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component("postgresFullFabric")
public class PostgresScriptFabric extends AbstractSqlScriptFabric {
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

    @Override
    public Map<ColumnMetadata.ColumnType, String> getDefinitions() {
        return postgresMapping;
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
        if (index.getIndexName() == null) {
            index.computeAndSetName();
        }
        indexBuilder.append(index.getIndexName());
        indexBuilder.append(" ON ");
        indexBuilder.append(tableMetadata.getName());
        indexBuilder.append(" USING ");
        indexBuilder.append(indexMapping.get(index.getIndexType()));
        indexBuilder.append(" (");
        indexBuilder.append(String.join(", ", affectedCols));
        indexBuilder.append(");");
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
}
