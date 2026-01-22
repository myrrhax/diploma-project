package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.UUID;

@Getter
@Setter
public class DeleteColumnCommand extends MetadataCommand {
    private UUID tableId;
    private UUID columnId;

    // Удаляет колонку и очищает индексы, части первичного ключа и связи
    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.getTable(tableId).ifPresent(table -> {
            table.getColumn(columnId).ifPresent(column -> {
                table.removeColumn(column);
                table.getIndexes().removeIf(idx -> idx.getColumnIds()
                        .contains(columnId));
                table.getPrimaryKeyParts().removeIf(pk ->
                        pk.getId().equals(columnId));
                metadata.getReferences().entrySet()
                        .removeIf(ref -> {
                            var key = ref.getKey();
                            return key.getFromTableId().equals(tableId)
                                    && Arrays.stream(key.getFromColumns())
                                        .anyMatch(fc -> fc.equals(columnId))
                                || key.getToTableId().equals(tableId)
                                    && Arrays.stream(key.getToColumns()).anyMatch(fc -> fc.equals(columnId));
                        });
            });
        });
    }
}
