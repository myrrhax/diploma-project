export interface Tree<T> {
    roots: TreeNode<T>[];
}

export interface TreeNode<T> {
    visible: boolean;
    containsWorkingCopy: boolean;
    value: T;
    parent: TreeNode<T> | null;
    children: TreeNode<T>[];
}