package com.example.controllers;

import Entities.AquacultureSpecies;
import ZONES.AquacultureZONE;
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

public class AquacultureController {

    // ── Table ─────────────────────────────────────────────────────────
    @FXML private TableView<AquacultureSpecies>            speciesTable;
    @FXML private TableColumn<AquacultureSpecies, String>  colSpeciesId;
    @FXML private TableColumn<AquacultureSpecies, String>  colName;
    @FXML private TableColumn<AquacultureSpecies, String>  colAquaZone;
    @FXML private TableColumn<AquacultureSpecies, String>  colStock;
    @FXML private TableColumn<AquacultureSpecies, String>  colHarvested;
    @FXML private TableColumn<AquacultureSpecies, String>  colSurvival;

    // ── Filters ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> filterZone;
    @FXML private TextField        searchField;

    // ── Stat labels ───────────────────────────────────────────────────
    @FXML private Label statTotalSpecies;
    @FXML private Label statTotalStock;
    @FXML private Label statHarvested;
    @FXML private Label statAquaZones;

    // ── Detail panels ─────────────────────────────────────────────────
    @FXML private VBox detailPanel;       // Info tab content
    @FXML private VBox detailHeaderPanel; // dynamic header above tabs
    @FXML private VBox harvestPanel;      // Harvest tab content
    @FXML private VBox actionsPanel;      // Actions tab content

    private ObservableList<AquacultureSpecies> allSpecies;
    private FilteredList<AquacultureSpecies>   filteredSpecies;

    private final ZoneService zoneService = ZoneService.getInstance();

    // ── Initialise ────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        List<AquacultureSpecies> species = new ArrayList<>();
        for (AquacultureZONE zone : zoneService.getAquacultureZones())
            species.addAll(zone.getSpeciesList());

        allSpecies      = FXCollections.observableArrayList(species);
        filteredSpecies = new FilteredList<>(allSpecies, p -> true);

