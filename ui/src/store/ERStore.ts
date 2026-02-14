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
    // Храним пары ID колонок: [ {sourceColId, targetColId}, ... ]
    pairs: { sourceColId: string; targetColId: string }[];
}

// Тип для выбранного порта
interface SelectedPort {
    tableId: string;
    colId: string;
}

class ERStore {
    tables: Table[] = [];
    relations: Relation[] = [];
    
    // Canvas State
    scale = 1;
    offsetX = 0;
    offsetY = 0;
    
    // Logic State
    draggingTableId: string | null = null;
    
    // Multi-select connection logic
    selectedSources: SelectedPort[] = []; // Правые порты (Outputs)
    selectedTargets: SelectedPort[] = []; // Левые порты (Inputs)

    contextMenu = { visible: false, x: 0, y: 0, screenX: 0, screenY: 0 };

    constructor() {
        makeAutoObservable(this);
    }

    // --- CANVAS ACTIONS ---
    setPan(dx: number, dy: number) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    // --- TABLE ACTIONS ---
    // Создаем таблицу точно в месте клика (screenX/Y берем из saved context menu)
    addTable() {
        const worldX = (this.contextMenu.screenX - this.offsetX) / this.scale;
        const worldY = (this.contextMenu.screenY - this.offsetY) / this.scale;

        this.tables.push({
            id: uuidv4(),
            name: `Table ${this.tables.length + 1}`,
            x: worldX,
            y: worldY,
            columns: [
                { id: uuidv4(), name: 'id', type: 'int' }
            ]
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

    // --- COLUMN ACTIONS ---
    addColumn(tableId: string) {
        const table = this.tables.find(t => t.id === tableId);
        if (table) {
            // Случайное имя колонки
            const randomName = `field_${Math.floor(Math.random() * 1000)}`;
            table.columns.push({
                id: uuidv4(),
                name: randomName,
                type: 'varchar'
            });
        }
    }

    // --- CONNECTION LOGIC (CLICK BASED) ---
    handlePortClick(side: 'left' | 'right', tableId: string, colId: string) {
        if (side === 'right') {
            // 1. Клик по ВЫХОДУ (Source)
            
            // Если мы уже выбрали сорс из ДРУГОЙ таблицы, сбрасываем (нельзя вести из двух таблиц одновременно)
            if (this.selectedSources.length > 0 && this.selectedSources[0].tableId !== tableId) {
                this.selectedSources = [];
                this.selectedTargets = [];
            }

            // Тоггл выбора (если кликнули повторно - убираем)
            const existsIdx = this.selectedSources.findIndex(p => p.colId === colId);
            if (existsIdx !== -1) {
                this.selectedSources.splice(existsIdx, 1);
            } else {
                this.selectedSources.push({ tableId, colId });
            }

            // При изменении сорсов, таргеты сбрасываем, так как валидация очереди нарушается
            this.selectedTargets = [];

        } else {
            // 2. Клик по ВХОДУ (Target)
            if (this.selectedSources.length === 0) return; // Нельзя выбрать таргет без сорса
            if (this.selectedSources[0].tableId === tableId) return; // Нельзя связывать таблицу саму с собой (пока)

            this.selectedTargets.push({ tableId, colId });

            // 3. ПРОВЕРКА: Если число таргетов совпало с числом сорсов -> СОЗДАЕМ СВЯЗЬ
            if (this.selectedTargets.length === this.selectedSources.length) {
                this.createMultiRelation();
            }
        }
    }

    createMultiRelation() {
        const sourceTableId = this.selectedSources[0].tableId;
        const targetTableId = this.selectedTargets[0].tableId;

        // Формируем пары по очередности выбора (1-й сорс к 1-му таргету, 2-й ко 2-му)
        const newPairs = this.selectedSources.map((src, index) => ({
            sourceColId: src.colId,
            targetColId: this.selectedTargets[index].colId
        }));

        // Проверяем, есть ли уже связь между этими таблицами
        const existingRel = this.relations.find(r => 
            r.sourceTableId === sourceTableId && r.targetTableId === targetTableId
        );

        if (existingRel) {
            // Добавляем новые пары в существующую связь, избегая дубликатов
            newPairs.forEach(pair => {
                const isDup = existingRel.pairs.some(p => p.sourceColId === pair.sourceColId && p.targetColId === pair.targetColId);
                if (!isDup) existingRel.pairs.push(pair);
            });
        } else {
            // Создаем новую связь
            this.relations.push({
                id: uuidv4(),
                sourceTableId,
                targetTableId,
                pairs: newPairs
            });
        }

        // Сброс
        this.selectedSources = [];
        this.selectedTargets = [];
    }

    // --- CONTEXT MENU ---
    openContextMenu(screenX: number, screenY: number, relativeX: number, relativeY: number) {
        this.contextMenu = { 
            visible: true, 
            screenX: screenX, // Абсолютные координаты курсора для логики
            screenY: screenY, 
            x: relativeX,     // Координаты для отрисовки меню
            y: relativeY 
        };
    }
    closeContextMenu() { this.contextMenu.visible = false; }
}

export const erStore = new ERStore();