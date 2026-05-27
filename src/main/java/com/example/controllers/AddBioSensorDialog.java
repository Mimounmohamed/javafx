package com.example.controllers;

import Animals.Animal;
import Sensors.BioMeasureType;
import Sensors.BioSensor;
import ZONES.LivestockZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.util.StringConverter;

public class AddBioSensorDialog extends Dialog<BioSensor> {

    public AddBioSensorDialog(String css, LivestockZONE zone) {
        setTitle("Add Bio Sensor");
        setHeaderText(null);

        // ── Fields ─────────────────────────────────────────────────────
        ComboBox<Animal> animalCombo = new ComboBox<>();
        animalCombo.getItems().addAll(zone.getAnimals());
        animalCombo.setValue(zone.getAnimals().get(0));
        animalCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Animal a) { return a == null ? "" : a.getName(); }
            @Override public Animal fromString(String s) { return null; }
        });
        animalCombo.setMaxWidth(Double.MAX_VALUE);
        animalCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        ComboBox<BioMeasureType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(BioMeasureType.values());
        typeCombo.setValue(BioMeasureType.Temperature);
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.getStyleClass().setAll("combo-box", "dialog-form-combo");

        TextField minField = new TextField("36.0");
        minField.getStyleClass().setAll("dialog-form-field");
        minField.setMaxWidth(Double.MAX_VALUE);

        TextField maxField = new TextField("39.5");
        maxField.getStyleClass().setAll("dialog-form-field");
        maxField.setMaxWidth(Double.MAX_VALUE);

        // ── Section: TARGET ──────────────────────────────────────────
        VBox targetSection = new VBox(10,
            sectionLabel("TARGET"),
            formGroup("Animal *", animalCombo),
            formGroup("Measure type *", typeCombo)
        );

        // ── Section: THRESHOLDS ──────────────────────────────────────
        HBox threshRow = sideBySide(
            formGroupHinted("Min threshold", minField, "e.g. 36.0"),
            formGroupHinted("Max threshold", maxField, "e.g. 39.5")
        );
        VBox threshSection = new VBox(10, sectionLabel("THRESHOLDS"), threshRow);

        // ── Body ──────────────────────────────────────────────────────
        VBox body = new VBox(20, targetSection, threshSection);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(zone.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(440);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add sensor");
        okBtn.getStyleClass().add("btn-primary");

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                return new BioSensor(animalCombo.getValue(), typeCombo.getValue(), min, max);
            } catch (NumberFormatException e) { return null; }
        });
    }

    private HBox buildHeader(String zoneName) {
        Label iconLbl = new Label("📡");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Add bio sensor");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Attach a sensor to an animal in \"" + zoneName + "\"");
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

    private static VBox formGroup(String labelText, javafx.scene.Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("az-form-label");
        return new VBox(5, lbl, input);
    }

    private static VBox formGroupHinted(String labelText, javafx.scene.Node input, String hintText) {
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
