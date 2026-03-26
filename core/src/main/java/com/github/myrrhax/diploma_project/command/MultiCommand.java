package com.github.myrrhax.diploma_project.command;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public class MultiCommand extends MetadataCommand {
    @NotEmpty
    private List<MetadataCommand> commands;

    @Override
    public SchemaDifference execute(SchemaStateMetadata metadata) {
        log.info("Executing MultiCommand for schema {}", metadata.getSchemaId());
        SchemaDifference difference = new SchemaDifference();

        for (MetadataCommand command : commands) {
            try {
                SchemaDifference localDiff = command.execute(metadata);

                difference.applyDifference(localDiff);
            } catch (Exception e) {
                log.error("Failed to execute command {} for schema {}",
                        command.getClass().getSimpleName(),
                        metadata.getSchemaId(),
                        e);
            }
        }

        return difference;
    }
}
