package pt.ua.deti.ies.smartive.api.smartive_api.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ua.deti.ies.smartive.api.smartive_api.model.devices.Sensor;

import java.util.List;

public interface SensorRepository extends MongoRepository<Sensor, Long> {

    Sensor findByDeviceId(ObjectId deviceId);
    void deleteByDeviceId(ObjectId deviceId);
    boolean existsSensorByDeviceId(ObjectId deviceId);

}
