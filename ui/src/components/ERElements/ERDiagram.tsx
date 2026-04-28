import React, { useRef, useEffect, useState, useMemo } from 'react';
import { erStore } from '@/store/ERStore';
import { TableNode } from './TableNode';
import { type Table } from '@/model/SchemaElements';
import { refKeyToString } from '@/utils/UtilFunctions';
import { observer } from 'mobx-react-lite';
import { AddReferenceMenu } from './AddReferenceMenu';
import { referenceStore } from '@/store/ReferenceStore';
import { tableModalsStore } from '@/store/TableModalsStore';
import { DeleteTableModal } from './DeleteTableModal';
import { columnModalsStore } from '@/store/ColumnModalsStore';
import { DeleteColumnModal } from './DeleteColumnModal';
import { EditTableModal } from './EditTableModal';
import { participationsStore } from '@/store/ParticipationStore';
import { OverlaySpinner } from '../SpinnerLoader/SpinnerLoader';
import { ERZoomControls } from './ZoomControls';
import { eventsStore } from '@/store/EventsStore';
import { selectionStore } from '@/store/SelectionStore';
import './css/ERDiagram.css';
import './css/ERSelections.css';
import { CursorControls } from './CursorControls';

const getPortPosition = (table: Table, colId: string, side: 'left' | 'right') => {
    const column = table.columns[colId];
    if (!column) return { x: 0, y: 0 };
    const columnsArray = Object.keys(table.columns);    
    const colIndex = columnsArray.indexOf(colId); 
    
    const y = table.y + erStore.HEADER_HEIGHT + (colIndex * erStore.ROW_HEIGHT) + (erStore.ROW_HEIGHT / 2) + 7;
    const x = side === 'left' ? table.x : table.x + erStore.TABLE_WIDTH;
    
    return { x, y };
}

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
        return `M ${sX} ${sY} L ${sX + 20} ${sY} L ${sX + 20} ${safeY} L ${tX - 20} ${safeY} L ${tX - 20} ${tY} L ${tX} ${tY}`;
    }
};

