package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class FullScriptProcessor implements ScriptProcessor {

    @Override
    public boolean supportsGenerationType(GeneratedScriptType genType) {
        return GeneratedScriptType.FULL.equals(genType);
    }

    @Override
    public String process(String name, SchemaStateMetadata schema) {
        var clone = schema.clone();
        List<ReferenceMetadata> refsToProcess = new ArrayList<>();
        List<TableMetadata> tablesToProcess = new ArrayList<>(clone.getTables().values());

        List<ReferenceMetadata> references = schema.getReferences().values()
                .stream()
                .peek(refsToProcess::add)
                .toList();

        // Обработка M-M связей
        references.stream()
                .filter(ref -> ref.getType() == ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                .forEach(mtmRef -> {
                    refsToProcess.remove(mtmRef);
                    MtmTableProcessingResult res = buildTableAndRefsFromMtmRef(schema, mtmRef);
                    tablesToProcess.add(res.mtmTable());
                    refsToProcess.addAll(Arrays.asList(res.betweenRefs()));
                });

        // Разворот 1-M связей
        references.stream()
                .filter(ref -> ref.getType() == ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                .forEach(otmRef -> {
                    refsToProcess.remove(otmRef);
                    refsToProcess.add(rotateOtmReference(otmRef, clone));
                });

        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder indexesBuilder = new StringBuilder();

        FullScriptFabric fabric = getFabric();
        fabric.appendHeader(sqlBuilder, schema, name);

        for (TableMetadata table : tablesToProcess) {
            fabric.appendTableDefinition(sqlBuilder, table);

            for (ColumnMetadata column : table.getColumns().values()) {
                fabric.appendColumnDefinition(sqlBuilder, column);
                sqlBuilder.append("\n");
            }
            for (IndexMetadata idx : table.getIndexes().values()) {
                fabric.appendIndexDefinition(indexesBuilder, idx);
                indexesBuilder.append("\n");
            }
            onEndTableDefinition(sqlBuilder, table);
            fabric.appendEndTablePadding(sqlBuilder);
        }

        for (ReferenceMetadata ref : refsToProcess) {
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

            fabric.appendReferenceDefinition(sqlBuilder,
                    ref.getName(),
                    baseTable,
                    referencedTable,
                    baseColumnNames,
                    referencedColumnNames,
                    ref.getOnDeleteAction(),
                    ref.getOnUpdateAction());
            sqlBuilder.append('\n');
        }
        sqlBuilder.append(indexesBuilder);

        return sqlBuilder.toString();
    }

    protected abstract void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table);
    protected abstract FullScriptFabric getFabric();

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
            column.setPkPart(true);
        }

        TableMetadata mtmTable = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name(computeMtmTableName(fromTable, toTable))
                .columns(concatColumnsMap)
                .primaryKeyParts(concatMtmCols.stream()
                        .map(ColumnMetadata::getId)
                        .collect(Collectors.toSet()))
                .schemaState(metadata)
                .build();
        metadata.addTable(mtmTable);

        ReferenceMetadata ftmRef = buildRef(mtmTable, fromTable, mtmFrom, fromCols, metadata);
        ReferenceMetadata mttRef = buildRef(mtmTable, toTable, mtmTo, toCols, metadata);
        metadata.addReference(ftmRef);
        metadata.addReference(mttRef);

        return new MtmTableProcessingResult(mtmTable, new ReferenceMetadata[]{ftmRef, mttRef});
    }

    private ReferenceMetadata buildRef(TableMetadata fromTable,
                                       TableMetadata toTable,
                                       ColumnMetadata[] fromCols,
                                       ColumnMetadata[] toCols,
                                       SchemaStateMetadata metadata) {
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
                .schemaState(metadata)
                .build();
    }

    private static String computeMtmTableName(TableMetadata fromTable, TableMetadata toTable) {
        return String.format("mtm_%s_%s", fromTable.getName(), toTable.getName());
    }

    private ReferenceMetadata rotateOtmReference(ReferenceMetadata otmRef, SchemaStateMetadata metadata) {
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
                .schemaState(metadata)
                .build();
    }

    private ColumnMetadata cloneColumn(TableMetadata table, ColumnMetadata origin) {
        return ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name(table.getName() + "_" + origin.getName())
                .columnType(origin.getColumnType())
                .scale(origin.getScale())
                .precision(origin.getPrecision())
                .defaultValue(origin.getDefaultValue())
                .build();
    }

    private record MtmTableProcessingResult(
            TableMetadata mtmTable,
            ReferenceMetadata[] betweenRefs
    ) {
    }
}
