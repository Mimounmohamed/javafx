package com.example.controllers;

import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SensorChartDialog extends Dialog<Void> {

    private static final DateTimeFormatter CHART_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final DateTimeFormatter TABLE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SensorChartDialog(NumericSensor ns, List<String> styleSheets) {
        String label = SensorService.getInstance().getSensorTypeLabel(ns);
        setTitle("Reading History — " + label);
        setHeaderText(null);
        setResizable(true);
        getDialogPane().setPrefSize(980, 640);
        getDialogPane().getStylesheets().addAll(styleSheets);

        VBox content = new VBox(0);

        // ── Custom header ───────────────────────────────────────────────────
        content.getChildren().add(buildCustomHeader(
            "📊",
            "Reading History — " + label,
            "Zone: " + ns.getZone().getName() + "  ·  Range: [" +
                ns.getMinThreshold() + " – " + ns.getMaxThreshold() + " " + ns.getUnit() + "]"
        ));

        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        content.getChildren().add(body);

        // ── Info banner ────────────────────────────────────────────────────
        Label info = new Label(String.format(
            "Status: %s  ·  Last reading: %.2f %s",
            ns.getStatus(),
            Double.isNaN(ns.getLastValue()) ? 0.0 : ns.getLastValue(), ns.getUnit()));
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280; " +
            "-fx-background-color: #F9FAFB; -fx-padding: 8 12; " +
            "-fx-background-radius: 6; -fx-border-color: #E5E5E2; " +
            "-fx-border-radius: 6; -fx-border-width: 1;");
        body.getChildren().add(info);

        // ── Collect readings ───────────────────────────────────────────────
        List<NumericSensorReading> readings = new ArrayList<>();
        for (SensorReading r : ns.getReadingHistory())
            if (r instanceof NumericSensorReading nr) readings.add(nr);

        if (readings.isEmpty()) {
            Label empty = new Label("No readings recorded yet.");
            empty.getStyleClass().add("text-muted");
            body.getChildren().add(empty);
        } else {
            body.getChildren().add(buildChart(ns, readings));
            Label tableTitle = new Label("All Readings (" + readings.size() + ")");
            tableTitle.getStyleClass().add("section-title");
            tableTitle.setPadding(new Insets(8, 0, 2, 0));
            body.getChildren().addAll(tableTitle, buildTable(readings));
        }

        getDialogPane().setContent(content);
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

    // ── Chart ──────────────────────────────────────────────────────────────

    private ScrollPane buildChart(NumericSensor ns, List<NumericSensorReading> readings) {
        // Y-axis zoomed in on actual data + threshold range
        double dataLow  = readings.stream().mapToDouble(NumericSensorReading::getValue).min().orElse(ns.getMinThreshold());
        double dataHigh = readings.stream().mapToDouble(NumericSensorReading::getValue).max().orElse(ns.getMaxThreshold());
        double yLow  = Math.min(dataLow,  ns.getMinThreshold());
        double yHigh = Math.max(dataHigh, ns.getMaxThreshold());
        double pad   = Math.max((yHigh - yLow) * 0.25, 2.0);
        double lower = yLow - pad;
        double upper = yHigh + pad;
        double tick  = Math.max(1.0, (upper - lower) / 6.0);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis(lower, upper, tick);
        yAxis.setAutoRanging(false);
        xAxis.setLabel("Date / Time");
        yAxis.setLabel(ns.getUnit());
        xAxis.setTickLabelRotation(40);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(ns.getUnit()
            + "  ·  range [" + ns.getMinThreshold() + " – " + ns.getMaxThreshold() + "]");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.getStyleClass().add("farm-chart");
        chart.setPrefHeight(340);

        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
        dataSeries.setName("Reading");
        XYChart.Series<String, Number> minSeries = new XYChart.Series<>();
        minSeries.setName("Min (" + ns.getMinThreshold() + " " + ns.getUnit() + ")");
        XYChart.Series<String, Number> maxSeries = new XYChart.Series<>();
        maxSeries.setName("Max (" + ns.getMaxThreshold() + " " + ns.getUnit() + ")");

        for (NumericSensorReading nr : readings) {
            String lbl = nr.getTimestamp().format(CHART_FMT);
            dataSeries.getData().add(new XYChart.Data<>(lbl, nr.getValue()));
            minSeries.getData().add(new XYChart.Data<>(lbl, ns.getMinThreshold()));
            maxSeries.getData().add(new XYChart.Data<>(lbl, ns.getMaxThreshold()));
        }

        chart.getData().add(dataSeries);
        chart.getData().add(minSeries);
        chart.getData().add(maxSeries);

        // 65 px per point keeps every label readable
        double chartW = Math.max(800, readings.size() * 65);
        chart.setPrefWidth(chartW);
        chart.setMinWidth(chartW);

        Platform.runLater(() -> {
            applyLineStyle(minSeries, "#f59e0b");
            applyLineStyle(maxSeries, "#ef4444");
            hideSymbols(minSeries);
            hideSymbols(maxSeries);
            for (int i = 0; i < readings.size() && i < dataSeries.getData().size(); i++) {
                Node node = dataSeries.getData().get(i).getNode();
                if (node == null) continue;
                String color = switch (readings.get(i).getSeverity()) {
                    case CRITICAL -> "#ef4444";
                    case WARNING  -> "#f59e0b";
                    default       -> "#22c55e";
                };
                node.setStyle("-fx-background-color: " + color + ", white;"
                    + " -fx-background-radius: 5px; -fx-padding: 4px;");
            }
        });

        ScrollPane scroll = new ScrollPane(chart);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(362); // chart + scrollbar
        scroll.setMaxHeight(362);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    // ── Table ──────────────────────────────────────────────────────────────

    private TableView<NumericSensorReading> buildTable(List<NumericSensorReading> readings) {
        TableView<NumericSensorReading> table = new TableView<>();
        table.setPrefHeight(200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);

        TableColumn<NumericSensorReading, String> colDate = new TableColumn<>("Date / Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTimestamp().format(TABLE_FMT)));
        colDate.setMinWidth(180);

        TableColumn<NumericSensorReading, String> colVal = new TableColumn<>("Value");
        colVal.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.4f %s", d.getValue().getValue(), d.getValue().getUnit())));
        colVal.setMinWidth(130);

        TableColumn<NumericSensorReading, String> colLevel = new TableColumn<>("Level");
        colLevel.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
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
            @Override protected void updateItem(NumericSensorReading r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(switch (r.getSeverity()) {
                    case CRITICAL -> "-fx-background-color: #fee2e2;";
                    case WARNING  -> "-fx-background-color: #fef9c3;";
                    default -> "";
                });
            }
        });
        return table;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void applyLineStyle(XYChart.Series<String, Number> series, String hex) {
        if (series.getNode() == null) return;
        Node line = series.getNode().lookup(".chart-series-line");
        if (line != null)
            line.setStyle("-fx-stroke: " + hex + "; -fx-stroke-width: 2.5; -fx-stroke-dash-array: 10 5;");
    }

    private void hideSymbols(XYChart.Series<String, Number> series) {
        for (XYChart.Data<?, ?> d : series.getData())
            if (d.getNode() != null) d.getNode().setVisible(false);
    }

    private String levelCss(ReadingLevel level) {
        return switch (level) {
            case CRITICAL -> "badge-critical";
            case WARNING  -> "badge-warning";
            default       -> "badge-normal";
        };
    }
}
