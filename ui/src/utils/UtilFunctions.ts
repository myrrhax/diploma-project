import { type ReferenceKey } from "@/model/SchemaElements";

export const length = (record: Record<PropertyKey, unknown>): number => {
    if (!record) return 0;

    return Object.keys(record).length;
}

export const refKeyToString = (key: ReferenceKey) => {
    return `${key.fromTableId}:(${key.fromColumns.join(',')})->${key.toTableId}:(${key.toColumns.join(',')})`
}

export const compareAndReturnNew = <T> (oldVal: T, newVal: T): T | null => {
    return oldVal === newVal ? null : newVal;
}