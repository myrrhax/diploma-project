import { useState, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { versionsStore } from '@/store/VersionsStore';
import { toVersionDateFormat } from '@/utils/UtilFunctions';
import { VersionTreeNode } from './VersionTreeNode';
import type { TreeNode } from '@/utils/Tree';
import type { Version } from '@/model/SchemaTypes';
import { contextMenuStore } from '@/store/VersionContextMenuStore';
import { VersionContextMenu } from './VersionContextMenu';
import { OverlaySpinner } from '../SpinnerLoader/SpinnerLoader';
import './css/VersionsContent.css';

type DISPLAY_MODE = 'tree' | 'list';

export const VersionsContent = observer(() => {
    const { isLoading, versions } = versionsStore;
    const [mode, setMode] = useState<DISPLAY_MODE>('list');

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

    const handleListContextMenu = (e: React.MouseEvent, version: Version) => {
        e.preventDefault();
        if (version.isWorkingCopy) 
            return;
        contextMenuStore.open(e.clientX, e.clientY, version);
    };

    return (
        <div className="versions-content-wrapper">
            <div className="versions-header-controls">
                <span>Отображение</span>
                <div className="mode-switchers">
                    <button className={mode === 'list' ? 'active' : ''} onClick={() => setMode('list')}>List</button>
                    <button className={mode === 'tree' ? 'active' : ''} onClick={() => setMode('tree')}>Tree</button>
                </div>
            </div>

            <div className="versions-list-container">
                {isLoading ? (
                    <OverlaySpinner text='Загрузка...' />
                ) : mode === 'list' ? (
                    sortedVersions.map(v => (
                        <div
                            onClick={() => {
                                if (v.isWorkingCopy) {
                                    return;
                                }
                                const url = `/schema/${v.schemeId}/version/${v.versionId}`;
                                window.open(url, '_blank', 'noopener,noreferrer');
                            }}
                            key={v.versionId} 
                            className={`list-version-item ${v.isWorkingCopy ? 'list-version-working' : ''} ${v.isInitial ? 'list-version-initial' : ''}`}
                            onContextMenu={(e) => handleListContextMenu(e, v)}
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

            <VersionContextMenu />
        </div>
    );
});