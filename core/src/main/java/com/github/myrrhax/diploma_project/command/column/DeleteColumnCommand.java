package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Getter
@Setter
public class DeleteColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotNull
    private UUID columnId;

    // Удаляет колонку и очищает индексы, части первичного ключа и связи
    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing DeleteColumnCommand for schema {}", schemeId);
        TableMetadata table = metadata.getTable(tableId)
                .orElseThrow(() -> {
                    log.info("Table {} does in schema {} not exist", tableId, schemeId);
                    return new ApplicationException(ErrorMessageKey.TABLE_NOT_FOUND.getKey());
                });
        ColumnMetadata column = table.getColumn(columnId).orElseThrow(() -> {
            log.info("Column {} does in table {} not exist", columnId, tableId);
            return new ApplicationException(ErrorMessageKey.COLUMN_NOT_FOUND.getKey());
        });

        if (table.getPrimaryKeyParts().contains(columnId) && table.getPrimaryKeyParts().size() == 1) {
            throw new ApplicationException(ErrorMessageKey.COLUMN_IS_PK.getKey());
        }

        SchemaDifference diff = new SchemaDifference();
        diff.applyDifference(table.removeColumn(column));
        log.info("Column {} was deleted from table {}", columnId, tableId);
        // Каскадное удаление связей
        var refDiff = metadata.deleteInvalidReferences(column);
        if (!refDiff.getDeletedReferences().isEmpty()) {
            log.info("References was deleted by cascade from table {}", tableId);
        }
        diff.applyDifference(refDiff);

        // ToDo каскадно удалять индексы

        return diff;
    }
}
