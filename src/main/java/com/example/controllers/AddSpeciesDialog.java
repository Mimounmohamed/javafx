package com.example.controllers;

import Entities.AquacultureSpecies;
import ZONES.AquacultureZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AddSpeciesDialog extends Dialog<AquacultureSpecies> {

    public AddSpeciesDialog(String css, AquacultureZONE zone) {
        setTitle("Add Aquaculture Species");
        setHeaderText(null);

        // ── Fields ─────────────────────────────────────────────────────
        TextField nameField  = new TextField();
        nameField.setPromptText("e.g. Tilapia, Salmon, Trout");
        styleField(nameField);

        TextField countField = new TextField("100");
        styleField(countField);

        TextField tankField  = new TextField();
        tankField.setPromptText("e.g. Tank A, Pond 1  (optional)");
        styleField(tankField);

        // ── Section: SPECIES ──────────────────────────────────────────
        VBox speciesSection = new VBox(10,
            sectionLabel("SPECIES"),
            formGroup("Species name *", nameField),
            formGroupHinted("Tank / pond label", tankField, "optional label shown on the zone map")
        );

        // ── Section: STOCKING ─────────────────────────────────────────
        VBox stockSection = new VBox(10,
            sectionLabel("STOCKING"),
            formGroupHinted("Initial stock count", countField, "number of live specimens at stocking")
        );

        // ── Section: ZONE INFO ────────────────────────────────────────
        Label zoneInfo = new Label("Zone: " + zone.getName());
        zoneInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        Label boundaryNote = new Label();
        if (zone.hasBoundaries()) {
            boundaryNote.setText("✓  Zone has a boundary — tub sub-zones can be drawn inside it");
            boundaryNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #16A34A;");
        } else {
            boundaryNote.setText("⚠  Zone has no boundary yet — set one to enable tub-level sub-zones");
            boundaryNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #D97706;");
        }
        VBox zoneSection = new VBox(8, sectionLabel("ZONE"), zoneInfo, boundaryNote);

        // ── Body ──────────────────────────────────────────────────────
        VBox body = new VBox(20, speciesSection, stockSection, zoneSection);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(zone.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(460);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add species");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Validation ────────────────────────────────────────────────
        nameField.textProperty().addListener((obs, o, n) ->
            okBtn.setDisable(n.trim().isEmpty()));

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String name  = nameField.getText().trim();
                int    count = Math.max(1, Integer.parseInt(countField.getText().trim()));
                return new AquacultureSpecies(name, count, zone);
            } catch (NumberFormatException e) { return null; }
        });
    }

    private HBox buildHeader(String zoneName) {
        Label iconLbl = new Label("🐟");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Add aquaculture species");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Add a species to \"" + zoneName + "\"");
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

    private static void styleField(TextField tf) {
        tf.getStyleClass().setAll("dialog-form-field");
        tf.setMaxWidth(Double.MAX_VALUE);
    }

    private static VBox formGroup(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("az-form-label");
        return new VBox(5, lbl, input);
    }

    private static VBox formGroupHinted(String labelText, Node input, String hintText) {
        VBox group = formGroup(labelText, input);
        Label hint = new Label(hintText);
        hint.getStyleClass().add("dialog-hint");
        group.getChildren().add(hint);
        return group;
    }

    private static Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("rd-section-label");
        return lbl;
    }
}
