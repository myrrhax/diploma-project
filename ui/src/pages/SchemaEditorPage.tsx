import { useEffect, useMemo, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useParams } from 'react-router-dom';
import { erStore } from '@/store/ERStore';
import { Sidebar } from '@/components/Sidebar/Sidebar';
import { UsersOverlay } from '@/components/UsersOverlay/UsersOverlay';
import { ERDiagram } from '@/components/ERElements/ERDiagram';
import { OverlaySpinner } from '@/components/SpinnerLoader/SpinnerLoader';
import { schemaSocketService } from '@/api/SchemaSocketService';
import { versionsStore } from '@/store/VersionsStore';
import { SaveVersionModal } from '@/components/SaveVersionModal/SaveVersionModal';
import { wsConnectionStore } from '@/store/WsConnectionStore';
import { ParticipationList } from '@/components/ParticipationList/ParticipationList';
import { participationsStore } from '@/store/ParticipationStore';
import { InviteModal } from '@/components/ParticipationList/InviteModal';
import { scriptsStore } from '@/store/ScriptsStore';
import './css/SchemaEditorPage.css';
import { ScriptsModal } from '@/components/ScriptsModal/ScriptsModal';

interface SchemaEditorPageProps {
    isReadonly?: boolean
}

export const SchemaEditorPage = observer(({ isReadonly = false }: SchemaEditorPageProps) => {
    const { id, versionId } = useParams<{ id: string, versionId?: string }>();
    const [isSidebarOpen, setSidebarOpen] = useState(false);
    const [isUsersOpen, setUsersOpen] = useState(true);
    const { schema, state, isLoading } = erStore;
    const { isConnected } = wsConnectionStore;
    const { authorities, onlineUsers } = participationsStore;

    const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);

    const isEditable = useMemo(() => {
        return !isReadonly && schema?.currentVersion?.isWorkingCopy;
    }, [isReadonly, schema]);

    const canVersionate = useMemo(() => {
        return !isReadonly && schema?.currentVersion?.isWorkingCopy && authorities?.some(au => au === 'ALL' || au === 'VERSION');
    }, [isReadonly, schema, authorities]);

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
                await participationsStore.loadParticipationInfo(id);
            }
        };

        initEditor();
        return () => {
            isMounted = false;
            schemaSocketService.disconnect();
            erStore.setSchema(null);
            participationsStore.clear();
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
        if (schema) {
            await versionsStore.saveVersion(schema.id, tag);
        }
        setIsSaveModalOpen(false);
    }

    const onLeaveClick = async () => {
        if (participationsStore.isLoading) {
            return;
        }
        await participationsStore.leave();
    }
    
    return (
        <div className="schema_page__container">
            <ParticipationList />
            <InviteModal />
            {isEditable && (
                <SaveVersionModal
                    isOpen={isSaveModalOpen}
                    onSave={onSave}
                    onClose={() => setIsSaveModalOpen(false)} 
                />
            )}
            {isLoading || (isEditable && !isConnected) ? (
                <OverlaySpinner text='Загрузка...' />
            ) : state != null ? (
                <>
                    <header className="schema_page__header">
                        <div className="header_left">
                            <h2 className="schema_page__name">{schema?.name}</h2>
                        </div>
                            <div className="schema_controls">
                                <button className='btn_primary' onClick={() => scriptsStore.openScriptsModal()}>
                                    Скрипты
                                </button>
                                <button className='btn_primary' onClick={() => participationsStore.openListModal(id)}>
                                    Участники
                                </button>
                                {canVersionate ? (
                                    <button 
                                        className="btn_primary" 
                                        onClick={() => setIsSaveModalOpen(true)}
                                    >
                                        Сохранить версию
                                    </button>
                                ) : null}
                                <button className='btn_leave' onClick={onLeaveClick}>
                                    Покинуть схему
                                </button>
                            </div>
                    </header>

                    <div className="schema_page__workspace">
                        {isReadonly ? null : (
                            <Sidebar 
                                isOpen={isSidebarOpen} 
                                changeVisibleCallback={toggleSidebar} 
                            />
                        )}
                        
                        <main className="schema_canvas_area">
                            <ERDiagram />

                            {isEditable && (
                                <UsersOverlay 
                                    isUsersOpen={isUsersOpen} 
                                    users={onlineUsers} 
                                    closeCallback={setUsersOpen} 
                                />
                            )}
                        </main>
                    </div>

                    {scriptsStore.isOpen ? (
                        <ScriptsModal />
                    ) : null}
                </>
            ) : (
                <div>Схема не найдена или пуста</div>
            )}
        </div>
    );
});