export type EventType = 'INFO' 
    | 'WARNING'
    | 'ERROR';

export interface Event {
    id: string;
    type: EventType;
    message: string;
}