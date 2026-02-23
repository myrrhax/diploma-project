package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadata implements Cloneable {
    @Setter
    @Builder.Default
    private UUID id = UUID.randomUUID();
    @Setter
    private UUID tableId;
    private String name;
    private String description;
    @Setter
    private ColumnType type;
    @Setter
    private String defaultValue;
    private Integer precision;
    private Integer scale;
    private Integer length;

    @Setter
    @JsonIgnore
    private SchemaStateMetadata schema;

    @Setter
    @JsonIgnore
    private TableMetadata table;

    @Setter
    @Builder.Default
    private List<ConstraintType> constraints = new ArrayList<>();
    private Boolean autoIncrement;

    @Override
    public ColumnMetadata clone() {
        try {
            ColumnMetadata clone = (ColumnMetadata) super.clone();
            clone.setSchema(schema);
            clone.setTable(table);
            clone.setId(id);
            clone.setTableId(tableId);
            clone.setName(name);
            clone.setDescription(description);
            clone.setType(type);
            clone.setDefaultValue(defaultValue);
            clone.setPrecision(precision);
            clone.setScale(scale);
            clone.setLength(length);
            clone.setConstraints(new ArrayList<>(constraints));
            clone.setAutoIncrement(autoIncrement);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void setName(String name) {
        if (name != null) {
            if (name.isBlank()) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_BLANK_NAME.getKey());
            }
            this.name = name;
        }
    }

    public void setDescription(String description) {
        if (this.description == null) {
            this.description = description;
            return;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void setPrecision(Integer precision) {
        if (this.precision == null) {
            this.precision = precision;
            return;
        }
        if (precision != null && type.equals(ColumnType.DECIMAL)) {
            this.precision = precision;
        }
    }

    public void setScale(Integer scale) {
        if (this.scale == null) {
            this.scale = scale;
            return;
        }
        if (scale != null && type.equals(ColumnType.DECIMAL)) {
            this.scale = precision;
        }
    }

    public void setAutoIncrement(Boolean autoIncrement) {
        if (this.autoIncrement == null) {
            this.autoIncrement = autoIncrement;
            return;
        }
        if (autoIncrement != null) {
            if (autoIncrement && !MetadataTypeUtils.isValidAutoincrement(this)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_AUTOINCREMENT_TYPE.getKey());
            }
            if (table.getColumns().values().stream()
                    .anyMatch(ColumnMetadata::getAutoIncrement)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_DUPLICATE_AUTOINCREMENT.getKey());
            }

            if (!constraints.contains(ConstraintType.UNIQUE)
                    && (table.getPrimaryKeyParts().size() != 1
                            || !table.getPrimaryKeyParts().contains(this.getId()))) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_AUTOINCREMENT.getKey());
            }

            this.autoIncrement = autoIncrement;
        }
    }

    public void setLength(Integer length) {
        if (this.length == null) {
            this.length = length;
        }

        if (length != null && MetadataTypeUtils.isCompactibleLengthLimitedType(this, length, defaultValue)) {
            this.length = length;
        }
    }

    public enum ConstraintType {
        NOT_NULL,
        UNIQUE,
    }

    public enum ColumnType {
        SMALLINT,
        INT,
        BIGINT,
        NUMERIC,
        CHAR,
        VARCHAR,
        TEXT,
        UUID,
        FLOAT,
        DOUBLE,
        DECIMAL,
        TIME,
        DATETIME,
        TIMESTAMP,
        JSON,
        BOOLEAN,
        DATE
    }
}
