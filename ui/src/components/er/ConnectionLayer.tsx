import { erStore } from "@/store/ERStore";
import { observer } from "mobx-react-lite";

const getElbowPath = (x1: number, y1: number, x2: number, y2: number) => {
  const midX = (x1 + x2) / 2;
  return `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2} L ${x2} ${y2}`;
};

export const ConnectionLayer = observer(() => {
  const { relations, tables, connectingSource, mousePos } = erStore;

  const getTableCoords = (id: string) => {
    const t = tables.find(x => x.id === id);
    if (!t) return { x: 0, y: 0, w: 0, h: 0 };
    return { x: t.x, y: t.y, w: 200, h: 100 + t.columns.length * 30 };
  };

  return (
    <svg className="svg-layer" style={{ width: '100%', height: '100%' }}>
      <defs>
        {/* Маркеры для стрелок */}
        <marker id="arrow-end" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
          <path d="M0,0 L0,6 L9,3 z" fill="#555" />
        </marker>
        <marker id="crows-foot-many" markerWidth="12" markerHeight="12" refX="12" refY="6" orient="auto">
           <path d="M0,6 L12,6 M12,0 L0,6 L12,12" stroke="#555" fill="none"/>
        </marker>
        <marker id="one-bar" markerWidth="12" markerHeight="12" refX="12" refY="6" orient="auto">
           <path d="M6,0 L6,12 M11,0 L11,12" stroke="#555" fill="none"/>
        </marker>
      </defs>

      {/* Отрисовка существующих связей */}
      {relations.map(rel => {
        const source = getTableCoords(rel.sourceTableId);
        const target = getTableCoords(rel.targetTableId);
        
        // Вычисляем точки привязки (справа у source, слева у target)
        const x1 = source.x + source.w;
        const y1 = source.y + source.h / 2;
        const x2 = target.x;
        const y2 = target.y + target.h / 2;

        return (
          <g key={rel.id}>
            <path 
              d={getElbowPath(x1, y1, x2, y2)} 
              stroke="black"
              markerEnd={`url(#${rel.type === '1:N' || rel.type === 'N:M' ? 'crows-foot-many' : 'one-bar'})`}
              markerStart={`url(#${rel.type === 'N:M' ? 'crows-foot-many' : 'one-bar'})`}
            />
            {/* Лейбл для количества колонок в связи (мультисвязь) */}
            <text x={(x1+x2)/2} y={(y1+y2)/2 - 10} fill="#666" textAnchor="middle" fontSize="12">
               {rel.columnPairs.length > 1 ? `(${rel.columnPairs.length} keys)` : ''}
            </text>
          </g>
        );
      })}

      {/* Временная линия при перетаскивании */}
      {connectingSource && (
        <line 
          x1={getTableCoords(connectingSource.tableId).x + 200} // Из правой части
          y1={getTableCoords(connectingSource.tableId).y + 30} 
          x2={mousePos.x} 
          y2={mousePos.y} 
          stroke="blue" 
          strokeDasharray="5,5" 
        />
      )}
    </svg>
  );
});