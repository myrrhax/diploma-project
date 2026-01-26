package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
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

    @Override
    public void execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        if (newTableName != null && !newTableName.isBlank() && !table.getName().equals(newTableName)) {
            if (metadata.getTable(newTableName).isPresent()) {
                throw new RuntimeException("Table with name " + newTableName + " already exists");
            }
            table.setName(newTableName);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            table.setDescription(newDescription);
        }
        if (newPrimaryKeyParts != null
                && !newPrimaryKeyParts.isEmpty()
                && newPrimaryKeyParts.stream().allMatch(kp -> table.getColumn(kp).isPresent())) {
            table.setPrimaryKeyParts(newPrimaryKeyParts.stream()
                    .map(table::getColumn)
                    .map(Optional::get)
                    .toList());
            // ToDo Add reference invalidation
        }
    }
}