package com.example.controllers;

import Animals.Animal;
import ZONES.ZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Interactive dialog for manually positioning an animal's GPS collar on the zone map.
 * Click anywhere on the canvas to drop a marker, or type coordinates directly in the fields.
 * Returns double[]{lat, lon} on confirm, null on cancel.
 */
public class GpsLocationEditorDialog extends Dialog<double[]> {

    private static final int CANVAS_W = 560;
    private static final int CANVAS_H = 360;
    private static final int MARGIN   = 44;

    private final Animal          animal;
    private final ZONE            zone;
    private final Canvas          canvas;
    private final GraphicsContext gc;
    private final TextField       latField;
    private final TextField       lonField;
    private final Label           statusLabel;
    private final Label           hintLabel;

    // Coordinate bounds for canvas ↔ lat/lon mapping
    private double geoMinLat, geoMaxLat, geoMinLon, geoMaxLon;

    // Current marker position (NaN = not set)
    private double markerLat = Double.NaN;
    private double markerLon = Double.NaN;

    public GpsLocationEditorDialog(Animal animal, List<String> stylesheets) {
        this.animal = animal;
        this.zone   = animal.getZone();
        this.canvas = new Canvas(CANVAS_W, CANVAS_H);
        this.gc     = canvas.getGraphicsContext2D();

        latField    = new TextField();
        lonField    = new TextField();
        statusLabel = new Label();
        hintLabel   = new Label();

        // Pre-fill from existing GPS collar position
        var gps = animal.getGpsCollarSensor();
        double curLat = gps.getCurrentLatitude();
        double curLon = gps.getCurrentLongitude();
        if (curLat != 0.0 || curLon != 0.0) {
            markerLat = curLat;
            markerLon = curLon;
            latField.setText(String.format("%.6f", curLat));
            lonField.setText(String.format("%.6f", curLon));
        }

        initGeoBounds();
        buildUI(stylesheets);
        setupCanvasInteraction();
        setupFieldListeners();
        updateStatus();
        redraw();
    }

    // ── Coordinate system ──────────────────────────────────────────────

    private void initGeoBounds() {
        if (zone.hasBoundaries() && zone.getBoundaries().size() >= 2) {
            List<double[]> pts = zone.getBoundaries().getPoints();
            double pad  = 0.0006;
            geoMinLat   = pts.stream().mapToDouble(p -> p[0]).min().orElse(35.99) - pad;
            geoMaxLat   = pts.stream().mapToDouble(p -> p[0]).max().orElse(36.01) + pad;
            geoMinLon   = pts.stream().mapToDouble(p -> p[1]).min().orElse(2.99)  - pad;
            geoMaxLon   = pts.stream().mapToDouble(p -> p[1]).max().orElse(3.01)  + pad;
        } else {
            // Default view around Algiers when no boundary exists
            geoMinLat = 36.690; geoMaxLat = 36.740;
            geoMinLon =  2.980; geoMaxLon =  3.030;
        }
    }

    // lat/lon → canvas pixel
    private double gX(double lon) {
        return MARGIN + (lon - geoMinLon) / (geoMaxLon - geoMinLon) * (CANVAS_W - 2 * MARGIN);
    }
    private double gY(double lat) {
        return CANVAS_H - MARGIN - (lat - geoMinLat) / (geoMaxLat - geoMinLat) * (CANVAS_H - 2 * MARGIN);
    }

    // canvas pixel → lat/lon
    private double toLat(double py) {
        return geoMinLat + (CANVAS_H - MARGIN - py) / (CANVAS_H - 2 * MARGIN) * (geoMaxLat - geoMinLat);
    }
    private double toLon(double px) {
        return geoMinLon + (px - MARGIN) / (CANVAS_W - 2 * MARGIN) * (geoMaxLon - geoMinLon);
    }

    // ── UI layout ──────────────────────────────────────────────────────

