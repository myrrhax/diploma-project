import type { Script, Version } from "@/model/SchemaTypes";
import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";

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
}

export const scriptsApi = new ScriptApiService();