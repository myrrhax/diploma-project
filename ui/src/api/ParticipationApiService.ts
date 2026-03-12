import type { Participation } from "@/model/Participation";
import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";

class ParticipationApiService extends AbstractApiService {
    async confirmInvitation(invitationId: string): Promise<Participation> {
        try {
            const response = await $api.post<Participation>('/participations/confirm/' + invitationId);
            
            console.log('Response is', response);
            return response.data;
        } catch (e: any) {
            console.error('Failed to confirm invitation', e);

            throw e;
        }
    }

    async loadParticipationInfo(schemaId: string): Promise<Participation> {
        try {
            const response = await $api.get<Participation>('/participations/my/' + schemaId);

            return response.data;
        } catch (e: any) {
            console.error('Error while fetching participation info', e);
            throw e;
        }
    }

    async fetchParticipations(schemaId: string): Promise<Participation[]> {
        try {
            const response = await $api.get<Participation[]>('/participations/schema/' + schemaId);

            return response.data;
        } catch (e: any) {
            console.error('Error while fetching participation info', e);
            throw e;
        }
    }
}

export const participationApiService = new ParticipationApiService();