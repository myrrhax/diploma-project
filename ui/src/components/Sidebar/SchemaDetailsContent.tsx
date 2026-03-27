import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore } from '@/store/ERStore';
import { TableDetailsNode } from './TableDetailsNode';
import { ReferenceDetailsNode } from './ReferenceDetailsNode';
import { refKeyToString } from '@/utils/UtilFunctions';
import { OverlaySpinner } from '../SpinnerLoader/SpinnerLoader';
import './css/SchemaDetailsContent.css';
import './css/SchemaNodes.css';
import { tableModalsStore } from '@/store/TableModalsStore';
import { columnModalsStore } from '@/store/ColumnModalsStore';

export type ContextMenuData = {
    visible: boolean;
    x: number;
    y: number;
    type: 'table' | 'column' | 'index' | 'reference' | null;
    id: string;
    parentId?: string;
    refKeyStr?: string;
    onRename?: () => void;
};

export const SchemaDetailsContent = observer(() => {
    const { state, isLoading, isEditable } = erStore;
    
    const [isTablesExpanded, setTablesExpanded] = useState(true);
    const [isRefsExpanded, setRefsExpanded] = useState(true);
    
    const [contextMenu, setContextMenu] = useState<ContextMenuData>({
        visible: false, x: 0, y: 0, type: null, id: ''
    });

    if (isLoading || !state) {
        return <OverlaySpinner text="Загрузка структуры..." />;
    }

    const handleContextMenu = (e: React.MouseEvent, data: Omit<ContextMenuData, 'visible' | 'x' | 'y'>) => {
        e.preventDefault();
        e.stopPropagation();
        if (!isEditable) return;
        
        setContextMenu({
            visible: true,
            x: e.clientX,
            y: e.clientY,
            ...data
        });
    };

    const closeContextMenu = () => {
        setContextMenu(prev => ({ ...prev, visible: false }));
    };

    const handleDelete = () => {
        if (contextMenu.type === 'table') {
            tableModalsStore.openForTable(contextMenu.id);
        } else if (contextMenu.type === 'column' && contextMenu.parentId) {
            columnModalsStore.open(contextMenu.parentId, contextMenu.id);
        } else if (contextMenu.type === 'reference' && contextMenu.refKeyStr) {
            erStore.deleteReference(contextMenu.refKeyStr);
        } else if (contextMenu.type === 'index') {
            erStore.deleteIndex(contextMenu.id, contextMenu.parentId!!);
        }
        closeContextMenu();
    };

    const handleAddTable = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (!isEditable) return;
        erStore.contextMenu.screenX = window.innerWidth / 2;
        erStore.contextMenu.screenY = window.innerHeight / 2;
        erStore.addTable();
    };

    const tables = Object.values(state.tables);
    const references = Object.values(state.references);

    return (
        <div className="schema-details-wrapper" onClick={closeContextMenu}>
            
            <div className="schema-group">
                <div className="schema-group-header" onClick={() => setTablesExpanded(!isTablesExpanded)}>
                    <div className="schema-group-title">
                        <div className={`chevron ${isTablesExpanded ? 'expanded' : ''}`}>›</div>
                        <div className='group_title'>Таблицы ({tables.length})</div>
                    </div>
                    {isEditable && (
                        <button className="schema-add-btn" onClick={handleAddTable} title="Добавить таблицу">+</button>
                    )}
                </div>
                
                {isTablesExpanded && (
                    <div className="schema-group-content">
                        {tables.length === 0 ? (
                            <div className="schema-empty-text">Нет таблиц</div>
                        ) : (
                            tables.map(table => (
                                <TableDetailsNode 
                                    key={table.id} 
                                    table={table} 
                                    onContextMenu={handleContextMenu} 
                                    isEditable={isEditable}
                                />
                            ))
                        )}
                    </div>
                )}
            </div>

            <div className="schema-group">
                <div className="schema-group-header" onClick={() => setRefsExpanded(!isRefsExpanded)}>
                    <div className="schema-group-title">
                        <div className={`chevron ${isRefsExpanded ? 'expanded' : ''}`}>›</div>
                        <div className='group_title'>Связи ({references.length})</div>
                    </div>
                </div>
                
                {isRefsExpanded && (
                    <div className="schema-group-content">
                        {references.length === 0 ? (
                            <div className="schema-empty-text">Нет связей</div>
                        ) : (
                            references.map(ref => (
                                <ReferenceDetailsNode 
                                    key={refKeyToString(ref.key)} 
                                    reference={ref} 
                                    onContextMenu={handleContextMenu} 
                                />
                            ))
                        )}
                    </div>
                )}
            </div>

            {contextMenu.visible && (
                <div 
                    className="schema-context-menu" 
                    style={{ left: contextMenu.x, top: contextMenu.y }}
                >
                    {contextMenu.onRename && (
                        <div 
                            className="schema-ctx-item" 
                            onClick={() => {
                                contextMenu.onRename!();
                                closeContextMenu();
                            }}
                        >
                            Переименовать
                        </div>
                    )}
                    <div className="schema-ctx-item danger" onClick={handleDelete}>
                        Удалить {contextMenu.type === 'table' ? 'таблицу' : contextMenu.type === 'column' ? 'колонку' : contextMenu.type === 'index' ? 'индекс' : 'связь'}
                    </div>
                </div>
            )}
        </div>
    );
});