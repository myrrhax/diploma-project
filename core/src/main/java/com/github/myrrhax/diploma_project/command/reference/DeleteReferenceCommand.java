package com.github.myrrhax.diploma_project.command.reference;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class DeleteReferenceCommand extends MetadataCommand {
    private ReferenceMetadata.ReferenceKey key;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        Objects.requireNonNull(metadata.getReferences().get(key));

        metadata.removeReference(key);
    }
}
