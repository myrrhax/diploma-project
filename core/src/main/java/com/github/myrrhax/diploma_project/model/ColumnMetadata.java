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
public class ColumnMetadata {
    @Builder.Default
    private UUID id = UUID.randomUUID();
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
