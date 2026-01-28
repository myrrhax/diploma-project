package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class UpdateTableCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    private String newTableName;
    private String newDescription;
    private List<UUID> newPrimaryKeyParts;
    private Double xCoord;
    private Double yCoord;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        if (xCoord != null) {
            table.setXCoord(xCoord);
        }

        if (yCoord != null) {
            table.setYCoord(yCoord);
        }

        if (newTableName != null && !newTableName.isBlank() && !table.getName().equals(newTableName)) {
            if (metadata.getTable(newTableName).isPresent()) {
                throw new RuntimeException("Table with name " + newTableName + " already exists");
            }
            table.setName(newTableName);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            table.setDescription(newDescription);
        }

        List<UUID> oldPk = table.getPrimaryKeyParts()
                .stream()
                .map(ColumnMetadata::getId)
                .toList();

        if (newPrimaryKeyParts != null
                && !newPrimaryKeyParts.isEmpty()
                && !MetadataTypeUtils.isFullEquals(oldPk, newPrimaryKeyParts)) {
            if (!newPrimaryKeyParts.stream().allMatch(kp -> table.getColumn(kp).isPresent())) {
                throw new RuntimeException("Primary key must contain all columns");
            }
            // Если ключ до этого был установлен, пересчитываем связи
            if (!oldPk.isEmpty()) {
                for (ReferenceMetadata.ReferenceKey ref : metadata.getReferences().keySet()) {
                    ReferenceMetadata.ReferenceType type = metadata.getReferences().get(ref).getType();
                    if (!MetadataTypeUtils.isRefValid(metadata, ref, type)) {
                        throw new RuntimeException("Invalid reference after primary key update");
                    }
                }
            }
            table.setPrimaryKeyParts(newPrimaryKeyParts.stream()
                    .map(table::getColumn)
                    .map(Optional::orElseThrow)
                    .toList());
        }
    }
}