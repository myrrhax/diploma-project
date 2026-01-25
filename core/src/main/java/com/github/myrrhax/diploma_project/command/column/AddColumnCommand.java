package com.github.myrrhax.diploma_project.command.column;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class AddColumnCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotBlank
    private String columnName;
    @NotNull
    private ColumnMetadata.ColumnType type;
    @Positive
    private Integer length;
    @Positive
    private Integer precision;
    @Positive
    private Integer scale;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        TableMetadata table = metadata.getTable(tableId).orElse(null);
        Objects.requireNonNull(table);

        table.addColumn(ColumnMetadata.builder()
                .name(columnName)
                .type(type)
                .build());
    }
}
