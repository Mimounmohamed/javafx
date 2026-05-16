package com.example.services;

import Alerts.Alert;
import Alerts.AlertManager;
import Alerts.AlertResolution;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import ZONES.ZONE;

import java.time.LocalDateTime;
import java.util.List;

public class AlertService {

    private static AlertService instance;

    private AlertService() {}

    public static AlertService getInstance() {
        if (instance == null) instance = new AlertService();
        return instance;
    }

    private static FarmService fs() { return FarmService.getInstance(); }

    public List<Alert> getAllAlerts() {
        return AlertManager.sortBySeverity(fs().getFarm().getAllAlerts());
    }

    public List<Alert> getActiveAlerts() { return fs().getFarm().getActiveAlerts(); }

    public List<Alert> getAlertsByType(AlertType type) {
        return AlertManager.filterByType(fs().getFarm().getAllAlerts(), type);
    }

    public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
        return AlertManager.filterBySeverity(fs().getFarm().getAllAlerts(), severity);
    }

    public List<Alert> getAlertsByZone(ZONE zone) {
        return AlertManager.filterByZone(fs().getFarm().getAllAlerts(), zone);
    }

    public List<Alert> getAlertsByPeriod(LocalDateTime from, LocalDateTime to) {
        return AlertManager.filterByPeriod(fs().getFarm().getAllAlerts(), from, to);
    }

    public List<Alert> getAlertsByResolution(AlertResolution resolution) {
        return fs().getFarm().getAllAlerts().stream()
            .filter(a -> a.getResolution() == resolution).toList();
    }

    public int getActiveAlertCount()   { return getActiveAlerts().size(); }
    public int getCriticalAlertCount() { return getAlertsBySeverity(AlertSeverity.Critical).size(); }

    public void acknowledge(Alert a) { a.acknowledge(); }
    public void resolve(Alert a)     { a.resolve(); }
    public void dismiss(Alert a)     { a.dismiss(); }
}
