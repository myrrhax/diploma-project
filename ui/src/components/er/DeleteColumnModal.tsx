import React from 'react';
import { observer } from 'mobx-react-lite';
import { columnDeleteStore } from '@/store/ColumnDeleteStore';
import { erStore } from '@/store/ERStore';
import './css/DeleteColumnModal.css'; // Подключаем наш новый файл

export const DeleteColumnModal = observer(() => {
    if (!columnDeleteStore.isOpen || !columnDeleteStore.tableId || !columnDeleteStore.colId) return null;

    const table = erStore.getTable(columnDeleteStore.tableId);
    const column = table?.columns[columnDeleteStore.colId];
    const columnName = column ? column.name : 'выбранную колонку';

    return (
        <div 
            className="er_modal_overlay"
            onClick={() => columnDeleteStore.close()}
            onWheel={(e) => e.stopPropagation()}       
            onMouseDown={(e) => e.stopPropagation()}   
            onMouseMove={(e) => e.stopPropagation()}   
            onDoubleClick={(e) => e.stopPropagation()} 
        >
            <div 
                className="er_modal_content"
                onClick={e => e.stopPropagation()}
            >
                <h3 className="er_modal_title">
                    <span className="er_modal_icon">⚠️</span> Удаление колонки
                </h3>
                
                <p className="er_modal_text">
                    Вы уверены, что хотите удалить колонку <b className="er_modal_highlight">{columnName}</b>? 
                    Это действие нельзя отменить. Связи, использующие эту колонку, также могут быть удалены.
                </p>
                
                <div className="er_modal_actions">
                    <button 
                        className="er_modal_btn er_modal_btn_cancel"
                        onClick={() => columnDeleteStore.close()} 
                    >
                        Отмена
                    </button>
                    <button 
                        className="er_modal_btn er_modal_btn_delete"
                        onClick={() => columnDeleteStore.confirm()} 
                    >
                        Да, удалить
                    </button>
                </div>
            </div>
        </div>
    );
});