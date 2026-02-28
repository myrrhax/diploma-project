import React, { useState, useEffect, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore } from '@/store/ERStore';
import { v4 as uuidv4 } from 'uuid';
import { type Column, type ColumnType, type ConstraintType } from '@/model/SchemaElements';
import './css/AddColumnMenu.css';

const ALL_TYPES: ColumnType[] = [
    'INT', 'BIGINT', 'SMALLINT', 'VARCHAR', 'CHAR', 'TEXT', 'NUMERIC', 'DECIMAL',
    'FLOAT', 'DOUBLE', 'BOOLEAN', 'UUID', 'DATE', 'TIME', 'DATETIME', 'TIMESTAMP', 'JSON'
];

interface AddColumnMenuProps {
    tableId: string;
    onClose: (column: Column) => void;
    onCancel: () => void;
    oldColumn?: Column | null;
}

export const AddColumnMenu = observer(({ onClose, onCancel, oldColumn, tableId }: AddColumnMenuProps) => {
    const { state } = erStore;
    const table = state?.tables[tableId];
    if (!table) {
        return null;
    }
    
    const isEditing = oldColumn != null && oldColumn != undefined;
    const allColumns = (table as any).columns ? Object.values((table as any).columns) : [];
    
    let otherPkPartsCount = 0;
    let hasOtherAutoIncrement = false;

    if (allColumns.length > 0) {
        otherPkPartsCount = allColumns.filter((c: any) => c.id !== oldColumn?.id && (c.pkPart || c.isPkPart)).length;
        hasOtherAutoIncrement = allColumns.some((c: any) => c.id !== oldColumn?.id && c.autoIncrement);
    } else {
        otherPkPartsCount = (table.primaryKeyParts || []).filter(id => id !== oldColumn?.id).length;
        hasOtherAutoIncrement = !!table.autoIncrementedColumn && table.autoIncrementedColumn !== oldColumn?.id;
    }

    const oldFullPk = isEditing && table.primaryKeyParts.length === 1 && table.primaryKeyParts.includes(oldColumn.id);

    const [name, setName] = useState(oldColumn?.name ?? '');
    const [type, setType] = useState<ColumnType>(oldColumn?.columnType ?? 'VARCHAR');
    const [length, setLength] = useState<number | ''>(oldColumn?.length ?? '');
    const [precision, setPrecision] = useState<number | ''>(oldColumn?.precision ?? '');
    const [scale, setScale] = useState<number | ''>(oldColumn?.scale ?? '');
    const [defaultValue, setDefaultValue] = useState<string | null>(oldColumn?.defaultValue ?? null);
    
    const [isPkPart, setIsPkPart] = useState<boolean>(isEditing ? (oldColumn.pkPart ?? false) : false);
    const [isNotNull, setIsNotNull] = useState((oldColumn?.constraints?.includes('NOT_NULL') ?? false) || oldFullPk);
    const [isUnique, setIsUnique] = useState((oldColumn?.constraints?.includes('UNIQUE') ?? false) || oldFullPk);
    const [description, setDescription] = useState<string | null>(oldColumn?.description ?? null);
    const [isAutoIncrement, setIsAutoIncrement] = useState(oldColumn?.autoIncrement ?? false);

    const hasLength = ['CHAR', 'VARCHAR', 'NUMERIC'].includes(type);
    const hasPrecisionScale = type === 'DECIMAL';
    const isDateType = ['DATE', 'TIME', 'TIMESTAMP', 'DATETIME'].includes(type);
    const isAutoIncrementableType = ['INT', 'SMALLINT', 'BIGINT'].includes(type);
    
    const isSolePk = isPkPart && otherPkPartsCount === 0;

    const canAutoincrement = 
        isPkPart && 
        isUnique && 
        isAutoIncrementableType &&
        !hasOtherAutoIncrement;

    const menuRef = useRef<HTMLDivElement>(null);
    const position = useRef({ x: 0, y: 0 });
    const isDragging = useRef(false);
    const dragOffset = useRef({ x: 0, y: 0 });

    useEffect(() => {
        if (isPkPart) {
            setIsNotNull(true);
        }
        
        if (isSolePk) {
            setIsUnique(true);
        }
    }, [isPkPart, isSolePk]);

    useEffect(() => {
        if (!canAutoincrement) {
            setIsAutoIncrement(false);
        }
    }, [canAutoincrement]);

    useEffect(() => {
        const handleMouseMove = (e: MouseEvent) => {
            if (isDragging.current && menuRef.current) {
                position.current = {
                    x: e.clientX - dragOffset.current.x,
                    y: e.clientY - dragOffset.current.y
                };
                menuRef.current.style.transform = `translate(${position.current.x}px, ${position.current.y}px)`;
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

    const handleSave = () => {
        if (!name.trim()) {
            alert('Введите имя колонки');
            return;
        }

        const constraints: ConstraintType[] = [];
        if (isNotNull) constraints.push('NOT_NULL');
        if (isUnique) constraints.push('UNIQUE');
        
        const column = {
            id: oldColumn?.id ?? uuidv4().toString(),
            name: name.trim(),
            columnType: type,
            constraints: constraints,
            defaultValue: defaultValue,
            autoIncrement: isAutoIncrement,
            length: hasLength && length !== '' ? Number(length) : null,
            precision: hasPrecisionScale && precision !== '' ? Number(precision) : null,
            scale: hasPrecisionScale && scale !== '' ? Number(scale) : null,
            description: description,
            pkPart: isPkPart
        };

        onClose(column);
    };

    const handleMouseDown = (e: React.MouseEvent) => {
        isDragging.current = true;
        dragOffset.current = {
            x: e.clientX - position.current.x,
            y: e.clientY - position.current.y
        };
    };

    const title = isEditing ? 'Обновление колонки' : 'Добавление колонки';

    return (
        <div 
            className="er_add_column_menu__container"
            style={{
                left: `${erStore.TABLE_WIDTH + 15}px`,
                transform: `translate(${position.current.x}px, ${position.current.y}px)`, 
                zIndex: 1000
            }}
            ref={menuRef}
            onMouseDown={(e) => e.stopPropagation()} 
            onClick={(e) => e.stopPropagation() }
            title={title}
        >
            <h4 
                className='add_column_title' 
                onMouseDown={handleMouseDown}
                style={{ 
                    cursor: 'grab', 
                    userSelect: 'none', 
                    margin: 0,
                    paddingBottom: '10px'
                }}
            >
                { title }
            </h4>

            <div className='er_add_column_menu'>
                <input className='col_input' placeholder="Имя колонки" value={name} onChange={e => setName(e.target.value)} />

                <select className='col_type_select' value={type} onChange={e => setType(e.target.value as ColumnType)}>
                    {ALL_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>

                {hasLength && (
                    <input type="number" placeholder="Длина" value={length} onChange={e => setLength(e.target.value ? Number(e.target.value) : '')} className='col_length_input' />
                )}

                {hasPrecisionScale && (
                    <div className='col_decimal_input'>
                        <input type="number" placeholder="Точность" value={precision} onChange={e => setPrecision(e.target.value ? Number(e.target.value) : '')} className='col_precision_input' />
                        <input type="number" placeholder="Масштаб" value={scale} onChange={e => setScale(e.target.value ? Number(e.target.value) : '')} className='col_scale_input' />
                    </div>
                )}
                
                {!isUnique && (
                    <div className='col_default_container'>
                        <input placeholder="По умолчанию" value={defaultValue ?? ''} onChange={e => setDefaultValue(e.target.value)} className='col_default_input' />
                        {isDateType && (
                            <button title="Установить текущее время" onClick={() => setDefaultValue('now')} style={{ padding: '6px' }}>🕒</button>
                        )}
                    </div>
                )}

                <input type='text' placeholder='Описание' className='col_description_input' value={description ?? ''} onChange={e => setDescription(e.target.value)} />

                <div className='add_col_constraint_container'>
                    <label className='add_col_constraint_label'>
                        <input 
                            type="checkbox" 
                            disabled={isPkPart}
                            checked={isNotNull} 
                            onChange={e => setIsNotNull(e.target.checked)} 
                        />
                        NOT NULL
                    </label>
                    <label className='add_col_constraint_label'>
                        <input 
                            type="checkbox" 
                            disabled={isSolePk}
                            checked={isUnique} 
                            onChange={e => setIsUnique(e.target.checked)} 
                        />
                        UNIQUE
                    </label>
                    <label className='add_col_constraint_label'>
                        <input 
                            type="checkbox" 
                            disabled={oldFullPk ?? false}
                            checked={isPkPart} 
                            onChange={e => setIsPkPart(e.target.checked)} 
                        />
                        PK
                    </label>
                    
                    {isAutoIncrementableType && (
                        <label className='add_col_constraint_label' title={canAutoincrement ? '' : 'Автоинкремент возможно установить на часть первичного ключа, являющуюся уникальным значением, при условии, что других автоинкрементых полей в таблице нет'}>
                            <input 
                                type='checkbox' 
                                checked={isAutoIncrement}
                                onChange={e => setIsAutoIncrement(e.target.checked)}
                                disabled={!canAutoincrement} 
                            />
                            Автоинкремент
                        </label>
                    )}
                </div>

                <div className='add_col_btn_container'>
                    <button onClick={handleSave} className='add_col_btn' style={{ backgroundColor: '#3b82f6' }}>Сохранить</button>
                    <button onClick={onCancel} className='add_col_btn' style={{ backgroundColor: '#64748b' }}>Отмена</button>
                </div>
            </div>
        </div>
    );
});