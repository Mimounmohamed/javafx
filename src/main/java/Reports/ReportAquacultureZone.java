package Reports;

import Alerts.Alert;
import Entities.AquacultureSpecies;
import Sensors.*;
import ZONES.AquacultureZONE;
import java.time.LocalDateTime;
import java.util.List;

public class ReportAquacultureZone extends Report {

    private int totalSpeciesGroups;
    private int totalCurrentIndividuals;  // ← renamed from totalIndividuals
    private int totalInitialIndividuals;  // ← added

    public ReportAquacultureZone(AquacultureZONE zone, ReportType reportType,
                                 LocalDateTime periodStart, LocalDateTime periodEnd,
                                 List<Alert> alerts) {
        super(zone, reportType, periodStart, periodEnd, alerts);
    }

    @Override
    protected void computeSpecificStats(List<Alert> alerts) {
        AquacultureZONE zone = (AquacultureZONE) getZone();
        totalSpeciesGroups      = zone.getSpeciesList().size();
        totalCurrentIndividuals = 0;
        totalInitialIndividuals = 0;

        for (AquacultureSpecies s : zone.getSpeciesList()) {
            totalCurrentIndividuals += s.getNumSpecies();
            totalInitialIndividuals += s.getInitialTotalIndividuals();
        }
    }

    @Override
    protected void computeSensorSummaries() {
        AquacultureZONE zone = (AquacultureZONE) getZone();

        for (WaterSensor sensor : zone.getWaterSensors()) {
            List<SensorReading> history = sensor.getReadingHistory();
            if (history.isEmpty()) continue;
            SensorReading last = history.get(history.size() - 1);
            if (last instanceof NumericSensorReading numeric && numeric.isOutOfThreshold()) {
                addSensorSummary(String.format(
                        "[WaterSensor | %s] %s — %.2f %s (%s)",
                        sensor.getMeasureType(),
                        numeric.getSeverity(),
                        numeric.getValue(),
                        numeric.getUnit(),
                        numeric.getTimestamp()
                ));
            }
        }

        if (getSensorSummaries().isEmpty()) addSensorSummary("All sensors normal");
    }

    @Override
    protected String specificStatsBlock() {
        return  "  SPECIES GROUPS     : " + totalSpeciesGroups + "\n" +
                "  CURRENT INDIVIDUALS: " + totalCurrentIndividuals + "\n" +
                "  INITIAL INDIVIDUALS: " + totalInitialIndividuals + "\n";
    }

    // ─── Getters ──────────────────────────────────────────────────
    public int getTotalSpeciesGroups()      { return totalSpeciesGroups; }
    public int getTotalCurrentIndividuals() { return totalCurrentIndividuals; }
    public int getTotalInitialIndividuals() { return totalInitialIndividuals; }
}