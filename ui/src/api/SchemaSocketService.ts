import type { Schema, Version } from "@/model/SchemaTypes";
import { authStore } from "@/store/AuthStore";
import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJs from 'sockjs-client';

const WS_ENDPOINT = 'http://localhost:8000/ws'

class SchemaSocketService {
    private client: Client | null = null;
    private subscription: StompSubscription | null = null;
    private schemaId: string | null = null;

    connect() {
        const token = authStore.token;

        this.client = new Client({
            webSocketFactory: () => new SockJs(WS_ENDPOINT),
            reconnectDelay: 5000,
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            onConnect: () => {
                console.log('Подключено к WS-серверу');
                if (this.schemaId) {
                    this.joinSchema(this.schemaId);
                }
            },
        });
    }

    private subscribe(schemaId: string) {
        if (!this.client || !this.client.connected) return;
        const topic = `/topic/schema/${schemaId}`;

        this.subscription = this.client.subscribe(topic, (msg) => {
            if (msg.body) {
                const schema = JSON.parse(msg.body) as Schema;
                if (version) {

                }
            }
        });
    }
}