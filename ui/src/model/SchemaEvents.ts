import type { MetadataCommandProcessResult } from "./SchemaTypes";

export type EventType = 'SCHEMA_UPDATE' | 'USER_CONNECTED';

export interface SchemaChangedEvent<T> {
    eventType: EventType;
    type: T;
}

export interface CommandEvent extends SchemaChangedEvent<MetadataCommandProcessResult> {
    eventType: 'SCHEMA_UPDATE';
}