package com.github.myrrhax.diploma_project.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.myrrhax.diploma_project.command.column.AddColumnCommand;
import com.github.myrrhax.diploma_project.command.column.DeleteColumnCommand;
import com.github.myrrhax.diploma_project.command.column.UpdateColumnCommand;
import com.github.myrrhax.diploma_project.command.reference.AddReferenceCommand;
import com.github.myrrhax.diploma_project.command.reference.DeleteReferenceCommand;
import com.github.myrrhax.diploma_project.command.reference.RenameReferenceCommand;
import com.github.myrrhax.diploma_project.command.table.AddTableCommand;
import com.github.myrrhax.diploma_project.command.table.DeleteTableCommand;
import com.github.myrrhax.diploma_project.command.table.UpdateTableCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddTableCommand.class, name = "add-table"),
        @JsonSubTypes.Type(value = AddColumnCommand.class, name = "add-column"),
        @JsonSubTypes.Type(value = AddReferenceCommand.class, name = "add-ref"),
        @JsonSubTypes.Type(value = UpdateColumnCommand.class, name = "update-column"),
        @JsonSubTypes.Type(value = UpdateTableCommand.class, name = "update-table"),
        @JsonSubTypes.Type(value = DeleteColumnCommand.class, name = "delete-column"),
        @JsonSubTypes.Type(value = DeleteTableCommand.class, name = "delete-table"),
        @JsonSubTypes.Type(value = DeleteReferenceCommand.class, name = "delete-ref"),
        @JsonSubTypes.Type(value = RenameReferenceCommand.class, name = "rename-ref"),
        @JsonSubTypes.Type(value = MultiCommand.class, name = "multi"),
})
@Getter
@Setter
public abstract class MetadataCommand {
    protected UUID schemeId;

    public abstract SchemaDifference execute(SchemaStateMetadata metadata);
}
