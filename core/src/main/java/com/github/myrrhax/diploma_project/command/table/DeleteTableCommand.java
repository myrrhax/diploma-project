package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeleteTableCommand extends MetadataCommand {
    private UUID tableId;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.getTable(tableId)
                .ifPresent(tableMetadata -> {
                    metadata.removeTable(tableMetadata);
                    metadata.getReferences()
                            .entrySet()
                            .removeIf(entry -> {
                                var key = entry.getKey();

                                return key.getFromTableId().equals(tableId) || key.getToTableId().equals(tableId);
                            });
                });
    }
}
