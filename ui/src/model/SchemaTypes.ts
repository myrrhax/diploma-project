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
    versionedAt: Date | null;
    parentId: number;
};


export interface VersionState {
    tables: Record<string, Table>;
    references: Record<string, Reference>;
    cacheVersion: number;
}

export type ScriptType = 'MYSQL' | 'POSTGRES' | 'LIQUIBASE';
export type GenType = 'FULL' | 'MIGRATION'; 

export interface Script {
    id: string;
    version: Version;
    scriptFileId: string;
    type: ScriptType;
    generatedType: GenType;
    fromVersion: Version | null;
}