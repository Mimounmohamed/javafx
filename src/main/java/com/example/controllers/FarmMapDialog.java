package com.example.controllers;

import Animals.Animal;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Farm.Farm;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import com.example.services.FarmService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class FarmMapDialog extends Dialog<Void> {

    // ── Zoom / pan state ─────────────────────────────────────────────
    private double zoomLevel = 1.0;
    private double panX = 0, panY = 0;
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;
    private double clickStartX, clickStartY;

    // ── Base viewport (geo coords, with 12% padding) ──────────────────
    private double baseMinLat, baseMaxLat, baseMinLon, baseMaxLon;
    private boolean hasGeoData = false;

    // ── Canvas ref ────────────────────────────────────────────────────
    private Canvas canvas;
    private final Farm farm;

    // ── Right-panel zone details ──────────────────────────────────────
    private VBox detailBox;

    public FarmMapDialog(Farm farm, List<String> styleSheets) {
        this.farm = farm;

        setTitle("Farm Map");
        setHeaderText(null);
        getDialogPane().setPrefWidth(1100);
        getDialogPane().setPrefHeight(700);
        getDialogPane().getButtonTypes().add(ButtonType.OK);

        setOnShown(ev -> {
            if (getDialogPane().getScene().getWindow() instanceof javafx.stage.Stage st) {
                st.setResizable(true);
                st.setMinWidth(700);
                st.setMinHeight(500);
            }
        });

        if (!styleSheets.isEmpty())
            getDialogPane().getStylesheets().add(styleSheets.get(0));

        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Close");
        okBtn.getStyleClass().add("btn-primary");

        // Compute base viewport
        computeBaseViewport();

        // ── Build layout ──────────────────────────────────────────────
        // Left: map pane (~70%)
        Pane mapPane = new Pane();
        HBox.setHgrow(mapPane, Priority.ALWAYS);
        mapPane.setMinWidth(400);

        canvas = new Canvas();
        canvas.widthProperty().bind(mapPane.widthProperty());
        canvas.heightProperty().bind(mapPane.heightProperty());
        mapPane.getChildren().add(canvas);

        // Toolbar above map
        Button zoomOutBtn = new Button("−");
        Button zoomInBtn  = new Button("＋");
        Button resetBtn   = new Button("⌂ Reset");
        zoomOutBtn.getStyleClass().add("btn-secondary");
        zoomInBtn.getStyleClass().add("btn-secondary");
        resetBtn.getStyleClass().add("btn-secondary");
        zoomOutBtn.setStyle("-fx-font-size: 13px; -fx-min-width: 30px;");
        zoomInBtn.setStyle("-fx-font-size: 13px; -fx-min-width: 30px;");

        zoomOutBtn.setOnAction(e -> { zoomLevel = clampZoom(zoomLevel * 0.87); redraw(); });
        zoomInBtn.setOnAction(e  -> { zoomLevel = clampZoom(zoomLevel * 1.15); redraw(); });
        resetBtn.setOnAction(e   -> { zoomLevel = 1.0; panX = 0; panY = 0; redraw(); });

        Label hintLbl = new Label("Scroll to zoom · Drag to pan · Click zone for details");
        hintLbl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        HBox toolbar = new HBox(6, zoomOutBtn, zoomInBtn, resetBtn, toolSpacer, hintLbl);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        toolbar.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        VBox leftPane = new VBox(0, toolbar, mapPane);
        VBox.setVgrow(mapPane, Priority.ALWAYS);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        // Right: detail panel (~30%)
        detailBox = new VBox(10);
        detailBox.setPadding(new Insets(12));
        showNoSelection();

        ScrollPane detailScroll = new ScrollPane(detailBox);
        detailScroll.setFitToWidth(true);
        detailScroll.setPrefWidth(310);
        detailScroll.setMinWidth(230);
        detailScroll.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 0 1;");

        HBox body = new HBox(0, leftPane, detailScroll);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Farm name and zone count
        int zoneCount = farm.getAllZones().size();
        String subtitle = FarmService.getInstance().getFarmName()
            + " · " + zoneCount + " zone" + (zoneCount == 1 ? "" : "s");

        VBox dialogRoot = new VBox(0,
            buildCustomHeader("🗺", "Farm Map", subtitle),
            body);
        VBox.setVgrow(body, Priority.ALWAYS);
        getDialogPane().setContent(dialogRoot);

        setResultConverter(bt -> null);

        // ── Canvas interactions ───────────────────────────────────────
        canvas.setOnScroll(e -> {
            zoomLevel = clampZoom(zoomLevel * (e.getDeltaY() > 0 ? 1.15 : 0.87));
            redraw();
        });

        canvas.setOnMousePressed(e -> {
            dragStartX    = e.getX();
            dragStartY    = e.getY();
            dragStartPanX = panX;
            dragStartPanY = panY;
            clickStartX   = e.getX();
            clickStartY   = e.getY();
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            double W  = canvas.getWidth();
            double H  = canvas.getHeight();
            double dLon = (baseMaxLon - baseMinLon) / zoomLevel;
            double dLat = (baseMaxLat - baseMinLat) / zoomLevel;
            panX = dragStartPanX - dx * dLon / W;
            // latitude is inverted on canvas (north = top), so dy positive = panning south
            panY = dragStartPanY + dy * dLat / H;
            redraw();
        });

        canvas.setOnMouseClicked(e -> {
            double dist = Math.hypot(e.getX() - clickStartX, e.getY() - clickStartY);
            if (dist < 5) {
                handleZoneClick(e.getX(), e.getY());
            }
        });

        // Draw once size is known
        canvas.widthProperty().addListener((o, ov, nv) -> redraw());
        canvas.heightProperty().addListener((o, ov, nv) -> redraw());
    }

    // ── Viewport calculation ─────────────────────────────────────────

    private void computeBaseViewport() {
        List<double[]> all = new ArrayList<>();
        if (farm.hasFarmBoundary())
            all.addAll(farm.getFarmBoundary().getPoints());
        for (ZONE z : farm.getAllZones())
            if (z.hasBoundaries())
                all.addAll(z.getBoundaries().getPoints());

        if (all.isEmpty()) { hasGeoData = false; return; }
        hasGeoData = true;

        double minLat = all.stream().mapToDouble(p -> p[0]).min().getAsDouble();
        double maxLat = all.stream().mapToDouble(p -> p[0]).max().getAsDouble();
        double minLon = all.stream().mapToDouble(p -> p[1]).min().getAsDouble();
        double maxLon = all.stream().mapToDouble(p -> p[1]).max().getAsDouble();
        double dLat = Math.max(0.001, maxLat - minLat);
        double dLon = Math.max(0.001, maxLon - minLon);
        baseMinLat = minLat - dLat * 0.12;
        baseMaxLat = maxLat + dLat * 0.12;
        baseMinLon = minLon - dLon * 0.12;
        baseMaxLon = maxLon + dLon * 0.12;
    }

    // ── Effective viewport with zoom + pan ───────────────────────────

    private double[] effectiveViewport() {
        double dLon = (baseMaxLon - baseMinLon) / zoomLevel;
        double dLat = (baseMaxLat - baseMinLat) / zoomLevel;
        double cLon = (baseMaxLon + baseMinLon) / 2.0 + panX;
        double cLat = (baseMaxLat + baseMinLat) / 2.0 + panY;
        return new double[]{
            cLon - dLon / 2, cLon + dLon / 2,
            cLat - dLat / 2, cLat + dLat / 2
        };
    }

    private double clampZoom(double z) {
        return Math.max(0.5, Math.min(20.0, z));
    }

    // ── Redraw ───────────────────────────────────────────────────────

    private void redraw() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        if (W <= 0 || H <= 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, W, H);

        // Background
        gc.setFill(Color.rgb(236, 252, 243));
        gc.fillRect(0, 0, W, H);

        if (!hasGeoData) {
            gc.setFill(Color.rgb(156, 163, 175));
            gc.setFont(Font.font("System", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No boundary data", W / 2, H / 2 - 10);
            gc.setFont(Font.font("System", 11));
            gc.fillText("Draw farm or zone boundaries to see the map", W / 2, H / 2 + 10);
            return;
        }

        double[] vp = effectiveViewport();
        double effMinLon = vp[0], effMaxLon = vp[1], effMinLat = vp[2], effMaxLat = vp[3];

        // Grid
        gc.setStroke(Color.rgb(160, 210, 180, 0.45));
        gc.setLineWidth(0.5);
        gc.setLineDashes();
        for (int i = 1; i <= 4; i++) {
            gc.strokeLine(W * i / 5.0, 0, W * i / 5.0, H);
            gc.strokeLine(0, H * i / 5.0, W, H * i / 5.0);
        }

        // Farm boundary
        if (farm.hasFarmBoundary()) {
            List<double[]> pts = farm.getFarmBoundary().getPoints();
            double[] xs = toXs(pts, W, effMinLon, effMaxLon);
            double[] ys = toYs(pts, H, effMinLat, effMaxLat);
            gc.setFill(Color.rgb(30, 80, 30, 0.04));
            gc.fillPolygon(xs, ys, pts.size());
            gc.setStroke(Color.rgb(55, 65, 81));
            gc.setLineWidth(2.0);
            gc.setLineDashes(10, 5);
            gc.strokePolygon(xs, ys, pts.size());
            gc.setLineDashes();
            // Farm name near top vertex
            double topY = Double.MAX_VALUE, topX = W / 2.0;
            for (int i = 0; i < xs.length; i++) {
                if (ys[i] < topY) { topY = ys[i]; topX = xs[i]; }
            }
            mapText(gc, FarmService.getInstance().getFarmName(),
                topX, Math.max(topY - 9, 12), Color.rgb(45, 55, 72), 9.5, true);
        }

        // Zone polygons
        for (ZONE z : farm.getAllZones()) {
            if (!z.hasBoundaries()) continue;
            Color fill, stroke;
            if (z instanceof LivestockZONE) {
                fill   = Color.rgb(34, 197, 94, 0.28);
                stroke = Color.rgb(22, 163, 74);
            } else if (z instanceof CropZONE) {
                fill   = Color.rgb(234, 179, 8, 0.30);
                stroke = Color.rgb(161, 110, 0);
            } else {
                fill   = Color.rgb(14, 165, 233, 0.28);
                stroke = Color.rgb(2, 132, 199);
            }
            List<double[]> pts = z.getBoundaries().getPoints();
            double[] xs = toXs(pts, W, effMinLon, effMaxLon);
            double[] ys = toYs(pts, H, effMinLat, effMaxLat);

            gc.setFill(fill);
            gc.fillPolygon(xs, ys, pts.size());
            gc.setStroke(stroke);
            gc.setLineWidth(1.5);
            gc.setLineDashes();
            gc.strokePolygon(xs, ys, pts.size());

            // Label at centroid
            double cx = 0, cy = 0;
            double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
            for (int i = 0; i < xs.length; i++) {
                cx += xs[i]; cy += ys[i];
                if (xs[i] < xMin) xMin = xs[i];
                if (xs[i] > xMax) xMax = xs[i];
            }
            cx /= xs.length; cy /= ys.length;
            int maxChars = Math.max(3, (int)((xMax - xMin) / 5.5));
            String label = z.getName().length() <= maxChars
                ? z.getName()
                : z.getName().substring(0, maxChars - 1) + "…";
            mapText(gc, label, cx, cy, Color.rgb(20, 20, 20), 9.5, false);
        }

        // Compass rose (top-right)
        drawCompass(gc, W - 22, 22);

        // Corner coordinate hints
        gc.setFill(Color.rgb(100, 116, 139));
        gc.setFont(Font.font("System", 8));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("%.3f°N", effMaxLat), 4, 10);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(String.format("%.3f°E", effMaxLon), W - 4, H - 4);
    }

    // ── Zone click detection ──────────────────────────────────────────

    private void handleZoneClick(double clickX, double clickY) {
        double W = canvas.getWidth(), H = canvas.getHeight();
        double[] vp = effectiveViewport();
        double effMinLon = vp[0], effMaxLon = vp[1], effMinLat = vp[2], effMaxLat = vp[3];

        for (ZONE z : farm.getAllZones()) {
            if (!z.hasBoundaries()) continue;
            List<double[]> pts = z.getBoundaries().getPoints();
            double[] xs = toXs(pts, W, effMinLon, effMaxLon);
            double[] ys = toYs(pts, H, effMinLat, effMaxLat);
            if (isInsidePolygon(xs, ys, clickX, clickY)) {
                showZoneDetail(z);
                return;
            }
        }
        showNoSelection();
    }

    private boolean isInsidePolygon(double[] xs, double[] ys, double px, double py) {
        int n = xs.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (((ys[i] > py) != (ys[j] > py))
                && (px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    // ── Right-panel content ───────────────────────────────────────────

    private void showNoSelection() {
        detailBox.getChildren().clear();
        Label placeholder = new Label("Click a zone to see details");
        placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        placeholder.setWrapText(true);
        detailBox.getChildren().add(placeholder);
    }

    private void showZoneDetail(ZONE z) {
        detailBox.getChildren().clear();

        // Header: zone name
        Label nameLbl = new Label(z.getName());
        nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        nameLbl.setWrapText(true);

        // Zone type badge
        String typeText;
        String typeBg;
        if (z instanceof LivestockZONE) {
            typeText = "Livestock";
            typeBg   = "#DCFCE7; -fx-text-fill: #16A34A;";
        } else if (z instanceof CropZONE) {
            typeText = "Crop";
            typeBg   = "#FEF9C3; -fx-text-fill: #854D0E;";
        } else {
            typeText = "Aquaculture";
            typeBg   = "#E0F2FE; -fx-text-fill: #0369A1;";
        }
        Label typeBadge = new Label(typeText);
        typeBadge.setStyle("-fx-background-color: #" + typeBg
            + " -fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: 600;");

        // Boundary info
        int nPts = z.getBoundaries().size();
        Label boundaryLbl = new Label("Boundary: " + nPts + " point" + (nPts == 1 ? "" : "s"));
        boundaryLbl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        detailBox.getChildren().addAll(nameLbl, typeBadge, boundaryLbl, new Separator());

        // Zone-type specific content
        if (z instanceof LivestockZONE lz) {
            buildLivestockDetail(lz);
        } else if (z instanceof CropZONE cz) {
            buildCropDetail(cz);
        } else if (z instanceof AquacultureZONE az) {
            buildAquaDetail(az);
        }
    }

    private void buildLivestockDetail(LivestockZONE lz) {
        List<Animal> animals = lz.getAnimals();
        Label hdr = sectionHeader("Animals (" + animals.size() + ")");
        detailBox.getChildren().add(hdr);

        if (animals.isEmpty()) {
            detailBox.getChildren().add(mutedLabel("No animals in this zone."));
        } else {
            for (Animal a : animals) {
                String icon = switch (a.getHealthStatus()) {
                    case Healthy     -> "✅";
                    case Sick        -> "⚠";
                    case Quarantined -> "🚨";
                };
                Label row = new Label(icon + "  " + a.getName()
                    + "  ·  " + a.getHealthStatus());
                row.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                row.setWrapText(true);
                detailBox.getChildren().add(row);
            }
        }
        Label total = new Label("Total: " + animals.size() + " animal" + (animals.size() == 1 ? "" : "s"));
        total.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-font-style: italic;");
        detailBox.getChildren().add(total);
    }

    private void buildCropDetail(CropZONE cz) {
        List<Crop> fields = cz.getFields();
        Label hdr = sectionHeader("Crops (" + fields.size() + ")");
        detailBox.getChildren().add(hdr);

        if (fields.isEmpty()) {
            detailBox.getChildren().add(mutedLabel("No crops in this zone."));
        } else {
            for (Crop c : fields) {
                Label row = new Label("🌱  " + c.getVariety()
                    + "  ·  " + c.getGrowthStage());
                row.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                row.setWrapText(true);
                detailBox.getChildren().add(row);
            }
        }
        double surface = cz.getSurfacePlanted();
        if (surface > 0) {
            Label surfLbl = new Label(String.format("Surface planted: %.2f ha", surface));
            surfLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-font-style: italic;");
            detailBox.getChildren().add(surfLbl);
        }
    }

    private void buildAquaDetail(AquacultureZONE az) {
        List<AquacultureSpecies> speciesList = az.getSpeciesList();
        Label hdr = sectionHeader("Species (" + speciesList.size() + ")");
        detailBox.getChildren().add(hdr);

        if (speciesList.isEmpty()) {
            detailBox.getChildren().add(mutedLabel("No species in this zone."));
        } else {
            for (AquacultureSpecies sp : speciesList) {
                Label row = new Label("🐟  " + sp.getName()
                    + "  ·  stock: " + sp.getNumSpecies());
                row.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                row.setWrapText(true);
                detailBox.getChildren().add(row);
            }
        }
    }

    // ── Drawing helpers ───────────────────────────────────────────────

    private double[] toXs(List<double[]> pts, double W, double minLon, double maxLon) {
        double[] xs = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++)
            xs[i] = (pts.get(i)[1] - minLon) / (maxLon - minLon) * W;
        return xs;
    }

    private double[] toYs(List<double[]> pts, double H, double minLat, double maxLat) {
        double[] ys = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++)
            ys[i] = (maxLat - pts.get(i)[0]) / (maxLat - minLat) * H;
        return ys;
    }

    private void mapText(GraphicsContext gc, String txt, double x, double y,
                         Color color, double sz, boolean bold) {
        gc.setFont(bold ? Font.font("System", FontWeight.BOLD, sz) : Font.font("System", sz));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.rgb(255, 255, 255, 0.82));
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0) gc.fillText(txt, x + dx, y + dy);
        gc.setFill(color);
        gc.fillText(txt, x, y);
    }

    private void drawCompass(GraphicsContext gc, double cx, double cy) {
        double r = 9;
        gc.setFill(Color.rgb(55, 65, 81, 0.13));
        gc.fillOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);
        gc.setFill(Color.rgb(220, 38, 38));
        gc.fillPolygon(new double[]{cx, cx - 4, cx + 4}, new double[]{cy - r, cy + 2, cy + 2}, 3);
        gc.setFill(Color.rgb(203, 213, 225));
        gc.fillPolygon(new double[]{cx, cx - 4, cx + 4}, new double[]{cy + r, cy - 2, cy - 2}, 3);
        gc.setFill(Color.rgb(45, 55, 72));
        gc.setFont(Font.font("System", FontWeight.BOLD, 7));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("N", cx, cy - r - 2);
    }

    // ── UI helpers ────────────────────────────────────────────────────

    private Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #374151; -fx-padding: 4 0 2 0;");
        return lbl;
    }

    private Label mutedLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        lbl.setWrapText(true);
        return lbl;
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
            Button footerClose = (Button) getDialogPane().lookupButton(ButtonType.OK);
            if (footerClose != null) footerClose.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, iconLbl, textBox, spacer, maxBtn, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }
}
