package com.example.controllers;

import Sensors.BioSensor;
import Sensors.EnvSensor;
import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
import Sensors.Sensor;
import Sensors.SensorReading;
import Sensors.SensorStatus;
import Sensors.SoilSensor;
import Sensors.WaterSensor;
import com.example.services.SensorService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SensorsController {

    @FXML private FlowPane         sensorGrid;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterSeverity;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterZone;
    @FXML private VBox             chartContainer;
    @FXML private VBox             sensorActions;
    @FXML private Label            detailTitle;
    @FXML private Label            detailZone;
    @FXML private Label            detailStatus;
    @FXML private Label            detailReading;

    @FXML private Label statTotalSensors;
    @FXML private Label statCriticalReadings;
    @FXML private Label statWarningReadings;
    @FXML private Label statNormalReadings;

    private final SensorService sensorService = SensorService.getInstance();
    private List<Sensor>        allSensors;
    private VBox                selectedCard;

    @FXML
    public void initialize() {
        allSensors = sensorService.getAllSensors();
        setupFilters();
        loadCards(allSensors);
        refreshStats();
    }

    private void refreshStats() {
        statTotalSensors.setText(String.valueOf(allSensors.size()));
        long critical = allSensors.stream()
            .filter(s -> sensorService.getLastReadingLevel(s) == ReadingLevel.CRITICAL).count();
        long warning  = allSensors.stream()
            .filter(s -> sensorService.getLastReadingLevel(s) == ReadingLevel.WARNING).count();
        long normal   = allSensors.stream()
            .filter(s -> sensorService.getLastReadingLevel(s) == ReadingLevel.NORMAL).count();
        statCriticalReadings.setText(String.valueOf(critical));
        statWarningReadings.setText(String.valueOf(warning));
        statNormalReadings.setText(String.valueOf(normal));
    }

    // ── Filters ───────────────────────────────────────────────────────

    private void setupFilters() {
        filterType.getItems().addAll("All Types", "Bio", "GPS", "Env", "Soil", "Water");
        filterType.setValue("All Types");

        filterSeverity.getItems().addAll("All Severities", "CRITICAL", "WARNING", "NORMAL");
        filterSeverity.setValue("All Severities");

        filterStatus.getItems().addAll("All Statuses", "Active", "Suspended", "Faulty");
        filterStatus.setValue("All Statuses");

        filterZone.getItems().add("All Zones");
        allSensors.stream()
            .map(s -> s.getZone().getName())
            .distinct().sorted()
            .forEach(filterZone.getItems()::add);
        filterZone.setValue("All Zones");

        filterType.setOnAction(e     -> applyFilters());
        filterSeverity.setOnAction(e -> applyFilters());
        filterStatus.setOnAction(e   -> applyFilters());
        filterZone.setOnAction(e     -> applyFilters());
    }

    private void applyFilters() {
        String type = filterType.getValue();
        String sev  = filterSeverity.getValue();
        String stat = filterStatus.getValue();
        String zone = filterZone.getValue();
        List<Sensor> result = allSensors.stream()
            .filter(s -> matchesType(s, type))
            .filter(s -> matchesSeverity(s, sev))
            .filter(s -> matchesStatus(s, stat))
            .filter(s -> matchesZone(s, zone))
            .toList();
        loadCards(result);
    }

    private boolean matchesType(Sensor s, String f) {
        return switch (f) {
            case "Bio"   -> s instanceof BioSensor;
            case "GPS"   -> s instanceof GPSCollarSensor;
            case "Env"   -> s instanceof EnvSensor;
            case "Soil"  -> s instanceof SoilSensor;
            case "Water" -> s instanceof WaterSensor;
            default      -> true;
        };
    }

    private boolean matchesSeverity(Sensor s, String sev) {
        if ("All Severities".equals(sev)) return true;
        return sensorService.getLastReadingLevel(s).toString().equals(sev);
    }

    private boolean matchesStatus(Sensor s, String stat) {
        if ("All Statuses".equals(stat)) return true;
        return s.getStatus().toString().equals(stat);
    }

    private boolean matchesZone(Sensor s, String zone) {
        if ("All Zones".equals(zone)) return true;
        return s.getZone().getName().equals(zone);
    }

    // ── Sensor card grid ─────────────────────────────────────────────

    private void loadCards(List<Sensor> sensors) {
        sensorGrid.getChildren().clear();
        for (Sensor s : sensors) {
            VBox card = createCard(s);
            card.setUserData(s);
            sensorGrid.getChildren().add(card);
        }
    }

    private VBox createCard(Sensor sensor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("sensor-card");
        card.setPrefWidth(215);

        Label type = new Label(sensorService.getSensorTypeLabel(sensor));
        type.getStyleClass().add("sensor-card-type");
        type.setWrapText(true);

        Label zone = new Label("📍 " + sensor.getZone().getName());
        zone.getStyleClass().add("sensor-card-zone");

        Label reading = new Label(sensorService.getLastReadingDisplay(sensor));
        reading.getStyleClass().add("sensor-card-reading");

        ReadingLevel level = sensorService.getLastReadingLevel(sensor);
        Label statusBadge = new Label(sensor.getStatus().toString());
        statusBadge.getStyleClass().addAll("badge", "badge-" + sensor.getStatus().toString().toLowerCase());

        Label levelBadge = new Label(level.toString());
        levelBadge.getStyleClass().addAll("badge", levelCss(level));

        HBox badges = new HBox(6, statusBadge, levelBadge);
        card.getChildren().addAll(type, zone, reading, badges);
        card.setOnMouseClicked(e -> showDetail(sensor, card));
        return card;
    }

    private String levelCss(ReadingLevel level) {
        return switch (level) {
            case CRITICAL -> "badge-critical";
            case WARNING  -> "badge-warning";
            default       -> "badge-normal";
        };
    }

    // ── Detail panel ─────────────────────────────────────────────────

    private void showDetail(Sensor sensor, VBox card) {
        if (selectedCard != null) selectedCard.getStyleClass().remove("sensor-card-selected");
        selectedCard = card;
        card.getStyleClass().add("sensor-card-selected");

        detailTitle.setText(sensorService.getSensorTypeLabel(sensor));
        detailZone.setText(sensor.getZone().getName());
        detailStatus.setText(sensor.getStatus().toString());
        String reading = sensorService.getLastReadingDisplay(sensor);
        if (sensor instanceof BioSensor bio && bio.isAnimalInDistress()) reading += "  ⚠ DISTRESS";
        if (sensor instanceof GPSCollarSensor gps && gps.hasEscaped())   reading += "  ⚠ OUTSIDE ZONE";
        detailReading.setText(reading);

        buildActionBar(sensor);
        buildChart(sensor);
    }

    private void buildActionBar(Sensor sensor) {
        sensorActions.getChildren().clear();
        SensorStatus status = sensor.getStatus();

        // Context-aware status buttons
        HBox statusBtns = new HBox(8);
        if (status == SensorStatus.Active) {
            Button suspendBtn = new Button("⏸ Suspend");
            suspendBtn.getStyleClass().add("btn-secondary");
            suspendBtn.setOnAction(e -> { sensor.suspend(); refreshAfterAction(sensor); });

            Button faultyBtn = new Button("⚠ Mark Faulty");
            faultyBtn.getStyleClass().add("btn-danger");
            faultyBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Mark Faulty");
                confirm.setHeaderText(null);
                confirm.setContentText("Mark sensor " + sensor.getCode() + " as faulty?");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) { sensor.markAsFaulty(); refreshAfterAction(sensor); }
                });
            });
            statusBtns.getChildren().addAll(suspendBtn, faultyBtn);

        } else if (status == SensorStatus.Suspended) {
            Button reactivateBtn = new Button("▶ Reactivate");
            reactivateBtn.getStyleClass().add("btn-secondary");
            reactivateBtn.setOnAction(e -> { sensor.reactivate(); refreshAfterAction(sensor); });

            Button faultyBtn = new Button("⚠ Mark Faulty");
            faultyBtn.getStyleClass().add("btn-danger");
            faultyBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Mark Faulty");
                confirm.setHeaderText(null);
                confirm.setContentText("Mark sensor " + sensor.getCode() + " as faulty?");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) { sensor.markAsFaulty(); refreshAfterAction(sensor); }
                });
            });
            statusBtns.getChildren().addAll(reactivateBtn, faultyBtn);

        } else { // Faulty
            Button unfaultBtn = new Button("✅ Unfault (Reactivate)");
            unfaultBtn.getStyleClass().add("btn-primary");
            unfaultBtn.setMaxWidth(Double.MAX_VALUE);
            unfaultBtn.setOnAction(e -> { sensor.reactivate(); refreshAfterAction(sensor); });
            statusBtns.getChildren().add(unfaultBtn);
        }

        sensorActions.getChildren().add(statusBtns);

        // Clear history
        Button clearBtn = new Button("🗑 Clear History");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Reading History");
            confirm.setHeaderText(null);
            confirm.setContentText("Delete all reading history for sensor " + sensor.getCode() + "?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    sensor.clearReadingHistory();
                    refreshAfterAction(sensor);
                    buildChart(sensor);
                }
            });
        });
        sensorActions.getChildren().add(clearBtn);

        // Threshold editing + reading injection for numeric sensors
        if (sensor instanceof NumericSensor ns) {
            Button threshBtn = new Button("⚙ Edit Thresholds");
            threshBtn.getStyleClass().add("btn-secondary");
            threshBtn.setMaxWidth(Double.MAX_VALUE);
            threshBtn.setOnAction(e -> showEditThresholdsDialog(ns, sensor));
            sensorActions.getChildren().add(threshBtn);

            Button injectBtn = new Button("📥 Inject Reading");
            injectBtn.getStyleClass().add("btn-secondary");
            injectBtn.setMaxWidth(Double.MAX_VALUE);
            injectBtn.setOnAction(e -> showInjectReadingDialog(ns, sensor));
            sensorActions.getChildren().add(injectBtn);
        }
    }

    private void refreshAfterAction(Sensor sensor) {
        detailStatus.setText(sensor.getStatus().toString());
        detailReading.setText(sensorService.getLastReadingDisplay(sensor));

        // Rebuild action bar with updated state
        buildActionBar(sensor);

        // Rebuild card grid, then re-select card for this sensor
        applyFilters();

        // Find and re-highlight the card for this sensor
        sensorGrid.getChildren().stream()
            .filter(n -> n instanceof VBox && n.getUserData() == sensor)
            .map(n -> (VBox) n)
            .findFirst()
            .ifPresent(newCard -> {
                newCard.getStyleClass().add("sensor-card-selected");
                selectedCard = newCard;
            });

        refreshStats();
    }

    private void showInjectReadingDialog(NumericSensor ns, Sensor sensor) {
        String hint = String.format("%.2f – %.2f %s", ns.getMinThreshold(), ns.getMaxThreshold(), ns.getUnit());
        TextField valueField = new TextField(Double.isNaN(ns.getLastValue()) ? "0.0" : String.format("%.2f", ns.getLastValue()));
        VBox form = new VBox(14, formGroup("Value  [" + hint + "]", valueField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Inject Reading");
        dialog.setHeaderText("Inject reading for sensor " + ns.getCode());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(380);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Inject");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(valueField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(value -> {
            ns.setLastValue(value);
            ns.sendReading();
            refreshAfterAction(sensor);
            buildChart(sensor);
        });
    }

    private void showEditThresholdsDialog(NumericSensor ns, Sensor sensor) {
        TextField minField = new TextField(String.format("%.2f", ns.getMinThreshold()));
        TextField maxField = new TextField(String.format("%.2f", ns.getMaxThreshold()));

        VBox form = new VBox(14,
            formGroup("Min Threshold (" + ns.getUnit() + ")", minField),
            formGroup("Max Threshold (" + ns.getUnit() + ")", maxField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Edit Thresholds");
        dialog.setHeaderText("Thresholds for sensor " + ns.getCode());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(380);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Save");

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                return new double[]{min, max};
            } catch (NumberFormatException e) { return null; }
        });

        dialog.showAndWait().ifPresent(vals -> {
            try {
                ns.setMinThreshold(vals[0]);
                ns.setMaxThreshold(vals[1]);
                refreshAfterAction(sensor);
            } catch (IllegalArgumentException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Thresholds");
                err.setHeaderText(null);
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }

    // ── History section ───────────────────────────────────────────────

    private static final DateTimeFormatter TABLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void buildChart(Sensor sensor) {
        chartContainer.getChildren().clear();

        if (sensor instanceof GPSCollarSensor gps) {
            // GPS: text area fits fine in the narrow panel
            TextArea area = new TextArea();
            area.setEditable(false);
            area.setPrefHeight(220);
            area.getStyleClass().add("report-text");
            StringBuilder sb = new StringBuilder();
            for (SensorReading r : gps.getReadingHistory()) {
                if (r instanceof GPSSensorReading gr)
                    sb.append(String.format("[%s]  %.5f°, %.5f°  —  %s%n",
                        gr.getTimestamp().format(TABLE_FMT),
                        gr.getLat(), gr.getLon(),
                        gr.isInsideZone() ? "INSIDE" : "OUTSIDE ⚠"));
            }
            area.setText(sb.isEmpty() ? "No GPS readings." : sb.toString());
            chartContainer.getChildren().add(area);
            return;
        }

        if (!(sensor instanceof NumericSensor ns)) return;

        long count = sensor.getReadingHistory().stream()
            .filter(r -> r instanceof NumericSensorReading).count();

        // Show last reading summary + open-chart button
        String summary = count == 0
            ? "No readings yet."
            : String.format("%d readings  ·  last: %.2f %s", count, ns.getLastValue(), ns.getUnit());
        Label summaryLbl = new Label(summary);
        summaryLbl.getStyleClass().add("text-muted");
        summaryLbl.setStyle("-fx-font-size: 12px;");

        Button openChartBtn = new Button("📊 View Full Chart & History");
        openChartBtn.getStyleClass().add("btn-primary");
        openChartBtn.setMaxWidth(Double.MAX_VALUE);
        openChartBtn.setDisable(count == 0);
        openChartBtn.setOnAction(e ->
            new SensorChartDialog(ns, chartContainer.getScene().getStylesheets()).showAndWait());

        chartContainer.getChildren().addAll(summaryLbl, openChartBtn);
    }

    // ── Dialog helpers ────────────────────────────────────────────────

    private VBox formGroup(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("dialog-form-label");
        if (input instanceof TextField tf) {
            tf.getStyleClass().setAll("dialog-form-field");
            tf.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof ComboBox<?> cb) {
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.getStyleClass().setAll("combo-box", "dialog-form-combo");
        }
        return new VBox(6, lbl, input);
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        var sheets = sensorGrid.getScene() == null ? null : sensorGrid.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0)
            : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);
        Button ok     = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (ok != null)     ok.getStyleClass().add("btn-primary");
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");

        String title = dialog.getTitle() == null ? "" : dialog.getTitle();
        String icon  = title.contains("Inject") ? "📊" : "📡";
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        dialog.setGraphic(iconLbl);
    }
}
