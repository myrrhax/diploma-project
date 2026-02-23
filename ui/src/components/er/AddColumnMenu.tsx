import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore } from '@/store/ERStore';
import { v4 as uuidv4 } from 'uuid';
import { type ColumnType, type ConstraintType } from '@/model/SchemaElements';
import './css/AddColumnMenu.css';

const ALL_TYPES: ColumnType[] = [
    'INT', 'BIGINT', 'SMALLINT', 'VARCHAR', 'CHAR', 'TEXT', 'NUMERIC', 'DECIMAL',
    'FLOAT', 'DOUBLE', 'BOOLEAN', 'UUID', 'DATE', 'TIME', 'DATETIME', 'TIMESTAMP', 'JSON'
];

interface AddColumnMenuProps {
    tableId: string;
    onClose: () => void;
}

export const AddColumnMenu = observer(({ tableId, onClose }: AddColumnMenuProps) => {
    const [name, setName] = useState('');
    const [type, setType] = useState<ColumnType>('VARCHAR');
    const [length, setLength] = useState<number | ''>('');
    const [precision, setPrecision] = useState<number | ''>('');
    const [scale, setScale] = useState<number | ''>('');
    const [defaultValue, setDefaultValue] = useState<string | null>(null);
    const [isNotNull, setIsNotNull] = useState(false);
    const [isUnique, setIsUnique] = useState(false);
    const [autoIncrement, setAutoIncrement] = useState(false);

    const hasLength = ['CHAR', 'VARCHAR', 'NUMERIC'].includes(type);
    const hasPrecisionScale = type === 'DECIMAL';
    const isDateType = ['DATE', 'TIME', 'TIMESTAMP', 'DATETIME'].includes(type);
    const isAutoIncrementable = ['SMALLINT', 'INT', 'BIGINT'].includes(type);

    const handleSave = () => {
        if (!name.trim()) {
            alert('Введите имя колонки');
            return;
        }

        const constraints: ConstraintType[] = [];
        if (isNotNull) constraints.push('NOT_NULL');
        if (isUnique) constraints.push('UNIQUE');

        erStore.addColumn(tableId, {
            id: uuidv4().toString(),
            name: name.trim(),
            columnType: type,
            constraints: constraints,
            autoIncrement: autoIncrement,
            defaultValue: defaultValue,
            length: hasLength && length !== '' ? Number(length) : null,
            precision: hasPrecisionScale && precision !== '' ? Number(precision) : null,
            scale: hasPrecisionScale && scale !== '' ? Number(scale) : null,
            description: ''
        });

        onClose();
    };

    return (
        <div 
            className="er_add_column_menu__container"
            style={{left: `${erStore.TABLE_WIDTH + 15}px`}}
            onMouseDown={(e) => e.stopPropagation()} 
        >
            <h4 className='add_column_title'>Новая колонка</h4>

            <div className='er_add_column_menu'>
                <input
                    className='col_input'
                    placeholder="Имя колонки" 
                    value={name} 
                    onChange={e => setName(e.target.value)} 
                />

                <select
                    className='col_type_select'
                    value={type} 
                    onChange={e => setType(e.target.value as ColumnType)}
                >
                    {ALL_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>

                {hasLength && (
                    <input 
                        type="number" 
                        placeholder="Длина (например, 255)" 
                        value={length} 
                        onChange={e => setLength(e.target.value ? Number(e.target.value) : '')}
                        className='col_length_input'
                    />
                )}

                {hasPrecisionScale && (
                    <div className='col_decimal_input'>
                        <input 
                            type="number" 
                            placeholder="Точность" 
                            value={precision} 
                            onChange={e => setPrecision(e.target.value ? Number(e.target.value) : '')}
                            className='col_precision_input'
                        />
                        <input 
                            type="number" 
                            placeholder="Масштаб" 
                            value={scale} 
                            onChange={e => setScale(e.target.value ? Number(e.target.value) : '')}
                            className='col_scale_input'
                        />
                    </div>
                )}
                
                {!isUnique && (
                    <div className='col_default_container'>
                        <input 
                            placeholder="По умолчанию" 
                            value={defaultValue ?? ''} 
                            onChange={e => setDefaultValue(e.target.value)}
                            className='col_default_input'
                        />
                        {isDateType && (
                            <button 
                                title="Установить текущее время"
                                onClick={() => setDefaultValue('now')}
                                style={{ padding: '6px' }}
                            >
                                🕒
                            </button>
                        )}
                    </div>
                )}

                <div className='add_col_constraint_container'>
                    <label className='add_col_constraint_label'>
                        <input type="checkbox" checked={isNotNull} onChange={e => setIsNotNull(e.target.checked)} />
                        NOT NULL
                    </label>
                    <label className='add_col_constraint_label'>
                        <input type="checkbox" checked={isUnique} onChange={e => setIsUnique(e.target.checked)} />
                        UNIQUE
                    </label>
                </div>

                <div className='add_col_btn_container'>
                    <button onClick={handleSave} className='add_col_btn' style={{ backgroundColor: '#3b82f6' }}>
                        Сохранить
                    </button>
                    <button onClick={onClose} className='add_col_btn' style={{ backgroundColor: '#64748b' }}>
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
});