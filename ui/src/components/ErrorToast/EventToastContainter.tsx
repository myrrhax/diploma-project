import { observer } from 'mobx-react-lite';
import './EventToastContainer.css';
import { eventsStore } from '@/store/EventsStore';

const TOAST_CONFIG: Record<string, { className: string; icon: string }> = {
    INFO: { className: 'er_toast_item--info', icon: 'ℹ️' },
    WARNING: { className: 'er_toast_item--warning', icon: '⚠️' },
    ERROR: { className: 'er_toast_item--error', icon: '❌' },
};

export const EventsToastContainer = observer(() => {
    if (eventsStore.events.length === 0) return null;

    return (
        <div className="er_toast_container">
            {eventsStore.events.map(event => {
                const config = TOAST_CONFIG[event.type] || TOAST_CONFIG.INFO;

                return (
                    <div key={event.id} className={`er_toast_item ${config.className}`}>
                        <span className="er_toast_icon">{config.icon}</span>
                        <span className="er_toast_text">{event.message}</span>
                        <button 
                            className="er_toast_close" 
                            onClick={() => eventsStore.removeEvent(event.id)}
                        >
                            ✕
                        </button>
                    </div>
                );
            })}
        </div>
    );
});