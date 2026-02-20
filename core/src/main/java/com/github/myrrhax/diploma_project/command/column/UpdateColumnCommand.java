package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
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
    private Boolean autoIncrement;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElseThrow();
        ColumnMetadata column = table.getColumn(columnId).orElseThrow();
        ColumnMetadata clone = column.clone();

        if (newColumnName != null) {
            clone.setName(newColumnName);
        }
        clone.setDescription(newDescription);
        if (newDefaultValue != null
                && MetadataTypeUtils.isCompatibleDefaultValue(newDefaultValue, clone, newLength)) {
            clone.setDefaultValue(newDefaultValue);
            clone.setLength(newLength);
        }

        if (clone.getType() == ColumnMetadata.ColumnType.DECIMAL
                && MetadataTypeUtils.isCompactibleDecimal(newPrecision, newScale, column)) {
            clone.setPrecision(newPrecision);
            clone.setScale(newScale);
        }
        if (constraints != null) {
            clone.setConstraints(constraints);
        }
        clone.setAutoIncrement(autoIncrement);
        table.updateColumn(clone);

        SchemaDifference diff = new SchemaDifference();
        diff.upsertColumn(clone);

        // Ограничение уникальности было убрано
        if (!clone.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)
                && column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)) {
            SchemaDifference refDiff = metadata.deleteHangingReferences(clone, false);
            diff.applyDifference(refDiff);
        }

        return diff;
    }
}
