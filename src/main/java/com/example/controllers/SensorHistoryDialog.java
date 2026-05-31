package com.example.controllers;

import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
import Sensors.Sensor;
import Sensors.SensorReading;
import com.example.services.SensorService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SensorHistoryDialog extends Dialog<Void> {

    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final DateTimeFormatter FULL_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SensorHistoryDialog(ZONE zone, List<String> styleSheets) {
        setTitle("Sensor History — " + zone.getName());
        setHeaderText(null);
        setResizable(true);
        getDialogPane().setPrefSize(920, 720);
        getDialogPane().getStylesheets().addAll(styleSheets);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        List<Sensor> sensors = sensorsFor(zone);

        if (sensors.isEmpty()) {
            Label empty = new Label("No sensors attached to this zone.");
            empty.getStyleClass().add("text-muted");
            content.getChildren().add(empty);
        } else {
            for (Sensor s : sensors) {
                content.getChildren().add(buildSection(s));
                content.getChildren().add(new Separator());
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox wrapper = new VBox(0,
            buildCustomHeader("📡", "Sensor History — " + zone.getName(),
                sensors.size() + " sensor(s) · all readings"),
            scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getDialogPane().setContent(wrapper);
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

    // ── Sensor list by zone type ─────────────────────────────────────────

    private List<Sensor> sensorsFor(ZONE zone) {
        List<Sensor> list = new ArrayList<>();
        if (zone instanceof LivestockZONE lz) {
            list.addAll(lz.getBioSensors());
            list.addAll(lz.getGpsCollarSensors());
        } else if (zone instanceof CropZONE cz) {
            list.addAll(cz.getEnvSensors());
            list.addAll(cz.getSoilSensors());
        } else if (zone instanceof AquacultureZONE az) {
            list.addAll(az.getWaterSensors());
        }
        return list;
    }

    // ── Per-sensor section ───────────────────────────────────────────────

    private VBox buildSection(Sensor sensor) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-border-color: #e2e8f0; -fx-border-radius: 8; " +
            "-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 16;");

        Label title = new Label(SensorService.getInstance().getSensorTypeLabel(sensor)
            + "  ·  code " + sensor.getCode());
        title.getStyleClass().add("card-title");
        section.getChildren().add(title);

        if (sensor instanceof GPSCollarSensor gps) {
            section.getChildren().add(buildGpsArea(gps));
            return section;
        }

        if (!(sensor instanceof NumericSensor ns)) return section;

        List<NumericSensorReading> readings = numericReadings(sensor);

        if (readings.isEmpty()) {
            Label empty = new Label("No readings recorded yet.");
            empty.getStyleClass().add("text-muted");
            section.getChildren().add(empty);
            return section;
        }

        // Threshold info banner
        Label threshInfo = new Label(String.format(
            "Thresholds:  min %.3f %s  |  max %.3f %s   (orange = min line, red = max line)",
            ns.getMinThreshold(), ns.getUnit(), ns.getMaxThreshold(), ns.getUnit()));
        threshInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        section.getChildren().add(threshInfo);

        section.getChildren().add(buildChart(ns, readings));

        Label tableTitle = new Label("All Readings (" + readings.size() + ")");
        tableTitle.getStyleClass().add("section-title");
        tableTitle.setPadding(new Insets(8, 0, 2, 0));
        section.getChildren().addAll(tableTitle, buildTable(readings));

        return section;
    }

    // ── GPS text area ────────────────────────────────────────────────────

    private TextArea buildGpsArea(GPSCollarSensor gps) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPrefHeight(180);
        area.getStyleClass().add("report-text");
        StringBuilder sb = new StringBuilder();
        for (SensorReading r : gps.getReadingHistory()) {
            if (r instanceof GPSSensorReading gr)
                sb.append(String.format("[%s]  %.5f°, %.5f°  —  %s%n",
                    gr.getTimestamp().format(FULL_FMT),
                    gr.getLat(), gr.getLon(),
                    gr.isInsideZone() ? "INSIDE" : "OUTSIDE ⚠"));
        }
        area.setText(sb.isEmpty() ? "No GPS readings." : sb.toString());
        return area;
    }

    // ── Line chart with threshold lines ─────────────────────────────────

    private LineChart<String, Number> buildChart(NumericSensor ns, List<NumericSensorReading> readings) {
        // Compute y bounds from actual data + thresholds so the plot fills its height
        double dataLow  = readings.stream().mapToDouble(NumericSensorReading::getValue).min().orElse(ns.getMinThreshold());
        double dataHigh = readings.stream().mapToDouble(NumericSensorReading::getValue).max().orElse(ns.getMaxThreshold());
        double yLow  = Math.min(dataLow,  ns.getMinThreshold());
        double yHigh = Math.max(dataHigh, ns.getMaxThreshold());
        double pad   = Math.max((yHigh - yLow) * 0.25, 2.0);
        double lower = yLow  - pad;
        double upper = yHigh + pad;
        double tick  = Math.max(1.0, (upper - lower) / 6.0);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis(lower, upper, tick);
        yAxis.setAutoRanging(false);
        xAxis.setLabel("Date / Time");
        yAxis.setLabel(ns.getUnit());
        xAxis.setTickLabelRotation(45);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(ns.getUnit()
            + "  ·  range  [" + ns.getMinThreshold() + " – " + ns.getMaxThreshold() + "]");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.getStyleClass().add("farm-chart");
        chart.setPrefHeight(310);
        chart.setLegendVisible(true);

        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
        dataSeries.setName("Reading");
        XYChart.Series<String, Number> minSeries = new XYChart.Series<>();
        minSeries.setName("Min threshold  (" + ns.getMinThreshold() + " " + ns.getUnit() + ")");
        XYChart.Series<String, Number> maxSeries = new XYChart.Series<>();
        maxSeries.setName("Max threshold  (" + ns.getMaxThreshold() + " " + ns.getUnit() + ")");

        for (NumericSensorReading nr : readings) {
            String lbl = nr.getTimestamp().format(SHORT_FMT);
            dataSeries.getData().add(new XYChart.Data<>(lbl, nr.getValue()));
            minSeries.getData().add(new XYChart.Data<>(lbl, ns.getMinThreshold()));
            maxSeries.getData().add(new XYChart.Data<>(lbl, ns.getMaxThreshold()));
        }

        chart.getData().add(dataSeries);
        chart.getData().add(minSeries);
        chart.getData().add(maxSeries);

        double minW = Math.max(600, readings.size() * 65.0);
        chart.setPrefWidth(minW);
        chart.setMinWidth(minW);
        chart.setMaxWidth(Double.MAX_VALUE);

        Platform.runLater(() -> applyChartStyle(dataSeries, minSeries, maxSeries, readings));

        return chart;
    }

    private void applyChartStyle(
            XYChart.Series<String, Number> dataSeries,
            XYChart.Series<String, Number> minSeries,
            XYChart.Series<String, Number> maxSeries,
            List<NumericSensorReading> readings) {

        // Threshold lines: orange dashed / red dashed
        styleLine(minSeries, "#f59e0b");
        styleLine(maxSeries, "#ef4444");

        // Hide threshold data-point symbols
        hideSymbols(minSeries);
        hideSymbols(maxSeries);

        // Color each data point by severity
        for (int i = 0; i < readings.size() && i < dataSeries.getData().size(); i++) {
            Node node = dataSeries.getData().get(i).getNode();
            if (node == null) continue;
            String color = switch (readings.get(i).getSeverity()) {
                case CRITICAL -> "#ef4444";
                case WARNING  -> "#f59e0b";
                default       -> "#22c55e";
            };
            node.setStyle("-fx-background-color: " + color + ", white; -fx-background-radius: 5px; -fx-padding: 4px;");
        }
    }

    private void styleLine(XYChart.Series<String, Number> series, String hexColor) {
        if (series.getNode() == null) return;
        Node line = series.getNode().lookup(".chart-series-line");
        if (line != null)
            line.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2.5; -fx-stroke-dash-array: 10 5;");
    }

    private void hideSymbols(XYChart.Series<String, Number> series) {
        for (XYChart.Data<?, ?> d : series.getData())
            if (d.getNode() != null) d.getNode().setVisible(false);
    }

    // ── Readings table ───────────────────────────────────────────────────

    private TableView<NumericSensorReading> buildTable(List<NumericSensorReading> readings) {
        TableView<NumericSensorReading> table = new TableView<>();
        table.setPrefHeight(200);
        table.setMaxHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);

        TableColumn<NumericSensorReading, String> colDate = new TableColumn<>("Date / Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTimestamp().format(FULL_FMT)));
        colDate.setMinWidth(180);

        TableColumn<NumericSensorReading, String> colVal = new TableColumn<>("Value");
        colVal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.4f %s", d.getValue().getValue(), d.getValue().getUnit())));
        colVal.setMinWidth(130);

        TableColumn<NumericSensorReading, String> colLevel = new TableColumn<>("Level");
        colLevel.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                NumericSensorReading r = getTableRow().getItem();
                badge.setText(r.getSeverity().toString());
                badge.getStyleClass().removeIf(s -> s.startsWith("badge-"));
                badge.getStyleClass().add(levelCss(r.getSeverity()));
                setGraphic(badge);
            }
        });
        colLevel.setMinWidth(90);

        table.getColumns().add(colDate);
        table.getColumns().add(colVal);
        table.getColumns().add(colLevel);
        table.setItems(FXCollections.observableArrayList(readings));

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(NumericSensorReading r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(switch (r.getSeverity()) {
                    case CRITICAL -> "-fx-background-color: #fee2e2;";
                    case WARNING  -> "-fx-background-color: #fef9c3;";
                    default       -> "";
                });
            }
        });

        return table;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private List<NumericSensorReading> numericReadings(Sensor sensor) {
        List<NumericSensorReading> list = new ArrayList<>();
        for (SensorReading r : sensor.getReadingHistory())
            if (r instanceof NumericSensorReading nr) list.add(nr);
        return list;
    }

    private String levelCss(ReadingLevel level) {
        return switch (level) {
            case CRITICAL -> "badge-critical";
            case WARNING  -> "badge-warning";
            default       -> "badge-normal";
        };
    }
}
