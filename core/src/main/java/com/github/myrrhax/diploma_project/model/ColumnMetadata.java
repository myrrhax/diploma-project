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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private ColumnType columnType;
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
    @Setter
    private boolean pkPart;
    private Double min;
    private Double max;

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
            clone.setColumnType(columnType);
            clone.setDefaultValue(defaultValue);
            clone.setPrecision(precision);
            clone.setScale(scale);
            clone.setLength(length);
            clone.setConstraints(new ArrayList<>(constraints));
            clone.setPkPart(pkPart);
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
        if (precision != null && columnType.equals(ColumnType.DECIMAL)) {
            this.precision = precision;
        }
    }

    public void setScale(Integer scale) {
        if (this.scale == null) {
            this.scale = scale;
            return;
        }
        if (scale != null && columnType.equals(ColumnType.DECIMAL)) {
            this.scale = precision;
        }
    }

    public void setAutoIncrement(Boolean autoIncrement) {
        if (this.autoIncrement == null) {
            this.autoIncrement = autoIncrement;
            return;
        }
        if (autoIncrement != null) {
            if (autoIncrement) {
                if (!MetadataTypeUtils.isValidAutoincrement(this)) {
                    throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_AUTOINCREMENT_TYPE.getKey());
                }
                if (table.getAutoIncrementedColumn() != null && !table.getAutoIncrementedColumn().equals(id)) {
                    throw new ApplicationException(ErrorMessageKey.COLUMN_DUPLICATE_AUTOINCREMENT.getKey());
                }
                if (!pkPart && !constraints.contains(ConstraintType.UNIQUE)) {
                    throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_AUTOINCREMENT.getKey());
                }
                table.setAutoIncrementedColumn(id);
            }

            this.autoIncrement = autoIncrement;
            if (!autoIncrement && !Objects.equals(table.getAutoIncrementedColumn(), id)) {
                table.setAutoIncrementedColumn(null);
            }
        }
    }

    public void setLength(Integer length) {
        if (this.length == null) {
            this.length = length;
            return;
        }

        if (length != null) {
            if (!MetadataTypeUtils.isCompactibleLengthLimitedType(this, length, defaultValue)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_LENGTH.getKey(), name);
            }
            this.length = length;
        }
    }

    public void setMin(Double min) {
        if (this.min == null) {
            this.min = min;
            return;
        }

        if (min == null) {
            this.min = null;
            return;
        }

        if (!MetadataTypeUtils.isMinMaxableType(this)) {
            return;
        }

        if (this.max != null && min >= this.max) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_MAX_VIOLATION.getKey(), min, this.max);
        }

        if (this.defaultValue != null && Double.parseDouble(this.defaultValue) < min) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_DEFAULT_MIN_VIOLATION.getKey(), this.defaultValue, min);
        }

        if (this.columnType == ColumnType.DECIMAL && this.precision != null && this.scale != null) {
            BigDecimal minValue = new BigDecimal(
                    BigInteger.TEN.pow(precision).subtract(BigInteger.ONE),
                    scale
            ).negate();

            if (minValue.compareTo(new BigDecimal(min)) < 0) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_MIN_PRECISION_VIOLATION.getKey(), min);
            }
        }

        if (this.columnType == ColumnType.NUMERIC
                && this.length != null
                && min < -(Math.pow(10, -this.length) - 1)) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_MIN_LENGTH_VIOLATION.getKey(), min, this.length);
        }

        this.min = min;
    }

    public void setMax(Double max) {
        if (this.max == null) {
            this.max = max;
            return;
        }

        if (max == null) {
            this.max = null;
            return;
        }

        if (!MetadataTypeUtils.isMinMaxableType(this)) {
            return;
        }

        if (this.min != null && max <= this.min) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_MIN_VIOLATION.getKey(), max, this.min);
        }

        if (this.defaultValue != null && Double.parseDouble(this.defaultValue) > max) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_MAX_VIOLATION.getKey(), defaultValue, max);
        }

        if (this.columnType == ColumnType.DECIMAL && this.precision != null && this.scale != null) {
            BigDecimal maxValue = new BigDecimal(
                    BigInteger.TEN.pow(precision).subtract(BigInteger.ONE),
                    scale
            );

            if (maxValue.compareTo(new BigDecimal(max)) > 0) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_MAX_PRECISION_VIOLATION.getKey(), max);
            }
        }

        if (this.columnType == ColumnType.NUMERIC
                && this.length != null
                && max > (Math.pow(10, -this.length) - 1)) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_MAX_LENGTH_VIOLATION.getKey(), max, this.length);
        }

        this.max = max;
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
