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
import javafx.scene.control.TableView;
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
    @FXML private Label lblFarmSummary;
    @FXML private VBox  chartContainer;

    @FXML private TableView<Alert>          recentAlertsTable;
    @FXML private TableColumn<Alert, String> colAlertTime;
    @FXML private TableColumn<Alert, String> colAlertSeverity;
    @FXML private TableColumn<Alert, String> colAlertMsg;

    @FXML
    public void initialize() {
        loadKpis();
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

    // ── Production bar chart ─────────────────────────────────────────

    private void buildProductionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Zone Type");
        yAxis.setLabel("Output");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Production Overview");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("farm-chart");
        chart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Farm farm = FarmService.getInstance().getFarm();

        double milk = farm.getLivestockZones().stream()
            .mapToDouble(z -> z.getTotalMilkYield()).sum();
        double crop = farm.getCropZones().stream()
            .mapToDouble(z -> z.getTotalCropYield()).sum();
        double aqua = farm.getAquacultureZones().stream()
            .mapToDouble(z -> z.getTotalHarvestWeight()).sum();

        series.getData().add(new XYChart.Data<>("Livestock (L)",     milk));
        series.getData().add(new XYChart.Data<>("Crops (kg)",        crop));
        series.getData().add(new XYChart.Data<>("Aquaculture (kg)",  aqua));
        chart.getData().add(series);

        chartContainer.getChildren().add(chart);
    }

    // ── Recent alerts table ──────────────────────────────────────────

    private void buildRecentAlertsTable() {
        colAlertTime.setCellValueFactory(d ->
            new SimpleStringProperty(
                d.getValue().getTimestamp().toLocalDate().toString()));

        colAlertMsg.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getMessage()));

        // Severity badge cell — no value factory needed, uses row item directly
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
}
