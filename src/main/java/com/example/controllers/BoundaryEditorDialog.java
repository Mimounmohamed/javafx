package com.example.controllers;

import ZONES.GoegraphicBoundries;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoundaryEditorDialog extends Dialog<GoegraphicBoundries> {

    // ── Draw tab state ────────────────────────────────────────────────
    private final List<double[]> drawnPoints = new ArrayList<>();
    private double minLat = 35.99, maxLat = 36.01;
    private double minLon = 2.99,  maxLon = 3.01;
    private Canvas      canvas;
    private ListView<String> drawnList;
    private Label       drawStatusLabel;
    private int dragIdx          = -1;
    private int selectedDrawnIdx = -1;

    // ── Manual tab state ──────────────────────────────────────────────
    private final List<double[]>         manualPoints      = new ArrayList<>();
    private final ObservableList<String> manualPointLabels = FXCollections.observableArrayList();
    private int editManualIdx = -1;

    // ── JSON tab state ────────────────────────────────────────────────
    private final List<double[]>         jsonPoints      = new ArrayList<>();
    private final ObservableList<String> jsonPointLabels = FXCollections.observableArrayList();

    private TabPane tabPane;
    private final String boundaryTitle;

    // ── Parent zone boundary (constraint for sub-zone editing) ────────
    private final GoegraphicBoundries parentBoundary;

    // ═════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════

    /** Zone-level boundary — no parent constraint */
    public BoundaryEditorDialog(String name, GoegraphicBoundries existing, List<String> styleSheets) {
        this(name, existing, null, styleSheets);
    }

    /**
     * Sub-zone boundary editor.
     * @param parentBoundary  the containing zone's boundary — points outside it are rejected
     */
    public BoundaryEditorDialog(String name, GoegraphicBoundries existing,
                                GoegraphicBoundries parentBoundary, List<String> styleSheets) {
        this.boundaryTitle  = name;
        this.parentBoundary = parentBoundary;

        setTitle("Boundary Editor — " + name);
        setHeaderText(null);
        getDialogPane().setMinWidth(820);
        getDialogPane().setPrefWidth(920);
        getDialogPane().setPrefHeight(660);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Make the dialog window resizable
        setOnShown(ev -> {
            if (getDialogPane().getScene().getWindow() instanceof javafx.stage.Stage st) {
                st.setResizable(true);
                st.setMinWidth(820);
                st.setMinHeight(560);
            }
        });

        if (!styleSheets.isEmpty())
            getDialogPane().getStylesheets().add(styleSheets.get(0));

        Button okBtn     = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        okBtn.setText("Save Boundary");
        okBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-secondary");

        String subtitle = parentBoundary != null
            ? "Must stay inside the zone outline · 3 points minimum"
            : "Define the geographic boundary · 3 points minimum";

        // Pre-load existing child boundary
        if (existing != null && !existing.getPoints().isEmpty()) {
            List<double[]> pts = existing.getPoints();
            drawnPoints.addAll(pts);
            manualPoints.addAll(pts);
            rebuildLabels(manualPoints, manualPointLabels);
            jsonPoints.addAll(pts);
            rebuildLabels(jsonPoints, jsonPointLabels);
        }

        // Canvas view: prefer parent extent (with tiny padding), else child extent
        if (parentBoundary != null && parentBoundary.size() >= 2) {
            initBoundsFromPoints(parentBoundary.getPoints(), 0.0008);
        } else if (existing != null && existing.size() >= 2) {
            initBoundsFromPoints(existing.getPoints(), 0.005);
        }

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(buildDrawTab(), buildManualTab(), buildJsonTab());

        VBox dialogRoot = new VBox(0,
            buildCustomHeader("🗺", "Boundary Editor — " + name, subtitle),
            tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        getDialogPane().setContent(dialogRoot);

        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            List<double[]> pts = getActivePoints();
            if (pts.size() < 3) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Not Enough Points");
                warn.setHeaderText(null);
                warn.setContentText("A boundary needs at least 3 points to form a polygon.");
                warn.showAndWait();
                return null;
            }
            return new GoegraphicBoundries(new ArrayList<>(pts));
        });
    }

    private void initBoundsFromPoints(List<double[]> pts, double pad) {
        minLat = pts.stream().mapToDouble(p -> p[0]).min().orElse(35.99) - pad;
        maxLat = pts.stream().mapToDouble(p -> p[0]).max().orElse(36.01) + pad;
        minLon = pts.stream().mapToDouble(p -> p[1]).min().orElse(2.99)  - pad;
        maxLon = pts.stream().mapToDouble(p -> p[1]).max().orElse(3.01)  + pad;
    }

    // ══════════════════════════════════════════════════════════════════
    // TAB 1 — Draw on Canvas
    // ══════════════════════════════════════════════════════════════════

    private Tab buildDrawTab() {
        // Bounds controls — hide when a parent boundary locks the view
        TextField minLatF = numField(String.valueOf(minLat), 90);
        TextField maxLatF = numField(String.valueOf(maxLat), 90);
        TextField minLonF = numField(String.valueOf(minLon), 90);
        TextField maxLonF = numField(String.valueOf(maxLon), 90);

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("btn-secondary");
        applyBtn.setOnAction(e -> {
            try {
                double nl1 = Double.parseDouble(minLatF.getText().trim());
                double nl2 = Double.parseDouble(maxLatF.getText().trim());
                double lo1 = Double.parseDouble(minLonF.getText().trim());
                double lo2 = Double.parseDouble(maxLonF.getText().trim());
                if (nl1 >= nl2 || lo1 >= lo2) throw new NumberFormatException();
                minLat = nl1; maxLat = nl2; minLon = lo1; maxLon = lo2;
                redrawCanvas();
            } catch (NumberFormatException ex) { showError("Invalid bounds — ensure min < max."); }
        });

        HBox boundsBar = new HBox(6,
            new Label("Lat min:"), minLatF, new Label("max:"), maxLatF,
            new Label("  Lon min:"), minLonF, new Label("max:"), maxLonF, applyBtn);
        boundsBar.setAlignment(Pos.CENTER_LEFT);
        boundsBar.setPadding(new Insets(6, 8, 4, 8));
        // When a parent boundary constrains the view, the user shouldn't need to change bounds
        if (parentBoundary != null) {
            boundsBar.setDisable(true);
            boundsBar.setStyle("-fx-opacity: 0.5;");
        }

        canvas = new Canvas(600, 360);
        redrawCanvas();

        drawnList = new ListView<>();
        drawnList.setPrefWidth(185);
        drawnList.setStyle("-fx-font-size: 11px;");
        syncListFromDrawn();

        drawnList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            selectedDrawnIdx = n.intValue();
            redrawCanvas();
        });

        // Status label for constraint feedback
        drawStatusLabel = new Label("");
        drawStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-style:italic; -fx-font-size:11px;");
        drawStatusLabel.setMaxWidth(Double.MAX_VALUE);

        canvas.setOnMousePressed(e -> {
            int hit = nearestDrawnPoint(e.getX(), e.getY());
            if (hit >= 0) {
                dragIdx = hit; selectedDrawnIdx = hit;
                drawnList.getSelectionModel().select(hit);
                drawStatusLabel.setText("");
                redrawCanvas();
            } else if (e.getButton() == MouseButton.PRIMARY) {
                double lat = canvasYToLat(e.getY());
                double lon = canvasXToLon(e.getX());
                if (parentBoundary != null && !parentBoundary.contains(lat, lon)) {
                    drawStatusLabel.setText("⚠  Point is outside the zone boundary — not added");
                    return;
                }
                drawStatusLabel.setText("");
                drawnPoints.add(new double[]{lat, lon});
                selectedDrawnIdx = drawnPoints.size() - 1;
                dragIdx = selectedDrawnIdx;
                syncListFromDrawn();
                drawnList.getSelectionModel().select(selectedDrawnIdx);
                redrawCanvas();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (dragIdx >= 0 && dragIdx < drawnPoints.size()) {
                double lat = clampLat(canvasYToLat(e.getY()));
                double lon = clampLon(canvasXToLon(e.getX()));
                // If dragging into forbidden area, clamp but warn
                if (parentBoundary != null && !parentBoundary.contains(lat, lon)) {
                    drawStatusLabel.setText("⚠  Dragged outside zone boundary");
                } else {
                    drawStatusLabel.setText("");
                    drawnPoints.get(dragIdx)[0] = lat;
                    drawnPoints.get(dragIdx)[1] = lon;
                    syncListFromDrawn();
                    redrawCanvas();
                }
            }
        });

        canvas.setOnMouseReleased(e -> dragIdx = -1);

        canvas.setOnContextMenuRequested(e -> {
            int hit = nearestDrawnPoint(e.getX(), e.getY());
            if (hit >= 0) drawnPoints.remove(hit);
            else if (!drawnPoints.isEmpty()) drawnPoints.remove(drawnPoints.size() - 1);
            selectedDrawnIdx = -1; dragIdx = -1;
            syncListFromDrawn(); redrawCanvas();
        });

        HBox canvasRow = new HBox(8, canvas, drawnList);
        canvasRow.setPadding(new Insets(0, 8, 0, 8));
        VBox.setVgrow(canvasRow, Priority.ALWAYS);

        // Resize canvas dynamically when the dialog is resized
        canvasRow.widthProperty().addListener((obs, o, newW) -> {
            double w = newW.doubleValue() - drawnList.getPrefWidth() - 24;
            if (w > 300) { canvas.setWidth(w); redrawCanvas(); }
        });
        canvasRow.heightProperty().addListener((obs, o, newH) -> {
            if (newH.doubleValue() > 200) { canvas.setHeight(newH.doubleValue()); redrawCanvas(); }
        });

        Button undoBtn = new Button("↩ Undo Last");
        undoBtn.getStyleClass().add("btn-secondary");
        undoBtn.setOnAction(e -> {
            if (!drawnPoints.isEmpty()) {
                drawnPoints.remove(drawnPoints.size() - 1);
                selectedDrawnIdx = -1; syncListFromDrawn(); redrawCanvas();
            }
        });

        Button removeSelBtn = new Button("✕ Remove Selected");
        removeSelBtn.getStyleClass().add("btn-secondary");
        removeSelBtn.setOnAction(e -> {
            if (selectedDrawnIdx >= 0 && selectedDrawnIdx < drawnPoints.size()) {
                drawnPoints.remove(selectedDrawnIdx);
                selectedDrawnIdx = -1; dragIdx = -1; syncListFromDrawn(); redrawCanvas();
            }
        });

        Button clearBtn = new Button("🗑 Clear All");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setOnAction(e -> {
            drawnPoints.clear(); selectedDrawnIdx = -1; dragIdx = -1;
            syncListFromDrawn(); redrawCanvas();
        });

        String hintText = parentBoundary != null
            ? "Click INSIDE zone outline → add point   Drag point → move   Right-click → remove"
            : "Click → add point   Drag point → move   Right-click → remove";
        Label hint = new Label(hintText);
        hint.getStyleClass().add("text-muted");
        hint.setStyle("-fx-font-size:11px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actionBar = new HBox(8, hint, sp, undoBtn, removeSelBtn, clearBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(4, 8, 4, 8));

        VBox root = new VBox(boundsBar, canvasRow, drawStatusLabel, actionBar);
        VBox.setVgrow(canvasRow, Priority.ALWAYS);
        VBox.setVgrow(root, Priority.ALWAYS);
        VBox.setMargin(drawStatusLabel, new Insets(2, 8, 0, 8));
        return new Tab("✏ Draw", root);
    }

    // ── Canvas rendering ──────────────────────────────────────────────

    private int nearestDrawnPoint(double px, double py) {
        final double SNAP = 13;
        int best = -1; double bestDist = SNAP;
        for (int i = 0; i < drawnPoints.size(); i++) {
            double d = Math.hypot(px - lngToX(drawnPoints.get(i)[1]), py - latToY(drawnPoints.get(i)[0]));
            if (d < bestDist) { bestDist = d; best = i; }
        }
        return best;
    }

    private void syncListFromDrawn() {
        drawnList.getItems().clear();
        for (int i = 0; i < drawnPoints.size(); i++)
            drawnList.getItems().add(formatPoint(i + 1, drawnPoints.get(i)));
    }

    private void redrawCanvas() {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();

        // Background
        gc.setFill(Color.web("#e8f5e9"));
        gc.fillRect(0, 0, w, h);

        // Grid
        gc.setStroke(Color.web("#c8e6c9")); gc.setLineWidth(0.8);
        for (int i = 1; i < 10; i++) {
            gc.strokeLine(i * w / 10, 0, i * w / 10, h);
            gc.strokeLine(0, i * h / 10, w, i * h / 10);
        }

        // Outer border
        gc.setStroke(Color.web("#388e3c")); gc.setLineWidth(2);
        gc.strokeRect(1, 1, w - 2, h - 2);

        // Corner coordinates
        gc.setFill(Color.web("#555")); gc.setFont(Font.font("System", 10));
        gc.fillText(String.format("%.5f°N / %.5f°E", maxLat, minLon), 4, 13);
        gc.fillText(String.format("%.5f°N / %.5f°E", minLat, maxLon), w - 178, h - 4);

        // ── Parent zone boundary (constraint outline) ──────────────
        if (parentBoundary != null && parentBoundary.size() >= 3) {
            List<double[]> ppts = parentBoundary.getPoints();
            double[] pxs = ppts.stream().mapToDouble(p -> lngToX(p[1])).toArray();
            double[] pys = ppts.stream().mapToDouble(p -> latToY(p[0])).toArray();

            // Shade area OUTSIDE parent as forbidden (dark overlay)
            gc.setFill(Color.web("#000000", 0.10));
            gc.fillRect(0, 0, w, h);

            // Clear the inside (allowed area)
            gc.setFill(Color.web("#c8e6c9", 0.55));
            gc.fillPolygon(pxs, pys, ppts.size());

            // Parent boundary outline
            gc.setStroke(Color.web("#1b5e20"));
            gc.setLineWidth(2.5);
            gc.setLineDashes(9, 4);
            gc.strokePolygon(pxs, pys, ppts.size());
            gc.setLineDashes(0);

            // Label
            gc.setFill(Color.web("#1b5e20"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText("Zone boundary", pxs[0] + 4, pys[0] - 5);
        }

        if (drawnPoints.isEmpty()) {
            String msg = parentBoundary != null
                ? "Click inside the zone outline to start drawing"
                : "Click anywhere to place the first boundary point";
            gc.setFill(Color.web("#2e7d32")); gc.setFont(Font.font("System", 13));
            gc.fillText(msg, 40, h / 2);
            return;
        }

        // ── Child polygon fill ────────────────────────────────────
        if (drawnPoints.size() >= 3) {
            double[] xs = drawnPoints.stream().mapToDouble(p -> lngToX(p[1])).toArray();
            double[] ys = drawnPoints.stream().mapToDouble(p -> latToY(p[0])).toArray();
            gc.setFill(Color.web("#1565c0", 0.22));
            gc.fillPolygon(xs, ys, drawnPoints.size());
        }

        // Lines between points
        gc.setStroke(Color.web("#1565c0")); gc.setLineWidth(2); gc.setLineDashes(0);
        for (int i = 1; i < drawnPoints.size(); i++)
            gc.strokeLine(lngToX(drawnPoints.get(i-1)[1]), latToY(drawnPoints.get(i-1)[0]),
                          lngToX(drawnPoints.get(i)[1]),   latToY(drawnPoints.get(i)[0]));

        // Closing dashed line
        if (drawnPoints.size() >= 3) {
            gc.setLineDashes(6, 4);
            gc.strokeLine(lngToX(drawnPoints.get(drawnPoints.size()-1)[1]),
                          latToY(drawnPoints.get(drawnPoints.size()-1)[0]),
                          lngToX(drawnPoints.get(0)[1]), latToY(drawnPoints.get(0)[0]));
            gc.setLineDashes(0);
        }

        // Points
        for (int i = 0; i < drawnPoints.size(); i++) {
            double cx = lngToX(drawnPoints.get(i)[1]);
            double cy = latToY(drawnPoints.get(i)[0]);
            boolean isSel = i == selectedDrawnIdx;
            if (isSel) { gc.setStroke(Color.GOLD); gc.setLineWidth(3); gc.strokeOval(cx-11, cy-11, 22, 22); }
            gc.setFill(isSel ? Color.web("#e65100") : i == 0 ? Color.web("#0d47a1") : Color.web("#1565c0"));
            gc.fillOval(cx - 7, cy - 7, 14, 14);
            gc.setFill(Color.WHITE); gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            String num = String.valueOf(i + 1);
            gc.fillText(num, cx - (num.length() > 1 ? 4 : 3), cy + 4);
        }
    }

    private double lngToX(double lon)     { return (lon - minLon) / (maxLon - minLon) * canvas.getWidth(); }
    private double latToY(double lat)     { return (1.0 - (lat - minLat) / (maxLat - minLat)) * canvas.getHeight(); }
    private double canvasXToLon(double x) { return minLon + x / canvas.getWidth()  * (maxLon - minLon); }
    private double canvasYToLat(double y) { return maxLat - y / canvas.getHeight() * (maxLat - minLat); }
    private double clampLat(double v)     { return Math.max(minLat, Math.min(maxLat, v)); }
    private double clampLon(double v)     { return Math.max(minLon, Math.min(maxLon, v)); }

    // ══════════════════════════════════════════════════════════════════
    // TAB 2 — Manual Point Input
    // ══════════════════════════════════════════════════════════════════

    private Tab buildManualTab() {
        TextField latF = new TextField(); latF.setPromptText("Latitude  (−90 … 90)");
        TextField lonF = new TextField(); lonF.setPromptText("Longitude (−180 … 180)");

        Label modeLabel = new Label("Adding new point");
        modeLabel.getStyleClass().add("text-muted");
        modeLabel.setStyle("-fx-font-style:italic;");

        Button addBtn        = new Button("➕ Add Point");
        Button updateBtn     = new Button("✏ Update Point");
        Button cancelEditBtn = new Button("✕ Cancel");
        addBtn.getStyleClass().add("btn-primary");
        updateBtn.getStyleClass().add("btn-primary");
        updateBtn.setDisable(true);
        cancelEditBtn.getStyleClass().add("btn-secondary");
        cancelEditBtn.setVisible(false);

        Runnable enterAddMode = () -> {
            editManualIdx = -1;
            latF.clear(); lonF.clear();
            modeLabel.setText("Adding new point");
            modeLabel.setStyle("-fx-font-style:italic;");
            updateBtn.setDisable(true); cancelEditBtn.setVisible(false);
        };
        cancelEditBtn.setOnAction(e -> enterAddMode.run());

        addBtn.setOnAction(e -> {
            try {
                double lat = Double.parseDouble(latF.getText().trim());
                double lon = Double.parseDouble(lonF.getText().trim());
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
                if (parentBoundary != null && !parentBoundary.contains(lat, lon)) {
                    showError("This point is outside the zone boundary.\nAll sub-zone points must be inside the parent zone.");
                    return;
                }
                manualPoints.add(new double[]{lat, lon});
                rebuildLabels(manualPoints, manualPointLabels);
                latF.clear(); lonF.clear(); latF.requestFocus();
            } catch (NumberFormatException ex) { showError("Latitude: −90 to 90   |   Longitude: −180 to 180"); }
        });

        updateBtn.setOnAction(e -> {
            if (editManualIdx < 0 || editManualIdx >= manualPoints.size()) return;
            try {
                double lat = Double.parseDouble(latF.getText().trim());
                double lon = Double.parseDouble(lonF.getText().trim());
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
                if (parentBoundary != null && !parentBoundary.contains(lat, lon)) {
                    showError("This point is outside the zone boundary.\nAll sub-zone points must be inside the parent zone.");
                    return;
                }
                manualPoints.get(editManualIdx)[0] = lat;
                manualPoints.get(editManualIdx)[1] = lon;
                rebuildLabels(manualPoints, manualPointLabels);
                enterAddMode.run();
            } catch (NumberFormatException ex) { showError("Latitude: −90 to 90   |   Longitude: −180 to 180"); }
        });

        lonF.setOnAction(ev -> { if (editManualIdx >= 0) updateBtn.fire(); else addBtn.fire(); });

        HBox inputRow = new HBox(8, new Label("Lat:"), latF, new Label("Lon:"), lonF);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(latF, Priority.ALWAYS); HBox.setHgrow(lonF, Priority.ALWAYS);

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        HBox btnRow = new HBox(8, addBtn, updateBtn, cancelEditBtn, sp1, modeLabel);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        ListView<String> ptList = new ListView<>(manualPointLabels);
        ptList.setPrefHeight(180);
        VBox.setVgrow(ptList, Priority.ALWAYS);

        ptList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            int idx = n.intValue();
            if (idx >= 0 && idx < manualPoints.size()) {
                editManualIdx = idx;
                latF.setText(String.format("%.8f", manualPoints.get(idx)[0]));
                lonF.setText(String.format("%.8f", manualPoints.get(idx)[1]));
                modeLabel.setText("Editing point " + (idx + 1));
                modeLabel.setStyle("-fx-font-style:italic; -fx-text-fill:#f39c12; -fx-font-weight:bold;");
                updateBtn.setDisable(false); cancelEditBtn.setVisible(true);
            }
        });

        Button removeSel = new Button("✕ Remove");
        removeSel.getStyleClass().add("btn-danger");
        removeSel.setOnAction(e -> {
            int idx = ptList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) { manualPoints.remove(idx); rebuildLabels(manualPoints, manualPointLabels); enterAddMode.run(); }
        });

        Button moveUp = new Button("↑");
        moveUp.getStyleClass().add("btn-secondary");
        moveUp.setOnAction(e -> {
            int idx = ptList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                double[] tmp = manualPoints.get(idx);
                manualPoints.set(idx, manualPoints.get(idx - 1));
                manualPoints.set(idx - 1, tmp);
                rebuildLabels(manualPoints, manualPointLabels);
                ptList.getSelectionModel().select(idx - 1);
            }
        });

        Button moveDown = new Button("↓");
        moveDown.getStyleClass().add("btn-secondary");
        moveDown.setOnAction(e -> {
            int idx = ptList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < manualPoints.size() - 1) {
                double[] tmp = manualPoints.get(idx);
                manualPoints.set(idx, manualPoints.get(idx + 1));
                manualPoints.set(idx + 1, tmp);
                rebuildLabels(manualPoints, manualPointLabels);
                ptList.getSelectionModel().select(idx + 1);
            }
        });

        Button clearAll = new Button("🗑 Clear All");
        clearAll.getStyleClass().add("btn-danger");
        clearAll.setOnAction(e -> { manualPoints.clear(); manualPointLabels.clear(); enterAddMode.run(); });

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        HBox listActions = new HBox(6, removeSel, moveUp, moveDown, sp2, clearAll);
        listActions.setAlignment(Pos.CENTER_LEFT);
        listActions.setPadding(new Insets(2, 0, 4, 0));

        // Presets only shown when there is no parent constraint (parent bounds are precise)
        Label presetsHdr = new Label("QUICK SHAPE PRESETS");
        presetsHdr.getStyleClass().add("detail-section-title");
        presetsHdr.setMaxWidth(Double.MAX_VALUE);

        TextField rLat1 = numField("36.10", 78); TextField rLon1 = numField("3.00", 78);
        TextField rLat2 = numField("36.00", 78); TextField rLon2 = numField("3.10", 78);
        Button rectBtn = new Button("Apply Rectangle");
        rectBtn.getStyleClass().add("btn-secondary");
        rectBtn.setOnAction(e -> {
            try {
                List<double[]> pts = GoegraphicBoundries.createRectangle(
                    Double.parseDouble(rLat1.getText()), Double.parseDouble(rLon1.getText()),
                    Double.parseDouble(rLat2.getText()), Double.parseDouble(rLon2.getText())
                ).getPoints();
                if (parentBoundary != null && pts.stream().anyMatch(p -> !parentBoundary.contains(p[0], p[1]))) {
                    showError("Some rectangle corners are outside the zone boundary.");
                    return;
                }
                applyPresetToManual(pts);
            } catch (Exception ignored) { showError("Invalid rectangle coordinates."); }
        });
        HBox rectRow = new HBox(6, new Label("NW Lat:"), rLat1, new Label("Lon:"), rLon1,
            new Label("  SE Lat:"), rLat2, new Label("Lon:"), rLon2, rectBtn);
        rectRow.setAlignment(Pos.CENTER_LEFT);

        TextField cLat = numField("36.05", 78); TextField cLon = numField("3.05", 78);
        TextField cRad = numField("200", 68); TextField cPts = numField("20", 48);
        Button circBtn = new Button("Apply Circle");
        circBtn.getStyleClass().add("btn-secondary");
        circBtn.setOnAction(e -> {
            try {
                List<double[]> pts = GoegraphicBoundries.createCircle(
                    Double.parseDouble(cLat.getText()), Double.parseDouble(cLon.getText()),
                    Double.parseDouble(cRad.getText()), Math.max(3, Integer.parseInt(cPts.getText().trim()))
                ).getPoints();
                if (parentBoundary != null && pts.stream().anyMatch(p -> !parentBoundary.contains(p[0], p[1]))) {
                    showError("Some circle points are outside the zone boundary. Reduce the radius.");
                    return;
                }
                applyPresetToManual(pts);
            } catch (Exception ignored) { showError("Invalid circle parameters."); }
        });
        HBox circRow = new HBox(6, new Label("Center Lat:"), cLat, new Label("Lon:"), cLon,
            new Label("  Radius (m):"), cRad, new Label("Pts:"), cPts, circBtn);
        circRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(8, inputRow, btnRow, ptList, listActions,
            new Separator(), presetsHdr, rectRow, circRow);
        root.setPadding(new Insets(8));
        return new Tab("📍 Manual", root);
    }

    private void applyPresetToManual(List<double[]> pts) {
        manualPoints.clear(); manualPoints.addAll(pts);
        rebuildLabels(manualPoints, manualPointLabels); editManualIdx = -1;
    }

    // ══════════════════════════════════════════════════════════════════
    // TAB 3 — JSON File / AI Prompt
    // ══════════════════════════════════════════════════════════════════

    private Tab buildJsonTab() {
        Label statusLbl = new Label("No file loaded.");
        statusLbl.setWrapText(true); statusLbl.getStyleClass().add("text-muted");

        TextArea filePreview = new TextArea();
        filePreview.setEditable(false); filePreview.setPrefHeight(120);
        filePreview.getStyleClass().add("report-text");
        filePreview.setPromptText("File content appears here after loading...");

        Label parsedCountLbl = new Label("Parsed points:");
        ListView<String> parsedView = new ListView<>(jsonPointLabels);
        parsedView.setPrefHeight(100);
        jsonPointLabels.addListener((javafx.collections.ListChangeListener<String>) c ->
            parsedCountLbl.setText("Parsed points (" + jsonPointLabels.size() + "):"));

        Button browseBtn = new Button("📂 Browse & Load JSON");
        browseBtn.getStyleClass().add("btn-primary");
        browseBtn.setMaxWidth(Double.MAX_VALUE);
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open Boundary JSON");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fc.showOpenDialog(getDialogPane().getScene().getWindow());
            if (file == null) return;
            try {
                String content = Files.readString(file.toPath());
                filePreview.setText(content);
                List<double[]> parsed = parseJson(content);
                if (parentBoundary != null) {
                    long outside = parsed.stream().filter(p -> !parentBoundary.contains(p[0], p[1])).count();
                    if (outside > 0) {
                        statusLbl.setText("⚠  " + outside + " point(s) are outside the zone boundary — fix the JSON and reload.");
                        statusLbl.setStyle("-fx-text-fill:#f39c12; -fx-font-weight:bold;");
                        return;
                    }
                }
                jsonPoints.clear(); jsonPoints.addAll(parsed);
                rebuildLabels(jsonPoints, jsonPointLabels);
                statusLbl.setText("✅  Loaded " + parsed.size() + " points from \"" + file.getName() + "\"");
                statusLbl.setStyle("-fx-text-fill:#22c55e; -fx-font-weight:bold;");
            } catch (Exception ex) {
                statusLbl.setText("❌  Parse error: " + ex.getMessage());
                statusLbl.setStyle("-fx-text-fill:#ef4444;");
            }
        });

        // Build AI prompt — if parent boundary exists, include its coordinates as context
        StringBuilder promptSb = new StringBuilder();
        promptSb.append("Generate a geographic boundary JSON for a farm sub-zone named \"")
                .append(boundaryTitle).append("\".\n");
        if (parentBoundary != null && parentBoundary.size() >= 3) {
            promptSb.append("\nThe sub-zone MUST fit inside this parent zone boundary:\n[\n");
            for (double[] p : parentBoundary.getPoints())
                promptSb.append(String.format("  {\"lat\": %.6f, \"lon\": %.6f},\n", p[0], p[1]));
            promptSb.append("]\n");
            promptSb.append("All points of the sub-zone must be INSIDE those coordinates.\n");
        }
        promptSb.append("\nReply with ONLY valid JSON in this exact format:\n")
                .append("{\n  \"points\": [\n")
                .append("    {\"lat\": 36.1210, \"lon\": 3.4510},\n")
                .append("    {\"lat\": 36.1250, \"lon\": 3.4560},\n")
                .append("    {\"lat\": 36.1230, \"lon\": 3.4600}\n")
                .append("  ]\n}\n\n")
                .append("Or circle format:\n{\n  \"centerLat\": 36.1230,\n  \"centerLon\": 3.4555,\n")
                .append("  \"radiusMeters\": 100,\n  \"numPoints\": 20\n}\n\n")
                .append("Rules: lat ≈ 35–37 (Algeria), lon ≈ 2–4, minimum 3 points, no explanation.");
        String aiPrompt = promptSb.toString();

        Label aiHdr = new Label("AI PROMPT  (copy → paste to any AI assistant)");
        aiHdr.getStyleClass().add("detail-section-title");
        aiHdr.setMaxWidth(Double.MAX_VALUE);

        TextArea promptArea = new TextArea(aiPrompt);
        promptArea.setEditable(false); promptArea.setPrefHeight(200);
        promptArea.getStyleClass().add("report-text"); promptArea.setWrapText(true);

        Button copyBtn = new Button("📋 Copy Prompt to Clipboard");
        copyBtn.getStyleClass().add("btn-secondary");
        copyBtn.setMaxWidth(Double.MAX_VALUE);
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(aiPrompt);
            Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("✅ Copied!");
        });

        VBox root = new VBox(8,
            browseBtn, statusLbl,
            new Label("File preview:"), filePreview,
            parsedCountLbl, parsedView,
            new Separator(), aiHdr, promptArea, copyBtn);
        root.setPadding(new Insets(8));
        return new Tab("📄 JSON / AI", root);
    }

    // ══════════════════════════════════════════════════════════════════
    // JSON Parser
    // ══════════════════════════════════════════════════════════════════

    private List<double[]> parseJson(String raw) throws Exception {
        String json = raw.replaceAll("//[^\n]*", "").trim();
        if (json.contains("centerLat")) {
            double cLat = extractDouble(json, "centerLat");
            double cLon = extractDouble(json, "centerLon");
            double rad  = extractDouble(json, "radiusMeters");
            int    npts = (int) extractDouble(json, "numPoints");
            return GoegraphicBoundries.createCircle(cLat, cLon, rad, Math.max(3, npts)).getPoints();
        }
        Pattern p = Pattern.compile(
            "\"lat\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+).*?\"lon\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)",
            Pattern.DOTALL);
        Matcher m = p.matcher(json);
        List<double[]> pts = new ArrayList<>();
        while (m.find())
            pts.add(new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))});
        if (pts.isEmpty()) throw new Exception("No points found — verify the format.");
        return pts;
    }

    private double extractDouble(String json, String key) throws Exception {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)").matcher(json);
        if (!m.find()) throw new Exception("Missing field: \"" + key + "\"");
        return Double.parseDouble(m.group(1));
    }

    // ══════════════════════════════════════════════════════════════════
    // Shared helpers
    // ══════════════════════════════════════════════════════════════════

    private List<double[]> getActivePoints() {
        String t = tabPane.getSelectionModel().getSelectedItem().getText();
        if (t.contains("Draw"))   return drawnPoints;
        if (t.contains("Manual")) return manualPoints;
        return jsonPoints;
    }

    private void rebuildLabels(List<double[]> pts, ObservableList<String> labels) {
        labels.clear();
        for (int i = 0; i < pts.size(); i++) labels.add(formatPoint(i + 1, pts.get(i)));
    }

    private String formatPoint(int n, double[] p) {
        return String.format("%-3d   %.6f° N    %.6f° E", n, p[0], p[1]);
    }

    private TextField numField(String init, int w) {
        TextField f = new TextField(init); f.setPrefWidth(w); return f;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Input Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private HBox buildCustomHeader(String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("dialog-custom-header-sub");

        VBox textBox = new VBox(2, titleLbl, subLbl);

        Button maxBtn = new Button("⛶");
        maxBtn.getStyleClass().add("dialog-header-close-btn");
        maxBtn.setStyle("-fx-font-size: 14px;");
        maxBtn.setOnAction(e -> {
            if (getDialogPane().getScene().getWindow() instanceof javafx.stage.Stage st) {
                boolean nowMax = !st.isMaximized();
                st.setMaximized(nowMax);
                maxBtn.setText(nowMax ? "❐" : "⛶");
            }
        });

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-header-close-btn");
        closeBtn.setOnAction(e -> {
            Button footerCancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            if (footerCancel != null) footerCancel.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, iconLbl, textBox, spacer, maxBtn, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }
}
