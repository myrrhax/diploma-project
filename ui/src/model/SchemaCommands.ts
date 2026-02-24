import type { ColumnType, ConstraintType, ReferenceKey, ReferenceType } from "./SchemaElements";

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
    autoIncrement: boolean | null;
}

export interface DeleteColumnCommand extends BaseMetadataCommand { type: 'delete-column'; /* поля */ }
export interface DeleteTableCommand extends BaseMetadataCommand { type: 'delete-table'; /* поля */ }
export interface DeleteReferenceCommand extends BaseMetadataCommand { type: 'delete-ref'; /* поля */ }

export type MetadataCommand = 
    | AddTableCommand
    | AddColumnCommand
    | AddReferenceCommand
    | UpdateColumnCommand
    | UpdateTableCommand
    | DeleteColumnCommand
    | DeleteTableCommand
    | DeleteReferenceCommand;