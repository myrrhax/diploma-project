package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractScriptProcessor {
    protected final DifferenceProcessor differenceProcessor;

    public String processFullScript(String name, SchemaStateMetadata schema) {
        StringBuilder scriptBuilder = new StringBuilder();
        SchemaStateMetadata preparedSchema = prepareSchema(schema);
        AbstractScriptBuilder fabric = getFabric();
        fabric.appendHeader(scriptBuilder, name);

        Collection<TableMetadata> tables = preparedSchema.getTables().values();
        Collection<ReferenceMetadata> references = preparedSchema.getReferences().values();
        for (TableMetadata table : tables) {
            fabric.addTable(scriptBuilder, table);
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

        AbstractScriptBuilder fabric = getFabric();
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

        AbstractScriptBuilder fabric = getFabric();
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
        ColumnMetadata fromColumn = change.from() != null ? (ColumnMetadata) change.from() : null;
        ColumnMetadata toColumn = change.to() != null ? (ColumnMetadata) change.to() : null;
        AbstractScriptBuilder fabric = getFabric();

        switch (change.differenceType()) {
            case ADD -> fabric.addColumnToTable(scriptBuilder, toColumn);
            case DROP -> fabric.appendDropColumn(scriptBuilder, fromColumn);
            case RENAME -> fabric.appendRenameColumn(scriptBuilder, fromColumn, toColumn);
            case UPDATE -> {
                if (fromColumn == null || toColumn == null) {
                    throw new IllegalStateException("Cannot apply change to null columns");
                }

                boolean typeChanged = fromColumn.getColumnType() != toColumn.getColumnType()
                        || !Objects.equals(fromColumn.getLength(), toColumn.getLength())
                        || !Objects.equals(fromColumn.getPrecision(), toColumn.getPrecision())
                        || !Objects.equals(fromColumn.getScale(), toColumn.getScale());

                if (typeChanged) {
                    fabric.appendChangeColumnType(scriptBuilder, toColumn);
                }

                if (!MetadataTypeUtils.isFullEquals(fromColumn.getConstraints(), toColumn.getConstraints())) {
                    boolean fromNotNull = fromColumn.getConstraints().contains(ColumnMetadata.ConstraintType.NOT_NULL);
                    boolean toNotNull = toColumn.getConstraints().contains(ColumnMetadata.ConstraintType.NOT_NULL);

                    if (fromNotNull && !toNotNull) {
                        fabric.appendDropNotNull(scriptBuilder, fromColumn, toColumn);
                    } else if (!fromNotNull && toNotNull) {
                        fabric.appendNotNullConstraint(scriptBuilder, toColumn);
                    }

                    boolean fromUnique = fromColumn.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE);
                    boolean toUnique = toColumn.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE); // ИСПРАВЛЕНО

                    if (fromUnique && !toUnique) {
                        fabric.appendDropUnique(scriptBuilder, fromColumn, toColumn);
                    } else if (!fromUnique && toUnique) {
                        fabric.appendAddUnique(scriptBuilder, toColumn);
                    }
                }

                if (!Objects.equals(fromColumn.getDefaultValue(), toColumn.getDefaultValue())) { // ИСПРАВЛЕНО
                    if (fromColumn.getDefaultValue() == null) {
                        fabric.addDefaultValue(scriptBuilder, toColumn);
                    } else if (toColumn.getDefaultValue() == null) {
                        fabric.dropDefaultValue(scriptBuilder, toColumn);
                    } else {
                        fabric.updateDefaultValue(scriptBuilder, fromColumn, toColumn);
                    }
                }

                if (!Objects.equals(fromColumn.getMin(), toColumn.getMin())
                        || !Objects.equals(fromColumn.getMax(), toColumn.getMax())) {
                    if (fromColumn.getMin() != null || fromColumn.getMax() != null) {
                        fabric.appendDropMinMax(scriptBuilder, fromColumn, toColumn);
                    }
                    if (toColumn.getMin() != null || toColumn.getMax() != null) {
                        fabric.appendAddMinMax(scriptBuilder, fromColumn, toColumn);
                    }
                }
            }
        }
    }

    private void applyIndexChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        IndexMetadata fromIdx = change.from() != null ? (IndexMetadata) change.from() : null;
        IndexMetadata toIdx = change.to() != null ? (IndexMetadata) change.to() : null;

        AbstractScriptBuilder fabric = getFabric();
        switch (change.differenceType()) {
            case ADD -> fabric.appendIndexDefinition(scriptBuilder, toIdx);
            case DROP -> fabric.appendDropIndexDefinition(scriptBuilder, fromIdx);
            case RENAME -> fabric.appendRenameIndexDefinition(scriptBuilder, fromIdx, toIdx);
            default -> throw new IllegalStateException("Unexpected difference type for reference: "
                    + change.differenceType());
        }
    }

    private void applyReferenceChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        ReferenceMetadata ref = (ReferenceMetadata) change.getOne();
        AbstractScriptBuilder fabric = getFabric();

        switch (change.differenceType()) {
            case ADD -> addReference(ref, scriptBuilder);
            case DROP -> fabric.appendDropFK(ref, scriptBuilder);
            default -> throw new IllegalStateException("Unexpected difference type for reference: "
                    + change.differenceType());
        }
    }

    private void applyTableChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        TableMetadata fromTable = change.from() != null ? (TableMetadata) change.from() : null;
        TableMetadata toTable = change.to() != null ? (TableMetadata) change.to() : null;
        AbstractScriptBuilder fabric = getFabric();

        switch (change.differenceType()) {
            case ADD -> fabric.addTable(scriptBuilder, toTable);
            case DROP -> fabric.appendDropTable(scriptBuilder, fromTable);
            case UPDATE -> {
                fabric.appendDropPkConstraint(scriptBuilder, toTable);
                fabric.appendAndPkConstraint(scriptBuilder, toTable);
            }
            case RENAME -> fabric.appendRenameTable(scriptBuilder, fromTable, toTable);
        }
    }

    public abstract boolean supports(ScriptType type);
    protected abstract AbstractScriptBuilder getFabric();
}
