package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTableCommand extends MetadataCommand {
    @NotBlank
    private String tableName;
    @NotNull
    private Double xCoord;
    @NotNull
    private Double yCoord;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        if (metadata.getTable(tableName).isPresent()) {
            throw new RuntimeException("Table already exists");
        }

        metadata.addTable(
                TableMetadata.builder()
                        .name(tableName)
                        .xCoord(xCoord)
                        .yCoord(yCoord)
                        .build()
        );
    }
}
