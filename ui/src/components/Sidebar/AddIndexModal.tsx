import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { type Table, type Index, type IndexType } from '@/model/SchemaElements';
import { v4 as uuidv4 } from 'uuid';
import './css/AddIndexModal.css';

interface AddIndexModalProps {
    table: Table;
    onClose: () => void;
    onSave: (index: Index) => void;
}

export const AddIndexModal = observer(({ table, onClose, onSave }: AddIndexModalProps) => {
    const [indexName, setIndexName] = useState('');
    const [indexType, setIndexType] = useState<IndexType>('B_TREE');
    const [isUnique, setIsUnique] = useState(false);
    const [selectedCols, setSelectedCols] = useState<string[]>([]);
    const columns = Object.values(table.columns || {});

    const handleToggleCol = (colId: string) => {
        setSelectedCols(prev => 
            prev.includes(colId) ? prev.filter(id => id !== colId) : [...prev, colId]
        );
    };

    const handleSave = () => {
        if (selectedCols.length === 0) return;
        const newIndex: Index = {
            id: uuidv4(),
            columnIds: selectedCols,
            indexType: indexType,
            indexName: indexName.trim() || undefined,
            unique: isUnique
        };
        onSave(newIndex);
    };

    return (
        <div className="index_modal_overlay" onMouseDown={onClose}>
            <div className="index_modal_content" onMouseDown={e => e.stopPropagation()}>
                <h3 className="index_modal_title">Добавление индекса</h3>
                
                <input 
                    className="index_modal_input" 
                    placeholder="Имя индекса (необязательно)" 
                    value={indexName}
                    onChange={e => setIndexName(e.target.value)}
                />

                <select 
                    className="index_modal_input" 
                    value={indexType} 
                    onChange={e => setIndexType(e.target.value as IndexType)}
                >
                    <option value="B_TREE">B-Tree</option>
                    <option value="HASH">Hash</option>
                </select>

                <label className="index_modal_checkbox_wrapper">
                    <input 
                        type="checkbox" 
                        checked={isUnique}
                        onChange={e => setIsUnique(e.target.checked)}
                    />
                    <span>Уникальный индекс (UNIQUE)</span>
                </label>

                <div className="index_modal_columns">
                    {columns.map(col => {
                        const isUQ = col.constraints?.includes('UNIQUE') || col.pkPart;
                        const isNN = col.constraints?.includes('NOT_NULL') || col.pkPart;
                        
                        return (
                            <label key={col.id} className="index_col_label">
                                <input 
                                    type="checkbox" 
                                    checked={selectedCols.includes(col.id)}
                                    onChange={() => handleToggleCol(col.id)}
                                />
                                <span className="index_col_name">{col.name}</span>
                                <div className="index_col_badges">
                                    {isUQ && <span className="index_badge uq">UQ</span>}
                                    {isNN && <span className="index_badge nn">NN</span>}
                                </div>
                            </label>
                        );
                    })}
                </div>

                <div className="index_modal_actions">
                    <button 
                        className="index_modal_btn submit" 
                        disabled={selectedCols.length === 0}
                        onClick={handleSave}
                    >
                        Создать
                    </button>
                    <button className="index_modal_btn cancel" onClick={onClose}>
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
});