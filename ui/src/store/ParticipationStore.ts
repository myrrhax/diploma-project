import { participationApiService } from "@/api/ParticipationApiService";
import type { AuthorityType } from "@/model/Participation";
import { makeAutoObservable, runInAction } from "mobx";
import type { Participation } from "@/model/Participation";

class ParticipationsStore {
    authorities: AuthorityType[] | null = null;
    participations: Participation[] = [];
    
    isListModalOpen: boolean = false;
    currentSchemaId: string | null = null;
    
    isInviteModalOpen: boolean = false;
    isLoading: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    async loadParticipationInfo(schemaId: string) {
        const participationInfo = await participationApiService.loadParticipationInfo(schemaId);
        runInAction(() => {
            this.authorities = participationInfo.authorities;
        });
    }

    openListModal(schemaId: string) {
        this.currentSchemaId = schemaId;
        this.isListModalOpen = true;
        this.getParticipations(schemaId);
    }

    closeListModal() {
        this.isListModalOpen = false;
    }

    async getParticipations(schemaId: string) {
        this.isLoading = true;
        try {
            const data = await participationApiService.fetchParticipations(schemaId);
            runInAction(() => {
                this.participations = data;
                this.isLoading = false;
            });
        } catch (error) {
            runInAction(() => { this.isLoading = false; });
        }
    }

    openInviteModal(schemaId: string) {
        this.currentSchemaId = schemaId;
        this.isInviteModalOpen = true;
    }

    closeInviteModal() {
        this.isInviteModalOpen = false;
    }

    async sendInvite(email: string, authorities: AuthorityType[]) {
        
    }

    clear() {
        this.authorities = null;
        this.participations = [];
        this.currentSchemaId = null;
        this.isListModalOpen = false;
        this.isInviteModalOpen = false;
    }
}

export const participationsStore = new ParticipationsStore();