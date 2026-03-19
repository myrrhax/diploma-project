import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJs from 'sockjs-client';
import { runInAction } from "mobx";
import { authStore } from "@/store/AuthStore";
import { erStore } from "@/store/ERStore";
import { errorsStore } from "@/store/ErrorsStore";
import { versionsStore } from "@/store/VersionsStore";
import { wsConnectionStore } from "@/store/WsConnectionStore";
import { participationsStore } from "@/store/ParticipationStore";
import type { 
    SchemaChangedEvent, 
    ConnectionChangedPayload 
} from "@/model/SchemaEvents"; 
import type { MetadataCommand } from "@/model/SchemaCommands";
import type ErrorResponse from "@/model/ErrorResponse";
import type { Version } from "@/model/SchemaTypes";
import type { User } from "@/model/User";

const WS_ENDPOINT = 'http://localhost:8000/ws';

class SchemaSocketService {
    private readonly ACCESS_DENIED_EXCEPTION = 'Access denied';
    private readonly INVALID_TOKEN_EXCEPTION = 'Invalid token';

    private client: Client | null = null;
    private subscription: StompSubscription | null = null;
    private activeSchemaId: string | null = null;
    private errorSubscription: StompSubscription | null = null;
    
    // Подписки для отслеживания присутствия
    private schemaConnectionsSubscription: StompSubscription | null = null;
    private schemaConnectionsQueue: StompSubscription | null = null;

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
                    this.executeTopicSubscriptions(this.activeSchemaId);
                }
                
                runInAction(() => {
                    wsConnectionStore.isConnected = true;
                });
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
            onWebSocketClose: (event) => {
                console.warn('[WS] Физическое соединение закрыто:', event);
                runInAction(() => {
                    wsConnectionStore.isConnected = false;
                });
            },
            onWebSocketError: (event) => {
                console.error('[WS] Ошибка физического соединения (SockJS):', event);
                runInAction(() => {
                    wsConnectionStore.isConnected = false;
                });
            },
            onDisconnect: (_) => {
                console.log('[WS] Штатное отключение (Disconnect)');
                runInAction(() => {
                    wsConnectionStore.isConnected = false;
                });
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
            this.executeTopicSubscriptions(schemaId);
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
        
        if (this.schemaConnectionsSubscription) {
            this.schemaConnectionsSubscription.unsubscribe();
            this.schemaConnectionsSubscription = null;
            console.log(`[WS] Отписались от топика подключений`);
        }
        
        if (this.schemaConnectionsQueue) {
            this.schemaConnectionsQueue.unsubscribe();
            this.schemaConnectionsQueue = null;
            console.log(`[WS] Отписались от очереди пользователей`);
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
        });
    }

    changeHead(fromVersion: Version, toVersion: Version) {
        if (!this.client || !this.client.connected) return;

        const destination = '/app/schema/' + fromVersion.schemeId + '/changeHead';
        this.client.publish({
            destination: destination,
            body: JSON.stringify({ 
                currentVersionId: fromVersion.versionId, 
                toVersionId: toVersion.versionId 
            })
        });
    }

    private executeTopicSubscriptions(schemaId: string) {
        if (!this.client || !this.client.connected) return;        
        const userQueue = `/user/queue/schema-connections/${schemaId}/users`;
        
        this.schemaConnectionsQueue = this.client.subscribe(userQueue, (msg) => {
            if (msg.body) {
                const currentUsers = JSON.parse(msg.body) as User[];
                console.log('[WS] Получен начальный список пользователей:', currentUsers);
                
                runInAction(() => {
                    participationsStore.setOnlineUsers(currentUsers);
                });
            }
        });
        console.log(`[WS] Подписаны на приватную очередь: ${userQueue}`);

        const topic = `/topic/schema-connections/${schemaId}`;
        
        this.schemaConnectionsSubscription = this.client.subscribe(topic, (msg) => {
            if (msg.body) {
                const body = JSON.parse(msg.body) as SchemaChangedEvent<ConnectionChangedPayload>;
                
                if (body.eventType === 'CONNECTION_CHANGED') {
                    const payload = body.payload;
                    console.log(`[WS] Изменение статуса пользователя:`, payload);
                    
                    runInAction(() => {
                        if (payload.type === 'CONNECTED') {
                            participationsStore.addUser(payload.user);
                        } else if (payload.type === 'DISCONNECTED') {
                            participationsStore.removeUser(payload.user);
                        }
                    });
                }
            }
        });
        console.log(`[WS] Подписаны на топик подключений: ${topic}`);
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
                    
                    runInAction(() => {
                        versionsStore.versions = [...versionsStore.versions.filter(v => v.versionId !== version.versionId), version];
                        versionsStore.currentVersion = version;
                        erStore.state = version.currentState;
                    });
                }    
            }
        });

        console.log(`[WS] Подписаны на топик схемы: ${topic}`);
    }

    private executeErrorsQueueSubscription() {
        if (!this.client || this.errorSubscription) {
            return;
        }
        
        const queue = '/user/queue/errors';
        
        this.errorSubscription = this.client.subscribe(queue, (msg) => {
            if (msg.body) {
                const body = JSON.parse(msg.body) as ErrorResponse;
                this.handleError(body); 
            }
        });
        
        console.log('[WS] Подписка на Error Queue');
    }

    private handleError(error: ErrorResponse) {
        console.log('Ошибка: ' + error.message);
        runInAction(() => {
            errorsStore.addError(error.message);
        });
    }
}

export const schemaSocketService = new SchemaSocketService();