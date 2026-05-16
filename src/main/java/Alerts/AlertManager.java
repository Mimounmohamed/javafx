package Alerts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;
import java.time.LocalDateTime;
import ZONES.ZONE;

public class AlertManager {
    private List<Alert> activeAlerts;
    private List<Alert> alertHistory;

    public AlertManager() {
        this.activeAlerts = new ArrayList<>();
        this.alertHistory = new ArrayList<>();
    }

    public void receiveAlert(Alert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("alert cannot be null");
        }
        activeAlerts.add(alert);
        alertHistory.add(alert);
        printAlert(alert);
    }

    public void acknowledgeAlert(String alertId) {
        Alert a = findActive(alertId);
        if (a != null) a.acknowledge();
    }

    public void resolveAlert(String alertId) {
        Alert a = findActive(alertId);
        if (a != null) {
            a.resolve();
            activeAlerts.remove(a); // closed — move out of active
        }
    }

    public void dismissAlert(String alertId) {
        Alert a = findActive(alertId);
        if (a != null) {
            a.dismiss();
            activeAlerts.remove(a); // closed — move out of active
        }
    }

    private Alert findActive(String alertId) {
        for (Alert a : activeAlerts)
            if (a.getId().equals(alertId)) return a;
        return null;
    }

    public List<Alert> getCriticalAlerts() {
        List<Alert> result = new ArrayList<>();
        for (Alert a : activeAlerts)
            if (a.getSeverity() == AlertSeverity.Critical) result.add(a);
        return result;
    }

    public List<Alert> getWarningAlerts() {
        List<Alert> result = new ArrayList<>();
        for (Alert a : activeAlerts)
            if (a.getSeverity() == AlertSeverity.Warning) result.add(a);
        return result;
    }

    public List<Alert> getAlertsByType(AlertType type) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : activeAlerts)
            if (a.getType() == type) result.add(a);
        return result;
    }

    public List<Alert> getByResolution(AlertResolution resolution) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alertHistory)
            if (a.getResolution() == resolution) result.add(a);
        return result;
    }

    public List<Alert> getActiveAlerts() { return Collections.unmodifiableList(activeAlerts); }
    public List<Alert> getAlertHistory() { return Collections.unmodifiableList(alertHistory); }

    public List<Alert> getHistoryByZone(ZONE zone) {
        return filterByZone(alertHistory, zone);
    }

    public List<Alert> getHistoryBySeverity(AlertSeverity severity) {
        return filterBySeverity(alertHistory, severity);
    }

    public int getTotalHistoryCount() { return alertHistory.size(); }
    public int getActiveCount()       { return activeAlerts.size(); }

    public String getStatsSummary() {
        long critical  = alertHistory.stream().filter(a -> a.getSeverity()  == AlertSeverity.Critical).count();
        long warning   = alertHistory.stream().filter(a -> a.getSeverity()  == AlertSeverity.Warning).count();
        long resolved  = alertHistory.stream().filter(a -> a.getResolution() == AlertResolution.RESOLVED).count();
        long dismissed = alertHistory.stream().filter(a -> a.getResolution() == AlertResolution.DISMISSED).count();
        return String.format(
            "Total: %d | Active: %d | Critical: %d | Warning: %d | Resolved: %d | Dismissed: %d",
            alertHistory.size(), activeAlerts.size(), critical, warning, resolved, dismissed);
    }

    public static List<Alert> filterBySeverity(List<Alert> alerts, AlertSeverity severity) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alerts)
            if (a.getSeverity() == severity) result.add(a);
        return result;
    }

    public static List<Alert> filterByType(List<Alert> alerts, AlertType type) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alerts)
            if (a.getType() == type) result.add(a);
        return result;
    }

    public static List<Alert> filterByZone(List<Alert> alerts, ZONE zone) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alerts)
            if (zone.equals(a.getZone())) result.add(a);
        return result;
    }

    public static List<Alert> filterByPeriod(List<Alert> alerts, LocalDateTime from, LocalDateTime to) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alerts)
            if (!a.getTimestamp().isBefore(from) && !a.getTimestamp().isAfter(to)) result.add(a);
        return result;
    }

    public static List<Alert> sortBySeverity(List<Alert> alerts) {
        List<Alert> result = new ArrayList<>(alerts);
        result.sort(Comparator
                .comparing((Alert a) -> a.getSeverity() == AlertSeverity.Critical ? 0 : 1)
                .thenComparing(Comparator.comparing(Alert::getTimestamp).reversed()));
        return result;
    }

    private void printAlert(Alert alert) {
        System.out.println("[" + alert.getSeverity() + "] " +
                alert.getType() + " — " + alert.getMessage() +
                " at " + alert.getTimestamp());
    }

    @Override
    public String toString() {
        return "AlertManager{" +
                "activeAlerts=" + activeAlerts.size() +
                ", totalHistory=" + alertHistory.size() +
                ", critical=" + getCriticalAlerts().size() +
                ", warnings=" + getWarningAlerts().size() +
                '}';
    }
}
