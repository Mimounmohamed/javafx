package com.example.controllers;

import Entities.Crop;
import Entities.CropType;
import Additional_classes.Range;
import ZONES.CropZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Date;

public class AddCropDialog extends Dialog<Crop> {

    public AddCropDialog(String css, CropZONE zone) {
        setTitle("Add Crop");
        setHeaderText(null);

        // ── Fields ─────────────────────────────────────────────────────
        TextField varietyField = new TextField();
        varietyField.setPromptText("e.g. Golden Wheat, Roma Tomato");
        styleField(varietyField);

        ComboBox<CropType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(CropType.values());
        typeCombo.setValue(CropType.cereals);
        styleCombo(typeCombo);

        TextField weeksField = new TextField("12");
        styleField(weeksField);

        TextField phMinField  = new TextField("6.0");
        TextField phMaxField  = new TextField("7.5");
        TextField moistMinField = new TextField("40.0");
        TextField moistMaxField = new TextField("70.0");
        styleField(phMinField); styleField(phMaxField);
        styleField(moistMinField); styleField(moistMaxField);

        // ── Section: IDENTITY ─────────────────────────────────────────
        VBox identitySection = new VBox(10,
            sectionLabel("IDENTITY"),
            formGroup("Variety / cultivar *", varietyField),
            formGroup("Crop type", typeCombo)
        );

        // ── Section: SOIL CONDITIONS ──────────────────────────────────
        HBox phRow   = sideBySide(
            formGroupHinted("Soil pH — min", phMinField,   "e.g. 5.5"),
            formGroupHinted("Soil pH — max", phMaxField,   "e.g. 7.5")
        );
        HBox moistRow = sideBySide(
            formGroupHinted("Moisture % — min", moistMinField, "e.g. 40"),
            formGroupHinted("Moisture % — max", moistMaxField, "e.g. 70")
        );
        VBox soilSection = new VBox(10, sectionLabel("SOIL CONDITIONS"), phRow, moistRow);

        // ── Section: TIMELINE ─────────────────────────────────────────
        VBox timeSection = new VBox(10,
            sectionLabel("TIMELINE"),
            formGroupHinted("Weeks to harvest", weeksField, "estimated time to maturity from today")
        );

        // ── Section: ZONE INFO ────────────────────────────────────────
        Label zoneInfo = new Label("Zone: " + zone.getName());
        zoneInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        Label boundaryNote = new Label();
        if (zone.hasBoundaries()) {
            boundaryNote.setText("✓  Zone has a boundary — field sub-boundaries can be drawn inside it");
            boundaryNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #16A34A;");
        } else {
            boundaryNote.setText("⚠  Zone has no boundary yet — set one to enable field-level sub-zones");
            boundaryNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #D97706;");
        }
        VBox zoneSection = new VBox(8, sectionLabel("ZONE"), zoneInfo, boundaryNote);

        // ── Body ──────────────────────────────────────────────────────
        VBox body = new VBox(20, identitySection, soilSection, timeSection, zoneSection);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(zone.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(480);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add crop");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Validation ────────────────────────────────────────────────
        varietyField.textProperty().addListener((obs, o, n) ->
            okBtn.setDisable(n.trim().isEmpty()));

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                String variety = varietyField.getText().trim();
                CropType type  = typeCombo.getValue();
                int weeks      = Math.max(1, Integer.parseInt(weeksField.getText().trim()));
                double phLo    = Double.parseDouble(phMinField.getText().trim());
                double phHi    = Double.parseDouble(phMaxField.getText().trim());
                double mLo     = Double.parseDouble(moistMinField.getText().trim());
                double mHi     = Double.parseDouble(moistMaxField.getText().trim());
                Date now       = new Date();
                Date harvest   = new Date(now.getTime() + (long) weeks * 7 * 24 * 3600 * 1000L);
                return new Crop(type, variety, now, harvest,
                    new Range(phLo, phHi), new Range(mLo, mHi), zone);
            } catch (NumberFormatException e) { return null; }
        });
    }

    private HBox buildHeader(String zoneName) {
        Label iconLbl = new Label("🌾");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Add crop");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Add a new crop field to \"" + zoneName + "\"");
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

    private static <T> void styleCombo(ComboBox<T> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.getStyleClass().setAll("combo-box", "dialog-form-combo");
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

    private static HBox sideBySide(VBox left, VBox right) {
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        return new HBox(12, left, right);
    }
}
