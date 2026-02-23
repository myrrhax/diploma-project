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
public class DeleteReferenceCommand extends MetadataCommand {
    @NotNull
    private ReferenceMetadata.ReferenceKey key;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing delete reference command for schema {}", schemeId);
        if (!metadata.containsReference(key)) {
            throw new ApplicationException(ErrorMessageKey.REFERENCE_NOT_FOUND.getKey());
        }
        SchemaDifference diff = new SchemaDifference();
        metadata.removeReference(key);
        log.info("Reference was deleted from schema {}", schemeId);
        diff.removeReference(key);

        return diff;
    }
}
