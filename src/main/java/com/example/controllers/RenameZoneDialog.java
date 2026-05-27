package com.example.controllers;

import ZONES.ZONE;
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

public class RenameZoneDialog extends Dialog<String> {

    public RenameZoneDialog(String css, ZONE zone) {
        setTitle("Rename Zone");
        setHeaderText(null);

        // ── Field ──────────────────────────────────────────────────────
        TextField nameField = new TextField(zone.getName());
        nameField.getStyleClass().setAll("dialog-form-field");
        nameField.setMaxWidth(Double.MAX_VALUE);

        Label fieldLabel = new Label("New name *");
        fieldLabel.getStyleClass().add("az-form-label");

        Label hintLabel = new Label("Press Enter or click Rename to confirm");
        hintLabel.getStyleClass().add("dialog-hint");

        VBox body = new VBox(20, new VBox(5, fieldLabel, nameField, hintLabel));
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(zone.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(380);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Rename");
        okBtn.getStyleClass().add("btn-primary");
        // FIX: field is pre-filled with zone.getName() so button starts enabled
        okBtn.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> bt == ButtonType.OK ? nameField.getText().trim() : null);
    }

    private HBox buildHeader(String zoneName) {
        Label iconLbl = new Label("✏️");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Rename zone");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Rename \"" + zoneName + "\"");
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
