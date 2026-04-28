import { makeAutoObservable } from "mobx";
import { schemaApi } from "@/api/SchemaApiService";
import { schemaStore } from "./SchemaStore";

class CreateSchemaModalStore {
    name: string = '';
    error: string | null = null;
    isCreateModalOpen: boolean = false;
    isLoading: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    public async createSchema(name: string) {
        this.setLoading(true);
        try {            
            const response = await schemaApi.createSchema(name);

            if ('id' in response) {
                this.closeCreateModal();
                schemaStore.addSchema(response);
            } else {
                if (response.errors && response.errors.has('name')) {
                    this.setError(response.errors.get('name')!.join(', '));
                } else {
                    this.setError(response.message);
                }
            }            
        } catch (error) {
            console.error("Ошибка при создании схемы", error);
        } finally {
            this.setLoading(false);
        }
    }

    public setName(name: string) {
        this.name = name;
    }

    public setError(error: string | null) {
        this.error = error;
    }

    public openCreateModal() {
        this.isCreateModalOpen = true;
    }

    public closeCreateModal() {
        this.error = null;
        this.name = '';
        this.isCreateModalOpen = false;
    }

    private setLoading(loading: boolean) {
        this.isLoading = loading;
    }
}

export const createSchemaModalStore = new CreateSchemaModalStore();