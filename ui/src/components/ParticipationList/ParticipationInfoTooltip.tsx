import type { AuthorityType, Participation } from "@/model/Participation"
import './css/ParticipationInfoTooltip.css'
import { observer } from "mobx-react-lite";
import { createPortal } from "react-dom";
import { useEffect, useMemo, useState, type ChangeEvent, type MouseEvent } from "react";
import { participationsStore } from "@/store/ParticipationStore";

interface ParticipationInfoTooltipProps {
    participation: Participation;
    top: number;
    left: number;
    cancelTimeout: () => void;
    onLeave: () => void;
}

const changeableAuthorities: AuthorityType[] = ['READ_SCHEME', 'SNAPSHOT_VERSION', 'CHANGE_HEAD', 'DELETE_VERSIONS', 'INVITE_USERS', 'MODIFY_SCHEME'];
const authorityLabels: Partial<Record<AuthorityType, string>> = {
    'READ_SCHEME': 'Просматривать схему',
    'SNAPSHOT_VERSION': 'Создание новых версий',
    'CHANGE_HEAD': 'Изменение текущей версии',
    'DELETE_VERSIONS': 'Удаление версий',
    'INVITE_USERS': 'Приглашение пользователей',
    'MODIFY_SCHEME': 'Изменение схемы',
};

export const ParticipationInfoTooltip = observer(({left, top, participation, cancelTimeout, onLeave}: ParticipationInfoTooltipProps) => {
    const [newUserAuthorities, setNewUserAuthorities] = useState(participation.authorities);
    const [canUpdate, setCanUpdate] = useState(false);

    const currentUserAuthoritySet = useMemo(() => {
        console.debug('Current user authorities update');
        return new Set(participation.authorities);
    }, [participation.authorities]);

    const isOwner = useMemo(() => {
        return new Set(participation.authorities).has('ALL');
    }, [participation.authorities]);

    const canChangeAuthorities = useMemo(() => {
        return participationsStore.authorities?.some(au => au === 'ALL') ?? false;
    }, [participationsStore.authorities]);

    useEffect(() => {
        if (newUserAuthorities.length !== currentUserAuthoritySet.size) {
            setCanUpdate(true);
            return;
        }

        setCanUpdate(newUserAuthorities.some(au => !participation.authorities.includes(au)));
    }, [newUserAuthorities]);

    const handleAuthorityChange = (au: AuthorityType, checked: boolean) => {
        let updatedAuthorities: AuthorityType[];
        
        if (checked) {
            updatedAuthorities = [...newUserAuthorities, au];
        } else {
            updatedAuthorities = newUserAuthorities.filter(nua => nua !== au);
        }
        
        setNewUserAuthorities(updatedAuthorities);
    };

    const handleUpdateAuthorities = async () => {
        if (participationsStore.isLoading) {
            return;
        }
        await participationsStore.grantUser(participation, newUserAuthorities);
    }

    return createPortal(
        <div className="participation_info_tooltip__container" 
            style={{ top, left }}
            onMouseEnter={cancelTimeout}
            onMouseLeave={onLeave}
            onClick={(e: MouseEvent) => {
                e.stopPropagation();
            }}
            >
            <h3 className="tooltip_header">{participation.user.email}</h3>

            <div className="tooltip_header_separator" />

            <div className="tooltip_authorities_list">
                {isOwner ? (
                    <span className="tooltip_user_creator_text">Пользователь является владельцем схемы</span>
                ) : (
                    changeableAuthorities.map((au) => (
                        <label className="tooltip_authority_container" key={au} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginBottom: '4px' }}>
                            <input type="checkbox"
                                disabled={!canChangeAuthorities || au === 'READ_SCHEME'}
                                checked={newUserAuthorities.includes(au)}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => handleAuthorityChange(au, e.target.checked)}
                            />
                            <span className="tooltip_authority_label">
                                {authorityLabels[au] || au}
                            </span>
                        </label>
                    ))
                )}
            </div>

            {canUpdate ? (
                <div className="save_btn_holder">
                    <button
                        disabled={participationsStore.isLoading} 
                        onClick={handleUpdateAuthorities} 
                        className="save_btn"
                    >
                        { participationsStore.isLoading ? 'Обновление...' : 'Обновить права' } 
                    </button>
                </div>
            ) : null}
        </div>,
        document.body
    );
});