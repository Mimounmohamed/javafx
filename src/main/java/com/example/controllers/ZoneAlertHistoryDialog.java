package com.example.controllers;

import Alerts.Alert;
import Alerts.AlertResolution;
import Alerts.AlertSeverity;
import Alerts.HealthAlert;
import Alerts.SensorAlert;
import Sensors.NumericSensorReading;
import ZONES.ZONE;
import com.example.services.AlertService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ZoneAlertHistoryDialog extends Dialog<Void> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AlertService alertService = AlertService.getInstance();
    private ObservableList<Alert> allAlerts;
    private FilteredList<Alert>   filtered;
    private TableView<Alert>      table;
    private ComboBox<String>      filterSev;
    private ComboBox<String>      filterStat;

    // detail labels
    private Label detailMsg;
    private Label detailTime;
    private Label detailSev;
    private Label detailRes;
    private Label detailTrigger;
    private Button btnAck;
    private Button btnResolve;
    private Button btnDismiss;

    public ZoneAlertHistoryDialog(ZONE zone, List<String> styleSheets) {
        setTitle("Alert History — " + zone.getName());
        setHeaderText(null);
        setResizable(true);
        getDialogPane().setPrefSize(980, 680);
        getDialogPane().getStylesheets().addAll(styleSheets);

        // Load all alerts for this zone (history)
        List<Alert> zoneAlerts = alertService.getAlertsByZone(zone);
        allAlerts = FXCollections.observableArrayList(zoneAlerts);
        filtered  = new FilteredList<>(allAlerts, p -> true);

        VBox root = new VBox(0);

        // ── Custom header ──────────────────────────────────────────────────
        root.getChildren().add(buildCustomHeader(
            "🔔", "Alert History — " + zone.getName(),
            zoneAlerts.size() + " alert(s) total"));

        // ── Stats bar ──────────────────────────────────────────────────────
        root.getChildren().add(buildStatsBar(zoneAlerts));

        // ── Filter bar ─────────────────────────────────────────────────────
        root.getChildren().add(new Separator());
        root.getChildren().add(buildFilterBar());

        // ── Table ─────────────────────────────────────────────────────────
        root.getChildren().add(new Separator());
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().add(table);

        root.getChildren().add(new Separator());

        // ── Detail / actions ───────────────────────────────────────────────
        root.getChildren().add(buildDetailPanel());

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) closeBtn.getStyleClass().add("btn-primary");

        setResultConverter(bt -> null);
    }

    private HBox buildCustomHeader(String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("dialog-custom-header-sub");

        VBox textBox = new VBox(2, titleLbl, subLbl);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-header-close-btn");
        closeBtn.setOnAction(e -> {
            Button footerClose = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
            if (footerClose != null) footerClose.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, iconLbl, textBox, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }

    // ── Stats bar ─────────────────────────────────────────────────────────

    private HBox buildStatsBar(List<Alert> alerts) {
        long total    = alerts.size();
        long critical = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.Critical).count();
        long warning  = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.Warning).count();
        long active   = alerts.stream().filter(Alert::isActive).count();
        long resolved = alerts.stream().filter(Alert::isResolved).count();

        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.getStyleClass().add("stats-bar");
        bar.getChildren().addAll(
            statCard(String.valueOf(total),    "Total",    "stat-accent-blue"),
            statCard(String.valueOf(critical), "Critical", "stat-accent-red"),
            statCard(String.valueOf(warning),  "Warning",  "stat-accent-yellow"),
            statCard(String.valueOf(active),   "Active",   "stat-accent-red"),
            statCard(String.valueOf(resolved), "Resolved", "stat-accent-green")
        );
        return bar;
    }

    private VBox statCard(String val, String label, String accent) {
        Label v = new Label(val);   v.getStyleClass().add("stat-value");
        Label l = new Label(label); l.getStyleClass().add("stat-label");
        VBox card = new VBox(2, v, l);
        card.getStyleClass().addAll("stat-card", accent);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // ── Filter bar ────────────────────────────────────────────────────────

    private HBox buildFilterBar() {
        filterSev  = new ComboBox<>();
        filterStat = new ComboBox<>();

        filterSev.getItems().addAll("All Severities", "Critical", "Warning");
        filterSev.setValue("All Severities");
        filterSev.getStyleClass().add("filter-combo");
        filterSev.setPrefWidth(150);

        filterStat.getItems().addAll("All Statuses", "ACTIVE", "ACKNOWLEDGED", "RESOLVED", "DISMISSED");
        filterStat.setValue("All Statuses");
        filterStat.getStyleClass().add("filter-combo");
        filterStat.setPrefWidth(150);

        filterSev.setOnAction(e  -> applyFilter());
        filterStat.setOnAction(e -> applyFilter());

        Label sevLbl  = new Label("Severity:");  sevLbl.getStyleClass().add("detail-key");
        Label statLbl = new Label("Status:");    statLbl.getStyleClass().add("detail-key");

        HBox bar = new HBox(10, sevLbl, filterSev, statLbl, filterStat);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    private void applyFilter() {
        String sev  = filterSev.getValue();
        String stat = filterStat.getValue();
        filtered.setPredicate(a -> {
            boolean sevOk  = "All Severities".equals(sev)  || a.getSeverity().toString().equals(sev);
            boolean statOk = "All Statuses".equals(stat)   || a.getResolution().toString().equals(stat);
            return sevOk && statOk;
        });
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private TableView<Alert> buildTable() {
        TableView<Alert> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        tv.setPrefHeight(330);

        TableColumn<Alert, String> colTime = new TableColumn<>("Date / Time");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTimestamp().format(FMT)));
        colTime.setMinWidth(140);

        TableColumn<Alert, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getType().toString()));
        colType.setMinWidth(140);

        TableColumn<Alert, String> colSev = new TableColumn<>("Severity");
        colSev.setCellFactory(c -> new TableCell<>() {
            private final Label b = new Label();
            { b.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Alert a = getTableRow().getItem();
                b.setText(a.getSeverity().toString());
                b.getStyleClass().removeIf(s -> s.startsWith("badge-"));
                b.getStyleClass().add(a.getSeverity() == AlertSeverity.Critical ? "badge-critical" : "badge-warning");
                setGraphic(b);
            }
        });
        colSev.setMinWidth(90);

        TableColumn<Alert, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        colMsg.setMinWidth(260);

        TableColumn<Alert, String> colStat = new TableColumn<>("Status");
        colStat.setCellFactory(c -> new TableCell<>() {
            private final Label b = new Label();
            { b.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Alert a = getTableRow().getItem();
                b.setText(a.getResolution().toString());
                b.getStyleClass().removeIf(s -> s.startsWith("badge-"));
                b.getStyleClass().add(resolutionCss(a.getResolution()));
                setGraphic(b);
            }
        });
        colStat.setMinWidth(110);

        tv.getColumns().add(colTime);
        tv.getColumns().add(colType);
        tv.getColumns().add(colSev);
        tv.getColumns().add(colMsg);
        tv.getColumns().add(colStat);
        tv.setItems(filtered);

        tv.setRowFactory(tbl -> new javafx.scene.control.TableRow<>() {
            @Override protected void updateItem(Alert a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setStyle(""); return; }
                setStyle(a.getSeverity() == AlertSeverity.Critical
                    ? "-fx-background-color: #fee2e2;"
                    : "-fx-background-color: #fef9c3;");
            }
        });

        tv.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, n) -> { if (n != null) showDetail(n); });

        return tv;
    }

    // ── Detail + actions ──────────────────────────────────────────────────

    private VBox buildDetailPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(12, 16, 12, 16));

        Label title = new Label("Selected Alert");
        title.getStyleClass().add("card-title");

        detailMsg     = new Label("Select a row above"); detailMsg.setWrapText(true);
        detailTime    = new Label();
        detailSev     = new Label();
        detailRes     = new Label();
        detailTrigger = new Label(); detailTrigger.setWrapText(true);

        detailMsg.getStyleClass().add("text-muted");

        HBox timeRow    = detailRow("Time",    detailTime);
        HBox sevRow     = detailRow("Severity",detailSev);
        HBox resRow     = detailRow("Status",  detailRes);
        HBox trigRow    = detailRow("Trigger", detailTrigger);

        btnAck     = new Button("Acknowledge");
        btnResolve = new Button("✔ Resolve");
        btnDismiss = new Button("✕ Dismiss");
        btnAck.getStyleClass().add("btn-secondary");
        btnResolve.getStyleClass().add("btn-primary");
        btnDismiss.getStyleClass().add("btn-danger");
        btnAck.setDisable(true);
        btnResolve.setDisable(true);
        btnDismiss.setDisable(true);

        btnAck.setOnAction(e -> {
            Alert a = table.getSelectionModel().getSelectedItem();
            if (a != null) { alertService.acknowledge(a); table.refresh(); showDetail(a); }
        });
        btnResolve.setOnAction(e -> {
            Alert a = table.getSelectionModel().getSelectedItem();
            if (a != null) { alertService.resolve(a); table.refresh(); showDetail(a); }
        });
        btnDismiss.setOnAction(e -> {
            Alert a = table.getSelectionModel().getSelectedItem();
            if (a != null) { alertService.dismiss(a); table.refresh(); showDetail(a); }
        });

        HBox actions = new HBox(10, btnAck, btnResolve, btnDismiss);

        panel.getChildren().addAll(title, detailMsg, timeRow, sevRow, resRow, trigRow, actions);
        return panel;
    }

    private HBox detailRow(String key, Label value) {
        Label k = new Label(key); k.getStyleClass().add("detail-key");
        value.getStyleClass().add("detail-value");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, k, value);
        row.getStyleClass().add("detail-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private void showDetail(Alert a) {
        detailMsg.setText(a.getMessage());
        detailMsg.getStyleClass().removeIf(s -> s.equals("text-muted"));
        detailTime.setText(a.getTimestamp().format(FMT));
        detailSev.setText(a.getSeverity().toString());
        detailRes.setText(a.getResolution().toString());

        if (a instanceof HealthAlert ha) {
            String animal = ha.getTriggerReading().getAnimal().getName();
            String event  = ha.getTriggerReading().getEventType().toString();
            String desc   = ha.getTriggerReading().getDescription();
            detailTrigger.setText("Animal: " + animal + " — " + event
                + (desc != null && !desc.isBlank() ? " (" + desc + ")" : ""));
        } else if (a instanceof SensorAlert sa) {
            String code = sa.getTriggerReading().getSensor().getCode();
            String val  = sa.getTriggerReading() instanceof NumericSensorReading nr
                ? String.format("%.2f %s", nr.getValue(), nr.getUnit()) : "GPS reading";
            detailTrigger.setText("Sensor " + code + " — value: " + val);
        } else {
            detailTrigger.setText("—");
        }

        btnAck.setDisable(!a.isActive());
        btnResolve.setDisable(a.isResolved() || a.isDismissed());
        btnDismiss.setDisable(a.isDismissed());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String resolutionCss(AlertResolution r) {
        return switch (r) {
            case ACTIVE       -> "badge-critical";
            case ACKNOWLEDGED -> "badge-warning";
            case RESOLVED     -> "badge-normal";
            case DISMISSED    -> "badge-suspended";
        };
    }
}
