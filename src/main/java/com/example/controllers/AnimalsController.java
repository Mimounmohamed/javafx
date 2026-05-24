package com.example.controllers;

import Animals.Animal;
import Animals.AnimalHealthStatus;
import Animals.HealthEvent;
import Entities.LIvestockType;
import Sensors.BioSensor;
import Sensors.GPSCollarSensor;
import com.example.services.AnimalService;
import com.example.services.FarmService;
import com.example.services.ZoneService;
import ZONES.LivestockZONE;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.ArrayList;

public class AnimalsController {

    // ── Table ─────────────────────────────────────────────────────────
    @FXML private TableView<Animal>            animalTable;
    @FXML private TableColumn<Animal, String>  colId;
    @FXML private TableColumn<Animal, String>  colName;
    @FXML private TableColumn<Animal, String>  colSpecies;
    @FXML private TableColumn<Animal, String>  colType;
    @FXML private TableColumn<Animal, String>  colZone;
    @FXML private TableColumn<Animal, String>  colHealth;

    // ── Filters ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> filterZone;
    @FXML private ComboBox<String> filterHealth;
    @FXML private TextField        searchField;

    // ── Stat labels ───────────────────────────────────────────────────
    @FXML private Label statTotalAnimals;
    @FXML private Label statHealthy;
    @FXML private Label statSick;
    @FXML private Label statQuarantined;

    // ── Detail panels (Info tab keeps original fx:id; two new panels) ─
    @FXML private VBox detailPanel;       // Info tab content
    @FXML private VBox detailHeaderPanel; // dynamic header above tabs
    @FXML private VBox sensorsPanel;      // Sensors tab content
    @FXML private VBox actionsPanel;      // Actions tab content

    private ObservableList<Animal> allAnimals;
    private FilteredList<Animal>   filteredAnimals;

    private final AnimalService animalService = AnimalService.getInstance();
    private final ZoneService   zoneService   = ZoneService.getInstance();

    // ── Initialise ────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        allAnimals      = FXCollections.observableArrayList(animalService.getAllAnimals());
        filteredAnimals = new FilteredList<>(allAnimals, p -> true);

