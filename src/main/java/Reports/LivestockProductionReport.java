package Reports;

import Alerts.Alert;
import Animals.Animal;
import ZONES.LivestockZONE;
import java.time.LocalDateTime;

public class LivestockProductionReport extends ProductionReport {

    private double totalMilkYieldLiters;
    private int    totalEggCount;
    private int    milkProducingAnimals;
    private int    eggLayingAnimals;
    private double avgMilkPerAnimal;
    private double avgEggsPerAnimal;

    public LivestockProductionReport(LivestockZONE zone, ReportType reportType,
                                     LocalDateTime periodStart, LocalDateTime periodEnd) {
        super(zone, reportType, periodStart, periodEnd, new java.util.ArrayList<>());
    }

    @Override
    protected void computeSpecificStats(java.util.List<Alert> alerts) {
        LivestockZONE zone = (LivestockZONE) getZone();

        totalMilkYieldLiters = 0;
        totalEggCount        = 0;
        milkProducingAnimals = 0;
        eggLayingAnimals     = 0;

        for (Animal animal : zone.getAnimals()) {
            double milk = animal.getMilkYieldLiters();
            int    eggs = animal.getEggCount();

            if (milk > 0) {
                totalMilkYieldLiters += milk;
                milkProducingAnimals++;
                addSensorSummary(String.format("[Milk] %s — %.2f L",
                        animal.getName(), milk));
            }

            if (eggs > 0) {
                totalEggCount += eggs;
                eggLayingAnimals++;
                addSensorSummary(String.format("[Eggs] %s — %d eggs",
                        animal.getName(), eggs));
            }
        }

        avgMilkPerAnimal = milkProducingAnimals > 0
                ? totalMilkYieldLiters / milkProducingAnimals : 0;
        avgEggsPerAnimal = eggLayingAnimals > 0
                ? (double) totalEggCount / eggLayingAnimals : 0;

        if (totalMilkYieldLiters == 0 && totalEggCount == 0)
            addNote("No production recorded for this period");
        if (zone.getFeedingProgram() != null && zone.getFeedingProgram().isOverdue())
            addNote("Feeding was overdue — may have affected yield");
    }

    @Override
    protected String specificStatsBlock() {
        return  "  MILK PRODUCTION\n" +
                "    Total yield       : " + String.format("%.2f", totalMilkYieldLiters) + " L\n" +
                "    Producing animals : " + milkProducingAnimals + "\n" +
                "    Avg per animal    : " + String.format("%.2f", avgMilkPerAnimal) + " L\n" +
                "  EGG PRODUCTION\n" +
                "    Total count       : " + totalEggCount + " eggs\n" +
                "    Laying animals    : " + eggLayingAnimals + "\n" +
                "    Avg per animal    : " + String.format("%.2f", avgEggsPerAnimal) + " eggs\n";
    }

    public double getTotalMilkYieldLiters() { return totalMilkYieldLiters; }
    public int    getTotalEggCount()        { return totalEggCount; }
    public int    getMilkProducingAnimals() { return milkProducingAnimals; }
    public int    getEggLayingAnimals()     { return eggLayingAnimals; }
    public double getAvgMilkPerAnimal()     { return avgMilkPerAnimal; }
    public double getAvgEggsPerAnimal()     { return avgEggsPerAnimal; }
}