import React, { useRef, useEffect, useState } from 'react';
import { erStore } from '@/store/ERStore';
import { TableNode } from './TableNode';
import { type Table } from '@/model/SchemaElements';
import { refKeyToString } from '@/utils/UtilFunctions';
import './css/ERDiagram.css';   
import { observer } from 'mobx-react-lite';

const getPortPosition = (table: Table, colId: string, side: 'left' | 'right') => {
    const column = table.columns[colId];
    if (!column) return { x: 0, y: 0 };
    const columnsArray = Object.keys(table.columns);    
    const colIndex = columnsArray.indexOf(colId); 
    
    const y = table.y + erStore.HEADER_HEIGHT + (colIndex * erStore.ROW_HEIGHT) + (erStore.ROW_HEIGHT / 2);
    const x = side === 'left' ? table.x : table.x + erStore.TABLE_WIDTH;
    
    return { x, y };
};

const getTrunkPath = (
    tables: Record<string, Table>,
    sX: number, sY: number,
    tX: number, tY: number,
    sTableId: string, 
    tTableId: string,
    offsetIndex: number = 0
) => {
    const sHeight = erStore.getTableHeight(sTableId);
    const tHeight = erStore.getTableHeight(tTableId);
    
    const sTableY = tables[sTableId].y || 0;
    const tTableY = tables[tTableId].y || 0;
    
    const sBottom = sTableY + sHeight;
    const tBottom = tTableY + tHeight;
    
    const gap = offsetIndex * 15;

    if (tX > sX + 40) {
        const midX = (sX + tX) / 2;
        return `M ${sX} ${sY} L ${midX} ${sY} L ${midX} ${tY} L ${tX} ${tY}`;
    }
    
    else {
        const safeY = Math.max(sBottom, tBottom) + 20 + gap;
        
        return `M ${sX} ${sY} 
                L ${sX + 20} ${sY} 
                L ${sX + 20} ${safeY} 
                L ${tX - 20} ${safeY} 
                L ${tX - 20} ${tY} 
                L ${tX} ${tY}`;
    }
};
    

export const ERDiagram = observer(() => {
    if (!erStore.state) {
        return (<div>Загрузка</div>);
    }
    const containerRef = useRef<HTMLDivElement>(null);
    const [isPanning, setPanning] = useState(false);

    const handleUp = () => { 
        erStore.draggingTableId = null;
        setPanning(false); 
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (erStore.draggingTableId) {
            erStore.moveTable(erStore.draggingTableId, e.movementX, e.movementY);
        }
        else if (isPanning || e.buttons === 4) {
            erStore.setPan(e.movementX, e.movementY);
        }
    };

    const handleCloseMenu = () => erStore.closeContextMenu();

    const handleOpenMenu = (screenX: number, screenY: number, relativeX: number, relativeY: number) => {
        erStore.openContextMenu(screenX, screenY, relativeX, relativeY);
    };

    const handleMouseDown = (e: React.MouseEvent) => {
        if (e.button === 0) {
            setPanning(true);
        }
    };

    const handleMouseUp = (_: React.MouseEvent) => {
        setPanning(false);
    }

    useEffect(() => {
        window.addEventListener('mouseup', handleUp);
        return () => window.removeEventListener('mouseup', handleUp);
    }, []);

    const { tables, references } = erStore.state;

    return (
        <div 
            className="er_diagram_wrapper" 
            ref={containerRef}
            onWheel={(e) => erStore.scale = Math.max(0.3, Math.min(2, erStore.scale + e.deltaY * -0.001))}
            onMouseMove={handleMouseMove}
            onMouseDown={handleMouseDown}
            onMouseUp={handleMouseUp}
            onContextMenu={(e) => {
                e.preventDefault();
                const rect = containerRef.current?.getBoundingClientRect();
                if (rect) {
                    handleOpenMenu(e.clientX, e.clientY, e.clientX - rect.left, e.clientY - rect.top);
                }
            }}
            style={{ cursor: isPanning ? 'grabbing' : 'default' }}
            onClick={handleCloseMenu}
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
                    
                    {Object.values(references).map((ref, index) => {
                        const key = ref.key;

                        const sTable = tables[key.fromTableId];
                        const tTable = tables[key.toTableId];
                        if (!sTable || !tTable) return null;
                        
                        if (key.toColumns.length === 1) {
                            const start = getPortPosition(tables[key.fromTableId], key.fromColumns[0], 'right');
                            const end = getPortPosition(tables[key.toTableId], key.toColumns[0], 'left');
                            
                            const d = getTrunkPath(tables, start.x, start.y, end.x, end.y, sTable.id, tTable.id, index);
                            
                            return ( 
                                <path 
                                    key={refKeyToString(key)} 
                                    d={d} 
                                    className="er_line" 
                                    markerEnd="url(#arrow)" 
                                />
                            );
                        }

                        else {
                            const sourcePoints = key.fromColumns.map(p => getPortPosition(sTable, p, 'right'));
                            const targetPoints = key.toColumns.map(p => getPortPosition(tTable, p, 'left'));

                            const sMinY = Math.min(...sourcePoints.map(p => p.y));
                            const sMaxY = Math.max(...sourcePoints.map(p => p.y));
                            const tMinY = Math.min(...targetPoints.map(p => p.y));
                            const tMaxY = Math.max(...targetPoints.map(p => p.y));

                            const sBusX = sTable.x + erStore.TABLE_WIDTH + 20;
                            const tBusX = tTable.x - 20;

                            const sCenterY = (sMinY + sMaxY) / 2;
                            const tCenterY = (tMinY + tMaxY) / 2;

                            let pathData = "";

                            sourcePoints.forEach(p => {
                                pathData += `M ${p.x} ${p.y} L ${sBusX} ${p.y} `;
                            });

                            pathData += `M ${sBusX} ${sMinY} L ${sBusX} ${sMaxY} `;

                            targetPoints.forEach(p => {
                                pathData += `M ${tBusX} ${p.y} L ${p.x} ${p.y} `; // Здесь стрелка придет в порт
                            });

                            pathData += `M ${tBusX} ${tMinY} L ${tBusX} ${tMaxY} `;

                            const trunkPath = getTrunkPath(tables, sBusX, sCenterY, tBusX, tCenterY, sTable.id, tTable.id, index);
                            
                            pathData += trunkPath;

                            return (
                                <g key={refKeyToString(key)} className="er_relation_group">
                                    <path 
                                        d={pathData} 
                                        className="er_line" 
                                        markerEnd="url(#arrow)"
                                        fill="none"
                                    />
                                    <circle cx={sBusX} cy={sCenterY} r="3" fill="#64748b" />
                                    <circle cx={tBusX} cy={tCenterY} r="3" fill="#64748b" />
                                </g>
                            );
                        }
                    })}
                </svg>

                {Object.values(tables).map(table => <TableNode key={table.id} table={table} />)}
            </div>

            {erStore.contextMenu.visible && (
                <div className="er_ctx_menu" style={{ left: erStore.contextMenu.x, top: erStore.contextMenu.y }}>
                    <div className="er_ctx_item" onClick={() => erStore.addTable()}>Добавить таблицу</div>
                </div>
            )}
        </div>
    );
});