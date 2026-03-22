import React, { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { runInAction } from 'mobx';
import { erStore } from '@/store/ERStore';
import './css/ERZoomControls.css';

export const ERZoomControls = observer(() => {
    const [inputValue, setInputValue] = useState(String(Math.round(erStore.scale * 100)));

    useEffect(() => {
        setInputValue(String(Math.round(erStore.scale * 100)));
    }, [erStore.scale]);

    const applyScale = (newPercent: number) => {
        const clampedPercent = Math.max(30, Math.min(200, newPercent));
        
        runInAction(() => {
            erStore.scale = clampedPercent / 100;
            erStore.constrainPan(); 
        });
        
        setInputValue(String(clampedPercent));
    };

    const handleZoomIn = (e: React.MouseEvent) => {
        e.stopPropagation();
        applyScale(Math.round(erStore.scale * 100) + 20);
    };

    const handleZoomOut = (e: React.MouseEvent) => {
        e.stopPropagation();
        applyScale(Math.round(erStore.scale * 100) - 20);
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value.replace(/\D/g, ''); 
        setInputValue(val);
    };

    const handleBlur = () => {
        const parsed = parseInt(inputValue, 10);
        if (isNaN(parsed)) {
            setInputValue(String(Math.round(erStore.scale * 100)));
        } else {
            applyScale(parsed);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            e.currentTarget.blur();
        }
    };

    const stopPropagation = (e: React.MouseEvent | React.KeyboardEvent) => e.stopPropagation();

    return (
        <div 
            className="er_zoom_controls" 
            onClick={stopPropagation}
            onMouseDown={stopPropagation}
            onDoubleClick={stopPropagation}
        >
            <button className="er_zoom_btn" onClick={handleZoomOut} title="Отдалить (-20%)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    <line x1="8" y1="11" x2="14" y2="11"></line>
                </svg>
            </button>
            
            <div className="er_zoom_input_wrapper" title="Текущий масштаб (введите значение)">
                <input 
                    className="er_zoom_input"
                    value={inputValue}
                    onChange={handleInputChange}
                    onBlur={handleBlur}
                    onKeyDown={handleKeyDown}
                    maxLength={3}
                />
                <span className="er_zoom_percent">%</span>
            </div>
            
            <button className="er_zoom_btn" onClick={handleZoomIn} title="Приблизить (+20%)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    <line x1="11" y1="8" x2="11" y2="14"></line>
                    <line x1="8" y1="11" x2="14" y2="11"></line>
                </svg>
            </button>
        </div>
    );
});