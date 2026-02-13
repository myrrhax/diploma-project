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

import { UsersOverlay } from '@/components/UsersOverlay/UsersOverlay';
import { VersionsSidebar } from '@/components/VersionsSidebar/VersionsSidebar';

const nodeTypes = { table: TableNode };

// ... (FAKE_VERSIONS и FAKE_USERS остаются прежними)
const FAKE_VERSIONS = [
    { id: 1, name: 'v1.0 - Initial', date: '12.02.2026' },
  ];
  
const FAKE_USERS = [
    { id: 'first-id', email: 'admin@test.com', isConfirmed: true },
];

const SchemaEditorContent = observer(() => {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const { screenToFlowPosition } = useReactFlow();

  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const [isUsersOpen, setUsersOpen] = useState(true);
  const [nodeMenu, setNodeMenu] = useState<{ x: number; y: number; visible: boolean } | null>(null);
  const [edgeMenu, setEdgeMenu] = useState<{ x: number; y: number; edgeId: string; visible: boolean } | null>(null);

  const schemaName = "Система управления складом v2";

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge({ 
        ...params, 
        animated: true, 
        style: { stroke: '#2563eb', strokeWidth: 2 },
        markerStart: undefined,
        markerEnd: { type: MarkerType.ArrowClosed, color: '#2563eb' },
    }, eds)),
    [setEdges],
  );

  const onPaneContextMenu = useCallback((event: React.MouseEvent | MouseEvent) => {
      event.preventDefault();
      setNodeMenu({ x: event.clientX, y: event.clientY, visible: true });
      setEdgeMenu(null); 
    }, []);

  const onEdgeContextMenu = useCallback((event: React.MouseEvent, edge: Edge) => {
    event.preventDefault();
    setEdgeMenu({ 
        x: event.clientX, 
        y: event.clientY, 
        edgeId: edge.id, 
        visible: true 
    });
    setNodeMenu(null);
  }, []);

  const closeMenus = useCallback(() => {
      setNodeMenu(null);
      setEdgeMenu(null);
  }, []);


  const createTable = useCallback(() => {
    if (!nodeMenu) return;
    const position = screenToFlowPosition({ x: nodeMenu.x, y: nodeMenu.y });
    
    const newNode: Node = {
      id: `table-${Date.now()}`,
      type: 'table',
      position,
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

  const updateEdgeType = (type: '1:1' | '1:M' | 'M:1' | 'M:M') => {
      if (!edgeMenu) return;

      setEdges((eds) => eds.map((edge) => {
        if (edge.id === edgeMenu.edgeId) {
            let markerStart = undefined;
            let markerEnd = undefined;

            switch (type) {
                case '1:1':
                    // markerStart = { type: MarkerType.ArrowClosed, width: 10, height: 10, color: '#2563eb' }; 
                    markerStart = undefined;
                    markerEnd = { type: MarkerType.ArrowClosed, width: 10, height: 10, color: '#2563eb' };
                    break;
                case '1:M':
                    markerStart = undefined; 
                    markerEnd = { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#2563eb' };
                    break;
                case 'M:1':
                    markerStart = { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#2563eb' };
                    markerEnd = undefined;
                    break;
                case 'M:M':
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
        <VersionsSidebar isOpen={isSidebarOpen} versions={FAKE_VERSIONS} changeVisibleCallback={(visible) => setSidebarOpen(visible)} />

        <main className="canvas-area">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            nodeTypes={nodeTypes}
            onPaneContextMenu={onPaneContextMenu}
            onEdgeContextMenu={onEdgeContextMenu}
            onPaneClick={closeMenus}
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
          <UsersOverlay isUsersOpen={isUsersOpen} users={FAKE_USERS} closeCallback={(close) => setUsersOpen(close)} />
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