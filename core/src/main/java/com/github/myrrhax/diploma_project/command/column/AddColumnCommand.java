package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class AddColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotBlank
    private String columnName;
    @NotNull
    private ColumnMetadata.ColumnType type;
    @Positive
    private Integer precision;
    @Positive
    private Integer scale;
    @Positive
    private Integer length;
    private String defaultValue;
    private List<ColumnMetadata.ConstraintType> constraints;
    private List<ColumnMetadata.AdditionalComponent> additionalComponents;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        if (table.getColumn(columnName).isPresent()) {
            throw new RuntimeException("Duplicate column name: " + columnName);
        }

        var column = ColumnMetadata.builder()
                .name(columnName)
                .type(type)
                .build();

        if (length == null && (type == ColumnMetadata.ColumnType.CHAR || type == ColumnMetadata.ColumnType.NUMERIC)) {
            throw new RuntimeException("Char or numeric columns must have max length");
        }

        if (length != null) {
            if (!MetadataTypeUtils.isCompactibleLengthLimitedType(column, length, defaultValue)) {
                throw new RuntimeException("Incompatible length value");
            }
            column.setLength(length);
        }

        if (defaultValue != null) {
            if (!MetadataTypeUtils.isCompatibleDefaultValue(defaultValue, column, length)) {
                throw new RuntimeException("Incompatible default value");
            }
            column.setDefaultValue(defaultValue);
        }

        if (type == ColumnMetadata.ColumnType.DECIMAL) {
            if (precision == null || scale == null) {
                throw new RuntimeException("Decimal columns must have precision and scale values");
            }
            if (!MetadataTypeUtils.isCompactibleDecimal(precision, scale, column)) {
                throw new RuntimeException("Decimal columns must have precision and scale values");
            }

            column.setPrecision(precision);
            column.setScale(scale);
        }

        table.addColumn(column);
    }
}
