import axios from "axios";
import {Room} from "./entities/Room";
import {Device} from "./entities/Device";
import {SensorStat} from "./entities/SensorStat";
import {Sensor} from "./entities/Sensor";

export class RestAPIHandler {

    private readonly _middlewareBaseURI;
    private readonly _publicAPIBaseURI;

    constructor() {
        this._middlewareBaseURI = 'http://localhost:8080/middleware';
        this._publicAPIBaseURI = 'http://localhost:8080/api'
    }

    getAllRooms(): Promise<Room[]> {

        const endpointURI = '/rooms'
        const requestURI = this._publicAPIBaseURI + endpointURI;

        return axios.get(requestURI)
            .then((response) => {

                const rooms: Room[] = response.data;
                return rooms

            })

            .catch(error => {
                console.log("Error on API request (getAllRooms()): " + error.message)
                return []
            });

    }

    getAllSensors(): Promise<Sensor[]> {

        const endpointURI = '/devices/sensors'
        const requestURI = this._publicAPIBaseURI + endpointURI;

        return axios.get(requestURI)
            .then((response) => {
                const sensors: Sensor[] = response.data;
                return sensors
            })
            .catch(error => {
                console.log("Error on API request (getAllSensors()): " + error.message)
                return []
            });

    }

    getSensorStats(sensorId: String): Promise<SensorStat | any[]> {

        const endpointURI = `/devices/sensor/${sensorId}`
        const requestURI = this._middlewareBaseURI + endpointURI;

        return axios.get(requestURI)
            .then((response) => {
                const sensorsStats: SensorStat = response.data;
                return sensorsStats
            })
            .catch(error => {
                console.log("Error on API request (getSensorsStats()): " + error.message)
                return []
            });

    }

    getAvailableDevices(): Promise<Device[]> {

        const endpointURI = '/devices/available'
        const requestURI = this._publicAPIBaseURI + endpointURI;

        return axios.get(requestURI)
            .then((response) => {
                const availableDevices: Device[] = response.data;
                return availableDevices
            })
            .catch(error => {
                console.log("Error on API request (getAvailableDevices()): " + error.message)
                return []
            });

    }

    registerNewDevice(device: Device): Promise<boolean> {

        const endpointURI = '/devices/sensors/register'
        const requestURI = this._publicAPIBaseURI + endpointURI;

        return axios.post(requestURI, device)
            .then((response) => {
                console.log(response);
                return response.status === 200;
            })
            .catch(error => {
                console.log("Error on API request (registerNewDevice()): " + error.message)
                return false
            });

    }

}