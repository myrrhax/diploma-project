import { useEffect, useMemo, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { contextMenuStore } from '@/store/VersionContextMenuStore';
import { versionsStore } from '@/store/VersionsStore'; 
import { createPortal } from 'react-dom';
import './css/VersionContextMenu.css';
import { participationsStore } from '@/store/ParticipationStore';

export const VersionContextMenu = observer(() => {
    const { isOpen, position, version } = contextMenuStore;
    const menuRef = useRef<HTMLDivElement>(null);
    const { authorities } = participationsStore;

    const canVersion = useMemo(() => {
        return authorities?.some(au => au === 'ALL' || au === 'VERSION') ?? false;
    }, [authorities]);

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
    };

    const onView = () => {
        if (version.isWorkingCopy) {
            return;
        }
        const url = `/schema/${version.schemeId}/version/${version.versionId}`;
        window.open(url, '_blank', 'noopener,noreferrer');
    };
    const onRollback = () => {
        if (canVersion) {
            versionsStore.changeHead(version);
        }
    }
    const onDelete = () => {
        if (canVersion) {
            versionsStore.deleteVersion(version);
        }
    }

    return createPortal(
        <div 
            ref={menuRef}
            className="custom-context-menu"
            style={{ top: position.y, left: position.x }}
            onContextMenu={(e) => e.preventDefault()} 
        >
            {canVersion ? (
                <>
                    <div className="menu-item" onClick={() => handleAction(onView)}>
                        Просмотреть версию
                    </div>
                    <div className="menu-item" onClick={() => handleAction(onRollback)}>
                        Перейти к версии
                    </div>
                    <div className="menu-item item-danger" onClick={() => handleAction(onDelete)}>
                        Удалить версию
                    </div>
                </>
            ) : null}
        </div>,
        document.body
    );
});