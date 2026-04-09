import { makeAutoObservable, runInAction } from "mobx";
import type { Script } from "@/model/SchemaTypes";
import { scriptsApi } from "@/api/ScriptsApiService";

class ScriptsStore {
    isOpen: boolean = false;
    scripts: Script[] = []; 
    isLoading: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    openScriptsModal() {
        this.isOpen = true;
    }

    closeModal() {
        this.isOpen = false;
        this.scripts = [];
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