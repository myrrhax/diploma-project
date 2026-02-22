import type { ColumnType, ConstraintType } from "./SchemaElements";

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
    precision: number | null;
    scale: number | null;
    length: number | null;
    defaultValue: string | null;
    constraints: ConstraintType[] | null;
}

export interface AddReferenceCommand extends BaseMetadataCommand { type: 'add-ref'; }
export interface UpdateColumnCommand extends BaseMetadataCommand { type: 'update-column'; /* поля */ }
export interface UpdateTableCommand extends BaseMetadataCommand { type: 'update-table'; /* поля */ }
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