export const ERDiagram = observer(() => {
    if (!erStore.state) {
        return (<OverlaySpinner text='Загрузка схемы...' />);
    }
    const containerRef = useRef<HTMLDivElement>(null);
    const [isPanning, setPanning] = useState(false);
    const { authorities } = participationsStore; 
    const { tables, references } = erStore.state;

    useEffect(() => {
        if (!containerRef.current) return;
        
        const resizeObserver = new ResizeObserver(entries => {
            for (let entry of entries) {
                erStore.setViewportSize(entry.contentRect.width, entry.contentRect.height);
                
                if (erStore.state && !erStore.isCentered) {
                    erStore.centerView();
                }
            }
        });
        
        resizeObserver.observe(containerRef.current);
        return () => resizeObserver.disconnect();
    }, [erStore.state]); 

    const handleUp = () => { 
        erStore.setDraggingTable(null);
        setPanning(false); 
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (erStore.draggingTableId) {
            erStore.moveTable(erStore.draggingTableId, e.movementX, e.movementY);
        } else if (isPanning || e.buttons === 4) {
            erStore.setPan(e.movementX, e.movementY);
        } else if (selectionStore.selectionBox) {
            const rect = containerRef.current?.getBoundingClientRect();
            if (rect) {
                const worldX = (e.clientX - rect.left - erStore.offsetX) / erStore.scale;
                const worldY = (e.clientY - rect.top - erStore.offsetY) / erStore.scale;
                selectionStore.updateSelectionBox(worldX, worldY);
            }
        }
    };

    const handleCloseMenu = () => {
        erStore.closeContextMenu();
        referenceStore.closeRefContextMenu();
        tableModalsStore.closeTableContextMenu(); 
        columnModalsStore.closeColumnContextMenu();
    }

    const handleModification = (action: () => void) => {
        if (!erStore.isEditable) {
            eventsStore.addError("Вы работаете с версией в режиме чтения. Изменения запрещены.");
            return;
        } 
        if (!authorities?.some(au => au === 'MODIFY_SCHEME' || au === 'ALL')) {
            return;
        }
        action();
    };

    const handleMultiDelete = (e: React.MouseEvent) => {
        e.stopPropagation();
        handleModification(() => {
            erStore.multiDelete();
            selectionStore.clear();
            handleCloseMenu();
        });
    };

    const handleOpenMenu = (screenX: number, screenY: number, relativeX: number, relativeY: number) => {
        handleModification(() => {
            erStore.openContextMenu(screenX, screenY, relativeX, relativeY);
        });
    };

    const handleMouseDown = (e: React.MouseEvent) => {
        if (e.button !== 0) return;

        if (selectionStore.mode === 'grab' || e.buttons === 4) {
            setPanning(true);
        } else if (selectionStore.mode === 'select') {
            if (!e.ctrlKey) {
                selectionStore.clear();
            }
            const rect = containerRef.current?.getBoundingClientRect();
            if (rect) {
                const worldX = (e.clientX - rect.left - erStore.offsetX) / erStore.scale;
                const worldY = (e.clientY - rect.top - erStore.offsetY) / erStore.scale;
                selectionStore.setSelectionBox({ startX: worldX, startY: worldY, endX: worldX, endY: worldY });
            }
        }
    };

    const handleMouseUp = (e: React.MouseEvent) => {
        setPanning(false);
        erStore.setDraggingTable(null);
        if (selectionStore.selectionBox) {
            selectionStore.applyBoxSelection(e.ctrlKey);
            selectionStore.setSelectionBox(null);
        }
    };

    useEffect(() => {
        window.addEventListener('mouseup', handleUp);
        return () => window.removeEventListener('mouseup', handleUp);
    }, []);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
                return;
            }

            if (e.key === 'Delete' || e.key === 'Backspace') {
                const hasSelections = selectionStore.selectedTableIds.size > 0 || selectionStore.selectedRefIds.size > 0;                
                if (hasSelections) {
                    handleModification(() => {
                        erStore.multiDelete();
                        selectionStore.clear();
                        handleCloseMenu();
                    });
                }
            }
        };

        window.addEventListener('keydown', handleKeyDown);        
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, []);

    const canModify = useMemo(() => {
        return authorities?.some(au => au === 'MODIFY_SCHEME' || au === 'ALL');
    }, [authorities]);

    const multiSelectCount = selectionStore.selectedTableIds.size + selectionStore.selectedRefIds.size;
    const showMultiMenu = multiSelectCount > 1 && (tableModalsStore.tableContextMenu.visible || (referenceStore.refContextMenu?.visible ?? false));
    const multiMenuX = tableModalsStore.tableContextMenu.visible ? tableModalsStore.tableContextMenu.x : (referenceStore.refContextMenu?.x || 0);
    const multiMenuY = tableModalsStore.tableContextMenu.visible ? tableModalsStore.tableContextMenu.y : (referenceStore.refContextMenu?.y || 0);

    return (
        <div 
            className="er_diagram_wrapper" 
            ref={containerRef}
            onWheel={(e) => erStore.zoom(e.deltaY)} 
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
            style={{ cursor: selectionStore.mode === 'grab' ? (isPanning ? 'grabbing' : 'grab') : 'crosshair' }}
            onClick={handleCloseMenu}
        >
            {erStore.isEditable && canModify && (
                <>
                    <AddReferenceMenu />
                    <DeleteTableModal />
                    <DeleteColumnModal />
                    <EditTableModal />
                </>
            )}
            
            <div className="er_viewport" style={{ transform: `translate(${erStore.offsetX}px, ${erStore.offsetY}px) scale(${erStore.scale})` }}>
                
                {selectionStore.selectionBox && (
                    <div 
                        className="er_selection_marquee"
                        style={{
                            left: Math.min(selectionStore.selectionBox.startX, selectionStore.selectionBox.endX),
                            top: Math.min(selectionStore.selectionBox.startY, selectionStore.selectionBox.endY),
                            width: Math.abs(selectionStore.selectionBox.endX - selectionStore.selectionBox.startX),
                            height: Math.abs(selectionStore.selectionBox.endY - selectionStore.selectionBox.startY)
                        }}
                    />
                )}

                <svg className="er_svg_layer">
                    <defs>
                        <marker id="marker-source-one" overflow='visible' orient='auto-start-reverse'>
                            <line x1="-10" y1="-4" x2="-10" y2="4" stroke="#94a3b8" strokeWidth="1.2" />
                        </marker>
                        
                        <marker id="marker-source-many" overflow="visible" orient="auto-start-reverse">
                            <line x1="-4" y1="-3" x2="-4" y2="3" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="-2" x2="-2" y2="2" stroke="#94a3b8" strokeWidth="1.2" />
                            <line x1="-6" y1="2" x2="-2" y2="-2" stroke="#94a3b8" strokeWidth="1.2" />
                        </marker>

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
                        const keyStr = refKeyToString(key);
                        const isSelected = selectionStore.selectedRefIds.has(keyStr);
                        const sTable = tables[key.fromTableId];
                        const tTable = tables[key.toTableId];
                        if (!sTable || !tTable) return null;

                        let sourceLabel = 'M';
                        let targetLabel = '1';
                        
                        let sourceTextOffset = 8;
                        let targetTextOffset = -14;

                        if (ref.type === 'ONE_TO_ONE') {
                            sourceLabel = '1'; 
                            targetLabel = '1';
                        } else if (ref.type === 'ONE_TO_MANY') {
                            sourceLabel = '1'; 
                            targetLabel = 'M';
                        } else if (ref.type === 'MANY_TO_ONE') {
                            sourceLabel = 'M'; 
                            targetLabel = '1';
                        } else if (ref.type === 'MANY_TO_MANY') {
                            sourceLabel = 'M'; 
                            targetLabel = 'M';
                            targetTextOffset = -8; 
                        }
                        
                        const handleRefContextMenu = (e: React.MouseEvent) => {
                            e.preventDefault();
                            e.stopPropagation(); 
                            handleModification(() => {
                                const rect = containerRef.current?.getBoundingClientRect();
                                if (rect) {
                                    referenceStore.openRefContextMenu(
                                        e.clientX - rect.left, 
                                        e.clientY - rect.top, 
                                        refKeyToString(key)
                                    );
                                }
                            });
                        };

                        const handleRefMouseDown = (e: React.MouseEvent) => {
                            e.stopPropagation();
                            if (e.button !== 0) return;
                            if (selectionStore.mode === 'select') {
                                selectionStore.toggleReference(keyStr, e.ctrlKey);
                            } else {
                                if (!isSelected && !e.ctrlKey) {
                                    selectionStore.clear();
                                    selectionStore.toggleReference(keyStr, false);
                                }
                            }
                        };

                        if (key.toColumns.length === 1) {
                            const start = getPortPosition(tables[key.fromTableId], key.fromColumns[0], 'right');
                            const end = getPortPosition(tables[key.toTableId], key.toColumns[0], 'left');
                            const d = getTrunkPath(tables, start.x, start.y, end.x, end.y, sTable.id, tTable.id, index);
                            
                            return ( 
                                <g 
                                    key={keyStr} 
                                    className={`er_relation_group ${isSelected ? 'selected' : ''}`} 
                                    style={{ cursor: selectionStore.mode === 'select' ? 'default' : 'grab' }} 
                                    onContextMenu={handleRefContextMenu}
                                    onMouseDown={handleRefMouseDown}
                                >
                                    <title>{ref.key.name || 'Связь'}</title>
                                    <path d={d} stroke="transparent" strokeWidth="15" fill="none" pointerEvents="stroke" />
                                    <path d={d} className="er_line" fill="none" />
                                    
                                    <text className='er_relation_label' x={start.x + sourceTextOffset} y={start.y - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{sourceLabel}</text>
                                    <text className='er_relation_label' x={end.x + targetTextOffset} y={end.y - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{targetLabel}</text>
                                </g>
                            );
                        } else {
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

                            let branchPaths = "";
                            sourcePoints.forEach(p => { branchPaths += `M ${p.x} ${p.y} L ${sBusX} ${p.y} `; });
                            branchPaths += `M ${sBusX} ${sMinY} L ${sBusX} ${sMaxY} `;
                            targetPoints.forEach(p => { branchPaths += `M ${tBusX} ${p.y} L ${p.x} ${p.y} `; });
                            branchPaths += `M ${tBusX} ${tMinY} L ${tBusX} ${tMaxY} `;

                            const trunkPath = getTrunkPath(tables, sBusX, sCenterY, tBusX, tCenterY, sTable.id, tTable.id, index);

                            return (
                                <g 
                                    key={keyStr} 
                                    className={`er_relation_group ${isSelected ? 'selected' : ''}`} 
                                    style={{ cursor: selectionStore.mode === 'select' ? 'default' : 'grab' }} 
                                    onContextMenu={handleRefContextMenu}
                                    onMouseDown={handleRefMouseDown}
                                >
                                    <title>{ref.key.name || 'Связь'}</title>
                                    <path d={branchPaths + trunkPath} stroke="transparent" strokeWidth="15" fill="none" pointerEvents="stroke" />
                                    
                                    <path d={branchPaths} className="er_line" fill="none" />
                                    <path d={trunkPath} className="er_line" fill="none" />
                                    
                                    <circle cx={sBusX} cy={sCenterY} r="3" fill="#64748b" />
                                    <circle cx={tBusX} cy={tCenterY} r="3" fill="#64748b" />

                                    <text className='er_relation_label' x={sBusX + sourceTextOffset} y={sCenterY - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{sourceLabel}</text>
                                    <text className='er_relation_label' x={tBusX + targetTextOffset} y={tCenterY - 12} textAnchor="middle" dominantBaseline="central" fill="#94a3b8" fontSize="12" fontWeight="bold">{targetLabel}</text>
                                </g>
                            );
                        }
                    })}
                </svg>

                {Object.values(tables).map(table => <TableNode key={table.id} table={table} />)}
            </div>
            
            <CursorControls />
            <ERZoomControls />

            {erStore.isEditable && erStore.contextMenu.visible && !showMultiMenu && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: erStore.contextMenu.x, top: erStore.contextMenu.y }}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            erStore.addTable();
                            handleCloseMenu();
                        }}
                    >
                        Добавить таблицу
                    </div>
                </div>
            )}

            {showMultiMenu && erStore.isEditable && canModify && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: multiMenuX, top: multiMenuY, zIndex: 1000 }}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div 
                        className="er_ctx_item" 
                        onClick={handleMultiDelete}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить выделенные ({multiSelectCount})
                    </div>
                </div>
            )}

            {!showMultiMenu && erStore.isEditable && canModify && referenceStore.refContextMenu?.visible && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: referenceStore.refContextMenu.x, top: referenceStore.refContextMenu.y, zIndex: 1000 }}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            erStore.deleteReference(referenceStore.refContextMenu!.refKeyStr);
                            handleCloseMenu();
                        }}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить
                    </div>
                </div>
            )}
            
            {!showMultiMenu && erStore.isEditable && canModify && tableModalsStore.tableContextMenu.visible && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: tableModalsStore.tableContextMenu.x, top: tableModalsStore.tableContextMenu.y, zIndex: 1000 }}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            tableModalsStore.openEdit();
                            tableModalsStore.closeTableContextMenu();
                        }}
                    >
                        Редактировать
                    </div>
                    
                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            tableModalsStore.open();
                            tableModalsStore.closeTableContextMenu();
                        }}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить таблицу
                    </div>
                </div>
            )}

            {erStore.isEditable && canModify && columnModalsStore.columnContextMenu.visible && (
                <div 
                    className="er_ctx_menu" 
                    style={{ left: columnModalsStore.columnContextMenu.x, top: columnModalsStore.columnContextMenu.y, zIndex: 1000 }}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            if (erStore) erStore.setActiveMenuId(columnModalsStore.columnContextMenu.colId);
                            columnModalsStore.closeColumnContextMenu();
                        }}
                        style={{ color: 'black' }}
                    >
                        Редактировать
                    </div>

                    <div 
                        className="er_ctx_item" 
                        onClick={(e) => {
                            e.stopPropagation();
                            columnModalsStore.open(columnModalsStore.columnContextMenu.tableId, columnModalsStore.columnContextMenu.colId);
                            columnModalsStore.closeColumnContextMenu();
                        }}
                        style={{ color: '#ef4444', fontWeight: 'bold' }}
                    >
                        Удалить колонку
                    </div>
                </div>
            )}
        </div>
    );
});