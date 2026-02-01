package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

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
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        ColumnMetadata column = table.getColumn(columnId).orElse(null);
        Objects.requireNonNull(column);

        ColumnMetadata clone = column.clone();

        if (newColumnName != null && !newColumnName.isBlank()) {
            if (table.getColumn(newColumnName).isPresent()) {
                throw new RuntimeException("Column with name " + newColumnName + " already exists");
            }
            clone.setName(newColumnName);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            clone.setDescription(newDescription);
        }
        if (newLength != null && MetadataTypeUtils.isCompactibleLengthLimitedType(clone, newLength, newDefaultValue)) {
            clone.setLength(newLength);
        }
        if (column.getType() == ColumnMetadata.ColumnType.DECIMAL
                && newScale != null || newPrecision != null
                && MetadataTypeUtils.isCompactibleDecimal(newPrecision, newScale, clone)) {
            if (newScale != null) {
                clone.setScale(newScale);
            }
            if (newPrecision != null) {
                clone.setPrecision(newPrecision);
            }
        }
        if (newDefaultValue != null && MetadataTypeUtils.isCompatibleDefaultValue(newDefaultValue, clone, newLength)) {
            clone.setDefaultValue(newDefaultValue);
        }
        if (constraints != null) {
            clone.setConstraints(constraints);
        }
        if (additionalComponents != null) {
            additionalComponents.forEach(it -> {
                if (it == ColumnMetadata.AdditionalComponent.AUTO_INCREMENT
                    && MetadataTypeUtils.isValidAutoincrement(clone)) {
                    clone.getAdditions().add(it);
                }
            });
        }

        table.updateColumn(clone);
    }
}
