package com.github.myrrhax.diploma_project.command.index;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeleteIndexCommand extends MetadataCommand {
    @NotNull
    private UUID indexId;
    @NotNull
    private UUID tableId;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElseThrow(() ->
                new ApplicationException("error.table.notfound"));
        IndexMetadata index = table.getIndex(indexId).orElseThrow(() ->
                new ApplicationException("error.indexes.notfound"));

        return table.removeIndex(index);
    }
}
