package com.example.controllers;

import Entities.LIvestockType;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.GoegraphicBoundries;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class AddZoneDialog extends Dialog<ZONE> {

    private final TextField        nameField    = new TextField();
    private final ToggleButton     lstBtn       = new ToggleButton("🐾  Livestock");
    private final ToggleButton     cropBtn      = new ToggleButton("🌿  Crop");
    private final ToggleButton     aquaBtn      = new ToggleButton("💧  Aquaculture");
    private final ComboBox<String> lstTypeCombo = new ComboBox<>();

    private final Label lstBoundaryStatus  = new Label("No boundary defined");
    private final Label cropBoundaryStatus = new Label("No boundary defined  (optional)");
    private final Label aquaBoundaryStatus = new Label("No boundary defined  (optional)");

    private final VBox lstCard;
    private final VBox cropCard;
    private final VBox aquaCard;

    private GoegraphicBoundries lstBoundary  = null;
    private GoegraphicBoundries cropBoundary = null;
    private GoegraphicBoundries aquaBoundary = null;

    private Button okBtn;
    private final String stylesheet;
    private final GoegraphicBoundries farmBoundary;
    private final List<GoegraphicBoundries> siblingBoundaries;

    public AddZoneDialog(String stylesheet) {
        this(stylesheet, null, List.of());
    }

    public AddZoneDialog(String stylesheet, GoegraphicBoundries farmBoundary) {
        this(stylesheet, farmBoundary, List.of());
    }

    public AddZoneDialog(String stylesheet, GoegraphicBoundries farmBoundary,
                         List<GoegraphicBoundries> siblingBoundaries) {
        this.stylesheet        = stylesheet;
        this.farmBoundary      = farmBoundary;
        this.siblingBoundaries = siblingBoundaries != null ? siblingBoundaries : List.of();
        setTitle("Add Zone");
        setHeaderText(null);

        // ── Pill toggle buttons ────────────────────────────────────────
        ToggleGroup tg = new ToggleGroup();
        lstBtn.setToggleGroup(tg);
        cropBtn.setToggleGroup(tg);
        aquaBtn.setToggleGroup(tg);
        lstBtn.setSelected(true);

        lstBtn.getStyleClass().setAll("az-pill-btn");
        cropBtn.getStyleClass().setAll("az-pill-btn");
        aquaBtn.getStyleClass().setAll("az-pill-btn");
        lstBtn.setMaxWidth(Double.MAX_VALUE);
        cropBtn.setMaxWidth(Double.MAX_VALUE);
        aquaBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lstBtn,  Priority.ALWAYS);
        HBox.setHgrow(cropBtn, Priority.ALWAYS);
        HBox.setHgrow(aquaBtn, Priority.ALWAYS);

        HBox pillRow = new HBox(8, lstBtn, cropBtn, aquaBtn);
        pillRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("Zone type *");
        typeLabel.getStyleClass().add("az-form-label");
        VBox typeGroup = new VBox(5, typeLabel, pillRow);

        // ── Name field ─────────────────────────────────────────────────
        nameField.setPromptText("e.g. North Pasture");
        nameField.getStyleClass().setAll("dialog-form-field");
        nameField.setMaxWidth(Double.MAX_VALUE);

        Label nameLabel = new Label("Zone name *");
        nameLabel.getStyleClass().add("az-form-label");
        VBox nameGroup = new VBox(5, nameLabel, nameField);

        // ── Livestock card (required boundary) ────────────────────────
        lstTypeCombo.getItems().addAll("RUMINANT", "POULTRY");
        lstTypeCombo.setValue("RUMINANT");
        lstTypeCombo.setMaxWidth(Double.MAX_VALUE);
        lstTypeCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        Label lstTypeLabel = new Label("Livestock type");
        lstTypeLabel.getStyleClass().add("az-form-label");

        lstCard = new VBox(10,
            new Label("LIVESTOCK OPTIONS") {{ getStyleClass().add("az-card-section-label"); }},
            new VBox(5, lstTypeLabel, lstTypeCombo),
            buildBoundaryGroup("Border limit  *", true,
                lstBoundaryStatus, () -> openEditor("lst"))
        );
        lstCard.getStyleClass().add("az-livestock-card");

        // ── Crop card (optional boundary) ─────────────────────────────
        cropCard = new VBox(10,
            new Label("CROP OPTIONS") {{ getStyleClass().add("az-card-section-label"); }},
            buildBoundaryGroup("Border limit  (optional)", false,
                cropBoundaryStatus, () -> openEditor("crop"))
        );
        cropCard.getStyleClass().add("az-livestock-card");
        cropCard.setVisible(false);
        cropCard.setManaged(false);

        // ── Aquaculture card (optional boundary) ──────────────────────
        aquaCard = new VBox(10,
            new Label("AQUACULTURE OPTIONS") {{ getStyleClass().add("az-card-section-label"); }},
            buildBoundaryGroup("Border limit  (optional)", false,
                aquaBoundaryStatus, () -> openEditor("aqua"))
        );
        aquaCard.getStyleClass().add("az-livestock-card");
        aquaCard.setVisible(false);
        aquaCard.setManaged(false);

        // Show correct card on toggle change
        tg.selectedToggleProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) { lstBtn.setSelected(true); return; }
            lstCard.setVisible(newVal == lstBtn);   lstCard.setManaged(newVal == lstBtn);
            cropCard.setVisible(newVal == cropBtn); cropCard.setManaged(newVal == cropBtn);
            aquaCard.setVisible(newVal == aquaBtn); aquaCard.setManaged(newVal == aquaBtn);
            updateOkBtn();
        });

        // ── Body ───────────────────────────────────────────────────────
        VBox body = new VBox(16, typeGroup, nameGroup, lstCard, cropCard, aquaCard);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(440);
        if (stylesheet != null) getDialogPane().getStylesheets().add(stylesheet);

        // ── Footer buttons ─────────────────────────────────────────────
        okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("+ Add zone");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        nameField.textProperty().addListener((obs, old, val) -> updateOkBtn());

        // ── Result converter ───────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String name = nameField.getText().trim();
            if (lstBtn.isSelected()) {
                return new LivestockZONE(name, LIvestockType.valueOf(lstTypeCombo.getValue()), lstBoundary);
            }
            if (cropBtn.isSelected()) {
                CropZONE z = new CropZONE(name);
                if (cropBoundary != null) z.setBoundaries(cropBoundary);
                return z;
            }
            AquacultureZONE z = new AquacultureZONE(name);
            if (aquaBoundary != null) z.setBoundaries(aquaBoundary);
            return z;
        });
    }

    // ── Shared boundary group builder ──────────────────────────────────

    private VBox buildBoundaryGroup(String labelText, boolean required,
                                    Label statusLabel, Runnable openAction) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("az-form-label");

        Button drawBtn = new Button("🗺  Draw Border Limit" + (required ? "" : "  (skip if not needed)"));
        drawBtn.getStyleClass().add("btn-secondary");
        drawBtn.setMaxWidth(Double.MAX_VALUE);
        drawBtn.setOnAction(e -> openAction.run());

        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (required ? "#9CA3AF" : "#6B7280") + ";");
        return new VBox(6, lbl, drawBtn, statusLabel);
    }

    // ── Boundary editor launcher ───────────────────────────────────────

    private void openEditor(String type) {
        String zoneName = nameField.getText().trim();
        if (zoneName.isEmpty()) zoneName = "New Zone";
        List<String> sheets = stylesheet != null ? List.of(stylesheet) : List.of();

        GoegraphicBoundries existing = switch (type) {
            case "lst"  -> lstBoundary;
            case "crop" -> cropBoundary;
            default     -> aquaBoundary;
        };

        new BoundaryEditorDialog(zoneName, existing, farmBoundary, siblingBoundaries, sheets).showAndWait().ifPresent(bounds -> {
            String status = "✓  " + bounds.size() + " boundary points defined";
            String style  = "-fx-font-size: 11px; -fx-text-fill: #16A34A; -fx-font-weight: 600;";
            switch (type) {
                case "lst"  -> { lstBoundary  = bounds; lstBoundaryStatus.setText(status);  lstBoundaryStatus.setStyle(style); }
                case "crop" -> { cropBoundary = bounds; cropBoundaryStatus.setText(status); cropBoundaryStatus.setStyle(style); }
                default     -> { aquaBoundary = bounds; aquaBoundaryStatus.setText(status); aquaBoundaryStatus.setStyle(style); }
            }
            updateOkBtn();
        });
    }

    private void updateOkBtn() {
        if (okBtn == null) return;
        boolean nameOk      = !nameField.getText().trim().isEmpty();
        boolean isLivestock = lstBtn.isSelected();
        boolean boundaryOk  = !isLivestock || (lstBoundary != null && lstBoundary.size() >= 3);
        okBtn.setDisable(!(nameOk && boundaryOk));
    }

    // ── Header ─────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Label iconLbl = new Label("🗺");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Add zone");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Create a new zone on the farm");
        subLbl.getStyleClass().add("dialog-custom-header-sub");

        VBox textBox = new VBox(2, titleLbl, subLbl);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-header-close-btn");
        closeBtn.setOnAction(e -> {
            Button footerCancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            if (footerCancel != null) footerCancel.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, iconLbl, textBox, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }
}
