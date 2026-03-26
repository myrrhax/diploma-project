import React from 'react';
import { observer } from 'mobx-react-lite';
import { selectionStore } from '@/store/SelectionStore';
import './css/CursorControls.css';

export const CursorControls = observer(() => {
    const handleGrab = (e: React.MouseEvent) => {
        e.stopPropagation();
        selectionStore.setMode('grab');
    };

    const handleSelect = (e: React.MouseEvent) => {
        e.stopPropagation();
        selectionStore.setMode('select');
    };

    const stopPropagation = (e: React.MouseEvent) => e.stopPropagation();

    return (
        <div 
            className="er_cursor_controls" 
            onClick={stopPropagation}
            onMouseDown={stopPropagation}
            onDoubleClick={stopPropagation}
        >
            <button 
                className={`er_cursor_btn ${selectionStore.mode === 'grab' ? 'active' : ''}`} 
                onClick={handleGrab} 
                title="Перемещение (Grab)"
            >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 11V6a2 2 0 0 0-2-2v0a2 2 0 0 0-2 2v0"></path>
                    <path d="M14 10V4a2 2 0 0 0-2-2v0a2 2 0 0 0-2 2v2"></path>
                    <path d="M10 10.5V6a2 2 0 0 0-2-2v0a2 2 0 0 0-2 2v8"></path>
                    <path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15"></path>
                </svg>
            </button>
            <button 
                className={`er_cursor_btn ${selectionStore.mode === 'select' ? 'active' : ''}`} 
                onClick={handleSelect} 
                title="Выделение (Select)"
            >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M3 3l7.07 16.97 2.51-7.39 7.39-2.51L3 3z"></path>
                    <path d="M13 13l6 6"></path>
                </svg>
            </button>
        </div>
    );
});