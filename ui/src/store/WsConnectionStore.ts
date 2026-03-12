import { makeAutoObservable } from "mobx";

class WsConnectionStore {
    isConnected: boolean = false;
    connectionErrorMessage: string | null = null;
    
    constructor() {
        makeAutoObservable(this);
    }
}

export const wsConnectionStore = new WsConnectionStore();