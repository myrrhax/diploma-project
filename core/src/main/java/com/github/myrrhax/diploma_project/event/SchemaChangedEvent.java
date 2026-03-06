package com.github.myrrhax.diploma_project.event;

import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public abstract sealed class SchemaChangedEvent<T> permits SchemaChangedEvent.CommandEvent,
                                                            SchemaChangedEvent.SchemaNewVersionEvent,
                                                            SchemaChangedEvent.SchemaVersionDeletedEvent,
                                                            SchemaChangedEvent.HeadChangedEvent {
    private final EventType eventType;
    private final T payload;

    public static final class CommandEvent extends SchemaChangedEvent<MetadataCommandProcessResult> {
        public CommandEvent(MetadataCommandProcessResult payload) {
            super(EventType.SCHEMA_UPDATE, payload);
        }
    }

    public static final class SchemaNewVersionEvent extends SchemaChangedEvent<List<VersionDTO>> {
        public SchemaNewVersionEvent(List<VersionDTO> payload) { super(EventType.SCHEMA_NEW_VERSION, payload); }
    }

    public static final class SchemaVersionDeletedEvent extends SchemaChangedEvent<List<VersionDTO>> {
        public SchemaVersionDeletedEvent(List<VersionDTO> payload) { super(EventType.SCHEMA_VERSION_DELETED, payload); }
    }

    public static final class HeadChangedEvent extends SchemaChangedEvent<VersionDTO> {
        public HeadChangedEvent(VersionDTO payload) { super(EventType.SCHEMA_HEAD_CHANGED,payload); }
    }
}
