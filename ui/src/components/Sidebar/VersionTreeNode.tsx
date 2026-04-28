import { useState, useEffect } from 'react';
import { toVersionDateFormat } from '@/utils/UtilFunctions';
import type { TreeNode } from '@/utils/Tree';
import type { Version } from '@/model/SchemaTypes';
import { contextMenuStore } from '@/store/VersionContextMenuStore';
import './css/VersionTreeNode.css';

interface VersionTreeNodeProps {
    node: TreeNode<Version>;
    level: number;
}

export const VersionTreeNode = ({ node, level }: VersionTreeNodeProps) => {
    const hasChildren = node.children && node.children.length > 0;
    const isForcedOpen = node.containsWorkingCopy; 
    
    const [isExpanded, setIsExpanded] = useState(node.visible || isForcedOpen);

    const handleToggle = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (!isForcedOpen) {
            setIsExpanded(!isExpanded);
        }
    };

    useEffect(() => {
        if (isForcedOpen || node.visible) {
            setIsExpanded(true);
        }
    }, [isForcedOpen, node.visible]);

    const handleContextMenu = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (node.value.isWorkingCopy) {
            return;
        }
        contextMenuStore.open(e.clientX, e.clientY, node.value);
    };

    const v = node.value;

    return (
        <div className="tree-node-container">
            <div 
                className={`tree-version-item ${v.isWorkingCopy ? 'tree-version-working' : ''} ${v.isInitial ? 'tree-version-initial' : ''}`}
                style={{ paddingLeft: `${10 + level * 20}px` }}
                onContextMenu={handleContextMenu}
            >
                <div className="tree-toggle-wrapper">
                    {hasChildren ? (
                        <button 
                            onClick={handleToggle}
                            disabled={isForcedOpen}
                            className={`tree-toggle-btn ${isForcedOpen ? 'disabled' : ''}`}
                        >
                            {isExpanded ? '-' : '+'}
                        </button>
                    ) : (
                        <span className="tree-toggle-placeholder" /> 
                    )}
                </div>
                
                <div className="tree-version-info"
                    onClick={() => {
                        if (v.isWorkingCopy) {
                            return;
                        }
                        const url = `/schema/${v.schemeId}/version/${v.versionId}`;
                        window.open(url, '_blank', 'noopener,noreferrer');
                    }}>
                    <span className="tree-version-name">{v.tag ?? 'Рабочая версия'}</span>
                    {v.versionedAt && (
                        <span className="tree-version-date">
                            {toVersionDateFormat(v.versionedAt)}
                        </span>
                    )}
                </div>
            </div>

            {isExpanded && hasChildren && (
                <div className="tree-children">
                    {node.children.map(child => (
                        <VersionTreeNode key={child.value.versionId} node={child} level={level + 1} />
                    ))}
                </div>
            )}
        </div>
    );
};