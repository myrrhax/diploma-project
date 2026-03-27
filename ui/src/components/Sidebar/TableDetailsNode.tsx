import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { type Table, type Index } from '@/model/SchemaElements';
import { ColumnDetailsNode } from './ColumnDetailsNode';
import { IndexDetailsNode } from './IndexDetailsNode';
import type { ContextMenuData } from './SchemaDetailsContent';
import { AddIndexModal } from './AddIndexModal';
import { erStore } from '@/store/ERStore';

interface Props {
    table: Table;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
    isEditable: boolean;
}

export const TableDetailsNode = observer(({ table, onContextMenu, isEditable }: Props) => {
    const [isExpanded, setIsExpanded] = useState(false);
    const [isIndexModalOpen, setIsIndexModalOpen] = useState(false);
    
    const columns = Object.values(table.columns || {});
    const indexes = Object.values(table.indexes || {});

    const handleSaveIndex = (newIndex: Index) => {
        erStore.addIndex(newIndex, table.id);
        setIsIndexModalOpen(false);
    };

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

                    <div className="tree-node-subgroup-wrapper">
                        <div className="tree-node-subgroup">Индексы ({indexes.length})</div>
                        {isEditable && (
                            <button 
                                className="tree-node-add-btn" 
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setIsIndexModalOpen(true);
                                }}
                            >
                                +
                            </button>
                        )}
                    </div>
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

            {isIndexModalOpen && (
                <AddIndexModal 
                    table={table} 
                    onClose={() => setIsIndexModalOpen(false)} 
                    onSave={handleSaveIndex} 
                />
            )}
        </div>
    );
});