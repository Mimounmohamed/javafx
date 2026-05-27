package com.example.controllers;

import Animals.FeedingProgram;
import ZONES.LivestockZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.Arrays;

public class EditFeedingProgramDialog extends Dialog<Boolean> {

    public EditFeedingProgramDialog(String css, LivestockZONE zone, FeedingProgram fp) {
        setTitle("Edit Feeding Program");
        setHeaderText(null);

        // ── Fields ─────────────────────────────────────────────────────
        TextField foodField = new TextField(fp.getFoodType());
        foodField.setPromptText("e.g. Hay + Grain");
        foodField.getStyleClass().setAll("dialog-form-field");
        foodField.setMaxWidth(Double.MAX_VALUE);

        TextField quantityField = new TextField(String.valueOf(fp.getQuantity()));
        quantityField.getStyleClass().setAll("dialog-form-field");
        quantityField.setMaxWidth(Double.MAX_VALUE);

        TextField scheduleField = new TextField(String.join(", ", fp.getSchedule()));
        scheduleField.getStyleClass().setAll("dialog-form-field");
        scheduleField.setMaxWidth(Double.MAX_VALUE);

        // ── Section: FEED ─────────────────────────────────────────────
        VBox foodGroup     = formGroup("Food type *", foodField);
        VBox quantityGroup = formGroupHinted("Quantity per day", quantityField, "in kg");
        VBox feedSection   = new VBox(10, sectionLabel("FEED"), foodGroup, quantityGroup);

        // ── Section: SCHEDULE ────────────────────────────────────────
        Label schedHint = new Label("Comma-separated HH:mm — e.g. 07:00, 12:30, 18:00");
        schedHint.getStyleClass().add("dialog-hint");
        VBox schedGroup = formGroup("Feeding times", scheduleField);
        schedGroup.getChildren().add(schedHint);

        // Info card with wake / sleep times
        Label infoText = new Label(
            "🌅 Wake: " + fp.getWakeUpTime() + "   🌙 Sleep: " + fp.getSleepTime());
        infoText.getStyleClass().add("rd-info-card-text");
        Label infoNote = new Label("To change wake/sleep, recreate the program");
        infoNote.getStyleClass().add("rd-info-card-note");
        VBox infoCard = new VBox(4, infoText, infoNote);
        infoCard.getStyleClass().add("rd-info-card");

        VBox schedSection = new VBox(10, sectionLabel("SCHEDULE"), schedGroup, infoCard);

        // ── Body ──────────────────────────────────────────────────────
        VBox body = new VBox(20, feedSection, schedSection);
        body.setPadding(new Insets(20));

        VBox content = new VBox(0, buildHeader(zone.getName()), body);
        content.setFillWidth(true);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setMinWidth(440);
        if (css != null) getDialogPane().getStylesheets().add(css);

        // ── Footer ────────────────────────────────────────────────────
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Save");
        okBtn.getStyleClass().add("btn-primary");

        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL))
            .getStyleClass().add("btn-secondary");

        // ── Result converter ──────────────────────────────────────────
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                fp.setFoodType(foodField.getText().trim());
                fp.setQuantity(Double.parseDouble(quantityField.getText().trim()));
                fp.setSchedule(Arrays.asList(scheduleField.getText().trim().split("\\s*,\\s*")));
                return Boolean.TRUE;
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input");
                err.setHeaderText(null);
                err.setContentText("Check quantity (number) and schedule (HH:mm).\n" + e.getMessage());
                err.showAndWait();
                return null;
            }
        });
    }

    private HBox buildHeader(String zoneName) {
        Label iconLbl = new Label("🌾");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label("Edit feeding program");
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label("Updating feeding schedule for " + zoneName);
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
}
