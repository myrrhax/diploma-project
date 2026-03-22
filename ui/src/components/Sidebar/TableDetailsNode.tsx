import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { type Table } from '@/model/SchemaElements';
import { ColumnDetailsNode } from './ColumnDetailsNode';
import { IndexDetailsNode } from './IndexDetailsNode';
import type { ContextMenuData } from './SchemaDetailsContent';

interface Props {
    table: Table;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
    isEditable: boolean;
}

export const TableDetailsNode = observer(({ table, onContextMenu }: Props) => {
    const [isExpanded, setIsExpanded] = useState(false);
    
    const columns = Object.values(table.columns || {});
    const indexes = Object.values(table.indexes || {});

    return (
        <div className="tree-node-container">
            <div 
                className="tree-node-header" 
                onClick={() => setIsExpanded(!isExpanded)}
                onContextMenu={(e) => onContextMenu(e, { type: 'table', id: table.id })}
            >
                <span className={`chevron ${isExpanded ? 'expanded' : ''}`}>›</span>
                <span className="tree-node-icon table-icon">▦</span>
                <span className="tree-node-title">{table.name}</span>
            </div>

            {isExpanded && (
                <div className="tree-node-children">
                    <div className="tree-node-subgroup">Колонки ({columns.length})</div>
                    {columns.map(col => (
                        <ColumnDetailsNode 
                            key={col.id} 
                            column={col} 
                            tableId={table.id} 
                            onContextMenu={onContextMenu} 
                        />
                    ))}

                    <div className="tree-node-subgroup">Индексы ({indexes.length})</div>
                    {indexes.map(idx => (
                        <IndexDetailsNode 
                            key={idx.id} 
                            index={idx} 
                            tableId={table.id} 
                            onContextMenu={onContextMenu} 
                        />
                    ))}
                </div>
            )}
        </div>
    );
});