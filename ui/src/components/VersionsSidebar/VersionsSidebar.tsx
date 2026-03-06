import { useState, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { versionsStore } from '@/store/VersionsStore';
import { toVersionDateFormat } from '@/utils/UtilFunctions';
import { VersionTreeNode } from './VersionTreeNode'; // Импортируем новый компонент
import type { TreeNode } from '@/utils/Tree';
import type { Version } from '@/model/SchemaTypes';
import './css/VersionSidebar.css';

interface VersionsSidebarProps {
    isOpen: boolean;
    changeVisibleCallback: ((open: boolean) => void);
}

type DISPLAY_MODE = 'tree' | 'list';

export const VersionsSidebar = observer(({isOpen, changeVisibleCallback}: VersionsSidebarProps) => {
    const { isLoading, versions } = versionsStore;
    const [mode, setMode] = useState<DISPLAY_MODE>('tree');

    const sortedVersions = useMemo(() => {
        if (mode !== 'list' || !versions || versions.length === 0)  {
            return [];
        }
        let versionsCopy = [...versions];
        const indices = new Map();
        let workingCopyIndex = versionsCopy.length - 1;
        const workingCopy = versionsCopy[workingCopyIndex];
        
        if (workingCopy.isInitial || versions.length === 1) {
            return versions;
        }

        versionsCopy.pop();
        versionsCopy.forEach((v, i) => {
            indices.set(v.versionId, i);
        });

        const parentIndex = indices.get(workingCopy.parentId);
        if (parentIndex !== undefined) {
            versionsCopy.splice(parentIndex + 1, 0, workingCopy); 
        } else {
            versionsCopy.push(workingCopy); 
        }
        return versionsCopy;
    }, [mode, versions]);
    
    const versionsTree = useMemo(() => {
        if (mode !== 'tree' || !versions || versions.length === 0) {
            return { roots: [] };
        }

        const nodeMap = new Map<number, TreeNode<Version>>();
        const roots: TreeNode<Version>[] = [];
        let workingCopyNode: TreeNode<Version> | null = null;

        versions.forEach(v => {
            nodeMap.set(v.versionId, { value: v, children: [], containsWorkingCopy: false, visible: false, parent: null });
        });
        
        versions.forEach(v => {
            const currentNode = nodeMap.get(v.versionId)!;
            if (currentNode.value.isWorkingCopy) {
                workingCopyNode = currentNode;
                currentNode.containsWorkingCopy = true;
            }
            if (v.parentId) {
                const parentNode = nodeMap.get(v.parentId);
                if (parentNode) {
                    currentNode.parent = parentNode;
                    parentNode.children.push(currentNode);
                } else {
                    currentNode.parent = null;
                    roots.push(currentNode);
                }
            } else {
                roots.push(currentNode);
            }
        });

        if (!workingCopyNode) {
            console.error('Failed to convert version set to tree');
            return { roots };
        }
        
        let currentNode: TreeNode<Version> = workingCopyNode;
        currentNode.visible = true;
        currentNode.containsWorkingCopy = true;

        while (currentNode.parent) {
            currentNode.parent.containsWorkingCopy = true;
            currentNode.parent.visible = true;
            currentNode = currentNode.parent;
        }

        return { roots };
    }, [mode, versions]);

    return (
        <aside className={`versions-sidebar ${isOpen ? 'open' : ''}`}>
            <div className="sidebar-toggle" onClick={() => changeVisibleCallback(!isOpen)}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d={isOpen ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}/></svg>
            </div>
            
            <div className="sidebar-content">
                <div className="sidebar-header">
                    <h3>История версий</h3>
                    <div className="mode-switchers">
                        <button className={mode === 'list' ? 'active' : ''} onClick={() => setMode('list')}>List</button>
                        <button className={mode === 'tree' ? 'active' : ''} onClick={() => setMode('tree')}>Tree</button>
                    </div>
                </div>

                <div className="versions">
                    {isLoading ? (
                        <div className="loading-state">Загрузка...</div>
                    ) : mode === 'list' ? (
                        sortedVersions.map(v => (
                            <div 
                                key={v.versionId} 
                                className={`list-version-item ${v.isWorkingCopy ? 'list-version-working' : ''} ${v.isInitial ? 'list-version-initial' : ''}`}
                            >
                                <span className="list-version-name">{v.tag ?? 'Рабочая версия'}</span>
                                {v.versionedAt && (
                                    <span className="list-version-date">{toVersionDateFormat(v.versionedAt)}</span>
                                )}
                            </div>
                        ))
                    ) : (
                        versionsTree.roots.map(root => (
                            <VersionTreeNode key={root.value.versionId} node={root} level={0} />
                        ))
                    )}
                </div>
            </div>
        </aside>
    )
});