import type { Version } from '@/model/SchemaTypes';
import { makeAutoObservable } from 'mobx';

export interface ContextMenuPosition {
    x: number;
    y: number;
}

class ContextMenuStore {
    isOpen: boolean = false;
    position: ContextMenuPosition = { x: 0, y: 0 };
    version: Version | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    open(x: number, y: number, version: Version) {
        this.isOpen = true;
        this.position = { x, y };
        this.version = version;
    }

    close() {
        this.isOpen = false;
        this.version = null; 
    }
}

export const contextMenuStore = new ContextMenuStore();