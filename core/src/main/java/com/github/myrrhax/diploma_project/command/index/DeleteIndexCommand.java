package com.github.myrrhax.diploma_project.command.index;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteIndexCommand extends MetadataCommand {

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        // ToDo add implementation reference invalidation
        return null;
    }
}
