package com.example.controllers;

import com.example.services.AlertService;
import com.example.services.FarmService;
import com.example.utils.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private VBox   sidebar;
    @FXML private Label  logoLabel;
    @FXML private Label  farmNameLabel;
    @FXML private Label  clockLabel;
    @FXML private Label  alertBadge;
    @FXML private Label  sidebarFarmName;
    @FXML private Label  sidebarAlertBadge;

    @FXML private Button btnDashboard;
    @FXML private Button btnZones;
    @FXML private Button btnAnimals;
    @FXML private Button btnSensors;
    @FXML private Button btnAlerts;
    @FXML private Button btnReports;
    @FXML private Button btnSimulation;
    @FXML private Button btnSettings;

    private boolean expanded = true;
    private Button  activeBtn;

    private static final DateTimeFormatter CLOCK_FMT =
        DateTimeFormatter.ofPattern("EEE dd MMM  HH:mm:ss");

    private static final String[] ICONS  = {"📊", "🗺", "🐄", "📡", "🔔", "📋", "⏩", "⚙"};
    private static final String[] LABELS = {"Dashboard", "Zones", "Animals",
                                            "Sensors", "Alerts", "Reports", "Simulation", "Settings"};

    @FXML
    public void initialize() {
        String farmName = FarmService.getInstance().getFarmName();
        farmNameLabel.setText(farmName);
        if (sidebarFarmName != null) sidebarFarmName.setText(farmName);
        startClock();
        updateAlertBadge();
        setActive(btnDashboard);
    }

    // ── Live clock ──────────────────────────────────────────────────

    private void startClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT));
        Timeline clock = new Timeline(
            new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // ── Alert badge ─────────────────────────────────────────────────

    private void updateAlertBadge() {
        int count = AlertService.getInstance().getActiveAlertCount();
        alertBadge.setText(String.valueOf(count));
        alertBadge.setVisible(count > 0);
        alertBadge.setManaged(count > 0);
        if (sidebarAlertBadge != null) {
            sidebarAlertBadge.setText(String.valueOf(count));
            sidebarAlertBadge.setVisible(count > 0);
            sidebarAlertBadge.setManaged(count > 0);
        }
    }

    // ── Sidebar toggle ───────────────────────────────────────────────

    @FXML private void toggleSidebar() {
        double target = expanded ? 64 : 240;
        KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), target);
        new Timeline(new KeyFrame(Duration.millis(200), kv)).play();
        expanded = !expanded;

        logoLabel.setVisible(expanded);
        logoLabel.setManaged(expanded);
        if (sidebarFarmName != null) { sidebarFarmName.setVisible(expanded); sidebarFarmName.setManaged(expanded); }

        Button[] buttons = {btnDashboard, btnZones, btnAnimals,
                            btnSensors,  btnAlerts, btnReports, btnSimulation, btnSettings};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setText(expanded ? ICONS[i] + "   " + LABELS[i] : ICONS[i]);
        }
    }

    // ── Active nav highlight ─────────────────────────────────────────

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.getStyleClass().remove("nav-active");
        activeBtn = btn;
        if (!btn.getStyleClass().contains("nav-active"))
            btn.getStyleClass().add("nav-active");
        updateAlertBadge();
    }

    // ── Navigation handlers ──────────────────────────────────────────

    @FXML private void navDashboard() { setActive(btnDashboard); SceneManager.getInstance().navigateTo("dashboard"); }
    @FXML private void navZones()     { setActive(btnZones);     SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void navAnimals()   { setActive(btnAnimals);   SceneManager.getInstance().navigateTo("animals"); }
    @FXML private void navSensors()   { setActive(btnSensors);   SceneManager.getInstance().navigateTo("sensors"); }
    @FXML private void navAlerts()    { setActive(btnAlerts);    SceneManager.getInstance().navigateTo("alerts"); }
    @FXML private void navReports()    { setActive(btnReports);    SceneManager.getInstance().navigateTo("reports"); }
    @FXML private void navSimulation() { setActive(btnSimulation); SceneManager.getInstance().navigateTo("simulation"); }
    @FXML private void navSettings()   { setActive(btnSettings);   SceneManager.getInstance().navigateTo("settings"); }
}
