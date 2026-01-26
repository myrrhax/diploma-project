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

        if (newColumnName != null && !newColumnName.isBlank()) {
            if (table.getColumn(newColumnName).isPresent()) {
                throw new RuntimeException("Column with name " + newColumnName + " already exists");
            }
            column.setName(newColumnName);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            column.setDescription(newDescription);
        }
        if (newLength != null && MetadataTypeUtils.isCompactibleLengthLimitedType(column, newLength, newDefaultValue)) {
            column.setLength(newLength);
        }
        if (column.getType() == ColumnMetadata.ColumnType.DECIMAL
                && newScale != null || newPrecision != null
                && MetadataTypeUtils.isCompactibleDecimal(newPrecision, newScale, column)) {
            if (newScale != null) {
                column.setScale(newScale);
            }
            if (newPrecision != null) {
                column.setPrecision(newPrecision);
            }
        }
        if (newDefaultValue != null && MetadataTypeUtils.isCompatibleDefaultValue(newDefaultValue, column, newLength)) {
            column.setDefaultValue(newDefaultValue);
        }
        if (constraints != null) {
            column.setConstraints(constraints);
        }
        if (additionalComponents != null) {
            additionalComponents.forEach(it -> {
                if (it == ColumnMetadata.AdditionalComponent.AUTO_INCREMENT
                    && MetadataTypeUtils.isValidAutoincrement(column)) {
                    column.getAdditions().add(it);
                }
            });
        }
    }
}
