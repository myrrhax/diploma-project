import React, { useRef, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore, type Table } from '@/store/ERStore';
import { TableNode } from './TableNode';
import './css/ERDiagram.css';

// --- Хелпер для получения координат порта ---
const getPortPosition = (table: Table, colId: string, side: 'left' | 'right') => {
    const colIndex = table.columns.findIndex(c => c.id === colId);
    if (colIndex === -1) return { x: 0, y: 0 };
    
    const y = table.y + erStore.HEADER_HEIGHT + (colIndex * erStore.ROW_HEIGHT) + (erStore.ROW_HEIGHT / 2);
    const x = side === 'left' ? table.x : table.x + erStore.TABLE_WIDTH;
    return { x, y };
};

// --- ALGORITHM: Smart Orthogonal Routing (TRUNK) ---
// Рассчитывает путь ГЛАВНОЙ (толстой) линии между двумя "шинами" или точками
const getTrunkPath = (
    sX: number, sY: number, // Координаты начала магистрали (от шины источника)
    tX: number, tY: number, // Координаты конца магистрали (у шины приемника)
    sTableId: string, 
    tTableId: string,
    offsetIndex: number = 0
) => {
    const sHeight = erStore.getTableHeight(sTableId);
    const tHeight = erStore.getTableHeight(tTableId);
    
    const sTableY = erStore.tables.find(t => t.id === sTableId)?.y || 0;
    const tTableY = erStore.tables.find(t => t.id === tTableId)?.y || 0;
    
    // Низ таблиц для огибания
    const sBottom = sTableY + sHeight;
    const tBottom = tTableY + tHeight;
    
    const gap = offsetIndex * 15; // Отступ между параллельными магистралями

    // 1. ПРЯМАЯ МАГИСТРАЛЬ (Target справа)
    // Условие: tX правее sX хотя бы на 40px
    if (tX > sX + 40) {
        const midX = (sX + tX) / 2;
        return `M ${sX} ${sY} L ${midX} ${sY} L ${midX} ${tY} L ${tX} ${tY}`;
    }
    
    // 2. ОГИБАЮЩАЯ МАГИСТРАЛЬ (Target слева или близко)
    else {
        const safeY = Math.max(sBottom, tBottom) + 20 + gap;
        
        // P1: Start
        // P2: Вниз до безопасной зоны
        // P3: Влево до уровня Target
        // P4: Вверх до уровня Target Y
        // P5: Finish
        
        return `M ${sX} ${sY} 
                L ${sX + 20} ${sY} 
                L ${sX + 20} ${safeY} 
                L ${tX - 20} ${safeY} 
                L ${tX - 20} ${tY} 
                L ${tX} ${tY}`;
    }
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
                        {/* Маркер для начала ветвления (опционально, точка) */}
                        <marker id="dot" markerWidth="6" markerHeight="6" refX="3" refY="3" orient="auto">
                             <circle cx="3" cy="3" r="2" fill="#64748b" />
                        </marker>
                    </defs>
                    
                    {erStore.relations.map((rel, relIndex) => {
                        const sTable = erStore.tables.find(t => t.id === rel.sourceTableId);
                        const tTable = erStore.tables.find(t => t.id === rel.targetTableId);
                        if (!sTable || !tTable) return null;

                        // 1. SINGLE CONNECTION (Простая линия)
                        if (rel.pairs.length === 1) {
                            const pair = rel.pairs[0];
                            const start = getPortPosition(sTable, pair.sourceColId, 'right');
                            const end = getPortPosition(tTable, pair.targetColId, 'left');
                            
                            const d = getTrunkPath(start.x, start.y, end.x, end.y, sTable.id, tTable.id, relIndex);
                            
                            return (
                                <path 
                                    key={rel.id} 
                                    d={d} 
                                    className="er_line" 
                                    markerEnd="url(#arrow)" 
                                />
                            );
                        }

                        // 2. MULTI CONNECTION (Разветвление / Шина)
                        else {
                            // Собираем координаты всех точек
                            const sourcePoints = rel.pairs.map(p => getPortPosition(sTable, p.sourceColId, 'right'));
                            const targetPoints = rel.pairs.map(p => getPortPosition(tTable, p.targetColId, 'left'));

                            // Находим границы для вертикальной "скобки"
                            const sMinY = Math.min(...sourcePoints.map(p => p.y));
                            const sMaxY = Math.max(...sourcePoints.map(p => p.y));
                            const tMinY = Math.min(...targetPoints.map(p => p.y));
                            const tMaxY = Math.max(...targetPoints.map(p => p.y));

                            // Определяем X-координаты "шин" (отступ 20px от таблицы)
                            const sBusX = sTable.x + erStore.TABLE_WIDTH + 20;
                            const tBusX = tTable.x - 20;

                            // Центры шин (откуда пойдет главная линия)
                            const sCenterY = (sMinY + sMaxY) / 2;
                            const tCenterY = (tMinY + tMaxY) / 2;

                            // Генерируем пути
                            let pathData = "";

                            // А. Отростки от Source Port до Source Bus
                            sourcePoints.forEach(p => {
                                pathData += `M ${p.x} ${p.y} L ${sBusX} ${p.y} `;
                            });

                            // Б. Вертикальная линия Source Bus
                            pathData += `M ${sBusX} ${sMinY} L ${sBusX} ${sMaxY} `;

                            // В. Отростки от Target Bus до Target Port
                            targetPoints.forEach(p => {
                                pathData += `M ${tBusX} ${p.y} L ${p.x} ${p.y} `; // Здесь стрелка придет в порт
                            });

                            // Г. Вертикальная линия Target Bus
                            pathData += `M ${tBusX} ${tMinY} L ${tBusX} ${tMaxY} `;

                            // Д. ГЛАВНАЯ МАГИСТРАЛЬ (Trunk) между центрами шин
                            // Используем sBusX и tBusX как точки старта/конца магистрали
                            const trunkPath = getTrunkPath(sBusX, sCenterY, tBusX, tCenterY, sTable.id, tTable.id, relIndex);
                            
                            pathData += trunkPath;

                            return (
                                <g key={rel.id} className="er_relation_group">
                                    <path 
                                        d={pathData} 
                                        className="er_line" 
                                        markerEnd="url(#arrow)" // Стрелка будет только в самом конце путей (у портов)
                                        fill="none"
                                    />
                                    {/* Можно добавить кружочек в центре разветвления */}
                                    <circle cx={sBusX} cy={sCenterY} r="3" fill="#64748b" />
                                    <circle cx={tBusX} cy={tCenterY} r="3" fill="#64748b" />
                                </g>
                            );
                        }
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