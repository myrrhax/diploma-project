import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJs from 'sockjs-client';
import { runInAction } from "mobx";
import { authStore } from "@/store/AuthStore";
import { erStore } from "@/store/ERStore";
import type { SchemaChangedEvent } from "@/model/SchemaEvents"; 
import type { MetadataCommandProcessResult } from "@/model/SchemaEvents";
import type { MetadataCommand } from "@/model/SchemaCommands";
import type ErrorResponse from "@/model/ErrorResponse";
import { errorsStore } from "@/store/ErrorsStore";

const WS_ENDPOINT = 'http://localhost:8000/ws';

class SchemaSocketService {
    private readonly ACCESS_DENIED_EXCEPTION = 'Access denied';
    private readonly INVALID_TOKEN_EXCEPTION = 'Invalid token';

    private client: Client | null = null;
    private subscription: StompSubscription | null = null;
    private activeSchemaId: string | null = null;
    private errorSubscription: StompSubscription | null = null;

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
                const oldSchemaId = this.activeSchemaId;
                this.leaveSchema();
                if (oldSchemaId) {
                    this.activeSchemaId = oldSchemaId;
                    this.executeSubscription(this.activeSchemaId);
                    this.executeErrorsQueueSubscription();
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
        
        if (this.client && this.client.connected && !this.errorSubscription) {
            this.executeErrorsQueueSubscription();   
        }
    }

    sendCommand(cmd: MetadataCommand) {
        if (!this.client || !this.subscription) {
            console.warn("[WS] Нет подключения, команда пропущена");
            return;
        }
        
        const destination = '/app/schema/' + cmd.schemeId;

        this.client.publish({
            destination: destination,
            body: JSON.stringify(cmd)
        });
    }

    leaveSchema() {
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
            console.log(`[WS] Отписались от схемы: ${this.activeSchemaId}`);
        }
        this.activeSchemaId = null;
        if (this.errorSubscription) {
            this.errorSubscription.unsubscribe();
            this.errorSubscription = null;
        }
    }

    disconnect() {
        this.leaveSchema();
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
    }

    private executeSubscription(schemaId: string) {
        if (!this.client || !this.client.connected) return;
        
        const topic = `/topic/schema/${schemaId}`;

        this.subscription = this.client.subscribe(topic, (msg) => {
            if (msg.body) {
                const event = JSON.parse(msg.body) as SchemaChangedEvent<MetadataCommandProcessResult>;
                console.log(`Received: ${event}`);
                this.handleDifference(event);
            }
        });

        console.log(`[WS] Подписаны на топик: ${topic}`);
    }

    private executeErrorsQueueSubscription() {
        if (!this.client || this.errorSubscription) {
            return;
        }
        const queue = '/user/queue/errors';
        this.errorSubscription = this.client.subscribe(queue, (msg) => {
            const body = JSON.parse(msg.body) as ErrorResponse;
            if (body) {
                this.handleError(body); 
            }
        });
        console.log('[WS] Подписка на Error Queue')
    }

    private handleError(error: ErrorResponse) {
        console.log('Ошибка: ' + error.message);
        errorsStore.addError(error.message);
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
}

export const schemaSocketService = new SchemaSocketService();