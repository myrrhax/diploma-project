package com.github.myrrhax.diploma_project.command.reference;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class DeleteReferenceCommand extends MetadataCommand {
    @NotNull
    private ReferenceMetadata.ReferenceKey key;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        Objects.requireNonNull(metadata.getReferences().get(key));
        if (!metadata.containsReference(key)) {
            throw new RuntimeException("Reference not found");
        }
        SchemaDifference diff = new SchemaDifference();
        metadata.removeReference(key);
        diff.removeReference(key);

        return diff;
    }
}
