package com.github.myrrhax.diploma_project.command.table;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTableCommand extends MetadataCommand {
    @NotBlank
    private String tableName;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        if (metadata.getTable(tableName).isPresent()) {
            throw new RuntimeException("Table already exists");
        }

        metadata.addTable(
                TableMetadata.builder()
                        .name(tableName)
                        .build()
        );
    }
}
