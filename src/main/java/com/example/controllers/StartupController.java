package com.example.controllers;

import com.example.services.DataRandomizerService;
import com.example.services.FarmService;
import com.example.utils.FarmRepository;
import com.example.utils.RandomFarmGenerator;
import com.example.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class StartupController {

    @FXML private ListView<FarmRepository.SavedFarm> farmList;
    @FXML private Label   emptyLabel;
    @FXML private TextField newFarmName;
    @FXML private TextField newFarmLocation;
    @FXML private TextField newFarmOwner;
    @FXML private Label   errorLabel;

    private ObservableList<FarmRepository.SavedFarm> farms;

    @FXML
    public void initialize() {
        farms = FXCollections.observableArrayList();

        // Demo farm is always the first entry
        List<FarmRepository.SavedFarm> saved = FarmRepository.loadAll();
        boolean hasDemo = saved.stream().anyMatch(f -> f.isDemo);
        if (!hasDemo) {
            // Pre-populate demo entry so it appears without the user having to open it first
            FarmRepository.SavedFarm demo = FarmRepository.SavedFarm.forDemo();
            FarmRepository.save(demo);
            saved = FarmRepository.loadAll();
        }
        // Demo first, then user farms
        saved.stream().filter(f ->  f.isDemo).forEach(farms::add);
        saved.stream().filter(f -> !f.isDemo).forEach(farms::add);

        farmList.setItems(farms);
        farmList.setCellFactory(lv -> new FarmCell());

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);
    }

    // ── Create new farm ───────────────────────────────────────────────

    @FXML
    private void createWithSampleData() { doCreateFarm(true); }

    @FXML
    private void createEmptyFarm() { doCreateFarm(false); }

    private void doCreateFarm(boolean populate) {
        String name  = newFarmName.getText().trim();
        String loc   = newFarmLocation.getText().trim();
        String owner = newFarmOwner.getText().trim();

        if (name.isBlank() || owner.isBlank()) {
            showError("Farm Name and Owner are required fields.");
            return;
        }
        try {
            FarmService.initWithNewFarm(name, loc, owner);
            if (populate) FarmService.getInstance().markAsRandomized();
            FarmService.getInstance().autoSave();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        if (populate) {
            runWithLoading("Generating farm data, please wait…", () -> {
                try { DataRandomizerService.getInstance().populateNewFarm(); }
                catch (Exception ex) { ex.printStackTrace(System.err); }
            }, () -> {
                refreshFarmList();
                offerFarmBoundary();
                SceneManager.getInstance().loadMainApp();
            });
            return;
        }

        refreshFarmList();
        offerFarmBoundary();
        SceneManager.getInstance().loadMainApp();
    }

    // ── Random farm ───────────────────────────────────────────────────

    @FXML
    private void generateRandomFarm() {
        String[] meta = RandomFarmGenerator.randomMeta();
        try {
            FarmService.initWithNewFarm(meta[0], meta[1], meta[2]);
            FarmService.getInstance().markAsRandomized();
            FarmService.getInstance().autoSave();
        } catch (Exception e) {
            showError("Failed to create farm: " + e.getMessage());
            return;
        }
        runWithLoading("Generating random farm data, please wait…", () -> {
            try { DataRandomizerService.getInstance().populateNewFarm(); }
            catch (Exception ex) { ex.printStackTrace(System.err); }
        }, () -> {
            refreshFarmList();
            offerFarmBoundary();
            SceneManager.getInstance().loadMainApp();
        });
    }

    // ── Open / delete actions (called from cells) ─────────────────────

    void openFarm(FarmRepository.SavedFarm sf) {
        FarmService.initFromSaved(sf);
        SceneManager.getInstance().loadMainApp();
    }

    void deleteFarm(FarmRepository.SavedFarm sf) {
        FarmRepository.delete(sf.id);
        farms.remove(sf);
    }

    // ── Farm boundary offer ───────────────────────────────────────────

    private void offerFarmBoundary() {
        if (FarmService.getInstance().hasFarmBoundary()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Farm Boundary");
        confirm.setHeaderText(null);
        confirm.setContentText("Do you want to draw the geographic boundary of your farm?\n"
            + "This is optional — zone boundaries can be constrained inside it.");
        confirm.getButtonTypes().setAll(
            new ButtonType("Draw Boundary"),
            new ButtonType("Skip")
        );
        confirm.showAndWait().ifPresent(bt -> {
            if (!"Draw Boundary".equals(bt.getText())) return;
            String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();
            new BoundaryEditorDialog("Farm Boundary", null, List.of(css))
                .showAndWait()
                .ifPresent(b -> FarmService.getInstance().setFarmBoundary(b));
        });
    }

    // ── Background generation helper ──────────────────────────────────

    private void runWithLoading(String msg, Runnable work, Runnable onDone) {
        javafx.scene.Parent root = farmList.getScene().getRoot();
        root.setDisable(true);
        root.setCursor(javafx.scene.Cursor.WAIT);
        errorLabel.setText("⏳  " + msg);
        errorLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() { work.run(); return null; }
        };
        Runnable cleanup = () -> {
            root.setDisable(false);
            root.setCursor(javafx.scene.Cursor.DEFAULT);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        };
        task.setOnSucceeded(e -> { cleanup.run(); onDone.run(); });
        task.setOnFailed(e -> {
            cleanup.run();
            if (task.getException() != null) task.getException().printStackTrace(System.err);
            onDone.run();
        });
        new Thread(task, "farm-generator").start();
    }

    private void refreshFarmList() {
        List<FarmRepository.SavedFarm> updated = FarmRepository.loadAll();
        farms.clear();
        updated.stream().filter(f ->  f.isDemo).forEach(farms::add);
        updated.stream().filter(f -> !f.isDemo).forEach(farms::add);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // ── Custom list cell ──────────────────────────────────────────────

    private class FarmCell extends ListCell<FarmRepository.SavedFarm> {

        private final HBox   row       = new HBox(16);
        private final VBox   info      = new VBox(4);
        private final Label  nameLabel = new Label();
        private final Label  metaLabel = new Label();
        private final Label  demoTag   = new Label("DEMO");
        private final Button openBtn   = new Button("Open →");
        private final Button deleteBtn = new Button("Delete");

        FarmCell() {
            nameLabel.getStyleClass().add("farm-row-name");
            metaLabel.getStyleClass().add("farm-row-meta");
            demoTag.getStyleClass().addAll("badge", "badge-active");

            openBtn.getStyleClass().add("btn-primary");
            deleteBtn.getStyleClass().add("btn-secondary");

            HBox nameLine = new HBox(8, nameLabel, demoTag);
            nameLine.setAlignment(Pos.CENTER_LEFT);
            info.getChildren().addAll(nameLine, metaLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(info, spacer, openBtn, deleteBtn);
            row.getStyleClass().add("farm-row");
            row.setAlignment(Pos.CENTER_LEFT);

            openBtn.setOnAction(e -> {
                FarmRepository.SavedFarm sf = getItem();
                if (sf != null) openFarm(sf);
            });
            deleteBtn.setOnAction(e -> {
                FarmRepository.SavedFarm sf = getItem();
                if (sf != null) deleteFarm(sf);
            });
        }

        @Override
        protected void updateItem(FarmRepository.SavedFarm sf, boolean empty) {
            super.updateItem(sf, empty);
            if (empty || sf == null) { setGraphic(null); setText(null); return; }

            nameLabel.setText(sf.name);
            demoTag.setVisible(sf.isDemo);
            demoTag.setManaged(sf.isDemo);

            String zonesStr = sf.zoneCount() + " zone" + (sf.zoneCount() == 1 ? "" : "s");
            String animStr  = sf.animalCount() + " animal" + (sf.animalCount() == 1 ? "" : "s");
            metaLabel.setText(sf.subtitle() + " · " + zonesStr + " · " + animStr
                + " · Created " + sf.createdAt);

            deleteBtn.setVisible(!sf.isDemo);
            deleteBtn.setManaged(!sf.isDemo);
            setGraphic(row);
            setText(null);
        }
    }
}
