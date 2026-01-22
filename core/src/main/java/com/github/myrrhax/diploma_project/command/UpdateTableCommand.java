package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateTableCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    private String newTableName;
    private String newDescription;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.getTable(tableId).ifPresent(table -> {
            if (newTableName != null && !newTableName.isBlank()) {
                table.setName(newTableName);
            }
            if (newDescription != null && !newDescription.isBlank()) {
                table.setDescription(newDescription);
            }
        });
    }
}