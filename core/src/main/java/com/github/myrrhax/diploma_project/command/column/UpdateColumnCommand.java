package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
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
public class UpdateColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotNull
    private UUID columnId;
    private String newColumnName;
    private String newDefaultValue;
    private String newDescription;
    private ColumnMetadata.ColumnType newColumnType;
    @Positive
    private Integer newPrecision;
    @Positive
    private Integer newScale;
    @Positive
    private Integer newLength;
    private List<ColumnMetadata.ConstraintType> constraints;
    private Boolean autoIncrement;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing UpdateColumnCommand for schema {}", schemeId);
        TableMetadata table = metadata.getTable(tableId).orElseThrow(() -> {
            log.info("Table {} not found in schema {}", tableId, schemeId);
            return new ApplicationException(ErrorMessageKey.TABLE_NOT_FOUND.getKey());
        });
        ColumnMetadata column = table.getColumn(columnId).orElseThrow(() -> {
            log.info("Column {} not found in table {}", columnId, tableId);
            return new ApplicationException(ErrorMessageKey.COLUMN_NOT_FOUND.getKey());
        });
        ColumnMetadata.ColumnType oldType = column.getType();

        ColumnMetadata clone = column.clone();
        if (newColumnType != null && newColumnType != oldType) {
            clone.setType(newColumnType);
        }
        if (newColumnName != null) {
            if (table.containsColumn(newColumnName)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_DUPLICATE.getKey(), newColumnName);
            }
            clone.setName(newColumnName);
        }
        clone.setDescription(newDescription);

        if (newDefaultValue != null) {
            if (!MetadataTypeUtils.isCompatibleDefaultValue(newDefaultValue, clone, newLength)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_DEFAULT.getKey(), clone.getName());
            }
            clone.setDefaultValue(newDefaultValue);
        }

        if (newLength != null) {
            clone.setLength(newLength);
        }

        if (clone.getType() == ColumnMetadata.ColumnType.DECIMAL) {
            if (!MetadataTypeUtils.isCompactibleDecimal(newPrecision, newScale, column)) {
                throw new ApplicationException(ErrorMessageKey.COLUMN_INVALID_DECIMAL.getKey(), column.getName());
            }
            clone.setPrecision(newPrecision);
            clone.setScale(newScale);
        }

        if (constraints != null) {
            clone.setConstraints(constraints);
        }

        clone.setAutoIncrement(autoIncrement);
        table.updateColumn(clone);

        log.info("Column {} was updated for table {}", columnId, tableId);
        SchemaDifference diff = new SchemaDifference();
        diff.upsertColumn(clone);

        if (newColumnType != null && newColumnType != oldType) { // Тип изменен
            metadata.deleteHangingReferences(column, true);
        } else if (!clone.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)
                && column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)) { // Ограничение уникальности было убрано
            SchemaDifference refDiff = metadata.deleteHangingReferences(clone, false);
            diff.applyDifference(refDiff);
        }
        log.info("Hanging references was deleted from column {}", columnId);
        return diff;
    }
}
