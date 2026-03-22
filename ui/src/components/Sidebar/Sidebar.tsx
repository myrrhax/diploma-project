import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { VersionsContent } from './VersionsContent';
import './css/Sidebar.css';

interface SidebarProps {
    isOpen: boolean;
    changeVisibleCallback: ((open: boolean) => void);
}

type SidebarTab = 'versions' | 'tables';

export const Sidebar = observer(({ isOpen, changeVisibleCallback }: SidebarProps) => {
    const [activeTab, setActiveTab] = useState<SidebarTab>('versions');

    return (
        <aside className={`schema-sidebar ${isOpen ? 'open' : ''}`}>
            <div className="sidebar-toggle" onClick={() => changeVisibleCallback(!isOpen)}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d={isOpen ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}/>
                </svg>
            </div>
            
            <div className="sidebar-main-content">
                <div className="sidebar-top-bar">
                    <select 
                        className="sidebar-select"
                        value={activeTab} 
                        onChange={(e) => setActiveTab(e.target.value as SidebarTab)}
                    >
                        <option value="versions">Версии</option>
                        <option value="tables">Описание таблиц</option>
                    </select>
                </div>

                <div className="sidebar-dynamic-area">
                    {activeTab === 'versions' && <VersionsContent />}
                    
                    {activeTab === 'tables' && (
                        <div className="sidebar-placeholder">
                            <p>Описание таблиц</p>
                        </div>
                    )}
                </div>
            </div>
        </aside>
    );
});