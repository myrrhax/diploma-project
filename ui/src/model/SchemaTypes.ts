import type { MetadataCommand } from "./SchemaCommands";
import type { Reference, Table } from "./SchemaElements";
import type { User } from "./User";

export interface Schema {
    id: string,
    name: string,
    creator: User,
    currentVersion: Version
}

export interface Version {
    schemeId: string,
    versionId: number,
    tag?: string,
    currentState: VersionState,
    isInitial: boolean,
    isWorkingCopy: boolean 
};

export interface MetadataCommandProcessResult {
    version: number;
    command: MetadataCommand;
}

export interface VersionState {
    tables: Record<string, Table>;
    references: Record<string, Reference>;
}