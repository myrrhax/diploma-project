import type { Table, Reference, Column, Index, ReferenceKey } from "./SchemaElements";
import type { Version } from "./SchemaTypes";

export type EventType = 'SCHEMA_UPDATE' 
    | 'USER_CONNECTED' 
    | 'SCHEMA_NEW_VERSION' 
    | 'SCHEMA_VERSION_DELETED'
    | 'SCHEMA_HEAD_CHANGED';

export interface SchemaChangedEvent<T> {
    eventType: EventType;
    payload: T;
}

export interface CommandEvent extends SchemaChangedEvent<MetadataCommandProcessResult> {
    eventType: 'SCHEMA_UPDATE';
}

export interface NewVersionEvent extends SchemaChangedEvent<Version[]> {
    eventType: 'SCHEMA_NEW_VERSION';
}

export interface VersionDeletedEvent extends SchemaChangedEvent<Version[]> {
    eventType: 'SCHEMA_VERSION_DELETED';
}

export interface HeadChangedEvent extends SchemaChangedEvent<Version> {
    eventType: 'SCHEMA_HEAD_CHANGED';
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