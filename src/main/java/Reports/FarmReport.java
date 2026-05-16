package Reports;

import Alerts.Alert;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Farm.Farm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FarmReport {

    private final String id;
    private final Farm farm;
    private final ReportType reportType;
    private final LocalDateTime periodStart;
    private final LocalDateTime periodEnd;
    private final LocalDateTime generatedAt;
    private final List<Report> zoneReports;

    // ─── Aggregated alert stats ───────────────────────────────────
    private int totalAlerts;
    private int criticalAlerts;
    private int warningAlerts;
    private int resolvedAlerts;
    private int unresolvedAlerts;
    private int gpsEscapes;

    // ─── Aggregated livestock stats ───────────────────────────────
    private int totalAnimals;
    private int healthyAnimals;
    private int sickAnimals;
    private int quarantinedAnimals;
    private int zonesWithOverdueFeeding;

    // ─── Aggregated crop stats ────────────────────────────────────
    private int totalFields;
    private int harvestReadyFields;
    private int pendingFields;

    // ─── Aggregated aquaculture stats ─────────────────────────────
    private int    totalSpeciesGroups;
    private int    totalCurrentIndividuals;
    private int    totalInitialIndividuals;

    private final List<String> notes;

    // ─── Constructor ──────────────────────────────────────────────
    public FarmReport(Farm farm, ReportType reportType,
                      LocalDateTime periodStart, LocalDateTime periodEnd,
                      List<Alert> alerts, List<Report> zoneReports) {
        this.id           = UUID.randomUUID().toString();
        this.farm         = farm;
        this.reportType   = reportType;
        this.periodStart  = periodStart;
        this.periodEnd    = periodEnd;
        this.generatedAt  = LocalDateTime.now();
        this.zoneReports  = new ArrayList<>(zoneReports);
        this.notes        = new ArrayList<>();

        computeAlertStats(alerts);
        computeFromZoneReports();
    }

    // ─── Computation ──────────────────────────────────────────────

    private void computeAlertStats(List<Alert> alerts) {
        totalAlerts      = alerts.size();
        criticalAlerts   = 0;
        warningAlerts    = 0;
        resolvedAlerts   = 0;
        unresolvedAlerts = 0;
        gpsEscapes       = 0;

        for (Alert a : alerts) {
            if (a.getSeverity() == AlertSeverity.Critical) criticalAlerts++;
            if (a.getSeverity() == AlertSeverity.Warning)  warningAlerts++;
            if (a.isResolved() || a.isDismissed())         resolvedAlerts++;
            else                                            unresolvedAlerts++;
            if (a.getType() == AlertType.GPS_ESCAPE_ALERT) gpsEscapes++;
        }
    }

    private void computeFromZoneReports() {
        totalAnimals           = 0;
        healthyAnimals         = 0;
        sickAnimals            = 0;
        quarantinedAnimals     = 0;
        zonesWithOverdueFeeding = 0;
        totalFields            = 0;
        harvestReadyFields     = 0;
        pendingFields          = 0;
        totalSpeciesGroups     = 0;
        totalCurrentIndividuals = 0;
        totalInitialIndividuals = 0;

        for (Report r : zoneReports) {
            if (r instanceof ReportLiveStockZone lr) {
                totalAnimals       += lr.getTotalAnimals();
                healthyAnimals     += lr.getHealthyAnimals();
                sickAnimals        += lr.getSickAnimals();
                quarantinedAnimals += lr.getQuarantinedAnimals();
                if (!lr.isFeedingOnTime()) zonesWithOverdueFeeding++;

            } else if (r instanceof ReportCropZone cr) {
                totalFields        += cr.getTotalFields();
                harvestReadyFields += cr.getReadyForHarvest();
                pendingFields      += cr.getGrowingSeason(); // Use getGrowingSeason() instead

            } else if (r instanceof ReportAquacultureZone ar) {
                totalSpeciesGroups      += ar.getTotalSpeciesGroups();
                totalCurrentIndividuals += ar.getTotalCurrentIndividuals();
                totalInitialIndividuals += ar.getTotalInitialIndividuals();
            }
        }

        if (sickAnimals > 0)
            notes.add(sickAnimals + " sick animal(s) across all livestock zones");
        if (zonesWithOverdueFeeding > 0)
            notes.add(zonesWithOverdueFeeding + " livestock zone(s) with overdue feeding");
        if (criticalAlerts > 0)
            notes.add(criticalAlerts + " critical alert(s) require immediate attention");
        if (gpsEscapes > 0)
            notes.add(gpsEscapes + " GPS escape(s) detected this period");
    }

    // ─── toString ─────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("  FARM REPORT — ").append(reportType).append("\n");
        sb.append("  Farm    : ").append(farm.getName()).append("\n");
        sb.append("  Owner   : ").append(farm.getOwnerName()).append("\n");
        sb.append("  Period  : ").append(periodStart).append(" → ").append(periodEnd).append("\n");
        sb.append("  Generated: ").append(generatedAt).append("\n");
        sb.append("───────────────────────────────────────\n");

        // livestock block
        if (!farm.getLivestockZones().isEmpty()) {
            sb.append("  LIVESTOCK (").append(farm.getLivestockZones().size()).append(" zones)\n");
            sb.append("    Animals     : ").append(totalAnimals).append(" total\n");
            sb.append("    Healthy     : ").append(healthyAnimals).append("\n");
            sb.append("    Sick        : ").append(sickAnimals).append("\n");
            sb.append("    Quarantined : ").append(quarantinedAnimals).append("\n");
            sb.append("    Overdue feeding zones: ").append(zonesWithOverdueFeeding).append("\n");
            sb.append("    GPS escapes : ").append(gpsEscapes).append("\n");
            sb.append("───────────────────────────────────────\n");
        }

        // crop block
        if (!farm.getCropZones().isEmpty()) {
            sb.append("  CROPS (").append(farm.getCropZones().size()).append(" zones)\n");
            sb.append("    Fields         : ").append(totalFields).append(" total\n");
            sb.append("    Harvest-ready  : ").append(harvestReadyFields).append("\n");
            sb.append("    Pending        : ").append(pendingFields).append("\n");
            sb.append("───────────────────────────────────────\n");
        }

        // aquaculture block
        if (!farm.getAquacultureZones().isEmpty()) {
            sb.append("  AQUACULTURE (").append(farm.getAquacultureZones().size()).append(" zones)\n");
            sb.append("    Species groups      : ").append(totalSpeciesGroups).append("\n");
            sb.append("    Current individuals : ").append(totalCurrentIndividuals).append("\n");
            sb.append("    Initial individuals : ").append(totalInitialIndividuals).append("\n");
            sb.append("───────────────────────────────────────\n");
        }

        // alerts block
        sb.append("  ALERTS  : ").append(totalAlerts).append(" total\n");
        sb.append("    Critical   : ").append(criticalAlerts).append("\n");
        sb.append("    Warning    : ").append(warningAlerts).append("\n");
        sb.append("    Resolved   : ").append(resolvedAlerts).append("\n");
        sb.append("    Unresolved : ").append(unresolvedAlerts).append("\n");
        sb.append("───────────────────────────────────────\n");

        // per-zone summary
        sb.append("  ZONE BREAKDOWN:\n");
        for (Report r : zoneReports) {
            sb.append("    [").append(r.getZone().getCode()).append("] ")
                    .append(r.getZone().getName()).append(" — ")
                    .append(r.getClass().getSimpleName()).append("\n");
        }
        sb.append("───────────────────────────────────────\n");

        // notes
        sb.append("  NOTES:\n");
        if (notes.isEmpty()) {
            sb.append("    None\n");
        } else {
            for (String n : notes) sb.append("    - ").append(n).append("\n");
        }
        sb.append("═══════════════════════════════════════");
        return sb.toString();
    }

    // ─── Getters ──────────────────────────────────────────────────

    public String getId()                    { return id; }
    public Farm getFarm()                    { return farm; }
    public ReportType getReportType()        { return reportType; }
    public LocalDateTime getPeriodStart()    { return periodStart; }
    public LocalDateTime getPeriodEnd()      { return periodEnd; }
    public LocalDateTime getGeneratedAt()    { return generatedAt; }
    public List<Report> getZoneReports()     { return new ArrayList<>(zoneReports); }
    public int getTotalAlerts()              { return totalAlerts; }
    public int getCriticalAlerts()           { return criticalAlerts; }
    public int getWarningAlerts()            { return warningAlerts; }
    public int getResolvedAlerts()           { return resolvedAlerts; }
    public int getUnresolvedAlerts()         { return unresolvedAlerts; }
    public int getGpsEscapes()               { return gpsEscapes; }
    public int getTotalAnimals()             { return totalAnimals; }
    public int getHealthyAnimals()           { return healthyAnimals; }
    public int getSickAnimals()              { return sickAnimals; }
    public int getQuarantinedAnimals()       { return quarantinedAnimals; }
    public int getZonesWithOverdueFeeding()  { return zonesWithOverdueFeeding; }
    public int getTotalFields()              { return totalFields; }
    public int getHarvestReadyFields()       { return harvestReadyFields; }
    public int getTotalSpeciesGroups()       { return totalSpeciesGroups; }
    public int getTotalCurrentIndividuals()  { return totalCurrentIndividuals; }
    public int getPendingFields()            { return pendingFields; }
    public List<String> getNotes()           { return new ArrayList<>(notes); }
}