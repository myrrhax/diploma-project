package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
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
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class UpdateTableCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    private String newTableName;
    private String newDescription;
    private List<UUID> newPrimaryKeyParts;
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
                throw new ApplicationException(ErrorMessageKey.TABLE_DUPLICATE.getKey());
            }
            clone.setName(newTableName);
        }
        clone.setDescription(newDescription);

        List<UUID> oldPk = table.getPrimaryKeyParts()
                .stream()
                .toList();
        SchemaDifference diff = new SchemaDifference();

        if (newPrimaryKeyParts != null
                && !newPrimaryKeyParts.isEmpty()
                && !MetadataTypeUtils.isFullEquals(oldPk, newPrimaryKeyParts)) {
            if (!newPrimaryKeyParts.stream().allMatch(kp -> table.getColumn(kp).isPresent())) {
                throw new ApplicationException(ErrorMessageKey.TABLE_PK_ERROR.getKey());
            }
            // Если ключ до этого был установлен, пересчитываем связи
            if (!oldPk.isEmpty()) {
                log.info("Recalculating primary keys for table {}", tableId);
                for (ReferenceMetadata.ReferenceKey ref : metadata.getReferences().keySet()) {
                    ReferenceMetadata.ReferenceType type = metadata.getReferences().get(ref).getType();
                    if (!MetadataTypeUtils.isRefValid(metadata, ref, type)) {
                        diff.removeReference(ref);
                        metadata.removeReference(ref);
                    }
                }
            }
            clone.setPrimaryKeyParts(newPrimaryKeyParts.stream().toList());
        }
        metadata.updateTable(clone);
        diff.upsertTable(clone);
        log.info("Table {} was updated", tableId);

        return diff;
    }
}