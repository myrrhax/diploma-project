import { makeAutoObservable } from "mobx";
import { erStore } from "./ERStore";

class TableDeleteStore {
    isOpen = false;
    tableIdToDelete: string | null = null;
    tableContextMenu = { visible: false, x: 0, y: 0, tableId: '' };

    constructor() {
        makeAutoObservable(this);
    }

    open() {
        if (this.tableContextMenu.tableId) {
            this.tableIdToDelete = this.tableContextMenu.tableId;
            this.isOpen = true;
        }
    }

    close() {
        this.isOpen = false;
        this.tableIdToDelete = null;
    }

    openTableContextMenu(x: number, y: number, tableId: string) {
        this.tableContextMenu = { visible: true, x, y, tableId };
    }

    closeTableContextMenu() {
        this.tableContextMenu.visible = false;
    }

    confirm() {
        if (this.tableIdToDelete) {
            erStore.deleteTable(this.tableIdToDelete);
        }
        this.close();
    }
}

export const tableDeleteStore = new TableDeleteStore();