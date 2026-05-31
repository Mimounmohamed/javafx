package com.example.controllers;

import Alerts.Alert;
import Alerts.AlertSeverity;
import Farm.Farm;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import com.example.services.AlertService;
import com.example.services.FarmService;
import com.example.services.SensorService;
import com.example.services.ZoneService;
import com.example.utils.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label kpiAnimals;
    @FXML private Label kpiSickAnimals;
    @FXML private Label kpiZones;
    @FXML private Label kpiTotalZones;
    @FXML private Label kpiSensors;
    @FXML private Label kpiAlerts;
    @FXML private Label kpiFields;
    @FXML private Label kpiSpecies;
    @FXML private Label    lblFarmSummary;
    @FXML private FlowPane summaryChipsPane;
    @FXML private VBox     chartContainer;

    @FXML private TableView<Alert>           recentAlertsTable;
    @FXML private TableColumn<Alert, String> colAlertTime;
    @FXML private TableColumn<Alert, String> colAlertSeverity;
    @FXML private TableColumn<Alert, String> colAlertMsg;

    @FXML
    public void initialize() {
        loadKpis();
        buildSummaryChips();
        buildFarmMap();
        buildRecentAlertsTable();
    }

    // ── KPI cards ────────────────────────────────────────────────────

    private void loadKpis() {
        Farm farm = FarmService.getInstance().getFarm();
        Farm.FarmStats stats = farm.getStats();
        kpiAnimals.setText(String.valueOf(stats.totalAnimals));
        kpiSickAnimals.setText(String.valueOf(stats.sickAnimals));
        kpiZones.setText(String.valueOf(ZoneService.getInstance().getActiveZoneCount()));
        kpiTotalZones.setText(String.valueOf(farm.getTotalZoneCount()));
        kpiSensors.setText(String.valueOf(SensorService.getInstance().getActiveSensorCount()));
        kpiAlerts.setText(String.valueOf(AlertService.getInstance().getActiveAlertCount()));
        kpiFields.setText(String.valueOf(stats.totalFields));
        kpiSpecies.setText(String.valueOf(stats.totalSpecies));
        lblFarmSummary.setText(farm.getSummary());
    }

    // ── Farm summary chips ───────────────────────────────────────────

    private void buildSummaryChips() {
        summaryChipsPane.getChildren().clear();
        String summary = FarmService.getInstance().getFarm().getSummary();
        String[] parts = summary.split("\\s*\\|\\s*");
        String[] emojis = {"👤", "📍", "🐾", "🗺", "📡", "⚠"};
        for (int i = 0; i < parts.length; i++) {
            String prefix = i < emojis.length ? emojis[i] + "  " : "";
            Label chip = new Label(prefix + parts[i].trim());
            boolean isAlert = parts[i].toLowerCase().contains("alert");
            chip.getStyleClass().add(isAlert ? "summary-chip-alert" : "summary-chip");
            summaryChipsPane.getChildren().add(chip);
        }
    }

    // ── Farm map ─────────────────────────────────────────────────────

    private void buildFarmMap() {
        Farm farm = FarmService.getInstance().getFarm();

        Pane mapPane = new Pane();
        VBox.setVgrow(mapPane, Priority.ALWAYS);
        mapPane.setMinHeight(260);

        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(mapPane.widthProperty());
        canvas.heightProperty().bind(mapPane.heightProperty());
        mapPane.getChildren().add(canvas);
        chartContainer.getChildren().add(mapPane);

        Runnable draw = () ->
            drawFarmMap(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), farm);
        canvas.widthProperty().addListener((o, ov, nv) -> draw.run());
        canvas.heightProperty().addListener((o, ov, nv) -> draw.run());
        Platform.runLater(draw);

        // Add "Expand" button to the title VBox (first child of chartContainer)
        if (!chartContainer.getChildren().isEmpty()
                && chartContainer.getChildren().get(0) instanceof VBox titleBox) {
            Button expandBtn = new Button("⛶  Expand");
            expandBtn.getStyleClass().add("btn-secondary");
            expandBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
            expandBtn.setOnAction(e ->
                new FarmMapDialog(farm, chartContainer.getScene().getStylesheets()).showAndWait());
            Region titleSpacer = new Region();
            HBox.setHgrow(titleSpacer, Priority.ALWAYS);
            HBox titleRow = new HBox(titleSpacer, expandBtn);
            titleRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            titleBox.getChildren().add(titleRow);
        }
    }

    private void drawFarmMap(GraphicsContext gc, double W, double H, Farm farm) {
        if (W <= 0 || H <= 0) return;
        gc.clearRect(0, 0, W, H);

        // Gather all geo points to determine viewport
        List<double[]> all = new ArrayList<>();
        if (farm.hasFarmBoundary())
            all.addAll(farm.getFarmBoundary().getPoints());
        for (ZONE z : farm.getAllZones())
            if (z.hasBoundaries())
                all.addAll(z.getBoundaries().getPoints());

        // Background (light terrain green)
        gc.setFill(Color.rgb(236, 252, 243));
        gc.fillRect(0, 0, W, H);

        if (all.isEmpty()) {
            gc.setFill(Color.rgb(156, 163, 175));
            gc.setFont(Font.font("System", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No boundary data", W / 2, H / 2 - 10);
            gc.setFont(Font.font("System", 11));
            gc.fillText("Draw farm or zone boundaries to see the map", W / 2, H / 2 + 10);
            return;
        }

        // Bounding box with 12 % padding on each side
        double minLat = all.stream().mapToDouble(p -> p[0]).min().getAsDouble();
        double maxLat = all.stream().mapToDouble(p -> p[0]).max().getAsDouble();
        double minLon = all.stream().mapToDouble(p -> p[1]).min().getAsDouble();
        double maxLon = all.stream().mapToDouble(p -> p[1]).max().getAsDouble();
        double dLat = Math.max(0.001, maxLat - minLat);
        double dLon = Math.max(0.001, maxLon - minLon);
        minLat -= dLat * 0.12;  maxLat += dLat * 0.12;
        minLon -= dLon * 0.12;  maxLon += dLon * 0.12;
        final double fML = minLat, fXL = maxLat, fMO = minLon, fXO = maxLon;

        // Subtle coordinate grid
        gc.setStroke(Color.rgb(160, 210, 180, 0.45));
        gc.setLineWidth(0.5);
        gc.setLineDashes();
        for (int i = 1; i <= 4; i++) {
            gc.strokeLine(W * i / 5.0, 0, W * i / 5.0, H);
            gc.strokeLine(0, H * i / 5.0, W, H * i / 5.0);
        }

        // ── Farm boundary (dashed outline) ────────────────────────────
        if (farm.hasFarmBoundary()) {
            List<double[]> pts = farm.getFarmBoundary().getPoints();
            double[] xs = toXs(pts, W, fMO, fXO);
            double[] ys = toYs(pts, H, fML, fXL);
            // Very faint interior fill
            gc.setFill(Color.rgb(30, 80, 30, 0.04));
            gc.fillPolygon(xs, ys, pts.size());
            // Dashed border
            gc.setStroke(Color.rgb(55, 65, 81));
            gc.setLineWidth(2.0);
            gc.setLineDashes(10, 5);
            gc.strokePolygon(xs, ys, pts.size());
            gc.setLineDashes();
            // Farm name near top-most vertex
            double topY = Double.MAX_VALUE, topX = W / 2.0;
            for (int i = 0; i < xs.length; i++) {
                if (ys[i] < topY) { topY = ys[i]; topX = xs[i]; }
            }
            mapText(gc, FarmService.getInstance().getFarmName(),
                topX, Math.max(topY - 9, 12), Color.rgb(45, 55, 72), 9.5, true);
        }

        // ── Zone polygons ─────────────────────────────────────────────
        for (ZONE z : farm.getAllZones()) {
            if (!z.hasBoundaries()) continue;
            Color fill, stroke;
            if (z instanceof LivestockZONE) {
                fill   = Color.rgb(34,  197, 94,  0.28);
                stroke = Color.rgb(22,  163, 74);
            } else if (z instanceof CropZONE) {
                fill   = Color.rgb(234, 179, 8,   0.30);
                stroke = Color.rgb(161, 110, 0);
            } else {
                fill   = Color.rgb(14,  165, 233, 0.28);
                stroke = Color.rgb(2,   132, 199);
            }
            List<double[]> pts = z.getBoundaries().getPoints();
            double[] xs = toXs(pts, W, fMO, fXO);
            double[] ys = toYs(pts, H, fML, fXL);

            gc.setFill(fill);
            gc.fillPolygon(xs, ys, pts.size());
            gc.setStroke(stroke);
            gc.setLineWidth(1.5);
            gc.strokePolygon(xs, ys, pts.size());

            // Label at centroid — truncate if zone is narrow on screen
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

        // ── Legend (bottom-left) ──────────────────────────────────────
        drawMapLegend(gc, H, farm);

        // ── Compass rose (top-right) ──────────────────────────────────
        drawCompass(gc, W - 22, 22);

        // ── Corner coordinate hints ───────────────────────────────────
        gc.setFill(Color.rgb(100, 116, 139));
        gc.setFont(Font.font("System", 8));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("%.3f°N", fXL), 4, 10);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(String.format("%.3f°E", fXO), W - 4, H - 4);
    }

    // ── Map drawing helpers ───────────────────────────────────────────

    private void mapText(GraphicsContext gc, String txt, double x, double y,
                         Color color, double sz, boolean bold) {
        gc.setFont(bold ? Font.font("System", FontWeight.BOLD, sz) : Font.font("System", sz));
        gc.setTextAlign(TextAlignment.CENTER);
        // White halo for readability over any background
        gc.setFill(Color.rgb(255, 255, 255, 0.82));
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0) gc.fillText(txt, x + dx, y + dy);
        gc.setFill(color);
        gc.fillText(txt, x, y);
    }

    private void drawMapLegend(GraphicsContext gc, double H, Farm farm) {
        String[] labels  = {"Farm boundary", "Livestock", "Crop", "Aquaculture"};
        Color[]  colors  = {
            Color.rgb(55, 65, 81),
            Color.rgb(22, 163, 74),
            Color.rgb(161, 110, 0),
            Color.rgb(2, 132, 199)
        };
        boolean[] show = {
            farm.hasFarmBoundary(),
            farm.getLivestockZones().stream().anyMatch(z -> z.hasBoundaries()),
            farm.getCropZones().stream().anyMatch(z -> z.hasBoundaries()),
            farm.getAquacultureZones().stream().anyMatch(z -> z.hasBoundaries())
        };

        double x = 8, y = H - 8;
        gc.setFont(Font.font("System", 9));
        for (int i = labels.length - 1; i >= 0; i--) {
            if (!show[i]) continue;
            gc.setFill(colors[i]);
            gc.fillOval(x, y - 7, 8, 8);
            gc.setFill(Color.rgb(30, 30, 30));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(labels[i], x + 11, y);
            y -= 13;
        }
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

    // ── Recent alerts table ──────────────────────────────────────────

    private void buildRecentAlertsTable() {
        colAlertTime.setCellValueFactory(d ->
            new SimpleStringProperty(
                d.getValue().getTimestamp().toLocalDate().toString()));

        colAlertMsg.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getMessage()));

        colAlertMsg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) { setText(null); setTooltip(null); return; }
                setText(msg);
                setTooltip(new Tooltip(msg));
            }
        });

        colAlertSeverity.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Alert a = (Alert) getTableRow().getItem();
                badge.setText(a.getSeverity().toString());
                badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                badge.getStyleClass().add(
                    a.getSeverity() == AlertSeverity.Critical ? "badge-critical" : "badge-warning");
                setGraphic(badge);
            }
        });

        recentAlertsTable.setRowFactory(tv -> new TableRow<Alert>() {
            @Override
            protected void updateItem(Alert alert, boolean empty) {
                super.updateItem(alert, empty);
                getStyleClass().removeIf(c -> c.startsWith("alert-row-"));
                if (!empty && alert != null) {
                    getStyleClass().add(
                        alert.getSeverity() == AlertSeverity.Critical
                            ? "alert-row-critical"
                            : "alert-row-warning");
                }
            }
        });

        List<Alert> allAlerts = AlertService.getInstance().getAllAlerts();
        List<Alert> top5 = allAlerts.subList(0, Math.min(5, allAlerts.size()));
        recentAlertsTable.setItems(FXCollections.observableArrayList(top5));
    }

    // ── Quick actions ────────────────────────────────────────────────

    @FXML private void quickAddAnimal()   { SceneManager.getInstance().navigateTo("animals"); }
    @FXML private void quickAddZone()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickAddCrop()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickAddAqua()     { SceneManager.getInstance().navigateTo("zones"); }
    @FXML private void quickViewReports() { SceneManager.getInstance().navigateTo("reports"); }
    @FXML private void quickViewAlerts()  { SceneManager.getInstance().navigateTo("alerts"); }
}
