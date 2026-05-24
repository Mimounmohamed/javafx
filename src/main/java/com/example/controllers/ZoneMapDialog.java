package com.example.controllers;

import Animals.Animal;
import Entities.AquacultureSpecies;
import Entities.Crop;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.GoegraphicBoundries;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ZoneMapDialog extends Dialog<Void> {

    private static final int CANVAS_W = 680;
    private static final int CANVAS_H = 460;
    private static final int MARGIN   = 48;

    private final ZONE zone;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Label infoTitle;
    private final Label infoLabel;

    // ── Geo coordinate mode (when zone has lat/lon boundary) ──────────
    private boolean hasGeoBounds = false;
    private double geoMinLat, geoMaxLat, geoMinLon, geoMaxLon;

    // ── Fallback normalized (0–1) positions for items without geo bounds
    private final Map<String, double[]> positions = new HashMap<>();
    private String  selected;
    private boolean dragging;

    // ── Constructors ──────────────────────────────────────────────────

    public ZoneMapDialog(ZONE zone, List<String> styleSheets) {
        this(zone, styleSheets, null);
    }

    public ZoneMapDialog(ZONE zone, List<String> styleSheets, String preSelectedKey) {
        this.zone      = zone;
        this.canvas    = new Canvas(CANVAS_W, CANVAS_H);
        this.gc        = canvas.getGraphicsContext2D();
        this.infoTitle = new Label("—");
        this.infoLabel = new Label("Click an entity to see details");
        this.selected  = preSelectedKey;

        initGeoBounds();
        initFallbackPositions();
        buildUI(styleSheets);
        setupInteraction();
        redraw();
        if (preSelectedKey != null) showInfo(preSelectedKey);
    }

    // ── Geo coordinate system ─────────────────────────────────────────

    private void initGeoBounds() {
        if (zone.hasBoundaries() && zone.getBoundaries().size() >= 2) {
            List<double[]> pts = zone.getBoundaries().getPoints();
            double pad  = 0.0003;
            geoMinLat   = pts.stream().mapToDouble(p -> p[0]).min().orElse(35.99) - pad;
            geoMaxLat   = pts.stream().mapToDouble(p -> p[0]).max().orElse(36.01) + pad;
            geoMinLon   = pts.stream().mapToDouble(p -> p[1]).min().orElse(2.99)  - pad;
            geoMaxLon   = pts.stream().mapToDouble(p -> p[1]).max().orElse(3.01)  + pad;
            hasGeoBounds = true;
        }
    }

    // lat/lon → canvas pixel (geo mode)
    private double gX(double lon) { return MARGIN + (lon - geoMinLon) / (geoMaxLon - geoMinLon) * (CANVAS_W - 2*MARGIN); }
    private double gY(double lat) { return CANVAS_H - MARGIN - (lat - geoMinLat) / (geoMaxLat - geoMinLat) * (CANVAS_H - 2*MARGIN); }

    // normalized 0–1 → canvas pixel (fallback mode)
    private double toPx(double nx) { return MARGIN + nx * (CANVAS_W - 2*MARGIN); }
    private double toPy(double ny) { return MARGIN + ny * (CANVAS_H - 2*MARGIN); }
    private double toNormX(double px) { return (px - MARGIN) / (CANVAS_W - 2*MARGIN); }
    private double toNormY(double py) { return (py - MARGIN) / (CANVAS_H - 2*MARGIN); }
    private double clamp(double v) { return Math.max(0.05, Math.min(0.95, v)); }

    // centroid of a lat/lon polygon → canvas pixel
    private double[] centroidPx(List<double[]> pts) {
        double lat = pts.stream().mapToDouble(p -> p[0]).average().orElse(0);
        double lon = pts.stream().mapToDouble(p -> p[1]).average().orElse(0);
        return new double[]{ gX(lon), gY(lat) };
    }

    // ── Fallback positions (for items that have no geo boundary) ──────

    private void initFallbackPositions() {
        if (zone instanceof LivestockZONE lz) {
            for (Animal a : lz.getAnimals())
                positions.computeIfAbsent(key(a), k -> pseudoRandom(a.getName()));
        } else if (zone instanceof CropZONE cz) {
            List<Crop> fields = cz.getFields();
            for (int i = 0; i < fields.size(); i++) {
                if (!fields.get(i).hasBoundary()) {
                    int idx = i;
                    positions.computeIfAbsent(key(fields.get(i)), k -> gridPos(idx, fields.size()));
                }
            }
        } else if (zone instanceof AquacultureZONE az) {
            List<AquacultureSpecies> sps = az.getSpeciesList();
            for (int i = 0; i < sps.size(); i++) {
                if (!sps.get(i).hasBoundary()) {
                    int idx = i;
                    positions.computeIfAbsent(key(sps.get(i)), k -> gridPos(idx, sps.size()));
                }
            }
        }
    }

    private double[] pseudoRandom(String seed) {
        Random rng = new Random(seed.hashCode() ^ 0xDEAD_BEEF);
        double m = 0.12;
        return new double[]{ m + rng.nextDouble() * (1 - 2*m), m + rng.nextDouble() * (1 - 2*m) };
    }

    private double[] gridPos(int idx, int total) {
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(total)));
        int col  = idx % cols, row = idx / cols;
        int rows = (total + cols - 1) / cols;
        double m = 0.15, cw = (1 - 2*m) / Math.max(1, cols), ch = (1 - 2*m) / Math.max(1, rows);
        return new double[]{ m + col*cw + cw*0.5, m + row*ch + ch*0.5 };
    }

    // ── Public key helpers ────────────────────────────────────────────

    public static String key(Animal a)             { return "A:" + a.getId(); }
    public static String key(Crop c)               { return "C:" + c.getId(); }
    public static String key(AquacultureSpecies s) { return "S:" + s.getId(); }

    // ── UI ────────────────────────────────────────────────────────────

    private void buildUI(List<String> styleSheets) {
        setTitle("Zone Map — " + zone.getName());
        setHeaderText(null);

        infoTitle.getStyleClass().add("detail-section-title");
        infoTitle.setMaxWidth(Double.MAX_VALUE);
        infoLabel.setWrapText(true);
        infoLabel.getStyleClass().add("text-muted");
        infoLabel.setMinHeight(100);

        Label hintLbl = new Label(hasGeoBounds
            ? "Geo-coordinates active — shapes shown at real position"
            : "Drag entities to reposition them on the map");
        hintLbl.getStyleClass().add("text-muted");
        hintLbl.setWrapText(true);

        VBox sidePanel = new VBox(10, infoTitle, infoLabel, new Separator(), hintLbl);
        sidePanel.setPadding(new Insets(12));
        sidePanel.setPrefWidth(200);
        sidePanel.getStyleClass().add("kpi-card");
        VBox.setVgrow(sidePanel, Priority.ALWAYS);

        HBox main = new HBox(12, canvas, sidePanel);
        main.setPadding(new Insets(12));

        String zoneType = zone instanceof ZONES.AquacultureZONE ? "Aquaculture"
            : zone instanceof ZONES.CropZONE ? "Crop" : "Livestock";

        VBox root = new VBox(0,
            buildCustomHeader("📍", "Zone Map — " + zone.getName(),
                zoneType + " zone · " + (hasGeoBounds ? "geo-coordinates active" : "normalized positions")),
            main);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setPrefWidth(CANVAS_W + 240);
        getDialogPane().setMinHeight(CANVAS_H + 80);
        if (!styleSheets.isEmpty())
            getDialogPane().getStylesheets().addAll(styleSheets);
        setResultConverter(bt -> null);
    }

    private HBox buildCustomHeader(String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-custom-header-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-custom-header-title");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("dialog-custom-header-sub");

        VBox textBox = new VBox(2, titleLbl, subLbl);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-header-close-btn");
        closeBtn.setOnAction(e -> {
            Button footerClose = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
            if (footerClose != null) footerClose.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, iconLbl, textBox, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-custom-header");
        return header;
    }

    // ── Interaction — only active for fallback (normalized) items ─────

    private void setupInteraction() {
        canvas.setOnMousePressed(e -> {
            String hit = hitTest(e.getX(), e.getY());
            if (hit != null) {
                selected = hit;
                dragging = true;
                showInfo(hit);
            } else {
                selected = null;
                infoTitle.setText("—");
                infoLabel.setText("Click an entity to see details");
            }
            redraw();
        });

        canvas.setOnMouseDragged(e -> {
            // Only drag items that use fallback (normalized) positions
            if (dragging && selected != null && positions.containsKey(selected)) {
                positions.put(selected, new double[]{
                    clamp(toNormX(e.getX())), clamp(toNormY(e.getY()))
                });
                redraw();
            }
        });

        canvas.setOnMouseReleased(e -> dragging = false);
    }

    /** Hit test: checks geo-boundary centroids first, then fallback positions. */
    private String hitTest(double px, double py) {
        double bestDist = 22;
        String best = null;

        // Check items with geo boundaries
        if (hasGeoBounds) {
            if (zone instanceof CropZONE cz) {
                for (Crop c : cz.getFields()) {
                    if (c.hasBoundary()) {
                        double[] ctr = centroidPx(c.getBoundary().getPoints());
                        double d = Math.hypot(px - ctr[0], py - ctr[1]);
                        if (d < bestDist) { bestDist = d; best = key(c); }
                    }
                }
            } else if (zone instanceof AquacultureZONE az) {
                for (AquacultureSpecies s : az.getSpeciesList()) {
                    if (s.hasBoundary()) {
                        double[] ctr = centroidPx(s.getBoundary().getPoints());
                        double d = Math.hypot(px - ctr[0], py - ctr[1]);
                        if (d < bestDist) { bestDist = d; best = key(s); }
                    }
                }
            }
        }

        // Check fallback normalized positions
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            double cx = toPx(entry.getValue()[0]);
            double cy = toPy(entry.getValue()[1]);
            double d  = Math.hypot(px - cx, py - cy);
            if (d < bestDist) { bestDist = d; best = entry.getKey(); }
        }
        return best;
    }

    // ── Rendering ─────────────────────────────────────────────────────

    private void redraw() {
        gc.clearRect(0, 0, CANVAS_W, CANVAS_H);

        Color bg = (zone instanceof AquacultureZONE) ? Color.web("#d0e8f5") : Color.web("#d4edda");
        gc.setFill(bg); gc.fillRect(0, 0, CANVAS_W, CANVAS_H);

        drawGrid();
        drawZoneBoundary();
        drawCompass();

        if (zone instanceof LivestockZONE lz)        drawAnimals(lz);
        else if (zone instanceof CropZONE cz)        drawFields(cz);
        else if (zone instanceof AquacultureZONE az) drawSpecies(az);

        gc.setFill(Color.web("#2d4a2d"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(zone.getName(), MARGIN + 6, MARGIN - 12);
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#88aa88", 0.3)); gc.setLineWidth(1);
        for (int x = MARGIN; x <= CANVAS_W - MARGIN; x += 40)
            gc.strokeLine(x, MARGIN, x, CANVAS_H - MARGIN);
        for (int y = MARGIN; y <= CANVAS_H - MARGIN; y += 40)
            gc.strokeLine(MARGIN, y, CANVAS_W - MARGIN, y);
    }

    private void drawZoneBoundary() {
        if (!zone.hasBoundaries()) {
            gc.setStroke(Color.web("#4a7a4a")); gc.setLineWidth(2.5);
            gc.setLineDashes(); gc.strokeRect(MARGIN, MARGIN, CANVAS_W - 2*MARGIN, CANVAS_H - 2*MARGIN);
            return;
        }
        List<double[]> pts = zone.getBoundaries().getPoints();
        if (pts.size() < 2) return;

        double[] xs = pts.stream().mapToDouble(p -> gX(p[1])).toArray();
        double[] ys = pts.stream().mapToDouble(p -> gY(p[0])).toArray();

        gc.setFill(zone instanceof AquacultureZONE ? Color.web("#a0c8e8", 0.4) : Color.web("#a0d8a0", 0.4));
        gc.fillPolygon(xs, ys, pts.size());
        gc.setStroke(Color.web("#2e6b2e")); gc.setLineWidth(2.5); gc.setLineDashes();
        gc.strokePolygon(xs, ys, pts.size());
    }

    private void drawCompass() {
        double cx = CANVAS_W - MARGIN + 24, cy = MARGIN - 18;
        gc.setFill(Color.web("#4a7a4a")); gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText("N", cx - 3, cy - 8);
        gc.setStroke(Color.web("#4a7a4a")); gc.setLineWidth(1.5);
        gc.strokeLine(cx, cy - 6, cx, cy + 6); gc.strokeLine(cx - 6, cy, cx + 6, cy);
    }

    // ── Livestock: always normalized positions ────────────────────────

    private void drawAnimals(LivestockZONE lz) {
        for (Animal a : lz.getAnimals()) {
            String k   = key(a);
            double[] p = positions.getOrDefault(k, new double[]{0.5, 0.5});
            double cx  = toPx(p[0]), cy = toPy(p[1]);
            boolean sel = k.equals(selected);

            Color fill = a.isSick()        ? Color.web("#e74c3c")
                       : a.isQuarantined() ? Color.web("#f39c12")
                       : Color.web("#27ae60");

            if (sel) { gc.setStroke(Color.GOLD); gc.setLineWidth(3.5); gc.strokeOval(cx-16, cy-16, 32, 32); }
            gc.setFill(fill); gc.fillOval(cx - 11, cy - 11, 22, 22);
            gc.setStroke(Color.web("#1a1a1a", 0.5)); gc.setLineWidth(1); gc.strokeOval(cx - 11, cy - 11, 22, 22);

            gc.setFill(Color.WHITE); gc.setFont(Font.font("System", FontWeight.BOLD, 8));
            String abbr = a.getSpecies().substring(0, Math.min(2, a.getSpecies().length())).toUpperCase();
            gc.fillText(abbr, cx - abbr.length() * 3.0, cy + 3);
            gc.setFill(Color.web("#1a1a1a"));
            gc.setFont(Font.font("System", sel ? FontWeight.BOLD : FontWeight.NORMAL, 11));
            gc.fillText(a.getName(), cx + 14, cy + 4);
        }
    }

    // ── Crop: draw geo polygon when boundary exists ───────────────────

    private static final Color[] FIELD_COLORS = {
        Color.web("#8bc34a"), Color.web("#cddc39"), Color.web("#ffc107"),
        Color.web("#ff9800"), Color.web("#4caf50"), Color.web("#a5d6a7")
    };

    private void drawFields(CropZONE cz) {
        List<Crop> fields = cz.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Crop c    = fields.get(i);
            String k  = key(c);
            Color col = FIELD_COLORS[i % FIELD_COLORS.length];
            boolean sel = k.equals(selected);

            if (c.hasBoundary() && hasGeoBounds) {
                drawFieldPolygon(c, col, sel);
            } else {
                drawFieldRect(c, k, col, sel, i, fields.size());
            }
        }
    }

    private void drawFieldPolygon(Crop c, Color col, boolean sel) {
        List<double[]> pts = c.getBoundary().getPoints();
        double[] xs = pts.stream().mapToDouble(p -> gX(p[1])).toArray();
        double[] ys = pts.stream().mapToDouble(p -> gY(p[0])).toArray();

        gc.setFill(col.deriveColor(0, 1, 1, 0.75));
        gc.fillPolygon(xs, ys, pts.size());

        // Hatch texture
        gc.setStroke(col.darker().deriveColor(0, 1, 1, 0.4));
        gc.setLineWidth(0.8);
        double minX = xs[0], maxX = xs[0], minY = ys[0], maxY = ys[0];
        for (int j = 1; j < xs.length; j++) {
            minX = Math.min(minX, xs[j]); maxX = Math.max(maxX, xs[j]);
            minY = Math.min(minY, ys[j]); maxY = Math.max(maxY, ys[j]);
        }
        for (double d = minX - (maxY - minY); d < maxX + (maxY - minY); d += 8)
            gc.strokeLine(d, minY, d + (maxY - minY), maxY);

        if (sel) {
            gc.setStroke(Color.GOLD); gc.setLineWidth(3.5);
        } else {
            gc.setStroke(col.darker()); gc.setLineWidth(1.8);
        }
        gc.strokePolygon(xs, ys, pts.size());

        double[] ctr = centroidPx(pts);
        if (sel) { gc.setStroke(Color.GOLD); gc.setLineWidth(2.5); gc.strokeOval(ctr[0]-8, ctr[1]-8, 16, 16); }
        gc.setFill(Color.web("#1a1a1a"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.fillText(c.getVariety(), ctr[0] + 6, ctr[1] - 2);
        gc.setFont(Font.font("System", 9));
        gc.fillText(c.getGrowthStage().toString(), ctr[0] + 6, ctr[1] + 10);
        if (c.isReadyForHarvest()) {
            gc.setFill(Color.web("#27ae60")); gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.fillText("✅ READY", ctr[0] + 6, ctr[1] + 21);
        }
    }

    private void drawFieldRect(Crop c, String k, Color col, boolean sel, int i, int total) {
        double[] p = positions.getOrDefault(k, gridPos(i, total));
        double cx = toPx(p[0]), cy = toPy(p[1]);

        if (sel) { gc.setStroke(Color.GOLD); gc.setLineWidth(3); gc.strokeRect(cx-21, cy-15, 42, 30); }
        gc.setFill(col); gc.fillRect(cx - 18, cy - 12, 36, 24);
        gc.setStroke(Color.web("#1a1a1a", 0.5)); gc.setLineWidth(1); gc.strokeRect(cx - 18, cy - 12, 36, 24);
        gc.setStroke(Color.web("#1a1a1a", 0.12)); gc.setLineWidth(0.5);
        for (int d = -30; d < 40; d += 7)
            gc.strokeLine(cx-18+Math.max(0,d), cy-12, cx-18+Math.min(36,36+d), cy+12);
        gc.setFill(Color.web("#1a1a1a")); gc.setFont(Font.font("System", FontWeight.BOLD, 9));
        gc.fillText(c.getVariety(), cx + 20, cy - 1);
        gc.setFont(Font.font("System", 9)); gc.fillText(c.getGrowthStage().toString(), cx + 20, cy + 11);
    }

    // ── Aquaculture: draw geo polygon when boundary exists ────────────

    private static final Color[] TUB_COLORS = {
        Color.web("#29b6f6"), Color.web("#0288d1"), Color.web("#4fc3f7"),
        Color.web("#0097a7"), Color.web("#00bcd4"), Color.web("#006064")
    };

    private void drawSpecies(AquacultureZONE az) {
        List<AquacultureSpecies> sps = az.getSpeciesList();
        for (int i = 0; i < sps.size(); i++) {
            AquacultureSpecies s = sps.get(i);
            String k  = key(s);
            Color col = TUB_COLORS[i % TUB_COLORS.length];
            boolean sel = k.equals(selected);

            if (s.hasBoundary() && hasGeoBounds) {
                drawTubPolygon(s, col, sel);
            } else {
                drawTubCircle(s, k, col, sel, i, sps.size());
            }
        }
    }

    private void drawTubPolygon(AquacultureSpecies s, Color col, boolean sel) {
        List<double[]> pts = s.getBoundary().getPoints();
        double[] xs = pts.stream().mapToDouble(p -> gX(p[1])).toArray();
        double[] ys = pts.stream().mapToDouble(p -> gY(p[0])).toArray();

        gc.setFill(col.deriveColor(0, 1, 1, 0.7));
        gc.fillPolygon(xs, ys, pts.size());
        if (sel) { gc.setStroke(Color.GOLD); gc.setLineWidth(3.5); }
        else      { gc.setStroke(Color.web("#01579b")); gc.setLineWidth(2); }
        gc.strokePolygon(xs, ys, pts.size());

        double[] ctr = centroidPx(pts);
        String cnt = String.valueOf(s.getNumSpecies());
        gc.setFill(Color.WHITE); gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.fillText(cnt, ctr[0] - cnt.length() * 3.5, ctr[1] + 4);
        gc.setFill(Color.web("#1a1a1a")); gc.setFont(Font.font("System", sel ? FontWeight.BOLD : FontWeight.NORMAL, 11));
        gc.fillText(s.getName(), ctr[0] + 8, ctr[1] + 4);
    }

    private void drawTubCircle(AquacultureSpecies s, String k, Color col, boolean sel, int i, int total) {
        double[] p = positions.getOrDefault(k, gridPos(i, total));
        double cx = toPx(p[0]), cy = toPy(p[1]);

        if (sel) { gc.setStroke(Color.GOLD); gc.setLineWidth(3.5); gc.strokeOval(cx-23, cy-23, 46, 46); }
        gc.setFill(col); gc.fillOval(cx - 18, cy - 18, 36, 36);
        gc.setStroke(Color.web("#01579b")); gc.setLineWidth(1.5); gc.strokeOval(cx - 18, cy - 18, 36, 36);
        String cnt = String.valueOf(s.getNumSpecies());
        gc.setFill(Color.WHITE); gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText(cnt, cx - cnt.length() * 3.0, cy + 4);
        gc.setFill(Color.web("#1a1a1a")); gc.setFont(Font.font("System", sel ? FontWeight.BOLD : FontWeight.NORMAL, 11));
        gc.fillText(s.getName(), cx + 21, cy + 4);
    }

    // ── Info panel ────────────────────────────────────────────────────

    private void showInfo(String k) {
        if (zone instanceof LivestockZONE lz) {
            lz.getAnimals().stream().filter(a -> key(a).equals(k)).findFirst().ifPresent(a -> {
                infoTitle.setText(a.getName());
                infoLabel.setText("Species: " + a.getSpecies() + "\nType: " + a.getType()
                    + "\nAge: " + a.getAge() + " yrs\nWeight: " + a.getWeight() + " kg"
                    + "\nHealth: " + a.getHealthStatus()
                    + "\n" + (a.hasGPSCollar() ? "GPS Collar: ✓" : "GPS Collar: —"));
            });
        } else if (zone instanceof CropZONE cz) {
            cz.getFields().stream().filter(c -> key(c).equals(k)).findFirst().ifPresent(c -> {
                infoTitle.setText(c.getVariety());
                infoLabel.setText("Type: " + c.getCropType() + "\nStage: " + c.getGrowthStage()
                    + "\nPlanted: " + c.getPlantingDate().toString().substring(0, 10)
                    + "\nHarvest by: " + c.getExpectedHarvestDate().toString().substring(0, 10)
                    + "\n" + (c.wasHarvested() ? String.format("Yield: %.1f kg", c.getYieldKg()) : "Not yet harvested")
                    + "\nBoundary: " + (c.hasBoundary() ? c.getBoundary().size() + " pts ✓" : "not set")
                    + (c.isReadyForHarvest() ? "\n✅ READY TO HARVEST" : ""));
            });
        } else if (zone instanceof AquacultureZONE az) {
            az.getSpeciesList().stream().filter(s -> key(s).equals(k)).findFirst().ifPresent(s -> {
                infoTitle.setText(s.getName());
                infoLabel.setText("Count: " + s.getNumSpecies()
                    + "\n" + String.format("Survival: %.1f%%", s.getCycleSurvivalRatePercent())
                    + "\n" + String.format("Harvested: %.2f kg", s.getTotalHarvestWeightKg())
                    + "\nSessions: " + s.getHarvestCount()
                    + "\nBoundary: " + (s.hasBoundary() ? s.getBoundary().size() + " pts ✓" : "not set"));
            });
        }
    }
}
