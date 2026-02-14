import { makeAutoObservable } from "mobx";
import { v4 as uuidv4 } from "uuid";

export interface Column {
    id: string;
    name: string;
    type: string;
}

export interface Table {
    id: string;
    name: string;
    x: number;
    y: number;
    columns: Column[];
}

export interface Relation {
    id: string;
    sourceTableId: string;
    targetTableId: string;
    pairs: { sourceColId: string; targetColId: string }[];
}

interface SelectedPort {
    tableId: string;
    colId: string;
}

class ERStore {
    readonly TABLE_WIDTH = 220;
    readonly HEADER_HEIGHT = 42; // Header + Separator
    readonly ROW_HEIGHT = 32;
    readonly FOOTER_HEIGHT = 32;

    tables: Table[] = [];
    relations: Relation[] = [];
    
    scale = 1;
    offsetX = 0;
    offsetY = 0;
    
    draggingTableId: string | null = null;
    
    selectedSources: SelectedPort[] = []; // Очередь выходов (Right)
    selectedTargets: SelectedPort[] = []; // Очередь входов (Left)

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };

    constructor() {
        makeAutoObservable(this);
    }

    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    addTable() {
        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;

        this.tables.push({
            id: uuidv4(),
            name: `Table ${this.tables.length + 1}`,
            x: worldX,
            y: worldY,
            columns: [{ id: uuidv4(), name: 'id', type: 'int' }]
        });
        this.closeContextMenu();
    }

    updateTableName(id: string, newName: string) {
        const t = this.tables.find(t => t.id === id);
        if (t) t.name = newName;
    }

    moveTable(id: string, dx: number, dy: number) {
        const table = this.tables.find(t => t.id === id);
        if (table) {
            table.x += dx / this.scale;
            table.y += dy / this.scale;
        }
    }

    addColumn(tableId: string) {
        const table = this.tables.find(t => t.id === tableId);
        if (table) {
            table.columns.push({
                id: uuidv4(),
                name: `field_${Math.floor(Math.random() * 1000)}`,
                type: 'varchar'
            });
        }
    }

    getTableHeight(tableId: string): number {
        const table = this.tables.find(t => t.id === tableId);
        if (!table) return 0;
        return this.HEADER_HEIGHT + (table.columns.length * this.ROW_HEIGHT) + this.FOOTER_HEIGHT;
    }

    // --- ЛОГИКА СОЕДИНЕНИЯ ---
    handlePortClick(side: 'left' | 'right', tableId: string, colId: string) {
        if (side === 'right') {
            // Клик по ВЫХОДУ (Source)
            
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
            // Клик по ВХОДУ (Target)
            if (this.selectedSources.length === 0) return; 

            // Находим какой по счету это будет таргет (0-й для 0-го сорса и т.д.)
            const currentIndex = this.selectedTargets.length;
            
            // Если мы уже выбрали все таргеты для текущих сорсов, ничего не делаем
            if (currentIndex >= this.selectedSources.length) return;

            // ВАЛИДАЦИЯ:
            // 1. Берем сорс, который соответствует текущей очереди
            const matchingSource = this.selectedSources[currentIndex];
            
            // 2. Запрещаем связь колонки саму на себя
            if (matchingSource.colId === colId) {
                alert("Нельзя создать связь колонки на саму себя!");
                return;
            }

            // Добавляем в очередь
            this.selectedTargets.push({ tableId, colId });

            // Если заполнили все пары -> создаем связь
            if (this.selectedTargets.length === this.selectedSources.length) {
                this.createMultiRelation();
            }
        }
    }

    createMultiRelation() {
        const sourceTableId = this.selectedSources[0].tableId;
        // Таргет берем из первого выбора, но учитываем, что мультисвязь должна идти в одну таблицу
        // В текущей реализации предполагаем, что пользователь кликает в одну таблицу-таргет
        const targetTableId = this.selectedTargets[0].tableId;

        const newPairs = this.selectedSources.map((src, index) => ({
            sourceColId: src.colId,
            targetColId: this.selectedTargets[index].colId
        }));

        const existingRel = this.relations.find(r => 
            r.sourceTableId === sourceTableId && r.targetTableId === targetTableId
        );

        if (existingRel) {
            newPairs.forEach(pair => {
                const isDup = existingRel.pairs.some(p => p.sourceColId === pair.sourceColId && p.targetColId === pair.targetColId);
                if (!isDup) existingRel.pairs.push(pair);
            });
        } else {
            this.relations.push({
                id: uuidv4(),
                sourceTableId,
                targetTableId,
                pairs: newPairs
            });
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