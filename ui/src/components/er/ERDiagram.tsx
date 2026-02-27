import React, { useRef, useEffect, useState } from 'react';
import { erStore } from '@/store/ERStore';
import { TableNode } from './TableNode';
import { type Table } from '@/model/SchemaElements';
import { refKeyToString } from '@/utils/UtilFunctions';
import './css/ERDiagram.css';   
import { observer } from 'mobx-react-lite';
import { AddReferenceMenu } from './AddReferenceMenu';
import { referenceStore } from '@/store/ReferenceStore';
import { tableDeleteStore } from '@/store/TableDeleteStore';
import { DeleteTableModal } from './DeleteTableModal';

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
    } else {
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
        erStore.setDraggingTable(null);
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

    const handleCloseMenu = () => {
        erStore.closeContextMenu();
        referenceStore.closeRefContextMenu();
        tableDeleteStore.closeTableContextMenu(); 
    }

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
            <AddReferenceMenu />
            <DeleteTableModal />
            
            <div className="er_viewport" style={{ transform: `translate(${erStore.offsetX}px, ${erStore.offsetY}px) scale(${erStore.scale})` }}>
                <svg className="er_svg_layer">
                    <defs>
                        {/* --- SOURCE МАРКЕРЫ (Начало связи, без стрелки направления) --- */}
                        <marker id="marker-source-one" overflow='visible' orient='auto-start-reverse'>
                            <line x1="-10" y1="-4" x2="-10" y2="4" stroke="#94a3b8" strokeWidth="1.2" />
                        </marker>
                        
                        <marker id="marker-source-many" overflow="visible" orient="auto-start-reverse">
                            <line x1="-4" y1="-3" x2="-4" y2="3" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="-2" x2="-2" y2="2" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="2" x2="-2" y2="-2" stroke="#94a3b8" strokeWidth="1.2" />
                        </marker>

                        {/* --- TARGET МАРКЕРЫ (Конец связи, со стрелкой, указывающей на таблицу) --- */}
                        <marker id="marker-target-one" overflow='visible' orient='auto-start-reverse'>
                            <line x1="-8" y1="-6" x2="-8" y2="6" stroke="#94a3b8" strokeWidth="2" />
                            <path d="M -20 -4 L -12 0 L -20 4 z" fill="#94a3b8" />
                        </marker>
                        
                        <marker id="marker-target-many" overflow="visible" orient="auto-start-reverse">
                            <line x1="-4" y1="-3" x2="-4" y2="3" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="-2" x2="-2" y2="2" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="2" x2="-2" y2="-2" stroke="#94a3b8" strokeWidth="1.2" />
                            <path d="M -20 -4 L -12 0 L -20 4 z" fill="#94a3b8" />
                        </marker>
                    </defs>
                    
                    {Object.values(references).map((ref, index) => {
                        const key = ref.key;
                        const sTable = tables[key.fromTableId];
                        const tTable = tables[key.toTableId];
                        if (!sTable || !tTable) return null;

                        let sourceLabel = 'M';
                        let targetLabel = '1';
                        
                        let sourceMarker = 'url(#marker-source-many)';
                        let targetMarker = 'url(#marker-target-one)';
                        
                        let sourceTextOffset = 8;
                        let targetTextOffset = -14;

                        if (ref.type === 'ONE_TO_ONE') {
                            sourceLabel = '1'; targetLabel = '1';
                            sourceMarker = 'url(#marker-source-one)'; 
                            targetMarker = 'url(#marker-target-one)';
                        } else if (ref.type === 'ONE_TO_MANY') {
                            sourceLabel = '1'; targetLabel = 'M';
                            sourceMarker = 'url(#marker-source-one)'; 
                            targetMarker = 'url(#marker-target-many)';
                        } else if (ref.type === 'MANY_TO_ONE') {
                            sourceLabel = 'M'; targetLabel = '1';
                            sourceMarker = 'url(#marker-source-many)'; 
                            targetMarker = 'url(#marker-target-one)';
                        } else if (ref.type === 'MANY_TO_MANY') {
                            sourceLabel = 'M'; targetLabel = 'M';
                            sourceMarker = 'url(#marker-source-many)'; 
                            targetMarker = 'url(#marker-source-many)'; 
                            targetTextOffset = -8; 
                        }
                        
                        const handleRefContextMenu = (e: React.MouseEvent) => {
                            e.preventDefault();
                            e.stopPropagation(); 
                            const rect = containerRef.current?.getBoundingClientRect();
                            if (rect) {
                                referenceStore.openRefContextMenu(
                                    e.clientX - rect.left, 
                                    e.clientY - rect.top, 
                                    refKeyToString(key)
                                );
                            }
                        };

                        // --- ОДИНОЧНАЯ СВЯЗЬ ---
                        if (key.toColumns.length === 1) {
                            const start = getPortPosition(tables[key.fromTableId], key.fromColumns[0], 'right');
                            const end = getPortPosition(tables[key.toTableId], key.toColumns[0], 'left');
                            
                            const d = getTrunkPath(tables, start.x, start.y, end.x, end.y, sTable.id, tTable.id, index);
                            
                            return ( 
                                <g key={refKeyToString(key)} className="er_relation_group" style={{ cursor: 'context-menu' }} onContextMenu={handleRefContextMenu}>
                                    {/* Хитбокс */}
                                    <path d={d} stroke="transparent" strokeWidth="15" fill="none" />
                                    {/* Линия с маркерами на концах */}
                                    <path d={d} className="er_line" markerStart={sourceMarker} markerEnd={targetMarker} fill="none" />
                                    
                                    {/* Подписи "1" и "M" */}
                                    <text x={start.x + sourceTextOffset} y={start.y - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{sourceLabel}</text>
                                    <text x={end.x + targetTextOffset} y={end.y - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{targetLabel}</text>
                                </g>
                            );
                        }

                        // --- МНОЖЕСТВЕННАЯ СВЯЗЬ (Составной ключ) ---
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

                            // Ветки (от портов до магистрали) - БЕЗ маркеров
                            let branchPaths = "";
                            sourcePoints.forEach(p => { branchPaths += `M ${p.x} ${p.y} L ${sBusX} ${p.y} `; });
                            branchPaths += `M ${sBusX} ${sMinY} L ${sBusX} ${sMaxY} `;
                            targetPoints.forEach(p => { branchPaths += `M ${tBusX} ${p.y} L ${p.x} ${p.y} `; });
                            branchPaths += `M ${tBusX} ${tMinY} L ${tBusX} ${tMaxY} `;

                            // Сама магистраль (соединяет две таблицы) - С маркерами
                            const trunkPath = getTrunkPath(tables, sBusX, sCenterY, tBusX, tCenterY, sTable.id, tTable.id, index);

                            return (
                                <g key={refKeyToString(key)} className="er_relation_group" style={{ cursor: 'context-menu' }} onContextMenu={handleRefContextMenu}>
                                    {/* Хитбокс для всей связи */}
                                    <path d={branchPaths + trunkPath} stroke="transparent" strokeWidth="15" fill="none" />
                                    
                                    {/* Отрисовка веток без маркеров */}
                                    <path d={branchPaths} className="er_line" fill="none" />
                                    
                                    {/* Отрисовка магистрали с маркерами */}
                                    <path d={trunkPath} className="er_line" markerStart={sourceMarker} markerEnd={targetMarker} fill="none" />
                                    
                                    {/* Узелки на стыке портов и магистрали */}
                                    <circle cx={sBusX} cy={sCenterY} r="3" fill="#64748b" />
                                    <circle cx={tBusX} cy={tCenterY} r="3" fill="#64748b" />

                                    {/* Подписи "1" и "M" около магистрали */}
                                    <text x={sBusX + sourceTextOffset} y={sCenterY - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{sourceLabel}</text>
                                    <text x={tBusX + targetTextOffset} y={tCenterY - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{targetLabel}</text>
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

            {referenceStore.refContextMenu?.visible && (
                <div className="er_ctx_menu" style={{ left: referenceStore.refContextMenu.x, top: referenceStore.refContextMenu.y, zIndex: 1000 }}>
                    <div 
                        className="er_ctx_item" 
                        onClick={() => erStore.deleteReference(referenceStore.refContextMenu.refKeyStr)}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить
                    </div>
                </div>
            )}
            
            {tableDeleteStore.tableContextMenu.visible && (
                <div className="er_ctx_menu" style={{ left: tableDeleteStore.tableContextMenu.x, top: tableDeleteStore.tableContextMenu.y, zIndex: 1000 }}>
                    <div 
                        className="er_ctx_item" 
                        onClick={() => {
                            tableDeleteStore.open();
                            tableDeleteStore.closeTableContextMenu();
                        }}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить таблицу
                    </div>
                </div>
            )}
        </div>
    );
});