        setupTable();
        setupFilters();
        refreshStats();
    }

    private void refreshStats() {
        long totalStock    = allSpecies.stream().mapToLong(AquacultureSpecies::getNumSpecies).sum();
        long totalHarvested = allSpecies.stream().mapToLong(AquacultureSpecies::getTotalHarvestedCount).sum();
        long aquaZones     = zoneService.getAquacultureZones().size();

        statTotalSpecies.setText(String.valueOf(allSpecies.size()));
        statTotalStock.setText(String.valueOf(totalStock));
        statHarvested.setText(String.valueOf(totalHarvested));
        statAquaZones.setText(String.valueOf(aquaZones));
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupTable() {
        colSpeciesId.setCellValueFactory(d  -> new SimpleStringProperty(
            "#" + d.getValue().getId().substring(0, 4).toUpperCase()));
        colName.setCellValueFactory(d       -> new SimpleStringProperty(d.getValue().getName()));
        colAquaZone.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue().getZone().getName()));
        colStock.setCellValueFactory(d      -> new SimpleStringProperty(
            String.valueOf(d.getValue().getNumSpecies())));
        colHarvested.setCellValueFactory(d  -> new SimpleStringProperty(
            String.valueOf(d.getValue().getTotalHarvestedCount())));
        colSurvival.setCellValueFactory(d   -> new SimpleStringProperty(
            String.format("%.1f%%", d.getValue().getOverallSurvivalRatePercent())));

        // ID column — monospace muted
        colSpeciesId.setCellFactory(col -> new TableCell<>() {
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

        // Stock column — colored by count
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle(""); return;
                }
                AquacultureSpecies s = (AquacultureSpecies) getTableRow().getItem();
                int count = s.getNumSpecies();
                setText(String.valueOf(count));
                if (count > 50)
                    setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                else if (count >= 1)
                    setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            }
        });

        speciesTable.setItems(filteredSpecies);
        speciesTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, n) -> { if (n != null) showDetail(n); });
    }

    // ── Filter setup ──────────────────────────────────────────────────

    private void setupFilters() {
        filterZone.getItems().add("All Zones");
        zoneService.getAquacultureZones().forEach(z -> filterZone.getItems().add(z.getName()));
        filterZone.setValue("All Zones");

        filterZone.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, old, n) -> applyFilter());
    }

    private void applyFilter() {
        String zone   = filterZone.getValue();
        String search = searchField.getText().toLowerCase();
        filteredSpecies.setPredicate(s -> {
            boolean zoneOk   = "All Zones".equals(zone) || s.getZone().getName().equals(zone);
            boolean searchOk = search.isEmpty()
                || s.getName().toLowerCase().contains(search)
                || s.getZone().getName().toLowerCase().contains(search);
            return zoneOk && searchOk;
        });
    }

    // ── Add Species dialog ────────────────────────────────────────────

    @FXML
    private void showAddSpeciesDialog() {
        List<AquacultureZONE> zones = zoneService.getAquacultureZones();
        if (zones.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Aquaculture Zone");
            alert.setHeaderText(null);
            alert.setContentText("Create an Aquaculture zone first before adding species.");
            alert.showAndWait();
            return;
        }
        String css = getClass().getResource("/com/example/styles/main.css").toExternalForm();

        ComboBox<AquacultureZONE> zoneCombo = new ComboBox<>();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.setValue(zones.get(0));
        zoneCombo.setMaxWidth(Double.MAX_VALUE);
        zoneCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        Dialog<AquacultureZONE> zonePicker = new Dialog<>();
        zonePicker.setTitle("Select Aquaculture Zone");
        zonePicker.setHeaderText(null);
        VBox zoneForm = new VBox(8, new Label("Zone") {{ getStyleClass().add("az-form-label"); }}, zoneCombo);
        zoneForm.setPadding(new Insets(20, 24, 8, 24));
        zonePicker.getDialogPane().setContent(new VBox(0, dialogHeader("🐟", "Select Aquaculture Zone", "Choose the zone to add the species to"), zoneForm));
        zonePicker.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        zonePicker.getDialogPane().setMinWidth(360);
        zonePicker.getDialogPane().getStylesheets().add(css);
        ((Button) zonePicker.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) zonePicker.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
        zonePicker.setResultConverter(bt -> bt == ButtonType.OK ? zoneCombo.getValue() : null);
        if (speciesTable.getScene() != null)
            zonePicker.initOwner(speciesTable.getScene().getWindow());

        zonePicker.showAndWait().ifPresent(selectedZone -> {
            AddSpeciesDialog dialog = new AddSpeciesDialog(css, selectedZone);
            if (speciesTable.getScene() != null)
                dialog.initOwner(speciesTable.getScene().getWindow());
            dialog.showAndWait().ifPresent(species -> {
                selectedZone.addSpecies(species);
                FarmService.getInstance().autoSave();
                allSpecies.add(species);
                if (!filterZone.getItems().contains(species.getZone().getName()))
                    filterZone.getItems().add(species.getZone().getName());
                refreshStats();
            });
        });
    }

    // ── Detail panel ──────────────────────────────────────────────────

    private void showDetail(AquacultureSpecies s) {
        detailHeaderPanel.getChildren().clear();
        detailPanel.getChildren().clear();
        harvestPanel.getChildren().clear();
        actionsPanel.getChildren().clear();

        buildDetailHeader(s);
        buildInfoTab(s);
        buildHarvestTab(s);
        buildActionsTab(s);
    }

    // ── Detail header ─────────────────────────────────────────────────

    private void buildDetailHeader(AquacultureSpecies s) {
        // Avatar circle
        Label avatar = new Label(s.getName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().addAll("animals-avatar", "animals-avatar-healthy");

        // Name + subtitle
        Label nameLbl = new Label(s.getName());
        nameLbl.getStyleClass().add("animals-detail-name");
        Label subtitleLbl = new Label(
            s.getZone().getName() + " · " + s.getNumSpecies() + " alive");
        subtitleLbl.getStyleClass().add("animals-detail-subtitle");
        VBox nameBox = new VBox(2, nameLbl, subtitleLbl);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox heroRow = new HBox(10, avatar, nameBox);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        // Stock badge
        Label stockBadge = new Label(s.getNumSpecies() + " alive");
        stockBadge.getStyleClass().addAll("badge",
            s.getNumSpecies() > 50 ? "badge-healthy"
            : s.getNumSpecies() >= 1 ? "badge-quarantined"
            : "badge-sick");

        // Ghost action buttons
        HBox actionBtns = new HBox(6);
        Button harvestBtn = new Button("Harvest");
        harvestBtn.getStyleClass().add("btn-ghost");
        harvestBtn.setOnAction(e -> showHarvestDialog(s));

        Button mortalityBtn = new Button("Mortality");
        mortalityBtn.getStyleClass().add("btn-ghost-danger");
        mortalityBtn.setOnAction(e -> showMortalityDialog(s));

        Button restockBtn = new Button("Restock");
        restockBtn.getStyleClass().add("btn-ghost");
        restockBtn.setOnAction(e -> showRestockDialog(s));

        Button mapBtn = new Button("📍 Zone Map");
        mapBtn.getStyleClass().add("btn-ghost");
        mapBtn.setOnAction(e -> openZoneMap(s));
        actionBtns.getChildren().addAll(harvestBtn, mortalityBtn, restockBtn, mapBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(8, stockBadge, spacer, actionBtns);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        detailHeaderPanel.setSpacing(10);
        detailHeaderPanel.setPadding(new Insets(14, 16, 12, 16));
        detailHeaderPanel.getChildren().addAll(heroRow, statusRow);
    }

    // ── Info tab ──────────────────────────────────────────────────────

    private void buildInfoTab(AquacultureSpecies s) {
        addSectionTitle(detailPanel, "Species details");
        addRow(detailPanel, "Name",              s.getName());
        addRow(detailPanel, "Zone",              s.getZone().getName());
        addRow(detailPanel, "Current stock",     String.valueOf(s.getNumSpecies()));
        addRow(detailPanel, "Initial count",     String.valueOf(s.getInitialTotalIndividuals()));
        addRow(detailPanel, "Total harvested",   String.valueOf(s.getTotalHarvestedCount()));
        addRow(detailPanel, "Survival rate",
            String.format("%.1f%%", s.getOverallSurvivalRatePercent()));
        addRow(detailPanel, "Total mortality",   String.valueOf(s.getTotalMortality()));
    }

    // ── Harvest tab ───────────────────────────────────────────────────

    private void buildHarvestTab(AquacultureSpecies s) {
        if (s.getHarvestHistory().isEmpty()) {
            Label none = new Label("No harvest records yet.");
            none.getStyleClass().add("text-muted");
            harvestPanel.getChildren().add(none);
            return;
        }
        addSectionTitle(harvestPanel, "Harvest records");
        for (AquacultureSpecies.HarvestRecord r : s.getHarvestHistory()) {
            String entry = String.format("%d harvested — %.2f kg",
                r.getCountHarvested(), r.getWeightKg());
            addRow(harvestPanel, r.getDate().toString().substring(0, 10), entry);
        }
    }

    // ── Actions tab ───────────────────────────────────────────────────

    private void buildActionsTab(AquacultureSpecies s) {
        addSectionTitle(actionsPanel, "Record data");

        Button harvestBtn = actionTile("🎣 Record Harvest");
        harvestBtn.setOnAction(e -> showHarvestDialog(s));
        actionsPanel.getChildren().add(harvestBtn);

        Button mortalityBtn = actionTile("💀 Record Mortality");
        mortalityBtn.setOnAction(e -> showMortalityDialog(s));
        actionsPanel.getChildren().add(mortalityBtn);

        Button restockBtn = actionTile("🔄 Restock");
        restockBtn.setOnAction(e -> showRestockDialog(s));
        actionsPanel.getChildren().add(restockBtn);

        actionsPanel.getChildren().add(new Separator());
        addSectionTitle(actionsPanel, "Manage");

        Button mapBtn = actionTile("📍 Zone Map");
        mapBtn.setOnAction(e -> openZoneMap(s));
        actionsPanel.getChildren().add(mapBtn);

        // Danger zone
        actionsPanel.getChildren().add(new Separator());
        Label dangerLbl = new Label("DANGER ZONE");
        dangerLbl.getStyleClass().add("animals-danger-section-label");
        actionsPanel.getChildren().add(dangerLbl);

        Button removeBtn = new Button("❌ Remove species");
        removeBtn.getStyleClass().add("animals-action-tile-danger");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setAlignment(Pos.CENTER_LEFT);
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Species");
            confirm.setHeaderText(null);
            confirm.setContentText("Permanently remove \"" + s.getName() + "\" from the farm?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    if (s.getZone() instanceof AquacultureZONE az) az.removeSpecies(s);
                    FarmService.getInstance().autoSave();
                    allSpecies.remove(s);
                    detailHeaderPanel.getChildren().clear();
                    detailPanel.getChildren().clear();
                    harvestPanel.getChildren().clear();
                    actionsPanel.getChildren().clear();
                    Label placeholder = new Label("Select a species to view details");
                    placeholder.getStyleClass().add("text-muted");
                    detailPanel.getChildren().add(placeholder);
                    refreshStats();
                }
            });
        });
        actionsPanel.getChildren().add(removeBtn);
    }

    // ── Inline dialogs ────────────────────────────────────────────────

    private void showHarvestDialog(AquacultureSpecies s) {
        TextField kgField    = new TextField("0.0");
        TextField countField = new TextField("0");
        styleDialogField(kgField);
        styleDialogField(countField);

        Label kgLbl    = new Label("Harvest weight (kg)");
        Label countLbl = new Label("Count harvested");
        kgLbl.getStyleClass().add("dialog-form-label");
        countLbl.getStyleClass().add("dialog-form-label");

        VBox form = new VBox(12, new VBox(6, kgLbl, kgField), new VBox(6, countLbl, countField));
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Record Harvest");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0, dialogHeader("🎣", "Record Harvest", "Recording harvest for \"" + s.getName() + "\""), form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        styleDlg(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                double kg    = Double.parseDouble(kgField.getText().trim());
                int    count = Integer.parseInt(countField.getText().trim());
                return new int[]{(int)(kg * 100), count};   // pack kg*100 + count
            } catch (NumberFormatException e) { return null; }
        });
        if (speciesTable.getScene() != null) dialog.initOwner(speciesTable.getScene().getWindow());
        dialog.showAndWait().ifPresent(arr -> {
            if (arr != null) {
                double kg    = arr[0] / 100.0;
                int    count = arr[1];
                if (count >= 0 && count <= s.getNumSpecies()) {
                    s.harvest(kg, count);
                    FarmService.getInstance().autoSave();
                    refreshStats();
                    showDetail(s);
                }
            }
        });
    }

    private void showMortalityDialog(AquacultureSpecies s) {
        TextField countField = new TextField("0");
        styleDialogField(countField);
        Label lbl = new Label("Mortality count");
        lbl.getStyleClass().add("dialog-form-label");
        VBox form = new VBox(8, lbl, countField);
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Record Mortality");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0, dialogHeader("💀", "Record Mortality", "Recording mortality for \"" + s.getName() + "\""), form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        styleDlg(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Record");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Integer.parseInt(countField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        if (speciesTable.getScene() != null) dialog.initOwner(speciesTable.getScene().getWindow());
        dialog.showAndWait().ifPresent(count -> {
            if (count != null && count >= 0 && count <= s.getNumSpecies()) {
                s.recordMortality(count);
                FarmService.getInstance().autoSave();
                refreshStats();
                showDetail(s);
            }
        });
    }

    private void showRestockDialog(AquacultureSpecies s) {
        TextField countField = new TextField("0");
        styleDialogField(countField);
        Label lbl = new Label("Restock count");
        lbl.getStyleClass().add("dialog-form-label");
        VBox form = new VBox(8, lbl, countField);
        form.setPadding(new Insets(20, 24, 8, 24));

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Restock");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(new VBox(0, dialogHeader("🔄", "Restock Species", "Restocking \"" + s.getName() + "\""), form));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(360);
        styleDlg(dialog);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Restock");
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Integer.parseInt(countField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        if (speciesTable.getScene() != null) dialog.initOwner(speciesTable.getScene().getWindow());
        dialog.showAndWait().ifPresent(count -> {
            if (count != null && count >= 0) {
                s.restock(count);
                FarmService.getInstance().autoSave();
                refreshStats();
                showDetail(s);
            }
        });
    }

    // ── Zone map ──────────────────────────────────────────────────────

    private void openZoneMap(AquacultureSpecies s) {
        List<String> sheets = speciesTable.getScene() == null
            ? new ArrayList<>()
            : new ArrayList<>(speciesTable.getScene().getStylesheets());
        ZoneMapDialog dlg = new ZoneMapDialog(s.getZone(), sheets, ZoneMapDialog.key(s));
        if (speciesTable.getScene() != null)
            dlg.initOwner(speciesTable.getScene().getWindow());
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

    private void styleDialogField(TextField tf) {
        tf.getStyleClass().setAll("dialog-form-field");
        tf.setMaxWidth(Double.MAX_VALUE);
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
        var sheets = speciesTable.getScene() == null ? null : speciesTable.getScene().getStylesheets();
        String css = (sheets != null && !sheets.isEmpty()) ? sheets.get(0) : getClass().getResource("/com/example/styles/main.css").toExternalForm();
        d.getDialogPane().getStylesheets().add(css);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        Button cancel = (Button) d.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (ok != null) ok.getStyleClass().add("btn-primary");
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");
    }
}
