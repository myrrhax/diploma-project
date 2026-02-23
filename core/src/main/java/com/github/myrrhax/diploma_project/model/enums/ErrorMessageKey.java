package com.github.myrrhax.diploma_project.model.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageKey {
    COLUMN_DUPLICATE("error.column.duplicate"),
    TABLE_NOT_FOUND("error.table.notfound"),
    COLUMN_INVALID_LENGTH("error.column.invalid-length"),
    COLUMN_INVALID_DEFAULT("error.column.invalid-default"),
    COLUMN_INVALID_DECIMAL("error.column.invalid-decimal"),;

    private final String key;

    ErrorMessageKey(String key) {
        this.key = key;
    }
}
