package com.github.myrrhax.diploma_project.command.reference;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
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
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing add reference command for schema: {}", schemeId);
        try {
            Objects.requireNonNull(referenceKey.getFromTableId());
            Objects.requireNonNull(referenceKey.getToTableId());
            Objects.requireNonNull(referenceKey.getFromColumns());
            Objects.requireNonNull(referenceKey.getToColumns());
        } catch (Exception e) {
            throw new ApplicationException(ErrorMessageKey.REFERENCE_INVALID_KEY.getKey());
        }

        ReferenceMetadata reference = ReferenceMetadata.builder()
                .type(referenceType)
                .key(referenceKey)
                .onDeleteAction(deleteAction == null ? ReferenceMetadata.OnDeleteAction.NO_ACTION : deleteAction)
                .onUpdateAction(updateAction == null ? ReferenceMetadata.OnUpdateAction.NO_ACTION : updateAction)
                .schemaState(metadata)
                .build();

        if (!reference.checkIsRefValid()) {
            throw new ApplicationException(ErrorMessageKey.REFERENCE_INVALID_REF.getKey());
        }

        if (metadata.checkDuplicate(reference)) {
            throw new ApplicationException(ErrorMessageKey.REFERENCE_DUPLICATE_REF_PART.getKey());
        }

        reference.computeAndSetName();
        metadata.addReference(reference);
        log.info("New reference was added to schema {}", schemeId);
        SchemaDifference diff = new SchemaDifference();
        diff.upsertReference(reference);

        return diff;
    }
}
