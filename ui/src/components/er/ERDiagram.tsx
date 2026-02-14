import React, { useRef, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { erStore, type Table } from '@/store/ERStore';
import './css/ERDiagram.css';
import { TableNode } from './TableNode';

export const ERDiagram = observer(() => {
    const containerRef = useRef<HTMLDivElement>(null);

    // Global MouseUp to stop dragging
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

    const handleContextMenu = (e: React.MouseEvent) => {
        e.preventDefault();
        const rect = containerRef.current?.getBoundingClientRect();
        if (!rect) return;
        
        const relativeX = e.clientX - rect.left;
        const relativeY = e.clientY - rect.top;
        erStore.openContextMenu(e.clientX - rect.left, e.clientY - rect.top, relativeX, relativeY);
    };

    const getPortPosition = (table: Table, colId: string, side: 'left' | 'right') => {
        const colIndex = table.columns.findIndex(c => c.id === colId);
        if (colIndex === -1) return { x: 0, y: 0 };

        const HEADER_HEIGHT = 40; // Высота хедера + сепаратора
        const ROW_HEIGHT = 32;    // Высота строки колонки
        const HALF_ROW = 16;
        
        // Координаты относительно канваса
        const y = table.y + HEADER_HEIGHT + (colIndex * ROW_HEIGHT) + HALF_ROW;
        const x = side === 'left' ? table.x : table.x + 220; // 220 - ширина таблицы

        return { x, y };
    };

    const getSmartPath = (x1: number, y1: number, x2: number, y2: number) => {
        // Рисуем кривую Безье или ломаную для красоты
        const controlOffset = 40;
        return `M ${x1} ${y1} C ${x1 + controlOffset} ${y1}, ${x2 - controlOffset} ${y2}, ${x2} ${y2}`;
    };

    return (
        <div 
            className="er_diagram_wrapper" 
            ref={containerRef}
            onWheel={(e) => erStore.scale = Math.max(0.3, Math.min(2, erStore.scale + e.deltaY * -0.001))}
            onMouseMove={handleMouseMove}
            onContextMenu={handleContextMenu}
            onClick={() => erStore.closeContextMenu()}
        >
            <div className="er_viewport" style={{ transform: `translate(${erStore.offsetX}px, ${erStore.offsetY}px) scale(${erStore.scale})` }}>
                
                {/* SVG Layer */}
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
                            
                            // Смещение линий, если их несколько, чтобы не слипались
                            const offsetY = (idx * 2) - ((rel.pairs.length * 2) / 2); 

                            return (
                                <path 
                                    key={`${rel.id}-${idx}`}
                                    d={getSmartPath(start.x, start.y + offsetY, end.x, end.y + offsetY)}
                                    className="er_line"
                                    markerEnd="url(#arrow)"
                                />
                            );
                        });
                    })}
                    
                    {/* Визуализация процесса соединения (линия от последнего выбранного сорса к мышке - опционально) */}
                    {/* Для чистоты UI пока не делаем линию к мышке, так как выделено может быть много сорсов */}
                </svg>

                {/* Nodes Layer */}
                {erStore.tables.map(table => <TableNode key={table.id} table={table} />)}
            </div>

            {/* Context Menu */}
            {erStore.contextMenu.visible && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: erStore.contextMenu.x, top: erStore.contextMenu.y }}
                >
                    <div className="er_ctx_item" onClick={() => erStore.addTable()}>Добавить таблицу</div>
                </div>
            )}
            
            {/* Info Hint */}
            <div className="er_hint">
                ЛКМ по правой точке — выбрать выход (Source).<br/>
                ЛКМ по левой точке — выбрать вход (Target).<br/>
                Соединение создается автоматически при совпадении кол-ва.
            </div>
        </div>
    );
});