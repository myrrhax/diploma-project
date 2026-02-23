import { makeAutoObservable } from "mobx";
import { v4 } from 'uuid';

interface ErrorMessage {
    id: string;
    text: string;
}

class ErrorsStore {
    readonly MAX_ERRORS = 5;
    readonly ERROR_TTL = 5000;

    errors: ErrorMessage[] = [];

    constructor() {
        makeAutoObservable(this);
    }

    addError(text: string) {
        const id = v4();
        this.errors.push({id: id, text: text});
        console.log(`Error ${id} was inserted`);
        if (this.errors.length > this.MAX_ERRORS) {
            this.errors.shift();
        }

        setTimeout(() => {
            this.removeError(id);
        }, this.ERROR_TTL)
    }

    removeError(id: string) {
        this.errors = this.errors.filter(err => err.id !== id);
    }
}

export const errorsStore = new ErrorsStore();