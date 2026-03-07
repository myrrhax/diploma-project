import type { Column, Table } from "@/model/SchemaElements";
import { observer } from "mobx-react-lite";
import { erStore } from "@/store/ERStore";
import './css/TableColumn.css';
import { AddColumnMenu } from "./AddColumnMenu";
import { referenceStore } from "@/store/ReferenceStore";
import { columnModalsStore } from "@/store/ColumnModalsStore";

interface TableColumnProps {
    col: Column;
    table: Table;
}

export const TableColumn = observer(({ col, table }: TableColumnProps) => {
    const srcIdx = referenceStore.sourceCols.indexOf(col.id);
    const tgtIdx = referenceStore.targetCols.indexOf(col.id);
    const isSource = srcIdx !== -1;
    const isTarget = tgtIdx !== -1;
    const isNotNull = col.constraints?.includes('NOT_NULL');
    const isUnique = col.constraints?.includes('UNIQUE');
    const tooltipText = col.description ? col.description : col.name;
    const isEditMenuOpen = erStore.activeMenuId === col.id;

    const handleModification = (action: () => void) => {
        if (!erStore.isEditable) {
            alert("Вы работаете с версией в режиме чтения. Изменения запрещены.");
            return;
        }
        action();
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
                onClick={(e) => handleModification(() => referenceStore.handlePortClick('right', table.id, col.id, e.clientX, e.clientY))}
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