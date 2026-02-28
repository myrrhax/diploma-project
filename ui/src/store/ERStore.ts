import { makeAutoObservable, runInAction } from "mobx";
import { type Column, type Reference } from "@/model/SchemaElements";
import type { Schema, VersionState } from "@/model/SchemaTypes";
import { type Table } from "@/model/SchemaElements";
import { compareAndReturnNew, length, refKeyToString } from "@/utils/UtilFunctions";
import { type MetadataCommandProcessResult } from "@/model/SchemaEvents";
import { schemaApi } from "@/api/SchemaApiService";
import { schemaSocketService } from "@/api/SchemaSocketService";
import { referenceStore } from "./ReferenceStore";
import { tableModalsStore } from "./TableModalsStore";

class ERStore {
    readonly TABLE_WIDTH = 220;
    readonly HEADER_HEIGHT = 42;
    readonly ROW_HEIGHT = 32;
    readonly FOOTER_HEIGHT = 32;
    readonly MOVE_TICK_MS = 2000;

    schemaId: string | null = null;
    schema: Schema | null = null;
    state: VersionState | null = null;
    currentVersion: number | null = null;

    isAccessDenied: boolean | null = null;
    isLoading: boolean = false;
    
    scale = 1;
    offsetX = 0;
    offsetY = 0;

    lastTick: number = Date.now();
    draggingTableId: string | null = null;

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };
    activeMenuId: string | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    setSchema(schema: Schema | null) {
        this.schema = schema;
        this.state = schema?.currentVersion.currentState ?? null;
    }

    setActiveMenuId(id: string | null) {
        this.activeMenuId = id;
    }

    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    async process(cmd: MetadataCommandProcessResult) {
        if (!this.state) return;

        if (cmd.version <= this.state.cacheVersion) {
            return; 
        }
        console.log('Обрабатывается новая команда');
        console.log(cmd);

        if (cmd.version - this.state.cacheVersion > 1) {
            console.warn(`[SYNC] Рассинхрон! Локальная: ${this.state.cacheVersion}, Серверная: ${cmd.version}`);
            await this.loadSchema(this.schema!!.id);
            return;
        }

        this.state.cacheVersion = cmd.version;
        const diff = cmd.difference;
        
        console.log("Difference", diff);
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
            console.log('Delete ' + keyStr);
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
        console.log(this.state.tables);
    }
    
    setLoading(loading: boolean) {
        this.isLoading = loading;
    }

    async loadSchema(schemaId: string) {
        this.schemaId = schemaId;
        this.setLoading(true);
        this.allow();
        
        try {
            const freshSchema = await schemaApi.fetchSchemaById(schemaId);
            runInAction(() => {
                this.setSchema(freshSchema);
                this.setLoading(false);
                console.log(this.state?.tables);
            });
            
        } catch (error: any) {
            runInAction(() => {
                this.deny();
                this.setLoading(false);
            });
            console.error("Ошибка загрузки схемы:", this.state);
        }
    }

    addTable() {
        if (!this.schema) return;
        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;

        console.log("X: " + worldX);
        console.log("Y: " + worldY);

        const tableName = 'Table ' + Date.now();

        schemaSocketService.sendCommand({ 
            schemeId: this.schema.id,
            type: 'add-table', 
            tableName: tableName, 
            x: worldX, 
            y: worldY 
        });

        this.closeContextMenu();
    }

    updateTable(id: string, newName: string | null, newDescription: string | null) {
        const table = this.getTable(id);
        if (this.schema && table) {
            schemaSocketService.sendCommand({
                type: 'update-table',
                schemeId: this.schema.id,
                tableId: id,
                newTableName: newName,
                newDescription: newDescription
            })
        }
    }

    updateColumn(tableId: string, oldColumn: Column, newColumn: Column) {
        const table = this.getTable(tableId);
        if (this.schema && table) {
            schemaSocketService.sendCommand({
                schemeId: this.schema.id,
                type: 'update-column',
                tableId: tableId,
                columnId: oldColumn.id,
                newColumnName: compareAndReturnNew(oldColumn.name, newColumn.name),
                newDefaultValue: compareAndReturnNew(oldColumn.defaultValue, newColumn.defaultValue),
                newDescription: compareAndReturnNew(oldColumn.description, newColumn.description),
                newColumnType: compareAndReturnNew(oldColumn.columnType, newColumn.columnType),
                newPrecision: compareAndReturnNew(oldColumn.precision, newColumn.precision),
                newScale: compareAndReturnNew(oldColumn.scale, newColumn.scale),
                newLength: compareAndReturnNew(oldColumn.length, newColumn.length),
                constraints: compareAndReturnNew(oldColumn.constraints, newColumn.constraints),
                pkPart: compareAndReturnNew(oldColumn.pkPart, newColumn.pkPart),
                autoIncrement: compareAndReturnNew(oldColumn.autoIncrement, newColumn.autoIncrement)
            })
        }
    }

    moveTable(id: string, dx: number, dy: number) {
        const table = this.getTable(id);
        if (table) {
            table.x += dx / this.scale;
            table.y += dy / this.scale;
            // Отправка по тику
            if (this.draggingTableId) {
                const now = Date.now();
                if (now - this.lastTick > this.MOVE_TICK_MS) {
                    this.sendCoords();
                }
                this.lastTick = now;
            }
        }
    }

    setDraggingTable(tableId: string | null) {
        if (this.schema && this.draggingTableId && !tableId) { // Отпустил таблицу
            this.sendCoords();
        }
        this.draggingTableId = tableId;
    }

    addColumn(tableId: string, column: Column) {
        const table = this.getTable(tableId);
        if (this.schema && table) {
           schemaSocketService.sendCommand({
                type: 'add-column',
                tableId: table.id,
                schemeId: this.schema.id,
                ...column
           }) 
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

    openContextMenu(screenX: number, screenY: number, relativeX: number, relativeY: number) {
        this.contextMenu = { visible: true, screenX, screenY, x: relativeX, y: relativeY };
    }
    closeContextMenu() { this.contextMenu.visible = false; }

    deny() {
        this.isAccessDenied = true;
        this.setSchema(null);
    }

    allow() {
        this.isAccessDenied = false;
    }

    deleteReference(refKeyStr: string) {
        if (!this.state || !this.schema) {
            return;
        }

        const ref = this.state.references[refKeyStr];
        if (!ref) return;

        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'delete-ref',
            key: ref.key
        });

        referenceStore.closeRefContextMenu();
    }

    deleteTable(tableId: string) {
        if (!this.schema || !this.state) return;

        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'delete-table',
            tableId: tableId
        });

        tableModalsStore.closeTableContextMenu();
    }

    deleteColumn(tableId: string, columnId: string) {
        if (!this.schema || !this.state) {
            return;
        }

        schemaSocketService.sendCommand({
            type: 'delete-column',
            schemeId: this.schema.id,
            tableId: tableId,
            columnId: columnId
        });
    }

    private sendCoords() {
        if (this.draggingTableId && this.schema) {
            const table = this.state?.tables[this.draggingTableId];
            schemaSocketService.sendCommand({
                type: 'update-table',
                schemeId: this.schema.id,
                tableId: this.draggingTableId,
                x: table?.x,
                y: table?.y
            });
        }
    }
}

export const erStore = new ERStore();