        setupTable();
        setupFilters();
        refreshStats();
    }

    private void refreshStats() {
        long healthy     = allAnimals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Healthy).count();
        long sick        = allAnimals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Sick).count();
        long quarantined = allAnimals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Quarantined).count();
        statTotalAnimals.setText(String.valueOf(allAnimals.size()));
        statHealthy.setText(String.valueOf(healthy));
        statSick.setText(String.valueOf(sick));
        statQuarantined.setText(String.valueOf(quarantined));
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupTable() {
        colId.setCellValueFactory(d      -> new SimpleStringProperty(d.getValue().getId().substring(0, 8)));
        colName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        colSpecies.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSpecies()));
        colType.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getType().toString()));
        colZone.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getZone().getName()));

        // ID column — monospace muted
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: #888780;");
            }
        });

        // Name column — bold
        colName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #2C2C2A;");
            }
        });

        // Type column — blue pill
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            { pill.getStyleClass().add("animals-pill-type"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                pill.setText(item);
                setGraphic(pill);
            }
        });

        // Health column — coloured pill
        colHealth.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Animal a = (Animal) getTableRow().getItem();
                badge.setText(a.getHealthStatus().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(healthCss(a.getHealthStatus()));
                setGraphic(badge);
            }
        });

        animalTable.setItems(filteredAnimals);
        animalTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, n) -> { if (n != null) showDetail(n); });
    }

    private String healthCss(AnimalHealthStatus s) {
        return switch (s) {
            case Healthy     -> "badge-healthy";
            case Sick        -> "badge-sick";
            case Quarantined -> "badge-quarantined";
        };
    }

    // ── Filter setup ──────────────────────────────────────────────────

    private void setupFilters() {
        filterZone.getItems().add("All Zones");
        zoneService.getLivestockZones().forEach(z -> filterZone.getItems().add(z.getName()));
        filterZone.setValue("All Zones");

        filterHealth.getItems().add("All");
        for (AnimalHealthStatus s : AnimalHealthStatus.values())
            filterHealth.getItems().add(s.toString());
        filterHealth.setValue("All");

        filterZone.setOnAction(e -> applyFilter());
        filterHealth.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, old, n) -> applyFilter());
    }

    private void applyFilter() {
        String zone   = filterZone.getValue();
        String health = filterHealth.getValue();
        String search = searchField.getText().toLowerCase();
        filteredAnimals.setPredicate(a -> {
            boolean zoneOk   = "All Zones".equals(zone)   || a.getZone().getName().equals(zone);
            boolean healthOk = "All".equals(health)        || a.getHealthStatus().toString().equals(health);
            boolean searchOk = search.isEmpty()
                || a.getName().toLowerCase().contains(search)
                || a.getSpecies().toLowerCase().contains(search);
            return zoneOk && healthOk && searchOk;
        });
    }

    // ── Add Animal dialog — @FXML handler (unchanged) ─────────────────

    @FXML
    private void showAddAnimalDialog() {
        List<LivestockZONE> zones = zoneService.getLivestockZones();
        if (zones.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Livestock Zone");
            alert.setHeaderText(null);
            alert.setContentText("Create a Livestock zone first before adding animals.");
            alert.showAndWait();
            return;
        }

        TextField nameField    = new TextField();  nameField.setPromptText("e.g. Bessie");
        TextField speciesField = new TextField();  speciesField.setPromptText("e.g. Cow, Sheep");
        TextField ageField     = new TextField("1");
        TextField weightField  = new TextField("100.0");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("RUMINANT", "POULTRY");
        typeCombo.setValue("RUMINANT");

        ComboBox<LivestockZONE> zoneCombo = new ComboBox<>();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.setValue(zones.get(0));
        zoneCombo.setConverter(new StringConverter<>() {
            @Override public String toString(LivestockZONE z) { return z == null ? "" : z.getName(); }
            @Override public LivestockZONE fromString(String s) { return null; }
        });

        VBox form = new VBox(14,
            formGroup("Name",         nameField),
            formGroup("Species",      speciesField),
            formGroup("Type",         typeCombo),
            formGroup("Age (years)",  ageField),
            formGroup("Weight (kg)",  weightField),
            formGroup("Zone",         zoneCombo)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Animal> dialog = new Dialog<>();
        dialog.setTitle("Add Animal");
        dialog.setHeaderText("Add a new animal to the farm");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(420);
        applyDialogStyle(dialog);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add Animal");
        okBtn.setDisable(true);
        Runnable validate = () -> okBtn.setDisable(
            nameField.getText().trim().isEmpty() || speciesField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, o, n) -> validate.run());
        speciesField.textProperty().addListener((obs, o, n) -> validate.run());

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String name    = nameField.getText().trim();
                String species = speciesField.getText().trim();
                LIvestockType type   = LIvestockType.valueOf(typeCombo.getValue());
                int    age    = Integer.parseInt(ageField.getText().trim());
                double weight = Double.parseDouble(weightField.getText().trim());
                LivestockZONE zone = zoneCombo.getValue();
                return animalService.addAnimal(name, species, type, age, weight, zone);
            } catch (NumberFormatException e) {
                return null;
            }
        });

        dialog.showAndWait().ifPresent(animal -> {
            allAnimals.add(animal);
            if (!filterZone.getItems().contains(animal.getZone().getName()))
                filterZone.getItems().add(animal.getZone().getName());
            refreshStats();
        });
    }

    // ── Detail panel — top-level dispatcher ───────────────────────────

    private void showDetail(Animal a) {
        detailHeaderPanel.getChildren().clear();
        detailPanel.getChildren().clear();
        sensorsPanel.getChildren().clear();
        actionsPanel.getChildren().clear();

        buildDetailHeader(a);
        buildInfoTab(a);
        buildSensorsTab(a);
        buildActionsTab(a);
    }

    // ── Panel header (avatar · name · health pill · ghost buttons) ────

    private void buildDetailHeader(Animal a) {
        // Avatar circle — letter + health color
        Label avatar = new Label(a.getName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().addAll("animals-avatar", avatarCss(a.getHealthStatus()));

        // Name + subtitle VBox
        Label nameLbl = new Label(a.getName());
        nameLbl.getStyleClass().add("animals-detail-name");
        Label subtitleLbl = new Label(
            a.getSpecies() + " · " + a.getType() + " · " + a.getId().substring(0, 8));
        subtitleLbl.getStyleClass().add("animals-detail-subtitle");
        VBox nameBox = new VBox(2, nameLbl, subtitleLbl);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        // Hero row
        HBox heroRow = new HBox(10, avatar, nameBox);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        // Health pill
        Label healthPill = new Label(a.getHealthStatus().toString());
        healthPill.getStyleClass().addAll("badge", healthCss(a.getHealthStatus()));

        // Ghost action buttons
        HBox actionBtns = new HBox(6);
        if (a.getHealthStatus() == AnimalHealthStatus.Healthy) {
            Button sickBtn = new Button("Mark sick");
            sickBtn.getStyleClass().add("btn-ghost");
            sickBtn.setOnAction(e -> showMarkHealthDialog(a, AnimalHealthStatus.Sick));
            Button quarBtn = new Button("Quarantine");
            quarBtn.getStyleClass().add("btn-ghost-danger");
            quarBtn.setOnAction(e -> showMarkHealthDialog(a, AnimalHealthStatus.Quarantined));
            actionBtns.getChildren().addAll(sickBtn, quarBtn);
        } else {
            Button resolveBtn = new Button("Resolve");
            resolveBtn.getStyleClass().add("btn-ghost");
            resolveBtn.setOnAction(e -> showResolveHealthDialog(a));
            actionBtns.getChildren().add(resolveBtn);
        }
        Button mapBtn = new Button("📍 Zone Map");
        mapBtn.getStyleClass().add("btn-ghost");
        mapBtn.setOnAction(e -> openZoneMap(a));
        actionBtns.getChildren().add(mapBtn);

        // Health row
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox healthRow = new HBox(8, healthPill, spacer, actionBtns);
        healthRow.setAlignment(Pos.CENTER_LEFT);

        detailHeaderPanel.setSpacing(10);
        detailHeaderPanel.setPadding(new Insets(14, 16, 12, 16));
        detailHeaderPanel.getChildren().addAll(heroRow, healthRow);
    }

    private String avatarCss(AnimalHealthStatus s) {
        return switch (s) {
            case Healthy     -> "animals-avatar-healthy";
            case Sick        -> "animals-avatar-sick";
            case Quarantined -> "animals-avatar-quarantined";
        };
    }

    // ── Info tab ──────────────────────────────────────────────────────

    private void buildInfoTab(Animal a) {
        addSectionTitle(detailPanel, "Animal details");
        addRow(detailPanel, "Age",    a.getAge() + " years");
        addRow(detailPanel, "Weight", String.format("%.1f kg", a.getWeight()));
        addRow(detailPanel, "Zone",   a.getZone().getName());
        addRow(detailPanel, "Health", a.getHealthStatus().toString());

        // Milk production
        if (a.getMilkYieldLiters() > 0) {
            addSectionTitle(detailPanel, "Milk production");
            addRow(detailPanel, "Total yield", String.format("%.2f L  (%d records)",
                a.getMilkYieldLiters(), a.getMilkHistory().size()));
            if (!a.getMilkHistory().isEmpty()) {
                Button btn = new Button("View milk history (" + a.getMilkHistory().size() + " records)");
                btn.getStyleClass().add("btn-secondary");
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setOnAction(e -> showHistoryDialog("Milk History — " + a.getName(),
                    a.getMilkHistory().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.toList())));
                detailPanel.getChildren().add(btn);
            }
        }

        // Egg production
        if (a.getEggCount() > 0) {
            addSectionTitle(detailPanel, "Egg production");
            addRow(detailPanel, "Total eggs", String.format("%d eggs  (%d records)",
                a.getEggCount(), a.getEggHistory().size()));
            if (!a.getEggHistory().isEmpty()) {
                Button btn = new Button("View egg history (" + a.getEggHistory().size() + " records)");
                btn.getStyleClass().add("btn-secondary");
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setOnAction(e -> showHistoryDialog("Egg History — " + a.getName(),
                    a.getEggHistory().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.toList())));
                detailPanel.getChildren().add(btn);
            }
        }

        // Weight history
        if (a.getWeightHistory().size() > 1) {
            addRow(detailPanel, "Weight records", a.getWeightHistory().size() + " entries");
            Button btn = new Button("View weight history (" + a.getWeightHistory().size() + " records)");
            btn.getStyleClass().add("btn-secondary");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showHistoryDialog("Weight History — " + a.getName(),
                a.getWeightHistory().stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toList())));
            detailPanel.getChildren().add(btn);
        }

        // Unresolved health events
        List<HealthEvent> unresolved = a.getUnresolvedEvents();
        if (!unresolved.isEmpty()) {
            addSectionTitle(detailPanel, "Unresolved events");
            for (HealthEvent e : unresolved)
                addRow(detailPanel, e.getDate().toLocalDate().toString(),
                    e.getEventType() + " — " + e.getDescription());
        }

        // Health history
        if (!a.getHealthHistory().isEmpty()) {
            addRow(detailPanel, "Health events", a.getHealthHistory().size() + " recorded");
            Button btn = new Button("View health history (" + a.getHealthHistory().size() + " events)");
            btn.getStyleClass().add("btn-secondary");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(ev -> showHistoryDialog("Health History — " + a.getName(),
                a.getHealthHistory().stream()
                    .map(e -> String.format("[%s]  %s → %s  |  %s%s",
                        e.getDate().toLocalDate(), e.getStatusBefore(),
                        e.getEventType(), e.getDescription(),
                        e.isResolved() ? "  ✓" : ""))
                    .collect(java.util.stream.Collectors.toList())));
            detailPanel.getChildren().add(btn);
        }
    }

    // ── Sensors tab ───────────────────────────────────────────────────

    private void buildSensorsTab(Animal a) {
        if (!a.getBioSensors().isEmpty()) {
            addSectionTitle(sensorsPanel, "Bio sensors");
            for (BioSensor s : a.getBioSensors()) {
                boolean alert = s.isAnimalInDistress();

                // Sensor name + reading row
                Label typeLbl = new Label(s.getMeasureType().toString());
                typeLbl.getStyleClass().add("animals-sensor-name");
                Label readingLbl = new Label(String.format("%.2f %s", s.getLastValue(), s.getUnit()));
                readingLbl.getStyleClass().add(alert ? "animals-sensor-reading-alert" : "animals-sensor-reading-ok");
                Region sp1 = new Region();
                HBox.setHgrow(sp1, Priority.ALWAYS);
                HBox nameRow = new HBox(typeLbl, sp1, readingLbl);
                nameRow.setAlignment(Pos.CENTER_LEFT);

                // Range bar (ProgressBar styled via CSS)
                double range = s.getMaxThreshold() - s.getMinThreshold();
                double normalized = (range > 0)
                    ? Math.max(0, Math.min(1, (s.getLastValue() - s.getMinThreshold()) / range))
                    : 0;
                ProgressBar bar = new ProgressBar(normalized);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefHeight(6);
                bar.getStyleClass().add(alert ? "animals-range-bar-alert" : "animals-range-bar");

                // Min / status / max row
                Label minLbl = new Label(String.format("%.1f", s.getMinThreshold()));
                minLbl.getStyleClass().add("animals-sensor-range-lbl");
                Label statusLbl = new Label(alert ? "Out of range" : "Normal range");
                statusLbl.getStyleClass().add(alert ? "animals-sensor-status-alert" : "animals-sensor-status-ok");
                Label maxLbl = new Label(String.format("%.1f", s.getMaxThreshold()));
                maxLbl.getStyleClass().add("animals-sensor-range-lbl");
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS);
                HBox rangeRow = new HBox(minLbl, sp2, statusLbl, sp3, maxLbl);
                rangeRow.setAlignment(Pos.CENTER);

                VBox card = new VBox(8, nameRow, bar, rangeRow);
                card.getStyleClass().add(alert ? "animals-sensor-card-alert" : "animals-sensor-card");
                card.setPadding(new Insets(10, 12, 10, 12));
                sensorsPanel.getChildren().add(card);
            }
        }

        if (a.hasGPSCollar()) {
            addSectionTitle(sensorsPanel, "GPS collar");
            GPSCollarSensor gps = a.getGpsCollarSensor();

            Label gpsLbl = new Label("GPS collar");
            gpsLbl.getStyleClass().add("animals-sensor-name");
            Label activeLbl = new Label(gps.hasEscaped() ? "⚠ Outside zone" : "● Active");
            activeLbl.getStyleClass().add(gps.hasEscaped() ? "animals-sensor-reading-alert" : "animals-gps-active");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            HBox gpsRow = new HBox(gpsLbl, sp, activeLbl);
            gpsRow.setAlignment(Pos.CENTER_LEFT);

            Label codeLbl = new Label("Code");
            codeLbl.getStyleClass().add("detail-key");
            Label codeVal = new Label(gps.getCode());
            codeVal.getStyleClass().add("animals-gps-code");
            HBox.setHgrow(codeVal, Priority.ALWAYS);
            HBox codeRow = new HBox(codeLbl, codeVal);
            codeRow.getStyleClass().add("detail-row");

            double lat = gps.getCurrentLatitude(), lon = gps.getCurrentLongitude();
            VBox card = new VBox(8, gpsRow, codeRow);
            if (lat != 0.0 || lon != 0.0) {
                Label posLbl = new Label(String.format("%.5f°, %.5f°", lat, lon));
                posLbl.getStyleClass().add("animals-sensor-range-lbl");
                card.getChildren().add(posLbl);
            }
            card.getStyleClass().add("animals-sensor-card");
            card.setPadding(new Insets(10, 12, 10, 12));
            sensorsPanel.getChildren().add(card);
        }

        if (a.getBioSensors().isEmpty() && !a.hasGPSCollar()) {
            Label none = new Label("No sensors attached to this animal.");
            none.getStyleClass().add("text-muted");
            sensorsPanel.getChildren().add(none);
        }
    }

    // ── Actions tab ───────────────────────────────────────────────────

    private void buildActionsTab(Animal a) {
        addSectionTitle(actionsPanel, "Record data");

        Button wBtn = actionTile("📏 Record weight");
        wBtn.setOnAction(e -> showRecordWeightDialog(a));
        actionsPanel.getChildren().add(wBtn);

        if (a.getType() == LIvestockType.RUMINANT) {
            Button milkBtn = actionTile("🥛 Record milk yield");
            milkBtn.setOnAction(e -> showRecordMilkDialog(a));
            actionsPanel.getChildren().add(milkBtn);
        }

        if (a.getType() == LIvestockType.POULTRY) {
            Button eggBtn = actionTile("🥚 Record eggs");
            eggBtn.setOnAction(e -> showRecordEggDialog(a));
            actionsPanel.getChildren().add(eggBtn);
        }

        if (a.getMilkYieldLiters() > 0 || a.getEggCount() > 0) {
            Button resetBtn = actionTile("🔄 Reset production stats");
            resetBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Reset Production Stats");
                confirm.setHeaderText(null);
                confirm.setContentText("Reset milk/egg stats for " + a.getName() + "?");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) { animalService.resetProductionStats(a); showDetail(a); }
                });
            });
            actionsPanel.getChildren().add(resetBtn);
        }

        // Manage section
        actionsPanel.getChildren().add(new Separator());
        addSectionTitle(actionsPanel, "Manage");

        Button zoneMapBtn = actionTile("📍 View in zone boundaries");
        zoneMapBtn.setOnAction(e -> openZoneMap(a));
        actionsPanel.getChildren().add(zoneMapBtn);

        if (a.getHealthStatus() != AnimalHealthStatus.Healthy) {
            Button resolveBtn = actionTile("✅ Resolve health status");
            resolveBtn.setOnAction(e -> showResolveHealthDialog(a));
            actionsPanel.getChildren().add(resolveBtn);
        }

        if (!a.hasGPSCollar()) {
            Button attachBtn = actionTile("📡 Attach GPS collar");
            attachBtn.setOnAction(e -> {
                GPSCollarSensor gps = new GPSCollarSensor(a);
                a.attachGPSCollar(gps);
                if (a.getZone() instanceof LivestockZONE lz)
                    lz.addGpsCollarSensor(gps);
                FarmService.getInstance().autoSave();
                showDetail(a);
            });
            actionsPanel.getChildren().add(attachBtn);
        } else {
            Button removeGpsBtn = actionTile("📡 Remove GPS collar");
            removeGpsBtn.setOnAction(e -> {
                a.removeGPSCollar();
                FarmService.getInstance().autoSave();
                showDetail(a);
            });
            actionsPanel.getChildren().add(removeGpsBtn);
        }

        // Danger zone
        actionsPanel.getChildren().add(new Separator());
        Label dangerLbl = new Label("DANGER ZONE");
        dangerLbl.getStyleClass().add("animals-danger-section-label");
        actionsPanel.getChildren().add(dangerLbl);

        Button removeBtn = new Button("❌ Remove animal");
        removeBtn.getStyleClass().add("animals-action-tile-danger");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setAlignment(Pos.CENTER_LEFT);
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Animal");
            confirm.setHeaderText(null);
            confirm.setContentText("Permanently remove " + a.getName() + " from the farm?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    animalService.removeAnimal(a);
                    allAnimals.remove(a);
                    detailHeaderPanel.getChildren().clear();
                    detailPanel.getChildren().clear();
                    sensorsPanel.getChildren().clear();
                    actionsPanel.getChildren().clear();
                    Label placeholder = new Label("Select an animal to view details");
                    placeholder.getStyleClass().add("text-muted");
                    detailPanel.getChildren().add(placeholder);
                    refreshStats();
                }
            });
        });
        actionsPanel.getChildren().add(removeBtn);
    }

    // ── Zone map ──────────────────────────────────────────────────────

    private void openZoneMap(Animal a) {
        List<String> sheets = animalTable.getScene() == null
            ? new ArrayList<>()
            : new ArrayList<>(animalTable.getScene().getStylesheets());
        ZoneMapDialog dlg = new ZoneMapDialog(a.getZone(), sheets, ZoneMapDialog.key(a));
        if (animalTable.getScene() != null)
            dlg.initOwner(animalTable.getScene().getWindow());
        dlg.showAndWait();
    }

    // ── Action tile helper ────────────────────────────────────────────

    private Button actionTile(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("animals-action-tile");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    // ── Record dialogs (all @FXML handler targets — unchanged) ────────

    private void showRecordWeightDialog(Animal a) {
        TextField field = new TextField(String.format("%.1f", a.getWeight()));
        VBox form = new VBox(14, formGroup("New Weight (kg)", field));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Record Weight");
        dialog.setHeaderText("Update weight for " + a.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(field.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(w -> { if (w > 0) { animalService.recordWeight(a, w); showDetail(a); } });
    }

    private void showRecordMilkDialog(Animal a) {
        TextField field = new TextField("0.0");
        VBox form = new VBox(14, formGroup("Liters to add", field));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Record Milk Yield");
        dialog.setHeaderText("Record milk yield for " + a.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(field.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(liters -> {
            if (liters >= 0) { animalService.recordMilkYield(a, liters); showDetail(a); }
        });
    }

    private void showRecordEggDialog(Animal a) {
        TextField field = new TextField("0");
        VBox form = new VBox(14, formGroup("Eggs to add", field));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Record Eggs");
        dialog.setHeaderText("Record egg count for " + a.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Integer.parseInt(field.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(count -> {
            if (count >= 0) { animalService.recordEgg(a, count); showDetail(a); }
        });
    }

    private void showResolveHealthDialog(Animal a) {
        ComboBox<AnimalHealthStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(AnimalHealthStatus.values());
        statusCombo.setValue(AnimalHealthStatus.Healthy);
        VBox form = new VBox(14, formGroup("New Health Status", statusCombo));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<AnimalHealthStatus> dialog = new Dialog<>();
        dialog.setTitle("Resolve Health Event");
        dialog.setHeaderText("Resolve event for " + a.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Resolve");
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? statusCombo.getValue() : null);
        dialog.showAndWait().ifPresent(status -> {
            if (!a.getUnresolvedEvents().isEmpty()) {
                animalService.resolveHealthEvent(a, status);
            } else {
                HealthEvent recovery = new HealthEvent(a, status, a.getHealthStatus(), status,
                    "Status changed to " + status);
                a.addHealthEvent(recovery);
                FarmService.getInstance().autoSave();
            }
            allAnimals.setAll(animalService.getAllAnimals());
            refreshStats();
            showDetail(a);
        });
    }

    private void showMarkHealthDialog(Animal a, AnimalHealthStatus newStatus) {
        TextField descField = new TextField();
        descField.setPromptText("Reason / description...");
        VBox form = new VBox(14, formGroup("Reason", descField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(newStatus == AnimalHealthStatus.Sick ? "Mark as Sick" : "Quarantine Animal");
        dialog.setHeaderText(a.getName() + " — " + newStatus);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Confirm");
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? descField.getText().trim() : null);
        dialog.showAndWait().ifPresent(desc -> {
            HealthEvent event = new HealthEvent(a, newStatus, a.getHealthStatus(), null,
                desc.isEmpty() ? "No description" : desc);
            a.addHealthEvent(event);
            FarmService.getInstance().autoSave();
            allAnimals.setAll(animalService.getAllAnimals());
            refreshStats();
            showDetail(a);
        });
    }

    private void showHistoryDialog(String title, List<String> entries) {
        ListView<String> list = new ListView<>();
        list.getItems().addAll(entries);
        list.getStyleClass().add("event-list");
        list.setPrefHeight(Math.min(400, entries.size() * 26.0 + 30));
        list.setPrefWidth(460);
        VBox content = new VBox(list);
        content.setPadding(new Insets(12, 16, 8, 16));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        if (animalTable.getScene() != null)
            dialog.initOwner(animalTable.getScene().getWindow());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(500);
        applyDialogStyle(dialog);
        dialog.showAndWait();
    }

    // ── Detail panel helpers ──────────────────────────────────────────

    private void addRow(VBox panel, String key, String value) {
        Label keyLbl = new Label(key);
        keyLbl.getStyleClass().add("detail-key");
        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("detail-value");
        valLbl.setWrapText(true);
        HBox row = new HBox(keyLbl, valLbl);
        row.getStyleClass().add("detail-row");
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        panel.getChildren().add(row);
    }

    private void addSectionTitle(VBox panel, String title) {
        if (!panel.getChildren().isEmpty()) {
            Separator sep = new Separator();
            sep.setPrefHeight(1);
            panel.getChildren().add(sep);
        }
        Label lbl = new Label(title.toUpperCase());
        lbl.getStyleClass().add("detail-section-title");
        lbl.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().add(lbl);
    }

    // ── Dialog helpers ────────────────────────────────────────────────

    private VBox formGroup(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("dialog-form-label");
        if (input instanceof TextField tf) {
            tf.getStyleClass().setAll("dialog-form-field");
            tf.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof ComboBox<?> cb) {
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.getStyleClass().setAll("combo-box", "dialog-form-combo");
        }
        VBox box = new VBox(6, lbl, input);
        return box;
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        var sheets = animalTable.getScene() == null ? null : animalTable.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0)
            : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);
        Button ok     = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (ok != null)     ok.getStyleClass().add("btn-primary");
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");

        String title = dialog.getTitle() == null ? "" : dialog.getTitle();
        String icon  = "🐄";
        if      (title.contains("Weight"))  icon = "⚖";
        else if (title.contains("Milk"))    icon = "🥛";
        else if (title.contains("Egg"))     icon = "🥚";
        else if (title.contains("Resolve")) icon = "✅";
        else if (title.contains("Sick") || title.contains("Quarantine")) icon = "🩺";
        else if (title.contains("Events"))  icon = "📋";
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        dialog.setGraphic(iconLbl);
    }
}
