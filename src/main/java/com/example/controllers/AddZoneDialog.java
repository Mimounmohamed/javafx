package com.example.controllers;

import Entities.LIvestockType;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
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

public class AddZoneDialog extends Dialog<ZONE> {

    private final TextField nameField        = new TextField();
    private final ToggleButton lstBtn        = new ToggleButton("🐾  Livestock");
    private final ToggleButton cropBtn       = new ToggleButton("🌿  Crop");
    private final ToggleButton aquaBtn       = new ToggleButton("💧  Aquaculture");
    private final ComboBox<String> lstTypeCombo = new ComboBox<>();
    private final VBox lstCard;

    public AddZoneDialog(String stylesheet) {
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

        // ── Livestock options card ─────────────────────────────────────
        lstTypeCombo.getItems().addAll("RUMINANT", "POULTRY");
        lstTypeCombo.setValue("RUMINANT");
        lstTypeCombo.setMaxWidth(Double.MAX_VALUE);
        lstTypeCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        Label lstTypeLabel = new Label("Livestock type");
        lstTypeLabel.getStyleClass().add("az-form-label");

        VBox lstTypeGroup = new VBox(5, lstTypeLabel, lstTypeCombo);

        Label cardSectionLabel = new Label("LIVESTOCK OPTIONS");
        cardSectionLabel.getStyleClass().add("az-card-section-label");

        lstCard = new VBox(10, cardSectionLabel, lstTypeGroup);
        lstCard.getStyleClass().add("az-livestock-card");

        // Show/hide livestock card based on pill selection
        tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { lstBtn.setSelected(true); return; }
            boolean isLs = (newVal == lstBtn);
            lstCard.setVisible(isLs);
            lstCard.setManaged(isLs);
        });

        // ── Body ───────────────────────────────────────────────────────
        VBox body = new VBox(16, typeGroup, nameGroup, lstCard);
        body.setPadding(new Insets(20));

        // ── Full content (header + body) ───────────────────────────────
        VBox content = new VBox(0, buildHeader(), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(440);
        if (stylesheet != null) {
            getDialogPane().getStylesheets().add(stylesheet);
        }

        // ── Footer buttons ─────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("+ Add zone");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-secondary");

        // ── Validation ─────────────────────────────────────────────────
        nameField.textProperty().addListener((obs, oldVal, newVal) ->
            okBtn.setDisable(newVal.trim().isEmpty()));

        // ── Result converter ───────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String name = nameField.getText().trim();
            if (lstBtn.isSelected()) return new LivestockZONE(name, LIvestockType.valueOf(lstTypeCombo.getValue()));
            if (cropBtn.isSelected()) return new CropZONE(name);
            return new AquacultureZONE(name);
        });
    }

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
