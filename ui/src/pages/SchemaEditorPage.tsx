import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import './css/SchemaEditorPage.css';
import profilePic from '@/assets/user.png';
import show from '@/assets/view.png';
import hide from '@/assets/hidden.png';

const FAKE_VERSIONS = [
    { id: 1, name: 'v1.0 - Initial', date: '12.02.2026' },
    { id: 2, name: 'v1.1 - Added Logic', date: '13.02.2026' },
    { id: 3, name: 'v1.2 - Bugfix', date: '14.02.2026' },
    { id: 4, name: 'v2.0 - Release', date: '15.02.2026' },
];

const FAKE_USERS = [
    { id: 1, email: 'admin@test.com' },
    { id: 2, email: 'designer_1@test.com' },
    { id: 3, email: 'viewer_john@test.com' },
    { id: 4, email: 'dev_ops@test.com' },
    { id: 5, email: 'manager@test.com' },
    { id: 6, email: 'guest_user@test.com' },
];

export const SchemaEditorPage = observer(() => {
    const [isSidebarOpen, setSidebarOpen] = useState(false);
    const [isUsersOpen, setUsersOpen] = useState(true); // Состояние для списка пользователей
    const schemaName = "Система управления складом v2";

    return (
        <div className="schema_page__container">
            <header className="schema_page__header">
                <h2 className='schema_page__name'>{schemaName}</h2>
                <div className="schema_controls">
                    <button className="btn_save">Сохранить</button>
                    <button className="btn_versionate">Сохранить версию</button>
                </div>
            </header>

            <div className="schema_page__workspace">
                {/* ЛЕВАЯ ПАНЕЛЬ (ВЕРСИИ) */}
                <aside className={`versions_sidebar ${isSidebarOpen ? 'open' : ''}`}>
                    <div className="sidebar_toggle" onClick={() => setSidebarOpen(!isSidebarOpen)}>
                        {isSidebarOpen ? '◀' : '▶'}
                    </div>
                    <div className="sidebar_content">
                        <h3>Версии</h3>
                        {FAKE_VERSIONS.map(v => (
                            <div key={v.id} className="version_item">
                                <span className="version_name">{v.name}</span>
                                <span className="version_date">{v.date}</span>
                            </div>
                        ))}
                    </div>
                </aside>

                {/* ЦЕНТРАЛЬНАЯ ЧАСТЬ (КАНВАС) */}
                <main className="schema_canvas_area">
                    <div className="canvas_placeholder">
                        <span>Здесь будет канвас...</span>
                    </div>

                    {/* ПРАВАЯ ПАНЕЛЬ (ПОЛЬЗОВАТЕЛИ) - ТЕПЕРЬ ABSOLUTE */}
                    <aside className={`users_overlay ${!isUsersOpen ? 'collapsed' : ''}`}>
                        <div className="users_list_header">
                            <span>В сети ({FAKE_USERS.length})</span>
                            {isUsersOpen ? (
                                <img src={hide} className='show_hide_logo' onClick={() => setUsersOpen(false)} />
                            ) : (
                                <img src={show} className='show_hide_logo' onClick={() => setUsersOpen(true)} />
                            )}
                        </div>
                        {isUsersOpen && (
                            <div className="users_scroll_container">
                                {FAKE_USERS.map(user => (
                                    <div key={user.id} className="user_element">
                                        <img src={profilePic} alt="user" />
                                        <span className="user_email">{user.email}</span>
                                    </div>
                                ))}
                            </div>
                        )}  
                    </aside>
                </main>
            </div>
        </div>
    );
});