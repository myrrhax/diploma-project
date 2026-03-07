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
import { versionsStore } from "@/store/VersionsStore";
import type { Version } from "@/model/SchemaTypes";

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

    saveVersion(id: string, tag: string) {
        if (!this.client || !this.client.connected) return;

        const destination = '/app/schema/' + id + '/saveVersion';
        this.client.publish({
            destination: destination,
            body: JSON.stringify({ tag: tag })
        });
    }

    deleteVersion(version: Version) {
        if (!this.client || !this.client.connected) return;

        const destination = '/app/schema/' + version.schemeId + '/deleteVersion';
        this.client.publish({
            destination: destination,
            body: JSON.stringify({ versionId: version.versionId })
        })
    }

    changeHead(fromVersion: Version, toVersion: Version) {
        if (!this.client || !this.client.connected) return;

        const destination = '/app/schema/' + fromVersion.schemeId + '/changeHead';
        this.client.publish({
            destination: destination,
            body: JSON.stringify({ currentVersionId: fromVersion.versionId, toVersionId: toVersion.versionId })
        })
    }

    private executeSubscription(schemaId: string) {
        if (!this.client || !this.client.connected) return;
        
        const topic = `/topic/schema/${schemaId}`;

        this.subscription = this.client.subscribe(topic, (msg) => {
            if (msg.body) {
                const body = JSON.parse(msg.body) as SchemaChangedEvent<any>;
                console.log('Received: ', body);
                if (body.eventType === 'SCHEMA_UPDATE') {
                    erStore.process(body.payload);
                } else if (body.eventType === 'SCHEMA_NEW_VERSION' || body.eventType === 'SCHEMA_VERSION_DELETED') {
                    versionsStore.setVersions(body.payload);
                } else if (body.eventType === 'SCHEMA_HEAD_CHANGED') {
                    const version = body.payload;
                    console.log('Updating head: ', version);
                    versionsStore.versions = [...versionsStore.versions.filter(v => v.versionId !== version.versionId), version];
                    versionsStore.currentVersion = version;
                    erStore.state = version.currentState;
                }     
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
}

export const schemaSocketService = new SchemaSocketService();