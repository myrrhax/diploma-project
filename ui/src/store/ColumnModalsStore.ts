import { makeAutoObservable } from "mobx";
import { erStore } from "./ERStore";

class ColumnModalsStore {
    isOpen = false;
    tableId: string | null = null;
    colId: string | null = null;
    columnContextMenu = { visible: false, x: 0, y: 0, tableId: '', colId: '' };

    constructor() {
        makeAutoObservable(this);
    }

    openColumnContextMenu(x: number, y: number, tableId: string, colId: string) {
        this.columnContextMenu = { visible: true, x, y, tableId, colId };
    }

    closeColumnContextMenu() {
        this.columnContextMenu.visible = false;
    }

    open(tableId: string, colId: string) {
        this.tableId = tableId;
        this.colId = colId;
        this.isOpen = true;
    }

    close() {
        this.isOpen = false;
        this.tableId = null;
        this.colId = null;
    }

    confirm() {
        if (this.tableId && this.colId) {
            erStore.deleteColumn(this.tableId, this.colId);
        }
        this.close();
    }
}

export const columnModalsStore = new ColumnModalsStore();