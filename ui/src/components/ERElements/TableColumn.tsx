import type { Column, Table } from "@/model/SchemaElements";
import { observer } from "mobx-react-lite";
import { erStore } from "@/store/ERStore";
import './css/TableColumn.css';
import { AddColumnMenu } from "./AddColumnMenu";
import { referenceStore } from "@/store/ReferenceStore";
import { columnModalsStore } from "@/store/ColumnModalsStore";
import { eventsStore } from "@/store/EventsStore";

interface TableColumnProps {
    col: Column;
    table: Table;
    canModify: boolean;
}

export const TableColumn = observer(({ canModify, col, table }: TableColumnProps) => {
    const srcIdx = referenceStore.sourceCols.indexOf(col.id);
    const tgtIdx = referenceStore.targetCols.indexOf(col.id);
    const isSource = srcIdx !== -1;
    const isTarget = tgtIdx !== -1;
    const isNotNull = col.constraints?.includes('NOT_NULL');
    const isUnique = col.constraints?.includes('UNIQUE');
    const tooltipText = col.description ? col.description : col.name;
    const isEditMenuOpen = erStore.activeMenuId === col.id;

    const handleModification = (action: () => void) => {
        if (!canModify) {
            eventsStore.addWarn('Вы не можете изменять схему!');
            return;
        }
        action();
    };

    const getMenuPositionNearPort = (e: React.MouseEvent<HTMLDivElement>) => {
        const portRect = e.currentTarget.getBoundingClientRect();

        const wrapper = e.currentTarget.closest('.er_diagram_wrapper');
        const wrapperRect = wrapper?.getBoundingClientRect();

        if (!wrapperRect) {
            return {
                x: table.x + erStore.TABLE_WIDTH + 16,
                y: table.y
            };
        }

        const OFFSET = 16;

        const screenX = portRect.right - wrapperRect.left + OFFSET;
        const screenY = portRect.top - wrapperRect.top + portRect.height / 2;

        return {
            x: (screenX - erStore.offsetX) / erStore.scale,
            y: (screenY - erStore.offsetY) / erStore.scale
        };
    };

    return (
        <div 
            key={col.id} 
            className="er_column_row" 
            title={tooltipText}
            onContextMenu={(e) => {
                e.preventDefault();
                e.stopPropagation();

                handleModification(() => {
                    const wrapper = e.currentTarget.closest('.er_diagram_wrapper');
                    const rect = wrapper?.getBoundingClientRect();
                    
                    if (rect) {
                        columnModalsStore.openColumnContextMenu(
                            e.clientX - rect.left, 
                            e.clientY - rect.top, 
                            table.id,
                            col.id
                        );
                    }
                });
            }}
        >
            <div 
                className={`er_port port_left ${isTarget ? 'port_target_active' : ''}`}
                onClick={(e) => handleModification(() => referenceStore.handlePortClick('left', table.id, col.id, e.clientX, e.clientY))}
                title="Input (Target)"
            >
                {isTarget && <span className="port_badge badge_left">{tgtIdx + 1}</span>}
            </div>

            <div className="col_info_wrapper">
                {col.pkPart && <span title="Primary Key" style={{ fontSize: '12px' }}>🔑</span>}
                
                <span className="col_name">
                    {col.name}
                </span>

                <span className="col_type">
                    {col.columnType}
                </span>
                
                <div className="col_constraint_container">
                    {isNotNull && <span title="NOT NULL" className="col_constraint">NN</span>}
                    {isUnique && <span title="UNIQUE" className="col_constraint">UQ</span>}
                </div>
            </div>
            
            <div 
                className={`er_port port_right ${isSource ? 'port_source_active' : ''}`}
                onClick={(e) => {
                    e.stopPropagation();

                    handleModification(() => {
                        const pos = getMenuPositionNearPort(e);

                        referenceStore.handlePortClick(
                            'right',
                            table.id,
                            col.id,
                            pos.x,
                            pos.y
                        );
                    });
                }}
                title="Output (Source)"
            >
                {isSource && <span className="port_badge badge_right">{srcIdx + 1}</span>}
            </div>

            {isEditMenuOpen && (
                <AddColumnMenu
                    tableId={table.id}
                    oldColumn={col}
                    onCancel={() => erStore.setActiveMenuId(null)}
                    onClose={(updatedColumn) => {
                        if (col !== updatedColumn) {
                            erStore.updateColumn(table.id, col, updatedColumn);
                        }
                        
                        erStore.setActiveMenuId(null);
                    }}
                />
            )}
        </div>
    )
})