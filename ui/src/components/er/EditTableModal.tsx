import { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { tableModalsStore } from '@/store/TableModalsStore';
import { erStore } from '@/store/ERStore';
import './css/EditTableModal.css';

export const EditTableModal = observer(() => {
    const { isEditOpen, tableIdToEdit } = tableModalsStore;
    const table = tableIdToEdit ? erStore.getTable(tableIdToEdit) : null;

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        if (isEditOpen && table) {
            setName(table.name || '');
            setDescription(table.description || '');
        }
    }, [isEditOpen, table]);

    if (!isEditOpen || !table) return null;

    const handleSave = () => {
        if (!name.trim()) {
            alert('Имя таблицы не может быть пустым');
            return;
        }
        const trimmedName = name.trim();
        const trimmedDescription = description.trim();
        if (trimmedName.length === 0 && trimmedDescription.length === 0) {
            return;
        }
        const newName = trimmedName === table.name ? null : trimmedName;
        const newDescription = trimmedDescription === table.description ? null : trimmedDescription;

        tableModalsStore.confirmEdit(newName, newDescription);
    };

    return (
        <div 
            className="er_edit_modal_overlay"
            onClick={() => tableModalsStore.closeEdit()}
            onWheel={(e) => e.stopPropagation()}       
            onMouseDown={(e) => e.stopPropagation()}   
            onMouseMove={(e) => e.stopPropagation()}   
            onDoubleClick={(e) => e.stopPropagation()} 
        >
            <div 
                className="er_edit_modal_content"
                onClick={e => e.stopPropagation()}
            >
                <h3 className="er_edit_modal_title">
                    <span>✏️</span> Редактирование таблицы
                </h3>
                
                <div className="er_edit_modal_form">
                    <div className="er_edit_modal_group">
                        <label className="er_edit_modal_label">Имя таблицы</label>
                        <input 
                            type="text" 
                            className="er_edit_modal_input" 
                            value={name} 
                            onChange={e => setName(e.target.value)} 
                            placeholder="Например, users"
                            autoFocus
                        />
                    </div>

                    <div className="er_edit_modal_group">
                        <label className="er_edit_modal_label">Описание (опционально)</label>
                        <textarea 
                            className="er_edit_modal_textarea" 
                            value={description} 
                            onChange={e => setDescription(e.target.value)} 
                            placeholder="Опишите назначение таблицы..."
                        />
                    </div>
                </div>
                
                <div className="er_edit_modal_actions">
                    <button 
                        className="er_edit_modal_btn er_edit_modal_btn_cancel"
                        onClick={() => tableModalsStore.closeEdit()} 
                    >
                        Отмена
                    </button>
                    <button 
                        className="er_edit_modal_btn er_edit_modal_btn_save"
                        onClick={handleSave} 
                    >
                        Сохранить
                    </button>
                </div>
            </div>
        </div>
    );
});