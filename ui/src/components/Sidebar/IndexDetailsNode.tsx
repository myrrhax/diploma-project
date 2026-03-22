import React from 'react';
import { observer } from 'mobx-react-lite';
import { type Index } from '@/model/SchemaElements';
import type { ContextMenuData } from './SchemaDetailsContent';

interface Props {
    index: Index;
    tableId: string;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
}

export const IndexDetailsNode = observer(({ index, tableId, onContextMenu }: Props) => {
    return (
        <div 
            className="tree-node-leaf"
            onContextMenu={(e) => onContextMenu(e, { type: 'index', id: index.id, parentId: tableId })}
        >
            <div className="leaf-main-info">
                <span className="tree-node-icon index-icon">⚡</span>
                <span className="tree-node-title">{index.indexName || index.indexType}</span>
                {index.isUnique && <span className="badge unique">UQ</span>}
            </div>
        </div>
    );
});