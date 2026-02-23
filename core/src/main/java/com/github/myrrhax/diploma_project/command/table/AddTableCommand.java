package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.command.SchemaDifference;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class AddTableCommand extends MetadataCommand {
    private static final String DEFAULT_ID_COL = "id";

    @NotBlank
    private String tableName;
    @NotNull
    private Double x;
    @NotNull
    private Double y;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Processing AddTableCommand for schema {}", schemeId);
        if (metadata.getTable(tableName).isPresent()) {
            throw new ApplicationException(ErrorMessageKey.TABLE_DUPLICATE.getKey(), tableName);
        }

        TableMetadata table = TableMetadata.builder()
                .name(tableName)
                .x(x)
                .y(y)
                .build();
        metadata.addTable(table);
        log.info("Table {} was added to schema {}", tableName, schemeId);

        var defaultColumn = ColumnMetadata.builder()
                .tableId(table.getId())
                .type(ColumnMetadata.ColumnType.INT)
                .name(DEFAULT_ID_COL)
                .autoIncrement(true)
                .build();
        table.addColumn(defaultColumn);
        table.getPrimaryKeyParts().add(defaultColumn.getId());
        log.info("Default PK {} was added to table {} of schema {}", DEFAULT_ID_COL, tableName, schemeId);

        SchemaDifference diff = new SchemaDifference();
        diff.upsertTable(table);
        diff.upsertColumn(defaultColumn);

        return diff;
    }
}
