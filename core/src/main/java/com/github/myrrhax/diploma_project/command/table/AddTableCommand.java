package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTableCommand extends MetadataCommand {
    private static final String DEFAULT_ID_COL = "id";

    @NotBlank
    private String tableName;
    @NotNull
    private Double xCoord;
    @NotNull
    private Double yCoord;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        if (metadata.getTable(tableName).isPresent()) {
            throw new RuntimeException("Table already exists");
        }

        TableMetadata table = TableMetadata.builder()
                .name(tableName)
                .xCoord(xCoord)
                .yCoord(yCoord)
                .build();
        metadata.addTable(table);
        var defaultColumn = ColumnMetadata.builder()
                .tableId(table.getId())
                .type(ColumnMetadata.ColumnType.INT)
                .name(DEFAULT_ID_COL)
                .autoIncrement(true)
                .build();

        table.addColumn(defaultColumn);
        table.getPrimaryKeyParts().add(defaultColumn.getId());

        SchemaDifference diff = new SchemaDifference();
        diff.upsertTable(table);
        diff.upsertColumn(defaultColumn);

        return diff;
    }
}
