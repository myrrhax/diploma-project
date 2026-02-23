import { observer } from 'mobx-react-lite';
import './ErrorToast.css';
import { errorsStore } from '@/store/ErrorsStore';

export const ErrorToasts = observer(() => {
    if (errorsStore.errors.length === 0) return null;

    return (
        <div className="er_toast_container">
            {errorsStore.errors.map(err => (
                <div key={err.id} className="er_toast_item">
                    <span className="er_toast_icon">⚠️</span>
                    <span className="er_toast_text">{err.text}</span>
                    <button 
                        className="er_toast_close" 
                        onClick={() => errorsStore.removeError(err.id)}
                    >
                        ✕
                    </button>
                </div>
            ))}
        </div>
    );
});