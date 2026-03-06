import type { Table, Reference, Column, Index, ReferenceKey } from "./SchemaElements";

export type EventType = 'SCHEMA_UPDATE' | 'USER_CONNECTED' | 'SCHEMA_NEW_VERSION';

export interface SchemaChangedEvent<T> {
    eventType: EventType;
    type: T;
}

export interface CommandEvent extends SchemaChangedEvent<MetadataCommandProcessResult> {
    eventType: 'SCHEMA_UPDATE';
}

export interface MetadataCommandProcessResult {
    version: number;
    difference: SchemaDifference;
}

export interface SchemaDifference {
    upsertedTables: Table[];
    upsertedReferences: Reference[];
    upsertedColumns: Record<string, Column[]>; 
    upsertedIndexes: Record<string, Index[]>;      
    deletedTables: string[]; 
    deletedColumns: Record<string, string[]>; 
    deletedIndexes: Record<string, string[]>; 
    deletedReferences: ReferenceKey[];
}