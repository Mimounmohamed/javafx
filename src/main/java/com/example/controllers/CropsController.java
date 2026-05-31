package com.example.controllers;

import Entities.Crop;
import Entities.CropType;
import Entities.GrowthStage;
import ZONES.CropZONE;
import com.example.services.FarmService;
import com.example.services.ZoneService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CropsController {

    // ── Table ─────────────────────────────────────────────────────────
    @FXML private TableView<Crop>            cropTable;
    @FXML private TableColumn<Crop, String>  colCropId;
    @FXML private TableColumn<Crop, String>  colVariety;
    @FXML private TableColumn<Crop, String>  colType;
    @FXML private TableColumn<Crop, String>  colStage;
    @FXML private TableColumn<Crop, String>  colCropZone;
    @FXML private TableColumn<Crop, String>  colYield;

    // ── Filters ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> filterZone;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStage;
    @FXML private TextField        searchField;

    // ── Stat labels ───────────────────────────────────────────────────
    @FXML private Label statTotalCrops;
    @FXML private Label statHarvestReady;
    @FXML private Label statGrowing;
    @FXML private Label statCropZones;

    // ── Detail panels ─────────────────────────────────────────────────
    @FXML private VBox detailPanel;       // Info tab content
    @FXML private VBox detailHeaderPanel; // dynamic header above tabs
    @FXML private VBox yieldPanel;        // Yield tab content
    @FXML private VBox actionsPanel;      // Actions tab content

    private ObservableList<Crop> allCrops;
    private FilteredList<Crop>   filteredCrops;

    private final ZoneService zoneService = ZoneService.getInstance();

    // ── Initialise ────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        List<Crop> crops = new ArrayList<>();
        for (CropZONE zone : zoneService.getCropZones())
            crops.addAll(zone.getFields());

        allCrops      = FXCollections.observableArrayList(crops);
        filteredCrops = new FilteredList<>(allCrops, p -> true);

        setupTable();
        setupFilters();
        refreshStats();
    }

    private void refreshStats() {
        long harvestReady = allCrops.stream().filter(Crop::isReadyForHarvest).count();
        long growing      = allCrops.stream()
            .filter(c -> !c.isReadyForHarvest() && c.getGrowthStage() != GrowthStage.sowing)
            .count();
        long cropZones    = zoneService.getCropZones().size();

        statTotalCrops.setText(String.valueOf(allCrops.size()));
        statHarvestReady.setText(String.valueOf(harvestReady));
        statGrowing.setText(String.valueOf(growing));
        statCropZones.setText(String.valueOf(cropZones));
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupTable() {
        colCropId.setCellValueFactory(d   -> new SimpleStringProperty(
            "#" + d.getValue().getId().substring(0, 4).toUpperCase()));
        colVariety.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getVariety()));
        colType.setCellValueFactory(d     -> new SimpleStringProperty(d.getValue().getCropType().toString()));
        colStage.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getGrowthStage().toString()));
        colCropZone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getZone().getName()));
        colYield.setCellValueFactory(d    -> new SimpleStringProperty(
            String.format("%.1f", d.getValue().getYieldKg())));

        // ID column — monospace muted
        colCropId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: #888780;");
            }
        });

        // Variety column — bold
        colVariety.setCellFactory(col -> new TableCell<>() {
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

        // Stage column — coloured badge
        colStage.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Crop c = (Crop) getTableRow().getItem();
                badge.setText(c.getGrowthStage().toString());
                badge.getStyleClass().removeIf(s -> s.startsWith("badge-"));
                badge.getStyleClass().add(stageCss(c.getGrowthStage()));
                setGraphic(badge);
            }
        });

        cropTable.setItems(filteredCrops);
        cropTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, n) -> { if (n != null) showDetail(n); });
    }

    private String stageCss(GrowthStage s) {
        return switch (s) {
            case harvest    -> "badge-healthy";
            case maturity   -> "badge-quarantined";
            case sowing     -> "badge-sick";
            default         -> "badge-healthy";
        };
    }

    // ── Filter setup ──────────────────────────────────────────────────

    private void setupFilters() {
        filterZone.getItems().add("All Zones");
        zoneService.getCropZones().forEach(z -> filterZone.getItems().add(z.getName()));
        filterZone.setValue("All Zones");

        filterType.getItems().add("All Types");
        for (CropType t : CropType.values())
            filterType.getItems().add(t.toString());
        filterType.setValue("All Types");

        filterStage.getItems().add("All Stages");
        for (GrowthStage g : GrowthStage.values())
            filterStage.getItems().add(g.toString());
        filterStage.setValue("All Stages");

        filterZone.setOnAction(e  -> applyFilter());
        filterType.setOnAction(e  -> applyFilter());
        filterStage.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, old, n) -> applyFilter());
    }

    private void applyFilter() {
        String zone   = filterZone.getValue();
        String type   = filterType.getValue();
        String stage  = filterStage.getValue();
        String search = searchField.getText().toLowerCase();
        filteredCrops.setPredicate(c -> {
            boolean zoneOk   = "All Zones".equals(zone)   || c.getZone().getName().equals(zone);
            boolean typeOk   = "All Types".equals(type)   || c.getCropType().toString().equals(type);
            boolean stageOk  = "All Stages".equals(stage) || c.getGrowthStage().toString().equals(stage);
            boolean searchOk = search.isEmpty()
                || c.getVariety().toLowerCase().contains(search)
                || c.getZone().getName().toLowerCase().contains(search);
            return zoneOk && typeOk && stageOk && searchOk;
        });
    }

    // ── Add Crop dialog ───────────────────────────────────────────────

    @FXML
    private void showAddCropDialog() {
        List<CropZONE> zones = zoneService.getCropZones();
        if (zones.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Crop Zone");
            alert.setHeaderText(null);
            alert.setContentText("Create a Crop zone first before adding crops.");
            alert.showAndWait();
            return;
        }
        String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();
        // Let user pick zone via ComboBox in a simple wrapper dialog
        ComboBox<CropZONE> zoneCombo = new ComboBox<>();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.setValue(zones.get(0));
        zoneCombo.setMaxWidth(Double.MAX_VALUE);
        zoneCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        Dialog<CropZONE> zonePicker = new Dialog<>();
        zonePicker.setTitle("Select Crop Zone");
        zonePicker.setHeaderText(null);
        VBox zoneForm = new VBox(8, new Label("Zone") {{ getStyleClass().add("az-form-label"); }}, zoneCombo);
        zoneForm.setPadding(new Insets(20, 24, 8, 24));
        zonePicker.getDialogPane().setContent(new VBox(0, dialogHeader("🌾", "Select Crop Zone", "Choose the zone to add the crop to"), zoneForm));
        zonePicker.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        zonePicker.getDialogPane().setMinWidth(360);
        zonePicker.getDialogPane().getStylesheets().add(css);
        ((Button) zonePicker.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) zonePicker.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
        zonePicker.setResultConverter(bt -> bt == ButtonType.OK ? zoneCombo.getValue() : null);
        if (cropTable.getScene() != null)
            zonePicker.initOwner(cropTable.getScene().getWindow());

        zonePicker.showAndWait().ifPresent(selectedZone -> {
            AddCropDialog dialog = new AddCropDialog(css, selectedZone);
            if (cropTable.getScene() != null)
                dialog.initOwner(cropTable.getScene().getWindow());
            dialog.showAndWait().ifPresent(crop -> {
                selectedZone.addField(crop);
                FarmService.getInstance().autoSave();
                allCrops.add(crop);
                if (!filterZone.getItems().contains(crop.getZone().getName()))
                    filterZone.getItems().add(crop.getZone().getName());
                refreshStats();
            });
        });
    }

    // ── Detail panel ──────────────────────────────────────────────────

    private void showDetail(Crop c) {
        detailHeaderPanel.getChildren().clear();
        detailPanel.getChildren().clear();
        yieldPanel.getChildren().clear();
        actionsPanel.getChildren().clear();

        buildDetailHeader(c);
        buildInfoTab(c);
        buildYieldTab(c);
        buildActionsTab(c);
    }

    // ── Detail header ─────────────────────────────────────────────────

    private void buildDetailHeader(Crop c) {
        // Avatar circle
        Label avatar = new Label(c.getVariety().substring(0, 1).toUpperCase());
        avatar.getStyleClass().addAll("animals-avatar", avatarCssForStage(c.getGrowthStage()));

        // Name + subtitle
        Label nameLbl = new Label(c.getVariety());
        nameLbl.getStyleClass().add("animals-detail-name");
        Label subtitleLbl = new Label(
            c.getCropType() + " · " + c.getGrowthStage() + " · " + c.getZone().getName());
        subtitleLbl.getStyleClass().add("animals-detail-subtitle");
        VBox nameBox = new VBox(2, nameLbl, subtitleLbl);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox heroRow = new HBox(10, avatar, nameBox);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        // Stage pill
        Label stagePill = new Label(c.getGrowthStage().toString());
        stagePill.getStyleClass().addAll("badge", stageCss(c.getGrowthStage()));

        // Ghost action buttons
        HBox actionBtns = new HBox(6);
        Button harvestBtn = new Button("Record Harvest");
        harvestBtn.getStyleClass().add("btn-ghost");
        harvestBtn.setOnAction(e -> showRecordHarvestDialog(c));

        Button stageBtn = new Button("Update Stage");
        stageBtn.getStyleClass().add("btn-ghost");
        stageBtn.setOnAction(e -> showUpdateStageDialog(c));

        Button mapBtn = new Button("📍 Zone Map");
        mapBtn.getStyleClass().add("btn-ghost");
        mapBtn.setOnAction(e -> openZoneMap(c));

        actionBtns.getChildren().addAll(harvestBtn, stageBtn, mapBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(8, stagePill, spacer, actionBtns);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        detailHeaderPanel.setSpacing(10);
        detailHeaderPanel.setPadding(new Insets(14, 16, 12, 16));
        detailHeaderPanel.getChildren().addAll(heroRow, statusRow);
    }

    private String avatarCssForStage(GrowthStage s) {
        return switch (s) {
            case harvest, maturity   -> "animals-avatar-healthy";
            case growth, germination -> "animals-avatar-sick";
            default                  -> "animals-avatar-quarantined";
        };
    }

    // ── Info tab ──────────────────────────────────────────────────────

    private void buildInfoTab(Crop c) {
        addSectionTitle(detailPanel, "Crop details");
        addRow(detailPanel, "Variety", c.getVariety());
        addRow(detailPanel, "Type",    c.getCropType().toString());
        addRow(detailPanel, "Stage",   c.getGrowthStage().toString());
        addRow(detailPanel, "Zone",    c.getZone().getName());
        addRow(detailPanel, "Ready",   c.isReadyForHarvest() ? "Yes" : "No");

        addSectionTitle(detailPanel, "Dates");
        addRow(detailPanel, "Planted",           c.getPlantingDate().toString());
        addRow(detailPanel, "Expected harvest",   c.getExpectedHarvestDate().toString());
        if (c.getHarvestDate() != null)
            addRow(detailPanel, "Actual harvest", c.getHarvestDate().toString());

        addSectionTitle(detailPanel, "Soil requirements");
        addRow(detailPanel, "pH range",
            String.format("%.1f – %.1f", c.getOptimalPHRange().getMin(), c.getOptimalPHRange().getMax()));
        addRow(detailPanel, "Moisture range",
            String.format("%.1f%% – %.1f%%", c.getOptimalMoistureRange().getMin(), c.getOptimalMoistureRange().getMax()));

        addSectionTitle(detailPanel, "Yield");
        addRow(detailPanel, "Total yield", String.format("%.1f kg", c.getYieldKg()));
    }

    // ── Yield tab ─────────────────────────────────────────────────────

    private void buildYieldTab(Crop c) {
        if (c.getYieldHistory().isEmpty()) {
            Label none = new Label("No harvest records yet.");
            none.getStyleClass().add("text-muted");
            yieldPanel.getChildren().add(none);
            return;
        }
        addSectionTitle(yieldPanel, "Harvest records");
        for (Crop.YieldRecord r : c.getYieldHistory()) {
            addRow(yieldPanel,
                r.getDate().toString().substring(0, 10),
                String.format("%.2f kg", r.getKg()));
        }
    }

    // ── Actions tab ───────────────────────────────────────────────────

    private void buildActionsTab(Crop c) {
        addSectionTitle(actionsPanel, "Record data");

        Button harvestBtn = actionTile("🌾 Record Harvest");
        harvestBtn.setOnAction(e -> showRecordHarvestDialog(c));
        actionsPanel.getChildren().add(harvestBtn);

        Button stageBtn = actionTile("🔄 Update Growth Stage");
        stageBtn.setOnAction(e -> showUpdateStageDialog(c));
        actionsPanel.getChildren().add(stageBtn);

        actionsPanel.getChildren().add(new Separator());
        addSectionTitle(actionsPanel, "Manage");

        Button mapBtn = actionTile("📍 Zone Map");
        mapBtn.setOnAction(e -> openZoneMap(c));
        actionsPanel.getChildren().add(mapBtn);

        // Danger zone
        actionsPanel.getChildren().add(new Separator());
        Label dangerLbl = new Label("DANGER ZONE");
        dangerLbl.getStyleClass().add("animals-danger-section-label");
        actionsPanel.getChildren().add(dangerLbl);

        Button removeBtn = new Button("❌ Remove crop");
        removeBtn.getStyleClass().add("animals-action-tile-danger");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setAlignment(Pos.CENTER_LEFT);
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Crop");
            confirm.setHeaderText(null);
            confirm.setContentText("Permanently remove \"" + c.getVariety() + "\" from the farm?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    if (c.getZone() instanceof CropZONE cz) cz.removeField(c);
                    FarmService.getInstance().autoSave();
                    allCrops.remove(c);
                    detailHeaderPanel.getChildren().clear();
                    detailPanel.getChildren().clear();
                    yieldPanel.getChildren().clear();
                    actionsPanel.getChildren().clear();
                    Label placeholder = new Label("Select a crop to view details");
                    placeholder.getStyleClass().add("text-muted");
                    detailPanel.getChildren().add(placeholder);
                    refreshStats();
                }
            });
        });
        actionsPanel.getChildren().add(removeBtn);
    }

    // ── Inline dialogs ────────────────────────────────────────────────

    private void showRecordHarvestDialog(Crop c) {
        TextField kgField = new TextField("0.0");
        kgField.getStyleClass().setAll("dialog-form-field");
        kgField.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label("Harvest yield (kg)");
        lbl.getStyleClass().add("dialog-form-label");
        VBox form = new VBox(8, lbl, kgField);
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Record Harvest");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0, dialogHeader("🌾", "Record Harvest", "Recording for \"" + c.getVariety() + "\""), form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        styleDlg(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(kgField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        if (cropTable.getScene() != null) dialog.initOwner(cropTable.getScene().getWindow());
        dialog.showAndWait().ifPresent(kg -> {
            if (kg >= 0) {
                c.recordHarvest(kg);
                FarmService.getInstance().autoSave();
                showDetail(c);
            }
        });
    }

    private void showUpdateStageDialog(Crop c) {
        ComboBox<GrowthStage> stageCombo = new ComboBox<>();
        stageCombo.getItems().addAll(GrowthStage.values());
        stageCombo.setValue(c.getGrowthStage());
        stageCombo.setMaxWidth(Double.MAX_VALUE);
        stageCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");
        Label lbl = new Label("Growth Stage");
        lbl.getStyleClass().add("dialog-form-label");
        VBox form = new VBox(8, lbl, stageCombo);
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<GrowthStage> dialog = new Dialog<>();
        dialog.setTitle("Update Growth Stage");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0, dialogHeader("🌱", "Update Growth Stage", "Updating stage for \"" + c.getVariety() + "\""), form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        styleDlg(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Update");
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? stageCombo.getValue() : null);
        if (cropTable.getScene() != null) dialog.initOwner(cropTable.getScene().getWindow());
        dialog.showAndWait().ifPresent(stage -> {
            c.updateGrowthStage(stage);
            FarmService.getInstance().autoSave();
            refreshStats();
            showDetail(c);
        });
    }

    // ── Zone map ──────────────────────────────────────────────────────

    private void openZoneMap(Crop c) {
        List<String> sheets = cropTable.getScene() == null
            ? new ArrayList<>()
            : new ArrayList<>(cropTable.getScene().getStylesheets());
        ZoneMapDialog dlg = new ZoneMapDialog(c.getZone(), sheets, ZoneMapDialog.key(c));
        if (cropTable.getScene() != null)
            dlg.initOwner(cropTable.getScene().getWindow());
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

    // ── Dialog style helpers ──────────────────────────────────────────

    private HBox dialogHeader(String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-custom-header-icon");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-custom-header-title");
        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("dialog-custom-header-sub");
        VBox textBox = new VBox(2, titleLbl, subLbl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, iconLbl, textBox, spacer);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }

    private void styleDlg(Dialog<?> d) {
        var sheets = cropTable.getScene() == null ? null : cropTable.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0) : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        d.getDialogPane().getStylesheets().add(css);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        Button cancel = (Button) d.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (ok != null) ok.getStyleClass().add("btn-primary");
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");
    }
}
