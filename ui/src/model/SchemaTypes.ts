import type { Reference, ReferenceKey, Table } from "./SchemaElements";
import type { User } from "./User";

export interface Schema {
    id: string,
    name: string,
    creator: User,
    currentVersion: Version,
}

export interface Version {
    schemeId: string,
    versionId: bigint,
    tag: string | null,
    currentState: VersionState,
    isInitial: boolean,
    isWorkingCopy: boolean 
};

export interface VersionState {
    tables: Map<string, Table>;
    references: Map<ReferenceKey, Reference>;
}