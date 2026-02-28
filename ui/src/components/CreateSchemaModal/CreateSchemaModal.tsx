import { observer } from 'mobx-react-lite';
import './CreateSchemaModal.css';
import { createSchemaModalStore } from '@/store/CreateShemaModalStore';

export const CreateSchemaModal = observer(() => {
    const { name, error, isCreateModalOpen } = createSchemaModalStore;

    if (!isCreateModalOpen) return null;

    const handleCreate = async () => {
        createSchemaModalStore.setError(null);

        if (!name.trim()) {
            createSchemaModalStore.setError('Введите название схемы');
            return;
        }

        await createSchemaModalStore.createSchema(name.trim());
    };

    const handleClose = () => {
        createSchemaModalStore.closeCreateModal();
    };

    return (
        <div className="schema_modal_overlay" onClick={handleClose}>
            <div className="schema_modal_content" onClick={e => e.stopPropagation()}>
                <h3 className="schema_modal_title">
                    <span>📁</span> Новая схема
                </h3>
                
                <div className="schema_modal_form_group">
                    <label className="schema_modal_label">Название схемы</label>
                    <input 
                        type="text" 
                        className="schema_modal_input"
                        value={name} 
                        onChange={e => {
                            createSchemaModalStore.setName(e.target.value);
                        }} 
                        placeholder="Например, E-commerce DB"
                        autoFocus
                        onKeyDown={e => e.key === 'Enter' && handleCreate()}
                    />
                    {error && <span className="schema_modal_error">{error}</span>}
                </div>
                
                <div className="schema_modal_actions">
                    <button 
                        className="schema_modal_btn schema_modal_btn_cancel"
                        onClick={handleClose} 
                    >
                        Отмена
                    </button>
                    <button 
                        className="schema_modal_btn schema_modal_btn_create"
                        onClick={handleCreate} 
                    >
                        Создать
                    </button>
                </div>
            </div>
        </div>
    );
});