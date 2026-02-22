package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
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
import java.util.UUID;

@Getter
@Setter
public class AddColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotBlank
    private String name;
    @NotNull
    private ColumnMetadata.ColumnType columnType;
    @Positive
    private Integer precision;
    @Positive
    private Integer scale;
    @Positive
    private Integer length;
    private String defaultValue;
    private List<ColumnMetadata.ConstraintType> constraints;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElseThrow();
        if (table.containsColumn(name)) {
            throw new RuntimeException("Duplicate column name: " + name);
        }

        SchemaDifference diff = new SchemaDifference();
        var column = ColumnMetadata.builder()
                .tableId(tableId)
                .name(name)
                .type(columnType)
                .build();

        if (length == null && (columnType == ColumnMetadata.ColumnType.CHAR || columnType == ColumnMetadata.ColumnType.NUMERIC)) {
            throw new RuntimeException("Char or numeric columns must have max length");
        }
        column.setLength(length);
        if (defaultValue != null) {
            if (!MetadataTypeUtils.isCompatibleDefaultValue(defaultValue, column, length)) {
                throw new RuntimeException("Incompatible default value: " + defaultValue);
            }
            column.setDefaultValue(defaultValue);
        }

        if (columnType == ColumnMetadata.ColumnType.DECIMAL) {
            if (precision == null || scale == null) {
                throw new RuntimeException("Decimal columns must have precision and scale values");
            }
            if (!MetadataTypeUtils.isCompactibleDecimal(precision, scale, column)) {
                throw new RuntimeException("Decimal columns must have precision and scale values");
            }

            column.setPrecision(precision);
            column.setScale(scale);
        }

        if (constraints != null && !constraints.isEmpty()) {
            column.setConstraints(constraints);
        }

        table.addColumn(column);
        diff.upsertColumn(column);

        return diff;
    }
}
