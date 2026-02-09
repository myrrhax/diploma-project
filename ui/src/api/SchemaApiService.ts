import $api from "./AxiosClient";
import type { Schema } from "../model/SchemaTypes";
import { schemaStore } from "../store/SchemaStore";
import { AbstractApiService } from "./AbstractApiService";
import type ErrorResponse from "../model/ErrorResponse";

class SchemaApiService extends AbstractApiService {
    async loadUserSchemas(takeParticipation?: boolean, query?: string): Promise<ErrorResponse | null> {
        schemaStore.setLoading(true);
        try {
            const response = await $api.get<Schema[]>('/schema', {
                params: {
                    takeParticipation: takeParticipation ?? true,
                    query: query ?? ''
                }
            });

            if (response.status === 200) {
                schemaStore.setSchemas(response.data);
            }

            return null;
        } catch (e: any) {
            console.error('Failed to fetch user`s schemas');
            schemaStore.setSchemas([]);
            return this.processApiError(e);
        } finally {
            schemaStore.setLoading(false);
        }
    }
}

export const schemaApi = new SchemaApiService();