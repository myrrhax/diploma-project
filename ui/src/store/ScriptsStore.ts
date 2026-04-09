import { makeAutoObservable, runInAction } from "mobx";
import type { GenType, Script, ScriptType } from "@/model/SchemaTypes";
import { scriptsApi } from "@/api/ScriptsApiService";

class ScriptsStore {
    isOpen: boolean = false;
    isCreateModalOpen: boolean = false;
    scripts: Script[] = []; 
    isLoading: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    openScriptsModal() {
        this.isOpen = true;
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

    generateScript(versionId: number, type: ScriptType, generatedType: GenType, fromVersionId: number | null) {
        throw new Error("Method not implemented.");
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