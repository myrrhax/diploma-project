package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class MigrationProcessor {
    protected final DifferenceProcessor differenceProcessor;

    public String process(VersionDTO from, VersionDTO to) {
        if (from == null || to == null) {
            throw new ApplicationException("From and To versions cannot be null");
        }
        if (from.isWorkingCopy() || to.isWorkingCopy()) {
            throw new ApplicationException("error.version.generating-on-working-copy");
        }

        List<DifferenceProcessor.GenericSchemaChanges<?>> changes = differenceProcessor.calculateDifference(from, to);
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

    private void applyColumnCommand(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {

    }

    private void applyIndexChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {

    }

    private void applyReferenceChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {

    }

    private void applyTableChange(DifferenceProcessor.GenericSchemaChanges<?> change, StringBuilder scriptBuilder) {
        TableMetadata fromTable = change.from() != null ? (TableMetadata) change.from() : null;
        TableMetadata toTable = change.to() != null ? (TableMetadata) change.to() : null;

        switch (change.differenceType()) {
            case ADD -> addTable(toTable, scriptBuilder);
            case DROP -> dropTable(fromTable, scriptBuilder);
            case UPDATE -> updateTable(fromTable, toTable, scriptBuilder);
            case RENAME -> renameTable(fromTable, toTable, scriptBuilder);
        }
    }

    private void renameTable(TableMetadata fromTable, TableMetadata toTable, StringBuilder scriptBuilder) {

    }

    private void updateTable(TableMetadata fromTable, TableMetadata toTable, StringBuilder scriptBuilder) {

    }

    private void dropTable(TableMetadata fromTable, StringBuilder scriptBuilder) {

    }

    private void addTable(TableMetadata toTable, StringBuilder scriptBuilder) {

    }

    protected abstract ScriptFabric getFabric();
    protected abstract void onEndTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
}
