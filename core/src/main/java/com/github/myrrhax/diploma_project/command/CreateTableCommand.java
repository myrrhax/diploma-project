package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTableCommand extends MetadataCommand {
    @NotBlank
    private String tableName;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.addTable(
                TableMetadata.builder()
                        .name(tableName)
                        .build()
        );
    }
}
