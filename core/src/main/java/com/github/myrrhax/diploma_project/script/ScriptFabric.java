package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;

import java.util.Map;

public interface ScriptFabric {
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
