import { participationApiService } from "@/api/ParticipationApiService";
import type { AuthorityType } from "@/model/Participation";
import { makeAutoObservable, runInAction } from "mobx";
import type { Participation } from "@/model/Participation";
import { eventsStore } from "./EventsStore";
import type { User } from "@/model/User";

class ParticipationsStore {
    readonly SERVER_ERROR_MESSAGE = 'Ошибка на стороне сервера, попробуйте позже';

    authorities: AuthorityType[] | null = null;
    participations: Participation[] = [];
    
    isListModalOpen: boolean = false;
    currentSchemaId: string | null = null;
    
    isInviteModalOpen: boolean = false;
    isLoading: boolean = false;
    onlineUsers: User[] = [];

    constructor() {
        makeAutoObservable(this);
    }

    async loadParticipationInfo(schemaId: string) {
        const participationInfo = await participationApiService.loadParticipationInfo(schemaId);
        runInAction(() => {
            this.authorities = participationInfo.authorities;
        });
    }

    setOnlineUsers(users: User[]) {
        this.onlineUsers = users;
    }

    removeUser(user: User) {
        this.onlineUsers = this.onlineUsers.filter(u => u.id !== user.id);
    }

    removeUserById(userId: string) {
        this.onlineUsers = this.onlineUsers.filter(u => u.id !== userId);
    }

    addUser(user: User) {
        this.onlineUsers = [...this.onlineUsers.filter(u => u.id !== user.id), user];
    }

    openInviteModal() {
        this.isListModalOpen = false;
        this.isInviteModalOpen = true;
    }

    closeInviteModal() {
        this.isListModalOpen = true;
        this.isInviteModalOpen = false;
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

    async grantUser(participation: Participation, newAuthorities: AuthorityType[]) {
        if (!this.currentSchemaId) {
            return;
        }

        this.isLoading = true;
        let errorMessage;
        try {
            const error = await participationApiService.grant(this.currentSchemaId,
                participation.user.id,
                newAuthorities
            );
            if (error) {
                errorMessage = error.message;
            }
        } catch (e: any) {
            errorMessage = this.SERVER_ERROR_MESSAGE;
        }

        if (errorMessage) {
            runInAction(() => {
                eventsStore.addError(errorMessage);
                this.isLoading = false;
            });

            return;
        }

        runInAction(() => {
            this.isLoading = false;
            this.closeListModal();
        });
    }

    async sendInvite(email: string, authorities: AuthorityType[]): Promise<boolean> {
        if (!this.currentSchemaId) {
            return false;
        }

        try {
            this.isLoading = true;
            let errorMessage;

            try {
                const error = await participationApiService.sendInvitation(this.currentSchemaId, email, authorities);
                if (error) {
                    errorMessage = error.message;
                }
            } catch (e: any) {
                errorMessage = this.SERVER_ERROR_MESSAGE;
            }

            if (errorMessage) {
                runInAction(() => {
                    eventsStore.addError(errorMessage);
                    this.isLoading = false;
                });
               
                return false;
            }
            
            runInAction(() => {
                this.isLoading = false;
                alert(`Пользователь ${email} успешно приглашен!`);
                this.closeInviteModal();
            });

            return true;
        } catch (error) {
            runInAction(() => { this.isLoading = false; });
            alert("Ошибка при отправке приглашения");

            return false;
        }
    }

    async kickUser(userId: string, userEmail: string) {
        if (!this.currentSchemaId) {
            return;
        }
        runInAction(() => {
            this.isLoading = true;
        });
        
        try {
            await participationApiService.kick(this.currentSchemaId, userId);
            eventsStore.addInfo(`Пользователь ${userEmail} был исключен`)
        } catch(ex: any) {
            eventsStore.addError('Не удалось исключить пользователя ' + userEmail);
        }

        runInAction(() => {
            this.isLoading = false;
        });
    }

    async leave() {
        if (!this.currentSchemaId) {
            return;
        }

        runInAction(() => {
            this.isLoading = true;
        });

        try {
            await participationApiService.leaveSchema(this.currentSchemaId);
            eventsStore.addInfo('Вы покинули схему');
            window.location.href = '/';
            this.clear();
        } catch (e: any) {
            eventsStore.addError('Не удалось покинуть схему');
        }

        runInAction(() => { this.isLoading = false; });
    }

    clear() {
        this.authorities = null;
        this.participations = [];
        this.currentSchemaId = null;
        this.isListModalOpen = false;
        this.isInviteModalOpen = false;
        this.onlineUsers = [];
    }
}

export const participationsStore = new ParticipationsStore();