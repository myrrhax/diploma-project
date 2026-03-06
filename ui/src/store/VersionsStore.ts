import { schemaSocketService } from "@/api/SchemaSocketService";
import { versionsApi } from "@/api/VersionsApiService";
import type { Version } from "@/model/SchemaTypes";
import { makeAutoObservable } from "mobx";

class VersionsStore {
    isLoading: boolean = false;
    schemaId: string | null = null;
    versions: Version[] = [];
    currentVersion: Version | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    addVersion(v: Version) {
        console.log('New version received: ', v);
        this.fetchSchemas();
    }

    async setSchema(id: string | null) {
        if (this.schemaId === id) {
            return;
        }
        this.schemaId = id;
        if (!id) {
            this.versions = [];
            this.currentVersion = null;
        } else {
            this.fetchSchemas();
        }
    }

    async saveVersion(tag: string) {
        if (!this.schemaId) {
            return;
        }
        this.isLoading = true;
        schemaSocketService.saveVersion(this.schemaId, tag);
        this.isLoading = false;
    }

    private async fetchSchemas() {
        if (!this.schemaId) {
            return;
        }
        this.isLoading = true;
        const fromApi = await versionsApi.loadVersions(this.schemaId);
        this.versions = fromApi;
        console.log('Fetched versions: ', fromApi);
        this.currentVersion = this.versions.find(v => v.isWorkingCopy) ?? null;
        this.isLoading = false;
    }
}

export const versionsStore = new VersionsStore(); 