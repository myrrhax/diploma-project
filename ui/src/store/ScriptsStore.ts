import { makeAutoObservable, runInAction } from "mobx";
import type { GenType, Script, ScriptType } from "@/model/SchemaTypes";
import { scriptsApi } from "@/api/ScriptsApiService";
import { eventsStore } from "./EventsStore";
import { versionsStore } from "./VersionsStore";
import { erStore } from "./ERStore";

class ScriptsStore {
    isOpen: boolean = false;
    isCreateModalOpen: boolean = false;
    scripts: Script[] = []; 
    isLoading: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    async openScriptsModal() {
        runInAction(() => {
            this.isLoading = true;    
        });

        await versionsStore.setSchema(erStore.schemaId);
        runInAction(() => {
            this.isLoading = false;
            this.isOpen = true;    
        });
    }

    openCreateScriptModal() {
        this.isOpen = false;
        this.isCreateModalOpen = true;
    }

    closeCreateScriptsModal() {
        this.isOpen = true;
        this.isCreateModalOpen = false;
    }

    closeModal() {
        this.isOpen = false;
        this.scripts = [];
    }

    async generateScript(versionId: number, type: ScriptType, generatedType: GenType, fromVersionId: number | null) {
        try {
            let data;
            if (generatedType === 'FULL') {
                data = await scriptsApi.generateFullScript(versionId, type);
            } else {
                if (!fromVersionId) {
                    throw new Error('Migration must have fromVersionId');
                }
                data = await scriptsApi.generateMigrationScript(versionId, fromVersionId, type);
            }
            if ('id' in data) {
                this.scripts = [...this.scripts, data];
            } else {
                eventsStore.addError(data.message);
            }
        } catch (e: any) {
            eventsStore.addError('Не удалось создать скрипт');
        }
    }

    async loadScripts(schemaId: string) {
        if (this.isLoading) {
            return;
        }
        runInAction(() => {
            this.isLoading = true;
        });
        
        this.scripts = await scriptsApi.loadScripts(schemaId);

        runInAction(() => {
            this.isLoading = false;
        });
    }
}

export const scriptsStore = new ScriptsStore();