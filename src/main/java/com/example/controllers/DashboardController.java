package com.example.controllers;

import Alerts.Alert;
import Alerts.AlertSeverity;
import Farm.Farm;
import com.example.services.AlertService;
import com.example.services.FarmService;
import com.example.services.SensorService;
import com.example.services.ZoneService;
import com.example.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardController {

    @FXML private Label kpiAnimals;
    @FXML private Label kpiSickAnimals;
    @FXML private Label kpiZones;
    @FXML private Label kpiTotalZones;
    @FXML private Label kpiSensors;
    @FXML private Label kpiAlerts;
    @FXML private Label kpiFields;
    @FXML private Label kpiSpecies;
    @FXML private Label     lblFarmSummary;
    @FXML private FlowPane  summaryChipsPane;
    @FXML private VBox      chartContainer;

    @FXML private TableView<Alert>          recentAlertsTable;
    @FXML private TableColumn<Alert, String> colAlertTime;
    @FXML private TableColumn<Alert, String> colAlertSeverity;
    @FXML private TableColumn<Alert, String> colAlertMsg;

    @FXML
    public void initialize() {
        loadKpis();
        buildSummaryChips();
        buildProductionChart();
        buildRecentAlertsTable();
    }

    // ── KPI cards ───────────────────────────────────────────────────

    private void loadKpis() {
        Farm farm = FarmService.getInstance().getFarm();
        Farm.FarmStats stats = farm.getStats();
        kpiAnimals.setText(String.valueOf(stats.totalAnimals));
        kpiSickAnimals.setText(String.valueOf(stats.sickAnimals));
        kpiZones.setText(String.valueOf(ZoneService.getInstance().getActiveZoneCount()));
        kpiTotalZones.setText(String.valueOf(farm.getTotalZoneCount()));
        kpiSensors.setText(String.valueOf(SensorService.getInstance().getActiveSensorCount()));
        kpiAlerts.setText(String.valueOf(AlertService.getInstance().getActiveAlertCount()));
        kpiFields.setText(String.valueOf(stats.totalFields));
        kpiSpecies.setText(String.valueOf(stats.totalSpecies));
        lblFarmSummary.setText(farm.getSummary());
    }

    // ── Farm summary chips ───────────────────────────────────────────

    private void buildSummaryChips() {
        summaryChipsPane.getChildren().clear();
        String summary = FarmService.getInstance().getFarm().getSummary();
        // summary is pipe-separated: "Owner: X | Location: Y | ..."
        String[] parts = summary.split("\\s*\\|\\s*");
        String[] emojis = {"👤", "📍", "🐾", "🗺", "📡", "⚠"};
        for (int i = 0; i < parts.length; i++) {
            String prefix = i < emojis.length ? emojis[i] + "  " : "";
            String text = prefix + parts[i].trim();
            Label chip = new Label(text);
            boolean isAlertChip = parts[i].toLowerCase().contains("alert");
            chip.getStyleClass().add(isAlertChip ? "summary-chip-alert" : "summary-chip");
            summaryChipsPane.getChildren().add(chip);
        }
    }

    // ── Production bar chart ─────────────────────────────────────────

    private void buildProductionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Zone Type");
        yAxis.setLabel("Output");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.getStyleClass().add("farm-chart");
        chart.setPrefHeight(320);
        chart.setBarGap(4);
        chart.setCategoryGap(40);

        Farm farm = FarmService.getInstance().getFarm();

        double milk = farm.getLivestockZones().stream()
            .mapToDouble(z -> z.getTotalMilkYield()).sum();
        double crop = farm.getCropZones().stream()
            .mapToDouble(z -> z.getTotalCropYield()).sum();
        double aqua = farm.getAquacultureZones().stream()
            .mapToDouble(z -> z.getTotalHarvestWeight()).sum();

        XYChart.Series<String, Number> milkSeries = new XYChart.Series<>();
        milkSeries.setName("Livestock");
        milkSeries.getData().add(new XYChart.Data<>("Livestock (L)", milk));

        XYChart.Series<String, Number> cropSeries = new XYChart.Series<>();
        cropSeries.setName("Crops");
        cropSeries.getData().add(new XYChart.Data<>("Crops (kg)", crop));

        XYChart.Series<String, Number> aquaSeries = new XYChart.Series<>();
        aquaSeries.setName("Aquaculture");
        aquaSeries.getData().add(new XYChart.Data<>("Aquaculture (kg)", aqua));

        chart.getData().addAll(milkSeries, cropSeries, aquaSeries);
        chartContainer.getChildren().add(chart);
    }

    // ── Recent alerts table ──────────────────────────────────────────

    private void buildRecentAlertsTable() {
        colAlertTime.setCellValueFactory(d ->
            new SimpleStringProperty(
                d.getValue().getTimestamp().toLocalDate().toString()));

        colAlertMsg.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getMessage()));

        colAlertMsg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) { setText(null); setTooltip(null); return; }
                setText(msg);
                setTooltip(new Tooltip(msg));
            }
        });

        colAlertSeverity.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(a.getSeverity().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(
                    a.getSeverity() == AlertSeverity.Critical ? "badge-critical" : "badge-warning");
                setGraphic(badge);
            }
        });

        recentAlertsTable.setRowFactory(tv -> new TableRow<Alert>() {
            @Override
            protected void updateItem(Alert alert, boolean empty) {
                super.updateItem(alert, empty);
                getStyleClass().removeIf(c -> c.startsWith("alert-row-"));
                if (!empty && alert != null) {
                    getStyleClass().add(
                        alert.getSeverity() == AlertSeverity.Critical
                            ? "alert-row-critical"
                            : "alert-row-warning");
                }
            }
        });

        List<Alert> allAlerts = AlertService.getInstance().getAllAlerts();
        List<Alert> top5 = allAlerts.subList(0, Math.min(5, allAlerts.size()));
        recentAlertsTable.setItems(FXCollections.observableArrayList(top5));
    }

    // ── Quick actions ────────────────────────────────────────────────

    @FXML private void quickAddAnimal()   { SceneManager.getInstance().navigateTo("animals"); }
    @FXML private void quickAddZone()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickAddCrop()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickAddAqua()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickViewReports() { SceneManager.getInstance().navigateTo("reports"); }
    @FXML private void quickViewAlerts()  { SceneManager.getInstance().navigateTo("alerts"); }

}
