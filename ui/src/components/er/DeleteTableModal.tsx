import { observer } from 'mobx-react-lite';
import { tableDeleteStore } from '@/store/TableDeleteStore';
import { erStore } from '@/store/ERStore';
import './css/DeleteTableModal.css'

export const DeleteTableModal = observer(() => {
    if (!tableDeleteStore.isOpen || !tableDeleteStore.tableIdToDelete) return null;

    const table = erStore.getTable(tableDeleteStore.tableIdToDelete);
    const tableName = table ? table.name : 'выбранную таблицу';

    return (
        <div 
            className='modal__overlay' 
            onClick={() => tableDeleteStore.close()}
            onWheel={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
            onMouseMove={(e) => e.stopPropagation()}
            onDoubleClick={(e) => e.stopPropagation()}
        >
            <div 
                className='modal__container'
                onClick={e => e.stopPropagation()}
            >
                <h3 className='delete_table_header'>
                    <span style={{ fontSize: '24px' }}>⚠️</span> Удаление таблицы
                </h3>
                
                <p className='delete_table_message'>
                    Вы уверены, что хотите удалить таблицу <b style={{ color: 'white' }}>{tableName}</b>? 
                    Это действие нельзя отменить. Все связанные с ней внешние ключи также будут удалены.
                </p>
                
                <div className='delete_table_btn_container'>
                    <button 
                        onClick={() => tableDeleteStore.close()} 
                        className='delete_table_btn_cancel'
                    >
                        Отмена
                    </button>
                    <button 
                        onClick={() => tableDeleteStore.confirm()} 
                        className='delete_table_btn_confirm'
                    >
                        Да, удалить
                    </button>
                </div>
            </div>
        </div>
    );
});