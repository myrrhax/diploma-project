import { observer } from "mobx-react-lite";
import { useState, useMemo, useEffect } from "react";
import { z } from "zod";
import { participationsStore } from "@/store/ParticipationStore";
import type { AuthorityType } from "@/model/Participation";
import closeIcon from '@/assets/close.svg';
import './css/InviteModal.css';

const emailSchema = z.string().email("Введите корректный email адрес");

const assignableAuthorities: AuthorityType[] = [
    'VERSION', 
    'MODIFY_SCHEME',
    'INVITE_USERS',
    'GENERATE_SCRIPT'
];

const authorityLabels: Partial<Record<AuthorityType, string>> = {
    'VERSION': 'Версионирование',
    'MODIFY_SCHEME': 'Изменение схемы',
    'INVITE_USERS': 'Приглашать пользователей',
    'GENERATE_SCRIPT': 'Создавать скрипты'
};

export const InviteModal = observer(() => {
    const { isInviteModalOpen, authorities: currentUserAuthorities, isLoading } = participationsStore;

    const [email, setEmail] = useState("");
    const [emailError, setEmailError] = useState<string | null>(null);
    const allowedAuthorities = useMemo(() => {
        if (!currentUserAuthorities) return [];
        
        const isOwner = currentUserAuthorities.includes('ALL');
        console.log('IS OWNER', isOwner)

        return assignableAuthorities.filter(auth => isOwner || currentUserAuthorities.includes(auth) && auth !== 'INVITE_USERS');
    }, [currentUserAuthorities]);

    const [selectedAuthorities, setSelectedAuthorities] = useState<AuthorityType[]>(allowedAuthorities);

    useEffect(() => {
        if (isInviteModalOpen) {
            setSelectedAuthorities(allowedAuthorities);
        }
    }, [isInviteModalOpen, allowedAuthorities]);

    if (!isInviteModalOpen) return null;

    const handleAuthorityChange = (auth: AuthorityType, checked: boolean) => {
        if (checked) {
            setSelectedAuthorities(prev => [...prev, auth]);
        } else {
            setSelectedAuthorities(prev => prev.filter(a => a !== auth));
        }
    };

    const handleSubmit = async () => {
        const validationResult = emailSchema.safeParse(email);
        
        if (!validationResult.success) {
            setEmailError(validationResult.error.issues[0].message);
            return;
        }

        setEmailError(null);
        
        const finalAuthorities = Array.from(new Set([...selectedAuthorities, 'READ_SCHEME' as AuthorityType]));
        const isSuccess = await participationsStore.sendInvite(email, finalAuthorities);
        
        if (isSuccess) {
            setEmail("");
            setSelectedAuthorities([]);
        }
    };

    const handleClose = () => {
        setEmail("");
        setEmailError(null);
        setSelectedAuthorities([]);
        participationsStore.closeInviteModal();
    };

    return (
        <div className="invite_modal__overlay" onClick={handleClose}>
            <div className="invite_modal__container" onClick={e => e.stopPropagation()}>
                <div className="invite_modal__header">
                    <h3>Пригласить пользователя</h3>
                    <img src={closeIcon} onClick={handleClose} alt="Закрыть" />
                </div>

                <div className="invite_modal__body">
                    <div className="invite_modal__field_group">
                        <label className="invite_modal__label">Email пользователя</label>
                        <input
                            type="text"
                            value={email}
                            onChange={e => {
                                setEmail(e.target.value);
                                if (emailError) setEmailError(null);
                            }}
                            placeholder="user@example.com"
                            className={`invite_modal__input ${emailError ? 'invite_modal__input--error' : ''}`}
                        />
                        {emailError && <span className="invite_modal__error_text">{emailError}</span>}
                    </div>

                    <div className="invite_modal__field_group">
                        <label className="invite_modal__label">Права доступа</label>
                        {allowedAuthorities.length === 0 ? (
                            <span className="invite_modal__empty_text">У вас недостаточно прав для выдачи доступов.</span>
                        ) : (
                            <div className="invite_modal__checkbox_list">
                                {allowedAuthorities.map(auth => (
                                    <label key={auth} className="invite_modal__checkbox_item">
                                        <input
                                            type="checkbox"
                                            checked={selectedAuthorities.includes(auth)}
                                            onChange={e => handleAuthorityChange(auth, e.target.checked)}
                                        />
                                        <span className="invite_modal__checkbox_label">{authorityLabels[auth] || auth}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="invite_modal__footer">
                        <button className="invite_modal__btn_cancel" onClick={handleClose}>
                            Отмена
                        </button>
                        <button 
                            className="invite_modal__btn_submit"
                            onClick={handleSubmit}
                            disabled={isLoading}
                        >
                            {isLoading ? 'Отправка...' : 'Отправить'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
});