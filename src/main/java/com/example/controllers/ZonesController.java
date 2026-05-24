package com.example.controllers;

import Additional_classes.Range;
import Animals.Animal;
import Animals.FeedingProgram;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Entities.CropType;
import Entities.GrowthStage;
import Entities.LIvestockType;
import Sensors.BioMeasureType;
import Sensors.BioSensor;
import Sensors.GPSCollarSensor;
import Sensors.EnvMeasureType;
import Sensors.EnvSensor;
import Sensors.SoilMeasureType;
import Sensors.SoilSensor;
import Sensors.WaterMeasureType;
import Sensors.WaterSensor;
import com.example.services.FarmService;
import com.example.services.ZoneService;
import javafx.util.StringConverter;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import ZONES.ZoneStatus;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class ZonesController {

    @FXML private TableView<LivestockZONE>     livestockTable;
    @FXML private TableColumn<LivestockZONE, String>  lsColCode;
    @FXML private TableColumn<LivestockZONE, String>  lsColName;
    @FXML private TableColumn<LivestockZONE, String>  lsColType;
    @FXML private TableColumn<LivestockZONE, Integer> lsColAnimals;
    @FXML private TableColumn<LivestockZONE, String>  lsColStatus;

    @FXML private TableView<CropZONE>          cropTable;
    @FXML private TableColumn<CropZONE, String>  crColCode;
    @FXML private TableColumn<CropZONE, String>  crColName;
    @FXML private TableColumn<CropZONE, Integer> crColFields;
    @FXML private TableColumn<CropZONE, Double>  crColSurface;
    @FXML private TableColumn<CropZONE, String>  crColStatus;

    @FXML private TableView<AquacultureZONE>   aquaTable;
    @FXML private TableColumn<AquacultureZONE, String>  aqColCode;
    @FXML private TableColumn<AquacultureZONE, String>  aqColName;
    @FXML private TableColumn<AquacultureZONE, Integer> aqColSpecies;
    @FXML private TableColumn<AquacultureZONE, String>  aqColStatus;

    @FXML private VBox      detailPanel;
    @FXML private TextField searchField;

    @FXML private Label statTotalZones;
    @FXML private Label statActiveZones;
    @FXML private Label statLivestockCount;
    @FXML private Label statCropCount;
    @FXML private Label statAquaCount;

    // injected for distress banner — visibility wired separately
    @FXML private HBox  distressBanner;
    @FXML private Label distressBannerText;

    private final ZoneService zoneService = ZoneService.getInstance();

    private ObservableList<LivestockZONE>   lsData;
    private ObservableList<CropZONE>        crData;
    private ObservableList<AquacultureZONE> aquaData;

    @FXML
    public void initialize() {
        lsData   = FXCollections.observableArrayList(zoneService.getLivestockZones());
        crData   = FXCollections.observableArrayList(zoneService.getCropZones());
        aquaData = FXCollections.observableArrayList(zoneService.getAquacultureZones());

        setupLivestockTable(lsData);
        setupCropTable(crData);
        setupAquaTable(aquaData);
        refreshStats();
    }

    private void refreshStats() {
        statTotalZones.setText(String.valueOf(zoneService.getAllZones().size()));
        statActiveZones.setText(String.valueOf(zoneService.getActiveZoneCount()));
        statLivestockCount.setText(String.valueOf(zoneService.getLivestockZones().size()));
        statCropCount.setText(String.valueOf(zoneService.getCropZones().size()));
        statAquaCount.setText(String.valueOf(zoneService.getAquacultureZones().size()));
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupLivestockTable(ObservableList<LivestockZONE> data) {
        lsColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        lsColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        lsColType.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getType().toString()));
        lsColAnimals.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAnimals().size()).asObject());
        setupStatusColumn(lsColStatus, z -> z.getStatus());
        livestockTable.setItems(data);
        livestockTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showLivestockDetail(n); });
    }

    private void setupCropTable(ObservableList<CropZONE> data) {
        crColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        crColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        crColFields.setCellValueFactory(d  -> new SimpleIntegerProperty(d.getValue().getFields().size()).asObject());
        crColSurface.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getSurfacePlanted()).asObject());
        setupStatusColumn(crColStatus, z -> z.getStatus());
        cropTable.setItems(data);
        cropTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showCropDetail(n); });
    }

    private void setupAquaTable(ObservableList<AquacultureZONE> data) {
        aqColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        aqColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        aqColSpecies.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSpeciesList().size()).asObject());
        setupStatusColumn(aqColStatus, z -> z.getStatus());
        aquaTable.setItems(data);
        aquaTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showAquaDetail(n); });
    }

    private <T> void setupStatusColumn(TableColumn<T, String> col, Function<T, ZoneStatus> statusFn) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @SuppressWarnings("unchecked")
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                ZoneStatus status = statusFn.apply((T) getTableRow().getItem());
                badge.setText(status.toString());
                badge.getStyleClass().removeIf(s -> s.startsWith("badge-"));
                badge.getStyleClass().add(status == ZoneStatus.ACTIVE ? "badge-active" : "badge-suspended");
                setGraphic(badge);
            }
        });
    }

    // ── Add Zone dialog ───────────────────────────────────────────────

    @FXML
    private void showAddZoneDialog() {
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Livestock", "Crop", "Aquaculture");
        typeCombo.setValue("Livestock");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. North Pasture");

        ComboBox<String> lstTypeCombo = new ComboBox<>();
        lstTypeCombo.getItems().addAll("RUMINANT", "POULTRY");
        lstTypeCombo.setValue("RUMINANT");

        VBox lstTypeGroup = formGroup("Livestock Type", lstTypeCombo);
        typeCombo.setOnAction(e -> {
            boolean isLs = "Livestock".equals(typeCombo.getValue());
            lstTypeGroup.setVisible(isLs);
            lstTypeGroup.setManaged(isLs);
        });

        VBox form = new VBox(16,
            formGroup("Zone Type", typeCombo),
            formGroup("Zone Name", nameField),
            lstTypeGroup
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<ZONE> dialog = new Dialog<>();
        dialog.setTitle("Add Zone");
        dialog.setHeaderText("Add a new zone to the farm");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(420);
        applyDialogStyle(dialog);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add Zone");
        okBtn.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String name = nameField.getText().trim();
            return switch (typeCombo.getValue()) {
                case "Livestock" -> new LivestockZONE(name, LIvestockType.valueOf(lstTypeCombo.getValue()));
                case "Crop"      -> new CropZONE(name);
                default          -> new AquacultureZONE(name);
            };
        });

        dialog.showAndWait().ifPresent(zone -> {
            zoneService.addZone(zone);
            reloadTables();
            refreshStats();
        });
    }

    // ── Add Crop dialog ───────────────────────────────────────────────

    private void showAddCropDialog(CropZONE zone) {
        TextField varietyField = new TextField();
        varietyField.setPromptText("e.g. Golden Wheat");

        ComboBox<CropType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(CropType.values());
        typeCombo.setValue(CropType.cereals);

        TextField weeksField = new TextField("12");

        VBox form = new VBox(16,
            formGroup("Variety", varietyField),
            formGroup("Crop Type", typeCombo),
            formGroup("Weeks to Harvest", weeksField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Crop> dialog = new Dialog<>();
        dialog.setTitle("Add Crop");
        dialog.setHeaderText("Add crop to \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogStyle(dialog);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add Crop");
        okBtn.setDisable(true);
        varietyField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String variety = varietyField.getText().trim();
                CropType type  = typeCombo.getValue();
                int weeks      = Math.max(1, Integer.parseInt(weeksField.getText().trim()));
                Date now       = new Date();
                Date harvest   = new Date(now.getTime() + (long) weeks * 7 * 24 * 3600 * 1000L);
                return new Crop(type, variety, now, harvest,
                    new Range(6.0, 7.5), new Range(30.0, 70.0), zone);
            } catch (NumberFormatException e) { return null; }
        });

        dialog.showAndWait().ifPresent(crop -> {
            zone.addField(crop);
            FarmService.getInstance().autoSave();
            showCropDetail(zone);
            crData.setAll(zoneService.getCropZones());
        });
    }

    // ── Add Species dialog ────────────────────────────────────────────

    private void showAddSpeciesDialog(AquacultureZONE zone) {
        TextField nameField  = new TextField();
        nameField.setPromptText("e.g. Tilapia, Salmon");

        TextField countField = new TextField("100");

        VBox form = new VBox(16,
            formGroup("Species Name", nameField),
            formGroup("Initial Count", countField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<AquacultureSpecies> dialog = new Dialog<>();
        dialog.setTitle("Add Species");
        dialog.setHeaderText("Add species to \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogStyle(dialog);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add Species");
        okBtn.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String name = nameField.getText().trim();
                int count   = Math.max(1, Integer.parseInt(countField.getText().trim()));
                return new AquacultureSpecies(name, count, zone);
            } catch (NumberFormatException e) { return null; }
        });

        dialog.showAndWait().ifPresent(species -> {
            zone.addSpecies(species);
            FarmService.getInstance().autoSave();
            showAquaDetail(zone);
            aquaData.setAll(zoneService.getAquacultureZones());
        });
    }

    // ── Feeding Program dialog ────────────────────────────────────────

    private void showCreateFeedingProgramDialog(LivestockZONE zone) {
        TextField foodField     = new TextField();  foodField.setPromptText("e.g. Hay, Grain Mix");
        TextField quantityField = new TextField("50.0");
        TextField scheduleField = new TextField("08:00,12:00,18:00");
        scheduleField.setPromptText("Comma-separated HH:mm times");
        TextField wakeField     = new TextField("06:00");
        TextField sleepField    = new TextField("20:00");

        VBox form = new VBox(14,
            formGroup("Food Type", foodField),
            formGroup("Quantity per day (kg)", quantityField),
            formGroup("Schedule (HH:mm, comma-separated)", scheduleField),
            formGroup("Wake-up Time (HH:mm)", wakeField),
            formGroup("Sleep Time (HH:mm)", sleepField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<FeedingProgram> dialog = new Dialog<>();
        dialog.setTitle("Create Feeding Program");
        dialog.setHeaderText("Feeding program for \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(440);
        applyDialogStyle(dialog);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Create");
        okBtn.setDisable(true);
        foodField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String food     = foodField.getText().trim();
                double qty      = Double.parseDouble(quantityField.getText().trim());
                List<String> sched = Arrays.asList(scheduleField.getText().trim().split("\\s*,\\s*"));
                LocalTime wake  = LocalTime.parse(wakeField.getText().trim());
                LocalTime sleep = LocalTime.parse(sleepField.getText().trim());
                return new FeedingProgram(food, qty, sched, wake, sleep);
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input");
                err.setHeaderText(null);
                err.setContentText("Check your times (HH:mm) and that wake < sleep.\n" + e.getMessage());
                err.showAndWait();
                return null;
            }
        });

        dialog.showAndWait().ifPresent(fp -> {
            zoneService.setFeedingProgram(zone, fp);
            showLivestockDetail(zone);
        });
    }

    // ── Detail panels ─────────────────────────────────────────────────

    private void showLivestockDetail(LivestockZONE z) {
        detailPanel.getChildren().clear();
        addRow("Code",        z.getCode());
        addRow("Name",        z.getName());
        addRow("Type",        z.getType().toString());
        addRow("Status",      z.getStatus().toString());
        addRow("Animals",     String.valueOf(z.getAnimals().size()));
        addRow("Milk Yield",  String.format("%.2f L", z.getTotalMilkYield()));
        if (z.getTotalEggCount() > 0)
            addRow("Total Eggs", String.valueOf(z.getTotalEggCount()));
        if (z.hasBoundaries())
            addRow("Boundaries", z.getBoundaries().size() + " points");

        // Bio sensors — individual rows with remove
        if (!z.getBioSensors().isEmpty()) {
            addSectionTitle("Bio Sensors (" + z.getBioSensors().size() + ")");
            for (BioSensor sensor : new java.util.ArrayList<>(z.getBioSensors())) {
                String info = sensor.getCode() + "  " + sensor.getMeasureType()
                    + " on " + sensor.getAnimal().getName()
                    + (sensor.isAnimalInDistress() ? "  ⚠ DISTRESS" : "");
                Label lbl = new Label(info);
                lbl.getStyleClass().add("detail-value");
                lbl.setWrapText(true);
                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("btn-danger");
                removeBtn.setOnAction(e -> {
                    z.removeBioSensor(sensor);
                    FarmService.getInstance().autoSave();
                    showLivestockDetail(z);
                });
                HBox row = new HBox(8, lbl, removeBtn);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(lbl, Priority.ALWAYS);
                detailPanel.getChildren().add(row);
            }
        } else {
            addRow("Bio Sensors", "0");
        }

        // GPS collar sensors — individual rows with remove
        if (!z.getGpsCollarSensors().isEmpty()) {
            addSectionTitle("GPS Collar Sensors (" + z.getGpsCollarSensors().size() + ")");
            for (GPSCollarSensor sensor : new java.util.ArrayList<>(z.getGpsCollarSensors())) {
                String info = sensor.getCode() + "  on " + sensor.getAnimal().getName()
                    + (sensor.hasEscaped() ? "  ⚠ OUTSIDE ZONE" : "");
                Label lbl = new Label(info);
                lbl.getStyleClass().add("detail-value");
                lbl.setWrapText(true);
                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("btn-danger");
                removeBtn.setOnAction(e -> {
                    sensor.getAnimal().removeGPSCollar();
                    z.removeGpsCollarSensor(sensor);
                    FarmService.getInstance().autoSave();
                    showLivestockDetail(z);
                });
                HBox row = new HBox(8, lbl, removeBtn);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(lbl, Priority.ALWAYS);
                detailPanel.getChildren().add(row);
            }
        } else {
            addRow("GPS Sensors", "0");
        }

        // Feeding Program section
        FeedingProgram fp = z.getFeedingProgram();
        if (fp != null) {
            addSectionTitle("Feeding Program");
            addRow("Food Type",    fp.getFoodType());
            addRow("Quantity",     fp.getQuantity() + " kg/day");
            addRow("Times/Day",    String.valueOf(fp.getTimesPerDay()));
            addRow("Schedule",     fp.getSchedule().toString());
            String status = fp.isOverdue() ? "⚠ OVERDUE" : fp.isDue() ? "⏰ DUE NOW" : "On schedule";
            addRow("Status",      status);
            addRow("Wake up",     fp.getWakeUpTime().toString());
            addRow("Sleep",       fp.getSleepTime().toString());
            addRow("Last fed",    fp.getLastFedTime() != null
                ? fp.getLastFedTime().toString().substring(0, 16) : "Never");
            long mins = fp.minutesUntilNextFeeding();
            if (mins > 0)
                addRow("Next Feeding", "in " + fp.hoursUntilNextFeeding() + "h " + (mins % 60) + "m");
            else
                addRow("Next Feeding", fp.getNextFeedingTime() != null
                    ? fp.getNextFeedingTime().toString().substring(0, 16) : "—");

            Button recordFeedBtn = new Button("🍽 Record Feeding Now");
            recordFeedBtn.getStyleClass().add("btn-primary");
            recordFeedBtn.setMaxWidth(Double.MAX_VALUE);
            recordFeedBtn.setOnAction(e -> { zoneService.recordFeeding(z); showLivestockDetail(z); });
            detailPanel.getChildren().add(recordFeedBtn);
        }

        // Animals list with Map + Remove buttons
        if (!z.getAnimals().isEmpty()) {
            addSectionTitle("Animals");
            for (Animal a : z.getAnimals()) {
                Label lbl = new Label(a.getName() + "  (" + a.getSpecies() + " · " + a.getHealthStatus() + ")");
                lbl.getStyleClass().add("detail-row");
                lbl.setWrapText(true);
                Button mapAnimalBtn = new Button("📍");
                mapAnimalBtn.getStyleClass().add("btn-secondary");
                mapAnimalBtn.setOnAction(e -> {
                    ZoneMapDialog mapDlg = new ZoneMapDialog(z,
                        detailPanel.getScene().getStylesheets(), ZoneMapDialog.key(a));
                    mapDlg.showAndWait();
                });
                Button removeAnimalBtn = new Button("✕");
                removeAnimalBtn.getStyleClass().add("btn-danger");
                removeAnimalBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Animal");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Remove " + a.getName() + " from " + z.getName() + "?");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            z.removeAnimal(a);
                            FarmService.getInstance().autoSave();
                            lsData.setAll(zoneService.getLivestockZones());
                            showLivestockDetail(z);
                        }
                    });
                });
                HBox row = new HBox(8, lbl, mapAnimalBtn, removeAnimalBtn);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
                detailPanel.getChildren().add(row);
            }
        }

        // Zone management actions
        addSectionTitle("Zone Actions");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = new Button("⏸ Suspend Zone");
            suspBtn.getStyleClass().add("btn-secondary");
            suspBtn.setMaxWidth(Double.MAX_VALUE);
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); lsData.setAll(zoneService.getLivestockZones()); showLivestockDetail(z); refreshStats(); });
            detailPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = new Button("▶ Activate Zone");
            actBtn.getStyleClass().add("btn-primary");
            actBtn.setMaxWidth(Double.MAX_VALUE);
            actBtn.setOnAction(e -> { zoneService.activateZone(z); lsData.setAll(zoneService.getLivestockZones()); showLivestockDetail(z); refreshStats(); });
            detailPanel.getChildren().add(actBtn);
        }

        if (fp == null) {
            Button fpBtn = new Button("🗓 Create Feeding Program");
            fpBtn.getStyleClass().add("btn-secondary");
            fpBtn.setMaxWidth(Double.MAX_VALUE);
            fpBtn.setOnAction(e -> showCreateFeedingProgramDialog(z));
            detailPanel.getChildren().add(fpBtn);
        } else {
            Button editFpBtn = new Button("✏ Edit Feeding Program");
            editFpBtn.getStyleClass().add("btn-secondary");
            editFpBtn.setMaxWidth(Double.MAX_VALUE);
            editFpBtn.setOnAction(e -> showEditFeedingProgramDialog(z, fp));
            detailPanel.getChildren().add(editFpBtn);
        }

        Button addBioBtn = new Button("📡 Add Bio Sensor");
        addBioBtn.getStyleClass().add("btn-secondary");
        addBioBtn.setMaxWidth(Double.MAX_VALUE);
        addBioBtn.setOnAction(e -> showAddBioSensorDialog(z));
        detailPanel.getChildren().add(addBioBtn);

        Button lsBoundaryBtn = new Button(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary");
        lsBoundaryBtn.getStyleClass().add("btn-secondary");
        lsBoundaryBtn.setMaxWidth(Double.MAX_VALUE);
        lsBoundaryBtn.setOnAction(e -> {
            BoundaryEditorDialog dlg = new BoundaryEditorDialog(z.getName(),
                z.hasBoundaries() ? z.getBoundaries() : null,
                detailPanel.getScene().getStylesheets());
            dlg.showAndWait().ifPresent(bounds -> {
                z.setBoundaries(bounds);
                FarmService.getInstance().autoSave();
                showLivestockDetail(z);
            });
        });
        detailPanel.getChildren().add(lsBoundaryBtn);

        Button lsMapBtn = new Button("📍 View Zone Map");
        lsMapBtn.getStyleClass().add("btn-secondary");
        lsMapBtn.setMaxWidth(Double.MAX_VALUE);
        lsMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(lsMapBtn);

        Button lsSensorHistBtn = new Button("📊 Sensor History");
        lsSensorHistBtn.getStyleClass().add("btn-secondary");
        lsSensorHistBtn.setMaxWidth(Double.MAX_VALUE);
        lsSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(lsSensorHistBtn);

        Button lsAlertHistBtn = new Button("🔔 Alert History");
        lsAlertHistBtn.getStyleClass().add("btn-secondary");
        lsAlertHistBtn.setMaxWidth(Double.MAX_VALUE);
        lsAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(lsAlertHistBtn);

        Button renameBtn = new Button("✏ Rename Zone");
        renameBtn.getStyleClass().add("btn-secondary");
        renameBtn.setMaxWidth(Double.MAX_VALUE);
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            lsData.setAll(zoneService.getLivestockZones());
            showLivestockDetail(z);
            refreshStats();
        }));
        detailPanel.getChildren().add(renameBtn);

        Button deleteBtn = new Button("🗑 Delete Zone");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> confirmDeleteZone(z, () -> {
            lsData.setAll(zoneService.getLivestockZones());
            detailPanel.getChildren().clear();
            refreshStats();
        }));
        detailPanel.getChildren().add(deleteBtn);
    }

    private void showCropDetail(CropZONE z) {
        detailPanel.getChildren().clear();
        addRow("Code",         z.getCode());
        addRow("Name",         z.getName());
        addRow("Status",       z.getStatus().toString());
        addRow("Surface",      z.getSurfacePlanted() + " ha");
        addRow("Fields",       String.valueOf(z.getFields().size()));
        addRow("Total Yield",  String.format("%.2f kg", z.getTotalCropYield()));
        addRow("Env Sensors",  String.valueOf(z.getEnvSensors().size()));
        addRow("Soil Sensors", String.valueOf(z.getSoilSensors().size()));

        // Per-crop management
        if (!z.getFields().isEmpty()) {
            addSectionTitle("Crops");
            for (Crop c : z.getFields()) {
                Label lbl = new Label(c.getVariety() + "  (" + c.getCropType() + " · " + c.getGrowthStage()
                    + (c.wasHarvested() ? String.format(" · %.1f kg", c.getYieldKg()) : "") + ")");
                lbl.getStyleClass().add("detail-row");
                lbl.setWrapText(true);
                detailPanel.getChildren().add(lbl);

                // crop metadata
                addRow("  ID",         c.getId().substring(0, 8));
                addRow("  Planted",    c.getPlantingDate().toString().substring(0, 10));
                addRow("  Harvest By", c.getExpectedHarvestDate().toString().substring(0, 10));
                if (c.wasHarvested() && c.getHarvestDate() != null)
                    addRow("  Harvested On", c.getHarvestDate().toString().substring(0, 10));
                addRow("  pH range",  c.getOptimalPHRange().getMin() + " – " + c.getOptimalPHRange().getMax());
                addRow("  Moisture",  c.getOptimalMoistureRange().getMin() + "% – " + c.getOptimalMoistureRange().getMax() + "%");

                // ready-to-harvest indicator
                if (c.isReadyForHarvest()) {
                    Label readyLbl = new Label("✅ READY TO HARVEST");
                    readyLbl.getStyleClass().addAll("badge", "badge-active");
                    detailPanel.getChildren().add(readyLbl);
                }

                // per-crop action buttons
                Button harvestBtn = new Button("🌾 Record Harvest");
                harvestBtn.getStyleClass().add("btn-secondary");
                harvestBtn.setOnAction(e -> showRecordCropHarvestDialog(c, z));

                Button stageBtn = new Button("🌱 Update Stage");
                stageBtn.getStyleClass().add("btn-secondary");
                stageBtn.setOnAction(e -> showUpdateGrowthStageDialog(c, z));

                Button fieldBoundaryBtn = new Button(c.hasBoundary()
                    ? "🗺 Field Boundary (" + c.getBoundary().size() + " pts)"
                    : "🗺 Set Field Boundary");
                fieldBoundaryBtn.getStyleClass().add("btn-secondary");
                fieldBoundaryBtn.setOnAction(e -> {
                    if (!z.hasBoundaries()) {
                        Alert warn = new Alert(Alert.AlertType.WARNING);
                        warn.setTitle("Zone Boundary Required");
                        warn.setHeaderText(null);
                        warn.setContentText("Set the zone boundary first — the field sub-zone must fit inside it.");
                        warn.showAndWait();
                        return;
                    }
                    BoundaryEditorDialog dlg = new BoundaryEditorDialog(
                        c.getVariety() + " field",
                        c.hasBoundary() ? c.getBoundary() : null,
                        z.getBoundaries(),
                        detailPanel.getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(bounds -> {
                        c.setBoundary(bounds);
                        FarmService.getInstance().autoSave();
                        showCropDetail(z);
                    });
                });

                Button removeCropBtn = new Button("✕");
                removeCropBtn.getStyleClass().add("btn-danger");
                removeCropBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Crop");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Remove " + c.getVariety() + " from " + z.getName() + "?");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            z.removeField(c);
                            FarmService.getInstance().autoSave();
                            crData.setAll(zoneService.getCropZones());
                            showCropDetail(z);
                        }
                    });
                });

                HBox cropActions = new HBox(8, harvestBtn, stageBtn, fieldBoundaryBtn, removeCropBtn);
                cropActions.setPadding(new Insets(2, 0, 8, 16));
                detailPanel.getChildren().add(cropActions);
            }
        }

        // Zone management actions
        addSectionTitle("Zone Actions");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = new Button("⏸ Suspend Zone");
            suspBtn.getStyleClass().add("btn-secondary");
            suspBtn.setMaxWidth(Double.MAX_VALUE);
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); crData.setAll(zoneService.getCropZones()); showCropDetail(z); refreshStats(); });
            detailPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = new Button("▶ Activate Zone");
            actBtn.getStyleClass().add("btn-primary");
            actBtn.setMaxWidth(Double.MAX_VALUE);
            actBtn.setOnAction(e -> { zoneService.activateZone(z); crData.setAll(zoneService.getCropZones()); showCropDetail(z); refreshStats(); });
            detailPanel.getChildren().add(actBtn);
        }

        Button addCropBtn = new Button("➕ Add Crop");
        addCropBtn.getStyleClass().add("btn-primary");
        addCropBtn.setMaxWidth(Double.MAX_VALUE);
        addCropBtn.setOnAction(e -> showAddCropDialog(z));
        detailPanel.getChildren().add(addCropBtn);

        Button addEnvBtn = new Button("🌡 Add Environment Sensor");
        addEnvBtn.getStyleClass().add("btn-secondary");
        addEnvBtn.setMaxWidth(Double.MAX_VALUE);
        addEnvBtn.setOnAction(e -> showAddEnvSensorDialog(z));
        detailPanel.getChildren().add(addEnvBtn);

        Button addSoilBtn = new Button("🌱 Add Soil Sensor");
        addSoilBtn.getStyleClass().add("btn-secondary");
        addSoilBtn.setMaxWidth(Double.MAX_VALUE);
        addSoilBtn.setOnAction(e -> showAddSoilSensorDialog(z));
        detailPanel.getChildren().add(addSoilBtn);

        Button setSurfaceBtn = new Button("📐 Set Surface Area");
        setSurfaceBtn.getStyleClass().add("btn-secondary");
        setSurfaceBtn.setMaxWidth(Double.MAX_VALUE);
        setSurfaceBtn.setOnAction(e -> {
            TextField surfField = new TextField(String.valueOf(z.getSurfacePlanted()));
            VBox form = new VBox(14, formGroup("Surface (hectares)", surfField));
            form.setPadding(new Insets(20, 24, 8, 24));
            Dialog<Double> d = new Dialog<>();
            d.setTitle("Set Surface Area");
            d.setHeaderText("Surface area for \"" + z.getName() + "\"");
            d.getDialogPane().setContent(form);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.getDialogPane().setMinWidth(360);
            applyDialogStyle(d);
            ((Button) d.getDialogPane().lookupButton(ButtonType.OK)).setText("Set");
            d.setResultConverter(bt -> {
                if (bt != ButtonType.OK) return null;
                try { return Double.parseDouble(surfField.getText().trim()); }
                catch (NumberFormatException ex) { return null; }
            });
            d.showAndWait().ifPresent(ha -> {
                if (ha >= 0) { z.setSurfacePlanted(ha); FarmService.getInstance().autoSave(); showCropDetail(z); crData.setAll(zoneService.getCropZones()); }
            });
        });
        detailPanel.getChildren().add(setSurfaceBtn);

        Button crBoundaryBtn = new Button(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary");
        crBoundaryBtn.getStyleClass().add("btn-secondary");
        crBoundaryBtn.setMaxWidth(Double.MAX_VALUE);
        crBoundaryBtn.setOnAction(e -> {
            BoundaryEditorDialog dlg = new BoundaryEditorDialog(z.getName(),
                z.hasBoundaries() ? z.getBoundaries() : null,
                detailPanel.getScene().getStylesheets());
            dlg.showAndWait().ifPresent(bounds -> {
                z.setBoundaries(bounds);
                FarmService.getInstance().autoSave();
                showCropDetail(z);
            });
        });
        detailPanel.getChildren().add(crBoundaryBtn);

        Button crMapBtn = new Button("📍 View Zone Map");
        crMapBtn.getStyleClass().add("btn-secondary");
        crMapBtn.setMaxWidth(Double.MAX_VALUE);
        crMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(crMapBtn);

        Button crSensorHistBtn = new Button("📊 Sensor History");
        crSensorHistBtn.getStyleClass().add("btn-secondary");
        crSensorHistBtn.setMaxWidth(Double.MAX_VALUE);
        crSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(crSensorHistBtn);

        Button crAlertHistBtn = new Button("🔔 Alert History");
        crAlertHistBtn.getStyleClass().add("btn-secondary");
        crAlertHistBtn.setMaxWidth(Double.MAX_VALUE);
        crAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(crAlertHistBtn);

        Button renameBtn = new Button("✏ Rename Zone");
        renameBtn.getStyleClass().add("btn-secondary");
        renameBtn.setMaxWidth(Double.MAX_VALUE);
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            crData.setAll(zoneService.getCropZones());
            showCropDetail(z);
            refreshStats();
        }));
        detailPanel.getChildren().add(renameBtn);

        Button deleteBtn = new Button("🗑 Delete Zone");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> confirmDeleteZone(z, () -> {
            crData.setAll(zoneService.getCropZones());
            detailPanel.getChildren().clear();
            refreshStats();
        }));
        detailPanel.getChildren().add(deleteBtn);
    }

    private void showAquaDetail(AquacultureZONE z) {
        detailPanel.getChildren().clear();
        addRow("Code",           z.getCode());
        addRow("Name",           z.getName());
        addRow("Status",         z.getStatus().toString());
        addRow("Species Groups", String.valueOf(z.getSpeciesList().size()));
        addRow("Total Fish",     String.valueOf(z.getTotalSpeciesCount()));
        addRow("Total Harvest",  String.format("%.2f kg", z.getTotalHarvestWeight()));
        addRow("Water Sensors",  String.valueOf(z.getWaterSensors().size()));

        // Per-species management
        if (!z.getSpeciesList().isEmpty()) {
            addSectionTitle("Species");
            for (AquacultureSpecies s : z.getSpeciesList()) {
                Label nameHdr = new Label(s.getName() + "  (" + s.getHarvestCount() + " harvest sessions)");
                nameHdr.getStyleClass().add("detail-section-title");
                nameHdr.setMaxWidth(Double.MAX_VALUE);
                detailPanel.getChildren().add(nameHdr);

                addRow("  ID",               s.getId().substring(0, 8));
                addRow("  Current count",    String.valueOf(s.getNumSpecies()));
                addRow("  Initial stock",    String.valueOf(s.getInitialTotalIndividuals()));
                addRow("  Cycle baseline",   String.valueOf(s.getCycleBaseline()));
                addRow("  Cycle mortality",  String.valueOf(s.getCycleMortality()));
                addRow("  Total mortality",  String.valueOf(s.getTotalMortality()));
                addRow("  Cycle survival",   String.format("%.1f%%", s.getCycleSurvivalRatePercent()));
                addRow("  Overall survival", String.format("%.1f%%", s.getOverallSurvivalRatePercent()));
                addRow("  Total harvested",  String.format("%d fish  (%.2f kg)", s.getTotalHarvestedCount(), s.getTotalHarvestWeightKg()));

                if (s.wasHarvested()) {
                    javafx.scene.control.ListView<String> histList = new javafx.scene.control.ListView<>();
                    histList.setPrefHeight(Math.min(120, s.getHarvestCount() * 30.0));
                    histList.getStyleClass().add("event-list");
                    for (int i = s.getHarvestHistory().size() - 1; i >= 0; i--) {
                        AquacultureSpecies.HarvestRecord hr = s.getHarvestHistory().get(i);
                        histList.getItems().add(String.format("[%s]  %d harvested of %d  —  %.2f kg",
                            hr.getDate().toLocalDate(), hr.getCountHarvested(), hr.getCountBefore(), hr.getWeightKg()));
                    }
                    detailPanel.getChildren().add(histList);
                }

                Button harvestBtn = new Button("🐟 Harvest");
                harvestBtn.getStyleClass().add("btn-secondary");
                harvestBtn.setOnAction(e -> showAquaHarvestDialog(s, z));

                Button mortalityBtn = new Button("💀 Mortality");
                mortalityBtn.getStyleClass().add("btn-secondary");
                mortalityBtn.setOnAction(e -> showMortalityDialog(s, z));

                Button restockBtn = new Button("➕ Restock");
                restockBtn.getStyleClass().add("btn-secondary");
                restockBtn.setOnAction(e -> showRestockDialog(s, z));

                Button tubBoundaryBtn = new Button(s.hasBoundary()
                    ? "🗺 Tub Boundary (" + s.getBoundary().size() + " pts)"
                    : "🗺 Set Tub Boundary");
                tubBoundaryBtn.getStyleClass().add("btn-secondary");
                tubBoundaryBtn.setOnAction(e -> {
                    if (!z.hasBoundaries()) {
                        Alert warn = new Alert(Alert.AlertType.WARNING);
                        warn.setTitle("Zone Boundary Required");
                        warn.setHeaderText(null);
                        warn.setContentText("Set the zone boundary first — the tub sub-zone must fit inside it.");
                        warn.showAndWait();
                        return;
                    }
                    BoundaryEditorDialog dlg = new BoundaryEditorDialog(
                        s.getName() + " tub",
                        s.hasBoundary() ? s.getBoundary() : null,
                        z.getBoundaries(),
                        detailPanel.getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(bounds -> {
                        s.setBoundary(bounds);
                        FarmService.getInstance().autoSave();
                        showAquaDetail(z);
                    });
                });

                Button removeSpeciesBtn = new Button("✕");
                removeSpeciesBtn.getStyleClass().add("btn-danger");
                removeSpeciesBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Species");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Remove " + s.getName() + " from " + z.getName() + "?");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            z.removeSpecies(s);
                            FarmService.getInstance().autoSave();
                            aquaData.setAll(zoneService.getAquacultureZones());
                            showAquaDetail(z);
                        }
                    });
                });

                HBox speciesActions = new HBox(8, harvestBtn, mortalityBtn, restockBtn, tubBoundaryBtn, removeSpeciesBtn);
                speciesActions.setPadding(new Insets(2, 0, 10, 0));
                detailPanel.getChildren().add(speciesActions);
            }
        }

        // Zone management actions
        addSectionTitle("Zone Actions");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = new Button("⏸ Suspend Zone");
            suspBtn.getStyleClass().add("btn-secondary");
            suspBtn.setMaxWidth(Double.MAX_VALUE);
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); aquaData.setAll(zoneService.getAquacultureZones()); showAquaDetail(z); refreshStats(); });
            detailPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = new Button("▶ Activate Zone");
            actBtn.getStyleClass().add("btn-primary");
            actBtn.setMaxWidth(Double.MAX_VALUE);
            actBtn.setOnAction(e -> { zoneService.activateZone(z); aquaData.setAll(zoneService.getAquacultureZones()); showAquaDetail(z); refreshStats(); });
            detailPanel.getChildren().add(actBtn);
        }

        Button addSpeciesBtn = new Button("➕ Add Species");
        addSpeciesBtn.getStyleClass().add("btn-primary");
        addSpeciesBtn.setMaxWidth(Double.MAX_VALUE);
        addSpeciesBtn.setOnAction(e -> showAddSpeciesDialog(z));
        detailPanel.getChildren().add(addSpeciesBtn);

        Button addWaterBtn = new Button("💧 Add Water Sensor");
        addWaterBtn.getStyleClass().add("btn-secondary");
        addWaterBtn.setMaxWidth(Double.MAX_VALUE);
        addWaterBtn.setOnAction(e -> showAddWaterSensorDialog(z));
        detailPanel.getChildren().add(addWaterBtn);

        Button aqBoundaryBtn = new Button(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary");
        aqBoundaryBtn.getStyleClass().add("btn-secondary");
        aqBoundaryBtn.setMaxWidth(Double.MAX_VALUE);
        aqBoundaryBtn.setOnAction(e -> {
            BoundaryEditorDialog dlg = new BoundaryEditorDialog(z.getName(),
                z.hasBoundaries() ? z.getBoundaries() : null,
                detailPanel.getScene().getStylesheets());
            dlg.showAndWait().ifPresent(bounds -> {
                z.setBoundaries(bounds);
                FarmService.getInstance().autoSave();
                showAquaDetail(z);
            });
        });
        detailPanel.getChildren().add(aqBoundaryBtn);

        Button aqMapBtn = new Button("📍 View Zone Map");
        aqMapBtn.getStyleClass().add("btn-secondary");
        aqMapBtn.setMaxWidth(Double.MAX_VALUE);
        aqMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(aqMapBtn);

        Button aqSensorHistBtn = new Button("📊 Sensor History");
        aqSensorHistBtn.getStyleClass().add("btn-secondary");
        aqSensorHistBtn.setMaxWidth(Double.MAX_VALUE);
        aqSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(aqSensorHistBtn);

        Button aqAlertHistBtn = new Button("🔔 Alert History");
        aqAlertHistBtn.getStyleClass().add("btn-secondary");
        aqAlertHistBtn.setMaxWidth(Double.MAX_VALUE);
        aqAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        detailPanel.getChildren().add(aqAlertHistBtn);

        Button renameBtn = new Button("✏ Rename Zone");
        renameBtn.getStyleClass().add("btn-secondary");
        renameBtn.setMaxWidth(Double.MAX_VALUE);
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            aquaData.setAll(zoneService.getAquacultureZones());
            showAquaDetail(z);
            refreshStats();
        }));
        detailPanel.getChildren().add(renameBtn);

        Button deleteBtn = new Button("🗑 Delete Zone");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> confirmDeleteZone(z, () -> {
            aquaData.setAll(zoneService.getAquacultureZones());
            detailPanel.getChildren().clear();
            refreshStats();
        }));
        detailPanel.getChildren().add(deleteBtn);
    }

    // ── Crop action dialogs ───────────────────────────────────────────

    private void showRecordCropHarvestDialog(Crop c, CropZONE zone) {
        TextField kgField = new TextField("0.0");
        VBox form = new VBox(14, formGroup("Harvest (kg)", kgField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Record Harvest");
        dialog.setHeaderText("Harvest for " + c.getVariety());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(kgField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(kg -> {
            if (kg >= 0) { c.recordHarvest(kg); FarmService.getInstance().autoSave(); showCropDetail(zone); crData.setAll(zoneService.getCropZones()); }
        });
    }

    private void showUpdateGrowthStageDialog(Crop c, CropZONE zone) {
        ComboBox<GrowthStage> stageCombo = new ComboBox<>();
        stageCombo.getItems().addAll(GrowthStage.values());
        stageCombo.setValue(c.getGrowthStage());
        VBox form = new VBox(14, formGroup("Growth Stage", stageCombo));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<GrowthStage> dialog = new Dialog<>();
        dialog.setTitle("Update Growth Stage");
        dialog.setHeaderText("Growth stage for " + c.getVariety());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Update");
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? stageCombo.getValue() : null);
        dialog.showAndWait().ifPresent(stage -> {
            c.updateGrowthStage(stage); FarmService.getInstance().autoSave(); showCropDetail(zone);
        });
    }

    // ── Aquaculture action dialogs ────────────────────────────────────

    private void showAquaHarvestDialog(AquacultureSpecies s, AquacultureZONE zone) {
        TextField kgField    = new TextField("0.0");
        TextField countField = new TextField("0");
        countField.setPromptText("Max: " + s.getNumSpecies());
        VBox form = new VBox(14,
            formGroup("Weight (kg)", kgField),
            formGroup("Count harvested (max " + s.getNumSpecies() + ")", countField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Record Harvest");
        dialog.setHeaderText("Harvest " + s.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(380);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                double kg = Double.parseDouble(kgField.getText().trim());
                int cnt   = Integer.parseInt(countField.getText().trim());
                return new int[]{(int)(kg * 100), cnt};
            } catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(arr -> {
            try {
                double kg = arr[0] / 100.0;
                int cnt   = arr[1];
                s.harvest(kg, cnt);
                FarmService.getInstance().autoSave();
                aquaData.setAll(zoneService.getAquacultureZones());
                showAquaDetail(zone);
            } catch (IllegalArgumentException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Harvest");
                err.setHeaderText(null);
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private void showMortalityDialog(AquacultureSpecies s, AquacultureZONE zone) {
        TextField countField = new TextField("0");
        countField.setPromptText("Max: " + s.getNumSpecies());
        VBox form = new VBox(14, formGroup("Mortality count", countField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Record Mortality");
        dialog.setHeaderText("Mortality for " + s.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Integer.parseInt(countField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(cnt -> {
            try {
                s.recordMortality(cnt);
                FarmService.getInstance().autoSave();
                aquaData.setAll(zoneService.getAquacultureZones());
                showAquaDetail(zone);
            } catch (IllegalArgumentException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input");
                err.setHeaderText(null);
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private void showRestockDialog(AquacultureSpecies s, AquacultureZONE zone) {
        TextField countField = new TextField("50");
        VBox form = new VBox(14, formGroup("Restock count", countField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Restock");
        dialog.setHeaderText("Restock " + s.getName());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Restock");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Integer.parseInt(countField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(cnt -> {
            if (cnt > 0) {
                s.restock(cnt);
                FarmService.getInstance().autoSave();
                aquaData.setAll(zoneService.getAquacultureZones());
                showAquaDetail(zone);
            }
        });
    }

    // ── Edit Feeding Program (uses setters — preserves lastFedTime) ───

    private void showEditFeedingProgramDialog(LivestockZONE zone, FeedingProgram fp) {
        TextField foodField     = new TextField(fp.getFoodType());
        TextField quantityField = new TextField(String.valueOf(fp.getQuantity()));
        TextField scheduleField = new TextField(String.join(", ", fp.getSchedule()));
        Label infoLbl = new Label("Wake: " + fp.getWakeUpTime() + "   Sleep: " + fp.getSleepTime()
            + "   (to change wake/sleep, recreate the program)");
        infoLbl.getStyleClass().add("text-muted");
        infoLbl.setWrapText(true);
        VBox form = new VBox(14,
            formGroup("Food Type", foodField),
            formGroup("Quantity per day (kg)", quantityField),
            formGroup("Schedule (HH:mm, comma-separated)", scheduleField),
            infoLbl
        );
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Feeding Program");
        dialog.setHeaderText("Edit feeding program for \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(440);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Save");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String food = foodField.getText().trim();
                double qty  = Double.parseDouble(quantityField.getText().trim());
                List<String> sched = Arrays.asList(scheduleField.getText().trim().split("\\s*,\\s*"));
                fp.setFoodType(food);
                fp.setQuantity(qty);
                fp.setSchedule(sched);
                return Boolean.TRUE;
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input");
                err.setHeaderText(null);
                err.setContentText("Check quantity (number) and schedule (HH:mm).\n" + e.getMessage());
                err.showAndWait();
                return null;
            }
        });
        dialog.showAndWait().ifPresent(ok -> {
            FarmService.getInstance().autoSave();
            showLivestockDetail(zone);
        });
    }

    // ── Sensor creation dialogs ───────────────────────────────────────

    private void showAddBioSensorDialog(LivestockZONE zone) {
        if (zone.getAnimals().isEmpty()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("No Animals"); warn.setHeaderText(null);
            warn.setContentText("Add animals to this zone before attaching a bio sensor.");
            warn.showAndWait(); return;
        }
        ComboBox<Animal> animalCombo = new ComboBox<>();
        animalCombo.getItems().addAll(zone.getAnimals());
        animalCombo.setValue(zone.getAnimals().get(0));
        animalCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Animal a) { return a == null ? "" : a.getName(); }
            @Override public Animal fromString(String s) { return null; }
        });
        ComboBox<BioMeasureType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(BioMeasureType.values());
        typeCombo.setValue(BioMeasureType.Temperature);
        TextField minField = new TextField("36.0");
        TextField maxField = new TextField("39.5");
        VBox form = new VBox(14, formGroup("Animal", animalCombo),
            formGroup("Measure Type", typeCombo),
            formGroup("Min Threshold", minField), formGroup("Max Threshold", maxField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<BioSensor> dialog = new Dialog<>();
        dialog.setTitle("Add Bio Sensor");
        dialog.setHeaderText("Attach bio sensor in \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(420);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Add Sensor");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                return new BioSensor(animalCombo.getValue(), typeCombo.getValue(), min, max);
            } catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(sensor -> {
            sensor.getAnimal().attachBioSensor(sensor);
            zone.addBioSensor(sensor);
            FarmService.getInstance().autoSave();
            showLivestockDetail(zone);
        });
    }

    private void showAddEnvSensorDialog(CropZONE zone) {
        ComboBox<EnvMeasureType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(EnvMeasureType.values());
        typeCombo.setValue(EnvMeasureType.Temperature);
        TextField minField = new TextField("15.0");
        TextField maxField = new TextField("35.0");
        VBox form = new VBox(14, formGroup("Measure Type", typeCombo),
            formGroup("Min Threshold", minField), formGroup("Max Threshold", maxField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<EnvSensor> dialog = new Dialog<>();
        dialog.setTitle("Add Environment Sensor");
        dialog.setHeaderText("Add env sensor to \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Add Sensor");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                EnvMeasureType type = typeCombo.getValue();
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                String unit = switch (type) { case Temperature -> "°C"; case Humidity -> "%"; case Rainfall -> "mm"; };
                return new EnvSensor(zone, type, min, max, unit);
            } catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(sensor -> {
            zone.addEnvSensor(sensor);
            FarmService.getInstance().autoSave();
            showCropDetail(zone);
        });
    }

    private void showAddSoilSensorDialog(CropZONE zone) {
        ComboBox<SoilMeasureType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(SoilMeasureType.values());
        typeCombo.setValue(SoilMeasureType.PH);
        TextField minField = new TextField("6.0");
        TextField maxField = new TextField("7.5");
        VBox form = new VBox(14, formGroup("Measure Type", typeCombo),
            formGroup("Min Threshold", minField), formGroup("Max Threshold", maxField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<SoilSensor> dialog = new Dialog<>();
        dialog.setTitle("Add Soil Sensor");
        dialog.setHeaderText("Add soil sensor to \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Add Sensor");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                SoilMeasureType type = typeCombo.getValue();
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                String unit = switch (type) { case PH -> "pH"; case Moisture -> "%"; case Nitrogen -> "mg/kg"; };
                return new SoilSensor(zone, type, min, max, unit);
            } catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(sensor -> {
            zone.addSoilSensor(sensor);
            FarmService.getInstance().autoSave();
            showCropDetail(zone);
        });
    }

    private void showAddWaterSensorDialog(AquacultureZONE zone) {
        ComboBox<WaterMeasureType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(WaterMeasureType.values());
        typeCombo.setValue(WaterMeasureType.Temperature);
        TextField minField = new TextField("20.0");
        TextField maxField = new TextField("28.0");
        VBox form = new VBox(14, formGroup("Measure Type", typeCombo),
            formGroup("Min Threshold", minField), formGroup("Max Threshold", maxField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<WaterSensor> dialog = new Dialog<>();
        dialog.setTitle("Add Water Sensor");
        dialog.setHeaderText("Add water sensor to \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(400);
        applyDialogStyle(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Add Sensor");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                WaterMeasureType type = typeCombo.getValue();
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                String unit = switch (type) { case Temperature -> "°C"; case DissolvedOxygen -> "mg/L"; };
                return new WaterSensor(zone, type, min, max, unit);
            } catch (NumberFormatException e) { return null; }
        });
        dialog.showAndWait().ifPresent(sensor -> {
            zone.addWaterSensor(sensor);
            FarmService.getInstance().autoSave();
            showAquaDetail(zone);
        });
    }

    // ── Shared zone action dialogs ────────────────────────────────────

    private void showRenameZoneDialog(ZONE zone, Runnable onSuccess) {
        TextField nameField = new TextField(zone.getName());
        VBox form = new VBox(14, formGroup("New name", nameField));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rename Zone");
        dialog.setHeaderText("Rename \"" + zone.getName() + "\"");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        applyDialogStyle(dialog);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Rename");
        okBtn.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? nameField.getText().trim() : null);
        dialog.showAndWait().ifPresent(name -> { zoneService.renameZone(zone, name); onSuccess.run(); });
    }

    private void confirmDeleteZone(ZONE zone, Runnable onSuccess) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Zone");
        confirm.setHeaderText(null);
        confirm.setContentText("Permanently delete zone \"" + zone.getName() + "\" and all its contents?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) { zoneService.removeZone(zone); onSuccess.run(); }
        });
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
        var sheets = detailPanel.getScene() == null ? null : detailPanel.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0)
            : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);
        Button ok     = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (ok != null)     ok.getStyleClass().add("btn-primary");
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");
    }

    private void reloadTables() {
        lsData.setAll(zoneService.getLivestockZones());
        crData.setAll(zoneService.getCropZones());
        aquaData.setAll(zoneService.getAquacultureZones());
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

    // ── Toolbar actions ───────────────────────────────────────────────

    @FXML private void activateZone() {
        ZONE z = getSelectedZone();
        if (z != null) { zoneService.activateZone(z); refreshTables(); refreshStats(); }
    }

    @FXML private void suspendZone() {
        ZONE z = getSelectedZone();
        if (z != null) { zoneService.suspendZone(z); refreshTables(); refreshStats(); }
    }

    private ZONE getSelectedZone() {
        ZONE z = livestockTable.getSelectionModel().getSelectedItem();
        if (z != null) return z;
        z = cropTable.getSelectionModel().getSelectedItem();
        if (z != null) return z;
        return aquaTable.getSelectionModel().getSelectedItem();
    }

    private void refreshTables() {
        livestockTable.refresh();
        cropTable.refresh();
        aquaTable.refresh();
    }
}
