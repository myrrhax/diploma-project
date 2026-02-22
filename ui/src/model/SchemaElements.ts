export interface Table {
    id: string;
    name: string;
    description?: string;
    x: number;
    y: number;
    primaryKeyParts: string[];
    columns: Record<string, Column>;
    indexes: Record<string, Index>;
}

export interface Column {
    id: string;
    name: string;
    description: string | null;
    columnType: ColumnType;
    defaultValue: string | null;
    precision: number | null;
    scale: number | null;
    length: number | null;
    constraints: ConstraintType[] | null;
    autoIncrement: boolean | null;
}

export interface Index {
    id: string;
    columnIds: string[];
    indexType: IndexType;
    indexName?: string;
    isUnique: boolean;
}

export interface Reference {
    key: ReferenceKey;
    type: ReferenceType;
    onDeleteAction?: OnDeleteAction;
    onUpdateAction?: OnUpdateAction;
}

export interface ReferenceKey {
    fromTableId: string;
    fromColumns: string[];
    toTableId: string;
    toColumns: string[];
    name?: string;
}

export type ReferenceType = 'ONE_TO_ONE' | 'ONE_TO_MANY' | 'MANY_TO_ONE' | 'MANY_TO_MANY';

export type OnDeleteAction = 'NO_ACTION' | 'RESTRICT' | 'SET_NULL' | 'CASCADE' | 'DEFAULT';
export type OnUpdateAction = 'NO_ACTION' | 'CASCADE';

export type ColumnType = 'SMALLINT' | 'INT' | 'BIGINT' 
    | 'NUMERIC' | 'CHAR' | 'VARCHAR' 
    | 'TEXT' | 'UUID' | 'FLOAT'
    | 'DOUBLE' | 'DECIMAL' | 'TIME'
    | 'TIMESTAMP' | 'DATETIME' | 'JSON'
    | 'BOOLEAN' | 'DATE';

export type ConstraintType = 'NOT_NULL' | 'UNIQUE';

export type AdditionType = 'AUTO_INCREMENT';

export type IndexType = 'B_TREE' | 'HASH';