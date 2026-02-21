import { makeAutoObservable } from "mobx";
import { v4 as uuidv4 } from "uuid";
import { type Reference, type ReferenceKey } from "@/model/SchemaElements";
import type { Schema, VersionState } from "@/model/SchemaTypes";
import { type Table } from "@/model/SchemaElements";
import { length, refKeyToString } from "@/utils/UtilFunctions";
import { type MetadataCommandProcessResult } from "@/model/SchemaEvents";

interface SelectedPort {
    tableId: string;
    colId: string;
}

class ERStore {
    readonly TABLE_WIDTH = 220;
    readonly HEADER_HEIGHT = 42;
    readonly ROW_HEIGHT = 32;
    readonly FOOTER_HEIGHT = 32;

    schema: Schema | null = null;
    state: VersionState | null = null;
    currentVersion: number | null = null;

    isAccessDenied: boolean | null = null;
    
    scale = 1;
    offsetX = 0;
    offsetY = 0;
    
    draggingTableId: string | null = null;
    
    selectedSources: SelectedPort[] = [];
    selectedTargets: SelectedPort[] = [];

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };

    constructor() {
        makeAutoObservable(this);
    }

    setSchema(schema: Schema) {
        this.schema = schema;
        this.state = schema.currentVersion.currentState;
    }

    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    // --- ОБРАБОТКА СИНХРОНИЗАЦИИ (ВЕБСОКЕТ) ---
    process(cmd: MetadataCommandProcessResult) {
        if (!this.state) return;

        // Игнорируем дублирующиеся или старые пакеты
        if (cmd.version <= this.state.cacheVersion) {
            return; 
        }

        // Если пакеты потерялись по пути, запрашиваем схему целиком
        if (cmd.version - this.state.cacheVersion > 1) {
            console.warn(`[SYNC] Рассинхрон! Локальная: ${this.state.cacheVersion}, Серверная: ${cmd.version}`);
            // this.refetchFullSchema();
            return;
        }

        this.state.cacheVersion = cmd.version;
        const diff = cmd.difference;

        Object.entries(diff.deletedColumns || {}).forEach(([tableId, colIds]) => {
            const table = this.state?.tables[tableId];
            if (table && table.columns) {
                colIds.forEach(colId => delete table.columns[colId]);
            }
        });

        Object.entries(diff.deletedIndexes || {}).forEach(([tableId, indexIds]) => {
            const table = this.state?.tables[tableId];
            if (table && table.indexes) {
                indexIds.forEach(indexId => delete table.indexes[indexId]);
            }
        });

        (diff.deletedReferences || []).forEach(refKey => {
            const keyStr = refKeyToString(refKey); 
            delete this.state?.references[keyStr];
        });

        (diff.deletedTables || []).forEach(tableId => {
            delete this.state?.tables[tableId];
        });


        (diff.upsertedTables || []).forEach(newTable => {
            if (!this.state) {
                return;
            }
            const existingTable = this.state?.tables[newTable.id];
            if (existingTable) {
                Object.assign(existingTable, newTable);
            } else {
                this.state.tables[newTable.id] = newTable;
            }
        });

        Object.entries(diff.upsertedColumns || {}).forEach(([tableId, columns]) => {
            const table = this.state?.tables[tableId];
            if (table) {
                if (!table.columns) table.columns = {};
                columns.forEach(col => table.columns[col.id] = col);
            }
        });

        Object.entries(diff.upsertedIndexes || {}).forEach(([tableId, indexes]) => {
            const table = this.state?.tables[tableId];
            if (table) {
                if (!table.indexes) table.indexes = {};
                indexes.forEach(idx => table.indexes[idx.id] = idx);
            }
        });

        (diff.upsertedReferences || []).forEach((ref: Reference) => {
            if (!this.state) {
                return;
            }
            const keyStr = refKeyToString(ref.key);
            this.state.references[keyStr] = ref;
        });
    }

    // async refetchFullSchema() {
    //     if (!this.schema) return;
        
    //     try {
    //         const response = await fetch(`/api/schema/${this.schema.id}`);
    //         if (response.ok) {
    //             const freshSchema = await response.json() as Schema;
    //             runInAction(() => {
    //                 this.setSchema(freshSchema);
    //                 console.log("[SYNC] Схема успешно восстановлена");
    //             });
    //         }
    //     } catch (error) {
    //         console.error("[SYNC] Ошибка полного восстановления схемы:", error);
    //     }
    // }
    
    addTable() {
        if (!this.state) return;

        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;
        const colId = uuidv4().toString();
        const tableId = uuidv4().toString();

        this.state.tables[tableId] = {
            id: tableId,
            name: `Table ${tableId.substring(0, 4)}`, // Сделал имя покороче для удобства
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

        // ToDo: Отправить команду 'add-table' на бэкенд через WebSocket
        // schemaSocketService.sendCommand({ type: 'add-table', tableId: tableId, ... });

        this.closeContextMenu();
    }

    updateTableName(id: string, newName: string) {
        const table = this.getTable(id);
        if (table) {
            table.name = newName;
        }
    }

    moveTable(id: string, dx: number, dy: number) {
        const table = this.getTable(id);
        if (table) {
            table.x += dx / this.scale;
            table.y += dy / this.scale;
        }
    }

    addColumn(tableId: string) {
        const table = this.getTable(tableId);
        if (table) {
            const newColId = uuidv4().toString();
            table.columns[newColId] = {
                id: newColId,
                name: `field_${Math.floor(Math.random() * 1000)}`,
                description: '',
                type: 'VARCHAR',
                additions: []
            };
            // ToDo: Отправить 'add-column' на бэкенд
        }
    }

    getTable(id: string): Table | null {
        if (!this.state) return null;
        return this.state.tables[id] || null;
    }

    getTableHeight(id: string): number {
        const table = this.getTable(id);
        if (!table) return 0;
        return this.HEADER_HEIGHT + (length(table.columns) * this.ROW_HEIGHT) + this.FOOTER_HEIGHT;
    }

    handlePortClick(side: 'left' | 'right', tableId: string, colId: string) {
        if (side === 'right') {
            if (this.selectedSources.length > 0 && this.selectedSources[0].tableId !== tableId) {
                this.selectedSources = [];
                this.selectedTargets = [];
            }

            const existsIdx = this.selectedSources.findIndex(p => p.colId === colId);
            if (existsIdx !== -1) {
                this.selectedSources.splice(existsIdx, 1);
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
            
            if (matchingSource.colId === colId) {
                alert("Нельзя создать связь колонки на саму себя!");
                return;
            }
            if (this.selectedTargets.some(t => t.colId === colId)) return;
            
            this.selectedTargets.push({ tableId, colId });

            if (this.selectedTargets.length === this.selectedSources.length) {
                this.createMultiRelation();
            }
        }
    }

    createMultiRelation() {
        if (!this.state) return;

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
            // ToDo: Отправить 'add-reference' на бэкенд
        }

        this.selectedSources = [];
        this.selectedTargets = [];
    }

    openContextMenu(screenX: number, screenY: number, relativeX: number, relativeY: number) {
        this.contextMenu = { visible: true, screenX, screenY, x: relativeX, y: relativeY };
    }
    closeContextMenu() { this.contextMenu.visible = false; }

    deny() {
        this.isAccessDenied = true;
        this.schema = null;
        this.state = null;
    }

    allow() {
        this.isAccessDenied = false;
    }
}

export const erStore = new ERStore();