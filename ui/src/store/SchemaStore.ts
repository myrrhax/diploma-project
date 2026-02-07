import { makeAutoObservable } from "mobx";

class SchemaStore {
    schemas: SchemaDto[]
    constructor() {
        makeAutoObservable(this);
    }
}

export const schemaStore = new SchemaStore();