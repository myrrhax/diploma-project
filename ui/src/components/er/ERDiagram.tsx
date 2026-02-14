import React, { useRef, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore, type Table } from '@/store/ERStore';
import { TableNode } from './TableNode';
import './css/ERDiagram.css';

// --- ALGORITHM: Smart Orthogonal Routing ---
const getSmartEdgePath = (
    sX: number, sY: number, // Точные координаты правого порта (Start)
    tX: number, tY: number, // Точные координаты левого порта (End)
    sTableId: string, 
    tTableId: string, 
    index: number // индекс для отступа параллельных линий
) => {
    // Получаем высоты таблиц для расчета обхода снизу
    const sHeight = erStore.getTableHeight(sTableId);
    const tHeight = erStore.getTableHeight(tTableId);
    
    // Находим Y-координаты низа таблиц
    const sTableY = erStore.tables.find(t => t.id === sTableId)?.y || 0;
    const tTableY = erStore.tables.find(t => t.id === tTableId)?.y || 0;
    const sBottom = sTableY + sHeight;
    const tBottom = tTableY + tHeight;
    
    // Динамический отступ между линиями
    const gap = index * 10; 

    // Сценарий 1: ПРЯМАЯ СВЯЗЬ (Target значительно правее Source)
    // Условие: tX правее sX хотя бы на 50px
    if (tX > sX + 50) {
        const midX = (sX + tX) / 2;
        // Start -> Середина вправо -> Середина вертикально -> End
        return `M ${sX} ${sY} L ${midX} ${sY} L ${midX} ${tY} L ${tX} ${tY}`;
    }

    // Сценарий 2: ОБРАТНАЯ СВЯЗЬ / ПЕТЛЯ / БЛИЗКАЯ СВЯЗЬ (Огибаем снизу)
    else {
        // Безопасная зона Y под самой нижней таблицей + базовый отступ 20px + динамический gap
        const safeY = Math.max(sBottom, tBottom) + 20 + gap;
        
        // 1. Старт точно из порта
        const p1 = { x: sX, y: sY };
        // 2. Короткий "пенек" вправо на 20px
        const p2 = { x: sX + 20, y: sY };
        // 3. Вниз до безопасной зоны
        const p3 = { x: sX + 20, y: safeY };
        // 4. Влево до уровня входа Target (с отступом 20px)
        const p4 = { x: tX - 20, y: safeY };
        // 5. Вверх до уровня Target Y
        const p5 = { x: tX - 20, y: tY };
        // 6. Финиш точно во входной порт
        const p6 = { x: tX, y: tY };

        return `M ${p1.x} ${p1.y} L ${p2.x} ${p2.y} L ${p3.x} ${p3.y} L ${p4.x} ${p4.y} L ${p5.x} ${p5.y} L ${p6.x} ${p6.y}`;
    }
};

// Хелпер координат порта (остался прежним, он корректен)
const getPortPosition = (table: Table, colId: string, side: 'left' | 'right') => {
    const colIndex = table.columns.findIndex(c => c.id === colId);
    if (colIndex === -1) return { x: 0, y: 0 };
    
    const y = table.y + erStore.HEADER_HEIGHT + (colIndex * erStore.ROW_HEIGHT) + (erStore.ROW_HEIGHT / 2);
    const x = side === 'left' ? table.x : table.x + erStore.TABLE_WIDTH;
    return { x, y };
};

export const ERDiagram = observer(() => {
    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleUp = () => { erStore.draggingTableId = null; };
        window.addEventListener('mouseup', handleUp);
        return () => window.removeEventListener('mouseup', handleUp);
    }, []);

    const handleMouseMove = (e: React.MouseEvent) => {
        if (erStore.draggingTableId) {
            erStore.moveTable(erStore.draggingTableId, e.movementX, e.movementY);
        }
        if (e.buttons === 4) {
            erStore.setPan(e.movementX, e.movementY);
        }
    };

    return (
        <div 
            className="er_diagram_wrapper" 
            ref={containerRef}
            onWheel={(e) => erStore.scale = Math.max(0.3, Math.min(2, erStore.scale + e.deltaY * -0.001))}
            onMouseMove={handleMouseMove}
            onContextMenu={(e) => {
                e.preventDefault();
                const rect = containerRef.current?.getBoundingClientRect();
                if (rect) erStore.openContextMenu(e.clientX, e.clientY, e.clientX - rect.left, e.clientY - rect.top);
            }}
            onClick={() => erStore.closeContextMenu()}
        >
            <div className="er_viewport" style={{ transform: `translate(${erStore.offsetX}px, ${erStore.offsetY}px) scale(${erStore.scale})` }}>
                <svg className="er_svg_layer">
                    <defs>
                        <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
                            <path d="M0,0 L0,6 L9,3 z" fill="#64748b" />
                        </marker>
                    </defs>
                    
                    {erStore.relations.map(rel => {
                        const sTable = erStore.tables.find(t => t.id === rel.sourceTableId);
                        const tTable = erStore.tables.find(t => t.id === rel.targetTableId);
                        if (!sTable || !tTable) return null;

                        return rel.pairs.map((pair, idx) => {
                            const start = getPortPosition(sTable, pair.sourceColId, 'right');
                            const end = getPortPosition(tTable, pair.targetColId, 'left');
                            
                            // Генерируем "умный" путь
                            const pathData = getSmartEdgePath(
                                start.x, start.y, 
                                end.x, end.y, 
                                sTable.id, tTable.id, 
                                idx 
                            );

                            return (
                                <path 
                                    key={`${rel.id}-${idx}`}
                                    d={pathData}
                                    className="er_line"
                                    markerEnd="url(#arrow)"
                                />
                            );
                        });
                    })}
                </svg>

                {erStore.tables.map(table => <TableNode key={table.id} table={table} />)}
            </div>

            {erStore.contextMenu.visible && (
                <div className="er_ctx_menu" style={{ left: erStore.contextMenu.x, top: erStore.contextMenu.y }}>
                    <div className="er_ctx_item" onClick={() => erStore.addTable()}>Добавить таблицу</div>
                </div>
            )}
        </div>
    );
});