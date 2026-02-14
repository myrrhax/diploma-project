import React from 'react';
import { observer } from 'mobx-react-lite';
import { erStore, type Table } from '@/store/ERStore';
import './css/TableNode.css';

interface TableNodeProps {
    table: Table;
}

export const TableNode = observer(({ table }: TableNodeProps) => {
    return (
        <div 
            className="er_table_card" 
            style={{ 
                left: table.x, 
                top: table.y,
                width: erStore.TABLE_WIDTH // Используем константу из стора
            }}
        >
            {/* Header */}
            <div 
                className="er_table_header"
                onMouseDown={(e) => {
                    e.stopPropagation();
                    erStore.draggingTableId = table.id;
                }}
            >
                <input 
                    className="er_table_name_input"
                    value={table.name}
                    onChange={(e) => erStore.updateTableName(table.id, e.target.value)}
                    onMouseDown={(e) => e.stopPropagation()} 
                />
            </div>

            <div className="er_table_separator" />

            {/* Columns */}
            <div className="er_column_list">
                {table.columns.map((col, index) => {
                    const srcIdx = erStore.selectedSources.findIndex(s => s.colId === col.id);
                    const tgtIdx = erStore.selectedTargets.findIndex(t => t.colId === col.id);
                    const isSource = srcIdx !== -1;
                    const isTarget = tgtIdx !== -1;

                    return (
                        <div key={col.id} className="er_column_row">
                            {/* Input Port (Left) */}
                            <div 
                                className={`er_port port_left ${isTarget ? 'port_target_active' : ''}`}
                                onClick={() => erStore.handlePortClick('left', table.id, col.id)}
                                title="Input (Target)"
                            >
                                {isTarget && <span className="port_badge badge_left">{tgtIdx + 1}</span>}
                            </div>

                            <span className="col_name">{col.name}</span>
                            
                            {/* Output Port (Right) */}
                            <div 
                                className={`er_port port_right ${isSource ? 'port_source_active' : ''}`}
                                onClick={() => erStore.handlePortClick('right', table.id, col.id)}
                                title="Output (Source)"
                            >
                                {isSource && <span className="port_badge badge_right">{srcIdx + 1}</span>}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Footer */}
            <button className="er_add_col_btn" onClick={() => erStore.addColumn(table.id)}>
                + Add
            </button>
        </div>
    );
});