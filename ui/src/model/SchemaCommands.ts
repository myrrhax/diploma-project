import type { ColumnType, ConstraintType, IndexType, OnDeleteAction, OnUpdateAction, ReferenceKey, ReferenceType } from "./SchemaElements";

export interface BaseMetadataCommand {
    schemeId: string;
}

export interface AddTableCommand extends BaseMetadataCommand {
    type: 'add-table';
    tableName: string;
    x: number;
    y: number;
}

export interface AddColumnCommand extends BaseMetadataCommand {
    type: 'add-column';
    tableId: string;
    name: string;
    columnType: ColumnType;
    description: string | null;
    precision: number | null;
    scale: number | null;
    length: number | null;
    defaultValue: string | null;
    constraints: ConstraintType[] | null;
    pkPart: boolean | null;
    autoIncrement: boolean | null;
}
export interface UpdateTableCommand extends BaseMetadataCommand { 
    type: 'update-table';
    tableId: string;
    newTableName?: string | null;
    newDescription?: string | null;
    newPrimaryKeyParts?: string[] | null;
    x?: number | null;
    y?: number | null;
 }

export interface AddReferenceCommand extends BaseMetadataCommand {
    type: 'add-ref';
    referenceKey: ReferenceKey;
    referenceType: ReferenceType;
    deleteAction: OnDeleteAction;
    updateAction: OnUpdateAction;
}

export interface UpdateColumnCommand extends BaseMetadataCommand {
    type: 'update-column';
    tableId: string;
    columnId: string;
    newColumnName: string | null;
    newDefaultValue: string | null;
    newDescription: string | null;
    newColumnType: ColumnType | null;
    newPrecision: number | null;
    newScale: number | null;
    newLength: number | null;
    constraints: ConstraintType[] | null;
    pkPart?: boolean | null;
    autoIncrement: boolean | null;
    min: number | null;
    max: number | null;
}

export interface DeleteColumnCommand extends BaseMetadataCommand {
    type: 'delete-column';
    tableId: string;
    columnId: string;    
}

export interface DeleteTableCommand extends BaseMetadataCommand { 
    type: 'delete-table';
    tableId: string;     
}

export interface DeleteReferenceCommand extends BaseMetadataCommand { 
    type: 'delete-ref';
    key: ReferenceKey;
}

export interface RenameReferenceCommand extends BaseMetadataCommand {
    type: 'rename-ref';
    key: ReferenceKey;
    newName: string;
}

export interface MultiCommand extends BaseMetadataCommand {
    type: 'multi',
    commands: MetadataCommand[]
}

export interface AddIndexCommand extends BaseMetadataCommand {
    type: 'add-index',
    tableId: string,
    affectedColumns: string[],
    indexName?: string,
    isUnique: boolean,
    indexType: IndexType
}

export type MetadataCommand = 
    | AddTableCommand
    | AddColumnCommand
    | AddReferenceCommand
    | UpdateColumnCommand
    | UpdateTableCommand
    | DeleteColumnCommand
    | DeleteTableCommand
    | DeleteReferenceCommand
    | RenameReferenceCommand
    | MultiCommand
    | AddIndexCommand;