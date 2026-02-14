import { makeAutoObservable } from "mobx";
import { v4 as uuidv4 } from "uuid";
import { type ReferenceKey } from "@/model/SchemaElements";
import type { Schema, VersionState } from "@/model/SchemaTypes";
import { authStore } from "./AuthStore";
import { type Table } from "@/model/SchemaElements";

interface SelectedPort {
    tableId: string;
    colId: string;
}

class ERStore {
    readonly TABLE_WIDTH = 220;
    readonly HEADER_HEIGHT = 42; // Header + Separator
    readonly ROW_HEIGHT = 32;
    readonly FOOTER_HEIGHT = 32;

    schema: Schema;
    state: VersionState;
    
    scale = 1;
    offsetX = 0;
    offsetY = 0;
    
    draggingTableId: string | null = null;
    
    selectedSources: SelectedPort[] = [];
    selectedTargets: SelectedPort[] = [];

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };

    constructor() {
        makeAutoObservable(this);

        // ToDo заменить на вызов API
        this.schema = {
			id: uuidv4().toString(),
			name: 'Схема v1',
			creator: authStore.user!!,
			currentVersion: {
				schemeId: uuidv4().toString(),
				versionId: 1,
				isInitial: true,
				isWorkingCopy: true,
				currentState: {
					tables: [],
					references: []
				}
			}
        }
		this.state = this.schema.currentVersion.currentState;
    }

    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    addTable() {
        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;
        const colId = uuidv4().toString();

        // ToDo поменять на вызов API
		const id = uuidv4().toString();
        this.state.tables.push({key: id, table: {
            id: id,
            name: `Table ${this.state.tables.length + 1}`,
            x: worldX,
            y: worldY,
            columns: [{ id: colId, column: { id: colId, name: 'id', type: 'INT' } }],
        }});
        this.closeContextMenu();
    }

    updateTableName(id: string, newName: string) {
        const t = this.state.tables.find(t => t.key === id);
        if (t) t.table.name = newName;
    }

    moveTable(id: string, dx: number, dy: number) {
        const table = this.state.tables.find(t => t.key === id);
        if (table) {
            table.table.x += dx / this.scale;
            table.table.y += dy / this.scale;
        }
    }

    addColumn(tableId: string) {
        const table = this.state.tables.find(t => t.key === tableId);
        if (table) {
            const id = uuidv4().toString();
            table.table.columns?.push({id: id, column: {
                id: uuidv4(),
                name: `field_${Math.floor(Math.random() * 1000)}`,
                type: 'VARCHAR'
            }});
        }
    }

	getTable(tableId: string): Table {
		return this.state.tables.find(t => t.key === tableId)!!.table;
	}

    getTableHeight(tableId: string): number {
        const table = this.state.tables.find(t => t.key === tableId);
        if (!table) return 0;
        return this.HEADER_HEIGHT + ((table.table.columns?.length ?? 0) * this.ROW_HEIGHT) + this.FOOTER_HEIGHT;
    }

    // --- ЛОГИКА СОЕДИНЕНИЯ ---
    handlePortClick(side: 'left' | 'right', tableId: string, colId: string) {
        if (side === 'right') {
            
            // Если начали выбирать из новой таблицы, сбрасываем старый выбор
            if (this.selectedSources.length > 0 && this.selectedSources[0].tableId !== tableId) {
                this.selectedSources = [];
                this.selectedTargets = [];
            }

            const existsIdx = this.selectedSources.findIndex(p => p.colId === colId);
            if (existsIdx !== -1) {
                this.selectedSources.splice(existsIdx, 1);
                // Если убрали сорс, нужно убрать и соответствующий таргет, если он был выбран
                if (this.selectedTargets.length > existsIdx) {
                    this.selectedTargets.splice(existsIdx, 1);
                }
            } else {
                this.selectedSources.push({ tableId, colId });
            }
        } else {
            if (this.selectedSources.length === 0) return; 
            const currentIndex = this.selectedTargets.length;            
            if (currentIndex >= this.selectedSources.length) return;
            const matchingSource = this.selectedSources[currentIndex];
            
            // 2. Запрещаем связь колонки саму на себя
            if (matchingSource.colId === colId) {
                alert("Нельзя создать связь колонки на саму себя!");
                return;
            }
			if (this.selectedTargets.some(t => t.colId === colId)) {
				return;
			}
            this.selectedTargets.push({ tableId, colId });

            if (this.selectedTargets.length === this.selectedSources.length) {
                this.createMultiRelation();
            }
        }
    }

    createMultiRelation() {
        const sourceTableId = this.selectedSources[0].tableId;
        const targetTableId = this.selectedTargets[0].tableId;

        const fromColumns = this.selectedSources.map((src, _) => src.colId);
        const toColumns = this.selectedTargets.map((src, _) => src.colId);
        const key: ReferenceKey = { 
			fromTableId: sourceTableId,
			toTableId: targetTableId,
			fromColumns: fromColumns,
			toColumns: toColumns
        };

        const existingRel = this.state.references.find(r => r.key === key);

        if (!existingRel) {
            this.state.references.push({key: key, ref: {
                key: key,
				type: 'MANY_TO_ONE'
            }});
        }

        this.selectedSources = [];
        this.selectedTargets = [];
    }

    openContextMenu(screenX: number, screenY: number, relativeX: number, relativeY: number) {
        this.contextMenu = { visible: true, screenX, screenY, x: relativeX, y: relativeY };
    }
    closeContextMenu() { this.contextMenu.visible = false; }
}

export const erStore = new ERStore();