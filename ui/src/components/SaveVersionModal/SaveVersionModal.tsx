import { useEffect, useState } from "react";
import "./SaveVersionModal.css"

interface SaveVersionModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (tag: string) => void;
}

export const SaveVersionModal = ({ isOpen, onClose, onSave }: SaveVersionModalProps) => {
    const [tag, setTag] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        if (isOpen) {
            setTag('');
            setError('');
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleSave = () => {
        if (!tag.trim()) {
            setError('Введите тэг версии (например, v1.0.0)');
            return;
        }
        
        onSave(tag.trim());
    };

    return (
        <div 
            className="version_modal_overlay" 
            onClick={onClose}
            onWheel={(e) => e.stopPropagation()}       
            onMouseDown={(e) => e.stopPropagation()}   
            onMouseMove={(e) => e.stopPropagation()}   
            onDoubleClick={(e) => e.stopPropagation()}
        >
            <div className="version_modal_content" onClick={e => e.stopPropagation()}>
                <h3 className="version_modal_title">
                    Сохранить версию
                </h3>
                
                <p className="version_modal_description">
                    Создайте снимок текущей схемы, чтобы к ней можно было вернуться позже.
                </p>
                
                <div className="version_modal_form_group">
                    <label className="version_modal_label">Тэг / Название версии</label>
                    <input 
                        type="text" 
                        className="version_modal_input"
                        value={tag} 
                        onChange={e => {
                            setTag(e.target.value);
                            if (error) setError('');
                        }} 
                        placeholder="Например, Release 1.2 или Before Refactoring"
                        autoFocus
                        onKeyDown={e => e.key === 'Enter' && handleSave()}
                    />
                    {error && <span className="version_modal_error">{error}</span>}
                </div>
                
                <div className="version_modal_actions">
                    <button 
                        className="version_modal_btn version_modal_btn_cancel"
                        onClick={onClose} 
                    >
                        Отмена
                    </button>
                    <button 
                        className="version_modal_btn version_modal_btn_save"
                        onClick={handleSave} 
                    >
                        Сохранить
                    </button>
                </div>
            </div>
        </div>
    );
};