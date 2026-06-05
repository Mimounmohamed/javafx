package com.example.services;

/*
 * PDF Report Generation — Apache PDFBox 3.x
 *
 * HOW IT WORKS:
 *  1. PDDocument  — the in-memory PDF file.
 *  2. PDPage      — one A4 page added to the document.
 *  3. PDPageContentStream — a drawing surface for one page.
 *     All text is written with beginText() / setFont() /
 *     newLineAtOffset() / showText() / endText().
 *  4. Lines are drawn with moveTo() / lineTo() / stroke().
 *  5. PdfWriter (inner helper) tracks the current Y position
 *     and automatically adds a new page when content runs out
 *     of room, so callers never have to think about pagination.
 *  6. PDType1Font uses standard Helvetica (built into every
 *     PDF viewer — no font embedding needed).
 *  7. Because Type-1 fonts only cover Latin-1 characters,
 *     every string is sanitized to strip emojis / CJK / etc.
 *     before being handed to showText().
 */

import Alerts.Alert;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Animals.Animal;
import Animals.AnimalHealthStatus;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Sensors.*;
import ZONES.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PdfReportService {

    private static PdfReportService instance;

    private final FarmService   farmService   = FarmService.getInstance();
    private final ZoneService   zoneService   = ZoneService.getInstance();
    private final AlertService  alertService  = AlertService.getInstance();
    private final AnimalService animalService = AnimalService.getInstance();
    private final SensorService sensorService = SensorService.getInstance();

    private PdfReportService() {}

    public static PdfReportService getInstance() {
        if (instance == null) instance = new PdfReportService();
        return instance;
    }

    // ── Public export entry-points ────────────────────────────────────

    public void exportOverview(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Farm Overview");
            writeFarmSummarySection(w);
            writeZoneBreakdownSection(w);
            writeAnimalHealthSection(w);
            writeAlertSummarySection(w);
            w.save(dest);
        }
    }

    public void exportLivestock(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Livestock Report");
            List<LivestockZONE> zones = zoneService.getLivestockZones();
            double totalMilk  = zones.stream().mapToDouble(LivestockZONE::getTotalMilkYield).sum();
            long   totalEggs  = zones.stream().mapToLong(LivestockZONE::getTotalEggCount).sum();
            long   totalAnims = zones.stream().mapToLong(z -> z.getAnimals().size()).sum();

            w.writeSectionTitle("Summary");
            w.writeRow("Livestock zones",  String.valueOf(zones.size()));
            w.writeRow("Total animals",    String.valueOf(totalAnims));
            w.writeRow("Total milk yield", String.format("%.1f L", totalMilk));
            w.writeRow("Total egg count",  String.valueOf(totalEggs));

            if (!zones.isEmpty()) {
                w.writeSectionTitle("Per-Zone Breakdown");
                for (LivestockZONE z : zones) {
                    w.writeSectionHeader(z.getName() + "  [" + z.getType() + "]");
                    long healthy   = z.getAnimals().stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Healthy).count();
                    long sick      = z.getAnimals().stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Sick).count();
                    long quarant   = z.getAnimals().stream().filter(Animal::isQuarantined).count();
                    w.writeRow("Animals",        String.valueOf(z.getAnimals().size()));
                    w.writeRow("Healthy",        String.valueOf(healthy));
                    w.writeRow("Sick",           String.valueOf(sick));
                    w.writeRow("Quarantined",    String.valueOf(quarant));
                    w.writeRow("Milk yield",     String.format("%.2f L", z.getTotalMilkYield()));
                    w.writeRow("Egg count",      String.valueOf(z.getTotalEggCount()));
                    w.writeRow("Bio sensors",    String.valueOf(z.getBioSensors().size()));
                    w.writeRow("GPS sensors",    String.valueOf(z.getGpsCollarSensors().size()));
                    w.space(6);

                    // Top 5 milk producers in zone
                    List<Animal> topMilk = z.getAnimals().stream()
                        .filter(a -> a.getMilkYieldLiters() > 0)
                        .sorted(Comparator.comparingDouble(Animal::getMilkYieldLiters).reversed())
                        .limit(5).collect(Collectors.toList());
                    if (!topMilk.isEmpty()) {
                        w.writeSubHeader("Top Milk Producers");
                        for (Animal a : topMilk)
                            w.writeRow("  " + a.getName(), String.format("%.2f L", a.getMilkYieldLiters()));
                    }
                }

                // Overall top producers across all zones
                w.writeSectionTitle("Top 10 Milk Producers (All Zones)");
                zones.stream().flatMap(z -> z.getAnimals().stream())
                     .filter(a -> a.getMilkYieldLiters() > 0)
                     .sorted(Comparator.comparingDouble(Animal::getMilkYieldLiters).reversed())
                     .limit(10)
                     .forEach(a -> {
                         try { w.writeRow(a.getName() + " [" + a.getZone().getName() + "]",
                                          String.format("%.2f L", a.getMilkYieldLiters())); }
                         catch (IOException ignored) {}
                     });
            }
            w.save(dest);
        }
    }

    public void exportCrops(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Crops Report");
            List<CropZONE> zones = zoneService.getCropZones();
            double totalYield = zones.stream().mapToDouble(CropZONE::getTotalCropYield).sum();
            long   harvested  = zones.stream().flatMap(z -> z.getFields().stream()).filter(Crop::wasHarvested).count();
            long   total      = zones.stream().mapToLong(z -> z.getFields().size()).sum();

            w.writeSectionTitle("Summary");
            w.writeRow("Crop zones",      String.valueOf(zones.size()));
            w.writeRow("Total fields",    String.valueOf(total));
            w.writeRow("Harvested",       String.valueOf(harvested));
            w.writeRow("Pending",         String.valueOf(total - harvested));
            w.writeRow("Total yield",     String.format("%.2f kg", totalYield));

            if (!zones.isEmpty()) {
                w.writeSectionTitle("Per-Zone Breakdown");
                for (CropZONE z : zones) {
                    w.writeSectionHeader(z.getName());
                    w.writeRow("Fields",      String.valueOf(z.getFields().size()));
                    w.writeRow("Total yield", String.format("%.2f kg", z.getTotalCropYield()));
                    w.writeRow("Surface",     String.format("%.1f ha", z.getSurfacePlanted()));
                    w.space(4);
                    for (Crop c : z.getFields()) {
                        w.writeRow("  " + c.getVariety() + " [" + c.getCropType() + "]",
                            c.getGrowthStage().name() + " — " + String.format("%.2f kg", c.getYieldKg()));
                    }
                }

                w.writeSectionTitle("Yield by Crop Type");
                Map<String, Double> byType = new LinkedHashMap<>();
                zones.stream().flatMap(z -> z.getFields().stream())
                     .forEach(c -> byType.merge(c.getCropType().name(), c.getYieldKg(), Double::sum));
                byType.forEach((k, v) -> {
                    try { w.writeRow(k, String.format("%.2f kg", v)); }
                    catch (IOException ignored) {}
                });
            }
            w.save(dest);
        }
    }

    public void exportAquaculture(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Aquaculture Report");
            List<AquacultureZONE> zones = zoneService.getAquacultureZones();
            double totalHarvest = zones.stream().mapToDouble(AquacultureZONE::getTotalHarvestWeight).sum();
            long   totalStock   = zones.stream().mapToLong(AquacultureZONE::getTotalSpeciesCount).sum();

            w.writeSectionTitle("Summary");
            w.writeRow("Aquaculture zones", String.valueOf(zones.size()));
            w.writeRow("Total stock",       String.valueOf(totalStock));
            w.writeRow("Total harvest",     String.format("%.2f kg", totalHarvest));

            if (!zones.isEmpty()) {
                w.writeSectionTitle("Per-Zone Breakdown");
                for (AquacultureZONE z : zones) {
                    w.writeSectionHeader(z.getName());
                    w.writeRow("Species groups", String.valueOf(z.getSpeciesList().size()));
                    w.writeRow("Total stock",    String.valueOf(z.getTotalSpeciesCount()));
                    w.writeRow("Harvest weight", String.format("%.2f kg", z.getTotalHarvestWeight()));
                    w.space(4);
                    for (AquacultureSpecies sp : z.getSpeciesList()) {
                        w.writeSubHeader("  " + sp.getName());
                        w.writeRow("    Current stock",   String.valueOf(sp.getNumSpecies()));
                        w.writeRow("    Harvested count", String.valueOf(sp.getTotalHarvestedCount()));
                        w.writeRow("    Harvest weight",  String.format("%.2f kg", sp.getTotalHarvestWeightKg()));
                        w.writeRow("    Cycle survival",  String.format("%.1f%%", sp.getCycleSurvivalRatePercent()));
                        w.writeRow("    Mortality",       String.valueOf(sp.getTotalMortality()));
                    }
                }
            }
            w.save(dest);
        }
    }

    public void exportAlerts(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Alerts Report");
            List<Alert> alerts = alertService.getAllAlerts();
            long active   = alerts.stream().filter(Alert::isActive).count();
            long acked    = alerts.stream().filter(Alert::isAcknowledged).count();
            long resolved = alerts.stream().filter(Alert::isResolved).count();
            long critical = alertService.getAlertsBySeverity(AlertSeverity.Critical).size();
            long warning  = alertService.getAlertsBySeverity(AlertSeverity.Warning).size();

            w.writeSectionTitle("Alert Summary");
            w.writeRow("Total alerts",  String.valueOf(alerts.size()));
            w.writeRow("Active",        String.valueOf(active));
            w.writeRow("Acknowledged",  String.valueOf(acked));
            w.writeRow("Resolved",      String.valueOf(resolved));
            w.writeRow("Critical",      String.valueOf(critical));
            w.writeRow("Warning",       String.valueOf(warning));

            // By type
            w.writeSectionTitle("Alerts by Type");
            Map<AlertType, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
            byType.entrySet().stream()
                .sorted(Map.Entry.<AlertType, Long>comparingByValue().reversed())
                .forEach(e -> {
                    try { w.writeRow(e.getKey().name(), String.valueOf(e.getValue())); }
                    catch (IOException ignored) {}
                });

            // Active alert list
            List<Alert> activeAlerts = alerts.stream()
                .filter(Alert::isActive)
                .sorted(Comparator.comparing(Alert::getSeverity))
                .limit(30)
                .collect(Collectors.toList());
            if (!activeAlerts.isEmpty()) {
                w.writeSectionTitle("Active Alerts  (up to 30)");
                w.writeSubHeader("Severity   Type                 Zone             Message");
                w.drawDivider();
                for (Alert a : activeAlerts) {
                    String line = pad(a.getSeverity().name(), 10)
                        + pad(a.getType().name(), 22)
                        + pad(a.getZoneName(), 18)
                        + truncStr(a.getMessage(), 40);
                    w.writeMono(line);
                }
            }
            w.save(dest);
        }
    }

    public void exportSensors(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Sensor History Report");

            w.writeSectionTitle("Sensor Counts");
            w.writeRow("Bio sensors",   String.valueOf(sensorService.getAllBioSensors().size()));
            w.writeRow("GPS sensors",   String.valueOf(sensorService.getAllGPSSensors().size()));
            w.writeRow("Env sensors",   String.valueOf(sensorService.getAllEnvSensors().size()));
            w.writeRow("Soil sensors",  String.valueOf(sensorService.getAllSoilSensors().size()));
            w.writeRow("Water sensors", String.valueOf(sensorService.getAllWaterSensors().size()));

            // Per livestock zone — bio + GPS sensors
            for (LivestockZONE z : zoneService.getLivestockZones()) {
                if (z.getBioSensors().isEmpty() && z.getGpsCollarSensors().isEmpty()) continue;
                w.writeSectionHeader(z.getName() + " — Livestock Zone");
                for (BioSensor s : z.getBioSensors()) {
                    w.writeSubHeader("Bio: " + s.getMeasureType() + " [" + s.getCode() + "] — " + s.getAnimal().getName());
                    w.writeRow("  Status",    s.getStatus().name());
                    w.writeRow("  Threshold", s.getMinThreshold() + " – " + s.getMaxThreshold() + " " + s.getUnit());
                    w.writeRow("  Last value", String.format("%.2f %s", s.getLastValue(), s.getUnit()));
                    w.writeRow("  Readings",  String.valueOf(s.getReadingHistory().size()));
                }
                for (GPSCollarSensor g : z.getGpsCollarSensors()) {
                    w.writeSubHeader("GPS: " + g.getAnimal().getName() + " [" + g.getCode() + "]");
                    w.writeRow("  Status",   g.getStatus().name());
                    w.writeRow("  Escaped",  g.hasEscaped() ? "YES - outside zone" : "No");
                    w.writeRow("  Readings", String.valueOf(g.getReadingHistory().size()));
                }
            }

            // Per crop zone — env + soil sensors
            for (CropZONE z : zoneService.getCropZones()) {
                if (z.getEnvSensors().isEmpty() && z.getSoilSensors().isEmpty()) continue;
                w.writeSectionHeader(z.getName() + " — Crop Zone");
                for (EnvSensor s : z.getEnvSensors()) {
                    w.writeSubHeader("Env: " + s.getMeasureType() + " [" + s.getCode() + "]");
                    w.writeRow("  Last value", String.format("%.2f %s", s.getLastValue(), s.getUnit()));
                    w.writeRow("  Readings",   String.valueOf(s.getReadingHistory().size()));
                }
                for (SoilSensor s : z.getSoilSensors()) {
                    w.writeSubHeader("Soil: " + s.getMeasureType() + " [" + s.getCode() + "]");
                    w.writeRow("  Last value", String.format("%.2f %s", s.getLastValue(), s.getUnit()));
                    w.writeRow("  Readings",   String.valueOf(s.getReadingHistory().size()));
                }
            }

            // Per aqua zone — water sensors
            for (AquacultureZONE z : zoneService.getAquacultureZones()) {
                if (z.getWaterSensors().isEmpty()) continue;
                w.writeSectionHeader(z.getName() + " — Aquaculture Zone");
                for (WaterSensor s : z.getWaterSensors()) {
                    w.writeSubHeader("Water: " + s.getMeasureType() + " [" + s.getCode() + "]");
                    w.writeRow("  Last value", String.format("%.2f %s", s.getLastValue(), s.getUnit()));
                    w.writeRow("  Readings",   String.valueOf(s.getReadingHistory().size()));
                }
            }
            w.save(dest);
        }
    }

    // ── Chart image exports ───────────────────────────────────────────

    /** Export a single numeric sensor card: embedded chart image + data table. */
    public void exportSensorChart(NumericSensor sensor,
                                   List<NumericSensorReading> readings,
                                   BufferedImage chartImage, File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Sensor Report — " + sensor.getCode());
            w.writeSectionTitle("Sensor Details");
            w.writeRow("Code",      sensor.getCode());
            w.writeRow("Type",      sensor.getClass().getSimpleName());
            w.writeRow("Unit",      sensor.getUnit());
            w.writeRow("Threshold", sensor.getMinThreshold() + " – " + sensor.getMaxThreshold() + " " + sensor.getUnit());
            w.writeRow("Last value",String.format("%.4f %s", sensor.getLastValue(), sensor.getUnit()));
            w.writeRow("Status",    sensor.getStatus().name());
            w.writeRow("Readings in range", String.valueOf(readings.size()));

            w.space(10);
            w.writeSectionTitle("Sensor Chart");
            w.addImage(chartImage, PdfWriter.RIGHT - PdfWriter.LEFT, 230);

            if (!readings.isEmpty()) {
                w.writeSectionTitle("Reading History");
                w.writeSubHeader("Timestamp                  Value          Status");
                w.drawDivider();
                for (NumericSensorReading r : readings) {
                    String line = pad(r.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 27)
                        + pad(String.format("%.4f %s", r.getValue(), sensor.getUnit()), 16)
                        + r.getSeverity().name();
                    w.writeMono(line);
                }
            }
            w.save(dest);
        }
    }

    /** Export a single GPS sensor card: embedded chart image + readings summary. */
    public void exportGpsChart(GPSCollarSensor sensor,
                                List<GPSSensorReading> readings,
                                BufferedImage chartImage, File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "GPS Sensor — " + sensor.getAnimal().getName());
            w.writeSectionTitle("GPS Sensor Details");
            w.writeRow("Code",    sensor.getCode());
            w.writeRow("Animal",  sensor.getAnimal().getName());
            w.writeRow("Status",  sensor.getStatus().name());
            w.writeRow("Escaped", sensor.hasEscaped() ? "YES — outside zone boundary" : "No");
            w.writeRow("Readings in range", String.valueOf(readings.size()));

            w.space(10);
            w.writeSectionTitle("Zone Status Chart");
            w.addImage(chartImage, PdfWriter.RIGHT - PdfWriter.LEFT, 230);

            if (!readings.isEmpty()) {
                w.writeSectionTitle("Position Log");
                w.writeSubHeader("Timestamp              Latitude        Longitude       Zone");
                w.drawDivider();
                for (GPSSensorReading r : readings) {
                    String line = pad(r.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 23)
                        + pad(String.format("%.5f", r.getLat()), 16)
                        + pad(String.format("%.5f", r.getLon()), 16)
                        + (r.isInsideZone() ? "INSIDE" : "OUTSIDE");
                    w.writeMono(line);
                }
            }
            w.save(dest);
        }
    }

    /** Export any chart card as a PDF with only the embedded image. */
    public void exportChartImage(String title, BufferedImage chartImage, File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, title);
            w.writeSectionTitle("Chart: " + title);
            w.addImage(chartImage, PdfWriter.RIGHT - PdfWriter.LEFT, 350);
            w.save(dest);
        }
    }

    public void exportFull(File dest) throws IOException {
        try (PdfWriter w = open()) {
            writeDocHeader(w, "Complete Farm Report");
            writeFarmSummarySection(w);
            writeZoneBreakdownSection(w);
            writeAnimalHealthSection(w);
            writeAlertSummarySection(w);

            // Livestock detail
            List<LivestockZONE> lsZones = zoneService.getLivestockZones();
            if (!lsZones.isEmpty()) {
                w.writeSectionTitle("LIVESTOCK");
                for (LivestockZONE z : lsZones) {
                    w.writeSectionHeader(z.getName() + "  [" + z.getType() + "]");
                    w.writeRow("Animals",     String.valueOf(z.getAnimals().size()));
                    w.writeRow("Milk yield",  String.format("%.2f L",  z.getTotalMilkYield()));
                    w.writeRow("Egg count",   String.valueOf(z.getTotalEggCount()));
                }
            }

            // Crops detail
            List<CropZONE> crZones = zoneService.getCropZones();
            if (!crZones.isEmpty()) {
                w.writeSectionTitle("CROPS");
                for (CropZONE z : crZones) {
                    w.writeSectionHeader(z.getName());
                    w.writeRow("Fields",      String.valueOf(z.getFields().size()));
                    w.writeRow("Total yield", String.format("%.2f kg", z.getTotalCropYield()));
                    for (Crop c : z.getFields())
                        w.writeRow("  " + c.getVariety(), c.getGrowthStage().name() + " — " + String.format("%.2f kg", c.getYieldKg()));
                }
            }

            // Aquaculture detail
            List<AquacultureZONE> aqZones = zoneService.getAquacultureZones();
            if (!aqZones.isEmpty()) {
                w.writeSectionTitle("AQUACULTURE");
                for (AquacultureZONE z : aqZones) {
                    w.writeSectionHeader(z.getName());
                    for (AquacultureSpecies sp : z.getSpeciesList())
                        w.writeRow("  " + sp.getName(), sp.getNumSpecies() + " alive  —  " + sp.getTotalHarvestedCount() + " harvested");
                }
            }

            // Alerts summary
            List<Alert> alerts = alertService.getAllAlerts();
            w.writeSectionTitle("ALERTS");
            w.writeRow("Total",    String.valueOf(alerts.size()));
            w.writeRow("Active",   String.valueOf(alerts.stream().filter(Alert::isActive).count()));
            w.writeRow("Critical", String.valueOf(alertService.getAlertsBySeverity(AlertSeverity.Critical).size()));

            w.save(dest);
        }
    }

    // ── Shared section writers ─────────────────────────────────────────

    private void writeDocHeader(PdfWriter w, String section) throws IOException {
        Farm.Farm farm = farmService.getFarm();
        w.writeLine("SMART FARM MANAGEMENT SYSTEM", w.fontBold, 18);
        w.writeLine(section.toUpperCase(), w.fontBold, 13);
        w.drawDivider();
        w.space(4);
        w.writeRow("Farm",      farm.getName());
        w.writeRow("Owner",     farm.getOwnerName());
        w.writeRow("Location",  farm.getLocation().isEmpty() ? "—" : farm.getLocation());
        w.writeRow("Generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy  HH:mm")));
        w.space(8);
        w.drawDivider();
        w.space(10);
        w.writeLine("Note: Charts and graphs are available in the Reports section of the application.",
            w.fontItalic, 9);
        w.space(12);
    }

    private void writeFarmSummarySection(PdfWriter w) throws IOException {
        w.writeSectionTitle("Farm Summary");
        w.writeRow("Total zones",   String.valueOf(zoneService.getAllZones().size()));
        w.writeRow("Livestock zones", String.valueOf(zoneService.getLivestockZones().size()));
        w.writeRow("Crop zones",    String.valueOf(zoneService.getCropZones().size()));
        w.writeRow("Aqua zones",    String.valueOf(zoneService.getAquacultureZones().size()));
        w.writeRow("Total animals", String.valueOf(animalService.getAllAnimals().size()));
        w.writeRow("Total sensors", String.valueOf(sensorService.getAllSensors().size()));
    }

    private void writeZoneBreakdownSection(PdfWriter w) throws IOException {
        w.writeSectionTitle("Zone Breakdown");
        for (LivestockZONE z : zoneService.getLivestockZones())
            w.writeRow("  " + z.getName() + " [Livestock]", z.getAnimals().size() + " animals");
        for (CropZONE z : zoneService.getCropZones())
            w.writeRow("  " + z.getName() + " [Crop]", z.getFields().size() + " fields — " + String.format("%.1f kg", z.getTotalCropYield()));
        for (AquacultureZONE z : zoneService.getAquacultureZones())
            w.writeRow("  " + z.getName() + " [Aquaculture]", z.getSpeciesList().size() + " species");
    }

    private void writeAnimalHealthSection(PdfWriter w) throws IOException {
        List<Animals.Animal> animals = animalService.getAllAnimals();
        w.writeSectionTitle("Animal Health");
        long healthy  = animals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Healthy).count();
        long sick     = animals.stream().filter(a -> a.getHealthStatus() == AnimalHealthStatus.Sick).count();
        long quarant  = animals.stream().filter(Animals.Animal::isQuarantined).count();
        w.writeRow("Healthy",     String.valueOf(healthy));
        w.writeRow("Sick",        String.valueOf(sick));
        w.writeRow("Quarantined", String.valueOf(quarant));
    }

    private void writeAlertSummarySection(PdfWriter w) throws IOException {
        List<Alert> alerts = alertService.getAllAlerts();
        w.writeSectionTitle("Alert Summary");
        w.writeRow("Total",    String.valueOf(alerts.size()));
        w.writeRow("Active",   String.valueOf(alerts.stream().filter(Alert::isActive).count()));
        w.writeRow("Critical", String.valueOf(alertService.getAlertsBySeverity(AlertSeverity.Critical).size()));
        w.writeRow("Warning",  String.valueOf(alertService.getAlertsBySeverity(AlertSeverity.Warning).size()));
        w.writeRow("Resolved", String.valueOf(alerts.stream().filter(Alert::isResolved).count()));
    }

    // ── String helpers ────────────────────────────────────────────────

    /** Strips non-Latin-1 characters so PDType1Font doesn't throw. */
    static String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\t') { sb.append("  "); }
            else if ((c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF)) sb.append(c);
            else sb.append(' ');
        }
        // Collapse multiple spaces
        return sb.toString().replaceAll(" {3,}", "  ").trim();
    }

    private static String pad(String s, int w) {
        s = sanitize(s);
        if (s.length() >= w) return s.substring(0, w - 1) + " ";
        return s + " ".repeat(w - s.length());
    }

    private static String truncStr(String s, int max) {
        s = sanitize(s);
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    // ── PdfWriter factory ──────────────────────────────────────────────

    private PdfWriter open() throws IOException {
        return new PdfWriter();
    }

    // ══════════════════════════════════════════════════════════════════
    // PdfWriter — manages pages and Y-coordinate automatically
    // ══════════════════════════════════════════════════════════════════

    class PdfWriter implements AutoCloseable {

        final PDDocument doc = new PDDocument();

        // Standard Helvetica fonts — always available in any PDF viewer
        final PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font fontBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        final PDType1Font fontMono   = new PDType1Font(Standard14Fonts.FontName.COURIER);

        // A4 page geometry (points: 1 pt = 1/72 inch)
        static final float PAGE_W  = PDRectangle.A4.getWidth();   // 595.28 pt
        static final float PAGE_H  = PDRectangle.A4.getHeight();  // 841.89 pt
        static final float LEFT    = 50;
        static final float RIGHT   = PAGE_W - 50;
        static final float TOP     = PAGE_H - 50;
        static final float BOTTOM  = 65;                           // space for footer

        PDPage                currentPage;
        PDPageContentStream   cs;
        float                 y;
        int                   pageNum = 0;

        PdfWriter() throws IOException { newPage(); }

        // ── Page management ───────────────────────────────────────────

        void newPage() throws IOException {
            if (cs != null) { writeFooter(); cs.close(); }
            currentPage = new PDPage(PDRectangle.A4);
            doc.addPage(currentPage);
            cs = new PDPageContentStream(doc, currentPage);
            y  = TOP;
            pageNum++;
        }

        /** Ensures there are at least `needed` points remaining on the page. */
        void checkBreak(float needed) throws IOException {
            if (y - needed < BOTTOM) newPage();
        }

        // ── Text primitives ───────────────────────────────────────────

        void writeLine(String raw, PDType1Font font, float size) throws IOException {
            writeLine(raw, font, size, LEFT);
        }

        void writeLine(String raw, PDType1Font font, float size, float x) throws IOException {
            checkBreak(size + 4);
            String text = truncToWidth(sanitize(raw), font, size, RIGHT - x - 4);
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            y -= (size + 4);
        }

        /** Key/value row: key at LEFT+10, value at LEFT+230. */
        void writeRow(String key, String value) throws IOException {
            checkBreak(13);
            String k = truncToWidth(sanitize(key),   fontNormal, 10, 210);
            String v = truncToWidth(sanitize(value),  fontBold,   10, RIGHT - LEFT - 240);
            cs.beginText();
            cs.setFont(fontNormal, 10);
            cs.newLineAtOffset(LEFT + 10, y);
            cs.showText(k);
            cs.endText();
            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(LEFT + 230, y);
            cs.showText(v);
            cs.endText();
            y -= 14;
        }

        /** Full-width monospaced line (for columnar alert list). */
        void writeMono(String raw) throws IOException {
            checkBreak(12);
            String text = truncToWidth(sanitize(raw), fontMono, 8, RIGHT - LEFT - 4);
            cs.beginText();
            cs.setFont(fontMono, 8);
            cs.newLineAtOffset(LEFT + 4, y);
            cs.showText(text);
            cs.endText();
            y -= 12;
        }

        // ── Section headings ──────────────────────────────────────────

        /** Large title, e.g. "LIVESTOCK" — new section in the document. */
        void writeSectionTitle(String title) throws IOException {
            checkBreak(36);
            y -= 10;
            writeLine(sanitize(title), fontBold, 13);
            drawLine(LEFT, y + 10, RIGHT, y + 10, 1.5f);
            y -= 4;
        }

        /** Medium heading inside a section (e.g. zone name). */
        void writeSectionHeader(String header) throws IOException {
            checkBreak(28);
            y -= 6;
            writeLine(sanitize(header), fontBold, 11, LEFT + 4);
            drawLine(LEFT + 4, y + 10, RIGHT - 4, y + 10, 0.8f);
            y -= 2;
        }

        /** Small sub-heading inside a zone block. */
        void writeSubHeader(String header) throws IOException {
            checkBreak(20);
            y -= 3;
            writeLine(sanitize(header), fontItalic, 10, LEFT + 10);
        }

        // ── Decorators ────────────────────────────────────────────────

        void drawDivider() throws IOException {
            checkBreak(6);
            drawLine(LEFT, y + 4, RIGHT, y + 4, 1f);
            y -= 6;
        }

        void space(float pts) { y -= pts; }

        /** Embeds a BufferedImage into the PDF, scaled to fit within maxWidth × maxHeight. */
        void addImage(BufferedImage img, float maxWidth, float maxHeight) throws IOException {
            if (img == null) return;
            PDImageXObject pdImg = LosslessFactory.createFromImage(doc, img);
            float iw = pdImg.getWidth();
            float ih = pdImg.getHeight();
            if (iw <= 0 || ih <= 0) return;
            float scale = Math.min(maxWidth / iw, maxHeight / ih);
            float w = iw * scale;
            float h = ih * scale;
            checkBreak(h + 10);
            y -= h;
            cs.drawImage(pdImg, LEFT, y, w, h);
            y -= 6;
        }

        // ── Footer ────────────────────────────────────────────────────

        private void writeFooter() throws IOException {
            drawLine(LEFT, BOTTOM - 4, RIGHT, BOTTOM - 4, 0.4f);
            String left = "Smart Farm Manager  —  " + farmService.getFarmName();
            String right = "Page " + pageNum;
            cs.beginText();
            cs.setFont(fontNormal, 8);
            cs.newLineAtOffset(LEFT, BOTTOM - 16);
            cs.showText(sanitize(left));
            cs.endText();
            float rw = fontNormal.getStringWidth(right) / 1000f * 8;
            cs.beginText();
            cs.setFont(fontNormal, 8);
            cs.newLineAtOffset(RIGHT - rw, BOTTOM - 16);
            cs.showText(right);
            cs.endText();
        }

        // ── Save ──────────────────────────────────────────────────────

        void save(File dest) throws IOException {
            writeFooter();
            cs.close();
            doc.save(dest);
        }

        @Override
        public void close() throws IOException { doc.close(); }

        // ── Internal helpers ──────────────────────────────────────────

        private void drawLine(float x1, float y1, float x2, float y2, float w) throws IOException {
            cs.setLineWidth(w);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private String truncToWidth(String text, PDType1Font font, float size, float maxW)
                throws IOException {
            if (text == null || text.isEmpty()) return "";
            float w = font.getStringWidth(text) / 1000f * size;
            if (w <= maxW) return text;
            while (text.length() > 1) {
                text = text.substring(0, text.length() - 1);
                if (font.getStringWidth(text + "...") / 1000f * size <= maxW) {
                    return text + "...";
                }
            }
            return text;
        }
    }
}
