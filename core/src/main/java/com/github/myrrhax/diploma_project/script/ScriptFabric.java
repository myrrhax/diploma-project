package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ScriptFabric {
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

    public void addTable(StringBuilder scriptBuilder, TableMetadata table,
                         Consumer<StringBuilder> onEndTable) {
        StringBuilder indexesBuilder = new StringBuilder();
        appendTableDefinition(scriptBuilder, table);

        List<ColumnMetadata> columns = new ArrayList<>(table.getColumns().values());
        for (int i = 0; i < columns.size(); i++) {
            appendColumnDefinition(scriptBuilder, columns.get(i));
            scriptBuilder.append("\n");
        }
        onEndTable.accept(scriptBuilder);
        appendEndTablePart(scriptBuilder);
        for (IndexMetadata idx : table.getIndexes().values()) {
            appendIndexDefinition(indexesBuilder, idx);
            indexesBuilder.append("\n");
        }

        scriptBuilder.append(indexesBuilder);
    }

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

    protected abstract void appendTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
    protected abstract void appendColumnDefinition(StringBuilder scriptBuilder, ColumnMetadata column);
    protected abstract void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);
    protected abstract void appendEndTablePart(StringBuilder scriptBuilder);
    protected abstract Map<ColumnMetadata.ColumnType, String> getDefinitions();
    protected abstract String getDecimalDefinition(ColumnMetadata column);
}
