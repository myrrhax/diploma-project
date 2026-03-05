package com.github.myrrhax.diploma_project.event;

import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract sealed class SchemaChangedEvent<T> permits SchemaChangedEvent.CommandEvent,
                                                            SchemaChangedEvent.SchemaNewVersionEvent {
    private final EventType eventType;
    private final T type;

    public static final class CommandEvent extends SchemaChangedEvent<MetadataCommandProcessResult> {
        public CommandEvent(MetadataCommandProcessResult payload) {
            super(EventType.SCHEMA_UPDATE, payload);
        }
    }

    public static final class SchemaNewVersionEvent extends SchemaChangedEvent<VersionDTO> {
        public SchemaNewVersionEvent(VersionDTO payload) { super(EventType.SCHEMA_NEW_VERSION, payload); }
    }
}
