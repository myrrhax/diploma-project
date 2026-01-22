package com.github.myrrhax.diploma_project.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateTableCommand.class, name = "create-table"),
        @JsonSubTypes.Type(value = AddColumnCommand.class, name = "add-column"),
        @JsonSubTypes.Type(value = UpdateColumnCommand.class, name = "update-column"),
        @JsonSubTypes.Type(value = UpdateTableCommand.class, name = "update-table"),
})
@Getter
@Setter
public abstract class MetadataCommand {
    private UUID schemeId;

    public abstract void execute(SchemaStateMetadata metadata);
}
