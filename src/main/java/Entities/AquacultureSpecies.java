package Entities;

import ZONES.GoegraphicBoundries;
import ZONES.ZONE;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AquacultureSpecies {
    public static class HarvestRecord {
        private final double weightKg;
        private final int countHarvested;
        private final int countBefore;
        private final LocalDateTime date;

        HarvestRecord(double weightKg, int countHarvested, int countBefore) {
            if (weightKg < 0)
                throw new IllegalArgumentException("Harvest weight cannot be negative");
            if (countHarvested < 0)
                throw new IllegalArgumentException("Count harvested cannot be negative");
            if (countBefore < 0)
                throw new IllegalArgumentException("countBefore cannot be negative");
            if (countHarvested > countBefore)
                throw new IllegalArgumentException(
                        "countHarvested " + countHarvested +
                                " cannot exceed countBefore " + countBefore);
            this.weightKg       = weightKg;
            this.countHarvested = countHarvested;
            this.countBefore    = countBefore;
            this.date           = LocalDateTime.now();
        }

        public double getCycleSurvivalRatePercent() {
            if (countBefore == 0) return 0;
            return ((countBefore - countHarvested) / (double) countBefore) * 100;
        }

        public double getWeightKg()    { return weightKg; }
        public int getCountHarvested() { return countHarvested; }
        public int getCountBefore()    { return countBefore; }
        public LocalDateTime getDate() { return date; }

        @Override
        public String toString() {
            return String.format("[%s] harvested %d / %d individuals — %.2f kg",
                    date, countHarvested, countBefore, weightKg);
        }
    }

    private final String id;
    private final String name;
    private GoegraphicBoundries boundary;
    private final int    initialTotalIndividuals; // original stock at creation, never changes
    private       int    cycleBaseline;           // starting count of the current cycle, resets on restock
    private       int    numSpecies;              // current live count, decreases on harvest or mortality
    private final ZONE   zone;
    private final List<HarvestRecord> harvestHistory;
    private       int    cycleStartHarvestIndex; // points to the first harvest of the current cycle in harvestHistory

    public AquacultureSpecies(String name, int numSpecies, ZONE zone) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Species name cannot be null or blank");
        if (zone == null)
            throw new IllegalArgumentException("Zone cannot be null");
        if (numSpecies < 0)
            throw new IllegalArgumentException("numSpecies cannot be negative");

        this.id                      = UUID.randomUUID().toString();
        this.name                    = name;
        this.numSpecies              = numSpecies;
        this.initialTotalIndividuals = numSpecies; // fixed forever
        this.cycleBaseline           = numSpecies; // starts equal, resets on restock
        this.zone                    = zone;
        this.harvestHistory          = new ArrayList<>();
        this.cycleStartHarvestIndex  = 0;
    }


    public void harvest(double kg, int countHarvested) {
        if (countHarvested < 0)
            throw new IllegalArgumentException("Count harvested cannot be negative");
        if (countHarvested > numSpecies)
            throw new IllegalArgumentException(
                    "Cannot harvest " + countHarvested +
                            " — only " + numSpecies + " remaining");

        harvestHistory.add(new HarvestRecord(kg, countHarvested, numSpecies));
        numSpecies -= countHarvested;
    }

    public void recordMortality(int count) {
        if (count < 0)
            throw new IllegalArgumentException("Mortality count cannot be negative");
        if (count > numSpecies)
            throw new IllegalArgumentException(
                    "Mortality count " + count +
                            " exceeds current population " + numSpecies);
        this.numSpecies -= count;
    }

    // a cycle = the period between two restocks
    // calling restock starts a new cycle: cycleBaseline resets and future harvests are tracked separately
    public void restock(int count) {
        if (count < 0)
            throw new IllegalArgumentException("Restock count cannot be negative");
        this.numSpecies    += count;
        this.cycleBaseline  = this.numSpecies;
        this.cycleStartHarvestIndex = harvestHistory.size();
    }

    private int getCycleHarvestedCount() {
        int total = 0;
        for (int i = cycleStartHarvestIndex; i < harvestHistory.size(); i++) {
            total += harvestHistory.get(i).getCountHarvested();
        }
        return total;
    }

    // mortality within current cycle only — excludes harvests, never negative
    public int getCycleMortality() {
        int totalHarvestedInCycle = getCycleHarvestedCount();
        return Math.max(0, cycleBaseline - numSpecies - totalHarvestedInCycle);
    }

    // mortality since original stock — excludes harvests, never negative
    public int getTotalMortality() {
        return Math.max(0,
                initialTotalIndividuals - numSpecies - getTotalHarvestedCount());
    }

    // survival rate within current cycle — always 0-100%
    public double getCycleSurvivalRatePercent() {
        if (cycleBaseline == 0) return 0;
        return Math.min(100.0, (numSpecies / (double) cycleBaseline) * 100);
    }

    // survival rate since original stock — always 0-100%
    public double getOverallSurvivalRatePercent() {
        if (initialTotalIndividuals == 0) return 0;
        return Math.min(100.0, (numSpecies / (double) initialTotalIndividuals) * 100);
    }

    public double getTotalHarvestWeightKg() {
        return harvestHistory.stream()
                .mapToDouble(HarvestRecord::getWeightKg)
                .sum();
    }

    public int getTotalHarvestedCount() {
        return harvestHistory.stream()
                .mapToInt(HarvestRecord::getCountHarvested)
                .sum();
    }

    public int     getHarvestCount() { return harvestHistory.size(); }
    public boolean wasHarvested()    { return !harvestHistory.isEmpty(); }

    public String getStatusReport() {
        return String.format(
                "Species: %s | Current: %d / %d initial | Cycle baseline: %d | " +
                        "Cycle mortality: %d | Total mortality: %d | " +
                        "Harvests: %d (%.2f kg total) | " +
                        "Cycle survival: %.1f%% | Overall survival: %.1f%%",
                name,
                numSpecies,
                initialTotalIndividuals,
                cycleBaseline,
                getCycleMortality(),
                getTotalMortality(),
                getHarvestCount(),
                getTotalHarvestWeightKg(),
                getCycleSurvivalRatePercent(),
                getOverallSurvivalRatePercent()
        );
    }

    public String getId()                        { return id; }
    public String getName()                      { return name; }
    public int    getNumSpecies()                { return numSpecies; }
    public int    getInitialTotalIndividuals()   { return initialTotalIndividuals; }
    public int    getCycleBaseline()             { return cycleBaseline; }
    public ZONE   getZone()                      { return zone; }

    public boolean hasBoundary()                 { return boundary != null && boundary.size() >= 3; }
    public GoegraphicBoundries getBoundary()     { return boundary; }
    public void setBoundary(GoegraphicBoundries b) { this.boundary = b; }

    public List<HarvestRecord> getHarvestHistory() {
        return Collections.unmodifiableList(harvestHistory);
    }
}