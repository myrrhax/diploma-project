import { observer } from 'mobx-react-lite';
import { erStore, type Table } from '@/store/ERStore';
import './css/TableNode.css';

export const TableNode = observer(({ table }: { table: Table }) => {
    return (
        <div className="er_table_card" style={{ left: table.x, top: table.y }}>
            
            {/* 1. HEADER (Draggable, Editable Name) */}
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
                    onMouseDown={(e) => e.stopPropagation()} // Чтобы текст выделялся, а не тащился
                />
            </div>

            {/* Separator Line */}
            <div className="er_table_separator" />

            {/* 2. BODY (Columns + Ports) */}
            <div className="er_column_list">
                {table.columns.map(col => {
                    // Проверяем, выбран ли порт
                    const isSourceSelected = erStore.selectedSources.some(s => s.colId === col.id);
                    // Вычисляем индекс выбора для отображения (1, 2, 3...)
                    const selectionIndex = erStore.selectedSources.findIndex(s => s.colId === col.id) + 1;

                    return (
                        <div key={col.id} className="er_column_row">
                            {/* LEFT PORT (Input) */}
                            <div 
                                className="er_port port_left"
                                onClick={() => erStore.handlePortClick('left', table.id, col.id)}
                                title="Input (Target)"
                            />

                            <span className="col_name">{col.name}</span>
                            
                            {/* RIGHT PORT (Output) */}
                            <div 
                                className={`er_port port_right ${isSourceSelected ? 'port_selected' : ''}`}
                                onClick={() => erStore.handlePortClick('right', table.id, col.id)}
                                title="Output (Source)"
                            >
                                {isSourceSelected && <span className="port_badge">{selectionIndex}</span>}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Footer Button */}
            <button className="er_add_col_btn" onClick={() => erStore.addColumn(table.id)}>
                + Добавить колонку
            </button>
        </div>
    );
});