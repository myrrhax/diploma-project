import { versionsApi } from "@/api/VersionsApiService";
import type { Version } from "@/model/SchemaTypes";
import { makeAutoObservable } from "mobx";

class VersionsStore {
    isLoading: boolean = false;
    schemaId: string | null = null;
    versions: Version[] = [];

    constructor() {
        makeAutoObservable(this);
    }

    async setSchema(id: string | null) {
        if (this.schemaId === id) {
            return;
        }
        this.schemaId = id;
        if (!id) {
            this.versions = [];
        } else {
            this.isLoading = true;
            this.versions = await versionsApi.loadVersions(id);
            this.isLoading = false;
        }
    }
}

export const versionsStore = new VersionsStore(); 