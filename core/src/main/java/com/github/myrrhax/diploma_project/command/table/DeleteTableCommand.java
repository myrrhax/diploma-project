package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class DeleteTableCommand extends MetadataCommand {
    private UUID tableId;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        metadata.removeTable(table);
        metadata.getReferences()
                .entrySet()
                .removeIf(entry -> {
                    var key = entry.getKey();

                    return key.getFromTableId().equals(tableId) || key.getToTableId().equals(tableId);
                });
    }
}
