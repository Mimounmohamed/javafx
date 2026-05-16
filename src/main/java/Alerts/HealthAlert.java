package Alerts;
import Animals.HealthEvent;
import ZONES.ZONE;

public class HealthAlert extends Alert {
    private HealthEvent triggeringEvent;

    public HealthAlert(HealthEvent healthEvent , AlertType alertType , AlertSeverity severity , String message) {
        super(alertType, severity , message);
        if (alertType != AlertType.HEALTH_ALERT)
            throw new IllegalArgumentException("HealthAlert requires AlertType.HEALTH_ALERT");
        this.triggeringEvent = healthEvent;
    }

    @Override
    public ZONE getZone() { return triggeringEvent.getAnimal().getZone(); }

    public HealthEvent getTriggerReading() {
        return triggeringEvent;
    }

    @Override
    public String toString() {
        return "HealthAlert{" +
                "type=" + getType() +
                ", severity=" + getSeverity() +
                ", message=" + getMessage() +
                ", resolution=" + getResolution() +
                ", timestamp=" + getTimestamp() +
                ", triggerReading=" + triggeringEvent +
                '}';
    }
}
