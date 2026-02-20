package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
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
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElseThrow();
        metadata.removeTable(table);
        SchemaDifference diff = new SchemaDifference();
        diff.removeTable(tableId);

        SchemaDifference refDiff = metadata.deleteHangingReferences(table, true);
        diff.applyDifference(refDiff);

        return diff;
    }
}
