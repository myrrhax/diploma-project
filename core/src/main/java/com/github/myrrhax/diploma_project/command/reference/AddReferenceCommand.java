package com.github.myrrhax.diploma_project.command.reference;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class AddReferenceCommand extends MetadataCommand {
    @NotNull
    private ReferenceMetadata.ReferenceKey referenceKey;
    @NotNull
    private ReferenceMetadata.ReferenceType referenceType;

    private ReferenceMetadata.OnDeleteAction deleteAction;
    private ReferenceMetadata.OnUpdateAction updateAction;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        Objects.requireNonNull(referenceKey.getFromTableId());
        Objects.requireNonNull(referenceKey.getToTableId());
        Objects.requireNonNull(referenceKey.getFromColumns());
        Objects.requireNonNull(referenceKey.getToColumns());

        if (referenceKey.getFromColumns().length != referenceKey.getToColumns().length
            || !MetadataTypeUtils.isRefValid(metadata, referenceKey, referenceType)
        ) {
            throw new RuntimeException("Invalid reference");
        }

        metadata.addReference(ReferenceMetadata.builder()
                .type(referenceType)
                .key(referenceKey)
                .onDeleteAction(deleteAction == null ? ReferenceMetadata.OnDeleteAction.NO_ACTION : deleteAction)
                .onUpdateAction(updateAction == null ? ReferenceMetadata.OnUpdateAction.NO_ACTION : updateAction)
                .build());
    }
}
