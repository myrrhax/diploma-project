import { makeAutoObservable } from "mobx";
import { erStore } from "./ERStore";
import { refKeyToString } from "@/utils/UtilFunctions";

type CursorMode = 'grab' | 'select';

interface SelectionBox {
    startX: number;
    startY: number;
    endX: number;
    endY: number;
}

class SelectionStore {
    mode: CursorMode = 'grab';
    selectedTableIds: Set<string> = new Set();
    selectedRefIds: Set<string> = new Set();
    selectionBox: SelectionBox | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    setMode(mode: CursorMode) {
        this.mode = mode;
    }

    toggleTable(id: string, multi: boolean) {
        if (this.mode === 'grab') {
            return;
        }
        if (multi) {
            if (this.selectedTableIds.has(id)) {
                this.selectedTableIds.delete(id);
            } else {
                this.selectedTableIds.add(id);
            }
        } else {
            this.clear();
            this.selectedTableIds.add(id);
        }
    }

    toggleReference(id: string, multi: boolean) {
        if (this.mode === 'grab') {
            return;
        }
        if (multi) {
            if (this.selectedRefIds.has(id)) {
                this.selectedRefIds.delete(id);
            } else {
                this.selectedRefIds.add(id);
            }
        } else {
            this.clear();
            this.selectedRefIds.add(id);
        }
    }

    clear() {
        this.selectedTableIds.clear();
        this.selectedRefIds.clear();
    }

    setSelectionBox(box: SelectionBox | null) {
        this.selectionBox = box;
    }

    updateSelectionBox(x: number, y: number) {
        if (this.selectionBox) {
            this.selectionBox.endX = x;
            this.selectionBox.endY = y;
        }
    }

    applyBoxSelection(ctrlPressed: boolean) {
        if (!this.selectionBox || !erStore.state) return;

        const minX = Math.min(this.selectionBox.startX, this.selectionBox.endX);
        const maxX = Math.max(this.selectionBox.startX, this.selectionBox.endX);
        const minY = Math.min(this.selectionBox.startY, this.selectionBox.endY);
        const maxY = Math.max(this.selectionBox.startY, this.selectionBox.endY);

        if (!ctrlPressed) {
            this.clear();
        }

        Object.values(erStore.state.tables).forEach(table => {
            const tLeft = table.x;
            const tRight = table.x + erStore.TABLE_WIDTH;
            const tTop = table.y;
            const tBottom = table.y + erStore.getTableHeight(table.id);

            if (tLeft < maxX && tRight > minX && tTop < maxY && tBottom > minY) {
                this.selectedTableIds.add(table.id);
            }
        });

        Object.values(erStore.state.references).forEach(ref => {
            const sTable = erStore.state!.tables[ref.key.fromTableId];
            const tTable = erStore.state!.tables[ref.key.toTableId];
            
            if (sTable && tTable) {
                const sCenterX = sTable.x + erStore.TABLE_WIDTH / 2;
                const sCenterY = sTable.y + erStore.getTableHeight(sTable.id) / 2;
                const tCenterX = tTable.x + erStore.TABLE_WIDTH / 2;
                const tCenterY = tTable.y + erStore.getTableHeight(tTable.id) / 2;
                
                const refCenterX = (sCenterX + tCenterX) / 2;
                const refCenterY = (sCenterY + tCenterY) / 2;

                if (refCenterX >= minX && refCenterX <= maxX && refCenterY >= minY && refCenterY <= maxY) {
                    this.selectedRefIds.add(refKeyToString(ref.key));
                }
            }
        });
    }
}

export const selectionStore = new SelectionStore();