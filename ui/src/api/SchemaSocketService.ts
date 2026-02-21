import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJs from 'sockjs-client';
import { runInAction } from "mobx";
import { authStore } from "@/store/AuthStore";
import { erStore } from "@/store/ERStore";
import type { SchemaChangedEvent } from "@/model/SchemaEvents"; 
import type { MetadataCommandProcessResult } from "@/model/SchemaEvents";

const WS_ENDPOINT = 'http://localhost:8000/ws';

class SchemaSocketService {
    private readonly ACCESS_DENIED_EXCEPTION = 'Access denied';
    private readonly INVALID_TOKEN_EXCEPTION = 'Invalid token';

    private client: Client | null = null;
    private subscription: StompSubscription | null = null;
    private activeSchemaId: string | null = null;

    connect() {
        if (this.client) return;

        this.client = new Client({
            webSocketFactory: () => new SockJs(WS_ENDPOINT),
            reconnectDelay: 5000,
            connectHeaders: {
                Authorization: `Bearer ${authStore.token}`
            },
            onConnect: () => {
                console.log('[WS] Успешно подключено к серверу');
                if (this.activeSchemaId) {
                    this.executeSubscription(this.activeSchemaId);
                }
            },
            onStompError: (frame) => {
                const errorMessage = frame.headers['message'];
                console.error('[WS] Ошибка брокера:', errorMessage);
                const msgLower = errorMessage.toLowerCase();
                if (msgLower.includes(this.ACCESS_DENIED_EXCEPTION) || msgLower.includes(this.INVALID_TOKEN_EXCEPTION)) {
                    this.client?.deactivate();
                    this.leaveSchema();
                    runInAction(() => {
                        erStore.deny();
                    });
                }
            },
            onWebSocketError: (event) => {
                console.error('[WS] Ошибка физического соединения (SockJS):', event);
            }
        });

        this.client.activate(); 
    }

    joinSchema(schemaId: string) {
        if (this.subscription && this.activeSchemaId !== schemaId) {
            this.leaveSchema();
        }

        this.activeSchemaId = schemaId;

        if (this.client && this.client.connected) {
            this.executeSubscription(schemaId);
        }
    }

    leaveSchema() {
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
            console.log(`[WS] Отписались от схемы: ${this.activeSchemaId}`);
        }
        this.activeSchemaId = null;
    }

    private executeSubscription(schemaId: string) {
        if (!this.client || !this.client.connected) return;
        
        const topic = `/topic/schema/${schemaId}`;

        this.subscription = this.client.subscribe(topic, (msg) => {
            if (msg.body) {
                const event = JSON.parse(msg.body) as SchemaChangedEvent<MetadataCommandProcessResult>;
                this.handleDifference(event);
            }
        });

        console.log(`[WS] Подписаны на топик: ${topic}`);
    }

    private handleDifference(event: SchemaChangedEvent<MetadataCommandProcessResult>) {
        runInAction(() => {
            if (event.eventType === 'SCHEMA_UPDATE') {
                if (erStore.state == null) {
                    return;
                }
                erStore.process(event.type);
            }
        });
    }

    // Полное отключение при выходе из приложения/разлогине
    disconnect() {
        this.leaveSchema();
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
    }
}

export const schemaSocketService = new SchemaSocketService();