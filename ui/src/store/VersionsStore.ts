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
            this.setVersions(null);
        } else {
            this.fetchSchemas();
        }
    }

    setVersions(versions: Version[] | null) {
        console.log('New versions: ', versions);
        this.versions = versions ?? [];
        this.currentVersion = versions?.find(v => v.isWorkingCopy) ?? null;
    }

    async saveVersion(schemaId: string, tag: string) {
        this.isLoading = true;
        schemaSocketService.saveVersion(schemaId, tag);
        this.isLoading = false;
    }

    async deleteVersion(version: Version) {
        console.log('Deleting version: ', version)
        this.isLoading = true;
        schemaSocketService.deleteVersion(version);
        this.isLoading = false;
    }

    changeHead(toVersion: Version) {
        if (!this.currentVersion) {
            console.error('No current version');
            return;
        }
        this.isLoading = true;
        schemaSocketService.changeHead(this.currentVersion, toVersion);
        this.isLoading = false;
    }

    private async fetchSchemas() {
        if (!this.schemaId) {
            return;
        }
        this.isLoading = true;
        this.setVersions(await versionsApi.loadVersions(this.schemaId));
        
        this.isLoading = false;
    }
}

export const versionsStore = new VersionsStore(); 