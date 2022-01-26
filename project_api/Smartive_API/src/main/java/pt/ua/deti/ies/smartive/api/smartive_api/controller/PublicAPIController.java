package pt.ua.deti.ies.smartive.api.smartive_api.controller;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pt.ua.deti.ies.smartive.api.smartive_api.auth.AuthManager;
import pt.ua.deti.ies.smartive.api.smartive_api.exceptions.*;
import pt.ua.deti.ies.smartive.api.smartive_api.middleware.rabbitmq.notifications.react.ReactNotificationFactory;
import pt.ua.deti.ies.smartive.api.smartive_api.middleware.rabbitmq.notifications.react.ReactNotificationType;
import pt.ua.deti.ies.smartive.api.smartive_api.model.MessageResponse;
import pt.ua.deti.ies.smartive.api.smartive_api.model.Room;
import pt.ua.deti.ies.smartive.api.smartive_api.model.User;
import pt.ua.deti.ies.smartive.api.smartive_api.model.devices.AvailableDevice;
import pt.ua.deti.ies.smartive.api.smartive_api.model.devices.Device;
import pt.ua.deti.ies.smartive.api.smartive_api.model.devices.Sensor;
import pt.ua.deti.ies.smartive.api.smartive_api.model.devices.events.SensorEvent;
import pt.ua.deti.ies.smartive.api.smartive_api.services.*;
import pt.ua.deti.ies.smartive.api.smartive_api.tokens.JwtRequest;
import pt.ua.deti.ies.smartive.api.smartive_api.tokens.JwtResponse;
import pt.ua.deti.ies.smartive.api.smartive_api.tokens.JwtTokenUtil;
import pt.ua.deti.ies.smartive.api.smartive_api.tokens.JwtUserDetailsService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PublicAPIController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SensorService sensorService;
    private final RoomService roomService;
    private final DeviceService deviceService;
    private final AvailableDeviceService availableDeviceService;
    private final ReactNotificationFactory reactNotificationFactory;
    private final SensorEventService sensorEventService;

    // Auth
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;
    private final AuthManager authManager;

    @Autowired
    public PublicAPIController(SensorService sensorService, RoomService roomService, DeviceService deviceService,
                               AvailableDeviceService availableDeviceService, ReactNotificationFactory reactNotificationFactory, SensorEventService sensorEventService,
                               AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil, JwtUserDetailsService userDetailsService, AuthManager authManager) {
        this.sensorService = sensorService;
        this.roomService = roomService;
        this.deviceService = deviceService;
        this.availableDeviceService = availableDeviceService;
        this.reactNotificationFactory = reactNotificationFactory;
        this.sensorEventService = sensorEventService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.authManager = authManager;
    }

    @GetMapping("/rooms")
    public List<Room> getAllRooms() {

        List<Room> allRooms = roomService.getAllRooms();

        return allRooms.stream()
                .filter(room -> room.getUsers().contains(authManager.getUserName()))
                .collect(Collectors.toList());

    }

    @PostMapping("/rooms")
    public MessageResponse registerRoom(@RequestBody Room room) {

        if (!room.isValid())
            throw new InvalidRoomException("Invalid Room. Please provide all the mandatory fields.");

        if (roomService.exists(room.getName()))
            throw new InvalidRoomException("A room with that name already exists. Please use a different name.");

        if (room.getUsers() == null)
            room.setUsers(Collections.singletonList(authManager.getUserName()));

        Room registeredRoom = roomService.registerRoom(room);
        reactNotificationFactory.generateNotification(ReactNotificationType.ROOM_ADDED, registeredRoom).sendNotification();

        return new MessageResponse("The room was successfully registered.");

    }

    @DeleteMapping("/rooms/{roomId}")
    public MessageResponse deleteRoom(@PathVariable ObjectId roomId) {

        if (!roomService.exists(roomId))
            throw new InvalidRoomException(String.format("Unable to find a room with the id %s.", roomId.toString()));

        Room room = roomService.getRoom(roomId);

        if (!room.getUsers().contains(authManager.getUserName()))
            throw new InvalidPermissionsException();

        for (Device currentDevice : room.getDevices()) {
            currentDevice.setRoomId(null);
            deviceService.update(currentDevice);
        }

        roomService.deleteRoom(room);
        reactNotificationFactory.generateNotification(ReactNotificationType.ROOM_DELETED, room).sendNotification();

        return new MessageResponse("The room was successfully removed.");

    }

    @PostMapping("/devices/sensors/register")
    public MessageResponse registerSensor(@RequestBody Sensor sensor) {

        sensorService.registerSensor(sensor);
        reactNotificationFactory.generateNotification(ReactNotificationType.DEVICE_ADDED).sendNotification();

        return new MessageResponse("The sensor was successfully registered.");

    }

    @GetMapping(value = "/devices/sensors")
    public List<Sensor> getAllRegisteredSensors() {

        return sensorService.getAllSensors().stream()
                .filter(sensor -> sensor.getRoomId() != null)
                .filter(sensor -> {
                    Room sensorRoom = roomService.getRoom(sensor.getRoomId());
                    if (sensorRoom == null) return false;
                    return sensorRoom.getUsers().contains(authManager.getUserName());
                })
                .collect(Collectors.toList());

    }

    @GetMapping(value = "/devices")
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices().stream()
                .filter(device -> device.getRoomId() != null)
                .filter(device -> {
                    Room deviceRoom = roomService.getRoom(device.getRoomId());
                    if (deviceRoom == null) return false;
                    return deviceRoom.getUsers().contains(authManager.getUserName());
                })
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/devices/available")
    public List<AvailableDevice> getAllAvailableDevices() {
        return availableDeviceService.getAllAvailableDevices();
    }

    @PostMapping(value = "/devices/available")
    public MessageResponse getAllAvailableDevices(@RequestBody AvailableDevice availableDevice) {
        availableDeviceService.save(availableDevice);
        return new MessageResponse("Successfully registered available device.");
    }

    @DeleteMapping(value = "/devices/available/{deviceId}")
    public MessageResponse removeAvailableDevice(@PathVariable ObjectId deviceId) {
        availableDeviceService.delete(deviceId);
        return new MessageResponse("Successfully removed available device.");
    }

    @GetMapping(value = "/devices/sensors/{roomId}")
    public List<Sensor> getRegisteredSensorsByRoom(@PathVariable ObjectId roomId) {

        if (roomId == null)
            throw new RoomNotFoundException("Unable to find the specified room.");

        Room room = roomService.getRoom(roomId);

        if (room == null || !room.getUsers().contains(authManager.getUserName()))
            throw new InvalidPermissionsException();

        return sensorService.getAllSensors()
                .stream()
                .filter(sensor -> sensor.getRoomId() != null && sensor.getRoomId().equals(roomId))
                .collect(Collectors.toList());

    }

    @GetMapping("/devices/sensors/events")
    public List<SensorEvent> getSensorsEvents() {
        return sensorEventService.getAllEvents().stream()
                .filter(sensorEvent -> sensorService.getSensorById(sensorEvent.getSensorId()) != null)
                .filter(sensorEvent -> {
                    Sensor sensor = sensorService.getSensorById(sensorEvent.getSensorId());
                    if (sensor.getRoomId() == null) return false;
                    Room sensorRoom = roomService.getRoom(sensor.getRoomId());
                    return sensorRoom.getUsers().contains(authManager.getUserName());
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/devices/sensors/events")
    public SensorEvent addSensorEvent(@RequestBody SensorEvent event) {

        if (!sensorService.sensorExists(event.getSensorId()))
            throw new InvalidDeviceException(String.format("Unable to find a sensor with the id %s.", event.getSensorId()));

        Sensor sensor = sensorService.getSensorById(event.getSensorId());

        if (sensor.getRoomId() == null || !roomService.exists(sensor.getRoomId()))
            throw new InvalidRoomException(String.format("Unable to find a room with the id %s.", sensor.getRoomId()));

        Room room = roomService.getRoom(sensor.getRoomId());

        if (!room.getUsers().contains(authManager.getUserName()))
            throw new InvalidPermissionsException();

        return sensorEventService.registerEvent(event);

    }

    @PostMapping("/login")
    public JwtResponse createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) {

        Authentication authResult;
        String username = authenticationRequest.getUsername();
        String password = authenticationRequest.getPassword();

        if (username == null || password == null) {
            return new JwtResponse("Please provide a valid request.", null);
        }

        UsernamePasswordAuthenticationToken userToken = new UsernamePasswordAuthenticationToken(username, password);

        try {
            authResult = authenticationManager.authenticate(userToken);
        } catch (DisabledException e) {
            throw new DisabledException("User is disabled.");
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        if (authResult.isAuthenticated())
            System.out.println("User is Authenticated.");

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        return new JwtResponse("Authentication succeed.", token);
    }

    @PostMapping("/register")
    public MessageResponse registerUser(@RequestBody User user) {

        if (!user.isValid())
            throw new InvalidUserException("Invalid user. Please provide all the mandatory fields.");

        try {
            userDetailsService.registerUser(user);
        } catch (DuplicateKeyException keyException) {
            throw new UserAlreadyExistsException(String.format("A user with the email %s is already registered.", user.getEmail()));
        }

        return new MessageResponse("The user was successfully registered.");
    }
}
