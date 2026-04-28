import { makeAutoObservable } from "mobx";
import type { Schema } from "../model/SchemaTypes";

class SchemaStore {
    schemas: Schema[] | null = null;
    currentSchema: Schema | null = null;
    isLoading: boolean = false;
    isCreateModalOpen: boolean = false;

    constructor() {
        makeAutoObservable(this);
    }

    public addSchema(schema: Schema) {
        if (!this.schemas) {
            this.schemas = [];
        }
        this.schemas.push(schema);
    }

    public setSchemas(schemas: Schema[]) {
        this.schemas = schemas;
    }

    public setCurrentSchema(schema: Schema | null) {
        this.currentSchema = schema;
    }

    public setLoading(isLoading: boolean) {
        this.isLoading = isLoading;
    }
}

export const schemaStore = new SchemaStore();