package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("postgresDialectFabric")
public class PostgreSQLDialectScriptFabric extends AbstractScriptFabric {
    Map<ColumnMetadata.ColumnType, String> postgresMapping = new HashMap<>() {{
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
    Map<IndexMetadata.IndexType, String> indexMapping = Map.of(
        IndexMetadata.IndexType.B_TREE, "btree",
        IndexMetadata.IndexType.HASH, "hash"
    );

    @Override
    public String getColumnDefinition(ColumnMetadata columnMeta) {
        String typeName = getSuitableType(columnMeta);
        StringBuilder sb = new StringBuilder();
        sb.append(columnMeta.getName()).append(" ").append(typeName);
        List<ColumnMetadata.ConstraintType> constraints = columnMeta.getConstraints();

        for (ColumnMetadata.ConstraintType constraint : constraints) {
            sb.append(" ").append(constraint.name().replace('_', ' '));
        }

        if (columnMeta.getDefaultValue() != null) {
            sb.append(" DEFAULT ").append(columnMeta.getDefaultValue());
        }

        return sb.toString();
    }

    @Override
    public String getIndexDefinition(TableMetadata tableMetadata,
                                     IndexMetadata indexMeta) {
        StringBuilder sb = new StringBuilder();
        String[] affectedCols = indexMeta.getColumnIds().stream()
                .map(col -> tableMetadata.getColumns().get(col))
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);

        sb.append("CREATE ");
        if (indexMeta.isUnique()) {
            sb.append("UNIQUE ");
        }
        sb.append("INDEX ");
        if (indexMeta.getIndexName() == null) {
            indexMeta.setIndexName(computeIndexName(tableMetadata.getName(), affectedCols, indexMeta.isUnique()));
        }
        sb.append(indexMeta.getIndexName());
        sb.append(" ON ");
        sb.append(tableMetadata.getName());
        sb.append(" USING ");
        sb.append(indexMapping.get(indexMeta.getIndexType()));
        sb.append(" (");
        sb.append(String.join(", ", affectedCols));
        sb.append(");");

        return sb.toString();
    }

    private String getSuitableType(ColumnMetadata metadata) {
        ColumnMetadata.ColumnType type = metadata.getType();
        if (metadata.getAdditions().stream().noneMatch(
                addition ->
                    addition == ColumnMetadata.AdditionalComponent.AUTO_INCREMENT)
            && !lengthLimitedTypes.contains(type)) {
            return postgresMapping.get(type);
        }

        if (lengthLimitedTypes.contains(type)) {
            if (type != ColumnMetadata.ColumnType.DECIMAL) {
                return generateLengthLimitedDefinition(metadata);
            }
            return generateDecimalDefinition(metadata);
        }

        return switch (type) {
            case SMALLINT -> "smallserial";
            case INT -> "serial";
            case BIGINT -> "bigserial";
            default -> throw new RuntimeException("Unsupported autoincrement type: " + type);
        };
    }

    @Override
    protected Map<ColumnMetadata.ColumnType, String> getDefinitions() {
        return postgresMapping;
    }
}
