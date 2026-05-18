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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class AnimalsController {

    @FXML private TableView<Animal>            animalTable;
    @FXML private TableColumn<Animal, String>  colId;
    @FXML private TableColumn<Animal, String>  colName;
    @FXML private TableColumn<Animal, String>  colSpecies;
    @FXML private TableColumn<Animal, String>  colType;
    @FXML private TableColumn<Animal, String>  colZone;
    @FXML private TableColumn<Animal, String>  colHealth;

    @FXML private ComboBox<String> filterZone;
    @FXML private ComboBox<String> filterHealth;
    @FXML private TextField        searchField;
    @FXML private VBox             detailPanel;

    @FXML private Label statTotalAnimals;
    @FXML private Label statHealthy;
    @FXML private Label statSick;
    @FXML private Label statQuarantined;

    private ObservableList<Animal> allAnimals;
    private FilteredList<Animal>   filteredAnimals;

    private final AnimalService animalService = AnimalService.getInstance();
    private final ZoneService   zoneService   = ZoneService.getInstance();

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

    // ── Table ─────────────────────────────────────────────────────────

    private void setupTable() {
        colId.setCellValueFactory(d      -> new SimpleStringProperty(d.getValue().getId().substring(0, 8)));
        colName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        colSpecies.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSpecies()));
        colType.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getType().toString()));
        colZone.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getZone().getName()));

        colHealth.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override
            protected void updateItem(String item, boolean empty) {
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

    // ── Filters ───────────────────────────────────────────────────────

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

    // ── Add Animal dialog ─────────────────────────────────────────────

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
            formGroup("Name", nameField),
            formGroup("Species", speciesField),
            formGroup("Type", typeCombo),
            formGroup("Age (years)", ageField),
            formGroup("Weight (kg)", weightField),
            formGroup("Zone", zoneCombo)
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

    // ── Detail panel ─────────────────────────────────────────────────

    private void showDetail(Animal a) {
        detailPanel.getChildren().clear();
        addRow("ID",      a.getId().substring(0, 8));
        addRow("Name",    a.getName());
        addRow("Species", a.getSpecies());
        addRow("Type",    a.getType().toString());
        addRow("Age",     a.getAge() + " years");
        addRow("Weight",  String.format("%.1f kg", a.getWeight()));
        addRow("Health",  a.getHealthStatus().toString());

        if (a.isSick()) {
            Label badge = new Label("🤒 SICK — monitor closely");
            badge.getStyleClass().addAll("badge", "badge-sick");
            detailPanel.getChildren().add(badge);
        } else if (a.isQuarantined()) {
            Label badge = new Label("🔒 QUARANTINED — isolated from herd");
            badge.getStyleClass().addAll("badge", "badge-quarantined");
            detailPanel.getChildren().add(badge);
        }

        if (a.getHealthStatus() == AnimalHealthStatus.Healthy) {
            HBox healthBtns = new HBox(8);
            Button sickBtn = new Button("🤒 Mark as Sick");
            sickBtn.getStyleClass().add("btn-secondary");
            sickBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(sickBtn, Priority.ALWAYS);
            sickBtn.setOnAction(e -> showMarkHealthDialog(a, AnimalHealthStatus.Sick));
            Button quarantineBtn = new Button("🔒 Quarantine");
            quarantineBtn.getStyleClass().add("btn-secondary");
            quarantineBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(quarantineBtn, Priority.ALWAYS);
            quarantineBtn.setOnAction(e -> showMarkHealthDialog(a, AnimalHealthStatus.Quarantined));
            healthBtns.getChildren().addAll(sickBtn, quarantineBtn);
            detailPanel.getChildren().add(healthBtns);
        }

        addRow("Zone",    a.getZone().getName());

        // ── Production stats ───────────────────────────────────────────
        if (a.getMilkYieldLiters() > 0) {
            addRow("Milk Total", String.format("%.2f L  (%d records)", a.getMilkYieldLiters(), a.getMilkHistory().size()));
            if (!a.getMilkHistory().isEmpty()) {
                Button milkHistBtn = new Button("View Milk History (" + a.getMilkHistory().size() + " records)");
                milkHistBtn.getStyleClass().add("btn-secondary");
                milkHistBtn.setMaxWidth(Double.MAX_VALUE);
                milkHistBtn.setOnAction(e -> showHistoryDialog("Milk History — " + a.getName(),
                    a.getMilkHistory().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.toList())));
                detailPanel.getChildren().add(milkHistBtn);
            }
        }
        if (a.getEggCount() > 0) {
            addRow("Egg Total", String.format("%d eggs  (%d records)", a.getEggCount(), a.getEggHistory().size()));
            if (!a.getEggHistory().isEmpty()) {
                Button eggHistBtn = new Button("View Egg History (" + a.getEggHistory().size() + " records)");
                eggHistBtn.getStyleClass().add("btn-secondary");
                eggHistBtn.setMaxWidth(Double.MAX_VALUE);
                eggHistBtn.setOnAction(e -> showHistoryDialog("Egg History — " + a.getName(),
                    a.getEggHistory().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.toList())));
                detailPanel.getChildren().add(eggHistBtn);
            }
        }

        // ── Weight history button ──────────────────────────────────────
        if (a.getWeightHistory().size() > 1) {
            addRow("Weight Records", a.getWeightHistory().size() + " entries");
            Button wHistBtn = new Button("View Weight History (" + a.getWeightHistory().size() + " records)");
            wHistBtn.getStyleClass().add("btn-secondary");
            wHistBtn.setMaxWidth(Double.MAX_VALUE);
            wHistBtn.setOnAction(e -> showHistoryDialog("Weight History — " + a.getName(),
                a.getWeightHistory().stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toList())));
            detailPanel.getChildren().add(wHistBtn);
        }

        // ── Bio sensors ───────────────────────────────────────────────
        if (!a.getBioSensors().isEmpty()) {
            addSectionTitle("Bio Sensors");
            for (BioSensor s : a.getBioSensors()) {
                String distress = s.isAnimalInDistress() ? " ⚠ DISTRESS" : "";
                addRow(s.getMeasureType().toString(),
                    String.format("%.2f %s  [%.1f – %.1f]%s",
                        s.getLastValue(), s.getUnit(),
                        s.getMinThreshold(), s.getMaxThreshold(), distress));
            }
        }

        if (a.hasGPSCollar()) {
            String gpsInfo = "code " + a.getGpsCollarSensor().getCode();
            if (a.getGpsCollarSensor().hasEscaped()) gpsInfo += "  ⚠ OUTSIDE ZONE";
            addRow("GPS Collar", gpsInfo);
        }

        // ── Unresolved health events ───────────────────────────────────
        List<HealthEvent> unresolved = a.getUnresolvedEvents();
        if (!unresolved.isEmpty()) {
            addSectionTitle("Unresolved Events");
            for (HealthEvent e : unresolved)
                addRow(e.getDate().toLocalDate().toString(),
                    e.getEventType() + " — " + e.getDescription());
        }

        // ── Health history button ──────────────────────────────────────
        if (!a.getHealthHistory().isEmpty()) {
            addRow("Health Events", a.getHealthHistory().size() + " recorded");
            Button healthHistBtn = new Button("View Health History (" + a.getHealthHistory().size() + " events)");
            healthHistBtn.getStyleClass().add("btn-secondary");
            healthHistBtn.setMaxWidth(Double.MAX_VALUE);
            healthHistBtn.setOnAction(ev -> showHistoryDialog("Health History — " + a.getName(),
                a.getHealthHistory().stream()
                    .map(e -> String.format("[%s]  %s → %s  |  %s%s",
                        e.getDate().toLocalDate(), e.getStatusBefore(),
                        e.getEventType(), e.getDescription(),
                        e.isResolved() ? "  ✓" : ""))
                    .collect(java.util.stream.Collectors.toList())));
            detailPanel.getChildren().add(healthHistBtn);
        }

        // ── Actions ───────────────────────────────────────────────────
        addSectionTitle("Actions");

        Button wBtn = new Button("📏 Record Weight");
        wBtn.getStyleClass().add("btn-secondary");
        wBtn.setMaxWidth(Double.MAX_VALUE);
        wBtn.setOnAction(e -> showRecordWeightDialog(a));
        detailPanel.getChildren().add(wBtn);

        if (a.getType() == LIvestockType.RUMINANT) {
            Button milkBtn = new Button("🥛 Record Milk Yield");
            milkBtn.getStyleClass().add("btn-secondary");
            milkBtn.setMaxWidth(Double.MAX_VALUE);
            milkBtn.setOnAction(e -> showRecordMilkDialog(a));
            detailPanel.getChildren().add(milkBtn);
        }

        if (a.getType() == LIvestockType.POULTRY) {
            Button eggBtn = new Button("🥚 Record Eggs");
            eggBtn.getStyleClass().add("btn-secondary");
            eggBtn.setMaxWidth(Double.MAX_VALUE);
            eggBtn.setOnAction(e -> showRecordEggDialog(a));
            detailPanel.getChildren().add(eggBtn);
        }

        if (a.getMilkYieldLiters() > 0 || a.getEggCount() > 0) {
            Button resetBtn = new Button("🔄 Reset Production Stats");
            resetBtn.getStyleClass().add("btn-secondary");
            resetBtn.setMaxWidth(Double.MAX_VALUE);
            resetBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Reset Production Stats");
                confirm.setHeaderText(null);
                confirm.setContentText("Reset milk/egg stats for " + a.getName() + "?");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) { animalService.resetProductionStats(a); showDetail(a); }
                });
            });
            detailPanel.getChildren().add(resetBtn);
        }

        if (a.getHealthStatus() != AnimalHealthStatus.Healthy) {
            Button resolveBtn = new Button("✅ Resolve Health Status");
            resolveBtn.getStyleClass().add("btn-secondary");
            resolveBtn.setMaxWidth(Double.MAX_VALUE);
            resolveBtn.setOnAction(e -> showResolveHealthDialog(a));
            detailPanel.getChildren().add(resolveBtn);
        }

        if (!a.hasGPSCollar()) {
            Button attachGpsBtn = new Button("📡 Attach GPS Collar");
            attachGpsBtn.getStyleClass().add("btn-secondary");
            attachGpsBtn.setMaxWidth(Double.MAX_VALUE);
            attachGpsBtn.setOnAction(e -> {
                GPSCollarSensor gps = new GPSCollarSensor(a);
                a.attachGPSCollar(gps);
                if (a.getZone() instanceof ZONES.LivestockZONE lz)
                    lz.addGpsCollarSensor(gps);
                FarmService.getInstance().autoSave();
                showDetail(a);
            });
            detailPanel.getChildren().add(attachGpsBtn);
        } else {
            Button removeGpsBtn = new Button("📡 Remove GPS Collar");
            removeGpsBtn.getStyleClass().add("btn-secondary");
            removeGpsBtn.setMaxWidth(Double.MAX_VALUE);
            removeGpsBtn.setOnAction(e -> {
                a.removeGPSCollar();
                FarmService.getInstance().autoSave();
                showDetail(a);
            });
            detailPanel.getChildren().add(removeGpsBtn);
        }

        Button removeBtn = new Button("❌ Remove Animal");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Animal");
            confirm.setHeaderText(null);
            confirm.setContentText("Permanently remove " + a.getName() + " from the farm?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    animalService.removeAnimal(a);
                    allAnimals.remove(a);
                    detailPanel.getChildren().clear();
                    refreshStats();
                }
            });
        });
        detailPanel.getChildren().add(removeBtn);
    }

    // ── Action dialogs ────────────────────────────────────────────────

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
        dialog.showAndWait().ifPresent(liters -> { if (liters >= 0) { animalService.recordMilkYield(a, liters); showDetail(a); } });
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
        dialog.showAndWait().ifPresent(count -> { if (count >= 0) { animalService.recordEgg(a, count); showDetail(a); } });
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
        // Must set owner so the dialog appears in front of the main window
        if (animalTable.getScene() != null)
            dialog.initOwner(animalTable.getScene().getWindow());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(500);
        applyDialogStyle(dialog);
        dialog.showAndWait();
    }

    private void addRow(String key, String value) {
        Label keyLbl = new Label(key);
        keyLbl.getStyleClass().add("detail-key");
        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("detail-value");
        valLbl.setWrapText(true);
        HBox row = new HBox(keyLbl, valLbl);
        row.getStyleClass().add("detail-row");
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        detailPanel.getChildren().add(row);
    }

    private void addSectionTitle(String title) {
        if (!detailPanel.getChildren().isEmpty()) {
            Separator sep = new Separator();
            sep.setPrefHeight(2);
            detailPanel.getChildren().add(sep);
        }
        Label lbl = new Label(title.toUpperCase());
        lbl.getStyleClass().add("detail-section-title");
        lbl.setMaxWidth(Double.MAX_VALUE);
        detailPanel.getChildren().add(lbl);
    }

    // ── Dialog helpers ────────────────────────────────────────────────

    private VBox formGroup(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("dialog-form-label");
        if (input instanceof TextField tf) {
            tf.getStyleClass().setAll("dialog-form-field");
            tf.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof ComboBox<?> cb) cb.setMaxWidth(Double.MAX_VALUE);
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
    }
}
