package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Getter
@Setter
public class DeleteTableCommand extends MetadataCommand {
    private UUID tableId;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Executing DeleteTableCommand for schema {}", schemeId);
        TableMetadata table = metadata.getTable(tableId).orElseThrow(() -> {
            log.info("Table {} is not found in schema {}", tableId, schemeId);
            return new ApplicationException(ErrorMessageKey.TABLE_NOT_FOUND.getKey());
        });
        metadata.removeTable(table);
        log.info("Table {} was deleted from schema {}", tableId, schemeId);
        SchemaDifference diff = new SchemaDifference();
        diff.removeTable(tableId);

        SchemaDifference refDiff = metadata.deleteInvalidReferences(table);
        diff.applyDifference(refDiff);
        log.info("References was cascade deleted from after deleting table {}", tableId);

        return diff;
    }
}
