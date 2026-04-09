import type { Script, ScriptType } from "@/model/SchemaTypes";
import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";
import type ErrorResponse from "@/model/ErrorResponse";

class ScriptApiService extends AbstractApiService {
    async loadScripts(schemaId: string): Promise<Script[]> {
        try {
            const response = await $api.get<Script[]>('/scripts?scheme_id=' + schemaId);
            console.log(response.data);
            return response.data;
        } catch (e: any) {
            console.error('Failed to fetch schema scripts', e);
            return [];
        }
    }

    async generateMigrationScript(versionId: number, fromVersionId: number, type: ScriptType): Promise<Script | ErrorResponse> {
        try {
            const response = await $api.post<Script>('/scripts/generate-migration', {
                versionId,
                fromVersionId,
                type
            });

            return response.data;
        } catch (e: any) {
            console.error('Failed to generate migration', e);
            const error = this.processApiError(e);

            return error;
        }
    }

    async generateFullScript(versionId: number, type: ScriptType): Promise<Script | ErrorResponse> {
        try {
            const response = await $api.post<Script>('/scripts/generate-script', {
                versionId,
                type
            });

            return response.data;
        } catch (e: any) {
            console.error('Failed to generate migration', e);
            const error = this.processApiError(e);

            return error;
        }
    }
}

export const scriptsApi = new ScriptApiService();