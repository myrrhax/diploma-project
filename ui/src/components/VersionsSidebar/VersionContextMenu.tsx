import { useEffect, useRef, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { contextMenuStore } from '@/store/VersionContextMenuStore';
import { versionsStore } from '@/store/VersionsStore'; 
import { createPortal } from 'react-dom';
import './css/VersionContextMenu.css';

export const VersionContextMenu = observer(() => {
    const { isOpen, position, version } = contextMenuStore;
    const menuRef = useRef<HTMLDivElement>(null);
    const [activeSubMenu, setActiveSubMenu] = useState<'generate' | 'diff' | null>(null);

    useEffect(() => {
        if (!isOpen) return;

        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                contextMenuStore.close();
            }
        };

        const handleScroll = () => contextMenuStore.close();

        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('scroll', handleScroll, true); 

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('scroll', handleScroll, true);
        };
    }, [isOpen]);

    if (!isOpen || version === null) return null;

    const handleAction = (action: () => void) => {
        action();
        contextMenuStore.close();
        setActiveSubMenu(null);
    };

    const onView = () => console.log('View:', version.versionId);
    const onRollback = () => console.log('Rollback:', version.versionId);
    const onGenerate = (format: string) => console.log(`Generate ${format}:`, version.versionId);
    const onDiff = (format: string) => console.log(`Diff ${format}:`, version.versionId);

    return createPortal(
        <div 
            ref={menuRef}
            className="custom-context-menu"
            style={{ top: position.y, left: position.x }}
            onContextMenu={(e) => e.preventDefault()} 
        >
            <div className="menu-item" onClick={() => handleAction(onView)}>
                Просмотреть версию
            </div>
            <div className="menu-item" onClick={() => handleAction(onRollback)}>
                Откатиться К
            </div>
            
            <div className="menu-divider" />

            <div 
                className="menu-item has-submenu"
                onMouseEnter={() => setActiveSubMenu('generate')}
                onMouseLeave={() => setActiveSubMenu(null)}
            >
                Создать на основании
                <span className="submenu-arrow">▶</span>
                {activeSubMenu === 'generate' && (
                    <div className="submenu">
                        <div className="menu-item" onClick={() => handleAction(() => onGenerate('SQL'))}>SQL</div>
                        <div className="menu-item" onClick={() => handleAction(() => onGenerate('PNG'))}>PNG</div>
                        <div className="menu-item" onClick={() => handleAction(() => onGenerate('Markdown'))}>Markdown</div>
                        <div className="menu-item" onClick={() => handleAction(() => onGenerate('Liquibase'))}>Liquibase</div>
                    </div>
                )}
            </div>

            <div 
                className="menu-item has-submenu"
                onMouseEnter={() => setActiveSubMenu('diff')}
                onMouseLeave={() => setActiveSubMenu(null)}
            >
                Создать по изменениям
                <span className="submenu-arrow">▶</span>
                {activeSubMenu === 'diff' && (
                    <div className="submenu">
                        <div className="menu-item" onClick={() => handleAction(() => onDiff('SQL'))}>SQL</div>
                        <div className="menu-item" onClick={() => handleAction(() => onDiff('Markdown'))}>Markdown</div>
                        <div className="menu-item" onClick={() => handleAction(() => onDiff('Liquibase'))}>Liquibase</div>
                    </div>
                )}
            </div>

            <div className="menu-divider" />

            <div className="menu-item item-danger" onClick={() => versionsStore.deleteVersion(version)}>
                Удалить версию
            </div>
        </div>,
        document.body
    );
});