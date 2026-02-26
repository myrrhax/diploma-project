import type { Column, Table } from "@/model/SchemaElements";
import { observer } from "mobx-react-lite";
import { erStore } from "@/store/ERStore";
import './css/TableColumn.css';
import { AddColumnMenu } from "./AddColumnMenu";

interface TableColumnProps {
    col: Column;
    table: Table;
}

export const TableColumn = observer(({ col, table }: TableColumnProps) => {
    const srcIdx = erStore.selectedSources.findIndex(s => s.colId === col.id);
    const tgtIdx = erStore.selectedTargets.findIndex(t => t.colId === col.id);
    const isSource = srcIdx !== -1;
    const isTarget = tgtIdx !== -1;
    const isNotNull = col.constraints?.includes('NOT_NULL');
    const isUnique = col.constraints?.includes('UNIQUE');
    const tooltipText = col.description ? col.description : col.name;
    const isEditMenuOpen = erStore.activeMenuId === col.id;

    return (
        <div 
            key={col.id} 
            className="er_column_row" 
            title={tooltipText}
        >
            <div 
                className={`er_port port_left ${isTarget ? 'port_target_active' : ''}`}
                onClick={() => erStore.handlePortClick('left', table.id, col.id)}
                title="Input (Target)"
            >
                {isTarget && <span className="port_badge badge_left">{tgtIdx + 1}</span>}
            </div>

            <div className="col_info_wrapper" onClick={() => erStore.setActiveMenuId(col.id)}>
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
                onClick={() => erStore.handlePortClick('right', table.id, col.id)}
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