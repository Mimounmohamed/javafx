package Farm;

import Alerts.Alert;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Reports.*;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Farm {

    // ─── Identity ─────────────────────────────────────────────────
    private final String id;
    private String name;
    private String location;
    private String ownerName;
    private final LocalDateTime createdAt;

    // ─── Zone registry ────────────────────────────────────────────
    private final List<LivestockZONE>   livestockZones;
    private final List<CropZONE>        cropZones;
    private final List<AquacultureZONE> aquacultureZones;

    // ─── Alert registry ───────────────────────────────────────────
    private final List<Alert> alerts;

    // ─── Report history ───────────────────────────────────────────
    private final List<Report>               zoneReports;
    private final List<ProductionReport>     zoneProductionReports;
    private final List<FarmReport>           farmReports;
    private final List<FarmProductionReport> farmProductionReports;

    // ─── Constructor ──────────────────────────────────────────────
    public Farm(String name, String location, String ownerName) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Farm name cannot be blank");
        if (ownerName == null || ownerName.isBlank())
            throw new IllegalArgumentException("Owner name cannot be blank");

        this.id               = UUID.randomUUID().toString();
        this.name             = name;
        this.location         = location;
        this.ownerName        = ownerName;
        this.createdAt        = LocalDateTime.now();

        this.livestockZones   = new ArrayList<>();
        this.cropZones        = new ArrayList<>();
        this.aquacultureZones = new ArrayList<>();

        this.alerts                = new ArrayList<>();
        this.zoneReports           = new ArrayList<>();
        this.zoneProductionReports = new ArrayList<>();
        this.farmReports           = new ArrayList<>();
        this.farmProductionReports = new ArrayList<>();
    }

    // ─── Zone management ──────────────────────────────────────────

    public void addZone(ZONE zone) {
        if (zone == null)
            throw new IllegalArgumentException("Zone cannot be null");
        if (zone instanceof LivestockZONE z)        livestockZones.add(z);
        else if (zone instanceof CropZONE z)        cropZones.add(z);
        else if (zone instanceof AquacultureZONE z) aquacultureZones.add(z);
        else throw new IllegalArgumentException("Unknown zone type: " + zone.getClass());
    }

    public void removeZone(ZONE zone) {
        if (zone instanceof LivestockZONE z)        livestockZones.remove(z);
        else if (zone instanceof CropZONE z)        cropZones.remove(z);
        else if (zone instanceof AquacultureZONE z) aquacultureZones.remove(z);
    }

    public ZONE getZoneByCode(String code) {
        if (code == null) return null;
        return getAllZones().stream()
                .filter(z -> z.getCode().equals(code.trim()))
                .findFirst().orElse(null);
    }

    public ZONE getZoneByName(String name) {
        if (name == null) return null;
        return getAllZones().stream()
                .filter(z -> z.getName().equalsIgnoreCase(name.trim()))
                .findFirst().orElse(null);
    }

    public List<ZONE> getZonesByType(Class<? extends ZONE> type) {
        return getAllZones().stream()
                .filter(type::isInstance)
                .collect(Collectors.toList());
    }

    public List<ZONE> getAllZones() {
        List<ZONE> all = new ArrayList<>();
        all.addAll(livestockZones);
        all.addAll(cropZones);
        all.addAll(aquacultureZones);
        return Collections.unmodifiableList(all);
    }

    // ─── Alert management ─────────────────────────────────────────

    public void registerAlert(Alert alert) {
        if (alert == null)
            throw new IllegalArgumentException("Alert cannot be null");
        alerts.add(alert);
    }

    public List<Alert> getAllAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    public List<Alert> getActiveAlerts() {
        return alerts.stream()
                .filter(a -> a.isActive() || a.isAcknowledged())
                .collect(Collectors.toList());
    }

    public List<Alert> getAlertsByType(AlertType type) {
        return alerts.stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
        return alerts.stream()
                .filter(a -> a.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    public List<Alert> getAlertsByZonePeriod(LocalDateTime start, LocalDateTime end) {
        return alerts.stream()
                .filter(a -> !a.getTimestamp().isBefore(start)
                        && !a.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // ─── Per-zone report generation ───────────────────────────────

    public Report generateReportForZone(ZONE zone, ReportType type,
                                        LocalDateTime start, LocalDateTime end) {
        if (zone == null)
            throw new IllegalArgumentException("Zone cannot be null");
        List<Alert> periodAlerts = getAlertsByZonePeriod(start, end);
        Report r;
        if (zone instanceof LivestockZONE z)
            r = new ReportLiveStockZone(z, type, start, end, periodAlerts);
        else if (zone instanceof CropZONE z)
            r = new ReportCropZone(z, type, start, end, periodAlerts);
        else if (zone instanceof AquacultureZONE z)
            r = new ReportAquacultureZone(z, type, start, end, periodAlerts);
        else
            throw new IllegalArgumentException("Unknown zone type: " + zone.getClass());
        zoneReports.add(r);
        return r;
    }

    public ProductionReport generateProductionReportForZone(ZONE zone, ReportType type,
                                                            LocalDateTime start,
                                                            LocalDateTime end) {
        if (zone == null)
            throw new IllegalArgumentException("Zone cannot be null");
        ProductionReport r;
        if (zone instanceof LivestockZONE z)
            r = new LivestockProductionReport(z, type, start, end);
        else if (zone instanceof CropZONE z)
            r = new CropProductionReport(z, type, start, end);
        else if (zone instanceof AquacultureZONE z)
            r = new AquacultureProductionReport(z, type, start, end);
        else
            throw new IllegalArgumentException("Unknown zone type: " + zone.getClass());
        zoneProductionReports.add(r);
        return r;
    }

    // ─── Zone lookup + report convenience wrappers ────────────────

    public Report generateReportForZoneByCode(String code, ReportType type,
                                              LocalDateTime start, LocalDateTime end) {
        ZONE zone = getZoneByCode(code);
        if (zone == null)
            throw new IllegalArgumentException("No zone found with code: " + code);
        return generateReportForZone(zone, type, start, end);
    }

    public Report generateReportForZoneByName(String name, ReportType type,
                                              LocalDateTime start, LocalDateTime end) {
        ZONE zone = getZoneByName(name);
        if (zone == null)
            throw new IllegalArgumentException("No zone found with name: " + name);
        return generateReportForZone(zone, type, start, end);
    }

    public ProductionReport generateProductionReportForZoneByCode(String code, ReportType type,
                                                                  LocalDateTime start,
                                                                  LocalDateTime end) {
        ZONE zone = getZoneByCode(code);
        if (zone == null)
            throw new IllegalArgumentException("No zone found with code: " + code);
        return generateProductionReportForZone(zone, type, start, end);
    }

    public ProductionReport generateProductionReportForZoneByName(String name, ReportType type,
                                                                  LocalDateTime start,
                                                                  LocalDateTime end) {
        ZONE zone = getZoneByName(name);
        if (zone == null)
            throw new IllegalArgumentException("No zone found with name: " + name);
        return generateProductionReportForZone(zone, type, start, end);
    }

    // ─── All zones report generation ──────────────────────────────

    public List<Report> generateAllZoneReports(ReportType type,
                                               LocalDateTime start,
                                               LocalDateTime end) {
        List<Report> generated = new ArrayList<>();
        getAllZones().forEach(z -> generated.add(
                generateReportForZone(z, type, start, end)));
        return generated;
    }

    public List<ProductionReport> generateAllZoneProductionReports(ReportType type,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end) {
        List<ProductionReport> generated = new ArrayList<>();
        getAllZones().forEach(z -> generated.add(
                generateProductionReportForZone(z, type, start, end)));
        return generated;
    }

    // ─── Farm-level consolidated reports ──────────────────────────

    public FarmReport generateFarmReport(ReportType type,
                                         LocalDateTime start,
                                         LocalDateTime end) {
        List<Alert>  periodAlerts   = getAlertsByZonePeriod(start, end);
        List<Report> zoneReportList = generateAllZoneReports(type, start, end);
        FarmReport r = new FarmReport(this, type, start, end, periodAlerts, zoneReportList);
        farmReports.add(r);
        return r;
    }

    public FarmProductionReport generateFarmProductionReport(ReportType type,
                                                             LocalDateTime start,
                                                             LocalDateTime end) {
        List<ProductionReport> zoneList = generateAllZoneProductionReports(type, start, end);
        FarmProductionReport r = new FarmProductionReport(this, type, start, end, zoneList);
        farmProductionReports.add(r);
        return r;
    }

    // ─── Farm stats ───────────────────────────────────────────────

    public FarmStats getStats() {
        int totalAnimals = livestockZones.stream()
                .mapToInt(z -> z.getAnimals().size()).sum();
        int sickAnimals  = livestockZones.stream()
                .mapToInt(z -> (int) z.getAnimals().stream()
                        .filter(a -> a.isSick()).count()).sum();
        int totalFields  = cropZones.stream()
                .mapToInt(z -> z.getFields().size()).sum();
        int totalSpecies = aquacultureZones.stream()
                .mapToInt(z -> z.getSpeciesList().size()).sum();
        int activeAlerts   = getActiveAlerts().size();
        int criticalAlerts = getAlertsBySeverity(AlertSeverity.Critical).size();

        return new FarmStats(
                getTotalZoneCount(),
                totalAnimals, sickAnimals,
                totalFields,
                totalSpecies,
                alerts.size(), activeAlerts, criticalAlerts
        );
    }

    public int getTotalZoneCount() {
        return livestockZones.size() + cropZones.size() + aquacultureZones.size();
    }

    // ─── Summary ──────────────────────────────────────────────────

    public String getSummary() {
        FarmStats s = getStats();
        return String.format(
                "Farm: %s | Owner: %s | Location: %s | " +
                        "Zones: %d (L:%d C:%d A:%d) | " +
                        "Animals: %d (%d sick) | Fields: %d | Species: %d | " +
                        "Active alerts: %d (%d critical)",
                name, ownerName, location,
                s.totalZones,
                livestockZones.size(), cropZones.size(), aquacultureZones.size(),
                s.totalAnimals, s.sickAnimals,
                s.totalFields, s.totalSpecies,
                s.activeAlerts, s.criticalAlerts
        );
    }

    @Override
    public String toString() {
        return  "═══════════════════════════════════════\n" +
                "  FARM: " + name + "\n" +
                "  Owner   : " + ownerName + "\n" +
                "  Location: " + location + "\n" +
                "  Created : " + createdAt + "\n" +
                "───────────────────────────────────────\n" +
                "  ZONES   : " + getTotalZoneCount() + "\n" +
                "    Livestock   : " + livestockZones.size() + "\n" +
                "    Crop        : " + cropZones.size() + "\n" +
                "    Aquaculture : " + aquacultureZones.size() + "\n" +
                "───────────────────────────────────────\n" +
                "  ALERTS  : " + alerts.size() + " total | " +
                getActiveAlerts().size() + " active\n" +
                "═══════════════════════════════════════";
    }

    // ─── Getters / Setters ────────────────────────────────────────

    public String getId()               { return id; }
    public String getName()             { return name; }
    public String getLocation()         { return location; }
    public String getOwnerName()        { return ownerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");
        this.name = name;
    }
    public void setLocation(String location)   { this.location = location; }
    public void setOwnerName(String ownerName) {
        if (ownerName == null || ownerName.isBlank())
            throw new IllegalArgumentException("Owner cannot be blank");
        this.ownerName = ownerName;
    }

    public List<LivestockZONE>   getLivestockZones()   { return Collections.unmodifiableList(livestockZones); }
    public List<CropZONE>        getCropZones()        { return Collections.unmodifiableList(cropZones); }
    public List<AquacultureZONE> getAquacultureZones() { return Collections.unmodifiableList(aquacultureZones); }

    public List<Report>               getZoneReports()            { return Collections.unmodifiableList(zoneReports); }
    public List<ProductionReport>     getZoneProductionReports()  { return Collections.unmodifiableList(zoneProductionReports); }
    public List<FarmReport>           getFarmReports()            { return Collections.unmodifiableList(farmReports); }
    public List<FarmProductionReport> getFarmProductionReports()  { return Collections.unmodifiableList(farmProductionReports); }

    // ─── FarmStats inner class ────────────────────────────────────

    public static class FarmStats {
        public final int totalZones;
        public final int totalAnimals;
        public final int sickAnimals;
        public final int totalFields;
        public final int totalSpecies;
        public final int totalAlerts;
        public final int activeAlerts;
        public final int criticalAlerts;

        public FarmStats(int totalZones, int totalAnimals, int sickAnimals,
                         int totalFields, int totalSpecies,
                         int totalAlerts, int activeAlerts, int criticalAlerts) {
            this.totalZones     = totalZones;
            this.totalAnimals   = totalAnimals;
            this.sickAnimals    = sickAnimals;
            this.totalFields    = totalFields;
            this.totalSpecies   = totalSpecies;
            this.totalAlerts    = totalAlerts;
            this.activeAlerts   = activeAlerts;
            this.criticalAlerts = criticalAlerts;
        }
    }
}

