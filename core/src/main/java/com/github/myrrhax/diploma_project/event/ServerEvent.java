package com.github.myrrhax.diploma_project.event;

import com.github.myrrhax.diploma_project.model.dto.ConnectionChangedPayload;
import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public abstract sealed class ServerEvent<T> permits ServerEvent.CommandEvent,
                                                    ServerEvent.SchemaNewVersionEvent,
                                                    ServerEvent.SchemaVersionDeletedEvent,
                                                    ServerEvent.HeadChangedEvent,
                                                    ServerEvent.ConnectionChangedEvent {
    private final EventType eventType;
    private final T payload;

    public static final class CommandEvent extends ServerEvent<MetadataCommandProcessResult> {
        public CommandEvent(MetadataCommandProcessResult payload) {
            super(EventType.SCHEMA_UPDATE, payload);
        }
    }

    public static final class SchemaNewVersionEvent extends ServerEvent<List<VersionDTO>> {
        public SchemaNewVersionEvent(List<VersionDTO> payload) { super(EventType.SCHEMA_NEW_VERSION, payload); }
    }

    public static final class SchemaVersionDeletedEvent extends ServerEvent<List<VersionDTO>> {
        public SchemaVersionDeletedEvent(List<VersionDTO> payload) { super(EventType.SCHEMA_VERSION_DELETED, payload); }
    }

    public static final class HeadChangedEvent extends ServerEvent<VersionDTO> {
        public HeadChangedEvent(VersionDTO payload) { super(EventType.SCHEMA_HEAD_CHANGED,payload); }
    }

    public static final class ConnectionChangedEvent extends ServerEvent<ConnectionChangedPayload> {
        public ConnectionChangedEvent(ConnectionChangedPayload payload) {
            super(EventType.CONNECTION_CHANGED, payload);
        }
    }
}