    private void buildUI(List<String> stylesheets) {
        setTitle("GPS Location — " + animal.getName());
        setHeaderText(null);
        setResizable(false);
        if (stylesheets != null) getDialogPane().getStylesheets().addAll(stylesheets);

        // ── Custom header bar
        Label iconLbl = new Label("📍");
        iconLbl.getStyleClass().add("dialog-custom-header-icon");
        Label titleLbl = new Label("Set GPS Location — " + animal.getName());
        titleLbl.getStyleClass().add("dialog-custom-header-title");
        String boundaryInfo = zone.hasBoundaries()
            ? zone.getBoundaries().size() + " boundary points"
            : "no boundary set";
        Label subLbl = new Label(zone.getName() + "  ·  " + boundaryInfo);
        subLbl.getStyleClass().add("dialog-custom-header-sub");
        VBox titleBox = new VBox(2, titleLbl, subLbl);
        HBox header = new HBox(12, iconLbl, titleBox);
        header.getStyleClass().add("dialog-custom-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Canvas wrapper (green background to match ZoneMapDialog)
        VBox canvasWrap = new VBox(canvas);
        canvasWrap.setStyle("-fx-background-color: #d4edda;");

        // ── Coordinate input row
        Label latLbl = new Label("Latitude:");
        latLbl.getStyleClass().add("form-label");
        latField.setPromptText("e.g. 36.7203");
        latField.getStyleClass().add("form-field");
        latField.setPrefWidth(130);

        Label lonLbl = new Label("Longitude:");
        lonLbl.getStyleClass().add("form-label");
        lonField.setPromptText("e.g. 3.0512");
        lonField.getStyleClass().add("form-field");
        lonField.setPrefWidth(130);

        statusLabel.setMinWidth(140);

        Button clearBtn = new Button("✕  Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> {
            markerLat = Double.NaN; markerLon = Double.NaN;
            latField.setText(""); lonField.setText("");
            updateStatus(); redraw();
        });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox fieldsRow = new HBox(10, latLbl, latField, lonLbl, lonField, sp, statusLabel, clearBtn);
        fieldsRow.setAlignment(Pos.CENTER_LEFT);

        // Animal info strip
        Label animalInfoLbl = new Label(
            "📌  " + animal.getName()
            + "   ·   " + animal.getSpecies()
            + "   ·   " + animal.getType()
            + "   ·   Zone: " + zone.getName());
        animalInfoLbl.getStyleClass().add("text-muted");
        animalInfoLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");

        hintLabel.getStyleClass().add("text-muted");
        hintLabel.setWrapText(true);

        VBox formCard = new VBox(8, animalInfoLbl, hintLabel, fieldsRow);
        formCard.setPadding(new Insets(14, 20, 14, 20));
        formCard.getStyleClass().add("kpi-card");

        VBox content = new VBox(0, header, canvasWrap, formCard);
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(CANVAS_W + 20);

        // ── Buttons
        ButtonType okType = new ButtonType("Set Location", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);
        setResultConverter(bt -> {
            if (bt == okType && !Double.isNaN(markerLat) && !Double.isNaN(markerLon))
                return new double[]{ markerLat, markerLon };
            return null;
        });
    }

    // ── Interaction ────────────────────────────────────────────────────

    private void setupCanvasInteraction() {
        canvas.setOnMouseClicked(e -> {
            markerLat = round6(toLat(e.getY()));
            markerLon = round6(toLon(e.getX()));
            // Suppress listener-triggered re-entry while we set field text
            latField.setText(String.format("%.6f", markerLat));
            lonField.setText(String.format("%.6f", markerLon));
            updateStatus();
            redraw();
        });
    }

