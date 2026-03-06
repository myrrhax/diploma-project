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

export const toVersionDateFormat = (rawDate: Date | string | number): string => {
    const date = new Date(rawDate);

    if (isNaN(date.getTime())) {
        return 'Неизвестная дата'; 
    }

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = String(date.getFullYear()).slice(-2);
    
    const hours = String(date.getHours()).padStart(2, '0'); 
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}/${month}/${year} ${hours}:${minutes}`;
}