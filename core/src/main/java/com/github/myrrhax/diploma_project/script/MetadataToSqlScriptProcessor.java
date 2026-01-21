package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class MetadataToSqlScriptProcessor {

    public final String convertMetadataToSql(SchemaStateMetadata metadata) {
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder indexesBuilder = new StringBuilder();
        List<ReferenceMetadata> refsToProcess = new ArrayList<>();
        List<TableMetadata> tablesToProcess = new ArrayList<>(metadata.getTables().values());

        List<ReferenceMetadata> references = metadata.getReferences().values()
                .stream()
                .peek(refsToProcess::add)
                .toList();

        // Обработка M-M связей
        references.stream()
                .filter(ref -> ref.getType() == ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                .forEach(mtmRef -> {
                    refsToProcess.remove(mtmRef);
                    MtmTableProcessingResult res = buildTableAndRefsFromMtmRef(metadata, mtmRef);
                    tablesToProcess.add(res.mtmTable());
                    refsToProcess.addAll(Arrays.asList(res.betweenRefs()));
                });

        // Разворот 1-M связей
        references.stream()
                .filter(ref -> ref.getType() == ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                .forEach(otmRef -> {
                    refsToProcess.remove(otmRef);
                    refsToProcess.add(rotateOtmReference(otmRef));
                });

        for (TableMetadata tableMetadata : tablesToProcess) {
            sqlBuilder.append(getScriptFabric().getTableDefinition(tableMetadata));
            sqlBuilder.append('\n');
            List<ColumnMetadata> columns = tableMetadata.getColumns().values()
                    .stream().toList();

            List<ColumnMetadata> primaryKeyParts = tableMetadata.getPrimaryKeyParts();
            if (primaryKeyParts.isEmpty()) {
                throw new ApplicationException("Table must contain primary key", HttpStatus.BAD_REQUEST);
            }
            buildColumnsPart(columns, sqlBuilder);
            buildPrimaryKeyConstraint(primaryKeyParts, sqlBuilder);

            sqlBuilder.append(");\n");

            List<IndexMetadata> indexes = tableMetadata.getIndexes();
            buildIndexPart(tableMetadata, indexes, indexesBuilder);
        }

        buildReferencePart(tablesToProcess.stream()
                        .collect(Collectors.toMap(TableMetadata::getId, Function.identity())),
                refsToProcess,
                sqlBuilder);
        sqlBuilder.append(indexesBuilder);

        return sqlBuilder.toString();
    }

    protected abstract AbstractScriptFabric getScriptFabric();

    private MtmTableProcessingResult buildTableAndRefsFromMtmRef(SchemaStateMetadata metadata,
                                                                 ReferenceMetadata ref) {
        TableMetadata fromTable = metadata.getTables().get(ref.getKey()
                .getFromTableId());
        TableMetadata toTable = metadata.getTables().get(ref.getKey()
                .getToTableId());

        ColumnMetadata[] fromCols = Arrays.stream(ref.getKey().getFromColumns())
                .map(id -> fromTable.getColumns().get(id))
                .toArray(ColumnMetadata[]::new);
        ColumnMetadata[] toCols = Arrays.stream(ref.getKey().getToColumns())
                .map(id -> toTable.getColumns().get(id))
                .toArray(ColumnMetadata[]::new);

        ColumnMetadata[] mtmFrom = new ColumnMetadata[fromCols.length];
        for (int i = 0; i < fromCols.length; i++) {
            mtmFrom[i] = cloneColumn(fromTable, fromCols[i]);
        }

        ColumnMetadata[] mtmTo = new ColumnMetadata[fromCols.length];
        for (int i = 0; i < toCols.length; i++) {
            mtmTo[i] = cloneColumn(toTable, toCols[i]);
        }

        List<ColumnMetadata> concatMtmCols = new ArrayList<>(Arrays.stream(mtmFrom).toList());
        concatMtmCols.addAll(Arrays.stream(mtmTo).toList());

        LinkedHashMap<UUID, ColumnMetadata> concatColumnsMap = new LinkedHashMap<>(concatMtmCols.size());
        for (ColumnMetadata column : concatMtmCols) {
            concatColumnsMap.put(column.getId(), column);
        }

        TableMetadata mtmTable = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name(computeMtmTableName(fromTable, toTable))
                .columns(concatColumnsMap)
                .primaryKeyParts(concatMtmCols)
                .build();

        ReferenceMetadata ftmRef = buildRef(mtmTable, fromTable, mtmFrom, fromCols);
        ReferenceMetadata mttRef = buildRef(mtmTable, toTable, mtmTo, toCols);

        return new MtmTableProcessingResult(mtmTable, new ReferenceMetadata[]{ftmRef, mttRef});
    }

    private static ReferenceMetadata buildRef(TableMetadata fromTable,
                                              TableMetadata toTable,
                                              ColumnMetadata[] fromCols,
                                              ColumnMetadata[] toCols) {
        return ReferenceMetadata.builder()
                .key(ReferenceMetadata.ReferenceKey.builder()
                        .fromTableId(fromTable.getId())
                        .toTableId(toTable.getId())
                        .fromColumns(Arrays.stream(fromCols)
                                .map(ColumnMetadata::getId)
                                .toArray(UUID[]::new))
                        .toColumns(Arrays.stream(toCols)
                                .map(ColumnMetadata::getId)
                                .toArray(UUID[]::new))
                        .build())
                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                .build();
    }

    private static String computeMtmTableName(TableMetadata fromTable, TableMetadata toTable) {
        return String.format("mtm_%s_%s", fromTable.getName(), toTable.getName());
    }

    private ReferenceMetadata rotateOtmReference(ReferenceMetadata otmRef) {
        return ReferenceMetadata.builder()
                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                .onUpdateAction(otmRef.getOnUpdateAction())
                .onDeleteAction(otmRef.getOnDeleteAction())
                .key(ReferenceMetadata.ReferenceKey.builder()
                        .fromTableId(otmRef.getKey().getToTableId())
                        .fromColumns(otmRef.getKey().getToColumns())
                        .toTableId(otmRef.getKey().getFromTableId())
                        .toColumns(otmRef.getKey().getFromColumns())
                        .build())
                .build();
    }

    private ColumnMetadata cloneColumn(TableMetadata table, ColumnMetadata origin) {
        return ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name(table.getName() + "_" + origin.getName())
                .type(origin.getType())
                .scale(origin.getScale())
                .precision(origin.getPrecision())
                .defaultValue(origin.getDefaultValue())
                .build();
    }

    private void buildPrimaryKeyConstraint(List<ColumnMetadata> primaryKeyParts, StringBuilder sqlBuilder) {
        sqlBuilder.append("\tPRIMARY KEY (");
        for (int i = 0; i < primaryKeyParts.size(); i++) {
            ColumnMetadata column = primaryKeyParts.get(i);
            sqlBuilder.append(column.getName());
            if (i < primaryKeyParts.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(")\n");
    }

    private void buildReferencePart(Map<UUID, TableMetadata> tables,
                                    List<ReferenceMetadata> refs,
                                    StringBuilder sqlBuilder) {
        for (ReferenceMetadata ref : refs) {
            sqlBuilder.append(getScriptFabric().getReferenceDefinition(tables, ref));
            sqlBuilder.append("\n");
        }
    }

    private void buildIndexPart(TableMetadata tableMetadata, List<IndexMetadata> indexes, StringBuilder indexBuilder) {
        for (IndexMetadata indexMetadata : indexes) {
            indexBuilder.append(getScriptFabric().getIndexDefinition(tableMetadata, indexMetadata));
            indexBuilder.append("\n");
        }
    }

    private void buildColumnsPart(List<ColumnMetadata> columns, StringBuilder sb) {
        for (ColumnMetadata columnMeta : columns) {
            String definition = getScriptFabric().getColumnDefinition(columnMeta);
            sb.append('\t');
            sb.append(definition);
            sb.append(',');
            sb.append('\n');
        }
    }

    private record MtmTableProcessingResult(
            TableMetadata mtmTable,
            ReferenceMetadata[] betweenRefs
    ) {
    }
}
