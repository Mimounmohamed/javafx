package Reports;

import Alerts.Alert;
import Alerts.AlertSeverity;
import ZONES.ZONE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

public abstract class Report {

    private String id;
    private ZONE zone;
    private LocalDateTime generatedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private ReportType reportType;
    protected int totalAlerts;
    protected int criticalAlerts;
    protected int warningAlerts;
    protected int resolvedAlerts;
    protected int unresolvedAlerts;

    protected List<String> sensorSummaries;
    protected List<String> notes;

    public Report(ZONE zone, ReportType reportType,
                  LocalDateTime periodStart, LocalDateTime periodEnd,
                  List<Alert> alerts) {
        if (zone == null) throw new IllegalArgumentException("zone cannot be null");
        if (reportType == null) throw new IllegalArgumentException("reportType cannot be null");
        if (periodStart == null) throw new IllegalArgumentException("periodStart cannot be null");
        if (periodEnd == null) throw new IllegalArgumentException("periodEnd cannot be null");
        if (periodEnd.isBefore(periodStart))
            throw new IllegalArgumentException("periodEnd cannot be before periodStart");
        this.id           = UUID.randomUUID().toString();
        this.zone         = zone;
        this.reportType   = reportType;
        this.periodStart  = periodStart;
        this.periodEnd    = periodEnd;
        this.generatedAt  = LocalDateTime.now();
        this.sensorSummaries = new ArrayList<>();
        this.notes           = new ArrayList<>();

        List<Alert> safeAlerts = alerts == null ? Collections.emptyList() : alerts;
        computeAlertStats(safeAlerts);   // shared across all report types
        computeSpecificStats(safeAlerts); // delegated to each subclass
        computeSensorSummaries();     // delegated to each subclass
    }

    protected abstract void computeSpecificStats(List<Alert> alerts);
    protected abstract void computeSensorSummaries();
    protected abstract String specificStatsBlock(); // for toString()

    private void computeAlertStats(List<Alert> alerts) {
        totalAlerts    = alerts.size();
        criticalAlerts = 0;
        warningAlerts  = 0;
        resolvedAlerts = 0;
        unresolvedAlerts = 0;

        for (Alert a : alerts) {
            if (a.getSeverity() == AlertSeverity.Critical) criticalAlerts++;
            if (a.getSeverity() == AlertSeverity.Warning)  warningAlerts++;
            if (a.isResolved() || a.isDismissed())         resolvedAlerts++;
            else                                            unresolvedAlerts++;
        }
    }


    protected void addNote(String note)              { notes.add(note); }
    protected void addSensorSummary(String summary)  { sensorSummaries.add(summary); }

    protected String formatList(List<String> list) {
        if (list.isEmpty()) return "    None\n";
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append("    - ").append(s).append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return  "═══════════════════════════════════════\n" +
                "  REPORT — " + reportType + " | " + getClass().getSimpleName() + "\n" +
                "  Zone     : " + zone.getName() + "\n" +
                "  Period   : " + periodStart + " → " + periodEnd + "\n" +
                "  Generated: " + generatedAt + "\n" +
                "───────────────────────────────────────\n" +
                specificStatsBlock() +
                "───────────────────────────────────────\n" +
                "  ALERTS   : " + totalAlerts + " total\n" +
                "    Critical    : " + criticalAlerts + "\n" +
                "    Warning     : " + warningAlerts + "\n" +
                "    Resolved    : " + resolvedAlerts + "\n" +
                "    Unresolved  : " + unresolvedAlerts + "\n" +
                "───────────────────────────────────────\n" +
                "  SENSOR SUMMARIES:\n" + formatList(sensorSummaries) +
                "  NOTES:\n"            + formatList(notes) +
                "═══════════════════════════════════════";
    }


    public String getId()                      { return id; }
    public ZONE getZone()                      { return zone; }
    public LocalDateTime getGeneratedAt()      { return generatedAt; }
    public LocalDateTime getPeriodStart()      { return periodStart; }
    public LocalDateTime getPeriodEnd()        { return periodEnd; }
    public ReportType getReportType()          { return reportType; }
    public int getTotalAlerts()                { return totalAlerts; }
    public int getCriticalAlerts()             { return criticalAlerts; }
    public int getWarningAlerts()              { return warningAlerts; }
    public int getResolvedAlerts()             { return resolvedAlerts; }
    public int getUnresolvedAlerts()           { return unresolvedAlerts; }
    public List<String> getSensorSummaries()   { return Collections.unmodifiableList(sensorSummaries); }
    public List<String> getNotes()             { return Collections.unmodifiableList(notes); }
}