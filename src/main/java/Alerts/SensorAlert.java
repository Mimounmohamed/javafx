package Alerts;
import Sensors.SensorReading;
import ZONES.ZONE;

public class SensorAlert extends Alert {
    private SensorReading triggeringEvent;

    public SensorAlert(SensorReading sensorReading , AlertType alertType , AlertSeverity severity , String message) {
        super(alertType, severity , message);
        if (alertType != AlertType.BioSensorAlert
                && alertType != AlertType.WaterSensorAlert
                && alertType != AlertType.SoilSensorAlert
                && alertType != AlertType.EnvSensorAlert)
            throw new IllegalArgumentException("SensorAlert requires a sensor AlertType");
        this.triggeringEvent = sensorReading;
    }

    @Override
    public ZONE getZone() { return triggeringEvent.getSensor().getZone(); }

    public SensorReading getTriggerReading() {
        return triggeringEvent;
    }

    @Override
    public String toString() {
        return "SensorAlert{" +
                "type=" + getType() +
                ", severity=" + getSeverity() +
                ", message=" + getMessage() +
                ", resolution=" + getResolution() +
                ", timestamp=" + getTimestamp() +
                ", triggerReading=" + triggeringEvent +
                '}';
    }
}
