// @ts-ignore
import Stomp, { Client, Subscription } from 'stompjs';
import {RabbitMQNotificationType} from "./RabbitMQNotificationType";
import store from "../store";
import {fetchRooms, fetchRoomStats} from "../features/rooms/roomsReducer";
import {fetchDevices} from "../features/devices/devicesReducer";
import {RestAPIHandler} from "./RestAPIHandler";

export class RabbitMQHandler {

    private readonly _username: string;
    private readonly _password: string;
    private readonly _websocket: WebSocket;
    private readonly _client: Client;
    private readonly _apiHandler: RestAPIHandler;

    private readonly _subscriptions: Array<Subscription>;

    constructor(username: string, password: string, webSocketAddress: string) {
        this._username = username;
        this._password = password;
        this._websocket = new WebSocket(webSocketAddress);
        this._client = Stomp.over(this._websocket)
        this._subscriptions = [];
        this._apiHandler = new RestAPIHandler();
    }

    public connect(): void {

        let on_success = () => {

            console.log('Connected to RabbitMQ.');

            this._subscriptions.push(this.client.subscribe('notifiers_reactJS', (message: any) => {

                let notification: any = JSON.parse(message.body);

                switch (notification['notification']) {
                    case RabbitMQNotificationType[RabbitMQNotificationType.ROOM_ADDED]:
                        store.dispatch(fetchRooms);
                        store.dispatch({ type: 'toasts/setToast', payload: { text: "Room Added" } });
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.ROOM_DELETED]:
                        store.dispatch(fetchRooms);
                        store.dispatch({ type: 'toasts/setToast', payload: { text: "Room Removed" } });
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.ROOM_STATS_CHANGED]:
                        let roomId: string = notification['roomId'];
                        store.dispatch(fetchRoomStats(roomId));
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.DEVICE_ADDED]:
                        store.dispatch(fetchDevices);
                        store.dispatch({ type: 'toasts/setToast', payload: { text: "Device Added" } });
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.DEVICE_STATS_CHANGED]:
                        store.dispatch(fetchDevices);
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.DEVICE_REMOVED]:
                        store.dispatch(fetchDevices);
                        store.dispatch({ type: 'toasts/setToast', payload: { text: "Device Removed" } });
                        break;
                    case RabbitMQNotificationType[RabbitMQNotificationType.EVENT_TRIGGERED]:

                        const event = notification['event'];
                        const deviceName = notification['targetName'];
                        const targetId = notification['targetId'];

                        this._apiHandler.getAllSensors()
                            .then(sensors => {
                                sensors.forEach(sensor => {
                                    if (sensor.deviceId === targetId) {
                                        store.dispatch({
                                            type: 'toasts/setToast',
                                            payload: {text: "Event " + event + " triggered in " + deviceName + "."}
                                        });
                                    }
                                });
                            });

                        break;
                }

                console.log('Received message from RabbitMQ: ' + message);

            }));

        };

        let on_error = (error: any) => {
            console.log("Error with RabbitMQ connection: ");
            console.log(error);
        }

        this.client.connect(this.username, this.password, on_success, on_error, '/');

    }

    public sendMessage(message: string): void {
        this.client.send('notifiers_reactJS', {}, message);
    }

    get username(): string {
        return this._username;
    }

    get password(): string {
        return this._password;
    }

    get websocket(): WebSocket {
        return this._websocket;
    }

    get client(): Client {
        return this._client;
    }

    get subscriptions(): Array<Subscription> {
        return this._subscriptions;
    }

}

