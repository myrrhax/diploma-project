package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class UpdateTableCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    private String newTableName;
    private String newDescription;
    private Double x;
    private Double y;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElseThrow(() -> {
            log.info("Table with id {} was not found", tableId);
            return new ApplicationException(ErrorMessageKey.TABLE_NOT_FOUND.getKey());
        });
        TableMetadata clone = table.clone();

        clone.setX(x);
        clone.setY(y);
        if (newTableName != null) {
            if (metadata.containsTable(newTableName)) {
                throw new ApplicationException(ErrorMessageKey.TABLE_DUPLICATE.getKey(), newTableName);
            }
            clone.setName(newTableName);
        }
        clone.setDescription(newDescription);

        SchemaDifference diff = new SchemaDifference();
        metadata.updateTable(clone);
        diff.upsertTable(clone);
        log.info("Table {} was updated", tableId);

        return diff;
    }
}