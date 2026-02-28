import { observer } from 'mobx-react-lite';
import { erStore } from '@/store/ERStore';
import { type Table } from '@/model/SchemaElements';
import { AddColumnMenu } from './AddColumnMenu';
import { TableColumn } from './TableColumn';
import './css/TableNode.css';
import { tableModalsStore } from '@/store/TableModalsStore';

interface TableNodeProps {
    table: Table;
}

export const TableNode = observer(({ table }: TableNodeProps) => {
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
            onContextMenu={(e) => {
                e.preventDefault();
                e.stopPropagation();

                const wrapper = e.currentTarget.closest('.er_diagram_wrapper');
                const rect = wrapper?.getBoundingClientRect();
                
                if (rect) {
                    tableModalsStore.openTableContextMenu(
                        e.clientX - rect.left, 
                        e.clientY - rect.top, 
                        table.id
                    );
                }
            }}
        >
            <div 
                className="er_table_header"
                onMouseDown={(e) => {
                    e.stopPropagation();
                    erStore.setDraggingTable(table.id);
                }}
                title={table.description ?? table.name}
            >
                {table.name}
            </div>

            <div className="er_table_separator" />

            <div className="er_column_list">
                {Object.values(table.columns).map((col, _) => (
                    <TableColumn table={table} col={col} />
                ))}
            </div>

            <button className="er_add_col_btn" onClick={() => erStore.setActiveMenuId(table.id)}>
                Добавить
            </button>

            {erStore.activeMenuId === table.id && (
                <AddColumnMenu 
                    tableId={ table.id }
                    onClose={(col) => {
                        erStore.addColumn(table.id, col);
                        erStore.setActiveMenuId(null);
                    }} 
                    onCancel={() => erStore.setActiveMenuId(null)} 
                />
            )}
        </div>
    );
});