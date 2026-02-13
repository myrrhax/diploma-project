import { Handle, Position } from '@xyflow/react';
import './TableNode.css';

export type TableNodeData = {
  label: string;
  columns: { name: string; type: string; isPk?: boolean }[];
};

export const TableNode = ({ data }: { data: TableNodeData }) => {
  return (
    <div className="table-node">
      {/* Добавляем класс custom-drag-handle для ограничения области перетаскивания */}
      <div className="table-node-header custom-drag-handle">
        <strong>{data.label}</strong>
      </div>
      <div className="table-node-body">
        {data.columns.map((col, idx) => (
          <div key={idx} className="table-row">
            {/* Левый хэндл (Target) - Вход */}
            <Handle 
              type="target" 
              position={Position.Left} 
              id={`t-${col.name}`} 
              className="handle-target"
            />
            
            <span className={`col-name ${col.isPk ? 'pk' : ''}`}>
              {col.isPk && '🔑 '} {col.name}
            </span>
            <span className="col-type">{col.type}</span>

            {/* Правый хэндл (Source) - Выход */}
            <Handle 
              type="source" 
              position={Position.Right} 
              id={`s-${col.name}`} 
              className="handle-source"
            />
          </div>
        ))}
      </div>
    </div>
  );
};