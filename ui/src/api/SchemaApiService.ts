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

    async fetchSchemaById(id: string): Promise<Schema> {
        try {
            const response = await $api.get<Schema>('/schema/' + id);
            return response.data;
        } catch (e: any) {
            this.processApiError(e);
            throw e;
        }
    }

    async createSchema(name: string): Promise<Schema | ErrorResponse> {
        try {
            const response = await $api.post<Schema | ErrorResponse>('/schema', {
                name: name
            });

            return response.data;
        } catch (e: any) {
            const errorResponse = this.processApiError(e);

            if (errorResponse) {
                return errorResponse;
            }
            throw e;
        }
    }
}

export const schemaApi = new SchemaApiService();