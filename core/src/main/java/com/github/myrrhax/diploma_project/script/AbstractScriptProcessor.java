package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractScriptProcessor {
    protected final DifferenceProcessor differenceProcessor;

    public String processFullScript(String name, SchemaStateMetadata schema) {
        StringBuilder scriptBuilder = new StringBuilder();
        SchemaStateMetadata preparedSchema = prepareSchema(schema);
        ScriptFabric fabric = getFabric();
        fabric.appendHeader(scriptBuilder, name);

        Collection<TableMetadata> tables = preparedSchema.getTables().values();
        Collection<ReferenceMetadata> references = preparedSchema.getReferences().values();
        for (TableMetadata table : tables) {
            addTable(table, scriptBuilder);
        }

        for (ReferenceMetadata ref : references) {
            if (ref.getName() == null) {
                ref.computeAndSetName();
            }
            addReference(ref, scriptBuilder);
        }

        return scriptBuilder.toString();
    }

    public String processMigration(VersionDTO from, VersionDTO to) {
        if (from == null || to == null) {
            throw new ApplicationException("From and To versions cannot be null");
        }
        if (from.isWorkingCopy() || to.isWorkingCopy()) {
            throw new ApplicationException("error.version.generating-on-working-copy");
        }

        SchemaStateMetadata preparedFromSchema = prepareSchema(from.getCurrentState());
        SchemaStateMetadata preparedToSchema = prepareSchema(to.getCurrentState());
        List<DifferenceProcessor.GenericSchemaChanges<?>> changes = differenceProcessor.calculateDifference(preparedFromSchema, preparedToSchema);
        if (changes.isEmpty()) {
            log.warn("No difference between versions {} and {} of schema {}", from.getTag(), to.getTag(), to.getSchemeId());
            return "";
        }

        ScriptFabric fabric = getFabric();
        StringBuilder scriptBuilder = new StringBuilder();
        fabric.appendHeader(scriptBuilder, to.getTag());

        for (DifferenceProcessor.GenericSchemaChanges<?> change : changes) {
            switch (change.getType()) {
                case TABLE -> applyTableChange(change, scriptBuilder);
                case REFERENCE -> applyReferenceChange(change, scriptBuilder);
                case INDEX -> applyIndexChange(change, scriptBuilder);
                case COLUMN -> applyColumnCommand(change, scriptBuilder);
            }
        }

        return scriptBuilder.toString();
    }

    private void addReference(ReferenceMetadata ref,
                              StringBuilder scriptBuilder) {
        TableMetadata baseTable = ref.getBaseTable();
        TableMetadata referencedTable = ref.getReferencedTable();

        String[] baseColumnNames = Arrays.stream(ref.getBaseColumns())
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);
        String[] referencedColumnNames = Arrays.stream(ref.getReferencedColumns())
                .map(ColumnMetadata::getName)
                .toArray(String[]::new);

        ScriptFabric fabric = getFabric();
        fabric.appendReferenceDefinition(scriptBuilder,
                ref.getName(),
                baseTable,
                referencedTable,
                baseColumnNames,
                referencedColumnNames,
                ref.getOnDeleteAction(),
                ref.getOnUpdateAction());
    }

    private SchemaStateMetadata prepareSchema(SchemaStateMetadata schema) {
        var clone = schema.clone();

        Collection<ReferenceMetadata> references = schema.getReferences().values();

        // Обработка M-M связей
        for (ReferenceMetadata ref : references) {
            if (ref.getType() != ReferenceMetadata.ReferenceType.MANY_TO_MANY) {
                continue;
            }
            clone.removeReference(ref.getKey());
            addMtmRefToSchema(clone, ref);
        }

        // Разворот 1-M связей
        for (ReferenceMetadata ref : references) {
            if (ref.getType() != ReferenceMetadata.ReferenceType.ONE_TO_MANY) {
                continue;
            }
            clone.removeReference(ref.getKey());
            clone.addReference(rotateOtmReference(ref, clone));
        }

        return clone;
    }

    private void addMtmRefToSchema(SchemaStateMetadata metadata,
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

        for (ColumnMetadata column : concatMtmCols) {
            column.setTable(mtmTable);
            column.setTableId(mtmTable.getId());
            column.setSchema(metadata);
        }
        metadata.addTable(mtmTable);

        ReferenceMetadata ftmRef = buildRef(mtmTable, fromTable, mtmFrom, fromCols, metadata);
        ReferenceMetadata mttRef = buildRef(mtmTable, toTable, mtmTo, toCols, metadata);
        ftmRef.computeAndSetName();
        mttRef.computeAndSetName();

        metadata.addReference(ftmRef);
        metadata.addReference(mttRef);
    }

    private ReferenceMetadata buildRef(TableMetadata fromTable,
                                       TableMetadata toTable,
                                       ColumnMetadata[] fromCols,
                                       ColumnMetadata[] toCols,
                                       SchemaStateMetadata metadata) {
        var ref = ReferenceMetadata.builder()
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
        ref.computeAndSetName();

        return ref;
    }

    protected static String computeMtmTableName(TableMetadata fromTable, TableMetadata toTable) {
        return String.format("mtm_%s_%s", fromTable.getName(), toTable.getName());
    }

    private ReferenceMetadata rotateOtmReference(ReferenceMetadata otmRef, SchemaStateMetadata metadata) {
        ReferenceMetadata ref =  ReferenceMetadata.builder()
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

        ref.computeAndSetName();
        return ref;
    }

    protected ColumnMetadata cloneColumn(TableMetadata table, ColumnMetadata origin) {
        return ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name(table.getName() + "_" + origin.getName())
                .columnType(origin.getColumnType())
                .scale(origin.getScale())
                .precision(origin.getPrecision())
                .defaultValue(origin.getDefaultValue())
                .build();
    }

    private void applyColumnCommand(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {

    }

    private void applyIndexChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {

    }

    private void applyReferenceChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        ReferenceMetadata ref = (ReferenceMetadata) change.getOne();

        switch (change.differenceType()) {
            case ADD -> addReference(ref, scriptBuilder);
            case DROP -> dropReference(ref, scriptBuilder);
            default -> throw new IllegalStateException("Unexpected difference type for reference: "
                    + change.differenceType());
        }
    }

    private void applyTableChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        TableMetadata fromTable = change.from() != null ? (TableMetadata) change.from() : null;
        TableMetadata toTable = change.to() != null ? (TableMetadata) change.to() : null;

        switch (change.differenceType()) {
            case ADD -> addTable(toTable, scriptBuilder);
            case DROP -> dropTable(fromTable, scriptBuilder);
            case UPDATE -> updateTable(toTable, scriptBuilder);
            case RENAME -> renameTable(fromTable, toTable, scriptBuilder);
        }
    }

    private void dropReference(ReferenceMetadata ref, StringBuilder scriptBuilder) {
        ScriptFabric fabric = getFabric();
        fabric.appendDropFK(ref, scriptBuilder);
    }

    private void renameTable(TableMetadata fromTable, TableMetadata toTable, StringBuilder scriptBuilder) {
        ScriptFabric fabric = getFabric();
        fabric.appendRenameTable(scriptBuilder, fromTable, toTable);
    }

    private void updateTable(TableMetadata toTable, StringBuilder scriptBuilder) {
        ScriptFabric fabric = getFabric();
        fabric.appendDropPkConstraint(scriptBuilder, toTable);
        fabric.appendAndPkConstraint(scriptBuilder, toTable);
    }

    private void dropTable(TableMetadata fromTable, StringBuilder scriptBuilder) {
        ScriptFabric fabric = getFabric();
        fabric.appendDropTable(scriptBuilder, fromTable);
    }

    private void addTable(TableMetadata toTable, StringBuilder scriptBuilder) {
        ScriptFabric fabric = getFabric();
        fabric.addTable(scriptBuilder, toTable, (builder) -> {
            onEndTableDefinition(builder, toTable);
        });
    }

    public abstract boolean supports(ScriptType type);
    protected abstract ScriptFabric getFabric();
    protected abstract void onEndTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
}
