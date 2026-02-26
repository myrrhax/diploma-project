import { makeAutoObservable } from "mobx";
import { schemaSocketService } from "@/api/SchemaSocketService";
import { erStore } from "./ERStore";
import type { ReferenceKey, ReferenceType, OnDeleteAction, OnUpdateAction } from "@/model/SchemaElements";


class ReferenceStore {
    isOpen = false;
    menuX = 0;
    menuY = 0;

    sourceTableId: string | null = null;
    targetTableId: string | null = null;
    
    sourceCols: string[] = [];
    targetCols: string[] = [];

    refType: ReferenceType = 'MANY_TO_ONE';
    onDelete: OnDeleteAction = 'NO_ACTION';
    onUpdate: OnUpdateAction = 'NO_ACTION';
    refContextMenu = { visible: false, x: 0, y: 0, refKeyStr: '' };

    constructor() {
        makeAutoObservable(this);
    }

    handlePortClick(side: 'left' | 'right', tableId: string, colId: string, clientX: number, clientY: number) {
        this.menuX = clientX;
        this.menuY = clientY;
        this.isOpen = true;

        if (side === 'right') { // Клик по Source (Output)
            if (this.sourceTableId && this.sourceTableId !== tableId) {
                this.reset();
                this.menuX = clientX; this.menuY = clientY; this.isOpen = true;
            }
            this.sourceTableId = tableId;
            
            const idx = this.sourceCols.indexOf(colId);
            if (idx === -1) this.sourceCols.push(colId);
            else this.sourceCols.splice(idx, 1);

        } else {
            if (this.sourceCols.length === 0) {
                alert("Сначала выберите исходную колонку (Output)");
                return;
            }
            if (this.targetTableId && this.targetTableId !== tableId) {
                this.targetCols = []; 
            }
            this.targetTableId = tableId;

            if (this.sourceTableId === tableId && this.sourceCols.includes(colId)) {
                alert("Нельзя создать связь колонки на саму себя!");
                return;
            }

            const idx = this.targetCols.indexOf(colId);
            if (idx === -1) this.targetCols.push(colId);
            else this.targetCols.splice(idx, 1);
        }
    }

    moveTargetUp(index: number) {
        if (index > 0) {
            const temp = this.targetCols[index - 1];
            this.targetCols[index - 1] = this.targetCols[index];
            this.targetCols[index] = temp;
        }
    }

    moveTargetDown(index: number) {
        if (index < this.targetCols.length - 1) {
            const temp = this.targetCols[index + 1];
            this.targetCols[index + 1] = this.targetCols[index];
            this.targetCols[index] = temp;
        }
    }

    get isReadyToSubmit() {
        return this.sourceCols.length > 0 
            && this.sourceTableId 
            && this.targetTableId 
            && this.sourceCols.length === this.targetCols.length;
    }

    submit() {
        if (!this.isReadyToSubmit || !erStore.schema) return;

        const key: ReferenceKey = {
            fromTableId: this.sourceTableId!,
            fromColumns: this.sourceCols,
            toTableId: this.targetTableId!,
            toColumns: this.targetCols
        };

        schemaSocketService.sendCommand({
            schemeId: erStore.schema.id,
            type: 'add-ref',
            referenceKey: key,
            referenceType: this.refType,
            deleteAction: this.onDelete,
            updateAction: this.onUpdate
        });

        this.reset();
    }

    reset() {
        this.isOpen = false;
        this.sourceTableId = null;
        this.targetTableId = null;
        this.sourceCols = [];
        this.targetCols = [];
        this.refType = 'MANY_TO_ONE';
        this.onDelete = 'NO_ACTION';
        this.onUpdate = 'NO_ACTION';
    }

    openRefContextMenu(x: number, y: number, refKeyStr: string) {
        this.refContextMenu = { visible: true, x, y, refKeyStr };
    }

    closeRefContextMenu() {
        this.refContextMenu.visible = false;
    }
}

export const referenceStore = new ReferenceStore();