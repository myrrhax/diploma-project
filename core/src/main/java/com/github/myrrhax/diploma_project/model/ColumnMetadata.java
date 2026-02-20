package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadata implements Cloneable {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private UUID tableId;
    private String name;
    private String description;
    private ColumnType type;
    private String defaultValue;
    private int precision;
    private int scale;
    private int length;

    @Builder.Default
    private List<ConstraintType> constraints = new ArrayList<>();

    @Builder.Default
    private List<AdditionalComponent> additions = new ArrayList<>();

    @Override
    public ColumnMetadata clone() {
        try {
            ColumnMetadata clone = (ColumnMetadata) super.clone();
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
            clone.setAdditions(new ArrayList<>(additions));
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public enum ConstraintType {
        NOT_NULL,
        UNIQUE,
    }

    public enum AdditionalComponent {
        AUTO_INCREMENT
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
