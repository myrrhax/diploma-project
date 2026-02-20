import { makeAutoObservable } from "mobx";
import { v4 as uuidv4 } from "uuid";
import { type ReferenceKey } from "@/model/SchemaElements";
import type { Schema, VersionState } from "@/model/SchemaTypes";
import { authStore } from "./AuthStore";
import { type Table } from "@/model/SchemaElements";
import { length, refKeyToString } from "@/utils/UtilFunctions";

interface SelectedPort {
    tableId: string;
    colId: string;
}

class ERStore {
    readonly TABLE_WIDTH = 220;
    readonly HEADER_HEIGHT = 42;
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
					tables: {},
					references: {}
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
        this.state.tables[id] = {
            id: id,
            name: `Table ${id}`,
            description: '',
            x: worldX,
            y: worldY,
            primaryKeyParts: [colId],
            indexes: {},
            columns: {
                [colId]: {
                    id: colId,
                    name: 'id',
                    description: '',
                    type: 'INT',
                    additions: ['AUTO_INCREMENT']
                }
            }
        };
        this.closeContextMenu();
    }

    updateTableName(id: string, newName: string) {
        const t = this.state.tables[id];
        if (t) t.name = newName;
    }

    moveTable(id: string, dx: number, dy: number) {
        const table = this.state.tables[id];
        if (table) {
            table.x += dx / this.scale;
            table.y += dy / this.scale;
        }
    }

    addColumn(id: string) {
        const table = this.state.tables[id];
        if (table) {
            const id = uuidv4().toString();
            table.columns[id] = {
                id: id,
                name: `field_${Math.floor(Math.random() * 1000)}`,
                type: 'VARCHAR'
            }
        };
    }

	getTable(id: string): Table {
		return this.state.tables[id];
	}

    getTableHeight(id: string): number {
        const table = this.state.tables[id];
        if (!table) return 0;
        return this.HEADER_HEIGHT + (length(table.columns) * this.ROW_HEIGHT) + this.FOOTER_HEIGHT;
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
        const refKeyStr = refKeyToString(key);
        const existingRel = this.state.references[refKeyStr];

        if (!existingRel) {
            this.state.references[refKeyStr] = {
                key: key,
				type: 'MANY_TO_ONE'
            };
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