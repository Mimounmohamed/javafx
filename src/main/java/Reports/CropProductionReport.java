package Reports;

import Alerts.Alert;
import Entities.Crop;
import Entities.CropType;
import ZONES.CropZONE;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CropProductionReport extends ProductionReport {

    private double totalYieldKg;
    private int    harvestedFieldsCount;
    private int    pendingFieldsCount;
    private double yieldPerHectare;
    private Map<CropType, Double> yieldByCropType;

    public CropProductionReport(CropZONE zone, ReportType reportType,
                                LocalDateTime periodStart, LocalDateTime periodEnd) {
        super(zone, reportType, periodStart, periodEnd, new java.util.ArrayList<>());
    }

    @Override
    protected void computeSpecificStats(java.util.List<Alert> alerts) {
        CropZONE zone = (CropZONE) getZone();

        totalYieldKg         = 0;
        harvestedFieldsCount = 0;
        pendingFieldsCount   = 0;
        yieldByCropType      = new HashMap<>();

        for (Crop crop : zone.getFields()) {
            // Ensure the crop's harvest date is within the report period.
            if (crop.wasHarvested() && crop.getHarvestDate() != null &&
                !crop.getHarvestDate().toInstant().isBefore(getPeriodStart().toInstant(java.time.ZoneOffset.UTC)) &&
                !crop.getHarvestDate().toInstant().isAfter(getPeriodEnd().toInstant(java.time.ZoneOffset.UTC))) {
                double kg = crop.getYieldKg();
                totalYieldKg += kg;
                harvestedFieldsCount++;
                yieldByCropType.merge(crop.getCropType(), kg, Double::sum);
                addSensorSummary(String.format("[%s] %s — %.2f kg",
                        crop.getCropType(), crop.getVariety(), kg));
            } else {
                pendingFieldsCount++;
            }
        }

        double surface = zone.getSurfacePlanted();
        if (surface > 0) {
            yieldPerHectare = totalYieldKg / surface;
        } else {
            yieldPerHectare = 0;
            addNote("Surface planted not set — yield per hectare unavailable");
        }

        if (harvestedFieldsCount == 0)
            addNote("No fields harvested in this period");
        if (pendingFieldsCount > 0)
            addNote(pendingFieldsCount + " field(s) not yet harvested");
    }

    @Override
    protected String specificStatsBlock() {
        StringBuilder byType = new StringBuilder();
        if (yieldByCropType.isEmpty()) {
            byType.append("    None\n");
        } else {
            for (Map.Entry<CropType, Double> e : yieldByCropType.entrySet())
                byType.append(String.format("    %-20s: %.2f kg%n",
                        e.getKey(), e.getValue()));
        }

        return  "  CROP PRODUCTION\n" +
                "    Total yield       : " + String.format("%.2f", totalYieldKg) + " kg\n" +
                "    Harvested fields  : " + harvestedFieldsCount + "\n" +
                "    Pending fields    : " + pendingFieldsCount + "\n" +
                "    Yield / hectare   : " + String.format("%.2f", yieldPerHectare) + " kg/ha\n" +
                "  YIELD BY CROP TYPE:\n" + byType;
    }

    public double getTotalYieldKg()                   { return totalYieldKg; }
    public int    getHarvestedFieldsCount()           { return harvestedFieldsCount; }
    public int    getPendingFieldsCount()             { return pendingFieldsCount; }
    public double getYieldPerHectare()                { return yieldPerHectare; }
    public Map<CropType, Double> getYieldByCropType() { return yieldByCropType; }
}