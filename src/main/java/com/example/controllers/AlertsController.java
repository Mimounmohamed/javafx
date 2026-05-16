package com.example.controllers;

import Alerts.Alert;
import Alerts.AlertResolution;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Alerts.HealthAlert;
import Alerts.SensorAlert;
import Sensors.NumericSensorReading;
import ZONES.ZONE;
import com.example.services.AlertService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class AlertsController {

    @FXML private TableView<Alert>            alertTable;
    @FXML private TableColumn<Alert, String>  colTime;
    @FXML private TableColumn<Alert, String>  colType;
    @FXML private TableColumn<Alert, String>  colSeverity;
    @FXML private TableColumn<Alert, String>  colMessage;
    @FXML private TableColumn<Alert, String>  colStatus;

    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterSeverity;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterZone;

    @FXML private Label statTotalAlerts;
    @FXML private Label statCritical;
    @FXML private Label statWarning;
    @FXML private Label statResolved;

    @FXML private VBox   detailPanel;
    @FXML private Label  detailMessage;
    @FXML private Label  detailZone;
    @FXML private Label  detailTimestamp;
    @FXML private Label  detailResolution;
    @FXML private Label  detailTrigger;
    @FXML private Button btnAcknowledge;
    @FXML private Button btnResolve;
    @FXML private Button btnDismiss;

    private ObservableList<Alert> allAlerts;
    private FilteredList<Alert>   filteredAlerts;
    private final AlertService    alertService = AlertService.getInstance();

    @FXML
    public void initialize() {
        allAlerts      = FXCollections.observableArrayList(alertService.getAllAlerts());
        filteredAlerts = new FilteredList<>(allAlerts, p -> true);
        setupTable();
        setupFilters();
        refreshStats();
        alertTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, n) -> { if (n != null) showDetail(n); });
    }

    // ── Table ─────────────────────────────────────────────────────────

    private void setupTable() {
        colTime.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getTimestamp().toString().substring(0, 16)));
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getType().toString()));
        colMessage.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getMessage()));

        colSeverity.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(a.getSeverity().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(a.getSeverity() == AlertSeverity.Critical ? "badge-critical" : "badge-warning");
                setGraphic(badge);
            }
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(a.getResolution().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(resolutionCss(a.getResolution()));
                setGraphic(badge);
            }
        });

        alertTable.setItems(filteredAlerts);
    }

    private String resolutionCss(AlertResolution r) {
        return switch (r) {
            case ACTIVE        -> "badge-critical";
            case ACKNOWLEDGED  -> "badge-warning";
            case RESOLVED      -> "badge-normal";
            case DISMISSED     -> "badge-suspended";
        };
    }

    // ── Filters ───────────────────────────────────────────────────────

    private void setupFilters() {
        filterType.getItems().add("All Types");
        for (AlertType t : AlertType.values()) filterType.getItems().add(t.toString());
        filterType.setValue("All Types");

        filterSeverity.getItems().addAll("All Severities", "Critical", "Warning");
        filterSeverity.setValue("All Severities");

        filterStatus.getItems().addAll("All", "ACTIVE", "ACKNOWLEDGED", "RESOLVED", "DISMISSED");
        filterStatus.setValue("All");

        filterZone.getItems().add("All Zones");
        allAlerts.stream()
            .map(a -> a.getZoneName())
            .filter(n -> !"N/A".equals(n))
            .distinct().sorted()
            .forEach(filterZone.getItems()::add);
        filterZone.setValue("All Zones");

        filterType.setOnAction(e     -> applyFilter());
        filterSeverity.setOnAction(e -> applyFilter());
        filterStatus.setOnAction(e   -> applyFilter());
        filterZone.setOnAction(e     -> applyFilter());
    }

    private void applyFilter() {
        String typeStr = filterType.getValue();
        String sevStr  = filterSeverity.getValue();
        String statStr = filterStatus.getValue();
        String zoneStr = filterZone.getValue();
        filteredAlerts.setPredicate(a -> {
            boolean typeOk = "All Types".equals(typeStr)     || a.getType().toString().equals(typeStr);
            boolean sevOk  = "All Severities".equals(sevStr) || a.getSeverity().toString().equals(sevStr);
            boolean statOk = "All".equals(statStr)           || a.getResolution().toString().equals(statStr);
            boolean zoneOk = "All Zones".equals(zoneStr)     || a.getZoneName().equals(zoneStr);
            return typeOk && sevOk && statOk && zoneOk;
        });
    }

    // ── Detail panel ─────────────────────────────────────────────────

    private void showDetail(Alert alert) {
        detailMessage.setText(alert.getMessage());
        ZONE zone = alert.getZone();
        detailZone.setText(zone != null ? zone.getName() : "N/A");
        detailTimestamp.setText(alert.getTimestamp().toString().substring(0, 16));
        detailResolution.setText(alert.getResolution().toString());

        if (alert instanceof HealthAlert ha) {
            String animal = ha.getTriggerReading().getAnimal().getName();
            String event  = ha.getTriggerReading().getEventType().toString();
            String desc   = ha.getTriggerReading().getDescription();
            detailTrigger.setText("Animal: " + animal + " — " + event + (desc != null && !desc.isBlank() ? " (" + desc + ")" : ""));
        } else if (alert instanceof SensorAlert sa) {
            String code = sa.getTriggerReading().getSensor().getCode();
            String val  = sa.getTriggerReading() instanceof NumericSensorReading nr
                ? String.format("%.2f %s", nr.getValue(), nr.getUnit()) : "GPS reading";
            detailTrigger.setText("Sensor: " + code + " — value: " + val);
        } else {
            detailTrigger.setText("—");
        }

        btnAcknowledge.setDisable(!alert.isActive());
        btnResolve.setDisable(alert.isResolved() || alert.isDismissed());
        btnDismiss.setDisable(alert.isDismissed());
    }

    // ── Actions ───────────────────────────────────────────────────────

    @FXML private void acknowledgeAlert() {
        Alert a = alertTable.getSelectionModel().getSelectedItem();
        if (a != null) { alertService.acknowledge(a); refresh(); }
    }

    @FXML private void resolveAlert() {
        Alert a = alertTable.getSelectionModel().getSelectedItem();
        if (a != null) { alertService.resolve(a); refresh(); }
    }

    @FXML private void dismissAlert() {
        Alert a = alertTable.getSelectionModel().getSelectedItem();
        if (a != null) { alertService.dismiss(a); refresh(); }
    }

    private void refreshStats() {
        statTotalAlerts.setText(String.valueOf(allAlerts.size()));
        statCritical.setText(String.valueOf(allAlerts.stream()
            .filter(a -> a.getSeverity() == AlertSeverity.Critical).count()));
        statWarning.setText(String.valueOf(allAlerts.stream()
            .filter(a -> a.getSeverity() == AlertSeverity.Warning).count()));
        statResolved.setText(String.valueOf(allAlerts.stream()
            .filter(a -> a.getResolution() == AlertResolution.RESOLVED).count()));
    }

    private void refresh() {
        Alert selected = alertTable.getSelectionModel().getSelectedItem();
        allAlerts.setAll(alertService.getAllAlerts());
        alertTable.refresh();
        refreshStats();
        if (selected != null) showDetail(selected);
    }
}
