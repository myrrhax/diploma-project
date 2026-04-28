import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { type Reference } from '@/model/SchemaElements';
import { refKeyToString } from '@/utils/UtilFunctions';
import type { ContextMenuData } from './SchemaDetailsContent';
import { erStore } from '@/store/ERStore';
import { eventsStore } from '@/store/EventsStore';

interface Props {
    reference: Reference;
    onContextMenu: (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => void;
}

export const ReferenceDetailsNode = observer(({ reference, onContextMenu }: Props) => {
    const keyStr = refKeyToString(reference.key);
    const [isEditing, setIsEditing] = useState(false);
    const [editName, setEditName] = useState(reference.name || '');

    const startEditing = () => {
        setIsEditing(true);
        setEditName(reference.name || '');
    };

    const handleSave = () => {
        if (isEditing) {
            setIsEditing(false);
            if (editName.trim().length === 0) {
                eventsStore.addWarn('Имя связи не может быть пустым');
                return;
            }
            if (editName.trim() !== reference.name) {
                erStore.renameReference(reference.key, editName);
            }
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSave();
        } else if (e.key === 'Escape') {
            setIsEditing(false);
            setEditName(reference.name || '');
        }
    };

    return (
        <div 
            className="tree-node-leaf ref-leaf"
            onDoubleClick={startEditing}
            onContextMenu={(e) => onContextMenu(e, { 
                type: 'reference', 
                id: keyStr, 
                refKeyStr: keyStr,
                onRename: startEditing 
            })}
        >
            <div className="leaf-main-info">
                <span className="tree-node-icon ref-icon">🔗</span>
                <div className="ref-details">
                    {isEditing ? (
                        <input
                            autoFocus
                            className="inline-edit-input"
                            value={editName}
                            onChange={(e) => setEditName(e.target.value)}
                            onBlur={handleSave}
                            onKeyDown={handleKeyDown}
                            onClick={(e) => e.stopPropagation()}
                        />
                        ) : (
                            <span className="ref-table">{reference.name || 'Без имени'}</span>
                        )}
                </div>
            </div>
            <span className="tree-node-type">{reference.type}</span>
        </div>
    );
});