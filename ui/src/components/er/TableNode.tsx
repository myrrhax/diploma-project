import { observer } from 'mobx-react-lite';
import { erStore } from '@/store/ERStore';
import { type Table } from '@/model/SchemaElements';
import { useState } from 'react';
import { AddColumnMenu } from './AddColumnMenu';
import { TableColumn } from './TableColumn';
import './css/TableNode.css';

interface TableNodeProps {
    table: Table;
}

export const TableNode = observer(({ table }: TableNodeProps) => {
    const [isAddMenuOpen, setIsAddMenuOpen] = useState(false);

    return (
        <div 
            className="er_table_card" 
            style={{ 
                position: 'absolute',
                left: table.x, 
                top: table.y,
                width: erStore.TABLE_WIDTH
            }}
            onMouseDown={(e) => e.stopPropagation()}
        >
            <div 
                className="er_table_header"
                onMouseDown={(e) => {
                    e.stopPropagation();
                    erStore.setDraggingTable(table.id);
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

            <div className="er_column_list">
                {Object.values(table.columns).map((col, _) => (
                    <TableColumn table={table} col={col} />
                ))}
            </div>

            <button className="er_add_col_btn" onClick={() => setIsAddMenuOpen(!isAddMenuOpen)}>
                Добавить
            </button>

            {isAddMenuOpen && (
                <AddColumnMenu 
                    tableId={table.id} 
                    onClose={() => setIsAddMenuOpen(false)} 
                />
            )}
        </div>
    );
});