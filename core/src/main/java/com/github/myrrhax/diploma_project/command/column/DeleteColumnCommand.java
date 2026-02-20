package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

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
        TableMetadata table = metadata.getTable(tableId).orElseThrow();
        ColumnMetadata column = table.getColumn(columnId).orElseThrow();

        SchemaDifference diff = new SchemaDifference();
        table.removeColumn(column, metadata, diff);

        return diff;
    }
}
