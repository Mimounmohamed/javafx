package com.example.controllers;

import com.example.services.SimulationService;
import com.example.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;

public class SimulationController {

    @FXML private Label simDateLabel;
    @FXML private Label simDayLabel;

    @FXML private Label kpiMilk;
    @FXML private Label kpiEggs;
    @FXML private Label kpiHarvests;
    @FXML private Label kpiHealthEvents;
    @FXML private Label kpiAlerts;
    @FXML private Label kpiMortality;

    @FXML private ListView<String> activityLog;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SimulationService sim = SimulationService.getInstance();

    @FXML
    public void initialize() {
        refreshDateDisplay();
        resetKpis();
    }

    // ── Step buttons ──────────────────────────────────────────────────

    @FXML private void stepOneDay()   { runStep(1); }
    @FXML private void stepOneWeek()  { runStep(7); }
    @FXML private void stepOneMonth() { runStep(30); }

    @FXML
    private void resetSim() {
        sim.reset();
        activityLog.getItems().clear();
        refreshDateDisplay();
        resetKpis();
    }

    @FXML
    private void clearLog() {
        activityLog.getItems().clear();
    }

    @FXML private void goToReports() { SceneManager.getInstance().navigateTo("reports"); }
    @FXML private void goToSensors() { SceneManager.getInstance().navigateTo("sensors"); }
    @FXML private void goToAnimals() { SceneManager.getInstance().navigateTo("animals"); }

    // ── Internal helpers ──────────────────────────────────────────────

    private void runStep(int days) {
        SimulationService.SimulationResult result = sim.simulateDays(days);
        refreshDateDisplay();
        updateKpis(result);

        // Prepend new entries so the most recent events appear at the top
        if (result.log.isEmpty()) {
            activityLog.getItems().add(0,
                "[" + sim.getSimulationDate() + "]  (quiet period — no notable events)");
        } else {
            // Add in reverse so that within the step, day-1 ends up above day-N
            for (int i = result.log.size() - 1; i >= 0; i--)
                activityLog.getItems().add(0, result.log.get(i));
        }
    }

    private void refreshDateDisplay() {
        simDateLabel.setText(sim.getSimulationDate().format(DATE_FMT));
        int d = sim.getTotalDaysSimulated();
        simDayLabel.setText(d == 0 ? "Not started" : "Day +" + d + " simulated");
    }

    private void updateKpis(SimulationService.SimulationResult r) {
        kpiMilk.setText(String.format("%.1f L", r.milkLitersTotal));
        kpiEggs.setText(String.valueOf(r.eggsTotal));
        kpiHarvests.setText(String.valueOf(r.harvestsRecorded));
        kpiHealthEvents.setText(String.valueOf(r.healthEvents));
        kpiAlerts.setText(String.valueOf(r.alertsGenerated));
        kpiMortality.setText(String.valueOf(r.mortalityCount));
    }

    private void resetKpis() {
        kpiMilk.setText("—");
        kpiEggs.setText("—");
        kpiHarvests.setText("—");
        kpiHealthEvents.setText("—");
        kpiAlerts.setText("—");
        kpiMortality.setText("—");
    }
}
