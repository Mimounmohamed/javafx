package Sensors;
import java.time.LocalDateTime;

public abstract class SensorReading {
    private Sensor sensor;
    private LocalDateTime timestamp;

public SensorReading(Sensor sensor) {
    this.sensor = sensor;
    this.timestamp = LocalDateTime.now();
}

protected SensorReading(Sensor sensor, LocalDateTime timestamp) {
    this.sensor = sensor;
    this.timestamp = timestamp;
}

    public Sensor getSensor() { return sensor; }
    public LocalDateTime getTimestamp() { return timestamp; }
}