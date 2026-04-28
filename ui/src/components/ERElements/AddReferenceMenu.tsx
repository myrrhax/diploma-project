import React, { useEffect, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { referenceStore } from '@/store/ReferenceStore';
import { erStore } from '@/store/ERStore';
import type { ReferenceType, OnDeleteAction, OnUpdateAction } from '@/model/SchemaElements';
import './css/AddReferenceMenu.css';

const REFERENCE_TYPE_LABELS: Record<ReferenceType, string> = {
    'MANY_TO_ONE': 'Многие к одному (M:1)',
    'ONE_TO_MANY': 'Один ко многим (1:M)',
    'ONE_TO_ONE': 'Один к одному (1:1)',
    'MANY_TO_MANY': 'Многие ко многим (M:M)'
};

const DELETE_ACTION_LABELS: Record<OnDeleteAction, string> = {
    'CASCADE': 'Удалить каскадно',
    'DEFAULT': 'По-умолчанию',
    'NO_ACTION': 'Без действия',
    'RESTRICT': 'Запретить',
    'SET_NULL': 'Установить NULL'
}

const UPDATE_ACTION_LABELS: Record<OnUpdateAction, string> = {
    'CASCADE': 'Обновить каскадно',
    'NO_ACTION': 'Без действия',
}

export const AddReferenceMenu = observer(() => {
    const menuRef = useRef<HTMLDivElement>(null);
    const position = useRef({ x: 0, y: 0 });
    const isDragging = useRef(false);
    const dragOffset = useRef({ x: 0, y: 0 });
    
    const scale = erStore.scale;
    useEffect(() => {
        if (referenceStore.isOpen) {
            position.current = { x: referenceStore.menuX, y: referenceStore.menuY };
            if (menuRef.current) {
                menuRef.current.style.transform = `translate(${position.current.x}px, ${position.current.y}px) scale(${scale})`;
            }
        }
    }, [referenceStore.isOpen, referenceStore.menuX, referenceStore.menuY]);

    useEffect(() => {
        if (referenceStore.isOpen && menuRef.current) {
            menuRef.current.style.transform = `translate(${position.current.x}px, ${position.current.y}px) scale(${scale})`;
        }
    }, [scale, referenceStore.isOpen]);

    useEffect(() => {
        const handleMouseMove = (e: MouseEvent) => {
            if (isDragging.current && menuRef.current) {
                position.current = {
                    x: e.clientX - dragOffset.current.x,
                    y: e.clientY - dragOffset.current.y
                };
                menuRef.current.style.transform = `translate(${position.current.x}px, ${position.current.y}px) scale(${erStore.scale})`;
            }
        };

        const handleMouseUp = () => {
            isDragging.current = false;
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);

        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, []);

    const handleMouseDown = (e: React.MouseEvent) => {
        isDragging.current = true;
        dragOffset.current = {
            x: e.clientX - position.current.x,
            y: e.clientY - position.current.y
        };
    };

    if (!referenceStore.isOpen) return null;

    const sourceTable = referenceStore.sourceTableId ? erStore.getTable(referenceStore.sourceTableId) : null;
    const targetTable = referenceStore.targetTableId ? erStore.getTable(referenceStore.targetTableId) : null;
    const maxRows = Math.max(referenceStore.sourceCols.length, referenceStore.targetCols.length);

    return (
        <div 
            className="er_add_reference_menu__container"
            ref={menuRef}
            onMouseDown={(e) => e.stopPropagation()}
            onClick={(e) => e.stopPropagation()}
        >
            <div className="add_reference_header" onMouseDown={handleMouseDown}>
                <h4 className="add_reference_title">Создание связи</h4>
            </div>

            <div className="er_add_reference_menu">
                <div className="ref_controls_group">
                    <select 
                        className="ref_select"
                        value={referenceStore.refType} 
                        onChange={e => referenceStore.refType = e.target.value as ReferenceType} 
                    >
                        {(Object.keys(REFERENCE_TYPE_LABELS) as ReferenceType[]).map(type => (
                            <option key={type} value={type}>
                                {REFERENCE_TYPE_LABELS[type]}
                            </option>
                        ))}
                    </select>

                    <div className="ref_actions_row">
                        <div className="ref_action_col">
                            <label className="ref_label">При удалении</label>
                            <select 
                                className="ref_select"
                                value={referenceStore.onDelete} 
                                onChange={e => referenceStore.onDelete = e.target.value as OnDeleteAction}
                            >
                                {(Object.keys(DELETE_ACTION_LABELS) as OnDeleteAction[]).map(action => (
                                    <option key={action} value={action}>
                                        {DELETE_ACTION_LABELS[action]}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="ref_action_col">
                            <label className="ref_label">При обновлении</label>
                            <select 
                                className="ref_select"
                                value={referenceStore.onUpdate} 
                                onChange={e => referenceStore.onUpdate = e.target.value as OnUpdateAction}
                            >
                                {(Object.keys(UPDATE_ACTION_LABELS) as OnUpdateAction[]).map(action => (
                                    <option key={action} value={action}>
                                        {UPDATE_ACTION_LABELS[action]}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>

                <div className="ref_mapping_box">
                    <div className="ref_mapping_header">
                        <div className="ref_mapping_col">Source: {sourceTable?.name || '...'}</div>
                        <div className="ref_mapping_col">Target: {targetTable?.name || '...'}</div>
                    </div>

                    {Array.from({ length: maxRows }).map((_, i) => {
                        const sColId = referenceStore.sourceCols[i];
                        const tColId = referenceStore.targetCols[i];
                        const sColName = sColId && sourceTable ? sourceTable.columns[sColId]?.name : '---';
                        const tColName = tColId && targetTable ? targetTable.columns[tColId]?.name : '---';

                        return (
                            <div key={i} className="ref_mapping_row">
                                <div className="ref_col_item">
                                    {sColName}
                                </div>
                                <span className="ref_arrow_icon">➡</span>
                                <div className="ref_col_item target_item">
                                    <span className="ref_col_text">{tColName}</span>
                                    
                                    {tColId && (
                                        <div className="ref_col_controls">
                                            <button 
                                                className="ref_ctrl_btn"
                                                disabled={i === 0} 
                                                onClick={() => referenceStore.moveTargetUp(i)} 
                                            >
                                                ▲
                                            </button>
                                            <button 
                                                className="ref_ctrl_btn"
                                                disabled={i === referenceStore.targetCols.length - 1} 
                                                onClick={() => referenceStore.moveTargetDown(i)} 
                                            >
                                                ▼
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>

                <div className="ref_btn_container">
                    <button 
                        className="ref_btn submit_btn"
                        onClick={() => referenceStore.submit()} 
                        disabled={!referenceStore.isReadyToSubmit}
                    >
                        Добавить
                    </button>
                    <button 
                        className="ref_btn cancel_btn"
                        onClick={() => referenceStore.reset()} 
                    >
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
});