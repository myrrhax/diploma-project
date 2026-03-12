import { participationApiService } from "@/api/ParticipationApiService";
import type { AuthorityType } from "@/model/Participation";
import { makeAutoObservable, runInAction } from "mobx";

class ParticipationsStore {
    authorities: AuthorityType[] | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async loadParticipationInfo(schemaId: string) {
        const participationInfo = await participationApiService.loadParticipationInfo(schemaId);
        runInAction(() => {
            this.authorities = participationInfo.authorities;
        });
    }

    clear() {
        this.authorities = null;
    }
}

export const participationsStore = new ParticipationsStore();