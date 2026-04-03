package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.Map;

public interface FullScriptFabric {
    void appendHeader(StringBuilder scriptBuilder, SchemaStateMetadata schema, String name);
    void appendTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
    void appendColumnDefinition(StringBuilder scriptBuilder, ColumnMetadata column);
    void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);
    void appendEndTablePadding(StringBuilder scriptBuilder);
    void appendReferenceDefinition(StringBuilder scriptBuilder,
                                   String refName,
                                   TableMetadata baseTable,
                                   TableMetadata referencedTable,
                                   String[] baseColumnNames,
                                   String[] referencedColumnNames,
                                   ReferenceMetadata.OnDeleteAction onDeleteAction,
                                   ReferenceMetadata.OnUpdateAction onUpdateAction);
    Map<ColumnMetadata.ColumnType, String> getDefinitions();
    String getDecimalDefinition(ColumnMetadata column);

    default String getLengthLimitedType(ColumnMetadata metadata) {
        Integer length = metadata.getLength();
        if (length == null) {
            return "";
        }
        String type = getDefinitions().get(metadata.getColumnType());
        return type + "(" + length + ")";
    }

    default String getConstraintName(ColumnMetadata.ConstraintType constraint) {
        return constraint.name().replace('_', ' ');
    }

    default String parseAction(Enum<?> action) {
        String actionName = ReferenceMetadata.OnDeleteAction.NO_ACTION.name();
        if (action != null) {
            actionName = action.name();
        }
        return actionName.replace('_', ' ');
    }
}
