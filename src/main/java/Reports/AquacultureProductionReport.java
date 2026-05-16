package Reports;

import Alerts.Alert;
import Entities.AquacultureSpecies;
import Sensors.NumericSensorReading;
import Sensors.SensorReading;
import Sensors.WaterSensor;
import ZONES.AquacultureZONE;
import java.time.LocalDateTime;
import java.util.List;

public class AquacultureProductionReport extends ProductionReport {

    private double totalHarvestWeightKg;
    private int    totalCurrentIndividuals;
    private int    totalInitialIndividuals;
    private int    totalCycleMortality;
    private int    totalOverallMortality;
    private double avgCycleSurvivalRatePercent;
    private double avgOverallSurvivalRatePercent;
    private double waterQualityScore;

    public AquacultureProductionReport(AquacultureZONE zone, ReportType reportType,
                                       LocalDateTime periodStart, LocalDateTime periodEnd) {
        super(zone, reportType, periodStart, periodEnd, new java.util.ArrayList<>());
    }

    @Override
    protected void computeSpecificStats(java.util.List<Alert> alerts) {
        AquacultureZONE zone        = (AquacultureZONE) getZone();
        List<AquacultureSpecies> list = zone.getSpeciesList();

        totalHarvestWeightKg         = 0;
        totalCurrentIndividuals      = 0;
        totalInitialIndividuals      = 0;
        totalCycleMortality          = 0;
        totalOverallMortality        = 0;
        double cycleSurvivalSum      = 0;
        double overallSurvivalSum    = 0;

        for (AquacultureSpecies species : list) {
            totalHarvestWeightKg      += species.getTotalHarvestWeightKg();
            totalCurrentIndividuals   += species.getNumSpecies();
            totalInitialIndividuals   += species.getInitialTotalIndividuals();
            totalCycleMortality       += species.getCycleMortality();
            totalOverallMortality     += species.getTotalMortality();
            cycleSurvivalSum          += species.getCycleSurvivalRatePercent();
            overallSurvivalSum        += species.getOverallSurvivalRatePercent();

            addSensorSummary(String.format(
                    "[%s] harvest: %.2f kg | cycle survival: %.1f%% | " +
                            "cycle mortality: %d | harvests: %d",
                    species.getName(),
                    species.getTotalHarvestWeightKg(),
                    species.getCycleSurvivalRatePercent(),
                    species.getCycleMortality(),
                    species.getHarvestCount()
            ));
        }

        // average survival rates across all species
        int speciesCount = list.size();
        avgCycleSurvivalRatePercent   = speciesCount > 0
                ? cycleSurvivalSum   / speciesCount : 0;
        avgOverallSurvivalRatePercent = speciesCount > 0
                ? overallSurvivalSum / speciesCount : 0;

        // water quality score
        waterQualityScore = computeWaterQualityScore(zone.getWaterSensors());

        // notes
        if (totalHarvestWeightKg == 0)
            addNote("No harvest weight recorded for this period");
        if (avgCycleSurvivalRatePercent < 70)
            addNote(String.format(
                    "Low avg cycle survival rate: %.1f%% — investigate water quality or disease",
                    avgCycleSurvivalRatePercent));
        if (waterQualityScore < 60)
            addNote(String.format(
                    "Poor water quality score: %.0f/100 — review sensor thresholds",
                    waterQualityScore));
    }

    private double computeWaterQualityScore(List<WaterSensor> sensors) {
        int total  = 0;
        int normal = 0;

        for (WaterSensor sensor : sensors) {
            for (SensorReading r : sensor.getReadingHistory()) {
                if (r instanceof NumericSensorReading numeric) {
                    total++;
                    if (!numeric.isOutOfThreshold()) normal++;
                }
            }
        }

        if (total == 0) {
            addNote("No water sensor readings — quality score unavailable");
            return 0;
        }
        return (normal / (double) total) * 100;
    }

    @Override
    protected String specificStatsBlock() {
        return  "  AQUACULTURE PRODUCTION\n" +
                "    Total harvest weight      : " +
                String.format("%.2f", totalHarvestWeightKg) + " kg\n" +
                "    Initial individuals       : " + totalInitialIndividuals + "\n" +
                "    Current individuals       : " + totalCurrentIndividuals + "\n" +
                "    Cycle mortality           : " + totalCycleMortality + "\n" +
                "    Overall mortality         : " + totalOverallMortality + "\n" +
                "    Avg cycle survival        : " +
                String.format("%.1f", avgCycleSurvivalRatePercent) + "%\n" +
                "    Avg overall survival      : " +
                String.format("%.1f", avgOverallSurvivalRatePercent) + "%\n" +
                "    Water quality score       : " +
                String.format("%.0f", waterQualityScore) + "/100\n";
    }

    public double getTotalHarvestWeightKg()       { return totalHarvestWeightKg; }
    public int    getTotalCurrentIndividuals()    { return totalCurrentIndividuals; }
    public int    getTotalInitialIndividuals()    { return totalInitialIndividuals; }
    public int    getTotalCycleMortality()        { return totalCycleMortality; }
    public int    getTotalOverallMortality()      { return totalOverallMortality; }
    public double getAvgCycleSurvivalRate()       { return avgCycleSurvivalRatePercent; }
    public double getAvgOverallSurvivalRate()     { return avgOverallSurvivalRatePercent; }
    public double getWaterQualityScore()          { return waterQualityScore; }
}