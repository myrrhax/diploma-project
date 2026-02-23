package com.github.myrrhax.diploma_project.model.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageKey {
    TABLE_NOT_FOUND("error.table.notfound"),
    COLUMN_DUPLICATE("error.column.duplicate"),
    COLUMN_NOT_FOUND("error.column.notfound"),
    COLUMN_INVALID_LENGTH("error.column.invalid-length"),
    COLUMN_INVALID_DEFAULT("error.column.invalid-default"),
    COLUMN_INVALID_DECIMAL("error.column.invalid-decimal"),
    COLUMN_BLANK_NAME("error.column.blank-name"),
    COLUMN_INVALID_AUTOINCREMENT_TYPE("error.column.invalid-autoincrement-type"),
    COLUMN_DUPLICATE_AUTOINCREMENT("error.column.duplicate-autoincrement"),
    COLUMN_INVALID_AUTOINCREMENT("error.column.invalid-autoincrement"),
    REFERENCE_INVALID_KEY("error.reference.invalid-key"),
    REFERENCE_INVALID_REF("error.reference.invalid-ref"),;

    private final String key;

    ErrorMessageKey(String key) {
        this.key = key;
    }
}
