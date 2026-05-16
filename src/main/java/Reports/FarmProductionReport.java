package Reports;

import Entities.CropType;
import Farm.Farm;
import ZONES.CropZONE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FarmProductionReport {

    private final String id;
    private final Farm farm;
    private final ReportType reportType;
    private final LocalDateTime periodStart;
    private final LocalDateTime periodEnd;
    private final LocalDateTime generatedAt;
    private final List<ProductionReport> zoneProductionReports;

    // ─── Aggregated livestock production ──────────────────────────
    private double totalMilkYieldLiters;
    private int    totalEggCount;
    private int    totalMilkProducingAnimals;
    private int    totalEggLayingAnimals;

    // ─── Aggregated crop production ───────────────────────────────
    private double totalCropYieldKg;
    private double totalYieldPerHectare;
    private int    totalHarvestedFields;
    private int    totalPendingFields;
    private final Map<CropType, Double> totalYieldByCropType;

    // ─── Aggregated aquaculture production ────────────────────────
    private double totalHarvestWeightKg;
    private int    totalCurrentIndividuals;
    private int    totalInitialIndividuals;
    private int    totalCycleMortality;
    private int    totalOverallMortality;
    private double avgCycleSurvivalRate;
    private double avgWaterQualityScore;

    private final List<String> notes;

    // ─── Constructor ──────────────────────────────────────────────
    public FarmProductionReport(Farm farm, ReportType reportType,
                                LocalDateTime periodStart, LocalDateTime periodEnd,
                                List<ProductionReport> zoneProductionReports) {
        this.id                   = UUID.randomUUID().toString();
        this.farm                 = farm;
        this.reportType           = reportType;
        this.periodStart          = periodStart;
        this.periodEnd            = periodEnd;
        this.generatedAt          = LocalDateTime.now();
        this.zoneProductionReports = new ArrayList<>(zoneProductionReports);
        this.totalYieldByCropType  = new HashMap<>();
        this.notes                 = new ArrayList<>();

        computeFromZoneProductionReports();
    }

    // ─── Computation ──────────────────────────────────────────────

    private void computeFromZoneProductionReports() {
        totalMilkYieldLiters         = 0;
        totalEggCount                = 0;
        totalMilkProducingAnimals    = 0;
        totalEggLayingAnimals        = 0;

        totalCropYieldKg             = 0;
        totalHarvestedFields         = 0;
        totalPendingFields           = 0;

        totalHarvestWeightKg         = 0;
        totalCurrentIndividuals      = 0;
        totalInitialIndividuals      = 0;
        totalCycleMortality          = 0;
        totalOverallMortality        = 0;

        double cycleSurvivalSum      = 0;
        double waterQualitySum       = 0;
        int    aquaZoneCount         = 0;
        double totalSurface          = 0;
        int    cropZoneCount         = 0;

        for (ProductionReport pr : zoneProductionReports) {

            if (pr instanceof LivestockProductionReport lr) {
                totalMilkYieldLiters      += lr.getTotalMilkYieldLiters();
                totalEggCount             += lr.getTotalEggCount();
                totalMilkProducingAnimals += lr.getMilkProducingAnimals();
                totalEggLayingAnimals     += lr.getEggLayingAnimals();

            } else if (pr instanceof CropProductionReport cr) {
                totalCropYieldKg     += cr.getTotalYieldKg();
                totalHarvestedFields += cr.getHarvestedFieldsCount();
                totalPendingFields   += cr.getPendingFieldsCount();
                // merge yield by crop type
                cr.getYieldByCropType().forEach((type, kg) ->
                        totalYieldByCropType.merge(type, kg, Double::sum));
                // accumulate surface for overall yield/ha
                CropZONE cz = (CropZONE) cr.getZone();
                totalSurface += cz.getSurfacePlanted();
                cropZoneCount++;

            } else if (pr instanceof AquacultureProductionReport ar) {
                totalHarvestWeightKg    += ar.getTotalHarvestWeightKg();
                totalCurrentIndividuals += ar.getTotalCurrentIndividuals();
                totalInitialIndividuals += ar.getTotalInitialIndividuals();
                totalCycleMortality     += ar.getTotalCycleMortality();
                totalOverallMortality   += ar.getTotalOverallMortality();
                cycleSurvivalSum        += ar.getAvgCycleSurvivalRate();
                waterQualitySum         += ar.getWaterQualityScore();
                aquaZoneCount++;
            }
        }

        // derived
        totalYieldPerHectare = totalSurface > 0
                ? totalCropYieldKg / totalSurface : 0;
        avgCycleSurvivalRate = aquaZoneCount > 0
                ? cycleSurvivalSum / aquaZoneCount : 0;
        avgWaterQualityScore = aquaZoneCount > 0
                ? waterQualitySum  / aquaZoneCount : 0;

        // farm-level notes
        if (totalMilkYieldLiters == 0 && !farm.getLivestockZones().isEmpty())
            notes.add("No milk yield recorded across livestock zones");
        if (totalCropYieldKg == 0 && !farm.getCropZones().isEmpty())
            notes.add("No crop yield recorded — check harvest status");
        if (avgCycleSurvivalRate < 70 && aquaZoneCount > 0)
            notes.add(String.format(
                    "Low avg aquaculture survival: %.1f%% — review water quality",
                    avgCycleSurvivalRate));
        if (avgWaterQualityScore < 60 && aquaZoneCount > 0)
            notes.add(String.format(
                    "Poor avg water quality score: %.0f/100",
                    avgWaterQualityScore));
    }

    // ─── toString ─────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("  FARM PRODUCTION REPORT — ").append(reportType).append("\n");
        sb.append("  Farm    : ").append(farm.getName()).append("\n");
        sb.append("  Owner   : ").append(farm.getOwnerName()).append("\n");
        sb.append("  Period  : ").append(periodStart).append(" → ").append(periodEnd).append("\n");
        sb.append("  Generated: ").append(generatedAt).append("\n");
        sb.append("───────────────────────────────────────\n");

        // livestock production
        if (!farm.getLivestockZones().isEmpty()) {
            sb.append("  LIVESTOCK PRODUCTION\n");
            sb.append("    Total milk yield      : ")
                    .append(String.format("%.2f", totalMilkYieldLiters)).append(" L\n");
            sb.append("    Producing animals     : ").append(totalMilkProducingAnimals).append("\n");
            sb.append("    Total egg count       : ").append(totalEggCount).append("\n");
            sb.append("    Laying animals        : ").append(totalEggLayingAnimals).append("\n");
            sb.append("───────────────────────────────────────\n");
        }

        // crop production
        if (!farm.getCropZones().isEmpty()) {
            sb.append("  CROP PRODUCTION\n");
            sb.append("    Total yield           : ")
                    .append(String.format("%.2f", totalCropYieldKg)).append(" kg\n");
            sb.append("    Yield per hectare     : ")
                    .append(String.format("%.2f", totalYieldPerHectare)).append(" kg/ha\n");
            sb.append("    Harvested fields      : ").append(totalHarvestedFields).append("\n");
            sb.append("    Pending fields        : ").append(totalPendingFields).append("\n");
            if (!totalYieldByCropType.isEmpty()) {
                sb.append("    By crop type:\n");
                totalYieldByCropType.forEach((type, kg) ->
                        sb.append(String.format("      %-20s: %.2f kg%n", type, kg)));
            }
            sb.append("───────────────────────────────────────\n");
        }

        // aquaculture production
        if (!farm.getAquacultureZones().isEmpty()) {
            sb.append("  AQUACULTURE PRODUCTION\n");
            sb.append("    Total harvest weight  : ")
                    .append(String.format("%.2f", totalHarvestWeightKg)).append(" kg\n");
            sb.append("    Initial individuals   : ").append(totalInitialIndividuals).append("\n");
            sb.append("    Current individuals   : ").append(totalCurrentIndividuals).append("\n");
            sb.append("    Cycle mortality       : ").append(totalCycleMortality).append("\n");
            sb.append("    Overall mortality     : ").append(totalOverallMortality).append("\n");
            sb.append("    Avg cycle survival    : ")
                    .append(String.format("%.1f", avgCycleSurvivalRate)).append("%\n");
            sb.append("    Avg water quality     : ")
                    .append(String.format("%.0f", avgWaterQualityScore)).append("/100\n");
            sb.append("───────────────────────────────────────\n");
        }

        // per-zone summary
        sb.append("  ZONE BREAKDOWN:\n");
        for (ProductionReport pr : zoneProductionReports) {
            sb.append("    [").append(pr.getZone().getCode()).append("] ")
                    .append(pr.getZone().getName()).append(" — ")
                    .append(pr.getClass().getSimpleName()).append("\n");
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

    public String getId()                               { return id; }
    public Farm getFarm()                               { return farm; }
    public ReportType getReportType()                   { return reportType; }
    public LocalDateTime getPeriodStart()               { return periodStart; }
    public LocalDateTime getPeriodEnd()                 { return periodEnd; }
    public LocalDateTime getGeneratedAt()               { return generatedAt; }
    public List<ProductionReport> getZoneProductionReports() { return new ArrayList<>(zoneProductionReports); }
    public double getTotalMilkYieldLiters()             { return totalMilkYieldLiters; }
    public int    getTotalEggCount()                    { return totalEggCount; }
    public int    getTotalMilkProducingAnimals()        { return totalMilkProducingAnimals; }
    public int    getTotalEggLayingAnimals()            { return totalEggLayingAnimals; }
    public double getTotalCropYieldKg()                 { return totalCropYieldKg; }
    public double getTotalYieldPerHectare()             { return totalYieldPerHectare; }
    public int    getTotalHarvestedFields()             { return totalHarvestedFields; }
    public int    getTotalPendingFields()               { return totalPendingFields; }
    public Map<CropType, Double> getTotalYieldByCropType() { return new HashMap<>(totalYieldByCropType); }
    public double getTotalHarvestWeightKg()             { return totalHarvestWeightKg; }
    public int    getTotalCurrentIndividuals()          { return totalCurrentIndividuals; }
    public int    getTotalInitialIndividuals()          { return totalInitialIndividuals; }
    public int    getTotalCycleMortality()              { return totalCycleMortality; }
    public int    getTotalOverallMortality()            { return totalOverallMortality; }
    public double getAvgCycleSurvivalRate()             { return avgCycleSurvivalRate; }
    public double getAvgWaterQualityScore()             { return avgWaterQualityScore; }
    public List<String> getNotes()                      { return new ArrayList<>(notes); }
}