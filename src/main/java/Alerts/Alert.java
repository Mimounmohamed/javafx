package Alerts;

import java.util.UUID;
import java.time.LocalDateTime;
import ZONES.ZONE;

public abstract class Alert {
    private String id;
    private AlertType type;
    private AlertSeverity severity;
    private String message;
    private LocalDateTime timestamp;
    private AlertResolution resolution;

    public Alert(AlertType type, AlertSeverity severity, String message) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (severity == null) throw new IllegalArgumentException("severity cannot be null");
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("message cannot be null or blank");
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.resolution = AlertResolution.ACTIVE;
    }

    public void acknowledge() { this.resolution = AlertResolution.ACKNOWLEDGED; }
    public void resolve()     { this.resolution = AlertResolution.RESOLVED; }
    public void dismiss()     { this.resolution = AlertResolution.DISMISSED; }

    public boolean isActive()       { return resolution == AlertResolution.ACTIVE; }
    public boolean isAcknowledged() { return resolution == AlertResolution.ACKNOWLEDGED; }
    public boolean isResolved()     { return resolution == AlertResolution.RESOLVED; }
    public boolean isDismissed()    { return resolution == AlertResolution.DISMISSED; }
    public boolean isClosed() {
        return resolution == AlertResolution.RESOLVED
                || resolution == AlertResolution.DISMISSED;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getZoneName() {
        ZONE z = getZone();
        return z != null ? z.getName() : "N/A";
    }

    public abstract ZONE getZone();

    public String getId()                    { return id; }
    public AlertType getType()               { return type; }
    public AlertSeverity getSeverity()       { return severity; }
    public String getMessage()               { return message; }
    public LocalDateTime getTimestamp()      { return timestamp; }
    public AlertResolution getResolution()   { return resolution; }

}
