package com.example.controllers;

import ZONES.GoegraphicBoundries;
import com.example.services.FarmService;
import com.example.utils.AppPreferences;
import com.example.utils.SaveHistoryRepository;
import com.example.utils.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsController {

    @javafx.fxml.FXML private TextField    farmNameField;
    @javafx.fxml.FXML private TextField    locationField;
    @javafx.fxml.FXML private TextField    ownerField;
    @javafx.fxml.FXML private ToggleButton themeToggle;
    @javafx.fxml.FXML private Label        farmIdLabel;
    @javafx.fxml.FXML private Label        farmBoundaryStatus;

    // Auto-save
    @javafx.fxml.FXML private ToggleButton     autoSaveToggle;
    @javafx.fxml.FXML private ComboBox<String> autoSaveIntervalCombo;
    @javafx.fxml.FXML private Label            autoSaveStatusLabel;
    @javafx.fxml.FXML private Label            lastSavedLabel;

    // Save history
    @javafx.fxml.FXML private VBox historyContainer;

    private final FarmService farmService = FarmService.getInstance();
    private boolean darkTheme = false;

    private static final Map<String, Integer> INTERVALS = new LinkedHashMap<>();
    static {
        INTERVALS.put("30 seconds",  30);
        INTERVALS.put("1 minute",    60);
        INTERVALS.put("5 minutes",  300);
        INTERVALS.put("10 minutes", 600);
        INTERVALS.put("30 minutes", 1800);
    }

    @javafx.fxml.FXML
    public void initialize() {
        farmNameField.setText(farmService.getFarmName());
        locationField.setText(farmService.getFarmLocation());
        ownerField.setText(farmService.getOwnerName());
        farmIdLabel.setText("Farm ID: " + farmService.getFarm().getId().substring(0, 8));
        refreshBoundaryStatus();

        // Theme — read persisted preference so the toggle reflects actual state
        AppPreferences prefs = AppPreferences.getInstance();
        darkTheme = prefs.isDarkTheme();
        themeToggle.setSelected(darkTheme);
        themeToggle.setText(darkTheme ? "🌙  Dark Mode" : "☀  Light Mode");

        // Auto-save
        autoSaveIntervalCombo.getItems().addAll(INTERVALS.keySet());
        int current = prefs.getAutoSaveIntervalSeconds();
        String label = INTERVALS.entrySet().stream()
            .filter(e -> e.getValue() == current)
            .map(Map.Entry::getKey)
            .findFirst().orElse("1 minute");
        autoSaveIntervalCombo.setValue(label);

        boolean enabled = prefs.isAutoSaveEnabled();
        autoSaveToggle.setSelected(enabled);
        autoSaveToggle.setText(enabled ? "🟢  Enabled" : "⏸  Disabled");

        updateAutoSaveStatus();
        refreshLastSaved();
        refreshHistory();
    }

    // ── Farm information ──────────────────────────────────────────────

    @javafx.fxml.FXML private void saveFarmInfo() {
        try {
            farmService.setFarmName(farmNameField.getText().trim());
            farmService.setFarmLocation(locationField.getText().trim());
            farmService.setOwnerName(ownerField.getText().trim());
            if (!farmService.isDemo()) {
                SaveHistoryRepository.record(farmService.getSavedId(), farmService.getFarmName(), "Settings");
                refreshHistory();
                refreshLastSaved();
            }
            new Alert(Alert.AlertType.INFORMATION, "Farm information saved.", ButtonType.OK).showAndWait();
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @javafx.fxml.FXML private void editFarmBoundary() {
        String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();
        GoegraphicBoundries existing = farmService.hasFarmBoundary() ? farmService.getFarmBoundary() : null;

        List<GoegraphicBoundries> zoneBoundaries = new ArrayList<>();
        Farm.Farm farm = farmService.getFarm();
        farm.getLivestockZones().stream()
            .filter(z -> z.hasBoundaries()).map(z -> z.getBoundaries()).forEach(zoneBoundaries::add);
        farm.getCropZones().stream()
            .filter(z -> z.hasBoundaries()).map(z -> z.getBoundaries()).forEach(zoneBoundaries::add);
        farm.getAquacultureZones().stream()
            .filter(z -> z.hasBoundaries()).map(z -> z.getBoundaries()).forEach(zoneBoundaries::add);

        new BoundaryEditorDialog("Farm Boundary", existing, null, zoneBoundaries, true, List.of(css))
            .showAndWait()
            .ifPresent(b -> {
                farmService.setFarmBoundary(b);
                refreshBoundaryStatus();
            });
    }

    private void refreshBoundaryStatus() {
        if (farmService.hasFarmBoundary()) {
            farmBoundaryStatus.setText("✓  " + farmService.getFarmBoundary().size() + " boundary points defined");
            farmBoundaryStatus.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: 600;");
        } else {
            farmBoundaryStatus.setText("No boundary defined  (optional)");
            farmBoundaryStatus.setStyle("");
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────

    @javafx.fxml.FXML private void toggleTheme() {
        darkTheme = themeToggle.isSelected();
        themeToggle.setText(darkTheme ? "🌙  Dark Mode" : "☀  Light Mode");
        String css = darkTheme ? "/com/example/styles/dark.css" : "/com/example/styles/main.css";
        SceneManager.getInstance().applyStylesheet(css);
        AppPreferences.getInstance().setDarkTheme(darkTheme);
    }

    // ── Farm selection ────────────────────────────────────────────────

    @javafx.fxml.FXML private void switchFarm() {
        SceneManager.getInstance().navigateToStartup();
    }

    // ── Auto-save ─────────────────────────────────────────────────────

    @javafx.fxml.FXML private void toggleAutoSave() {
        boolean enabled = autoSaveToggle.isSelected();
        autoSaveToggle.setText(enabled ? "🟢  Enabled" : "⏸  Disabled");
        AppPreferences.getInstance().setAutoSaveEnabled(enabled);
        if (enabled) {
            farmService.startAutoSaveTimer();
        } else {
            farmService.stopAutoSaveTimer();
        }
        updateAutoSaveStatus();
    }

    @javafx.fxml.FXML private void changeInterval() {
        String sel = autoSaveIntervalCombo.getValue();
        if (sel == null) return;
        Integer seconds = INTERVALS.get(sel);
        if (seconds == null) return;
        AppPreferences.getInstance().setAutoSaveIntervalSeconds(seconds);
        if (AppPreferences.getInstance().isAutoSaveEnabled())
            farmService.startAutoSaveTimer();
        updateAutoSaveStatus();
    }

    @javafx.fxml.FXML private void saveNow() {
        if (farmService.isDemo()) {
            new Alert(Alert.AlertType.INFORMATION,
                "The Demo Farm is read-only and cannot be saved.", ButtonType.OK).showAndWait();
            return;
        }
        farmService.manualSave();
        refreshLastSaved();
        refreshHistory();
        new Alert(Alert.AlertType.INFORMATION, "Farm saved successfully.", ButtonType.OK).showAndWait();
    }

    private void updateAutoSaveStatus() {
        AppPreferences prefs = AppPreferences.getInstance();
        if (!prefs.isAutoSaveEnabled()) {
            autoSaveStatusLabel.setText("Auto-save is disabled");
            autoSaveStatusLabel.getStyleClass().setAll("autosave-status-off");
        } else {
            int s = prefs.getAutoSaveIntervalSeconds();
            String human = s < 60 ? s + " second" + (s == 1 ? "" : "s")
                : s < 3600 ? (s / 60) + " minute" + (s / 60 == 1 ? "" : "s")
                : (s / 3600) + " hour" + (s / 3600 == 1 ? "" : "s");
            autoSaveStatusLabel.setText("Saving every " + human);
            autoSaveStatusLabel.getStyleClass().setAll("autosave-status-on");
        }
    }

    // ── Save history ──────────────────────────────────────────────────

    @javafx.fxml.FXML private void refreshHistory() {
        historyContainer.getChildren().clear();

        if (farmService.isDemo()) {
            Label lbl = new Label("Save history is not available for the Demo Farm.");
            lbl.getStyleClass().add("save-history-empty");
            historyContainer.getChildren().add(lbl);
            return;
        }

        List<SaveHistoryRepository.SaveEntry> all =
            SaveHistoryRepository.loadForFarm(farmService.getSavedId());
        List<SaveHistoryRepository.SaveEntry> entries =
            all.subList(0, Math.min(15, all.size()));

        if (entries.isEmpty()) {
            Label lbl = new Label(
                "No saves recorded yet. Data is saved automatically in the background.");
            lbl.getStyleClass().add("save-history-empty");
            lbl.setWrapText(true);
            historyContainer.getChildren().add(lbl);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");
        for (SaveHistoryRepository.SaveEntry e : entries) {
            Label timeLbl = new Label(e.timestamp().format(fmt));
            timeLbl.getStyleClass().add("save-history-time");

            Label typeLbl = new Label(e.type());
            typeLbl.getStyleClass().addAll("save-history-type", typeClass(e.type()));

            Label farmLbl = new Label(e.farmName());
            farmLbl.getStyleClass().add("save-history-farm");
            HBox.setHgrow(farmLbl, Priority.ALWAYS);

            HBox row = new HBox(16, timeLbl, typeLbl, farmLbl);
            row.getStyleClass().add("save-history-row");
            row.setAlignment(Pos.CENTER_LEFT);
            historyContainer.getChildren().add(row);
        }
    }

    private String typeClass(String type) {
        return switch (type) {
            case "Manual"   -> "save-history-type-manual";
            case "Settings" -> "save-history-type-settings";
            default         -> "save-history-type-scheduled";
        };
    }

    private void refreshLastSaved() {
        String ts = farmService.getLastSavedAt();
        lastSavedLabel.setText(ts == null ? "Not saved yet this session" : "Last saved: " + ts);
    }

    // ── Help ──────────────────────────────────────────────────────────

    @javafx.fxml.FXML private void openHelp() {
        String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();
        new HelpDialog(List.of(css)).showAndWait();
    }
}
