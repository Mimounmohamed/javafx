package com.example.controllers;

import ZONES.GoegraphicBoundries;
import com.example.services.FarmService;
import com.example.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class SettingsController {

    @FXML private TextField    farmNameField;
    @FXML private TextField    locationField;
    @FXML private TextField    ownerField;
    @FXML private ToggleButton themeToggle;
    @FXML private Label        farmIdLabel;
    @FXML private Label        farmBoundaryStatus;

    private final FarmService farmService = FarmService.getInstance();
    private boolean           darkTheme   = false;

    @FXML
    public void initialize() {
        farmNameField.setText(farmService.getFarmName());
        locationField.setText(farmService.getFarmLocation());
        ownerField.setText(farmService.getOwnerName());
        farmIdLabel.setText("Farm ID: " + farmService.getFarm().getId().substring(0, 8));
        themeToggle.setText("☀  Light Mode");
        themeToggle.setSelected(false);
        refreshBoundaryStatus();
    }

    @FXML private void saveFarmInfo() {
        try {
            farmService.setFarmName(farmNameField.getText().trim());
            farmService.setFarmLocation(locationField.getText().trim());
            farmService.setOwnerName(ownerField.getText().trim());
            new Alert(Alert.AlertType.INFORMATION,
                "Farm information saved.", ButtonType.OK).showAndWait();
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR,
                e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML private void editFarmBoundary() {
        String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();
        GoegraphicBoundries existing = farmService.hasFarmBoundary() ? farmService.getFarmBoundary() : null;

        // Collect all zone boundaries to show as reference (blue outlines, no overlap enforcement)
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

    @FXML private void switchFarm() {
        SceneManager.getInstance().navigateToStartup();
    }

    @FXML private void toggleTheme() {
        darkTheme = themeToggle.isSelected();
        themeToggle.setText(darkTheme ? "🌙  Dark Mode" : "☀  Light Mode");
        String css = darkTheme
            ? "/com/example/styles/dark.css"
            : "/com/example/styles/main.css";
        SceneManager.getInstance().applyStylesheet(css);
    }
}
