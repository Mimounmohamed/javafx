package com.example.controllers;

import Alerts.AlertResolution;
import Reports.FarmProductionReport;
import Reports.FarmReport;
import Reports.Report;
import Reports.ReportType;
import ZONES.ZONE;
import com.example.services.AlertService;
import com.example.services.AnimalService;
import com.example.services.FarmService;
import com.example.services.ReportService;
import com.example.services.SensorService;
import com.example.services.ZoneService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ReportsController {

    @FXML private ListView<String> reportTypeList;
    @FXML private ComboBox<String> periodCombo;
    @FXML private TextArea         reportContent;
    @FXML private Label            reportTitle;

    @FXML private Label statZones;
    @FXML private Label statAnimals;
    @FXML private Label statSensors;
    @FXML private Label statActiveAlerts;

    private final ReportService reportService = ReportService.getInstance();
    private final ZoneService   zoneService   = ZoneService.getInstance();
    private String              currentText   = "";

    @FXML
    public void initialize() {
        periodCombo.getItems().addAll("Daily", "Weekly", "Monthly", "Quarterly", "Yearly");
        periodCombo.setValue("Monthly");

        statZones.setText(String.valueOf(zoneService.getAllZones().size()));
        statAnimals.setText(String.valueOf(AnimalService.getInstance().getAllAnimals().size()));
        statSensors.setText(String.valueOf(SensorService.getInstance().getAllSensors().size()));
        long activeAlerts = AlertService.getInstance().getAllAlerts().stream()
            .filter(a -> a.getResolution() == AlertResolution.ACTIVE).count();
        statActiveAlerts.setText(String.valueOf(activeAlerts));

        List<String> items = new ArrayList<>();
        items.add("Farm Overview");
        items.add("Farm Production");
        for (ZONE z : zoneService.getAllZones())
            items.add("Zone: " + z.getName());

        reportTypeList.getItems().addAll(items);
        reportTypeList.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) generateReport(n); });
        reportTypeList.getSelectionModel().select(0);
    }

    // ── Report generation ─────────────────────────────────────────────

    private void generateReport(String selection) {
        ReportType type = ReportType.valueOf(periodCombo.getValue());
        try {
            if ("Farm Overview".equals(selection)) {
                FarmReport r = reportService.generateFarmReport(type);
                reportTitle.setText("Farm Overview — " + type);
                currentText = r.toString();

            } else if ("Farm Production".equals(selection)) {
                FarmProductionReport r = reportService.generateFarmProductionReport(type);
                reportTitle.setText("Farm Production — " + type);
                currentText = r.toString();

            } else {
                String zoneName = selection.replace("Zone: ", "");
                ZONE zone = FarmService.getInstance().getFarm().getZoneByName(zoneName);
                if (zone != null) {
                    Report r = reportService.generateZoneReport(zone, type);
                    reportTitle.setText("Zone Report: " + zoneName + " — " + type);
                    currentText = r.toString();
                }
            }
            reportContent.setText(currentText);
        } catch (Exception e) {
            reportContent.setText("Error generating report:\n" + e.getMessage());
        }
    }

    // ── Toolbar actions ───────────────────────────────────────────────

    @FXML private void refreshReport() {
        String sel = reportTypeList.getSelectionModel().getSelectedItem();
        if (sel != null) generateReport(sel);
    }

    @FXML private void exportReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("farm_report.txt");
        File file = chooser.showSaveDialog(reportContent.getScene().getWindow());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), currentText);
                new Alert(Alert.AlertType.INFORMATION,
                    "Exported to: " + file.getName(), ButtonType.OK).showAndWait();
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                    "Export failed: " + e.getMessage(), ButtonType.OK).showAndWait();
            }
        }
    }
}
