import type { Event } from "@/model/Events";
import { makeAutoObservable } from "mobx";
import { v4 } from 'uuid';

class EventsStore {
    readonly MAX_EVENTS = 10;
    readonly EVENT_TTL = 5000;

    events: Event[] = [];

    constructor() {
        makeAutoObservable(this);
    }

    addError(msg: string) {
        const id = v4();
        this.addEvent({id: id.toString(), message: msg, type: 'ERROR' });
    }

    addInfo(msg: string) {
        const id = v4();
        this.addEvent({id: id.toString(), message: msg, type: 'INFO' });
    }

    addWarn(msg: string) {
        const id = v4();
        this.addEvent({id: id.toString(), message: msg, type: 'WARNING' });
    }

    private addEvent(event: Event) {
        if (this.events.length > this.MAX_EVENTS) {
            this.events.shift();
        }
        this.events.push(event);

        setTimeout(() => {
            this.removeEvent(event.id);
        }, this.EVENT_TTL)
    }

    removeEvent(id: string) {
        this.events = this.events.filter(event => event.id !== id);
    }
}

export const eventsStore = new EventsStore();