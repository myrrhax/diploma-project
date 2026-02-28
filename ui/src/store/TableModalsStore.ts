import { makeAutoObservable } from "mobx";
import { erStore } from "./ERStore";

class TableModalsStore {
    isOpen = false;
    tableIdToDelete: string | null = null;
    
    isEditOpen = false;
    tableIdToEdit: string | null = null;

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

    confirm() {
        if (this.tableIdToDelete) {
            erStore.deleteTable(this.tableIdToDelete);
        }
        this.close();
    }

    openEdit() {
        if (this.tableContextMenu.tableId) {
            this.tableIdToEdit = this.tableContextMenu.tableId;
            this.isEditOpen = true;
        }
    }

    closeEdit() {
        this.isEditOpen = false;
        this.tableIdToEdit = null;
    }

    confirmEdit(newName: string | null, newDescription: string | null) {
        if (this.tableIdToEdit) {
            erStore.updateTable(this.tableIdToEdit, newName, newDescription);
        }
        this.closeEdit();
    }

    openTableContextMenu(x: number, y: number, tableId: string) {
        this.tableContextMenu = { visible: true, x, y, tableId };
    }

    closeTableContextMenu() {
        this.tableContextMenu.visible = false;
    }
}

export const tableModalsStore = new TableModalsStore();