import { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useParams } from 'react-router-dom';
import { erStore } from '@/store/ERStore';
import { VersionsSidebar } from '@/components/VersionsSidebar/VersionsSidebar';
import { UsersOverlay } from '@/components/UsersOverlay/UsersOverlay';
import { ERDiagram } from '@/components/er/ERDiagram';
import './css/SchemaEditorPage.css';
import { schemaSocketService } from '@/api/SchemaSocketService';
import { ErrorToasts } from '@/components/ErrorToast/ErrorToast';
import { versionsStore } from '@/store/VersionsStore';
import { SaveVersionModal } from '@/components/SaveVersionModal/SaveVersionModal';

const FAKE_USERS = [
    { id: '1', email: 'admin@test.com', isConfirmed: true },
    { id: '2', email: 'designer_1@test.com', isConfirmed: true },
];

export const SchemaEditorPage = observer(() => {
    const { id } = useParams<{ id: string }>();
    const [isSidebarOpen, setSidebarOpen] = useState(false);
    const [isUsersOpen, setUsersOpen] = useState(true);
    const { schema, state, isLoading } = erStore;

    const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);

    if (!id) return;

    useEffect(() => {
        if (!id) return;

        let isMounted = true;
        const initEditor = async () => {
            schemaSocketService.connect();
            await erStore.loadSchema(id);
            if (isMounted && !erStore.isAccessDenied && erStore.state) {
                schemaSocketService.joinSchema(id);
            }
        };

        initEditor();
        return () => {
            isMounted = false;
            schemaSocketService.leaveSchema();
            erStore.setSchema(null);
        };
    }, [id]);

    const toggleSidebar = async () => {
        if (!id)
            return;
        const toggle = !isSidebarOpen;
        setSidebarOpen(toggle);
        if (toggle) {
            await versionsStore.setSchema(id);
        }
    }

    const onSave = async (tag: string) => {
        await versionsStore.saveVersion(tag);
        setIsSaveModalOpen(false);
    }
    
    return (
        <div className="schema_page__container">
            <ErrorToasts />
            <SaveVersionModal
                isOpen={isSaveModalOpen}
                onSave={onSave}
                onClose={() => setIsSaveModalOpen(false)} 
            />
            {isLoading ? (
                <div>Загрузка...</div>
            ) : state != null ? (
                <>
                    <header className="schema_page__header">
                        <div className="header_left">
                            <button 
                                className="btn_sidebar_toggle" 
                                onClick={() => toggleSidebar()}
                            >
                                ☰
                            </button>
                            <h2 className="schema_page__name">{schema?.name}</h2>
                        </div>
                        <div className="schema_controls">
                            <button className="btn_secondary">История</button>
                            <button 
                                className="btn_primary" 
                                onClick={() => setIsSaveModalOpen(true)}
                            >
                                Сохранить версию
                            </button>
                        </div>
                    </header>

                    <div className="schema_page__workspace">
                        <VersionsSidebar 
                            isOpen={isSidebarOpen} 
                            changeVisibleCallback={toggleSidebar} 
                        />

                        <main className="schema_canvas_area">
                            {/* ИНТЕРАКТИВНЫЙ КАНВАС */}
                            <ERDiagram />

                            <UsersOverlay 
                                isUsersOpen={isUsersOpen} 
                                users={FAKE_USERS} 
                                closeCallback={setUsersOpen} 
                            />
                        </main>
                    </div>
                </>
            ) : (
                <div>Схема не найдена или пуста</div>
            )}
        </div>
    );
});