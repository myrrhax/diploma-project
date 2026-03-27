import { makeAutoObservable, runInAction } from "mobx";
import { type Column, type Index, type Reference, type ReferenceKey } from "@/model/SchemaElements";
import type { Schema, VersionState } from "@/model/SchemaTypes";
import { type Table } from "@/model/SchemaElements";
import { compareAndReturnNew, length, refKeyToString } from "@/utils/UtilFunctions";
import { type MetadataCommandProcessResult } from "@/model/SchemaEvents";
import { schemaApi } from "@/api/SchemaApiService";
import { schemaSocketService } from "@/api/SchemaSocketService";
import { referenceStore } from "./ReferenceStore";
import { tableModalsStore } from "./TableModalsStore";
import { participationsStore } from "./ParticipationStore";
import { selectionStore } from "./SelectionStore";
import type { MultiCommand } from "@/model/SchemaCommands";

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

    viewportWidth = 0;
    viewportHeight = 0;
    isCentered = false;

    draggingTableId: string | null = null;

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };
    activeMenuId: string | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    get isEditable() {
        return this.schema?.currentVersion?.isWorkingCopy === true;
    }

    setSchema(schema: Schema | null) {
        this.schema = schema;
        this.state = schema?.currentVersion.currentState ?? null;
    }

    setActiveMenuId(id: string | null) {
        this.activeMenuId = id;
    }

    setViewportSize(width: number, height: number) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    get tablesBoundingBox() {
        if (!this.state || !this.state.tables || Object.keys(this.state.tables).length === 0) {
            return { minX: Infinity, maxX: -Infinity, minY: Infinity, maxY: -Infinity };
        }
        
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        for (const table of Object.values(this.state.tables)) {
            if (table.x < minX) minX = table.x;
            if (table.x + this.TABLE_WIDTH > maxX) maxX = table.x + this.TABLE_WIDTH;
            
            const h = this.getTableHeight(table.id);
            if (table.y < minY) minY = table.y;
            if (table.y + h > maxY) maxY = table.y + h;
        }
        return { minX, maxX, minY, maxY };
    }

    centerView() {
        if (this.viewportWidth === 0 || this.viewportHeight === 0) return;
        
        const { minX, maxX, minY, maxY } = this.tablesBoundingBox;
        
        if (minX === Infinity) {
            this.offsetX = this.viewportWidth / 2;
            this.offsetY = this.viewportHeight / 2;
            this.isCentered = true;
            return;
        }

        const centerX = (minX + maxX) / 2;
        const centerY = (minY + maxY) / 2;

        this.offsetX = (this.viewportWidth / 2) - (centerX * this.scale);
        this.offsetY = (this.viewportHeight / 2) - (centerY * this.scale);
        this.constrainPan(); 
        this.isCentered = true;
    }

    constrainPan() {
        const PADDING = 1500;
        const { minX, maxX, minY, maxY } = this.tablesBoundingBox;
        
        if (minX === Infinity) return; 

        let minOffsetX = this.viewportWidth - (maxX + PADDING) * this.scale;
        let maxOffsetX = -(minX - PADDING) * this.scale;
        
        let minOffsetY = this.viewportHeight - (maxY + PADDING) * this.scale;
        let maxOffsetY = -(minY - PADDING) * this.scale;

        if (minOffsetX > maxOffsetX) [minOffsetX, maxOffsetX] = [maxOffsetX, minOffsetX];
        if (minOffsetY > maxOffsetY) [minOffsetY, maxOffsetY] = [maxOffsetY, minOffsetY];

        if (this.offsetX < minOffsetX) this.offsetX = minOffsetX;
        if (this.offsetX > maxOffsetX) this.offsetX = maxOffsetX;
        if (this.offsetY < minOffsetY) this.offsetY = minOffsetY;
        if (this.offsetY > maxOffsetY) this.offsetY = maxOffsetY;
    }

    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
        this.constrainPan();
    }

    zoom(deltaY: number) {
        this.scale = Math.max(0.3, Math.min(2, this.scale + deltaY * -0.001));
        this.constrainPan();
    }

    async process(cmd: MetadataCommandProcessResult) {
        console.log('State: ', this.state);
        if (!this.state) return;

        console.log('Processing command: ', cmd);
        if (cmd.version <= this.state.cacheVersion) {
            return; 
        }

        if (cmd.version - this.state.cacheVersion > 1) {
            await this.loadSchema(this.schema!!.id);
            return;
        }

        this.state.cacheVersion = cmd.version;
        const diff = cmd.difference;

        console.log('Processing diff', diff);
        
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
    
    setLoading(loading: boolean) {
        this.isLoading = loading;
    }

    async loadSchema(schemaId: string) {
        this.schemaId = schemaId;
        this.setLoading(true);
        this.allow();
        this.isCentered = false; 
        
        try {
            const freshSchema = await schemaApi.fetchSchemaById(schemaId);
            await participationsStore.loadParticipationInfo(schemaId);

            runInAction(() => {
                this.setSchema(freshSchema);
                this.setLoading(false);
            });
            
        } catch (error: any) {
            runInAction(() => {
                this.deny();
                this.setLoading(false);
            });
        }
    }

    addTable() {
        if (!this.isEditable || !this.schema) return;
        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;

        const tableName = 'Table_' + Date.now();

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
        if (!this.isEditable) return;
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
        if (!this.isEditable) return;
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
                autoIncrement: compareAndReturnNew(oldColumn.autoIncrement, newColumn.autoIncrement),
                min: compareAndReturnNew(oldColumn.min, newColumn.min),
                max: compareAndReturnNew(oldColumn.max, newColumn.max)
            })
        }
    }

    moveTable(id: string, dx: number, dy: number) {
        if (!this.isEditable) return;

        if (selectionStore.selectedTableIds.size > 1) {
            selectionStore.selectedTableIds.forEach(tableId => {
                const table = this.state?.tables[tableId];
                if (table) {
                    table.x += dx / this.scale;
                    table.y += dy / this.scale;
                }
                
            })
        } else {
            const table = this.getTable(id);
            if (table) {
                table.x += dx / this.scale;
                table.y += dy / this.scale;
            }
        }
    }

    setDraggingTable(tableId: string | null) {
        if (!this.isEditable) return;
        if (this.schema && this.draggingTableId && !tableId) { 
            this.sendCoords();
        }
        this.draggingTableId = tableId;
    }

    addColumn(tableId: string, column: Column) {
        if (!this.isEditable) return;
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
        if (!this.isEditable || !this.state || !this.schema) return;

        const ref = this.state.references[refKeyStr];
        if (!ref) return;

        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'delete-ref',
            key: ref.key
        });

        referenceStore.closeRefContextMenu();
    }

    renameReference(ref: ReferenceKey, newName: string) {
        if (!this.isEditable || !this.state || !this.schema) return;

        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'rename-ref',
            key: ref,
            newName: newName
        })
    }

    deleteTable(tableId: string) {
        if (!this.isEditable || !this.schema || !this.state) return;
        
        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'delete-table',
            tableId: tableId
        });

        tableModalsStore.closeTableContextMenu();
    }

    async loadSchemaWithVersion(versionId: number) {
        this.isCentered = false; 
        const schema = await schemaApi.findReadonlyWithVersion(versionId);
        this.setSchema(schema);
    }

    addIndex(index: Index, tableId: string) {
        if (!this.isEditable || !this.schema || !this.state) return;
        
        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'add-index',
            tableId: tableId,
            affectedColumns: index.columnIds,
            indexName: index.indexName,
            isUnique: index.isUnique,
            indexType: index.indexType
        });
    }

    deleteIndex(indexId: string, tableId: string) {
        if (!this.isEditable || !this.schema || !this.state) return;
        
        schemaSocketService.sendCommand({
            schemeId: this.schema.id,
            type: 'delete-index',
            tableId: tableId,
            indexId: indexId
        });
    }

    deleteColumn(tableId: string, columnId: string) {
        if (!this.isEditable || !this.schema || !this.state) return;

        schemaSocketService.sendCommand({
            type: 'delete-column',
            schemeId: this.schema.id,
            tableId: tableId,
            columnId: columnId
        });
    }

    multiDelete() {
        if (!this.isEditable || !this.schemaId) return;
        const multiCommand: MultiCommand = { 
            schemeId: this.schemaId,
            type: 'multi', 
            commands: [] 
        };

        selectionStore.selectedTableIds.forEach((tableId) => {
            if (!this.state || !this.schemaId) {
                return;
            }            
            multiCommand.commands.push({
                schemeId: this.schemaId,
                type: 'delete-table',
                tableId: tableId
            });
        });

        selectionStore.selectedRefIds.forEach((refId) => {
            if (!this.state || !this.schemaId) {
                return;
            }
            const reference = this.state.references[refId];
            
            multiCommand.commands.push({
                schemeId: this.schemaId,
                type: 'delete-ref',
                key: reference.key
            });
        });
        schemaSocketService.sendCommand(multiCommand);
    }

    private sendCoords() {
        if (!this.schema || !this.state) {
            return;
        }

        if (selectionStore.selectedTableIds.size > 0) {
            const multiCommand: MultiCommand = { 
                schemeId: this.schema.id,
                type: 'multi', 
                commands: [] 
            };

            selectionStore.selectedTableIds.forEach(tableId => {
                if (!this.state || !this.schema) {
                    return;
                }
                console.log("TableId", selectionStore.selectedTableIds);
                const table = this.state.tables[tableId];
                multiCommand.commands.push({
                    schemeId: this.schema.id,
                    type: 'update-table',
                    tableId: table.id,
                    x: table.x,
                    y: table.y
                });
                schemaSocketService.sendCommand(multiCommand);
            })
        } else if (this.draggingTableId) {
            const table = this.state.tables[this.draggingTableId];
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