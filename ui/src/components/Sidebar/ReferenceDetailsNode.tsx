import React from 'react';
import { observer } from 'mobx-react-lite';
import { type Reference } from '@/model/SchemaElements';
import { refKeyToString } from '@/utils/UtilFunctions';
import type { ContextMenuData } from './SchemaDetailsContent';

interface Props {
    reference: Reference;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
}

export const ReferenceDetailsNode = observer(({ reference, onContextMenu }: Props) => {
    const keyStr = refKeyToString(reference.key);

    return (
        <div 
            className="tree-node-leaf ref-leaf"
            onContextMenu={(e) => onContextMenu(e, { type: 'reference', id: keyStr, refKeyStr: keyStr })}
        >
            <div className="leaf-main-info">
                <span className="tree-node-icon ref-icon">🔗</span>
                <div className="ref-details">
                    <span className="ref-table">{reference.name}</span>
                </div>
            </div>
            <span className="tree-node-type">{reference.type}</span>
        </div>
    );
});