import type { Reference, ReferenceKey, Table } from "./SchemaElements";
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

export interface VersionState {
    tables: { key: string, table: Table }[];
    references: { key: ReferenceKey, ref: Reference }[];
}