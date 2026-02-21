package com.github.myrrhax.diploma_project.command.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);
        TableMetadata clone = table.clone();

        clone.setX(x);
        clone.setY(y);
        if (newTableName != null) {
            if (metadata.containsTable(newTableName)) {
                throw new RuntimeException("Table already exists");
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
                throw new RuntimeException("Primary key must contain all columns");
            }
            // Если ключ до этого был установлен, пересчитываем связи
            if (!oldPk.isEmpty()) {
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

        return diff;
    }
}