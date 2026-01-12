package com.github.myrrhax.diploma_project.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReferenceMetadata {
    private ReferenceKey key;
    private ReferenceType type;
    private OnDeleteAction onDeleteAction;
    private OnUpdateAction onUpdateAction;

    public enum ReferenceType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    public enum OnDeleteAction {
        NO_ACTION,
        RESTRICT,
        SET_NULL,
        CASCADE,
        DEFAULT
    }

    public enum OnUpdateAction {
        NO_ACTION,
        CASCADE
    }
}
