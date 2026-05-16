package Reports;

import Alerts.Alert;
import Entities.Crop;
import Entities.GrowthStage;
import Sensors.*;
import ZONES.CropZONE;
import java.time.LocalDateTime;
import java.util.List;

public class ReportCropZone extends Report {

    private int totalFields;
    private int readyForHarvest;
    private int growingSeason;
    private String dominantGrowthStage;

    public ReportCropZone(CropZONE zone, ReportType reportType,
                          LocalDateTime periodStart, LocalDateTime periodEnd,
                          List<Alert> alerts) {
        super(zone, reportType, periodStart, periodEnd, alerts);
    }

    @Override
    protected void computeSpecificStats(List<Alert> alerts) {
        CropZONE zone = (CropZONE) getZone();
        List<Crop> fields = zone.getFields();

        totalFields     = fields.size();
        readyForHarvest = 0;
        growingSeason   = 0;

        for (Crop c : fields) {
            if (c.isReadyForHarvest()) readyForHarvest++;
            else                       growingSeason++;
        }

        dominantGrowthStage = findDominantStage(fields);
    }

    private String findDominantStage(List<Crop> fields) {
        if (fields.isEmpty()) return "N/A";
        java.util.Map<GrowthStage, Integer> counts = new java.util.HashMap<>();
        for (Crop c : fields)
            counts.merge(c.getGrowthStage(), 1, Integer::sum);
        return counts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    @Override
    protected void computeSensorSummaries() {
        CropZONE zone = (CropZONE) getZone();

        for (EnvSensor sensor : zone.getEnvSensors()) {
            List<SensorReading> history = sensor.getReadingHistory();
            if (history.isEmpty()) continue;
            SensorReading last = history.get(history.size() - 1);
            if (last instanceof NumericSensorReading numeric && numeric.isOutOfThreshold()) {
                addSensorSummary(String.format(
                        "[EnvSensor | %s] %s — %.2f %s (%s)",
                        sensor.getMeasureType(),
                        numeric.getSeverity(),
                        numeric.getValue(),
                        numeric.getUnit(),
                        numeric.getTimestamp()
                ));
            }
        }

        for (SoilSensor sensor : zone.getSoilSensors()) {
            List<SensorReading> history = sensor.getReadingHistory();
            if (history.isEmpty()) continue;
            SensorReading last = history.get(history.size() - 1);
            if (last instanceof NumericSensorReading numeric && numeric.isOutOfThreshold()) {
                addSensorSummary(String.format(
                        "[SoilSensor | %s] %s — %.2f %s (%s)",
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
        return  "  FIELDS   : " + totalFields + " total\n" +
                "    Ready for harvest : " + readyForHarvest + "\n" +
                "    Growing           : " + growingSeason + "\n" +
                "    Dominant stage    : " + dominantGrowthStage + "\n";
    }

    // ─── Getters ──────────────────────────────────────────────────
    public int getTotalFields()            { return totalFields; }
    public int getReadyForHarvest()        { return readyForHarvest; }
    public int getPendingFieldsCount()     { return growingSeason; } // ← was getGrowingSeason()
    public int getGrowingSeason()          { return growingSeason; }
    public String getDominantGrowthStage() { return dominantGrowthStage; }
}