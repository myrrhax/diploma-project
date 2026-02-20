import type { Column } from "./SchemaElements";

export interface BaseMetadataCommand {
    schemeId: string;
}

export interface AddTableCommand extends BaseMetadataCommand {
    type: 'add-table';
    tableName: string;
    xCoord: number;
    yCoord: number;
}

export interface AddColumnCommand extends BaseMetadataCommand {
    type: 'add-column';
    tableId: string;
    payload?: Column; // Пример поля
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