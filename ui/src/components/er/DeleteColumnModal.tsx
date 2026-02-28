import { observer } from 'mobx-react-lite';
import { columnModalsStore } from '@/store/ColumnModalsStore';
import { erStore } from '@/store/ERStore';
import './css/DeleteColumnModal.css'; // Подключаем наш новый файл

export const DeleteColumnModal = observer(() => {
    if (!columnModalsStore.isOpen || !columnModalsStore.tableId || !columnModalsStore.colId) return null;

    const table = erStore.getTable(columnModalsStore.tableId);
    const column = table?.columns[columnModalsStore.colId];
    const columnName = column ? column.name : 'выбранную колонку';

    return (
        <div 
            className="er_modal_overlay"
            onClick={() => columnModalsStore.close()}
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
                        onClick={() => columnModalsStore.close()} 
                    >
                        Отмена
                    </button>
                    <button 
                        className="er_modal_btn er_modal_btn_delete"
                        onClick={() => columnModalsStore.confirm()} 
                    >
                        Да, удалить
                    </button>
                </div>
            </div>
        </div>
    );
});