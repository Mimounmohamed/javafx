package Sensors;

import java.time.LocalDateTime;

public class GPSSensorReading extends SensorReading{
    private double latitude;
    private double longitude;
    private boolean isInsideZone;

    public GPSSensorReading(GPSCollarSensor sensor, double latitude, double longitude, boolean isInsideZone) {
        super(sensor);
        this.latitude = latitude;
        this.longitude = longitude;
        this.isInsideZone = isInsideZone;
    }

    public GPSSensorReading(GPSCollarSensor sensor, double latitude, double longitude, boolean isInsideZone, LocalDateTime timestamp) {
        super(sensor, timestamp);
        this.latitude = latitude;
        this.longitude = longitude;
        this.isInsideZone = isInsideZone;
    }

    public boolean isOutsideZone() {
        return !isInsideZone;
    }

    @Override
    public String toString() {
        return "GPSSensorReading{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", insideZone=" + isInsideZone +
                ", timestamp=" + getTimestamp() +
                '}';
    }

    public double getLat()        { return latitude; }
    public double getLon()        { return longitude; }
    public boolean isInsideZone() { return isInsideZone; }
}
