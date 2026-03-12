import { observer } from 'mobx-react-lite';
import { participationsStore } from '@/store/ParticipationStore';
import { type AuthorityType } from '@/model/Participation';
import './ParticipationList.css';

const AUTHORITY_TRANSLATIONS: Record<AuthorityType, string> = {
    'READ_SCHEME': 'Просмотр схемы',
    'MODIFY_SCHEME': 'Редактирование схемы',
    'SNAPSHOT_VERSION': 'Сохранение версий',
    'DELETE_VERSIONS': 'Удаление версий',
    'INVITE_USERS': 'Управление доступом',
    'CHANGE_HEAD': 'Изменение рабочей версии',
    'ALL': 'Владелец'
};

export const ParticipationList = observer(() => {
    const { isListModalOpen, currentSchemaId, isLoading, participations } = participationsStore;

    if (!isListModalOpen) return null;

    const handleInviteClick = () => {
        if (currentSchemaId) {
            participationsStore.openInviteModal(currentSchemaId);
        }
    };

    const handleClose = () => {
        participationsStore.closeListModal();
    };

    return (
        <div className="participation_modal_overlay" onClick={handleClose}>
            <div className="participation_modal_content" onClick={(e) => e.stopPropagation()}>
                <div className="participation_modal_header">
                    <h3 className="participation_title">Участники проекта</h3>
                    <button className="participation_close_btn" onClick={handleClose}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                {isLoading ? (
                    <div className="participation_loading">Загрузка...</div>
                ) : (
                    <div className="participation_list">
                        {participations.map((p, index) => (
                            <div key={p.user.id || index} className="participation_item">
                                <div className="participation_avatar">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
                                    </svg>
                                </div>

                                <span className="participation_email">{p.user.email}</span>

                                <div className="participation_tooltip">
                                    <div className="tooltip_header">Информация</div>
                                    <div className="tooltip_email">{p.user.email}</div>
                                    
                                    <div className="tooltip_auth_title">Права доступа:</div>
                                    <ul className="tooltip_auth_list">
                                        {p.authorities.map(auth => (
                                            <li key={auth} className="tooltip_auth_item">
                                                ✓ {AUTHORITY_TRANSLATIONS[auth] || auth}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                <button className="btn_primary participation_invite_btn" onClick={handleInviteClick}>
                    <span>+</span> Пригласить пользователя
                </button>
            </div>
        </div>
    );
});