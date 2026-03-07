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

interface SchemaEditorPageProps {
    isReadonly?: boolean
}

export const SchemaEditorPage = observer(({ isReadonly = false }: SchemaEditorPageProps) => {
    const { id, versionId } = useParams<{ id: string, versionId?: string }>();
    const [isSidebarOpen, setSidebarOpen] = useState(false);
    const [isUsersOpen, setUsersOpen] = useState(true);
    const { schema, state, isLoading } = erStore;

    const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);

    const isEditable = !isReadonly && schema?.currentVersion?.isWorkingCopy === true;

    if (!id) return null;

    useEffect(() => {
        let isMounted = true;
        const initEditor = async () => {
            if (isReadonly && versionId) {
                const parsedVersionId = Number(versionId);
                if (!isNaN(parsedVersionId)) {
                    await erStore.loadSchemaWithVersion(parsedVersionId);
                }
            } else {
                schemaSocketService.connect();
                await erStore.loadSchema(id);
                if (isMounted && !erStore.isAccessDenied && erStore.state) {
                    schemaSocketService.joinSchema(id);
                }
            }
        };

        initEditor();
        return () => {
            isMounted = false;
            schemaSocketService.leaveSchema();
            erStore.setSchema(null);
        };
    }, [id, isReadonly, versionId]);

    const toggleSidebar = async () => {
        if (!id) return;
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
            {isEditable && (
                <SaveVersionModal
                    isOpen={isSaveModalOpen}
                    onSave={onSave}
                    onClose={() => setIsSaveModalOpen(false)} 
                />
            )}
            {isLoading ? (
                <div>Загрузка...</div>
            ) : state != null ? (
                <>
                    <header className="schema_page__header">
                        <div className="header_left">
                            {isReadonly ? null : (
                                <button 
                                    className="btn_sidebar_toggle" 
                                    onClick={() => toggleSidebar()}
                                >
                                    ☰
                                </button>
                            )}
                            <h2 className="schema_page__name">{schema?.name}</h2>
                        </div>
                        {isEditable && (
                            <div className="schema_controls">
                                <button className="btn_secondary">История</button>
                                <button 
                                    className="btn_primary" 
                                    onClick={() => setIsSaveModalOpen(true)}
                                >
                                    Сохранить версию
                                </button>
                            </div>
                        )}
                    </header>

                    <div className="schema_page__workspace">
                        {isReadonly ? null : (
                            <VersionsSidebar 
                                isOpen={isSidebarOpen} 
                                changeVisibleCallback={toggleSidebar} 
                            />
                        )}
                        
                        <main className="schema_canvas_area">
                            <ERDiagram />

                            {isEditable && (
                                <UsersOverlay 
                                    isUsersOpen={isUsersOpen} 
                                    users={FAKE_USERS} 
                                    closeCallback={setUsersOpen} 
                                />
                            )}
                        </main>
                    </div>
                </>
            ) : (
                <div>Схема не найдена или пуста</div>
            )}
        </div>
    );
});