import React from 'react';
import { observer } from 'mobx-react-lite';
import { type Column } from '@/model/SchemaElements';
import type { ContextMenuData } from './SchemaDetailsContent';

interface Props {
    column: Column;
    tableId: string;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
}

export const ColumnDetailsNode = observer(({ column, tableId, onContextMenu }: Props) => {    
    let typeStr = column.columnType;
    if (column.length) typeStr += `(${column.length})`;
    else if (column.precision && column.scale) typeStr += `(${column.precision},${column.scale})`;

    return (
        <div 
            className="tree-node-leaf"
            onContextMenu={(e) => onContextMenu(e, { type: 'column', id: column.id, parentId: tableId })}
        >
            <div className="leaf-main-info">
                {column.pkPart && <span className="badge pk" title="Primary Key">PK</span>}
                <span className="tree-node-title">{column.name}</span>
            </div>
            
            <div className="leaf-badges">
                <span className="tree-node-type">{typeStr}</span>
                {column.constraints?.includes('NOT_NULL') ? (
                    <span className="badge constraint" title={'NOT NULL'}>NN</span>
                ) : null}

                {column.constraints?.includes('UNIQUE') ? (
                    <span className="badge constraint" title={'UNIQUE'}>UQ</span>
                ) : null}
            </div>
        </div>
    );
});