    private void setupFieldListeners() {
        latField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) syncFromFields();
        });
        lonField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) syncFromFields();
        });
    }

    private void syncFromFields() {
        try {
            double lat = Double.parseDouble(latField.getText().trim());
            double lon = Double.parseDouble(lonField.getText().trim());
            markerLat = lat;
            markerLon = lon;
        } catch (NumberFormatException ignored) {
            markerLat = Double.NaN;
            markerLon = Double.NaN;
        }
        updateStatus();
        redraw();
    }

    // ── Status indicator ───────────────────────────────────────────────

    private void updateStatus() {
        if (Double.isNaN(markerLat) || Double.isNaN(markerLon)) {
            statusLabel.setText("No position set");
            statusLabel.getStyleClass().setAll("text-muted");
            hintLabel.setText(
                "Click anywhere on the map to drop a position marker, or type coordinates in the fields below.");
        } else {
            hintLabel.setText(String.format("Position: %.6f°,  %.6f°  —  click the map to reposition.",
                markerLat, markerLon));
            if (!zone.hasBoundaries()) {
                statusLabel.setText("✓  No boundary");
                statusLabel.getStyleClass().setAll("text-muted");
            } else {
                boolean inside = zone.getBoundaries().contains(markerLat, markerLon);
                if (inside) {
                    statusLabel.setText("✓  Inside zone");
                    statusLabel.getStyleClass().setAll("autosave-status-on");
                } else {
                    statusLabel.setText("⚠  Outside zone");
                    statusLabel.getStyleClass().setAll("zones-gps-warning");
                }
            }
        }
    }

    // ── Canvas rendering ───────────────────────────────────────────────

    private void redraw() {
        gc.clearRect(0, 0, CANVAS_W, CANVAS_H);
        gc.setFill(Color.web("#d4edda")); gc.fillRect(0, 0, CANVAS_W, CANVAS_H);

        drawGrid();
        drawZoneBoundary();
        drawAxisLabels();
        drawCompass();

        if (!Double.isNaN(markerLat) && !Double.isNaN(markerLon))
            drawMarker(gX(markerLon), gY(markerLat));

        gc.setFill(Color.web("#2d4a2d"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(zone.getName(), MARGIN + 6, MARGIN - 12);
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#88aa88", 0.28)); gc.setLineWidth(1);
        for (int x = MARGIN; x <= CANVAS_W - MARGIN; x += 40)
            gc.strokeLine(x, MARGIN, x, CANVAS_H - MARGIN);
        for (int y = MARGIN; y <= CANVAS_H - MARGIN; y += 40)
            gc.strokeLine(MARGIN, y, CANVAS_W - MARGIN, y);
    }

    private void drawZoneBoundary() {
        if (!zone.hasBoundaries()) {
            gc.setStroke(Color.web("#4a7a4a")); gc.setLineWidth(2);
            gc.setLineDashes(8, 4);
            gc.strokeRect(MARGIN, MARGIN, CANVAS_W - 2 * MARGIN, CANVAS_H - 2 * MARGIN);
            gc.setLineDashes();
            gc.setFill(Color.web("#4a7a4a", 0.05));
            gc.fillRect(MARGIN, MARGIN, CANVAS_W - 2 * MARGIN, CANVAS_H - 2 * MARGIN);
            gc.setFill(Color.web("#4a7a4a", 0.45));
            gc.setFont(Font.font("System", 10));
            gc.fillText("No zone boundary defined — position will not trigger escape alerts",
                MARGIN + 8, CANVAS_H - MARGIN + 16);
            return;
        }
        List<double[]> pts = zone.getBoundaries().getPoints();
        if (pts.size() < 2) return;
        double[] xs = pts.stream().mapToDouble(p -> gX(p[1])).toArray();
        double[] ys = pts.stream().mapToDouble(p -> gY(p[0])).toArray();
        gc.setFill(Color.web("#a0d8a0", 0.55));
        gc.fillPolygon(xs, ys, pts.size());
        gc.setStroke(Color.web("#2e6b2e")); gc.setLineWidth(2.5); gc.setLineDashes();
        gc.strokePolygon(xs, ys, pts.size());
    }

    private void drawAxisLabels() {
        gc.setFill(Color.web("#4a7a4a", 0.7));
        gc.setFont(Font.font("System", 9));
        for (int i = 0; i <= 4; i++) {
            double lon = geoMinLon + i * (geoMaxLon - geoMinLon) / 4.0;
            gc.fillText(String.format("%.4f", lon), gX(lon) - 14, CANVAS_H - MARGIN + 16);
        }
        for (int i = 0; i <= 3; i++) {
            double lat = geoMinLat + i * (geoMaxLat - geoMinLat) / 3.0;
            gc.fillText(String.format("%.4f", lat), 2, gY(lat) + 3);
        }
    }

    private void drawCompass() {
        double cx = CANVAS_W - MARGIN + 24, cy = MARGIN - 18;
        gc.setFill(Color.web("#4a7a4a")); gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText("N", cx - 3, cy - 8);
        gc.setStroke(Color.web("#4a7a4a")); gc.setLineWidth(1.5);
        gc.strokeLine(cx, cy - 6, cx, cy + 6);
        gc.strokeLine(cx - 6, cy, cx + 6, cy);
    }

    private void drawMarker(double mx, double my) {
        boolean inside = !zone.hasBoundaries() || zone.getBoundaries().contains(markerLat, markerLon);
        Color pin = inside ? Color.web("#16A34A") : Color.web("#DC2626");

        // Crosshair
        gc.setStroke(pin.deriveColor(0, 1, 1, 0.35)); gc.setLineWidth(1);
        gc.strokeLine(MARGIN, my, CANVAS_W - MARGIN, my);
        gc.strokeLine(mx, MARGIN, mx, CANVAS_H - MARGIN);

        // Outer ring
        gc.setStroke(pin); gc.setLineWidth(2.5);
        gc.strokeOval(mx - 10, my - 10, 20, 20);
        // Inner dot
        gc.setFill(pin); gc.fillOval(mx - 5, my - 5, 10, 10);

        // Label beside marker
        gc.setFill(Color.web("#1a1a1a"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.fillText(animal.getName(), mx + 14, my);
        gc.setFont(Font.font("System", 10));
        gc.fillText(String.format("%.5f°, %.5f°", markerLat, markerLon), mx + 14, my + 13);
    }

    private static double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}
