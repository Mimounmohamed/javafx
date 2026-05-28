package com.example.controllers;

import Animals.Animal;
import Entities.LIvestockType;
import com.example.services.AnimalService;
import ZONES.LivestockZONE;
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
import javafx.util.StringConverter;
import java.util.List;

public class AddAnimalDialog extends Dialog<Animal> {

    public AddAnimalDialog(String css, AnimalService service, List<LivestockZONE> zones, String subtitle) {
        setTitle("Add Animal");
        setHeaderText(null);

        // ── Fields ─────────────────────────────────────────────────────
        TextField nameField    = new TextField();
        nameField.setPromptText("e.g. Bessie");
        styleField(nameField);

        TextField speciesField = new TextField();
        speciesField.setPromptText("e.g. Cow, Sheep");
        styleField(speciesField);

        TextField ageField = new TextField("1");
        styleField(ageField);

        TextField weightField = new TextField("100.0");
        styleField(weightField);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("RUMINANT", "POULTRY");
        typeCombo.setValue("RUMINANT");
        styleCombo(typeCombo);

        ComboBox<LivestockZONE> zoneCombo = new ComboBox<>();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.setValue(zones.get(0));
        zoneCombo.setConverter(new StringConverter<>() {
            @Override public String toString(LivestockZONE z) { return z == null ? "" : z.getName(); }
            @Override public LivestockZONE fromString(String s) { return null; }
        });
        styleCombo(zoneCombo);

        // ── Section: IDENTITY ─────────────────────────────────────────
        HBox speciesTypeRow = sideBySide(
            formGroup("Species *", speciesField),
            formGroup("Type", typeCombo)
        );
        VBox identitySection = new VBox(10,
            sectionLabel("IDENTITY"),
            formGroup("Name *", nameField),
            speciesTypeRow
        );

        // ── Section: PHYSICAL ─────────────────────────────────────────
        HBox physRow = sideBySide(
            formGroupHinted("Age", ageField, "in years"),
            formGroupHinted("Weight", weightField, "in kg")
        );
        VBox physSection = new VBox(10, sectionLabel("PHYSICAL"), physRow);

        // ── Section: ASSIGNMENT ──────────────────────────────────────
        Label zoneHint = new Label("Animal will be placed in this zone immediately");
        zoneHint.getStyleClass().add("dialog-hint");

        Label gpsBoundaryNote = new Label();
        updateGpsBoundaryNote(gpsBoundaryNote, zoneCombo.getValue());
        zoneCombo.setOnAction(e -> updateGpsBoundaryNote(gpsBoundaryNote, zoneCombo.getValue()));

        VBox assignSection = new VBox(10,
            sectionLabel("ASSIGNMENT"),
            formGroup("Zone *", zoneCombo),
            zoneHint,
            gpsBoundaryNote
        );

        // ── Body ──────────────────────────────────────────────────────
        VBox body = new VBox(20, identitySection, physSection, assignSection);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(subtitle), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(460);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add animal");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Validation ────────────────────────────────────────────────
        Runnable validate = () -> okBtn.setDisable(
            nameField.getText().trim().isEmpty() || speciesField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, o, n) -> validate.run());
        speciesField.textProperty().addListener((obs, o, n) -> validate.run());

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                return service.addAnimal(
                    nameField.getText().trim(),
                    speciesField.getText().trim(),
                    LIvestockType.valueOf(typeCombo.getValue()),
                    Integer.parseInt(ageField.getText().trim()),
                    Double.parseDouble(weightField.getText().trim()),
                    zoneCombo.getValue());
            } catch (NumberFormatException e) { return null; }
        });
    }

    private HBox buildHeader(String subtitle) {
        Label iconLbl = new Label("🐾");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Add animal");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label(subtitle);
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

    private static void updateGpsBoundaryNote(Label lbl, LivestockZONE zone) {
        if (zone == null) { lbl.setText(""); return; }
        if (zone.hasBoundaries()) {
            lbl.setText("✓  Zone has a boundary — GPS escape alerts will be active");
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #16A34A;");
        } else {
            lbl.setText("⚠  Zone has no boundary — GPS escape alerts won't fire until one is drawn");
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #D97706;");
        }
    }

    private static void styleField(TextField tf) {
        tf.getStyleClass().setAll("dialog-form-field");
        tf.setMaxWidth(Double.MAX_VALUE);
    }

    private static <T> void styleCombo(ComboBox<T> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.getStyleClass().setAll("combo-box", "dialog-form-combo");
    }

    static VBox formGroup(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("az-form-label");
        return new VBox(5, lbl, input);
    }

    static VBox formGroupHinted(String labelText, Node input, String hintText) {
        VBox group = formGroup(labelText, input);
        Label hint = new Label(hintText);
        hint.getStyleClass().add("dialog-hint");
        group.getChildren().add(hint);
        return group;
    }

    static Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("rd-section-label");
        return lbl;
    }

    static HBox sideBySide(VBox left, VBox right) {
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        return new HBox(12, left, right);
    }
}
