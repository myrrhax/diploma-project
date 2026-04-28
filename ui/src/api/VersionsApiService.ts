import type { Version } from "@/model/SchemaTypes";
import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";

class VersionsApiService extends AbstractApiService {
    async loadVersions(schema: string): Promise<Version[]> {
        try {
            const response = await $api.get<Version[]>("/versions/schema/" + schema);
            console.log('Received:', response.data)
            return response.data;
        } catch(e: any) {
            return [];
        }
    }

    async loadById(id: number): Promise<Version | null> {
        try {
            const response = await $api.get<Version>("/versions/" + id);
            
            return response.data;
        } catch(e: any) {
            return null;
        }
    }
}

export const versionsApi = new VersionsApiService();