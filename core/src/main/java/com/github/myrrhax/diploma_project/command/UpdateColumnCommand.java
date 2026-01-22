package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class UpdateColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotNull
    private UUID columnId;
    private String newColumnName;
    private String newDefaultValue;
    private String newDescription;
    @Positive
    private Integer newPrecision;
    @Positive
    private Integer newScale;
    @Positive
    private Integer newLength;
    private List<ColumnMetadata.ConstraintType> constraints;
    private List<ColumnMetadata.AdditionalComponent> additionalComponents;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.getTable(tableId).ifPresent(table -> {
           table.getColumn(columnId).ifPresent(column -> {
                if (newColumnName != null && !newColumnName.isBlank()) {
                    column.setName(newColumnName);
                }
                if (newDescription != null && !newDescription.isBlank()) {
                    column.setDescription(newDescription);
                }
                if (newDefaultValue != null
                        && isCompatibleDefaultValue(newDefaultValue, column)) {
                    column.setDefaultValue(newDefaultValue);
                }
                if (column.getType() == ColumnMetadata.ColumnType.DECIMAL
                    && newScale != null
                    || newPrecision != null
                    && isCompactibleDecimal(column)) {
                    if (newScale != null) {
                        column.setScale(newScale);
                    }
                    if (newPrecision != null) {
                        column.setPrecision(newPrecision);
                    }
                }
                if (constraints != null) {
                    column.setConstraints(constraints);
                }
                if (additionalComponents != null) {
                    column.setAdditions(additionalComponents);
                }
           });
        });
    }

    private boolean isCompactibleDecimal(ColumnMetadata column) {
        if (newPrecision != null) {
            return newPrecision > Objects.requireNonNullElseGet(newScale, column::getScale);
        }
        return newScale > column.getPrecision();
    }

    private boolean isCompatibleDefaultValue(String defaultValue, ColumnMetadata column) {
        if (defaultValue == null) return true;
        try {
            return switch (column.getType()) {
                case SMALLINT -> {
                    Short.parseShort(defaultValue);
                    yield true;
                }
                case INT -> {
                    Integer.parseInt(defaultValue);
                    yield true;
                }
                case BIGINT -> {
                    Long.parseLong(defaultValue);
                    yield true;
                }
                case FLOAT -> {
                    Float.parseFloat(defaultValue);
                    yield true;
                }
                case DOUBLE -> {
                    Double.parseDouble(defaultValue);
                    yield true;
                }
                case CHAR -> {
                    int len = column.getLength();
                    yield defaultValue.length() == len || defaultValue.length() == newLength;
                }
                case BOOLEAN -> {
                    Boolean.parseBoolean(defaultValue);
                    yield true;
                }
                case DATE -> {
                    if (defaultValue.equals("now"))
                        yield true;

                    LocalDate.parse(defaultValue);
                    yield true;
                }
                case NUMERIC -> {
                    if (defaultValue.length() != column.getLength())
                        yield true;

                    new BigDecimal(defaultValue);
                    yield true;
                }
                case TIMESTAMP ->  {
                    if (defaultValue.equals("now")) {
                        yield true;
                    } else {
                        Instant.parse(defaultValue);
                    }
                    yield true;
                }
                default -> true;
            };
        } catch (Exception e) {
            return false;
        }
    }
}
