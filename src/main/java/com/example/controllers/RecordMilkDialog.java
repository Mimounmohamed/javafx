package com.example.controllers;

import Animals.Animal;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class RecordMilkDialog extends Dialog<Double> {

    public RecordMilkDialog(String css, Animal a) {
        setTitle("Record Milk Yield");
        setHeaderText(null);

        // ── Field ──────────────────────────────────────────────────────
        TextField litersField = new TextField("0.0");
        litersField.setPromptText("0.0");
        litersField.getStyleClass().setAll("dialog-form-field");
        litersField.setMaxWidth(Double.MAX_VALUE);

        Label fieldLabel = new Label("Liters to add *");
        fieldLabel.getStyleClass().add("az-form-label");

        Label hintLabel = new Label("Enter the amount produced in this session");
        hintLabel.getStyleClass().add("dialog-hint");

        VBox body = new VBox(20, new VBox(5, fieldLabel, litersField, hintLabel));
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(a.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(380);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Record");
        okBtn.getStyleClass().add("btn-primary");

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(litersField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
    }

    private HBox buildHeader(String animalName) {
        Label iconLbl = new Label("🥛");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Record milk yield");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Recording for " + animalName);
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
