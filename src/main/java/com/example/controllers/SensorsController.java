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
import java.util.Comparator;
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
    // Injected programmatically into the filter bar (not in FXML)
    private ComboBox<String> sortBy;
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

    private static final String SEL_CLASS = "sensor-card-selected";

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

        // Sort combo — injected into the same filter bar at runtime
        // Core: a Comparator is applied to the filtered list before rendering cards.
        // (For TableView you'd use SortedList + TableColumn.setComparator instead.)
        sortBy = new ComboBox<>();
        sortBy.getItems().addAll(
            "Default", "Severity ↑ (critical first)", "Severity ↓ (normal first)",
            "Zone A→Z", "Type A→Z", "Most Readings");
        sortBy.setValue("Default");
        sortBy.getStyleClass().setAll("animals-filter-combo");
        sortBy.setPrefWidth(190);
        sortBy.setOnAction(e -> applyFilters());
        if (filterZone.getParent() instanceof HBox filterBar) {
            Label sortLbl = new Label("Sort:");
            sortLbl.getStyleClass().add("sensor-sort-label");
            filterBar.getChildren().addAll(sortLbl, sortBy);
        }
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
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        loadCards(applySortOrder(result));
    }

    // Core of sorting: apply a Comparator to the filtered list.
    // For TableView you'd use SortedList + column.setComparator() instead;
    // for card grids like this one, we sort the list before rendering.
    private List<Sensor> applySortOrder(List<Sensor> list) {
        if (sortBy == null) return list;
        return switch (sortBy.getValue()) {
            case "Severity ↑ (critical first)" -> list.stream()
                .sorted(Comparator.comparingInt(s -> severityRank(sensorService.getLastReadingLevel(s))))
                .toList();
            case "Severity ↓ (normal first)" -> list.stream()
                .sorted(Comparator.comparingInt((Sensor s) -> severityRank(sensorService.getLastReadingLevel(s))).reversed())
                .toList();
            case "Zone A→Z" -> list.stream()
                .sorted(Comparator.comparing(s -> s.getZone().getName()))
                .toList();
            case "Type A→Z" -> list.stream()
                .sorted(Comparator.comparing(s -> sensorService.getSensorTypeLabel(s)))
                .toList();
            case "Most Readings" -> list.stream()
                .sorted(Comparator.comparingInt((Sensor s) -> s.getReadingHistory().size()).reversed())
                .toList();
            default -> list;
        };
    }

    private int severityRank(ReadingLevel l) {
        return switch (l) { case CRITICAL -> 0; case WARNING -> 1; default -> 2; };
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

        // CSS class encodes the state — no inline styles needed
        String stateClass = (status == SensorStatus.Suspended || status == SensorStatus.Faulty)
            ? "sensor-card-suspended"
            : switch (level) {
                case CRITICAL -> "sensor-card-critical";
                case WARNING  -> "sensor-card-warning";
                default       -> "sensor-card-normal";
            };

        VBox card = new VBox(0);
        card.getStyleClass().addAll("sensor-card", stateClass);
        card.getProperties().put("stateClass", stateClass);
        card.setPrefWidth(215);
        card.setMinHeight(160);

        // ── HEADER ────────────────────────────────────────────────────
        Label typeBadge = new Label(sensorService.getSensorTypeLabel(sensor));
        typeBadge.getStyleClass().addAll("sensor-type-badge", "sensor-type-" + sensorTypeKey(sensor));

        // 8px status dot — CSS class drives the colour
        Label statusDot = new Label();
        String dotCssColor = switch (status) {
            case Active  -> "#22c55e";
            case Faulty  -> "#ef4444";
            default      -> "#64748b";
        };
        statusDot.setStyle(
            "-fx-background-color:" + dotCssColor + ";" +
            "-fx-background-radius:99;" +
            "-fx-min-width:8;-fx-min-height:8;" +
            "-fx-max-width:8;-fx-max-height:8;"
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
        sensorName.getStyleClass().add("sensor-card-name");
        sensorName.setWrapText(true);

        Label zoneLbl = new Label("📍 " + sensor.getZone().getName() + animalSuffix(sensor));
        zoneLbl.getStyleClass().add("sensor-card-zone");

        String readingCssClass = (status == SensorStatus.Suspended || status == SensorStatus.Faulty)
            ? "sensor-card-reading-muted"
            : switch (level) {
                case CRITICAL -> "sensor-card-reading-critical";
                case WARNING  -> "sensor-card-reading-warning";
                default       -> "sensor-card-reading-normal";
            };
        Label reading = new Label(sensorService.getLastReadingDisplay(sensor));
        reading.getStyleClass().add(readingCssClass);
        reading.setWrapText(true);

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
        // Restore previous card to its state class
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove(SEL_CLASS);
            String sc = (String) selectedCard.getProperties().get("stateClass");
            if (sc != null && !selectedCard.getStyleClass().contains(sc))
                selectedCard.getStyleClass().add(sc);
        }
        selectedCard = card;
        card.getStyleClass().remove((String) card.getProperties().get("stateClass"));
        if (!card.getStyleClass().contains(SEL_CLASS)) card.getStyleClass().add(SEL_CLASS);

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

    private String animalSuffix(Sensor sensor) {
        if (sensor instanceof BioSensor bs)        return "  ·  " + bs.getAnimal().getName();
        if (sensor instanceof GPSCollarSensor gs)  return "  ·  " + gs.getAnimal().getName();
        return "";
    }

    private void updateDetailHeader(Sensor sensor) {
        detailTitle.setText(sensorService.getSensorTypeLabel(sensor));
        detailZone.setText("📍  " + sensor.getZone().getName() + animalSuffix(sensor));
        detailStatus.setText(sensor.getStatus().toString());
        detailStatus.getStyleClass().removeIf(c -> c.startsWith("badge-") || c.equals("badge"));
        detailStatus.getStyleClass().addAll("badge", "badge-" + sensor.getStatus().toString().toLowerCase());
    }

    private void buildActionBar(Sensor sensor) {
        sensorActions.getChildren().clear();
        SensorStatus status = sensor.getStatus();

        if (status == SensorStatus.Active) {
            sensorActions.getChildren().add(actionBtn("⏸  Suspend",     "sensor-action-default",
                e -> { sensor.suspend(); refreshAfterAction(sensor); }));
            sensorActions.getChildren().add(actionBtn("⚠  Mark Faulty", "sensor-action-danger",
                e -> confirmMarkFaulty(sensor)));

        } else if (status == SensorStatus.Suspended) {
            sensorActions.getChildren().add(actionBtn("▶  Reactivate",  "sensor-action-success",
                e -> { sensor.reactivate(); refreshAfterAction(sensor); }));
            sensorActions.getChildren().add(actionBtn("⚠  Mark Faulty", "sensor-action-danger",
                e -> confirmMarkFaulty(sensor)));
        } else {
            sensorActions.getChildren().add(actionBtn("✅  Reactivate Sensor", "sensor-action-success",
                e -> { sensor.reactivate(); refreshAfterAction(sensor); }));
        }

        sensorActions.getChildren().add(actionBtn("🗑  Clear History", "sensor-action-danger",
            e -> confirmClearHistory(sensor)));

        if (sensor instanceof NumericSensor ns) {
            sensorActions.getChildren().add(actionBtn("⚙  Edit Thresholds", "sensor-action-primary",
                e -> showEditThresholdsDialog(ns, sensor)));
            sensorActions.getChildren().add(actionBtn("📥  Inject Reading",  "sensor-action-success",
                e -> showInjectReadingDialog(ns, sensor)));
        }
    }

    private Button actionBtn(String text, String cssClass,
                             javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add(cssClass);
        btn.setOnAction(handler);
        return btn;
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
                newCard.getStyleClass().remove((String) newCard.getProperties().get("stateClass"));
                if (!newCard.getStyleClass().contains(SEL_CLASS)) newCard.getStyleClass().add(SEL_CLASS);
                selectedCard = newCard;
            });
        refreshStats();
    }

    private void showInjectReadingDialog(NumericSensor ns, Sensor sensor) {
        String hint = String.format("%.2f – %.2f %s", ns.getMinThreshold(), ns.getMaxThreshold(), ns.getUnit());
        TextField valueField = new TextField(Double.isNaN(ns.getLastValue()) ? "0.0" : String.format("%.2f", ns.getLastValue()));
        styleFormField(valueField);
        VBox form = new VBox(14, formGroup("Value  [" + hint + "]", valueField));
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Inject Reading");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0,
            buildDialogHeader("📥", "Inject Reading",
                "Sensor #" + ns.getCode() + " — " + SensorService.getInstance().getSensorTypeLabel(ns)),
            form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogCss(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Inject");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
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
        styleFormField(minField);
        styleFormField(maxField);
        VBox form = new VBox(14,
            formGroup("Min Threshold (" + ns.getUnit() + ")", minField),
            formGroup("Max Threshold (" + ns.getUnit() + ")", maxField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Edit Thresholds");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0,
            buildDialogHeader("⚙", "Edit Thresholds",
                "Sensor #" + ns.getCode() + " — " + SensorService.getInstance().getSensorTypeLabel(ns)),
            form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogCss(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Save");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
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
        lbl.getStyleClass().add("az-form-label");
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

    private void styleFormField(TextField f) {
        f.getStyleClass().setAll("dialog-form-field");
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private HBox buildDialogHeader(String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("dialog-custom-header-sub");

        VBox textBox = new VBox(2, titleLbl, subLbl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, iconLbl, textBox, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }

    private void applyDialogCss(Dialog<?> d) {
        var sheets = sensorGrid.getScene() == null ? null : sensorGrid.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0)
            : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        d.getDialogPane().getStylesheets().add(css);
    }
}
