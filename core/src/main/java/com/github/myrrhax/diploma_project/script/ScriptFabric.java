package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.Map;

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

    public abstract void appendHeader(StringBuilder scriptBuilder, String name);
    public abstract void appendTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
    public abstract void appendColumnDefinition(StringBuilder scriptBuilder, ColumnMetadata column);
    public abstract void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);
    public abstract void appendEndTablePadding(StringBuilder scriptBuilder);
    public abstract void appendReferenceDefinition(StringBuilder scriptBuilder,
                                            String refName,
                                            TableMetadata baseTable,
                                            TableMetadata referencedTable,
                                            String[] baseColumnNames,
                                            String[] referencedColumnNames,
                                            ReferenceMetadata.OnDeleteAction onDeleteAction,
                                            ReferenceMetadata.OnUpdateAction onUpdateAction);
    public abstract Map<ColumnMetadata.ColumnType, String> getDefinitions();
    public abstract String getDecimalDefinition(ColumnMetadata column);
}
