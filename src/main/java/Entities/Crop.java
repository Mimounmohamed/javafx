package Entities;
import Additional_classes.Range;
import ZONES.GoegraphicBoundries;
import ZONES.ZONE;
import java.util.UUID;
import java.util.Date;

public class Crop {
    private String id;
    private CropType cropType;
    private String variety;
    private Date plantingDate;
    private Date expectedHarvestDate;
    private GrowthStage growthStage;
    private ZONE zone;
    private Range optimalPHRange;
    private Range optimalMoistureRange;
    private double yieldKg;
    private Date harvestDate;
    private GoegraphicBoundries boundary;

    public Crop(CropType cropType, String variety, Date plantingDate, Date expectedHarvestDate,Range optimalPHRange, Range optimalMoistureRange, ZONE zone) {
        if (cropType == null) throw new IllegalArgumentException("cropType cannot be null");
        if (variety == null || variety.isBlank()) throw new IllegalArgumentException("variety cannot be null or blank");
        if (plantingDate == null) throw new IllegalArgumentException("plantingDate cannot be null");
        if (expectedHarvestDate == null) throw new IllegalArgumentException("expectedHarvestDate cannot be null");
        if (expectedHarvestDate.before(plantingDate))
            throw new IllegalArgumentException("expectedHarvestDate cannot be before plantingDate");
        if (optimalPHRange == null) throw new IllegalArgumentException("optimalPHRange cannot be null");
        if (optimalMoistureRange == null) throw new IllegalArgumentException("optimalMoistureRange cannot be null");
        if (zone == null) throw new IllegalArgumentException("zone cannot be null");
        this.id = UUID.randomUUID().toString();
        this.cropType = cropType;
        this.variety = variety;
        this.plantingDate = plantingDate;
        this.expectedHarvestDate = expectedHarvestDate;
        this.growthStage = GrowthStage.sowing;
        this.optimalMoistureRange = optimalMoistureRange;
        this.optimalPHRange = optimalPHRange;
        this.zone = zone;
    }

    public void updateGrowthStage(GrowthStage stage) {
        if (stage == null) throw new IllegalArgumentException("stage cannot be null");
        this.growthStage = stage;
    }

    public boolean isReadyForHarvest() {
        return this.growthStage == GrowthStage.harvest;
    }

    public String getStatusReport() {
        return "Crop: " + variety + " | Stage: " + growthStage + " | Type: " + cropType;
    }

    public String getId() { return id; }
    public CropType getCropType() { return cropType; }
    public String getVariety() { return variety; }
    public Date getPlantingDate() { return plantingDate; }
    public Date getExpectedHarvestDate() { return expectedHarvestDate; }
    public GrowthStage getGrowthStage() { return growthStage; }
    public Range getOptimalPHRange() { return optimalPHRange; }
    public Range getOptimalMoistureRange() { return optimalMoistureRange; }
    public ZONE getZone() { return zone; }

    public void recordHarvest(double kg) {
        if (kg < 0) throw new IllegalArgumentException("kg cannot be negative");
        this.yieldKg = this.yieldKg + kg;
        this.harvestDate = new Date(); // Set harvest date when recording harvest
    }

    public Date getHarvestDate()           { return harvestDate; }
    public double getYieldKg()             { return yieldKg; }
    public boolean wasHarvested()          { return yieldKg > 0; }

    public boolean hasBoundary()           { return boundary != null && boundary.size() >= 3; }
    public GoegraphicBoundries getBoundary(){ return boundary; }
    public void setBoundary(GoegraphicBoundries b) { this.boundary = b; }
}
