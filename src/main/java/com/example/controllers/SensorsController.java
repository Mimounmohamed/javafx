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
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SensorsController {

    @FXML private TilePane          sensorGrid;
    @FXML private VBox              noSelectionState;
    @FXML private VBox              detailContent;
    @FXML private VBox              gridEmptyState;
    @FXML private ComboBox<String>  filterType;
    @FXML private ComboBox<String>  filterSeverity;
    @FXML private ComboBox<String>  filterStatus;
    @FXML private ComboBox<String>  filterZone;
    @FXML private VBox              chartContainer;
    @FXML private VBox              sensorActions;
    @FXML private Label             detailTitle;
    @FXML private Label             detailZone;
    @FXML private Label             detailStatus;
    @FXML private Label             detailReading;
    @FXML private Label             statTotalSensors;
    @FXML private Label             statCriticalReadings;
    @FXML private Label             statWarningReadings;
    @FXML private Label             statNormalReadings;

    private static final String SEL_STYLE =
        "-fx-background-color: #16A34A, #F0FDF4;" +
        "-fx-background-insets: 0, 0 0 0 4;" +
        "-fx-background-radius: 12, 0 12 12 0;" +
        "-fx-border-width: 0;" +
        "-fx-padding: 0;" +
        "-fx-cursor: hand;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(134,239,172,0.7), 12, 0, 0, 0);";

    private final SensorService  sensorService   = SensorService.getInstance();
    private final List<Timeline> activeTimelines = new ArrayList<>();
    private List<Sensor>         allSensors;
    private VBox                 selectedCard;

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

    // ── Card grid ─────────────────────────────────────────────────────

    private void loadCards(List<Sensor> sensors) {
        stopAllTimelines();
        sensorGrid.getChildren().clear();
        for (Sensor s : sensors) {
            VBox card = createCard(s);
            card.setUserData(s);
            sensorGrid.getChildren().add(card);
        }
        boolean empty = sensors.isEmpty();
        gridEmptyState.setVisible(empty);
        gridEmptyState.setManaged(empty);
    }

    private void stopAllTimelines() {
        activeTimelines.forEach(Timeline::stop);
        activeTimelines.clear();
    }

    private VBox createCard(Sensor sensor) {
        SensorStatus status = sensor.getStatus();
        ReadingLevel level  = sensorService.getLastReadingLevel(sensor);

        // Two-layer stripe background (same pattern as zones/dashboard)
        String stripeColor, cardBg, shadowColor;
        if (status == SensorStatus.Suspended || status == SensorStatus.Faulty) {
            stripeColor = "#6B7280"; cardBg = "#F9FAFB";
            shadowColor = "rgba(107,114,128,0.10)";
        } else {
            stripeColor = switch (level) {
                case CRITICAL -> "#DC2626";
                case WARNING  -> "#D97706";
                default       -> "#1D9E75";
            };
            cardBg = switch (level) {
                case CRITICAL -> "#FFF5F5";
                case WARNING  -> "#FFFBEB";
                default       -> "#ffffff";
            };
            shadowColor = switch (level) {
                case CRITICAL -> "rgba(220,38,38,0.12)";
                case WARNING  -> "rgba(245,158,11,0.12)";
                default       -> "rgba(0,0,0,0.07)";
            };
        }

        String cardStyle =
            "-fx-background-color: " + stripeColor + ", " + cardBg + ";" +
            "-fx-background-insets: 0, 0 0 0 4;" +
            "-fx-background-radius: 12, 0 12 12 0;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 0;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 10, 0, 0, 2);";

        VBox card = new VBox(0);
        card.getStyleClass().add("sensor-card");
        card.setStyle(cardStyle);
        card.getProperties().put("origStyle", cardStyle);
        card.setPrefWidth(215);
        card.setMinHeight(160);

        // ── HEADER ────────────────────────────────────────────────────
        Label typeBadge = new Label(sensorService.getSensorTypeLabel(sensor));
        typeBadge.getStyleClass().addAll("sensor-type-badge", "sensor-type-" + sensorTypeKey(sensor));

        // 8px status dot with pulse animation for Active/Faulty
        Label statusDot = new Label();
        String dotColor = switch (status) {
            case Active    -> "#16A34A";
            case Faulty    -> "#DC2626";
            default        -> "#9CA3AF";
        };
        statusDot.setStyle(
            "-fx-background-color: " + dotColor + ";" +
            "-fx-background-radius: 99;" +
            "-fx-min-width: 8; -fx-min-height: 8;" +
            "-fx-max-width: 8; -fx-max-height: 8;"
        );
        if (status == SensorStatus.Active || status == SensorStatus.Faulty) {
            Timeline dotPulse = new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(statusDot.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1.0), new KeyValue(statusDot.opacityProperty(), 0.2)),
                new KeyFrame(Duration.seconds(2.0), new KeyValue(statusDot.opacityProperty(), 1.0))
            );
            dotPulse.setCycleCount(Timeline.INDEFINITE);
            dotPulse.play();
            activeTimelines.add(dotPulse);
        }

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(6, typeBadge, headerSpacer, statusDot);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(14, 16, 8, 16));

        // ── BODY ──────────────────────────────────────────────────────
        Label sensorName = new Label("Sensor #" + sensor.getCode());
        sensorName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        sensorName.setWrapText(true);

        Label zoneLbl = new Label("📍 " + sensor.getZone().getName());
        zoneLbl.getStyleClass().add("sensor-card-zone");

        String readingColor;
        if (status == SensorStatus.Suspended || status == SensorStatus.Faulty) {
            readingColor = "#6B7280";
        } else {
            readingColor = switch (level) {
                case CRITICAL -> "#DC2626";
                case WARNING  -> "#D97706";
                default       -> "#16A34A";
            };
        }
        Label reading = new Label(sensorService.getLastReadingDisplay(sensor));
        reading.setWrapText(true);
        reading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: " + readingColor + ";");

        // Opacity pulse for CRITICAL reading value only (not WARNING)
        if (level == ReadingLevel.CRITICAL && status == SensorStatus.Active) {
            Timeline readingPulse = new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(reading.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1.0), new KeyValue(reading.opacityProperty(), 0.3)),
                new KeyFrame(Duration.seconds(2.0), new KeyValue(reading.opacityProperty(), 1.0))
            );
            readingPulse.setCycleCount(Timeline.INDEFINITE);
            readingPulse.play();
            activeTimelines.add(readingPulse);
        }

        VBox body = new VBox(4, sensorName, zoneLbl, reading);
        body.setPadding(new Insets(0, 16, 8, 16));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── FOOTER ────────────────────────────────────────────────────
        Label statusBadge = new Label(sensor.getStatus().toString());
        statusBadge.getStyleClass().addAll("badge", "badge-" + sensor.getStatus().toString().toLowerCase());

        Label levelBadge = new Label(level.toString());
        levelBadge.getStyleClass().addAll("badge", levelCss(level));

        HBox badgeRow = new HBox(5, statusBadge, levelBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label codeLabel = new Label("#" + sensor.getCode());
        codeLabel.getStyleClass().add("sensor-card-code");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox footer = new HBox(0, badgeRow, footerSpacer, codeLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("sensor-card-footer");
        footer.setPadding(new Insets(10, 16, 10, 16));

        card.getChildren().addAll(headerRow, body, spacer, footer);
        card.setOnMouseClicked(e -> showDetail(sensor, card));
        return card;
    }

    private String sensorTypeKey(Sensor s) {
        if (s instanceof BioSensor)       return "bio";
        if (s instanceof GPSCollarSensor) return "gps";
        if (s instanceof EnvSensor)       return "env";
        if (s instanceof SoilSensor)      return "soil";
        if (s instanceof WaterSensor)     return "water";
        return "bio";
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
        // Restore previous card's original style
        if (selectedCard != null) {
            Object orig = selectedCard.getProperties().get("origStyle");
            if (orig instanceof String s) selectedCard.setStyle(s);
        }
        selectedCard = card;
        card.setStyle(SEL_STYLE);

        // Toggle empty / detail views
        noSelectionState.setVisible(false);
        noSelectionState.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        updateDetailHeader(sensor);

        String reading = sensorService.getLastReadingDisplay(sensor);
        if (sensor instanceof BioSensor bio && bio.isAnimalInDistress()) reading += "  ⚠ DISTRESS";
        if (sensor instanceof GPSCollarSensor gps && gps.hasEscaped())   reading += "  ⚠ OUTSIDE ZONE";
        detailReading.setText(reading);

        buildActionBar(sensor);
        buildChart(sensor);
    }

    private void updateDetailHeader(Sensor sensor) {
        detailTitle.setText(sensorService.getSensorTypeLabel(sensor));
        detailZone.setText("📍  " + sensor.getZone().getName());
        detailStatus.setText(sensor.getStatus().toString());
        detailStatus.getStyleClass().removeIf(c -> c.startsWith("badge-") || c.equals("badge"));
        detailStatus.getStyleClass().addAll("badge", "badge-" + sensor.getStatus().toString().toLowerCase());
    }

    private void buildActionBar(Sensor sensor) {
        sensorActions.getChildren().clear();
        SensorStatus status = sensor.getStatus();

        if (status == SensorStatus.Active) {
            Button suspendBtn = new Button("⏸  Suspend");
            suspendBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(suspendBtn, "white", "#D1D5DB", "#374151");
            suspendBtn.setOnAction(e -> { sensor.suspend(); refreshAfterAction(sensor); });
            sensorActions.getChildren().add(suspendBtn);

            Button faultyBtn = new Button("⚠  Mark Faulty");
            faultyBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(faultyBtn, "#FEF2F2", "#FCA5A5", "#DC2626");
            faultyBtn.setOnAction(e -> confirmMarkFaulty(sensor));
            sensorActions.getChildren().add(faultyBtn);

        } else if (status == SensorStatus.Suspended) {
            Button reactivateBtn = new Button("▶  Reactivate");
            reactivateBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(reactivateBtn, "#F0FDF4", "#6EE7B7", "#16A34A");
            reactivateBtn.setOnAction(e -> { sensor.reactivate(); refreshAfterAction(sensor); });
            sensorActions.getChildren().add(reactivateBtn);

            Button faultyBtn = new Button("⚠  Mark Faulty");
            faultyBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(faultyBtn, "#FEF2F2", "#FCA5A5", "#DC2626");
            faultyBtn.setOnAction(e -> confirmMarkFaulty(sensor));
            sensorActions.getChildren().add(faultyBtn);

        } else {
            Button unfaultBtn = new Button("✅  Reactivate Sensor");
            unfaultBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(unfaultBtn, "#F0FDF4", "#6EE7B7", "#16A34A");
            unfaultBtn.setOnAction(e -> { sensor.reactivate(); refreshAfterAction(sensor); });
            sensorActions.getChildren().add(unfaultBtn);
        }

        Button clearBtn = new Button("🗑  Clear History");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        styledActionBtn(clearBtn, "#FEF2F2", "#FCA5A5", "#DC2626");
        clearBtn.setOnAction(e -> confirmClearHistory(sensor));
        sensorActions.getChildren().add(clearBtn);

        if (sensor instanceof NumericSensor ns) {
            Button threshBtn = new Button("⚙  Edit Thresholds");
            threshBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(threshBtn, "#EFF6FF", "#93C5FD", "#2563EB");
            threshBtn.setOnAction(e -> showEditThresholdsDialog(ns, sensor));
            sensorActions.getChildren().add(threshBtn);

            Button injectBtn = new Button("📥  Inject Reading");
            injectBtn.setMaxWidth(Double.MAX_VALUE);
            styledActionBtn(injectBtn, "#F0FDF4", "#6EE7B7", "#16A34A");
            injectBtn.setOnAction(e -> showInjectReadingDialog(ns, sensor));
            sensorActions.getChildren().add(injectBtn);
        }
    }

    private void styledActionBtn(Button btn, String bg, String border, String color) {
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 13px;" +
            "-fx-min-height: 38px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 8 16;"
        );
    }

    private void confirmMarkFaulty(Sensor sensor) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark Faulty");
        confirm.setHeaderText(null);
        confirm.setContentText("Mark sensor " + sensor.getCode() + " as faulty?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) { sensor.markAsFaulty(); refreshAfterAction(sensor); }
        });
    }

    private void confirmClearHistory(Sensor sensor) {
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
    }

    private void refreshAfterAction(Sensor sensor) {
        updateDetailHeader(sensor);
        detailReading.setText(sensorService.getLastReadingDisplay(sensor));
        buildActionBar(sensor);
        applyFilters();
        sensorGrid.getChildren().stream()
            .filter(n -> n instanceof VBox && n.getUserData() == sensor)
            .map(n -> (VBox) n)
            .findFirst()
            .ifPresent(newCard -> {
                newCard.setStyle(SEL_STYLE);
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
