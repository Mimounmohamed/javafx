package com.example.controllers;

import Alerts.Alert;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Animals.Animal;
import Animals.AnimalHealthStatus;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Entities.CropType;
import Entities.GrowthStage;
import Reports.*;
import Sensors.*;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import com.example.services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.example.services.PdfReportService;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsController {

    @FXML private VBox   contentArea;
    @FXML private Button btnOverview;
    @FXML private Button btnLivestock;
    @FXML private Button btnCrops;
    @FXML private Button btnAquaculture;
    @FXML private Button btnAlerts;
    @FXML private Button btnSensors;
    @FXML private Button btnExport;
    @FXML private Label  farmNameLabel;
    @FXML private Label  farmSubLabel;
    @FXML private Label  kpiZones;
    @FXML private Label  kpiAnimals;
    @FXML private Label  kpiAlerts;
    @FXML private Label  kpiSensors;

    private ZoneService    zoneService;
    private AlertService   alertService;
    private AnimalService  animalService;
    private SensorService  sensorService;
    private FarmService    farmService;
    private ReportService  reportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");

    // ── Sensor History state ──────────────────────────────────────────────
    private LocalDateTime  sensorStart   = LocalDateTime.now().minusDays(14);
    private LocalDateTime  sensorEnd     = LocalDateTime.now();
    private String         sTypeFilter   = "All";       // "All","Bio","GPS","Env","Soil","Water"
    private String         sZoneFilter   = "All Zones"; // zone name or "All Zones"
    private static final DateTimeFormatter RANGE_LABEL_FMT =
        DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter CSV_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Label sensorRangeLabel = new Label();

    @FXML
    public void initialize() {
        try {
            zoneService    = ZoneService.getInstance();
            alertService   = AlertService.getInstance();
            animalService  = AnimalService.getInstance();
            sensorService  = SensorService.getInstance();
            farmService    = FarmService.getInstance();
            reportService  = ReportService.getInstance();

            farmNameLabel.setText(farmService.getFarmName());
            farmSubLabel.setText(farmService.getFarmLocation());

            kpiZones.setText(String.valueOf(zoneService.getAllZones().size()));
            kpiAnimals.setText(String.valueOf(animalService.getAllAnimals().size()));
            kpiAlerts.setText(String.valueOf(alertService.getActiveAlertCount()));
            kpiSensors.setText(String.valueOf(sensorService.getAllSensors().size()));

            showOverview();
        } catch (Throwable t) {
            t.printStackTrace();
            Label err = new Label("Reports failed to load: " + t.getMessage());
            err.setWrapText(true);
            err.getStyleClass().add("text-muted");
            err.setPadding(new Insets(24));
            if (contentArea != null) contentArea.getChildren().setAll(err);
        }
    }

    // ── Nav ───────────────────────────────────────────────────────────

    private void setActive(Button btn) {
        for (Button b : new Button[]{btnOverview, btnLivestock, btnCrops, btnAquaculture, btnAlerts, btnSensors, btnExport}) {
            if (b == null) continue;
            b.getStyleClass().removeAll("nav-active");
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (btn == null) return;
        btn.getStyleClass().remove("nav-btn");
        if (!btn.getStyleClass().contains("nav-active")) btn.getStyleClass().add("nav-active");
    }

    @FXML private void showOverview()    { setActive(btnOverview);    buildOverview(); }
    @FXML private void showLivestock()   { setActive(btnLivestock);   buildLivestock(); }
    @FXML private void showCrops()       { setActive(btnCrops);       buildCrops(); }
    @FXML private void showAquaculture() { setActive(btnAquaculture); buildAquaculture(); }
    @FXML private void showAlerts()      { setActive(btnAlerts);      buildAlerts(); }
    @FXML private void showSensors()     { setActive(btnSensors);     buildSensors(); }
    @FXML private void showExport()      { setActive(btnExport);      buildExport(); }

    // ═══════════════════════════════════════════════════════════════════
    // FARM OVERVIEW
    // ═══════════════════════════════════════════════════════════════════

    private void buildOverview() {
        contentArea.getChildren().clear();

        List<ZONE>   allZones = zoneService.getAllZones();
        List<Animal> animals  = animalService.getAllAnimals();
        List<Alert>  alerts   = alertService.getAllAlerts();
        long activeAlerts     = alerts.stream().filter(Alert::isActive).count();

        contentArea.getChildren().add(sectionTitleRow("Farm Overview", "Export PDF",
            PdfReportService.getInstance()::exportOverview));
        contentArea.getChildren().add(kpiRow(
            kpi("Zones",         String.valueOf(allZones.size()),                      "stat-accent-blue"),
            kpi("Animals",       String.valueOf(animals.size()),                       "stat-accent-green"),
            kpi("Sensors",       String.valueOf(sensorService.getAllSensors().size()), "stat-accent-yellow"),
            kpi("Active Alerts", String.valueOf(activeAlerts),                        "stat-accent-red")
        ));

        contentArea.getChildren().add(chartRow(
            wrappedChart("Zone Distribution", zoneTypePie(allZones),    true),
            wrappedChart("Animal Health",      animalHealthPie(animals), true)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Production by Zone",  productionByZoneBar(),     false),
            wrappedChart("Alert Severity",       alertSeverityPie(alerts), true)
        ));

        // ── Farm Report data table ──
        try {
            FarmReport r = reportService.generateFarmReport(ReportType.Monthly);
            contentArea.getChildren().add(sectionTitle("Farm Report — Monthly"));
            contentArea.getChildren().add(chartRow(
                tableCard("Livestock Summary", List.of(
                    row("Total animals",        r.getTotalAnimals()),
                    row("Healthy",              r.getHealthyAnimals()),
                    row("Sick",                 r.getSickAnimals()),
                    row("Quarantined",          r.getQuarantinedAnimals()),
                    row("Overdue feeding zones",r.getZonesWithOverdueFeeding()),
                    row("GPS escapes",          r.getGpsEscapes())
                )),
                tableCard("Crop Summary", List.of(
                    row("Total fields",         r.getTotalFields()),
                    row("Harvest-ready",        r.getHarvestReadyFields()),
                    row("Pending",              r.getPendingFields())
                ))
            ));
            contentArea.getChildren().add(chartRow(
                tableCard("Aquaculture Summary", List.of(
                    row("Species groups",       r.getTotalSpeciesGroups()),
                    row("Current individuals",  r.getTotalCurrentIndividuals())
                )),
                tableCard("Alert Summary", List.of(
                    row("Total alerts",         r.getTotalAlerts()),
                    row("Critical",             r.getCriticalAlerts()),
                    row("Warning",              r.getWarningAlerts()),
                    row("Resolved",             r.getResolvedAlerts()),
                    row("Unresolved",           r.getUnresolvedAlerts())
                ))
            ));
            if (!r.getNotes().isEmpty()) {
                VBox notesCard = new VBox(6);
                notesCard.getStyleClass().add("kpi-card");
                notesCard.setPadding(new Insets(16));
                notesCard.getChildren().add(cardTitle("Notes"));
                notesCard.getChildren().add(new Separator());
                for (String n : r.getNotes()) {
                    Label nl = new Label("• " + n);
                    nl.getStyleClass().add("detail-value");
                    nl.setWrapText(true);
                    notesCard.getChildren().add(nl);
                }
                contentArea.getChildren().add(notesCard);
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    // LIVESTOCK
    // ═══════════════════════════════════════════════════════════════════

    private void buildLivestock() {
        contentArea.getChildren().clear();
        List<LivestockZONE> zones = zoneService.getLivestockZones();
        double totalMilk    = zones.stream().mapToDouble(LivestockZONE::getTotalMilkYield).sum();
        long   totalEggs    = zones.stream().mapToLong(LivestockZONE::getTotalEggCount).sum();
        long   totalAnimals = zones.stream().mapToLong(z -> z.getAnimals().size()).sum();

        contentArea.getChildren().add(sectionTitleRow("Livestock Production", "Export PDF",
            PdfReportService.getInstance()::exportLivestock));
        contentArea.getChildren().add(kpiRow(
            kpi("Livestock Zones", String.valueOf(zones.size()),             "stat-accent-blue"),
            kpi("Total Animals",   String.valueOf(totalAnimals),             "stat-accent-green"),
            kpi("Total Milk (L)",  String.format("%.1f", totalMilk),        "stat-accent-yellow"),
            kpi("Total Eggs",      String.valueOf(totalEggs),                "stat-accent-red")
        ));

        if (zones.isEmpty()) { contentArea.getChildren().add(emptyState("No livestock zones configured.")); return; }

        contentArea.getChildren().add(chartRow(
            wrappedChart("Milk Yield per Zone (L)", milkPerZoneBar(zones),      false),
            wrappedChart("Egg Count per Zone",       eggPerZoneBar(zones),       false)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Top Milk Producers",       topMilkProducersBar(zones), false),
            wrappedChart("Health Distribution",      livestockHealthPie(zones),  true)
        ));

        // ── Trend charts ──
        contentArea.getChildren().add(sectionTitle("Production Trends (15 days)"));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Daily Milk Yield Trend (L)",    milkTrendLine(zones), false),
            wrappedChart("Daily Egg Count Trend",          eggTrendLine(zones),  false)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Average Weight Trend (kg)",     weightTrendLine(zones), false),
            new VBox()   // right slot empty — weight stands alone
        ));

        // ── Per-zone production report tables ──
        contentArea.getChildren().add(sectionTitle("Zone Production Reports"));
        for (LivestockZONE z : zones) {
            try {
                LivestockProductionReport rpt = (LivestockProductionReport)
                        reportService.generateZoneProductionReport(z, ReportType.Monthly);
                long healthy   = z.getAnimals().stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Healthy).count();
                long sick      = z.getAnimals().stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Sick).count();
                long quarant   = z.getAnimals().stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Quarantined).count();

                contentArea.getChildren().add(chartRow(
                    tableCard(z.getName() + " — Milk", List.of(
                        row("Total milk yield (L)",    fmt2(rpt.getTotalMilkYieldLiters())),
                        row("Producing animals",       rpt.getMilkProducingAnimals()),
                        row("Avg per animal (L)",      fmt2(rpt.getAvgMilkPerAnimal()))
                    )),
                    tableCard(z.getName() + " — Eggs & Health", List.of(
                        row("Total egg count",         rpt.getTotalEggCount()),
                        row("Laying animals",          rpt.getEggLayingAnimals()),
                        row("Avg per animal (eggs)",   fmt2(rpt.getAvgEggsPerAnimal())),
                        row("Healthy",                 (int) healthy),
                        row("Sick",                    (int) sick),
                        row("Quarantined",             (int) quarant)
                    ))
                ));
            } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CROPS
    // ═══════════════════════════════════════════════════════════════════

    private void buildCrops() {
        contentArea.getChildren().clear();
        List<CropZONE> zones = zoneService.getCropZones();
        double totalYield = zones.stream().mapToDouble(CropZONE::getTotalCropYield).sum();
        long   harvested  = zones.stream().flatMap(z -> z.getFields().stream()).filter(Crop::wasHarvested).count();
        long   total      = zones.stream().mapToLong(z -> z.getFields().size()).sum();

        contentArea.getChildren().add(sectionTitleRow("Crop Production", "Export PDF",
            PdfReportService.getInstance()::exportCrops));
        contentArea.getChildren().add(kpiRow(
            kpi("Crop Zones",       String.valueOf(zones.size()),       "stat-accent-blue"),
            kpi("Total Yield (kg)", String.format("%.1f", totalYield), "stat-accent-green"),
            kpi("Harvested Fields", String.valueOf(harvested),          "stat-accent-yellow"),
            kpi("Total Fields",     String.valueOf(total),              "stat-accent-red")
        ));

        if (zones.isEmpty()) { contentArea.getChildren().add(emptyState("No crop zones configured.")); return; }

        contentArea.getChildren().add(chartRow(
            wrappedChart("Yield per Zone (kg)",       yieldPerZoneBar(zones),       false),
            wrappedChart("Growth Stage Distribution", growthStagePie(zones),         true)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Yield by Crop Type (kg)",   yieldByCropTypeBar(zones),    false),
            wrappedChart("Harvested vs Pending",       harvestedVsPendingPie(zones), true)
        ));

        // ── Yield history trend ──
        contentArea.getChildren().add(sectionTitle("Harvest History Trends"));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Crop Yield History — Cumulative (kg)", cropYieldTrendLine(zones), false),
            wrappedChart("Harvest Batches by Zone (kg)",          cropBatchBar(zones),       false)
        ));

        // ── Per-zone crop report tables ──
        contentArea.getChildren().add(sectionTitle("Zone Production Reports"));
        for (CropZONE z : zones) {
            try {
                CropProductionReport rpt = (CropProductionReport)
                        reportService.generateZoneProductionReport(z, ReportType.Monthly);

                List<String[]> yieldRows = new ArrayList<>(List.of(
                    row("Total yield (kg)",     fmt2(rpt.getTotalYieldKg())),
                    row("Harvested fields",      rpt.getHarvestedFieldsCount()),
                    row("Pending fields",        rpt.getPendingFieldsCount()),
                    row("Yield per hectare",     fmt2(rpt.getYieldPerHectare()))
                ));
                for (Map.Entry<CropType, Double> e : rpt.getYieldByCropType().entrySet())
                    yieldRows.add(row("  " + e.getKey().name(), fmt2(e.getValue()) + " kg"));

                Map<GrowthStage, Long> stages = z.getFields().stream()
                    .collect(Collectors.groupingBy(Crop::getGrowthStage, Collectors.counting()));
                List<String[]> stageRows = stages.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> row(capitalize(e.getKey().name()), (int)(long)e.getValue()))
                    .collect(Collectors.toList());
                if (stageRows.isEmpty()) stageRows.add(row("No crops", 0));

                contentArea.getChildren().add(chartRow(
                    tableCard(z.getName() + " — Yield", yieldRows),
                    tableCard(z.getName() + " — Growth Stages", stageRows)
                ));
            } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // AQUACULTURE
    // ═══════════════════════════════════════════════════════════════════

    private void buildAquaculture() {
        contentArea.getChildren().clear();
        List<AquacultureZONE> zones = zoneService.getAquacultureZones();
        double totalHarvest = zones.stream().mapToDouble(AquacultureZONE::getTotalHarvestWeight).sum();
        double avgSurvival  = zones.stream().flatMap(z -> z.getSpeciesList().stream())
                                   .mapToDouble(AquacultureSpecies::getCycleSurvivalRatePercent)
                                   .average().orElse(0.0);
        long   totalStock   = zones.stream().mapToLong(AquacultureZONE::getTotalSpeciesCount).sum();

        contentArea.getChildren().add(sectionTitleRow("Aquaculture Production", "Export PDF",
            PdfReportService.getInstance()::exportAquaculture));
        contentArea.getChildren().add(kpiRow(
            kpi("Aquaculture Zones",  String.valueOf(zones.size()),           "stat-accent-blue"),
            kpi("Total Stock",        String.valueOf(totalStock),             "stat-accent-green"),
            kpi("Total Harvest (kg)", String.format("%.1f", totalHarvest),   "stat-accent-yellow"),
            kpi("Avg Cycle Survival", String.format("%.1f%%", avgSurvival),  "stat-accent-red")
        ));

        if (zones.isEmpty()) { contentArea.getChildren().add(emptyState("No aquaculture zones configured.")); return; }

        contentArea.getChildren().add(chartRow(
            wrappedChart("Harvest Weight per Zone (kg)", harvestPerZoneBar(zones), false),
            wrappedChart("Cycle Survival Rate (%)",      survivalRateBar(zones),   false)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Stock vs Harvested Count",     stockVsHarvestedBar(zones), false),
            wrappedChart("Species Distribution",          speciesDistPie(zones),      true)
        ));

        // ── Harvest timeline trend ──
        contentArea.getChildren().add(sectionTitle("Harvest Timeline Trends"));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Harvest Weight Over Time (kg)", aquaHarvestTrendLine(zones), false),
            wrappedChart("Cumulative Harvest per Species (kg)", aquaCumulativeBar(zones), false)
        ));

        // ── Per-zone aquaculture report tables ──
        contentArea.getChildren().add(sectionTitle("Zone Production Reports"));
        for (AquacultureZONE z : zones) {
            try {
                AquacultureProductionReport rpt = (AquacultureProductionReport)
                        reportService.generateZoneProductionReport(z, ReportType.Monthly);

                List<String[]> stockRows = new ArrayList<>(List.of(
                    row("Total harvest (kg)",       fmt2(rpt.getTotalHarvestWeightKg())),
                    row("Initial individuals",      rpt.getTotalInitialIndividuals()),
                    row("Current individuals",      rpt.getTotalCurrentIndividuals()),
                    row("Cycle mortality",          rpt.getTotalCycleMortality()),
                    row("Overall mortality",        rpt.getTotalOverallMortality())
                ));
                List<String[]> survivalRows = new ArrayList<>(List.of(
                    row("Avg cycle survival (%)",   fmt2(rpt.getAvgCycleSurvivalRate())),
                    row("Avg overall survival (%)", fmt2(rpt.getAvgOverallSurvivalRate())),
                    row("Water quality score",      fmt2(rpt.getWaterQualityScore()) + " / 100")
                ));
                for (AquacultureSpecies sp : z.getSpeciesList())
                    survivalRows.add(row("  " + sp.getName() + " stock", sp.getNumSpecies()));

                contentArea.getChildren().add(chartRow(
                    tableCard(z.getName() + " — Stock", stockRows),
                    tableCard(z.getName() + " — Survival & Quality", survivalRows)
                ));
            } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ALERTS
    // ═══════════════════════════════════════════════════════════════════

    private void buildAlerts() {
        contentArea.getChildren().clear();
        List<Alert> alerts = alertService.getAllAlerts();
        long active   = alerts.stream().filter(Alert::isActive).count();
        long acked    = alerts.stream().filter(Alert::isAcknowledged).count();
        long resolved = alerts.stream().filter(Alert::isResolved).count();
        long critical = alertService.getAlertsBySeverity(AlertSeverity.Critical).size();

        contentArea.getChildren().add(sectionTitleRow("Alerts & Incidents", "Export PDF",
            PdfReportService.getInstance()::exportAlerts));
        contentArea.getChildren().add(kpiRow(
            kpi("Total Alerts", String.valueOf(alerts.size()), "stat-accent-blue"),
            kpi("Active",       String.valueOf(active),        "stat-accent-red"),
            kpi("Acknowledged", String.valueOf(acked),         "stat-accent-yellow"),
            kpi("Critical",     String.valueOf(critical),      "stat-accent-purple")
        ));

        if (alerts.isEmpty()) { contentArea.getChildren().add(emptyState("No alerts recorded.")); return; }

        contentArea.getChildren().add(chartRow(
            wrappedChart("Alerts by Type",     alertTypePie(alerts),     true),
            wrappedChart("Alerts by Severity", alertSeverityPie(alerts), true)
        ));
        contentArea.getChildren().add(chartRow(
            wrappedChart("Alerts per Zone",    alertsPerZoneBar(alerts), false),
            wrappedChart("Alert Status",       alertStatusPie(alerts),   true)
        ));

        // ── Alert summary table ──
        Map<AlertType, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
        List<String[]> typeRows = byType.entrySet().stream()
                .sorted(Map.Entry.<AlertType, Long>comparingByValue().reversed())
                .map(e -> row(e.getKey().name(), (int)(long)e.getValue()))
                .collect(Collectors.toList());

        contentArea.getChildren().add(chartRow(
            tableCard("Alerts by Type", typeRows),
            tableCard("Alert Status Breakdown", List.of(
                row("Active",      (int) active),
                row("Acknowledged",(int) acked),
                row("Resolved",    (int) resolved),
                row("Critical",    (int) critical),
                row("Warning",     (int) alertService.getAlertsBySeverity(AlertSeverity.Warning).size())
            ))
        ));

        List<Alert> activeList = alerts.stream()
                .filter(Alert::isActive)
                .sorted(Comparator.comparing(Alert::getSeverity))
                .limit(12).toList();
        if (!activeList.isEmpty()) {
            contentArea.getChildren().add(sectionTitle("Active Alerts"));
            VBox alertList = new VBox(4);
            alertList.getStyleClass().add("kpi-card");
            alertList.setPadding(new Insets(12));
            for (Alert a : activeList) alertList.getChildren().add(alertRow(a));
            contentArea.getChildren().add(alertList);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SENSOR HISTORY
    // ═══════════════════════════════════════════════════════════════════

    private void buildSensors() {
        contentArea.getChildren().clear();
        var bio   = sensorService.getAllBioSensors();
        var gps   = sensorService.getAllGPSSensors();
        var env   = sensorService.getAllEnvSensors();
        var soil  = sensorService.getAllSoilSensors();
        var water = sensorService.getAllWaterSensors();

        contentArea.getChildren().add(sectionTitle("Sensor History"));
        contentArea.getChildren().add(buildSensorToolbar());
        contentArea.getChildren().add(buildSensorPills(bio.size(), gps.size(),
            env.size(), soil.size(), water.size()));
        rebuildSensorContent();
    }

    // ── Feature 1: Date range / zone / type toolbar ───────────────────────

    private HBox buildSensorToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("kpi-card");
        toolbar.setPadding(new Insets(10, 12, 10, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Zone ComboBox
        ComboBox<String> zoneCombo = new ComboBox<>();
        zoneCombo.getItems().add("All Zones");
        zoneService.getLivestockZones().forEach(z -> zoneCombo.getItems().add(z.getName()));
        zoneService.getCropZones().forEach(z -> zoneCombo.getItems().add(z.getName()));
        zoneService.getAquacultureZones().forEach(z -> zoneCombo.getItems().add(z.getName()));
        zoneCombo.setValue(sZoneFilter);
        zoneCombo.setOnAction(e -> {
            sZoneFilter = zoneCombo.getValue() != null ? zoneCombo.getValue() : "All Zones";
            rebuildSensorContent();
        });

        // Type filter ToggleButtons
        ToggleGroup typeGroup = new ToggleGroup();
        HBox typePills = new HBox(4);
        for (String type : new String[]{"All", "Bio", "GPS", "Env", "Soil", "Water"}) {
            ToggleButton tb = new ToggleButton(type);
            tb.getStyleClass().add("animals-pill-type");
            tb.setToggleGroup(typeGroup);
            if (type.equals(sTypeFilter)) tb.setSelected(true);
            tb.setOnAction(e -> {
                sTypeFilter = type;
                rebuildSensorContent();
            });
            typePills.getChildren().add(tb);
        }

        // Spacer to push date controls right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Date range ToggleButtons
        ToggleGroup dateGroup = new ToggleGroup();
        HBox datePills = new HBox(4);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(sensorStart.toLocalDate(), LocalDateTime.now().toLocalDate());
        for (String[] pair : new String[][]{{"7d", "7"}, {"14d", "14"}, {"30d", "30"}}) {
            ToggleButton tb = new ToggleButton(pair[0]);
            tb.getStyleClass().add("animals-pill-type");
            tb.setToggleGroup(dateGroup);
            int days = Integer.parseInt(pair[1]);
            if (Math.abs(daysBetween - days) <= 1) tb.setSelected(true);
            tb.setOnAction(e -> {
                sensorStart = LocalDateTime.now().minusDays(days);
                sensorEnd   = LocalDateTime.now();
                updateRangeLabel();
                rebuildSensorContent();
            });
            datePills.getChildren().add(tb);
        }
        ToggleButton customBtn = new ToggleButton("Custom...");
        customBtn.getStyleClass().add("animals-pill-type");
        customBtn.setToggleGroup(dateGroup);
        customBtn.setOnAction(e -> {
            showCustomDateDialog();
            updateRangeLabel();
        });
        datePills.getChildren().add(customBtn);
        if (dateGroup.getSelectedToggle() == null) customBtn.setSelected(true);

        // Range label
        updateRangeLabel();
        sensorRangeLabel.getStyleClass().add("text-muted");
        sensorRangeLabel.setStyle("-fx-font-size: 11px;");

        // Export all PDF button
        Button exportAllBtn = new Button("📄 Export All PDF");
        exportAllBtn.getStyleClass().add("btn-secondary");
        exportAllBtn.setOnAction(e -> exportAllSensorsPdf());

        toolbar.getChildren().addAll(zoneCombo, typePills, spacer, datePills, sensorRangeLabel, exportAllBtn);
        return toolbar;
    }

    private void updateRangeLabel() {
        sensorRangeLabel.setText(sensorStart.format(RANGE_LABEL_FMT)
            + " – " + sensorEnd.format(RANGE_LABEL_FMT));
    }

    // ── Feature 3: Sensor type filter pills (clickable KPI cards) ────────

    private HBox buildSensorPills(int bio, int gps, int env, int soil, int water) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        int total = bio + gps + env + soil + water;
        String[][] pills = {
            {"All",   String.valueOf(total), "stat-accent-blue"},
            {"Bio",   String.valueOf(bio),   "stat-accent-green"},
            {"GPS",   String.valueOf(gps),   "stat-accent-purple"},
            {"Env",   String.valueOf(env),   "stat-accent-yellow"},
            {"Soil",  String.valueOf(soil),  "stat-accent-red"},
            {"Water", String.valueOf(water), "stat-accent-blue"}
        };
        for (String[] p : pills) {
            String typeName = p[0];
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().addAll("stat-card", p[2]);
            card.setPadding(new Insets(12, 12, 12, 12));
            card.setStyle("-fx-cursor: hand;" + (typeName.equals(sTypeFilter)
                ? "-fx-border-color: #16A34A; -fx-border-width: 0 0 3 0;" : ""));
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            Label val = new Label(p[1]); val.getStyleClass().add("stat-value");
            Label lbl = new Label(typeName + " Sensors"); lbl.getStyleClass().add("stat-label");
            card.getChildren().addAll(val, lbl);
            card.setOnMouseClicked(e -> {
                sTypeFilter = typeName;
                // refresh pills highlighting: rebuild entire sensor view
                buildSensors();
            });
            row.getChildren().add(card);
        }
        return row;
    }

    // ── rebuildSensorContent ─────────────────────────────────────────────

    private void rebuildSensorContent() {
        while (contentArea.getChildren().size() > 3)
            contentArea.getChildren().remove(3);
        if (sensorService.getAllSensors().isEmpty()) {
            contentArea.getChildren().add(emptyState("No sensors found."));
            return;
        }
        // Livestock zones (bio + GPS)
        for (LivestockZONE z : zoneService.getLivestockZones()) {
            if (!"All Zones".equals(sZoneFilter) && !z.getName().equals(sZoneFilter)) continue;
            if (!shouldShowZoneForType(z)) continue;
            if (z.getBioSensors().isEmpty() && z.getGpsCollarSensors().isEmpty()) continue;
            contentArea.getChildren().add(zoneSeparator(z.getName() + " — Livestock Zone"));
            Set<String> syncDates = collectZoneDates(z);
            if (matchesTypeFilter("Bio"))
                buildSensorPairsNew(z.getBioSensors().stream().map(s -> (NumericSensor)s).toList(),
                    s -> "Bio: " + ((Sensors.BioSensor)s).getMeasureType() + "  [" + s.getCode() + "] — " + ((Sensors.BioSensor)s).getAnimal().getName(),
                    z, syncDates);
            if (matchesTypeFilter("GPS"))
                buildGpsPairsNew(z.getGpsCollarSensors(), z);
        }
        // Crop zones (env + soil)
        for (CropZONE z : zoneService.getCropZones()) {
            if (!"All Zones".equals(sZoneFilter) && !z.getName().equals(sZoneFilter)) continue;
            if (!z.getEnvSensors().isEmpty() || !z.getSoilSensors().isEmpty()) {
                if (!shouldShowZoneForType(z)) continue;
                contentArea.getChildren().add(zoneSeparator(z.getName() + " — Crop Zone"));
                Set<String> syncDates = collectZoneDates(z);
                if (matchesTypeFilter("Env"))
                    buildSensorPairsNew(z.getEnvSensors().stream().map(s -> (NumericSensor)s).toList(),
                        s -> "Env: " + ((Sensors.EnvSensor)s).getMeasureType() + "  [" + s.getCode() + "]",
                        z, syncDates);
                if (matchesTypeFilter("Soil"))
                    buildSensorPairsNew(z.getSoilSensors().stream().map(s -> (NumericSensor)s).toList(),
                        s -> "Soil: " + ((Sensors.SoilSensor)s).getMeasureType() + "  [" + s.getCode() + "]",
                        z, syncDates);
            }
        }
        // Aquaculture zones (water)
        for (AquacultureZONE z : zoneService.getAquacultureZones()) {
            if (!"All Zones".equals(sZoneFilter) && !z.getName().equals(sZoneFilter)) continue;
            if (!z.getWaterSensors().isEmpty()) {
                if (!shouldShowZoneForType(z)) continue;
                contentArea.getChildren().add(zoneSeparator(z.getName() + " — Aquaculture Zone"));
                Set<String> syncDates = collectZoneDates(z);
                if (matchesTypeFilter("Water"))
                    buildSensorPairsNew(z.getWaterSensors().stream().map(s -> (NumericSensor)s).toList(),
                        s -> "Water: " + ((Sensors.WaterSensor)s).getMeasureType() + "  [" + s.getCode() + "]",
                        z, syncDates);
            }
        }
    }

    private boolean matchesTypeFilter(String type) {
        return "All".equals(sTypeFilter) || sTypeFilter.equals(type);
    }

    private boolean shouldShowZoneForType(ZONE z) {
        if ("All".equals(sTypeFilter)) return true;
        if (z instanceof LivestockZONE)    return "Bio".equals(sTypeFilter) || "GPS".equals(sTypeFilter);
        if (z instanceof CropZONE)         return "Env".equals(sTypeFilter) || "Soil".equals(sTypeFilter);
        if (z instanceof AquacultureZONE)  return "Water".equals(sTypeFilter);
        return true;
    }

    // ── Feature 6: Synchronized X-axis helpers ───────────────────────────

    private Set<String> collectZoneDates(ZONE zone) {
        Set<String> dates = new java.util.TreeSet<>();
        if (zone instanceof LivestockZONE lz) {
            for (Sensors.BioSensor s : lz.getBioSensors())
                filteredNumericReadings(s).forEach(r -> dates.add(r.getTimestamp().format(DATE_FMT)));
        } else if (zone instanceof CropZONE cz) {
            for (Sensors.EnvSensor  s : cz.getEnvSensors())  filteredNumericReadings(s).forEach(r -> dates.add(r.getTimestamp().format(DATE_FMT)));
            for (Sensors.SoilSensor s : cz.getSoilSensors()) filteredNumericReadings(s).forEach(r -> dates.add(r.getTimestamp().format(DATE_FMT)));
        } else if (zone instanceof AquacultureZONE az) {
            for (Sensors.WaterSensor s : az.getWaterSensors()) filteredNumericReadings(s).forEach(r -> dates.add(r.getTimestamp().format(DATE_FMT)));
        }
        return dates;
    }

    private List<NumericSensorReading> filteredNumericReadings(NumericSensor s) {
        return s.getReadingHistory().stream()
            .filter(r -> r instanceof NumericSensorReading)
            .map(r -> (NumericSensorReading) r)
            .filter(r -> !r.getTimestamp().isBefore(sensorStart) && !r.getTimestamp().isAfter(sensorEnd))
            .collect(Collectors.toList());
    }

    private List<GPSSensorReading> filteredGpsReadings(GPSCollarSensor s) {
        return s.getReadingHistory().stream()
            .filter(r -> r instanceof GPSSensorReading)
            .map(r -> (GPSSensorReading) r)
            .filter(r -> !r.getTimestamp().isBefore(sensorStart) && !r.getTimestamp().isAfter(sensorEnd))
            .collect(Collectors.toList());
    }

    // ── Pair builders (new) ───────────────────────────────────────────────

    private void buildSensorPairsNew(List<NumericSensor> sensors,
            java.util.function.Function<NumericSensor, String> labelFn,
            ZONE zone, Set<String> syncDates) {
        for (int i = 0; i < sensors.size(); i += 2) {
            NumericSensor s1 = sensors.get(i);
            Node card1 = sensorChartCard(s1, labelFn.apply(s1), zone, syncDates);
            if (i + 1 < sensors.size()) {
                NumericSensor s2 = sensors.get(i + 1);
                contentArea.getChildren().add(chartRow(card1, sensorChartCard(s2, labelFn.apply(s2), zone, syncDates)));
            } else {
                HBox row = new HBox(16);
                HBox.setHgrow(card1, Priority.ALWAYS);
                if (card1 instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(card1);
                contentArea.getChildren().add(row);
            }
        }
    }

    private void buildGpsPairsNew(List<GPSCollarSensor> sensors, ZONE zone) {
        for (int i = 0; i < sensors.size(); i += 2) {
            GPSCollarSensor g1 = sensors.get(i);
            Node card1 = gpsChartCard(g1, zone);
            if (i + 1 < sensors.size()) {
                contentArea.getChildren().add(chartRow(card1, gpsChartCard(sensors.get(i+1), zone)));
            } else {
                HBox row = new HBox(16);
                HBox.setHgrow(card1, Priority.ALWAYS);
                if (card1 instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(card1);
                contentArea.getChildren().add(row);
            }
        }
    }

    // ── Feature 7+2+5+8: sensorChartCard ─────────────────────────────────

    private VBox sensorChartCard(NumericSensor sensor, String title, ZONE zone, Set<String> syncDates) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card");
        card.setPadding(new Insets(14));

        // Header: title left, PDF button right
        Label titleLbl = new Label(title); titleLbl.getStyleClass().add("card-title"); titleLbl.setWrapText(true);
        Button pdfBtn = new Button("↓ PDF");
        pdfBtn.getStyleClass().add("btn-secondary");
        pdfBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
        pdfBtn.setOnAction(e -> exportSensorPdf(sensor, card));
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, titleLbl, hSpacer, pdfBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Chart (Feature 1: date-filtered, Feature 6: synced X-axis)
        LineChart<String, Number> chart = rangedNumericChart(sensor, syncDates);

        // Feature 5: Alert markers
        List<Alert> zoneAlerts = alertService.getAlertsByZone(zone).stream()
            .filter(a -> !a.getTimestamp().isBefore(sensorStart)
                      && !a.getTimestamp().isAfter(sensorEnd))
            .collect(Collectors.toList());
        addAlertMarkers(chart, sensor, zoneAlerts);

        // Feature 8: Inline legend
        chart.setLegendVisible(false);
        HBox legend = buildInlineLegend(
            new String[]{ sensor.getUnit(), "Min (" + sensor.getMinThreshold() + ")",
                          "Max (" + sensor.getMaxThreshold() + ")" },
            new String[]{"#2196F3", "#f59e0b", "#ef4444"}
        );

        // Summary stat row
        List<NumericSensorReading> readings = filteredNumericReadings(sensor);
        String statLine = readings.isEmpty() ? "No readings in range"
            : String.format("Last: %.2f %s  ·  %d readings", sensor.getLastValue(), sensor.getUnit(), readings.size());
        Label statLbl = new Label(statLine); statLbl.getStyleClass().add("text-muted");
        statLbl.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(header, new Separator(), chart, legend, statLbl);
        return card;
    }

    private VBox gpsChartCard(GPSCollarSensor sensor, ZONE zone) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card");
        card.setPadding(new Insets(14));

        String title = "GPS: " + sensor.getAnimal().getName() + "  [" + sensor.getCode() + "]";
        Label titleLbl = new Label(title); titleLbl.getStyleClass().add("card-title");
        Button pdfBtn = new Button("↓ PDF");
        pdfBtn.getStyleClass().add("btn-secondary");
        pdfBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
        pdfBtn.setOnAction(e -> exportGpsPdf(sensor, card));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(8, titleLbl, sp, pdfBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        LineChart<String, Number> chart = rangedGpsChart(sensor);
        chart.setLegendVisible(false);

        List<GPSSensorReading> readings = filteredGpsReadings(sensor);
        String statusLine = sensor.hasEscaped() ? "Status: OUTSIDE ZONE" : "Status: Inside zone";
        Label stat = new Label(statusLine + "  ·  " + readings.size() + " readings in range");
        stat.getStyleClass().add("text-muted");
        stat.setStyle("-fx-font-size: 11px;");

        HBox legend = buildInlineLegend(
            new String[]{"Zone status (1=in, 0=out)", "Latitude (normalised)"},
            new String[]{"#2196F3", "#9C27B0"});

        card.getChildren().addAll(header, new Separator(), chart, legend, stat);
        return card;
    }

    // ── Feature 1+6: rangedNumericChart / rangedGpsChart ─────────────────

    private LineChart<String, Number> rangedNumericChart(NumericSensor sensor, Set<String> syncDates) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis   yAxis = new NumberAxis();   yAxis.setLabel(sensor.getUnit());
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(260);

        List<NumericSensorReading> readings = filteredNumericReadings(sensor);

        if (!syncDates.isEmpty()) {
            xAxis.setAutoRanging(false);
            xAxis.setCategories(javafx.collections.FXCollections.observableArrayList(syncDates));
        }

        if (readings.isEmpty()) {
            XYChart.Series<String,Number> empty = new XYChart.Series<>();
            empty.setName("No data in range"); chart.getData().add(empty); return chart;
        }

        XYChart.Series<String,Number> dataSeries = new XYChart.Series<>(); dataSeries.setName(sensor.getUnit());
        XYChart.Series<String,Number> minSeries  = new XYChart.Series<>(); minSeries.setName("Min (" + sensor.getMinThreshold() + ")");
        XYChart.Series<String,Number> maxSeries  = new XYChart.Series<>(); maxSeries.setName("Max (" + sensor.getMaxThreshold() + ")");

        for (NumericSensorReading r : readings) {
            String x = r.getTimestamp().format(DATE_FMT);
            dataSeries.getData().add(new XYChart.Data<>(x, r.getValue()));
            minSeries.getData().add(new XYChart.Data<>(x, sensor.getMinThreshold()));
            maxSeries.getData().add(new XYChart.Data<>(x, sensor.getMaxThreshold()));
        }
        chart.getData().addAll(dataSeries, minSeries, maxSeries);

        // Feature 2: Tooltips on all data point symbols
        Platform.runLater(() -> {
            applyLineStyle(minSeries, "#f59e0b"); applyLineStyle(maxSeries, "#ef4444");
            hideSymbolsChart(minSeries); hideSymbolsChart(maxSeries);
            String sCode = sensor.getCode(); String sUnit = sensor.getUnit();
            for (int i = 0; i < readings.size() && i < dataSeries.getData().size(); i++) {
                NumericSensorReading reading = readings.get(i);
                Node node = dataSeries.getData().get(i).getNode();
                if (node == null) continue;
                String color = switch (reading.getSeverity()) {
                    case CRITICAL -> "#ef4444"; case WARNING -> "#f59e0b"; default -> "#22c55e";
                };
                node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 4px;");
                String ttText = "Sensor: " + sCode + "\nTime: " + reading.getTimestamp().format(DATE_FMT)
                    + "\nValue: " + String.format("%.4f", reading.getValue()) + " " + sUnit
                    + "\nStatus: " + reading.getSeverity().name();
                Tooltip tip = new Tooltip(ttText);
                tip.setStyle("-fx-font-size: 11px; -fx-show-delay: 100ms;");
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setScaleX(1.8));
                node.setOnMouseExited(e -> node.setScaleX(1.0));
            }
        });
        return chart;
    }

    private LineChart<String, Number> rangedGpsChart(GPSCollarSensor sensor) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis   yAxis = new NumberAxis(0, 1.2, 0.5); yAxis.setLabel("In(1)/Out(0)");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart"); chart.setAnimated(false); chart.setCreateSymbols(true); chart.setPrefHeight(260);

        List<GPSSensorReading> readings = filteredGpsReadings(sensor);
        if (readings.isEmpty()) {
            XYChart.Series<String,Number> e = new XYChart.Series<>(); e.setName("No GPS data"); chart.getData().add(e); return chart;
        }
        double minLat = readings.stream().mapToDouble(GPSSensorReading::getLat).min().orElse(0);
        double maxLat = readings.stream().mapToDouble(GPSSensorReading::getLat).max().orElse(1);
        double range  = maxLat - minLat;

        XYChart.Series<String,Number> inSeries  = new XYChart.Series<>(); inSeries.setName("Zone status");
        XYChart.Series<String,Number> latSeries = new XYChart.Series<>(); latSeries.setName("Latitude");
        for (GPSSensorReading r : readings) {
            String x = r.getTimestamp().format(DATE_FMT);
            inSeries.getData().add(new XYChart.Data<>(x, r.isInsideZone() ? 1.0 : 0.0));
            latSeries.getData().add(new XYChart.Data<>(x, range > 0 ? (r.getLat() - minLat) / range : 0.5));
        }
        chart.getData().addAll(inSeries, latSeries);
        Platform.runLater(() -> {
            for (int i = 0; i < readings.size() && i < inSeries.getData().size(); i++) {
                GPSSensorReading r = readings.get(i);
                Node node = inSeries.getData().get(i).getNode();
                if (node == null) continue;
                node.setStyle("-fx-background-color: " + (r.isInsideZone() ? "#22c55e" : "#ef4444") + "; -fx-background-radius: 5px; -fx-padding: 4px;");
                Tooltip tip = new Tooltip("Time: " + r.getTimestamp().format(DATE_FMT) + "\nLat: " + String.format("%.5f", r.getLat()) + "\nLon: " + String.format("%.5f", r.getLon()) + "\nZone: " + (r.isInsideZone() ? "INSIDE" : "OUTSIDE"));
                Tooltip.install(node, tip);
            }
        });
        return chart;
    }

    // ── Feature 5: Alert markers ──────────────────────────────────────────

    private void addAlertMarkers(LineChart<String,Number> chart, NumericSensor sensor, List<Alert> alerts) {
        if (alerts.isEmpty()) return;
        XYChart.Series<String,Number> alertSeries = new XYChart.Series<>();
        alertSeries.setName("Alerts");
        double markerY = sensor.getMaxThreshold() * 1.08;
        for (Alert a : alerts) {
            String dateStr = a.getTimestamp().format(DATE_FMT);
            alertSeries.getData().add(new XYChart.Data<>(dateStr, markerY));
        }
        chart.getData().add(alertSeries);
        Platform.runLater(() -> {
            for (int i = 0; i < alerts.size() && i < alertSeries.getData().size(); i++) {
                Alert a = alerts.get(i);
                javafx.scene.Node node = alertSeries.getData().get(i).getNode();
                if (node == null) continue;
                node.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 0; -fx-padding: 8 3 0 3; -fx-shape: 'M 0 -3.5 L 3 3.5 L -3 3.5 Z';");
                Tooltip tip = new Tooltip("ALERT  [" + a.getSeverity().name() + "]\n"
                    + a.getTimestamp().format(DATE_FMT) + "\n" + a.getMessage());
                tip.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626;");
                Tooltip.install(node, tip);
            }
            if (alertSeries.getNode() != null) {
                javafx.scene.Node line = alertSeries.getNode().lookup(".chart-series-line");
                if (line != null) line.setStyle("-fx-stroke: transparent;");
            }
        });
    }

    // ── Feature 8: Inline legend ──────────────────────────────────────────

    private HBox buildInlineLegend(String[] names, String[] hexColors) {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(4, 0, 0, 0));
        for (int i = 0; i < names.length; i++) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + hexColors[i] + "; -fx-font-size: 13px;");
            Label name = new Label(names[i]);
            name.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");
            HBox item = new HBox(4, dot, name);
            item.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(item);
        }
        return legend;
    }

    // ── Line style helpers ────────────────────────────────────────────────

    private void applyLineStyle(XYChart.Series<String,Number> s, String hex) {
        if (s.getNode() == null) return;
        javafx.scene.Node line = s.getNode().lookup(".chart-series-line");
        if (line != null) line.setStyle("-fx-stroke: " + hex + "; -fx-stroke-width: 2; -fx-stroke-dash-array: 8 4;");
    }

    private void hideSymbolsChart(XYChart.Series<String,Number> s) {
        for (XYChart.Data<?,?> d : s.getData()) if (d.getNode() != null) d.getNode().setVisible(false);
    }

    // ── PDF export helpers ────────────────────────────────────────────────

    /** Section title row with inline Export PDF button. */
    private HBox sectionTitleRow(String text, String btnLabel, ThrowingExporter exporter) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        Button btn = new Button("📄 " + btnLabel);
        btn.getStyleClass().add("btn-secondary");
        btn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        btn.setOnAction(e -> {
            if (contentArea.getScene() == null) return;
            FileChooser fc = new FileChooser();
            fc.setTitle("Export PDF — " + text);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("FarmReport_" + text.replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
            File dest = fc.showSaveDialog(contentArea.getScene().getWindow());
            if (dest == null) return;
            btn.setDisable(true); btn.setText("⏳");
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override protected Void call() throws Exception { exporter.export(dest); return null; }
            };
            task.setOnSucceeded(ev -> { btn.setDisable(false); btn.setText("📄 " + btnLabel); });
            task.setOnFailed(ev -> {
                btn.setDisable(false); btn.setText("📄 " + btnLabel);
                if (task.getException() != null) task.getException().printStackTrace();
            });
            new Thread(task, "section-pdf").start();
        });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, l, spacer, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Snapshots a chart card and exports it as a PDF with embedded image. */
    private void exportChartNodePdf(String title, Node card) {
        WritableImage img = card.snapshot(new SnapshotParameters(), null);
        BufferedImage buffImg = SwingFXUtils.fromFXImage(img, null);
        if (contentArea.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Chart — " + title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("chart_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        File dest = fc.showSaveDialog(contentArea.getScene().getWindow());
        if (dest == null) return;
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                PdfReportService.getInstance().exportChartImage(title, buffImg, dest); return null;
            }
        };
        task.setOnFailed(ev -> { if (task.getException() != null) task.getException().printStackTrace(); });
        new Thread(task, "chart-pdf").start();
    }

    /** Per-sensor PDF: snapshots the card + includes a data table. */
    private void exportSensorPdf(NumericSensor sensor, Node card) {
        WritableImage img = card.snapshot(new SnapshotParameters(), null);
        BufferedImage buffImg = SwingFXUtils.fromFXImage(img, null);
        List<NumericSensorReading> readings = filteredNumericReadings(sensor);
        if (contentArea.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Sensor PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("sensor_" + sensor.getCode() + "_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");
        File dest = fc.showSaveDialog(contentArea.getScene().getWindow());
        if (dest == null) return;
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                PdfReportService.getInstance().exportSensorChart(sensor, readings, buffImg, dest);
                return null;
            }
        };
        task.setOnFailed(ev -> { if (task.getException() != null) task.getException().printStackTrace(); });
        new Thread(task, "sensor-pdf").start();
    }

    /** Per-GPS-sensor PDF: snapshots the card + includes readings summary. */
    private void exportGpsPdf(GPSCollarSensor sensor, Node card) {
        WritableImage img = card.snapshot(new SnapshotParameters(), null);
        BufferedImage buffImg = SwingFXUtils.fromFXImage(img, null);
        List<GPSSensorReading> readings = filteredGpsReadings(sensor);
        if (contentArea.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export GPS PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("gps_" + sensor.getCode() + ".pdf");
        File dest = fc.showSaveDialog(contentArea.getScene().getWindow());
        if (dest == null) return;
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                PdfReportService.getInstance().exportGpsChart(sensor, readings, buffImg, dest);
                return null;
            }
        };
        task.setOnFailed(ev -> { if (task.getException() != null) task.getException().printStackTrace(); });
        new Thread(task, "gps-pdf").start();
    }

    /** Export all sensors as a formatted PDF with data tables. */
    private void exportAllSensorsPdf() {
        if (contentArea.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export All Sensors PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("all_sensors_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        File dest = fc.showSaveDialog(contentArea.getScene().getWindow());
        if (dest == null) return;
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                PdfReportService.getInstance().exportSensors(dest); return null;
            }
        };
        task.setOnFailed(ev -> { if (task.getException() != null) task.getException().printStackTrace(); });
        new Thread(task, "all-sensors-pdf").start();
    }

    // ── Custom date dialog ────────────────────────────────────────────────

    private void showCustomDateDialog() {
        DatePicker startPicker = new DatePicker(sensorStart.toLocalDate());
        DatePicker endPicker   = new DatePicker(sensorEnd.toLocalDate());
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);
        Label startLbl = new Label("From"); startLbl.getStyleClass().add("az-form-label");
        Label endLbl   = new Label("To");   endLbl.getStyleClass().add("az-form-label");
        VBox form = new VBox(12, new VBox(5, startLbl, startPicker), new VBox(5, endLbl, endPicker));
        form.setPadding(new Insets(20, 24, 8, 24));
        Dialog<boolean[]> dlg = new Dialog<>();
        dlg.setTitle("Custom Date Range"); dlg.setHeaderText(null);
        dlg.getDialogPane().setContent(new VBox(0, buildDialogHeader("📅", "Custom Date Range",
            "Select the start and end dates for sensor data"), form));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setMinWidth(360);
        if (contentArea.getScene() != null) dlg.getDialogPane().getStylesheets().addAll(contentArea.getScene().getStylesheets());
        ((Button)dlg.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button)dlg.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? new boolean[]{true} : null);
        dlg.showAndWait().ifPresent(ok -> {
            if (startPicker.getValue() != null && endPicker.getValue() != null) {
                sensorStart = startPicker.getValue().atStartOfDay();
                sensorEnd   = endPicker.getValue().atTime(23, 59, 59);
                rebuildSensorContent();
            }
        });
    }

    private HBox buildDialogHeader(String icon, String title, String sub) {
        Label i = new Label(icon); i.getStyleClass().add("dialog-custom-header-icon");
        Label t = new Label(title); t.getStyleClass().add("dialog-custom-header-title");
        Label s = new Label(sub);  s.getStyleClass().add("dialog-custom-header-sub");
        VBox txt = new VBox(2, t, s);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(12, i, txt, sp);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("dialog-custom-header");
        return h;
    }

    // ── Old pair builder (kept for other callers) ─────────────────────────

    private void buildNumericSensorPairs(String kind, List<NumericSensor> sensors,
                                          java.util.function.Function<NumericSensor, String> label) {
        for (int i = 0; i < sensors.size(); i += 2) {
            NumericSensor s1 = sensors.get(i);
            Node chart1 = wrappedChart(label.apply(s1), numericLineChart(s1), false);
            Node table1 = tableCard("Readings — " + label.apply(s1).replace(kind + ": ", "") + " (" + s1.getUnit() + ")",
                    numericReadingRows(s1));
            if (i + 1 < sensors.size()) {
                NumericSensor s2 = sensors.get(i + 1);
                contentArea.getChildren().add(chartRow(chart1,
                    wrappedChart(label.apply(s2), numericLineChart(s2), false)));
                contentArea.getChildren().add(chartRow(table1,
                    tableCard("Readings — " + label.apply(s2).replace(kind + ": ", "") + " (" + s2.getUnit() + ")",
                        numericReadingRows(s2))));
            } else {
                contentArea.getChildren().add(chartRow(chart1, new VBox()));
                contentArea.getChildren().add(chartRow(table1, new VBox()));
            }
        }
    }

    // ── Sensor chart builders ─────────────────────────────────────────

    private LineChart<String, Number> numericLineChart(NumericSensor sensor) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis   yAxis = new NumberAxis();   yAxis.setLabel(sensor.getUnit());
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        List<SensorReading> history = sensor.getReadingHistory();
        if (history.isEmpty()) {
            XYChart.Series<String, Number> empty = new XYChart.Series<>();
            empty.setName("No data");
            chart.getData().add(empty);
            return chart;
        }

        XYChart.Series<String, Number> values = new XYChart.Series<>();
        values.setName(sensor.getUnit());
        XYChart.Series<String, Number> minLine = new XYChart.Series<>();
        minLine.setName("Min (" + sensor.getMinThreshold() + ")");
        XYChart.Series<String, Number> maxLine = new XYChart.Series<>();
        maxLine.setName("Max (" + sensor.getMaxThreshold() + ")");

        for (SensorReading r : history) {
            if (r instanceof NumericSensorReading nr) {
                String x = r.getTimestamp().format(DATE_FMT);
                values.getData().add(new XYChart.Data<>(x, nr.getValue()));
                minLine.getData().add(new XYChart.Data<>(x, sensor.getMinThreshold()));
                maxLine.getData().add(new XYChart.Data<>(x, sensor.getMaxThreshold()));
            }
        }

        chart.getData().addAll(values, minLine, maxLine);
        return chart;
    }

    private LineChart<String, Number> gpsLineChart(GPSCollarSensor sensor) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis   yAxis = new NumberAxis(0, 1.2, 0.5); yAxis.setLabel("Inside (1) / Outside (0)");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        List<SensorReading> history = sensor.getReadingHistory();
        if (history.isEmpty()) {
            XYChart.Series<String, Number> empty = new XYChart.Series<>();
            empty.setName("No GPS data");
            chart.getData().add(empty);
            return chart;
        }

        XYChart.Series<String, Number> insideSeries = new XYChart.Series<>();
        insideSeries.setName("Zone status (1=inside, 0=outside)");
        XYChart.Series<String, Number> latSeries = new XYChart.Series<>();
        latSeries.setName("Latitude (normalised)");

        // Compute lat range for normalisation
        double minLat = history.stream()
                .filter(r -> r instanceof GPSSensorReading)
                .mapToDouble(r -> ((GPSSensorReading)r).getLat()).min().orElse(0);
        double maxLat = history.stream()
                .filter(r -> r instanceof GPSSensorReading)
                .mapToDouble(r -> ((GPSSensorReading)r).getLat()).max().orElse(1);
        double range  = maxLat - minLat;

        for (SensorReading r : history) {
            if (r instanceof GPSSensorReading gr) {
                String x = r.getTimestamp().format(DATE_FMT);
                insideSeries.getData().add(new XYChart.Data<>(x, gr.isInsideZone() ? 1.0 : 0.0));
                double normLat = range > 0 ? (gr.getLat() - minLat) / range : 0.5;
                latSeries.getData().add(new XYChart.Data<>(x, normLat));
            }
        }

        chart.getData().addAll(insideSeries, latSeries);
        return chart;
    }

    // ── Sensor reading table rows ─────────────────────────────────────

    private List<String[]> numericReadingRows(NumericSensor sensor) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Reading", "Value", "Status"});  // header marker
        List<SensorReading> history = sensor.getReadingHistory();
        if (history.isEmpty()) { rows.add(row("No readings", "—")); return rows; }
        rows.add(row("Threshold", sensor.getMinThreshold() + " – " + sensor.getMaxThreshold() + " " + sensor.getUnit()));
        rows.add(row("Latest value", fmt2(sensor.getLastValue()) + " " + sensor.getUnit()));
        for (SensorReading r : history) {
            if (r instanceof NumericSensorReading nr) {
                String date   = r.getTimestamp().format(DATE_FMT);
                String status = nr.getSeverity().name();
                rows.add(row(date, fmt2(nr.getValue()) + " " + sensor.getUnit() + "  [" + status + "]"));
            }
        }
        return rows;
    }

    private List<String[]> gpsReadingRows(GPSCollarSensor sensor) {
        List<String[]> rows = new ArrayList<>();
        rows.add(row("Animal", sensor.getAnimal().getName()));
        rows.add(row("Status", sensor.hasEscaped() ? "OUTSIDE ZONE" : "Inside zone"));
        List<SensorReading> history = sensor.getReadingHistory();
        if (history.isEmpty()) { rows.add(row("No readings", "—")); return rows; }
        for (SensorReading r : history) {
            if (r instanceof GPSSensorReading gr) {
                String date   = r.getTimestamp().format(DATE_FMT);
                String loc    = String.format("%.4f, %.4f [%s]", gr.getLat(), gr.getLon(),
                        gr.isInsideZone() ? "IN" : "OUT");
                rows.add(row(date, loc));
            }
        }
        return rows;
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — OVERVIEW
    // ═══════════════════════════════════════════════════════════════════

    private PieChart zoneTypePie(List<ZONE> zones) {
        long ls   = zones.stream().filter(z -> z instanceof LivestockZONE).count();
        long crop = zones.stream().filter(z -> z instanceof CropZONE).count();
        long aqua = zones.stream().filter(z -> z instanceof AquacultureZONE).count();
        PieChart chart = new PieChart();
        if (ls   > 0) chart.getData().add(new PieChart.Data("Livestock ("   + ls   + ")", ls));
        if (crop > 0) chart.getData().add(new PieChart.Data("Crop ("        + crop + ")", crop));
        if (aqua > 0) chart.getData().add(new PieChart.Data("Aquaculture (" + aqua + ")", aqua));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No zones", 1));
        return stylePie(chart);
    }

    private PieChart animalHealthPie(List<Animal> animals) {
        long healthy = animals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Healthy).count();
        long sick    = animals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Sick).count();
        long quarant = animals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Quarantined).count();
        PieChart chart = new PieChart();
        if (healthy > 0) chart.getData().add(new PieChart.Data("Healthy ("     + healthy + ")", healthy));
        if (sick    > 0) chart.getData().add(new PieChart.Data("Sick ("        + sick    + ")", sick));
        if (quarant > 0) chart.getData().add(new PieChart.Data("Quarantined (" + quarant + ")", quarant));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No animals", 1));
        return stylePie(chart);
    }

    private BarChart<String, Number> productionByZoneBar() {
        BarChart<String, Number> chart = newBarChart("Zone", "Production");
        XYChart.Series<String, Number> milkS = new XYChart.Series<>(); milkS.setName("Milk (L)");
        XYChart.Series<String, Number> eggS  = new XYChart.Series<>(); eggS.setName("Eggs");
        for (LivestockZONE z : zoneService.getLivestockZones()) {
            milkS.getData().add(new XYChart.Data<>(shortName(z.getName()), z.getTotalMilkYield()));
            eggS.getData().add(new XYChart.Data<>(shortName(z.getName()),  z.getTotalEggCount()));
        }
        if (!milkS.getData().isEmpty()) chart.getData().add(milkS);
        if (!eggS.getData().isEmpty())  chart.getData().add(eggS);
        if (chart.getData().isEmpty())  chart.getData().add(emptySeries("No livestock zones"));
        return chart;
    }

    private PieChart alertSeverityPie(List<Alert> alerts) {
        long critical = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.Critical).count();
        long warning  = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.Warning).count();
        PieChart chart = new PieChart();
        if (critical > 0) chart.getData().add(new PieChart.Data("Critical (" + critical + ")", critical));
        if (warning  > 0) chart.getData().add(new PieChart.Data("Warning ("  + warning  + ")", warning));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No alerts", 1));
        return stylePie(chart);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — LIVESTOCK
    // ═══════════════════════════════════════════════════════════════════

    private BarChart<String, Number> milkPerZoneBar(List<LivestockZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Zone", "Milk (L)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Milk (L)");
        for (LivestockZONE z : zones)
            s.getData().add(new XYChart.Data<>(shortName(z.getName()), z.getTotalMilkYield()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private BarChart<String, Number> eggPerZoneBar(List<LivestockZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Zone", "Eggs");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Eggs");
        for (LivestockZONE z : zones)
            s.getData().add(new XYChart.Data<>(shortName(z.getName()), z.getTotalEggCount()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private BarChart<String, Number> topMilkProducersBar(List<LivestockZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Animal", "Milk (L)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Milk (L)");
        zones.stream().flatMap(z -> z.getAnimals().stream())
             .filter(a -> a.getMilkYieldLiters() > 0)
             .sorted(Comparator.comparingDouble(Animal::getMilkYieldLiters).reversed())
             .limit(8)
             .forEach(a -> s.getData().add(new XYChart.Data<>(a.getName(), a.getMilkYieldLiters())));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No milk producers") : s);
        return chart;
    }

    private PieChart livestockHealthPie(List<LivestockZONE> zones) {
        return animalHealthPie(zones.stream().flatMap(z -> z.getAnimals().stream()).toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — CROPS
    // ═══════════════════════════════════════════════════════════════════

    private BarChart<String, Number> yieldPerZoneBar(List<CropZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Zone", "Yield (kg)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Yield (kg)");
        for (CropZONE z : zones)
            s.getData().add(new XYChart.Data<>(shortName(z.getName()), z.getTotalCropYield()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private PieChart growthStagePie(List<CropZONE> zones) {
        Map<GrowthStage, Long> counts = zones.stream().flatMap(z -> z.getFields().stream())
                .collect(Collectors.groupingBy(Crop::getGrowthStage, Collectors.counting()));
        PieChart chart = new PieChart();
        for (Map.Entry<GrowthStage, Long> e : counts.entrySet())
            chart.getData().add(new PieChart.Data(capitalize(e.getKey().name()) + " (" + e.getValue() + ")", e.getValue()));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No crops", 1));
        return stylePie(chart);
    }

    private BarChart<String, Number> yieldByCropTypeBar(List<CropZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Crop Type", "Yield (kg)");
        chart.setLegendVisible(false);
        Map<CropType, Double> byType = new LinkedHashMap<>();
        zones.stream().flatMap(z -> z.getFields().stream())
             .forEach(c -> byType.merge(c.getCropType(), c.getYieldKg(), Double::sum));
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Yield (kg)");
        byType.forEach((k, v) -> s.getData().add(new XYChart.Data<>(k.name(), v)));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No harvests") : s);
        return chart;
    }

    private PieChart harvestedVsPendingPie(List<CropZONE> zones) {
        long harvested = zones.stream().flatMap(z -> z.getFields().stream()).filter(Crop::wasHarvested).count();
        long pending   = zones.stream().flatMap(z -> z.getFields().stream()).filter(c -> !c.wasHarvested()).count();
        PieChart chart = new PieChart();
        if (harvested > 0) chart.getData().add(new PieChart.Data("Harvested (" + harvested + ")", harvested));
        if (pending   > 0) chart.getData().add(new PieChart.Data("Pending ("   + pending   + ")", pending));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No crops", 1));
        return stylePie(chart);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — AQUACULTURE
    // ═══════════════════════════════════════════════════════════════════

    private BarChart<String, Number> harvestPerZoneBar(List<AquacultureZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Zone", "Harvest (kg)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Harvest (kg)");
        for (AquacultureZONE z : zones)
            s.getData().add(new XYChart.Data<>(shortName(z.getName()), z.getTotalHarvestWeight()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private BarChart<String, Number> survivalRateBar(List<AquacultureZONE> zones) {
        NumberAxis yAxis = new NumberAxis(0, 100, 10); yAxis.setLabel("Cycle Survival (%)");
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Species");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart"); chart.setAnimated(false); chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Survival (%)");
        for (AquacultureZONE z : zones)
            for (AquacultureSpecies sp : z.getSpeciesList())
                s.getData().add(new XYChart.Data<>(shortName(sp.getName()), sp.getCycleSurvivalRatePercent()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private BarChart<String, Number> stockVsHarvestedBar(List<AquacultureZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Species", "Count");
        XYChart.Series<String, Number> stockS = new XYChart.Series<>(); stockS.setName("Current Stock");
        XYChart.Series<String, Number> harvS  = new XYChart.Series<>(); harvS.setName("Harvested");
        for (AquacultureZONE z : zones)
            for (AquacultureSpecies sp : z.getSpeciesList()) {
                String lbl = shortName(sp.getName());
                stockS.getData().add(new XYChart.Data<>(lbl, sp.getNumSpecies()));
                harvS.getData().add(new XYChart.Data<>(lbl,  sp.getTotalHarvestedCount()));
            }
        if (!stockS.getData().isEmpty()) chart.getData().add(stockS);
        if (!harvS.getData().isEmpty())  chart.getData().add(harvS);
        if (chart.getData().isEmpty())   chart.getData().add(emptySeries("No species"));
        return chart;
    }

    private PieChart speciesDistPie(List<AquacultureZONE> zones) {
        PieChart chart = new PieChart();
        for (AquacultureZONE z : zones)
            for (AquacultureSpecies sp : z.getSpeciesList())
                if (sp.getNumSpecies() > 0)
                    chart.getData().add(new PieChart.Data(sp.getName() + " (" + sp.getNumSpecies() + ")", sp.getNumSpecies()));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("No species", 1));
        return stylePie(chart);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — TREND LINES
    // ═══════════════════════════════════════════════════════════════════

    /** Daily milk yield per zone (one series per zone). */
    private LineChart<String, Number> milkTrendLine(List<LivestockZONE> zones) {
        LineChart<String, Number> chart = newLineChart("Date", "Milk (L)");
        for (LivestockZONE z : zones) {
            Map<String, Double> byDay = new TreeMap<>();
            for (Animal a : z.getAnimals())
                for (Animal.MilkRecord r : a.getMilkHistory())
                    byDay.merge(r.getTimestamp().format(DATE_FMT), r.getLiters(), Double::sum);
            if (byDay.isEmpty()) continue;
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(shortName(z.getName()));
            byDay.forEach((d, v) -> s.getData().add(new XYChart.Data<>(d, Math.round(v * 100.0) / 100.0)));
            chart.getData().add(s);
        }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No milk data"));
        return chart;
    }

    /** Daily egg count per zone (one series per zone). */
    private LineChart<String, Number> eggTrendLine(List<LivestockZONE> zones) {
        LineChart<String, Number> chart = newLineChart("Date", "Eggs");
        for (LivestockZONE z : zones) {
            Map<String, Long> byDay = new TreeMap<>();
            for (Animal a : z.getAnimals())
                for (Animal.EggRecord r : a.getEggHistory())
                    byDay.merge(r.getTimestamp().format(DATE_FMT), (long) r.getCount(), Long::sum);
            if (byDay.isEmpty()) continue;
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(shortName(z.getName()));
            byDay.forEach((d, v) -> s.getData().add(new XYChart.Data<>(d, v)));
            chart.getData().add(s);
        }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No egg data"));
        return chart;
    }

    /** Average animal weight per zone over time (one series per zone). */
    private LineChart<String, Number> weightTrendLine(List<LivestockZONE> zones) {
        LineChart<String, Number> chart = newLineChart("Date", "Avg Weight (kg)");
        for (LivestockZONE z : zones) {
            // sum and count per day so we can average
            Map<String, double[]> byDay = new TreeMap<>();  // date → [sum, count]
            for (Animal a : z.getAnimals())
                for (Animal.WeightRecord r : a.getWeightHistory()) {
                    String d = r.getTimestamp().format(DATE_FMT);
                    byDay.computeIfAbsent(d, k -> new double[]{0, 0});
                    byDay.get(d)[0] += r.getWeight();
                    byDay.get(d)[1] += 1;
                }
            if (byDay.isEmpty()) continue;
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(shortName(z.getName()));
            byDay.forEach((d, arr) -> s.getData().add(
                new XYChart.Data<>(d, Math.round(arr[0] / arr[1] * 10.0) / 10.0)));
            chart.getData().add(s);
        }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No weight data"));
        return chart;
    }

    /** Cumulative crop yield over time — one series per zone. */
    private LineChart<String, Number> cropYieldTrendLine(List<CropZONE> zones) {
        LineChart<String, Number> chart = newLineChart("Date", "Cumul. Yield (kg)");
        for (CropZONE z : zones) {
            Map<String, Double> byDay = new TreeMap<>();
            for (Crop c : z.getFields())
                for (Crop.YieldRecord r : c.getYieldHistory())
                    byDay.merge(r.getDate().format(DATE_FMT), r.getKg(), Double::sum);
            if (byDay.isEmpty()) continue;
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(shortName(z.getName()));
            double cumul = 0;
            for (Map.Entry<String, Double> e : byDay.entrySet()) {
                cumul += e.getValue();
                s.getData().add(new XYChart.Data<>(e.getKey(), Math.round(cumul * 10.0) / 10.0));
            }
            chart.getData().add(s);
        }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No yield history"));
        return chart;
    }

    /** Per-date total harvest batch weight across all zones (grouped bar). */
    private BarChart<String, Number> cropBatchBar(List<CropZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Date", "Batch (kg)");
        for (CropZONE z : zones) {
            Map<String, Double> byDay = new TreeMap<>();
            for (Crop c : z.getFields())
                for (Crop.YieldRecord r : c.getYieldHistory())
                    byDay.merge(r.getDate().format(DATE_FMT), r.getKg(), Double::sum);
            if (byDay.isEmpty()) continue;
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(shortName(z.getName()));
            byDay.forEach((d, v) -> s.getData().add(new XYChart.Data<>(d, Math.round(v * 10.0) / 10.0)));
            chart.getData().add(s);
        }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No batch data"));
        return chart;
    }

    /** Harvest weight over time per species — one series per species. */
    private LineChart<String, Number> aquaHarvestTrendLine(List<AquacultureZONE> zones) {
        LineChart<String, Number> chart = newLineChart("Date", "Harvest (kg)");
        for (AquacultureZONE z : zones)
            for (AquacultureSpecies sp : z.getSpeciesList()) {
                if (sp.getHarvestHistory().isEmpty()) continue;
                XYChart.Series<String, Number> s = new XYChart.Series<>();
                s.setName(shortName(sp.getName()));
                for (AquacultureSpecies.HarvestRecord r : sp.getHarvestHistory())
                    s.getData().add(new XYChart.Data<>(r.getDate().format(DATE_FMT), r.getWeightKg()));
                chart.getData().add(s);
            }
        if (chart.getData().isEmpty()) chart.getData().add(emptySeries("No harvest history"));
        return chart;
    }

    /** Cumulative harvest weight per species (bar). */
    private BarChart<String, Number> aquaCumulativeBar(List<AquacultureZONE> zones) {
        BarChart<String, Number> chart = newBarChart("Species", "Total Harvest (kg)");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Harvest (kg)");
        for (AquacultureZONE z : zones)
            for (AquacultureSpecies sp : z.getSpeciesList())
                s.getData().add(new XYChart.Data<>(shortName(sp.getName()), sp.getTotalHarvestWeightKg()));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private LineChart<String, Number> newLineChart(String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel(xLabel);
        NumberAxis   yAxis = new NumberAxis();   yAxis.setLabel(yLabel);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        return chart;
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHART BUILDERS — ALERTS
    // ═══════════════════════════════════════════════════════════════════

    private PieChart alertTypePie(List<Alert> alerts) {
        Map<AlertType, Long> counts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
        PieChart chart = new PieChart();
        for (Map.Entry<AlertType, Long> e : counts.entrySet())
            chart.getData().add(new PieChart.Data(e.getKey().name() + " (" + e.getValue() + ")", e.getValue()));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("None", 1));
        chart.setLabelsVisible(false);
        return stylePie(chart);
    }

    private PieChart alertStatusPie(List<Alert> alerts) {
        long active    = alerts.stream().filter(Alert::isActive).count();
        long acked     = alerts.stream().filter(Alert::isAcknowledged).count();
        long resolved  = alerts.stream().filter(Alert::isResolved).count();
        long dismissed = alerts.stream().filter(Alert::isDismissed).count();
        PieChart chart = new PieChart();
        if (active    > 0) chart.getData().add(new PieChart.Data("Active (" + active + ")",         active));
        if (acked     > 0) chart.getData().add(new PieChart.Data("Acknowledged (" + acked + ")",    acked));
        if (resolved  > 0) chart.getData().add(new PieChart.Data("Resolved (" + resolved + ")",     resolved));
        if (dismissed > 0) chart.getData().add(new PieChart.Data("Dismissed (" + dismissed + ")",   dismissed));
        if (chart.getData().isEmpty()) chart.getData().add(new PieChart.Data("None", 1));
        return stylePie(chart);
    }

    private BarChart<String, Number> alertsPerZoneBar(List<Alert> alerts) {
        BarChart<String, Number> chart = newBarChart("Zone", "Alert Count");
        chart.setLegendVisible(false);
        Map<String, Long> perZone = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getZoneName, Collectors.counting()));
        XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Alerts");
        perZone.entrySet().stream()
               .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
               .forEach(e -> s.getData().add(new XYChart.Data<>(shortName(e.getKey()), e.getValue())));
        chart.getData().add(s.getData().isEmpty() ? emptySeries("No data") : s);
        return chart;
    }

    private HBox alertRow(Alert a) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("detail-row");
        row.setPadding(new Insets(6, 8, 6, 8));
        Label sevLabel = new Label(a.getSeverity().name());
        sevLabel.getStyleClass().addAll("badge", a.getSeverity() == AlertSeverity.Critical ? "badge-critical" : "badge-warning");
        Label typeLabel = new Label(a.getType().name());
        typeLabel.getStyleClass().add("detail-key");
        typeLabel.setMinWidth(140);
        Label msgLabel = new Label(a.getMessage());
        msgLabel.getStyleClass().add("detail-value");
        msgLabel.setWrapText(true);
        HBox.setHgrow(msgLabel, Priority.ALWAYS);
        Label timeLabel = new Label(a.getFormattedTimestamp());
        timeLabel.getStyleClass().add("text-muted");
        row.getChildren().addAll(sevLabel, typeLabel, msgLabel, timeLabel);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PDF EXPORT
    // ═══════════════════════════════════════════════════════════════════

    /*
     * HOW PDF EXPORT WORKS
     * ────────────────────
     * Library : Apache PDFBox 3.x (added to pom.xml)
     *
     * PDFBox lets us open a PDDocument, add PDPages, and write
     * text / lines onto each page via PDPageContentStream.
     * All rendering is done in PdfReportService using standard
     * Helvetica Type-1 fonts (built into every PDF viewer — no
     * font embedding needed). A custom PdfWriter inner class
     * tracks the current Y coordinate and auto-creates new pages
     * when content would overflow the bottom margin.
     *
     * Each export button:
     *  1. Opens a JavaFX FileChooser so the user picks a save path.
     *  2. Calls the matching PdfReportService.export*() method.
     *  3. Shows an inline success or error notification.
     *
     * Charts are NOT in the PDF (bitmap-rendering a JavaFX chart
     * to PDF is very complex). The PDF contains the same numeric
     * data as the on-screen tables — all the same key/value rows.
     */

    private void buildExport() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(sectionTitle("Export PDF Reports"));

        // Intro card
        VBox intro = new VBox(6);
        intro.getStyleClass().add("kpi-card");
        intro.setPadding(new Insets(16));
        Label introText = new Label(
            "Generate professional PDF summaries for any section of your farm data. " +
            "Each PDF contains the same numeric data shown in the reports tables. " +
            "Charts are available interactively in the other sections above.");
        introText.setWrapText(true);
        introText.getStyleClass().add("detail-value");
        Label techNote = new Label(
            "Technical note: PDFs are generated with Apache PDFBox 3.x using standard " +
            "Helvetica fonts. A custom page-manager tracks Y-position and inserts new " +
            "pages automatically. All text is Latin-1 sanitized for font compatibility.");
        techNote.setWrapText(true);
        techNote.getStyleClass().add("text-muted");
        techNote.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
        intro.getChildren().addAll(introText, techNote);
        contentArea.getChildren().add(intro);

        // Notification placeholder
        Label notification = new Label();
        notification.setVisible(false);
        notification.setManaged(false);
        notification.setWrapText(true);
        notification.getStyleClass().add("kpi-card");
        notification.setPadding(new Insets(12, 16, 12, 16));
        contentArea.getChildren().add(notification);

        // Export cards grid — 2 per row
        record ExportDef(String icon, String title, String desc, String[] includes,
                         ThrowingExporter exporter) {}

        PdfReportService pdf = PdfReportService.getInstance();

        List<ExportDef> defs = List.of(
            new ExportDef("📊", "Farm Overview",
                "Complete farm snapshot — all zones, animals, sensors, and alerts at a glance.",
                new String[]{"Farm identity & location", "Zone breakdown (all types)",
                             "Animal health summary", "Alert summary"},
                pdf::exportOverview),
            new ExportDef("🐄", "Livestock Report",
                "Detailed livestock production — milk, eggs, health per zone and per animal.",
                new String[]{"Per-zone animal counts", "Milk & egg totals",
                             "Health breakdown", "Top milk producers"},
                pdf::exportLivestock),
            new ExportDef("🌾", "Crops Report",
                "Crop production summary — yield, growth stages, and field breakdown per zone.",
                new String[]{"Per-zone yield totals", "Field-level stage & yield",
                             "Yield by crop type", "Harvested vs pending"},
                pdf::exportCrops),
            new ExportDef("🐟", "Aquaculture Report",
                "Aquaculture production — stock counts, harvest weights, and survival rates.",
                new String[]{"Per-zone species list", "Stock & harvest per species",
                             "Survival & mortality rates"},
                pdf::exportAquaculture),
            new ExportDef("🔔", "Alerts Report",
                "Full alert history — by type, severity, status, and zone.",
                new String[]{"Alert totals by type & severity", "Status breakdown",
                             "Active alerts list (up to 30)"},
                pdf::exportAlerts),
            new ExportDef("📡", "Sensor History Report",
                "Sensor inventory — last readings and history counts for all sensor types.",
                new String[]{"Sensor counts by type", "Per-zone sensor details",
                             "Last value & reading count per sensor"},
                pdf::exportSensors)
        );

        for (int i = 0; i < defs.size(); i += 2) {
            ExportDef d1 = defs.get(i);
            VBox card1 = buildExportCard(d1.icon(), d1.title(), d1.desc(), d1.includes(),
                d1.exporter(), notification);
            if (i + 1 < defs.size()) {
                ExportDef d2 = defs.get(i + 1);
                VBox card2 = buildExportCard(d2.icon(), d2.title(), d2.desc(), d2.includes(),
                    d2.exporter(), notification);
                contentArea.getChildren().add(chartRow(card1, card2));
            } else {
                HBox row = new HBox(16);
                HBox.setHgrow(card1, Priority.ALWAYS);
                card1.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(card1);
                contentArea.getChildren().add(row);
            }
        }

        // Full report card — full width
        contentArea.getChildren().add(sectionTitle("Complete Report"));
        VBox fullCard = buildExportCard("📋", "Full Farm Report",
            "All sections combined into one comprehensive PDF document.",
            new String[]{"Farm overview", "All livestock zones", "All crop zones",
                         "All aquaculture zones", "Alert summary"},
            pdf::exportFull, notification);
        fullCard.setStyle(fullCard.getStyle() +
            "-fx-border-color: #16A34A; -fx-border-width: 2; -fx-border-radius: 12;");
        contentArea.getChildren().add(fullCard);
    }

    private VBox buildExportCard(String icon, String title, String desc,
                                  String[] includes, ThrowingExporter exporter,
                                  Label notification) {
        VBox card = new VBox(10);
        card.getStyleClass().add("kpi-card");
        card.setPadding(new Insets(16));

        // Header row
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title");
        HBox header = new HBox(10, iconLbl, titleLbl);
        header.setAlignment(Pos.CENTER_LEFT);

        // Description
        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.getStyleClass().add("detail-value");

        // Includes bullet list
        VBox bullets = new VBox(3);
        for (String inc : includes) {
            Label bullet = new Label("  ✓  " + inc);
            bullet.getStyleClass().add("text-muted");
            bullet.setStyle("-fx-font-size: 11px;");
            bullets.getChildren().add(bullet);
        }

        // Export button
        Button exportBtn = new Button("📄  Export PDF");
        exportBtn.getStyleClass().add("btn-primary");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> runExport(title, exporter, exportBtn, notification));

        card.getChildren().addAll(header, new Separator(), descLbl, bullets, exportBtn);
        return card;
    }

    private void runExport(String section, ThrowingExporter exporter,
                           Button btn, Label notification) {
        Window window = contentArea.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF — " + section);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String safeName = section.replaceAll("[^a-zA-Z0-9]", "_");
        fc.setInitialFileName("FarmReport_" + safeName + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");

        File dest = fc.showSaveDialog(window);
        if (dest == null) return;

        btn.setDisable(true);
        btn.setText("⏳  Generating…");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                exporter.export(dest); return null;
            }
        };
        task.setOnSucceeded(ev -> {
            btn.setDisable(false);
            btn.setText("📄  Export PDF");
            showNotification(notification, true,
                "✅  PDF saved: " + dest.getAbsolutePath());
        });
        task.setOnFailed(ev -> {
            btn.setDisable(false);
            btn.setText("📄  Export PDF");
            String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
            showNotification(notification, false, "❌  Export failed: " + err);
            task.getException().printStackTrace();
        });
        new Thread(task, "pdf-export").start();
    }

    private void showNotification(Label lbl, boolean success, String msg) {
        lbl.setText(msg);
        lbl.setStyle(null);
        lbl.getStyleClass().removeAll("notification-success", "notification-error", "kpi-card");
        lbl.getStyleClass().add(success ? "notification-success" : "notification-error");
        lbl.setVisible(true);
        lbl.setManaged(true);
        lbl.requestFocus();
    }

    @FunctionalInterface
    private interface ThrowingExporter {
        void export(File dest) throws Exception;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    private Label cardTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-title");
        return l;
    }

    private HBox kpiRow(VBox... cards) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        for (VBox card : cards) { HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE); row.getChildren().add(card); }
        return row;
    }

    private VBox kpi(String label, String value, String accent) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("stat-card", accent);
        card.setPadding(new Insets(16, 12, 16, 12));
        Label val = new Label(value); val.getStyleClass().add("stat-value");
        Label lbl = new Label(label); lbl.getStyleClass().add("stat-label");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private HBox chartRow(Node left, Node right) {
        HBox row = new HBox(16);
        HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        if (left  instanceof Region r1) r1.setMaxWidth(Double.MAX_VALUE);
        if (right instanceof Region r2) r2.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().addAll(left, right);
        return row;
    }

    private VBox wrappedChart(String title, Node chart, boolean smallHeight) {
        VBox box = new VBox(8);
        box.getStyleClass().add("kpi-card");
        box.setPadding(new Insets(16));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Button pdfBtn = new Button("↓ PDF");
        pdfBtn.getStyleClass().add("btn-secondary");
        pdfBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
        pdfBtn.setOnAction(e -> exportChartNodePdf(title, box));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(8, titleLabel, spacer, pdfBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (chart instanceof Region r) { r.setPrefHeight(smallHeight ? 240 : 280); r.setMinHeight(smallHeight ? 200 : 240); }
        box.getChildren().addAll(headerRow, new Separator(), chart);
        return box;
    }

    /** Two-column label/value table wrapped in a kpi-card. */
    private VBox tableCard(String title, List<String[]> rows) {
        VBox box = new VBox(0);
        box.getStyleClass().add("kpi-card");
        box.setPadding(new Insets(16));
        Label titleLabel = new Label(title); titleLabel.getStyleClass().add("card-title");
        box.getChildren().addAll(titleLabel, new Separator());

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(0);
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140); col0.setPrefWidth(200); col0.setMaxWidth(260);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(60);  col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);
        grid.setPadding(new Insets(8, 0, 0, 0));

        int rowIdx = 0;
        for (String[] r : rows) {
            if (r.length == 1 && r[0].startsWith("__SEP__")) {
                // section separator row
                Separator sep = new Separator();
                GridPane.setColumnSpan(sep, 2);
                grid.add(sep, 0, rowIdx++);
                continue;
            }
            // Skip the header marker row used internally
            if (r.length >= 2 && r[0].equals("Reading")) continue;

            Label key = new Label(r[0]);
            key.getStyleClass().add("detail-key");
            key.setWrapText(true);

            Label val = new Label(r.length > 1 ? r[1] : "");
            val.getStyleClass().add("detail-value");
            val.setWrapText(true);

            HBox rowBox = new HBox(key);
            rowBox.getStyleClass().add("detail-row");
            rowBox.setPadding(new Insets(5, 0, 5, 0));
            grid.add(rowBox, 0, rowIdx);
            grid.add(val,    1, rowIdx);
            rowIdx++;
        }
        box.getChildren().add(grid);
        return box;
    }

    private Label emptyState(String msg) {
        Label l = new Label(msg); l.getStyleClass().add("text-muted"); l.setPadding(new Insets(32));
        return l;
    }

    private Label zoneSeparator(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("detail-section-title");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(10, 16, 10, 12));
        return l;
    }

    private BarChart<String, Number> newBarChart(String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel(xLabel);
        NumberAxis   yAxis = new NumberAxis();   yAxis.setLabel(yLabel);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().add("farm-chart"); chart.setAnimated(false);
        return chart;
    }

    private XYChart.Series<String, Number> emptySeries(String label) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(label); s.getData().add(new XYChart.Data<>(label, 0));
        return s;
    }

    private PieChart stylePie(PieChart chart) {
        chart.getStyleClass().add("farm-chart");
        chart.setLegendVisible(true); chart.setLabelsVisible(true);
        return chart;
    }

    // ── Table row factories ───────────────────────────────────────────

    private String[] row(String key, int value)    { return new String[]{key, String.valueOf(value)}; }
    private String[] row(String key, String value) { return new String[]{key, value}; }

    // ── Misc helpers ──────────────────────────────────────────────────

    private String shortName(String name) {
        if (name == null) return "N/A";
        return name.length() > 15 ? name.substring(0, 13) + "…" : name;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String fmt2(double v) { return String.format("%.2f", v); }
}
