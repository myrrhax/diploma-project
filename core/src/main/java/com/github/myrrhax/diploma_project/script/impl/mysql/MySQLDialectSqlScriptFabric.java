package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.script.AbstractFullSqlScriptFabric;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component("mysqlDialectFabric")
public class MySQLDialectSqlScriptFabric {
//
//    Map<ColumnMetadata.ColumnType, String> mysqlMapping = new HashMap<>() {{
//        put(ColumnMetadata.ColumnType.BOOLEAN, "boolean");
//        put(ColumnMetadata.ColumnType.SMALLINT, "smallint");
//        put(ColumnMetadata.ColumnType.INT, "int");
//        put(ColumnMetadata.ColumnType.BIGINT, "bigint");
//        put(ColumnMetadata.ColumnType.NUMERIC, "numeric");
//        put(ColumnMetadata.ColumnType.DECIMAL, "decimal");
//        put(ColumnMetadata.ColumnType.FLOAT, "float");
//        put(ColumnMetadata.ColumnType.DOUBLE, "double");
//        put(ColumnMetadata.ColumnType.CHAR, "char");
//        put(ColumnMetadata.ColumnType.VARCHAR, "varchar");
//        put(ColumnMetadata.ColumnType.TEXT, "text");
//        put(ColumnMetadata.ColumnType.UUID, "varchar(36)");
//        put(ColumnMetadata.ColumnType.TIME, "time");
//        put(ColumnMetadata.ColumnType.DATE, "date");
//        put(ColumnMetadata.ColumnType.DATETIME, "datetime");
//        put(ColumnMetadata.ColumnType.TIMESTAMP, "timestamp");
//        put(ColumnMetadata.ColumnType.JSON, "json");
//    }};
//
//    Map<IndexMetadata.IndexType, String> indexMapping = Map.of(
//            IndexMetadata.IndexType.B_TREE, "BTREE",
//            IndexMetadata.IndexType.HASH, "HASH"
//    );
//
////    @Override
//    public String getMinMaxDefinition(ColumnMetadata column) {
//        boolean hasMin = column.getMin() != null;
//        boolean hasMax = column.getMax() != null;
//
//        if (!hasMin && !hasMax) {
//            throw new ApplicationException("Failed to generate min/max definition");
//        }
//        boolean hasBetween = hasMin && hasMax;
//        StringBuilder result = new StringBuilder();
//        result.append(" CHECK(");
//        result.append(column.getName());
//
//        if (hasBetween) {
//            result.append(" BETWEEN ");
//            result.append(column.getMin());
//            result.append(" AND ").append(column.getMax());
//        } else if (hasMin) {
//            result.append(" >= ").append(column.getMin());
//        } else {
//            result.append(" <= ").append(column.getMax());
//        }
//
//        result.append(")");
//        return result.toString();
//    }
//
////    @Override
//    public String getColumnDefinition(ColumnMetadata columnMeta) {
//        String typeName = getSuitableType(columnMeta);
//        StringBuilder sb = new StringBuilder();
//        sb.append(columnMeta.getName()).append(" ").append(typeName);
//        List<ColumnMetadata.ConstraintType> constraints = columnMeta.getConstraints();
//
//        for (ColumnMetadata.ConstraintType constraint : constraints) {
//            sb.append(" ").append(constraint.name().replace('_', ' '));
//        }
//
//        if (columnMeta.getDefaultValue() != null) {
//            sb.append(" DEFAULT ").append(columnMeta.getDefaultValue());
//        }
//
//        if (columnMeta.getMin() != null || columnMeta.getMax() != null) {
//            sb.append(getMinMaxDefinition(columnMeta));
//        }
//
//        return sb.toString();
//    }
//
////    @Override
//    public String getIndexDefinition(TableMetadata tableMetadata,
//                                     IndexMetadata indexMeta) {
//        StringBuilder sb = new StringBuilder();
//        String[] affectedCols = indexMeta.getColumnIds().stream()
//                .map(col -> tableMetadata.getColumns().get(col))
//                .map(ColumnMetadata::getName)
//                .toArray(String[]::new);
//
//        sb.append("CREATE ");
//        if (indexMeta.isUnique()) {
//            sb.append("UNIQUE ");
//        }
//        sb.append("INDEX ");
//        if (indexMeta.getIndexName() == null) {
//            indexMeta.computeAndSetName();
//        }
//        sb.append(indexMeta.getIndexName());
//        sb.append(" ON ");
//        sb.append(tableMetadata.getName());
//
//        sb.append(" (");
//        sb.append(String.join(", ", affectedCols));
//        sb.append(")");
//
//        sb.append(" USING ");
//        sb.append(indexMapping.get(indexMeta.getIndexType()));
//        sb.append(";");
//
//        return sb.toString();
//    }
//
//    private String getSuitableType(ColumnMetadata metadata) {
//        ColumnMetadata.ColumnType type = metadata.getColumnType();
//        String baseType;
//
//        if (MetadataTypeUtils.lengthLimitedTypes.contains(type) && metadata.getLength() != null) {
//            if (type != ColumnMetadata.ColumnType.DECIMAL) {
//                baseType = generateLengthLimitedDefinition(metadata);
//            } else {
//                baseType = generateDecimalDefinition(metadata);
//            }
//        } else {
//            baseType = mysqlMapping.get(type);
//        }
//
//        if (Objects.requireNonNullElse(metadata.getAutoIncrement(), false)) {
//            if (type == ColumnMetadata.ColumnType.SMALLINT ||
//                    type == ColumnMetadata.ColumnType.INT ||
//                    type == ColumnMetadata.ColumnType.BIGINT) {
//                return baseType + " AUTO_INCREMENT";
//            } else {
//                throw new ApplicationException("Unsupported autoincrement type for MySQL: " + type);
//            }
//        }
//
//        return baseType;
//    }
//
//    @Override
//    public Map<ColumnMetadata.ColumnType, String> getDefinitions() {
//        return mysqlMapping;
//    }
}