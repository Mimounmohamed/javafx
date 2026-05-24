package com.example.controllers;

import Additional_classes.Range;
import Animals.Animal;
import Animals.AnimalHealthStatus;
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
import com.example.services.AnimalService;
import com.example.services.FarmService;
import com.example.services.ZoneService;
import com.example.utils.SceneManager;
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
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TableRow;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class ZonesController {

    // ── Table columns (all existing fx:ids preserved) ─────────────────
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

    // ── Detail panel (preserved fx:id) ────────────────────────────────
    @FXML private VBox      detailPanel;
    @FXML private TextField searchField;

    // ── Filter combos (new — match Animals filter row) ─────────────────
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;

    // ── Stat labels (preserved) ───────────────────────────────────────
    @FXML private Label statTotalZones;
    @FXML private Label statActiveZones;
    @FXML private Label statLivestockCount;
    @FXML private Label statCropCount;
    @FXML private Label statAquaCount;

    // ── Stat sub-labels (preserved) ───────────────────────────────────
    @FXML private Label statTotalSub;
    @FXML private Label statActiveSub;
    @FXML private Label statLivestockSub;
    @FXML private Label statCropSub;
    @FXML private Label statAquaSub;

    // ── Distress banner (preserved) ────────────────────────────────────
    @FXML private HBox  distressBanner;
    @FXML private Label distressBannerText;

    // ── Right-panel structure (preserved) ─────────────────────────────
    @FXML private VBox     zoneDetailHeader;
    @FXML private Label    zoneDetailName;
    @FXML private TabPane  zoneDetailTabs;
    @FXML private VBox     actionsPanel;
    @FXML private VBox     zoneDetailPlaceholder;

    private final ZoneService   zoneService   = ZoneService.getInstance();
    private final AnimalService animalService = AnimalService.getInstance();

    private ObservableList<LivestockZONE>   lsData;
    private ObservableList<CropZONE>        crData;
    private ObservableList<AquacultureZONE> aquaData;

    private FilteredList<LivestockZONE>   lsFiltered;
    private FilteredList<CropZONE>        crFiltered;
    private FilteredList<AquacultureZONE> aquaFiltered;

    @FXML
    public void initialize() {
        lsData   = FXCollections.observableArrayList(zoneService.getLivestockZones());
        crData   = FXCollections.observableArrayList(zoneService.getCropZones());
        aquaData = FXCollections.observableArrayList(zoneService.getAquacultureZones());

        lsFiltered   = new FilteredList<>(lsData,   p -> true);
        crFiltered   = new FilteredList<>(crData,   p -> true);
        aquaFiltered = new FilteredList<>(aquaData, p -> true);

        setupLivestockTable(lsFiltered);
        setupCropTable(crFiltered);
        setupAquaTable(aquaFiltered);
        setupFilters();
        refreshStats();
    }

    private void setupFilters() {
        filterType.getItems().addAll("All Types", "Livestock", "Crop", "Aquaculture");
        filterType.setValue("All Types");

        filterStatus.getItems().addAll("All Status", "Active", "Suspended");
        filterStatus.setValue("All Status");

        filterType.setOnAction(e -> applyFilters());
        filterStatus.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String type   = filterType.getValue();
        String status = filterStatus.getValue();

        lsFiltered.setPredicate(z -> {
            if ("Crop".equals(type) || "Aquaculture".equals(type)) return false;
            boolean statusOk = "All Status".equals(status)
                || ("Active".equals(status)    && z.getStatus() == ZoneStatus.ACTIVE)
                || ("Suspended".equals(status) && z.getStatus() == ZoneStatus.SUSPENDED);
            boolean searchOk = search.isEmpty()
                || z.getName().toLowerCase().contains(search)
                || z.getCode().toLowerCase().contains(search);
            return statusOk && searchOk;
        });

        crFiltered.setPredicate(z -> {
            if ("Livestock".equals(type) || "Aquaculture".equals(type)) return false;
            boolean statusOk = "All Status".equals(status)
                || ("Active".equals(status)    && z.getStatus() == ZoneStatus.ACTIVE)
                || ("Suspended".equals(status) && z.getStatus() == ZoneStatus.SUSPENDED);
            boolean searchOk = search.isEmpty()
                || z.getName().toLowerCase().contains(search)
                || z.getCode().toLowerCase().contains(search);
            return statusOk && searchOk;
        });

        aquaFiltered.setPredicate(z -> {
            if ("Livestock".equals(type) || "Crop".equals(type)) return false;
            boolean statusOk = "All Status".equals(status)
                || ("Active".equals(status)    && z.getStatus() == ZoneStatus.ACTIVE)
                || ("Suspended".equals(status) && z.getStatus() == ZoneStatus.SUSPENDED);
            boolean searchOk = search.isEmpty()
                || z.getName().toLowerCase().contains(search)
                || z.getCode().toLowerCase().contains(search);
            return statusOk && searchOk;
        });
    }

    private void refreshStats() {
        List<LivestockZONE> lstZones  = zoneService.getLivestockZones();
        List<CropZONE>      crpZones  = zoneService.getCropZones();
        List<AquacultureZONE> aqZones = zoneService.getAquacultureZones();
        List<ZONE> allZones           = zoneService.getAllZones();

        int total     = allZones.size();
        int active    = (int) allZones.stream().filter(z -> z.getStatus() == ZoneStatus.ACTIVE).count();
        int suspended = total - active;

        statTotalZones.setText(String.valueOf(total));
        statActiveZones.setText(String.valueOf(active));
        statLivestockCount.setText(String.valueOf(lstZones.size()));
        statCropCount.setText(String.valueOf(crpZones.size()));
        statAquaCount.setText(String.valueOf(aqZones.size()));

        if (statTotalSub != null) statTotalSub.setText("all types");

        if (statActiveSub != null)
            statActiveSub.setText(suspended > 0 ? suspended + " suspended" : "all running");

        // Livestock: count quarantined animals across all livestock zones
        long quarantined = lstZones.stream()
            .flatMap(z -> z.getAnimals().stream())
            .filter(a -> a.getHealthStatus() == AnimalHealthStatus.Quarantined)
            .count();
        if (statLivestockSub != null) {
            if (quarantined > 0) {
                statLivestockSub.setText("⚠ " + quarantined + " quarantined");
                statLivestockSub.getStyleClass().setAll("zones-stat-card-sub-warn");
            } else {
                statLivestockSub.setText("no alerts");
                statLivestockSub.getStyleClass().setAll("zones-stat-card-sub");
            }
        }

        // Crop / Aqua: basic alert count from sensor distress
        long crpAlerts = crpZones.stream()
            .flatMap(z -> z.getEnvSensors().stream())
            .filter(s -> s.isOutOfRange(s.getLastValue())).count()
            + crpZones.stream().flatMap(z -> z.getSoilSensors().stream())
            .filter(s -> s.isOutOfRange(s.getLastValue())).count();

        if (statCropSub != null)
            statCropSub.setText(crpAlerts > 0 ? crpAlerts + " alerts" : "no alerts");

        long aqAlerts = aqZones.stream()
            .flatMap(z -> z.getWaterSensors().stream())
            .filter(s -> s.isOutOfRange(s.getLastValue())).count();

        if (statAquaSub != null)
            statAquaSub.setText(aqAlerts > 0 ? aqAlerts + " alerts" : "no alerts");
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupLivestockTable(FilteredList<LivestockZONE> data) {
        lsColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        lsColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        lsColType.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getType().toString()));
        lsColAnimals.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAnimals().size()).asObject());

        // Animals column — styled link with arrow
        lsColAnimals.setCellFactory(col -> new TableCell<>() {
            private final Label link = new Label();
            { link.getStyleClass().add("zones-animals-link"); }
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                link.setText(item + " →");
                link.setOnMouseClicked(e -> SceneManager.getInstance().navigateTo("animals"));
                setGraphic(link);
            }
        });

        setupStatusColumn(lsColStatus, z -> z.getStatus());
        addChevronColumn(livestockTable);

        // Fix 3: row factory ensures clicks always fire, including re-clicking the same row
        livestockTable.setRowFactory(tv -> {
            TableRow<LivestockZONE> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) showLivestockDetail(row.getItem()); });
            return row;
        });
        livestockTable.setItems(data);
        livestockTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showLivestockDetail(n); });
    }

    private void setupCropTable(FilteredList<CropZONE> data) {
        crColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        crColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        crColFields.setCellValueFactory(d  -> new SimpleIntegerProperty(d.getValue().getFields().size()).asObject());
        crColSurface.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getSurfacePlanted()).asObject());
        setupStatusColumn(crColStatus, z -> z.getStatus());
        addChevronColumn(cropTable);

        cropTable.setRowFactory(tv -> {
            TableRow<CropZONE> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) showCropDetail(row.getItem()); });
            return row;
        });
        cropTable.setItems(data);
        cropTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showCropDetail(n); });
    }

    private void setupAquaTable(FilteredList<AquacultureZONE> data) {
        aqColCode.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getCode()));
        aqColName.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getName()));
        aqColSpecies.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSpeciesList().size()).asObject());
        setupStatusColumn(aqColStatus, z -> z.getStatus());
        addChevronColumn(aquaTable);

        aquaTable.setRowFactory(tv -> {
            TableRow<AquacultureZONE> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) showAquaDetail(row.getItem()); });
            return row;
        });
        aquaTable.setItems(data);
        aquaTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showAquaDetail(n); });
    }

    private <T> void addChevronColumn(TableView<T> table) {
        TableColumn<T, String> col = new TableColumn<>("");
        col.setPrefWidth(28); col.setMinWidth(28); col.setMaxWidth(28);
        col.setSortable(false); col.setReorderable(false);
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); return;
                }
                setText("›");
                setStyle("-fx-text-fill: #888780; -fx-font-size: 18px; -fx-alignment: CENTER;");
            }
        });
        table.getColumns().add(col);
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

    // ── Detail panel — open/close ──────────────────────────────────────

    private void openDetailPanel(ZONE zone) {
        // keep zoneDetailName in sync (invisible label preserved for fx:id contract)
        zoneDetailName.setText(zone.getName());

        // ── Build Animals-style header ─────────────────────────────────
        zoneDetailHeader.getChildren().clear();
        zoneDetailHeader.setSpacing(10);
        zoneDetailHeader.setPadding(new Insets(14, 16, 12, 16));

        boolean active = zone.getStatus() == ZoneStatus.ACTIVE;

        // Avatar circle
        Label avatar = new Label(zone.getName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().addAll("zones-avatar",
            active ? "zones-avatar-active" : "zones-avatar-suspended");

        // Name + subtitle VBox
        Label nameLbl = new Label(zone.getName());
        nameLbl.getStyleClass().add("animals-detail-name");

        String typeStr = (zone instanceof LivestockZONE ls)
            ? ls.getType().toString()
            : (zone instanceof CropZONE) ? "CROP" : "AQUACULTURE";
        Label subtitleLbl = new Label(
            typeStr + " · " + zone.getCode());
        subtitleLbl.getStyleClass().add("animals-detail-subtitle");

        VBox nameBox = new VBox(2, nameLbl, subtitleLbl);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox heroRow = new HBox(10, avatar, nameBox);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        // Status pill
        Label statusPill = new Label(zone.getStatus().toString());
        statusPill.getStyleClass().addAll("badge",
            active ? "badge-active" : "badge-suspended");

        // Ghost action buttons
        HBox actionBtns = new HBox(6);
        if (active) {
            Button suspBtn = new Button("⏸ Suspend");
            suspBtn.getStyleClass().add("btn-ghost");
            suspBtn.setOnAction(e -> {
                zoneService.suspendZone(zone); reloadTables(); refreshStats(); openDetailPanel(zone);
            });
            actionBtns.getChildren().add(suspBtn);
        } else {
            Button actBtn = new Button("▶ Activate");
            actBtn.getStyleClass().add("btn-ghost");
            actBtn.setOnAction(e -> {
                zoneService.activateZone(zone); reloadTables(); refreshStats(); openDetailPanel(zone);
            });
            actionBtns.getChildren().add(actBtn);
        }
        Button mapBtn = new Button("📍 Map");
        mapBtn.getStyleClass().add("btn-ghost");
        mapBtn.setOnAction(e -> new ZoneMapDialog(zone, detailPanel.getScene().getStylesheets()).showAndWait());
        actionBtns.getChildren().add(mapBtn);

        // Status row
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(8, statusPill, spacer, actionBtns);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        zoneDetailHeader.getChildren().addAll(heroRow, statusRow);

        // ── Show panel ─────────────────────────────────────────────────
        zoneDetailHeader.setVisible(true);
        zoneDetailHeader.setManaged(true);
        zoneDetailTabs.setVisible(true);
        zoneDetailTabs.setManaged(true);
        zoneDetailPlaceholder.setVisible(false);
        zoneDetailPlaceholder.setManaged(false);
        detailPanel.getChildren().clear();
        actionsPanel.getChildren().clear();
    }

    @FXML
    private void closeDetail() {
        zoneDetailHeader.getChildren().clear();
        zoneDetailHeader.setVisible(false);
        zoneDetailHeader.setManaged(false);
        zoneDetailTabs.setVisible(false);
        zoneDetailTabs.setManaged(false);
        zoneDetailPlaceholder.setVisible(true);
        zoneDetailPlaceholder.setManaged(true);
        livestockTable.getSelectionModel().clearSelection();
        cropTable.getSelectionModel().clearSelection();
        aquaTable.getSelectionModel().clearSelection();
        detailPanel.getChildren().clear();
        actionsPanel.getChildren().clear();
    }

    // ── Add Zone dialog ───────────────────────────────────────────────

    @FXML
    private void showAddZoneDialog() {
        var sheets = detailPanel.getScene() == null ? null : detailPanel.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0)
            : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        AddZoneDialog dialog = new AddZoneDialog(css);
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
                err.setTitle("Invalid Input"); err.setHeaderText(null);
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
        openDetailPanel(z);

        // ── DETAILS TAB ──────────────────────────────────────────────
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

        // Animals in zone — always show header with Add Animal button (Fix 4)
        Button addAnimalBtn = new Button("+ Add Animal");
        addAnimalBtn.getStyleClass().add("zones-add-entity-btn");
        addAnimalBtn.setOnAction(e -> showAddAnimalInZoneDialog(z));
        addSectionTitleWithButton("Animals in zone", addAnimalBtn);

        if (!z.getAnimals().isEmpty()) {
            // Chip row (Fix 1: 8px gap, updated padding, quarantine icon)
            FlowPane chips = new FlowPane(8, 8);
            for (Animal a : z.getAnimals()) {
                String prefix = (a.getHealthStatus() == AnimalHealthStatus.Quarantined) ? "⚠ " : "";
                Label chip = new Label(prefix + a.getName() + " · " + a.getSpecies());
                chip.getStyleClass().add("zones-animal-chip");
                chip.getStyleClass().add(switch (a.getHealthStatus()) {
                    case Healthy     -> "zones-animal-chip-healthy";
                    case Sick        -> "zones-animal-chip-sick";
                    case Quarantined -> "zones-animal-chip-quarantined";
                });
                chips.getChildren().add(chip);
            }
            detailPanel.getChildren().add(chips);

            // Per-animal management rows (Fix 1: restyled with badge + icon buttons)
            addSectionTitle("Animal management");
            for (Animal a : z.getAnimals()) {
                Label nameLbl = new Label(a.getName() + "  " + a.getSpecies());
                nameLbl.getStyleClass().add("zones-animal-mgmt-name");
                nameLbl.setWrapText(true);

                Label statusBadge = new Label(a.getHealthStatus().toString());
                statusBadge.getStyleClass().addAll("badge", switch (a.getHealthStatus()) {
                    case Healthy     -> "badge-healthy";
                    case Sick        -> "badge-sick";
                    case Quarantined -> "badge-quarantined";
                });

                VBox nameCol = new VBox(3, nameLbl, statusBadge);
                HBox.setHgrow(nameCol, Priority.ALWAYS);

                Button mapAnimalBtn = new Button("📍");
                mapAnimalBtn.getStyleClass().add("zones-icon-btn");
                mapAnimalBtn.setMinWidth(28); mapAnimalBtn.setMaxWidth(28);
                mapAnimalBtn.setMinHeight(28); mapAnimalBtn.setMaxHeight(28);
                mapAnimalBtn.setOnAction(e -> {
                    ZoneMapDialog mapDlg = new ZoneMapDialog(z,
                        detailPanel.getScene().getStylesheets(), ZoneMapDialog.key(a));
                    mapDlg.showAndWait();
                });

                Button removeAnimalBtn = new Button("✕");
                removeAnimalBtn.getStyleClass().add("zones-icon-btn");
                removeAnimalBtn.setMinWidth(28); removeAnimalBtn.setMaxWidth(28);
                removeAnimalBtn.setMinHeight(28); removeAnimalBtn.setMaxHeight(28);
                removeAnimalBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Animal"); confirm.setHeaderText(null);
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

                HBox mgmtRow = new HBox(8, nameCol, new HBox(8, mapAnimalBtn, removeAnimalBtn));
                mgmtRow.setAlignment(Pos.CENTER_LEFT);
                mgmtRow.getStyleClass().add("zones-animal-mgmt-row");
                mgmtRow.setPadding(new Insets(10, 8, 10, 8));
                detailPanel.getChildren().add(mgmtRow);
            }
        }

        // Bio sensors — styled rows with distress highlighting + 6px gap (Fix 1)
        if (!z.getBioSensors().isEmpty()) {
            addSectionTitle("Bio Sensors (" + z.getBioSensors().size() + ")");
            for (BioSensor sensor : new java.util.ArrayList<>(z.getBioSensors())) {
                HBox sensorRow = buildSensorRow(sensor, z);
                VBox.setMargin(sensorRow, new Insets(0, 0, 6, 0));
                detailPanel.getChildren().add(sensorRow);
            }
        } else {
            addRow("Bio Sensors", "0");
        }

        // GPS collar sensors
        if (!z.getGpsCollarSensors().isEmpty()) {
            addSectionTitle("GPS Collar Sensors (" + z.getGpsCollarSensors().size() + ")");
            for (GPSCollarSensor sensor : new java.util.ArrayList<>(z.getGpsCollarSensors())) {
                String info = sensor.getCode() + "  on " + sensor.getAnimal().getName()
                    + (sensor.hasEscaped() ? "  ⚠ OUTSIDE ZONE" : "");
                Label lbl = new Label(info);
                lbl.getStyleClass().add(sensor.hasEscaped() ? "zones-sensor-name-alert" : "detail-value");
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
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(lbl, Priority.ALWAYS);
                detailPanel.getChildren().add(row);
            }
        } else {
            addRow("GPS Sensors", "0");
        }

        // Feeding program details (read-only; actions go to Actions tab)
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
        }

        // ── ACTIONS TAB ──────────────────────────────────────────────
        addActionGroupTitle("Manage");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = actionBtn("⏸ Suspend Zone", "btn-secondary");
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); lsData.setAll(zoneService.getLivestockZones()); showLivestockDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = actionBtn("▶ Activate Zone", "btn-primary");
            actBtn.setOnAction(e -> { zoneService.activateZone(z); lsData.setAll(zoneService.getLivestockZones()); showLivestockDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(actBtn);
        }

        if (fp != null) {
            Button recordFeedBtn = actionBtn("🍽 Record Feeding Now", "btn-primary");
            recordFeedBtn.setOnAction(e -> { zoneService.recordFeeding(z); showLivestockDetail(z); });
            actionsPanel.getChildren().add(recordFeedBtn);

            Button editFpBtn = actionBtn("✏ Edit Feeding Program", "btn-secondary");
            editFpBtn.setOnAction(e -> showEditFeedingProgramDialog(z, fp));
            actionsPanel.getChildren().add(editFpBtn);
        } else {
            Button fpBtn = actionBtn("🗓 Create Feeding Program", "btn-secondary");
            fpBtn.setOnAction(e -> showCreateFeedingProgramDialog(z));
            actionsPanel.getChildren().add(fpBtn);
        }

        addActionGroupTitle("Sensors & Map");

        Button addBioBtn = actionBtn("📡 Add Bio Sensor", "btn-secondary");
        addBioBtn.setOnAction(e -> showAddBioSensorDialog(z));
        actionsPanel.getChildren().add(addBioBtn);

        Button lsBoundaryBtn = actionBtn(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary", "btn-secondary");
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
        actionsPanel.getChildren().add(lsBoundaryBtn);

        Button lsMapBtn = actionBtn("📍 View Zone Map", "btn-secondary");
        lsMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(lsMapBtn);

        addActionGroupTitle("History");

        Button lsSensorHistBtn = actionBtn("📊 Sensor History", "btn-secondary");
        lsSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(lsSensorHistBtn);

        Button lsAlertHistBtn = actionBtn("🔔 Alert History", "btn-secondary");
        lsAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(lsAlertHistBtn);

        addActionGroupTitle("Settings");

        Button renameBtn = actionBtn("✏ Rename Zone", "btn-secondary");
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            lsData.setAll(zoneService.getLivestockZones());
            showLivestockDetail(z);
            refreshStats();
        }));
        actionsPanel.getChildren().add(renameBtn);

        addDangerZoneAction("🗑 Delete Zone", e -> confirmDeleteZone(z, () -> {
            lsData.setAll(zoneService.getLivestockZones());
            closeDetail();
            refreshStats();
        }));
    }

    private void showCropDetail(CropZONE z) {
        openDetailPanel(z);

        // ── DETAILS TAB ──────────────────────────────────────────────
        addRow("Code",         z.getCode());
        addRow("Name",         z.getName());
        addRow("Status",       z.getStatus().toString());
        addRow("Surface",      z.getSurfacePlanted() + " ha");
        addRow("Fields",       String.valueOf(z.getFields().size()));
        addRow("Total Yield",  String.format("%.2f kg", z.getTotalCropYield()));
        addRow("Env Sensors",  String.valueOf(z.getEnvSensors().size()));
        addRow("Soil Sensors", String.valueOf(z.getSoilSensors().size()));

        if (!z.getFields().isEmpty()) {
            addSectionTitle("Crops");
            for (Crop c : z.getFields()) {
                Label lbl = new Label(c.getVariety() + "  (" + c.getCropType()
                    + (c.wasHarvested() ? String.format(" · %.1f kg", c.getYieldKg()) : "") + ")");
                lbl.getStyleClass().add("detail-row");
                lbl.setWrapText(true);
                detailPanel.getChildren().add(lbl);

                // Growth stage pipeline — always shown (Fix 2)
                Node stagePipeline = buildCropStagePipeline(c);
                VBox.setMargin(stagePipeline, new Insets(6, 0, 6, 0));
                detailPanel.getChildren().add(stagePipeline);

                addRow("  ID",         c.getId().substring(0, 8));
                addRow("  Planted",    c.getPlantingDate().toString().substring(0, 10));
                addRow("  Harvest By", c.getExpectedHarvestDate().toString().substring(0, 10));
                if (c.wasHarvested() && c.getHarvestDate() != null)
                    addRow("  Harvested On", c.getHarvestDate().toString().substring(0, 10));
                addRow("  pH range",  c.getOptimalPHRange().getMin() + " – " + c.getOptimalPHRange().getMax());
                addRow("  Moisture",  c.getOptimalMoistureRange().getMin() + "% – " + c.getOptimalMoistureRange().getMax() + "%");
                // Standalone READY TO HARVEST badge removed — pipeline handles it (Fix 2)

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
                        warn.setTitle("Zone Boundary Required"); warn.setHeaderText(null);
                        warn.setContentText("Set the zone boundary first — the field sub-zone must fit inside it.");
                        warn.showAndWait(); return;
                    }
                    BoundaryEditorDialog dlg = new BoundaryEditorDialog(
                        c.getVariety() + " field",
                        c.hasBoundary() ? c.getBoundary() : null,
                        z.getBoundaries(), detailPanel.getScene().getStylesheets());
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
                    confirm.setTitle("Remove Crop"); confirm.setHeaderText(null);
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

        // ── ACTIONS TAB ──────────────────────────────────────────────
        addActionGroupTitle("Manage");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = actionBtn("⏸ Suspend Zone", "btn-secondary");
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); crData.setAll(zoneService.getCropZones()); showCropDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = actionBtn("▶ Activate Zone", "btn-primary");
            actBtn.setOnAction(e -> { zoneService.activateZone(z); crData.setAll(zoneService.getCropZones()); showCropDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(actBtn);
        }

        Button addCropBtn = actionBtn("➕ Add Crop", "btn-primary");
        addCropBtn.setOnAction(e -> showAddCropDialog(z));
        actionsPanel.getChildren().add(addCropBtn);

        Button setSurfaceBtn = actionBtn("📐 Set Surface Area", "btn-secondary");
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
        actionsPanel.getChildren().add(setSurfaceBtn);

        addActionGroupTitle("Sensors & Map");

        Button addEnvBtn = actionBtn("🌡 Add Environment Sensor", "btn-secondary");
        addEnvBtn.setOnAction(e -> showAddEnvSensorDialog(z));
        actionsPanel.getChildren().add(addEnvBtn);

        Button addSoilBtn = actionBtn("🌱 Add Soil Sensor", "btn-secondary");
        addSoilBtn.setOnAction(e -> showAddSoilSensorDialog(z));
        actionsPanel.getChildren().add(addSoilBtn);

        Button crBoundaryBtn = actionBtn(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary", "btn-secondary");
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
        actionsPanel.getChildren().add(crBoundaryBtn);

        Button crMapBtn = actionBtn("📍 View Zone Map", "btn-secondary");
        crMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(crMapBtn);

        addActionGroupTitle("History");

        Button crSensorHistBtn = actionBtn("📊 Sensor History", "btn-secondary");
        crSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(crSensorHistBtn);

        Button crAlertHistBtn = actionBtn("🔔 Alert History", "btn-secondary");
        crAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(crAlertHistBtn);

        addActionGroupTitle("Settings");

        Button renameBtn = actionBtn("✏ Rename Zone", "btn-secondary");
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            crData.setAll(zoneService.getCropZones());
            showCropDetail(z);
            refreshStats();
        }));
        actionsPanel.getChildren().add(renameBtn);

        addDangerZoneAction("🗑 Delete Zone", e -> confirmDeleteZone(z, () -> {
            crData.setAll(zoneService.getCropZones());
            closeDetail();
            refreshStats();
        }));
    }

    private void showAquaDetail(AquacultureZONE z) {
        openDetailPanel(z);

        // ── DETAILS TAB ──────────────────────────────────────────────
        addRow("Code",           z.getCode());
        addRow("Name",           z.getName());
        addRow("Status",         z.getStatus().toString());
        addRow("Species Groups", String.valueOf(z.getSpeciesList().size()));
        addRow("Total Fish",     String.valueOf(z.getTotalSpeciesCount()));
        addRow("Total Harvest",  String.format("%.2f kg", z.getTotalHarvestWeight()));
        addRow("Water Sensors",  String.valueOf(z.getWaterSensors().size()));

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
                        warn.setTitle("Zone Boundary Required"); warn.setHeaderText(null);
                        warn.setContentText("Set the zone boundary first — the tub sub-zone must fit inside it.");
                        warn.showAndWait(); return;
                    }
                    BoundaryEditorDialog dlg = new BoundaryEditorDialog(
                        s.getName() + " tub",
                        s.hasBoundary() ? s.getBoundary() : null,
                        z.getBoundaries(), detailPanel.getScene().getStylesheets());
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
                    confirm.setTitle("Remove Species"); confirm.setHeaderText(null);
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

        // ── ACTIONS TAB ──────────────────────────────────────────────
        addActionGroupTitle("Manage");

        if (z.getStatus() == ZoneStatus.ACTIVE) {
            Button suspBtn = actionBtn("⏸ Suspend Zone", "btn-secondary");
            suspBtn.setOnAction(e -> { zoneService.suspendZone(z); aquaData.setAll(zoneService.getAquacultureZones()); showAquaDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(suspBtn);
        } else {
            Button actBtn = actionBtn("▶ Activate Zone", "btn-primary");
            actBtn.setOnAction(e -> { zoneService.activateZone(z); aquaData.setAll(zoneService.getAquacultureZones()); showAquaDetail(z); refreshStats(); });
            actionsPanel.getChildren().add(actBtn);
        }

        Button addSpeciesBtn = actionBtn("➕ Add Species", "btn-primary");
        addSpeciesBtn.setOnAction(e -> showAddSpeciesDialog(z));
        actionsPanel.getChildren().add(addSpeciesBtn);

        addActionGroupTitle("Sensors & Map");

        Button addWaterBtn = actionBtn("💧 Add Water Sensor", "btn-secondary");
        addWaterBtn.setOnAction(e -> showAddWaterSensorDialog(z));
        actionsPanel.getChildren().add(addWaterBtn);

        Button aqBoundaryBtn = actionBtn(z.hasBoundaries()
            ? "🗺 Edit Boundary (" + z.getBoundaries().size() + " pts)"
            : "🗺 Set Zone Boundary", "btn-secondary");
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
        actionsPanel.getChildren().add(aqBoundaryBtn);

        Button aqMapBtn = actionBtn("📍 View Zone Map", "btn-secondary");
        aqMapBtn.setOnAction(e -> new ZoneMapDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(aqMapBtn);

        addActionGroupTitle("History");

        Button aqSensorHistBtn = actionBtn("📊 Sensor History", "btn-secondary");
        aqSensorHistBtn.setOnAction(e ->
            new SensorHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(aqSensorHistBtn);

        Button aqAlertHistBtn = actionBtn("🔔 Alert History", "btn-secondary");
        aqAlertHistBtn.setOnAction(e ->
            new ZoneAlertHistoryDialog(z, detailPanel.getScene().getStylesheets()).showAndWait());
        actionsPanel.getChildren().add(aqAlertHistBtn);

        addActionGroupTitle("Settings");

        Button renameBtn = actionBtn("✏ Rename Zone", "btn-secondary");
        renameBtn.setOnAction(e -> showRenameZoneDialog(z, () -> {
            aquaData.setAll(zoneService.getAquacultureZones());
            showAquaDetail(z);
            refreshStats();
        }));
        actionsPanel.getChildren().add(renameBtn);

        addDangerZoneAction("🗑 Delete Zone", e -> confirmDeleteZone(z, () -> {
            aquaData.setAll(zoneService.getAquacultureZones());
            closeDetail();
            refreshStats();
        }));
    }

    // ── Bio sensor row builder (with distress styling) ────────────────

    private HBox buildSensorRow(BioSensor sensor, LivestockZONE z) {
        boolean distress = sensor.isAnimalInDistress();

        Label typeLbl = new Label(sensor.getMeasureType() + " — " + sensor.getAnimal().getName());
        typeLbl.getStyleClass().add(distress ? "zones-sensor-name-alert" : "detail-value");
        typeLbl.setWrapText(true);

        Label codeLbl = new Label(sensor.getCode());
        codeLbl.getStyleClass().add(distress ? "zones-sensor-code-alert" : "text-muted");

        VBox infoCol = new VBox(2, typeLbl, codeLbl);
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> {
            z.removeBioSensor(sensor);
            FarmService.getInstance().autoSave();
            showLivestockDetail(z);
        });

        HBox row = new HBox(8, infoCol);

        if (distress) {
            Label badge = new Label("⚠ DISTRESS");
            badge.getStyleClass().addAll("badge", "badge-distress");
            row.getChildren().add(badge);
        }

        row.getChildren().add(removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(distress ? "zones-sensor-row-distress" : "zones-sensor-row");
        row.setPadding(new Insets(10, 10, 10, 10));
        return row;
    }

    // ── Crop growth stage pipeline builder (Fix 2) ────────────────────

    private Node buildCropStagePipeline(Crop c) {
        // Map 5 enum stages + 1 virtual "Harvested" state to 6 display nodes
        int currentOrdinal = c.getGrowthStage().ordinal(); // sowing=0 … harvest=4
        int currentIdx     = c.wasHarvested() ? 5 : currentOrdinal;

        String[] names = {"Sown", "Germination", "Growing", "Maturity", "Ready", "Harvested"};
        int total = 6;

        HBox stagesRow = new HBox(0);
        stagesRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < total; i++) {
            boolean isPast    = i < currentIdx;
            boolean isCurrent = i == currentIdx;
            boolean isAlert   = isCurrent && i == 4; // "Ready to harvest" urgency
            boolean isLast    = i == total - 1;
            boolean isFirst   = i == 0;

            // Dot (checkmark on Harvested node when that state is active)
            Label dot = new Label(isCurrent && isLast ? "✓" : "");
            dot.setMinWidth(14); dot.setMaxWidth(14);
            dot.setMinHeight(14); dot.setMaxHeight(14);
            dot.setAlignment(Pos.CENTER);
            dot.getStyleClass().add("crop-stage-dot");
            if      (isPast)    dot.getStyleClass().add("crop-stage-dot-done");
            else if (isAlert)   dot.getStyleClass().add("crop-stage-dot-alert");
            else if (isCurrent) dot.getStyleClass().add("crop-stage-dot-current");
            else                dot.getStyleClass().add("crop-stage-dot-future");

            // Left connector
            Region leftLine = new Region();
            leftLine.setPrefHeight(2); leftLine.setMinHeight(2); leftLine.setMaxHeight(2);
            HBox.setHgrow(leftLine, Priority.ALWAYS);
            leftLine.getStyleClass().add((isPast || isCurrent) && !isFirst
                ? "crop-stage-line-done" : "crop-stage-line-future");

            // Right connector
            Region rightLine = new Region();
            rightLine.setPrefHeight(2); rightLine.setMinHeight(2); rightLine.setMaxHeight(2);
            HBox.setHgrow(rightLine, Priority.ALWAYS);
            rightLine.getStyleClass().add(isPast && !isLast
                ? "crop-stage-line-done" : "crop-stage-line-future");

            // Assemble connector row: pad|dot|pad or line|dot|line
            HBox connRow = new HBox(0);
            connRow.setAlignment(Pos.CENTER);
            if (isFirst) {
                Region pad = new Region(); HBox.setHgrow(pad, Priority.ALWAYS);
                connRow.getChildren().addAll(pad, dot, rightLine);
            } else if (isLast) {
                Region pad = new Region(); HBox.setHgrow(pad, Priority.ALWAYS);
                connRow.getChildren().addAll(leftLine, dot, pad);
            } else {
                connRow.getChildren().addAll(leftLine, dot, rightLine);
            }

            // Stage label
            Label lbl = new Label(names[i]);
            lbl.getStyleClass().add("crop-stage-lbl");
            if      (isPast)    lbl.getStyleClass().add("crop-stage-lbl-done");
            else if (isAlert)   lbl.getStyleClass().add("crop-stage-lbl-alert");
            else if (isCurrent) lbl.getStyleClass().add("crop-stage-lbl-current");
            else                lbl.getStyleClass().add("crop-stage-lbl-future");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);

            VBox stageVBox = new VBox(4, connRow, lbl);
            stageVBox.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(stageVBox, Priority.ALWAYS);
            stagesRow.getChildren().add(stageVBox);
        }

        VBox pipeline = new VBox(0, stagesRow);
        pipeline.getStyleClass().add("crop-stage-pipeline");
        pipeline.setPadding(new Insets(10, 8, 12, 8));
        return pipeline;
    }

    // ── Add animal to zone dialog (Fix 4) — same logic as AnimalsController but zone pre-filled

    private void showAddAnimalInZoneDialog(LivestockZONE zone) {
        TextField nameField    = new TextField(); nameField.setPromptText("e.g. Bessie");
        TextField speciesField = new TextField(); speciesField.setPromptText("e.g. Cow, Sheep");
        TextField ageField     = new TextField("1");
        TextField weightField  = new TextField("100.0");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("RUMINANT", "POULTRY");
        typeCombo.setValue(zone.getType().toString());

        VBox form = new VBox(14,
            formGroup("Name",        nameField),
            formGroup("Species",     speciesField),
            formGroup("Type",        typeCombo),
            formGroup("Age (years)", ageField),
            formGroup("Weight (kg)", weightField)
        );
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Animal> dialog = new Dialog<>();
        dialog.setTitle("Add Animal");
        dialog.setHeaderText("Add animal to \"" + zone.getName() + "\"");
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
                return animalService.addAnimal(
                    nameField.getText().trim(),
                    speciesField.getText().trim(),
                    LIvestockType.valueOf(typeCombo.getValue()),
                    Integer.parseInt(ageField.getText().trim()),
                    Double.parseDouble(weightField.getText().trim()),
                    zone);
            } catch (NumberFormatException e) { return null; }
        });

        dialog.showAndWait().ifPresent(animal -> {
            lsData.setAll(zoneService.getLivestockZones());
            showLivestockDetail(zone);
            refreshStats();
        });
    }

    // ── Action tab helpers ────────────────────────────────────────────

    private Button actionBtn(String text, String styleClass) {
        Button btn = new Button(text);
        // primary actions keep btn-primary; all others use the Animals tile style
        if ("btn-primary".equals(styleClass)) {
            btn.getStyleClass().add("btn-primary");
        } else {
            btn.getStyleClass().add("animals-action-tile");
        }
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    private void addActionGroupTitle(String title) {
        if (!actionsPanel.getChildren().isEmpty()) {
            Separator sep = new Separator();
            sep.setPrefHeight(1);
            actionsPanel.getChildren().add(sep);
        }
        Label lbl = new Label(title.toUpperCase());
        lbl.getStyleClass().add("zones-action-group-label");
        lbl.setMaxWidth(Double.MAX_VALUE);
        actionsPanel.getChildren().add(lbl);
    }

    private void addDangerZoneAction(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Separator sep = new Separator();
        sep.setPrefHeight(1);
        actionsPanel.getChildren().add(sep);

        Label dangerLbl = new Label("DANGER ZONE");
        dangerLbl.getStyleClass().add("animals-danger-section-label");
        actionsPanel.getChildren().add(dangerLbl);

        Button btn = new Button(text);
        btn.getStyleClass().add("animals-action-tile-danger");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(handler);
        actionsPanel.getChildren().add(btn);
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
                double kg = arr[0] / 100.0; int cnt = arr[1];
                s.harvest(kg, cnt);
                FarmService.getInstance().autoSave();
                aquaData.setAll(zoneService.getAquacultureZones());
                showAquaDetail(zone);
            } catch (IllegalArgumentException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Harvest"); err.setHeaderText(null);
                err.setContentText(ex.getMessage()); err.showAndWait();
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
                err.setTitle("Invalid Input"); err.setHeaderText(null);
                err.setContentText(ex.getMessage()); err.showAndWait();
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

    // ── Edit Feeding Program ──────────────────────────────────────────

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
                fp.setFoodType(foodField.getText().trim());
                fp.setQuantity(Double.parseDouble(quantityField.getText().trim()));
                fp.setSchedule(Arrays.asList(scheduleField.getText().trim().split("\\s*,\\s*")));
                return Boolean.TRUE;
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input"); err.setHeaderText(null);
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
        if (input instanceof ComboBox<?> cb) {
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.getStyleClass().setAll("combo-box", "dialog-form-combo");
        }
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

        String title = dialog.getTitle() == null ? "" : dialog.getTitle();
        String icon  = "🌿";
        if      (title.contains("Animal"))   icon = "🐄";
        else if (title.contains("Zone"))     icon = "📍";
        else if (title.contains("Sensor"))   icon = "📡";
        else if (title.contains("Feeding"))  icon = "🍽";
        else if (title.contains("Rename"))   icon = "✏";
        else if (title.contains("Crop"))     icon = "🌾";
        else if (title.contains("Species") || title.contains("Aqua")) icon = "🐟";
        else if (title.contains("Harvest") || title.contains("Mortality") || title.contains("Restock")) icon = "📦";
        else if (title.contains("Inject") || title.contains("Reading")) icon = "📊";
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        dialog.setGraphic(iconLbl);
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
        addSectionTitleWithButton(title, null);
    }

    private void addSectionTitleWithButton(String title, Node actionBtn) {
        HBox header = new HBox(8);
        header.getStyleClass().add("zones-section-header");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(16, 0, 10, 0));
        Label lbl = new Label(title.toUpperCase());
        lbl.getStyleClass().add("zones-section-header-text");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        header.getChildren().add(lbl);
        if (actionBtn != null) header.getChildren().add(actionBtn);
        detailPanel.getChildren().add(header);
    }

    // ── Toolbar actions (preserved @FXML handlers) ────────────────────

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
