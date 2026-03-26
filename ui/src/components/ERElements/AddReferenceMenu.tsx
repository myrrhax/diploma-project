import React, { useEffect, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { referenceStore } from '@/store/ReferenceStore';
import { erStore } from '@/store/ERStore';
import type { ReferenceType, OnDeleteAction, OnUpdateAction } from '@/model/SchemaElements';

// Словарь для читаемого отображения типов связей
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
    const isDragging = useRef(false);
    const dragOffset = useRef({ x: 0, y: 0 });

    useEffect(() => {
        const handleMouseMove = (e: MouseEvent) => {
            if (isDragging.current) {
                referenceStore.menuX = e.clientX - dragOffset.current.x;
                referenceStore.menuY = e.clientY - dragOffset.current.y;
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
            x: e.clientX - referenceStore.menuX,
            y: e.clientY - referenceStore.menuY
        };
    };

    if (!referenceStore.isOpen) return null;

    const sourceTable = referenceStore.sourceTableId ? erStore.getTable(referenceStore.sourceTableId) : null;
    const targetTable = referenceStore.targetTableId ? erStore.getTable(referenceStore.targetTableId) : null;

    const maxRows = Math.max(referenceStore.sourceCols.length, referenceStore.targetCols.length);

    return (
        <div 
            style={{
                position: 'fixed',
                left: referenceStore.menuX + 15,
                top: referenceStore.menuY + 15,
                backgroundColor: '#1e293b',
                border: '2px solid #ef4444', 
                borderRadius: '8px',
                zIndex: 9999,
                width: '350px',
                boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.5)',
                color: 'white',
                display: 'flex',
                flexDirection: 'column'
            }}
            // Останавливаем клики, чтобы не тащился канвас под меню
            onMouseDown={(e) => e.stopPropagation()}
        >
            {/* ШАПКА ОКНА (Draggable Area) */}
            <div 
                onMouseDown={handleMouseDown}
                style={{ 
                    padding: '12px 16px', 
                    borderBottom: '1px solid #334155', 
                    cursor: 'grab', 
                    userSelect: 'none', // Чтобы текст не выделялся при перетаскивании
                    backgroundColor: '#0f172a', // Делаем шапку чуть темнее для визуального выделения
                    borderTopLeftRadius: '6px',
                    borderTopRightRadius: '6px'
                }}
            >
                <h4 style={{ margin: 0, fontSize: '15px' }}>Создание связи</h4>
            </div>

            {/* ОСНОВНОЕ ТЕЛО ОКНА */}
            <div style={{ padding: '16px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                    <select 
                        value={referenceStore.refType} 
                        onChange={e => referenceStore.refType = e.target.value as ReferenceType} 
                        style={{ padding: '6px', borderRadius: '4px' }}
                    >
                        {/* Используем наш словарь для красивых названий */}
                        {(Object.keys(REFERENCE_TYPE_LABELS) as ReferenceType[]).map(type => (
                            <option key={type} value={type}>
                                {REFERENCE_TYPE_LABELS[type]}
                            </option>
                        ))}
                    </select>

                    <div style={{ display: 'flex', gap: '8px' }}>
                        <div style={{ flex: 1 }}>
                            <label style={{ fontSize: '12px', color: '#94a3b8' }}>При удалении</label>
                            <select value={referenceStore.onDelete} onChange={e => referenceStore.onDelete = e.target.value as OnDeleteAction} style={{ width: '100%', padding: '4px' }}>
                                {(Object.keys(DELETE_ACTION_LABELS) as OnDeleteAction[]).map(action => (
                                    <option key={action} value={action}>
                                        {DELETE_ACTION_LABELS[action]}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div style={{ flex: 1 }}>
                            <label style={{ fontSize: '12px', color: '#94a3b8' }}>При обновлении</label>
                            <select value={referenceStore.onUpdate} onChange={e => referenceStore.onUpdate = e.target.value as OnUpdateAction} style={{ width: '100%', padding: '4px' }}>
                                {(Object.keys(UPDATE_ACTION_LABELS) as OnUpdateAction[]).map(action => (
                                    <option key={action} value={action}>
                                        {UPDATE_ACTION_LABELS[action]}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>

                <div style={{ border: '1px solid #334155', borderRadius: '6px', padding: '8px', marginBottom: '16px' }}>
                    <div style={{ display: 'flex', borderBottom: '1px solid #334155', paddingBottom: '4px', marginBottom: '8px', fontSize: '12px', color: '#94a3b8' }}>
                        <div style={{ flex: 1 }}>Source: {sourceTable?.name || '...'}</div>
                        <div style={{ flex: 1 }}>Target: {targetTable?.name || '...'}</div>
                    </div>

                    {Array.from({ length: maxRows }).map((_, i) => {
                        const sColId = referenceStore.sourceCols[i];
                        const tColId = referenceStore.targetCols[i];
                        const sColName = sColId && sourceTable ? sourceTable.columns[sColId]?.name : '---';
                        const tColName = tColId && targetTable ? targetTable.columns[tColId]?.name : '---';

                        return (
                            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', fontSize: '14px' }}>
                                <div style={{ flex: 1, backgroundColor: '#334155', padding: '4px 8px', borderRadius: '4px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                    {sColName}
                                </div>
                                <span style={{ color: '#64748b' }}>➡</span>
                                <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '4px', backgroundColor: '#334155', padding: '4px 8px', borderRadius: '4px' }}>
                                    <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis' }}>{tColName}</span>
                                    
                                    {tColId && (
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                            <button disabled={i === 0} onClick={() => referenceStore.moveTargetUp(i)} style={{ fontSize: '8px', padding: '1px 4px', cursor: 'pointer' }}>▲</button>
                                            <button disabled={i === referenceStore.targetCols.length - 1} onClick={() => referenceStore.moveTargetDown(i)} style={{ fontSize: '8px', padding: '1px 4px', cursor: 'pointer' }}>▼</button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                    <button 
                        onClick={() => referenceStore.submit()} 
                        disabled={!referenceStore.isReadyToSubmit}
                        style={{ 
                            flex: 1, padding: '8px', borderRadius: '4px', border: 'none', color: 'white', cursor: referenceStore.isReadyToSubmit ? 'pointer' : 'not-allowed',
                            backgroundColor: referenceStore.isReadyToSubmit ? '#3b82f6' : '#475569'
                        }}
                    >
                        Добавить
                    </button>
                    <button 
                        onClick={() => referenceStore.reset()} 
                        style={{ flex: 1, padding: '8px', borderRadius: '4px', border: 'none', backgroundColor: '#64748b', color: 'white', cursor: 'pointer' }}
                    >
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
});