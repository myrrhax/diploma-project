package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
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
    private Boolean isPkPart;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing AddColumnCommand for schema {}", schemeId);
        TableMetadata table = metadata.getTable(tableId).orElseThrow(() -> {
            log.info("Table {} for schema {} not found", tableId, schemeId);
            return new ApplicationException(ErrorMessageKey.TABLE_NOT_FOUND.getKey());
        });

        if (table.containsColumn(name)) {
            log.info("Column {} is already present in table {}", name, table.getId());
            throw new ApplicationException(ErrorMessageKey.COLUMN_DUPLICATE.getKey(), name);
        }

        SchemaDifference diff = new SchemaDifference();
        var column = ColumnMetadata.builder()
                .tableId(tableId)
                .name(name)
                .columnType(columnType)
                .build();

        if (length == null && (columnType == ColumnMetadata.ColumnType.CHAR || columnType == ColumnMetadata.ColumnType.NUMERIC)) {
            log.info("Processing column of type {} must have length", columnType);
            throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_LENGTH.getKey(), name);
        }
        column.setLength(length);

        if (defaultValue != null) {
            if (!MetadataTypeUtils.isCompatibleDefaultValue(defaultValue, column, length)) {
                log.info("Invalid default value for column while processing command");
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_DEFAULT.getKey(), name);
            }
            column.setDefaultValue(defaultValue);
        }

        if (columnType == ColumnMetadata.ColumnType.DECIMAL) {
            if (precision == null
                    || scale == null
                    || !MetadataTypeUtils.isCompactibleDecimal(precision, scale, column)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_DECIMAL.getKey(), name);
            }

            column.setPrecision(precision);
            column.setScale(scale);
        }

        if (constraints != null && !constraints.isEmpty()) {
            column.setConstraints(constraints);
        }
        table.addColumn(column);
        if (isPkPart != null && isPkPart) {
            table.addPkPart(column.getId());
            metadata.deleteInvalidReferences(table);
        }
        log.info("Column {} added to table {}", name, tableId);
        diff.upsertColumn(column);

        return diff;
    }
}
