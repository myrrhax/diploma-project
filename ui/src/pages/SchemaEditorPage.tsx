import { useState, useCallback } from 'react';
import { observer } from 'mobx-react-lite';
import { 
  ReactFlow, 
  Background, 
  Controls, 
  MiniMap,
  useNodesState, 
  useEdgesState, 
  addEdge,
  type Connection,
  type Edge, 
  type Node, 
  ReactFlowProvider,
  useReactFlow,
  MarkerType, // Импортируем типы маркеров
} from '@xyflow/react';
import '@xyflow/react/dist/style.css'; 

import { TableNode } from '@/components/TableNode/TableNode.tsx';
import './css/SchemaEditorPage.css';
import profilePic from '@/assets/user.png';

const nodeTypes = { table: TableNode };

// ... (FAKE_VERSIONS и FAKE_USERS остаются прежними)
const FAKE_VERSIONS = [
    { id: 1, name: 'v1.0 - Initial', date: '12.02.2026' },
  ];
  
const FAKE_USERS = [
    { id: 1, email: 'admin@test.com' },
];

const SchemaEditorContent = observer(() => {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const { screenToFlowPosition } = useReactFlow();

  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const [isUsersOpen, setUsersOpen] = useState(true);
  
  // Меню для Узлов (Nodes)
  const [nodeMenu, setNodeMenu] = useState<{ x: number; y: number; visible: boolean } | null>(null);
  
  // Меню для Связей (Edges)
  const [edgeMenu, setEdgeMenu] = useState<{ x: number; y: number; edgeId: string; visible: boolean } | null>(null);

  const schemaName = "Система управления складом v2";

  // Подключение: по умолчанию ставим стрелку на конце (1 к М, где стрелка указывает на М)
  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge({ 
        ...params, 
        animated: true, 
        style: { stroke: '#2563eb', strokeWidth: 2 },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#2563eb' }, // Стрелка по умолчанию
    }, eds)),
    [setEdges],
  );

  // --- NODE CONTEXT MENU HANDLERS ---
  const onPaneContextMenu = useCallback((event: React.MouseEvent | MouseEvent) => {
      event.preventDefault();
      setNodeMenu({ x: event.clientX, y: event.clientY, visible: true });
      setEdgeMenu(null); // Закрываем меню связей
    }, []);

  // --- EDGE CONTEXT MENU HANDLERS ---
  const onEdgeContextMenu = useCallback((event: React.MouseEvent, edge: Edge) => {
    event.preventDefault();
    setEdgeMenu({ 
        x: event.clientX, 
        y: event.clientY, 
        edgeId: edge.id, 
        visible: true 
    });
    setNodeMenu(null); // Закрываем меню узлов
  }, []);

  const closeMenus = useCallback(() => {
      setNodeMenu(null);
      setEdgeMenu(null);
  }, []);

  // --- ACTIONS ---

  const createTable = useCallback(() => {
    if (!nodeMenu) return;
    const position = screenToFlowPosition({ x: nodeMenu.x, y: nodeMenu.y });
    
    const newNode: Node = {
      id: `table-${Date.now()}`,
      type: 'table',
      position,
      // ВАЖНО: Указываем селектор класса для перетаскивания
      dragHandle: '.custom-drag-handle', 
      data: { 
        label: 'New_Table', 
        columns: [
          { name: 'id', type: 'uuid', isPk: true },
          { name: 'name', type: 'varchar' },
        ] 
      },
    };

    setNodes((nds) => nds.concat(newNode));
    closeMenus();
  }, [nodeMenu, screenToFlowPosition, setNodes, closeMenus]);

  // Функция изменения типа связи
  const updateEdgeType = (type: '1:1' | '1:M' | 'M:1' | 'M:M') => {
      if (!edgeMenu) return;

      setEdges((eds) => eds.map((edge) => {
        if (edge.id === edgeMenu.edgeId) {
            let markerStart = undefined;
            let markerEnd = undefined;

            // Логика визуализации UML через стрелки
            switch (type) {
                case '1:1':
                    // Две вертикальные черты (имитируем отсутствием стрелок или специальным SVG, пока просто линии)
                    markerStart = { type: MarkerType.ArrowClosed, width: 10, height: 10, color: '#2563eb' }; // Временно стрелки с двух сторон
                    markerEnd = { type: MarkerType.ArrowClosed, width: 10, height: 10, color: '#2563eb' };
                    break;
                case '1:M':
                    // Стрелка указывает на Many
                    markerStart = undefined; 
                    markerEnd = { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#2563eb' };
                    break;
                case 'M:1':
                    // Стрелка указывает на Many (в обратную сторону)
                    markerStart = { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#2563eb' };
                    markerEnd = undefined;
                    break;
                case 'M:M':
                    // Crow's feet с обеих сторон (имитируем большими стрелками)
                    markerStart = { type: MarkerType.Arrow, width: 15, height: 15, color: '#2563eb' };
                    markerEnd = { type: MarkerType.Arrow, width: 15, height: 15, color: '#2563eb' };
                    break;
            }

            return { 
                ...edge, 
                markerStart, 
                markerEnd 
            };
        }
        return edge;
      }));
      closeMenus();
  };

  const deleteEdge = () => {
      if (!edgeMenu) return;
      setEdges((eds) => eds.filter(e => e.id !== edgeMenu.edgeId));
      closeMenus();
  }

  return (
    <div className="schema-page">
      <header className="schema-header">
        <div className="header-left">
          <button className="back-btn" onClick={() => window.history.back()}>◀</button>
          <h2 className="schema-name">{schemaName}</h2>
        </div>
        <div className="schema-controls">
          <button className="btn-secondary">Версии</button>
          <button className="btn-primary">Сохранить</button>
        </div>
      </header>

      <div className="schema-workspace">
        {/* SIDEBAR VERSIONS*/}
        <aside className={`versions-sidebar ${isSidebarOpen ? 'open' : ''}`}>
          <div className="sidebar-toggle" onClick={() => setSidebarOpen(!isSidebarOpen)}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d={isSidebarOpen ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}/></svg>
          </div>
          <div className="sidebar-content">
            <h3>История версий</h3>
            <div className="version-list">
              {FAKE_VERSIONS.map(v => (
                <div key={v.id} className="version-item">
                  <span className="version-name">{v.name}</span>
                  <span className="version-date">{v.date}</span>
                </div>
              ))}
            </div>
          </div>
        </aside>

        <main className="canvas-area">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            nodeTypes={nodeTypes}
            onPaneContextMenu={onPaneContextMenu} // ПКМ по пустому месту
            onEdgeContextMenu={onEdgeContextMenu} // ПКМ по связи
            onPaneClick={closeMenus} // Закрыть меню при клике
            fitView
          >
            <Background color="#e5e7eb" gap={20} size={1} />
            <Controls showInteractive={false} />
            <MiniMap style={{ height: 100 }} zoomable pannable />
            
            {/* NODE CONTEXT MENU */}
            {nodeMenu && nodeMenu.visible && (
              <div className="context-menu" style={{ top: nodeMenu.y, left: nodeMenu.x }}>
                <div className="menu-item" onClick={createTable}>
                  <span>🔲 Создать таблицу</span>
                </div>
              </div>
            )}

            {/* EDGE CONTEXT MENU */}
            {edgeMenu && edgeMenu.visible && (
               <div className="context-menu" style={{ top: edgeMenu.y, left: edgeMenu.x }}>
                  <div className="menu-item" style={{color: '#6b7280', fontSize: '11px', cursor: 'default'}}>
                      Тип связи:
                  </div>
                  <div className="menu-item" onClick={() => updateEdgeType('1:1')}>
                    <span>1 — 1 (One to One)</span>
                  </div>
                  <div className="menu-item" onClick={() => updateEdgeType('1:M')}>
                    <span>1 — M (One to Many)</span>
                  </div>
                  <div className="menu-item" onClick={() => updateEdgeType('M:1')}>
                    <span>M — 1 (Many to One)</span>
                  </div>
                  <div className="menu-item" onClick={() => updateEdgeType('M:M')}>
                    <span>M — M (Many to Many)</span>
                  </div>
                  <div className="menu-divider"></div>
                  <div className="menu-item" onClick={deleteEdge} style={{color: 'red'}}>
                      🗑 Удалить связь
                  </div>
               </div>
            )}

          </ReactFlow>

          <aside className={`users-overlay ${!isUsersOpen ? 'collapsed' : ''}`}>
            <div className="users-header" onClick={() => setUsersOpen(!isUsersOpen)}>
              <span>В сети ({FAKE_USERS.length})</span>
              <button className="toggle-users-btn">
                {isUsersOpen ? 
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6"/></svg> : 
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 15l-6-6-6 6"/></svg>
                }
              </button>
            </div>
            {isUsersOpen && (
              <div className="users-list">
                {FAKE_USERS.map(user => (
                  <div key={user.id} className="user-item">
                    <img src={profilePic} alt="user" className="user-avatar" />
                    <span className="user-email">{user.email}</span>
                  </div>
                ))}
              </div>
            )}  
          </aside>
        </main>
      </div>
    </div>
  );
});

export const SchemaEditorPage = () => (
  <ReactFlowProvider>
    <SchemaEditorContent />
  </ReactFlowProvider>
);