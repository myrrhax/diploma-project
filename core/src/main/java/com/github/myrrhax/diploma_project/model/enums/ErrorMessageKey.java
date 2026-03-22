package com.github.myrrhax.diploma_project.model.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageKey {
    TABLE_NOT_FOUND("error.table.notfound"),
    TABLE_DUPLICATE("error.table.duplicate"),
    TABLE_PK_ERROR("error.table.pkerror"),
    COLUMN_IS_PK("error.column.ispk"),
    COLUMN_DUPLICATE("error.column.duplicate"),
    COLUMN_NOT_FOUND("error.column.notfound"),
    COLUMN_INVALID_LENGTH("error.column.invalid-length"),
    COLUMN_INVALID_DEFAULT("error.column.invalid-default"),
    COLUMN_INVALID_DECIMAL("error.column.invalid-decimal"),
    COLUMN_BLANK_NAME("error.column.blank-name"),
    COLUMN_PK_PART_MUST_BE_NOT_NULL("error.column.pk-part-must-be-not-null"),
    COLUMN_INVALID_AUTOINCREMENT_TYPE("error.column.invalid-autoincrement-type"),
    COLUMN_DUPLICATE_AUTOINCREMENT("error.column.duplicate-autoincrement"),
    COLUMN_INVALID_AUTOINCREMENT("error.column.invalid-autoincrement"),
    REFERENCE_INVALID_KEY("error.reference.invalid-key"),
    REFERENCE_INVALID_REF("error.reference.invalid-ref"),
    REFERENCE_NOT_FOUND("error.reference.notfound"),
    REFERENCE_DUPLICATE_REF_PART("error.reference.duplicate-ref-part"),
    REFERENCE_DUPLICATE_NAME("error.reference.duplicate-name"),
    VERSION_DUPLICATE("error.versions.duplicate"),
    VERSION_TAG_DUPLICATE("error.versions.tag.duplicate"),
    VERSION_NOT_FOUND("error.version.notfound"),
    VERSION_CANT_DELETE_WORKING_COPY("error.version.cant-delete-working-copy"),
    VERSION_NON_READONLY_READ("error.version.non-readonly-read"),
    VERSION_CANT_CHANGE_HEAD_ON_NON_WORKING_COPY("error.version.cant-change-head-on-non-working-copy"),;

    private final String key;

    ErrorMessageKey(String key) {
        this.key = key;
    }
}
