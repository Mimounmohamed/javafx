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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

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
    @FXML private VBox   detailSeverityBanner;
    @FXML private VBox   detailInfoCard;
    @FXML private Button btnAcknowledge;
    @FXML private Button btnResolve;
    @FXML private Button btnDismiss;

    private ObservableList<Alert> allAlerts;
    private FilteredList<Alert>   filteredAlerts;
    private final AlertService    alertService = AlertService.getInstance();

    private static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT   = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DETAIL_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

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
        // Row factory — severity left stripe
        alertTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Alert item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(c -> c.startsWith("alert-row-"));
                if (!empty && item != null) {
                    if (item.getSeverity() == AlertSeverity.Critical)
                        getStyleClass().add("alert-row-critical");
                    else if (item.getSeverity() == AlertSeverity.Warning)
                        getStyleClass().add("alert-row-warning");
                }
            }
        });

        // Time column — two-line date + time
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTimestamp().toString()));
        colTime.setCellFactory(col -> new TableCell<>() {
            private final Label dateLbl = new Label();
            private final Label timeLbl = new Label();
            private final VBox  box     = new VBox(1, dateLbl, timeLbl);
            {
                dateLbl.getStyleClass().add("detail-value");
                dateLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
                timeLbl.getStyleClass().add("text-muted");
                timeLbl.setStyle("-fx-font-size: 10px;");
                box.setPadding(new Insets(2, 0, 2, 0));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Alert a = (Alert) getTableRow().getItem();
                dateLbl.setText(a.getTimestamp().format(DATE_FMT));
                timeLbl.setText(a.getTimestamp().format(TIME_FMT));
                setGraphic(box);
            }
        });

        // Type column — colored badge pill
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().toString()));
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(alertTypeLabel(a.getType()));
                badge.setStyle("-fx-background-radius: 9999; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;"
                    + alertTypeBadgeStyle(a.getType()));
                setGraphic(badge);
            }
        });

        // Message column — tooltip on hover
        colMessage.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        colMessage.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTooltip(null); return; }
                setText(item);
                setStyle("-fx-text-overrun: ellipsis;");
                setTooltip(new Tooltip(item));
            }
        });

        // Severity column — badge
        colSeverity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity().toString()));
        colSeverity.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(a.getSeverity().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(a.getSeverity() == AlertSeverity.Critical
                    ? "badge-critical" : "badge-warning");
                setGraphic(badge);
            }
        });

        // Status column — badge
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResolution().toString()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
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

    private String alertTypeLabel(AlertType t) {
        return switch (t) {
            case BioSensorAlert   -> "🧬 Bio Sensor";
            case WaterSensorAlert -> "💧 Water Sensor";
            case SoilSensorAlert  -> "🌱 Soil Sensor";
            case EnvSensorAlert   -> "🌡 Environment";
            case GPS_ESCAPE_ALERT -> "📍 GPS Escape";
            case HEALTH_ALERT     -> "🐾 Health";
        };
    }

    private String alertTypeBadgeStyle(AlertType t) {
        return switch (t) {
            case BioSensorAlert   -> "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
            case WaterSensorAlert -> "-fx-background-color: #DBEAFE; -fx-text-fill: #2563EB;";
            case SoilSensorAlert  -> "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
            case EnvSensorAlert   -> "-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED;";
            case GPS_ESCAPE_ALERT -> "-fx-background-color: #FFEDD5; -fx-text-fill: #EA580C;";
            case HEALTH_ALERT     -> "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
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
            .map(Alert::getZoneName)
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
        // Severity banner
        detailSeverityBanner.getChildren().clear();
        String bannerBg  = alert.getSeverity() == AlertSeverity.Critical ? "#FEF2F2" : "#FFFBEB";
        String bannerBdr = alert.getSeverity() == AlertSeverity.Critical ? "#DC2626" : "#D97706";
        detailSeverityBanner.setStyle(
            "-fx-background-color: " + bannerBg + ";" +
            "-fx-border-color: " + bannerBdr + ";" +
            "-fx-border-width: 0 0 0 4;" +
            "-fx-padding: 14 16;");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(alertTypeLabel(alert.getType()));
        typeBadge.setStyle(
            "-fx-background-radius: 9999; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;" +
            alertTypeBadgeStyle(alert.getType()));

        String sevBg = alert.getSeverity() == AlertSeverity.Critical ? "#DC2626" : "#D97706";
        Label sevBadge = new Label(alert.getSeverity().toString().toUpperCase());
        sevBadge.setStyle(
            "-fx-background-color: " + sevBg + "; -fx-text-fill: white;" +
            "-fx-background-radius: 9999; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;");

        Label statusBadge = new Label(alert.getResolution().toString());
        statusBadge.getStyleClass().addAll("badge", resolutionCss(alert.getResolution()));

        headerRow.getChildren().addAll(typeBadge, sevBadge, statusBadge);

        Label msgLbl = new Label(alert.getMessage());
        msgLbl.setWrapText(true);
        msgLbl.getStyleClass().add("detail-value");
        msgLbl.setStyle("-fx-font-size: 13px; -fx-padding: 8 0 0 0;");

        detailSeverityBanner.getChildren().addAll(headerRow, msgLbl);

        // Info card
        detailInfoCard.getChildren().clear();
        detailInfoCard.getStyleClass().add("kpi-card");
        detailInfoCard.setStyle("-fx-padding: 0;");

        addDetailRow(detailInfoCard, "Zone",    alert.getZone() != null ? alert.getZone().getName() : "N/A", false);
        addDetailRow(detailInfoCard, "Time",    alert.getTimestamp().format(DETAIL_FMT), true);
        addDetailRow(detailInfoCard, "Status",  alert.getResolution().toString(), true);
        addDetailRow(detailInfoCard, "Trigger", buildTriggerText(alert), true);

        btnAcknowledge.setDisable(!alert.isActive());
        btnResolve.setDisable(alert.isResolved() || alert.isDismissed());
        btnDismiss.setDisable(alert.isDismissed());
    }

    private void addDetailRow(VBox container, String key, String value, boolean withDivider) {
        if (withDivider) container.getChildren().add(new Separator());
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));

        Label keyLbl = new Label(key);
        keyLbl.getStyleClass().add("detail-key");
        keyLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-min-width: 56;");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("detail-value");
        valLbl.setStyle("-fx-font-size: 12px;");
        valLbl.setWrapText(true);
        HBox.setHgrow(valLbl, Priority.ALWAYS);

        row.getChildren().addAll(keyLbl, valLbl);
        container.getChildren().add(row);
    }

    private String buildTriggerText(Alert alert) {
        if (alert instanceof HealthAlert ha) {
            String animal = ha.getTriggerReading().getAnimal().getName();
            String event  = ha.getTriggerReading().getEventType().toString();
            String desc   = ha.getTriggerReading().getDescription();
            return "Animal: " + animal + " — " + event +
                (desc != null && !desc.isBlank() ? " (" + desc + ")" : "");
        } else if (alert instanceof SensorAlert sa) {
            String code = sa.getTriggerReading().getSensor().getCode();
            String val  = sa.getTriggerReading() instanceof NumericSensorReading nr
                ? String.format("%.2f %s", nr.getValue(), nr.getUnit()) : "GPS reading";
            return "Sensor: " + code + " — value: " + val;
        }
        return "—";
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
