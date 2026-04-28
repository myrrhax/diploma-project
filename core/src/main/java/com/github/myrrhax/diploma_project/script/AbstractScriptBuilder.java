package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractScriptBuilder {
    public static final String UQ_CONSTRAINT_PATTERN = "uq_%s_%s";
    public static final String CHECK_CONSTRAINT_PATTERN = "chk_%s_%s_min_max";

    public String getLengthLimitedType(ColumnMetadata metadata) {
        Integer length = metadata.getLength();
        if (length == null) {
            return "";
        }
        String type = getDefinitions().get(metadata.getColumnType());
        return type + "(" + length + ")";
    }

    public String getConstraintName(ColumnMetadata.ConstraintType constraint) {
        return constraint.name().replace('_', ' ');
    }

    public String parseAction(Enum<?> action) {
        String actionName = ReferenceMetadata.OnDeleteAction.NO_ACTION.name();
        if (action != null) {
            actionName = action.name();
        }
        return actionName.replace('_', ' ');
    }

    public void addTable(StringBuilder scriptBuilder,
                         TableMetadata table) {
        StringBuilder indexesBuilder = new StringBuilder();
        appendTableDefinition(scriptBuilder, table);

        List<ColumnMetadata> columns = new ArrayList<>(table.getColumns().values());
        for (ColumnMetadata column : columns) {
            appendColumnDefinition(scriptBuilder, column, false);
            scriptBuilder.append('\n');
        }
        onEndTableDefinition(scriptBuilder, table);
        appendEndTablePart(scriptBuilder);

        for (IndexMetadata idx : table.getIndexes().values()) {
            appendIndexDefinition(indexesBuilder, idx);
        }

        scriptBuilder.append(indexesBuilder);
    }

    public void updateDefaultValue(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn) {
        dropDefaultValue(scriptBuilder, newColumn);
        addDefaultValue(scriptBuilder, newColumn);
    }

    protected abstract void onEndTableDefinition(StringBuilder scriptBuilder, TableMetadata table);

    public abstract void appendPrimaryKeyDefinition(StringBuilder sqlBuilder, TableMetadata table);

    public abstract void appendReferenceDefinition(StringBuilder scriptBuilder,
                                                   String refName,
                                                   TableMetadata baseTable,
                                                   TableMetadata referencedTable,
                                                   String[] baseColumnNames,
                                                   String[] referencedColumnNames,
                                                   ReferenceMetadata.OnDeleteAction onDeleteAction,
                                                   ReferenceMetadata.OnUpdateAction onUpdateAction);

    public abstract void appendHeader(StringBuilder scriptBuilder, String name);

    public abstract void appendDropTable(StringBuilder scriptBuilder, TableMetadata fromTable);

    public abstract void appendRenameTable(StringBuilder scriptBuilder, TableMetadata from, TableMetadata to);

    public abstract void appendDropPkConstraint(StringBuilder scriptBuilder, TableMetadata toTable);

    public abstract void appendAndPkConstraint(StringBuilder scriptBuilder, TableMetadata toTable);

    public abstract void appendDropFK(ReferenceMetadata ref, StringBuilder scriptBuilder);

    public abstract void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);

    public abstract void appendDropIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx);

    public abstract void appendRenameIndexDefinition(StringBuilder scriptBuilder, IndexMetadata idx, IndexMetadata toIdx);

    public abstract void addColumnToTable(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendDropColumn(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendRenameColumn(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn);

    public abstract void appendChangeColumnType(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendNotNullConstraint(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendDropNotNull(StringBuilder scriptBuilder, ColumnMetadata fromColumn, ColumnMetadata column);

    public abstract void appendDropUnique(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn);

    protected abstract void appendTableDefinition(StringBuilder scriptBuilder, TableMetadata table);

    protected abstract void appendColumnDefinition(StringBuilder scriptBuilder, ColumnMetadata column, boolean addExisting);

    protected abstract void appendEndTablePart(StringBuilder scriptBuilder);

    protected abstract Map<ColumnMetadata.ColumnType, String> getDefinitions();

    protected abstract String getDecimalDefinition(ColumnMetadata column);

    protected abstract String getSuitableType(ColumnMetadata column);

    public abstract void appendAddUnique(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void addDefaultValue(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void dropDefaultValue(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendDropMinMax(StringBuilder scriptBuilder, ColumnMetadata oldColumn, ColumnMetadata newColumn);

    public abstract void appendMinMaxConstraint(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendDropAutoIncrement(StringBuilder scriptBuilder, ColumnMetadata column);

    public abstract void appendAddAutoIncrement(StringBuilder scriptBuilder, ColumnMetadata column);
}
