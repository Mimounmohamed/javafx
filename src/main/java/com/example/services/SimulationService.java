package com.example.services;

import Alerts.AlertSeverity;
import Alerts.AlertType;
import Alerts.HealthAlert;
import Alerts.SensorAlert;
import Animals.Animal;
import Animals.AnimalHealthStatus;
import Animals.HealthEvent;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Entities.GrowthStage;
import Entities.LIvestockType;
import Farm.Farm;
import Sensors.*;
import ZONES.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationService {

    public static class SimulationResult {
        public final List<String> log = new ArrayList<>();
        public int  daysSimulated;
        public int  healthEvents;
        public double milkLitersTotal;
        public int  eggsTotal;
        public int  harvestsRecorded;
        public int  alertsGenerated;
        public int  mortalityCount;
    }

    private static SimulationService instance;
    private final Random rng = new Random();

    private LocalDate simulationDate   = LocalDate.now();
    private int       totalDaysSimulated = 0;

    private SimulationService() {}

    public static SimulationService getInstance() {
        if (instance == null) instance = new SimulationService();
        return instance;
    }

    public LocalDate getSimulationDate()   { return simulationDate; }
    public int getTotalDaysSimulated()     { return totalDaysSimulated; }

    public void reset() {
        simulationDate    = LocalDate.now();
        totalDaysSimulated = 0;
    }

    // ── Main entry point ──────────────────────────────────────────────

    public SimulationResult simulateDays(int days) {
        SimulationResult result = new SimulationResult();
        result.daysSimulated = days;
        Farm farm = FarmService.getInstance().getFarm();

        for (int d = 1; d <= days; d++) {
            simulationDate = simulationDate.plusDays(1);
            totalDaysSimulated++;
            LocalDateTime dayTime = simulationDate.atTime(8, 0);

            for (LivestockZONE lz : farm.getLivestockZones()) {
                for (Animal a : lz.getAnimals())
                    simulateAnimal(a, dayTime, farm, result);
                for (BioSensor s : lz.getBioSensors())
                    simulateNumericSensor(s, dayTime, farm, result, lz.getName());
                for (GPSCollarSensor gps : lz.getGpsCollarSensors())
                    simulateGPS(gps, dayTime, farm, result, lz.getName());
            }

            for (CropZONE cz : farm.getCropZones()) {
                for (Crop c : cz.getFields())
                    simulateCrop(c, dayTime, result);
                for (EnvSensor s : cz.getEnvSensors())
                    simulateNumericSensor(s, dayTime, farm, result, cz.getName());
                for (SoilSensor s : cz.getSoilSensors())
                    simulateNumericSensor(s, dayTime, farm, result, cz.getName());
            }

            for (AquacultureZONE az : farm.getAquacultureZones()) {
                for (AquacultureSpecies sp : az.getSpeciesList())
                    simulateAquaculture(sp, totalDaysSimulated, dayTime, result);
                for (WaterSensor s : az.getWaterSensors())
                    simulateNumericSensor(s, dayTime, farm, result, az.getName());
            }
        }

        FarmService.getInstance().autoSave();
        return result;
    }

    // ── Per-entity simulators ─────────────────────────────────────────

    private void simulateAnimal(Animal a, LocalDateTime day, Farm farm, SimulationResult result) {
        // Weight drift: healthy grows slightly, sick loses slightly
        double driftPct = a.getHealthStatus() == AnimalHealthStatus.Sick
            ? -0.003 + (rng.nextDouble() - 0.5) * 0.010
            :  0.002 + (rng.nextDouble() - 0.5) * 0.010;
        double newWeight = round2(Math.max(0.5, a.getWeight() * (1.0 + driftPct)));
        a.updateWeight(newWeight, day);

        // Milk production (RUMINANT)
        if (a.getType() == LIvestockType.RUMINANT) {
            double base = 2.0 + a.getWeight() / 200.0;
            double milk = round2(base * (0.75 + rng.nextDouble() * 0.50));
            a.recordMilkYield(milk, day);
            result.milkLitersTotal += milk;
        }

        // Egg production (POULTRY)
        if (a.getType() == LIvestockType.POULTRY) {
            int eggs = rng.nextInt(10) < 8 ? 1 : 0;
            a.recordEgg(eggs, day);
            result.eggsTotal += eggs;
        }

        // Health — 1 % chance of getting sick
        if (a.getHealthStatus() == AnimalHealthStatus.Healthy && rng.nextInt(100) == 0) {
            String[] reasons = {"fever detected", "lethargy observed", "weight loss concern",
                                 "respiratory symptoms", "injury reported"};
            String reason = reasons[rng.nextInt(reasons.length)];
            HealthEvent ev = new HealthEvent(a, AnimalHealthStatus.Sick,
                AnimalHealthStatus.Healthy, null, reason);
            a.addHealthEvent(ev);
            farm.registerAlert(new HealthAlert(ev, AlertType.HEALTH_ALERT,
                AlertSeverity.Critical, a.getName() + " — " + reason));
            result.healthEvents++;
            result.alertsGenerated++;
            result.log.add("[" + day.toLocalDate() + "]  " + a.getName()
                + " became sick — " + reason);
        }
        // 10 % chance of natural recovery when sick
        else if (a.getHealthStatus() == AnimalHealthStatus.Sick && rng.nextInt(10) == 0) {
            try {
                AnimalHealthStatus before = a.getHealthStatus();
                a.resolveLastHealthEvent(AnimalHealthStatus.Healthy);
                HealthEvent recovery = new HealthEvent(a, AnimalHealthStatus.Healthy,
                    before, AnimalHealthStatus.Healthy, "Natural recovery");
                a.addHealthEvent(recovery);
                result.healthEvents++;
                result.log.add("[" + day.toLocalDate() + "]  "
                    + a.getName() + " recovered naturally ✓");
            } catch (Exception ignored) {}
        }
    }

    private void simulateCrop(Crop crop, LocalDateTime day, SimulationResult result) {
        LocalDate planting  = crop.getPlantingDate()
            .toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate expected  = crop.getExpectedHarvestDate()
            .toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        long total   = expected.toEpochDay() - planting.toEpochDay();
        if (total <= 0) return;
        long elapsed = day.toLocalDate().toEpochDay() - planting.toEpochDay();

        double pct = Math.min(1.0, (double) elapsed / total);
        GrowthStage newStage;
        if      (pct < 0.05) newStage = GrowthStage.sowing;
        else if (pct < 0.20) newStage = GrowthStage.germination;
        else if (pct < 0.55) newStage = GrowthStage.growth;
        else if (pct < 0.85) newStage = GrowthStage.maturity;
        else                 newStage = GrowthStage.harvest;

        if (newStage != crop.getGrowthStage()) {
            crop.updateGrowthStage(newStage);
            result.log.add("[" + day.toLocalDate() + "]  " + crop.getVariety()
                + " advanced to " + newStage + " stage");
        }

        // Partial harvest batch every Wednesday when ready
        if (crop.getGrowthStage() == GrowthStage.harvest
                && day.getDayOfWeek().getValue() == 3) {
            double kg = round2(200 + rng.nextDouble() * 800);
            crop.recordHarvest(kg, day);
            result.harvestsRecorded++;
            result.log.add("[" + day.toLocalDate() + "]  " + crop.getVariety()
                + " partial harvest — " + String.format("%.0f kg", kg));
        }
    }

    private void simulateAquaculture(AquacultureSpecies sp, int simDay,
                                      LocalDateTime day, SimulationResult result) {
        int pop = sp.getNumSpecies();
        if (pop <= 0) return;

        // Daily mortality 0.05–0.15 %
        int mort = (int)(pop * (0.0005 + rng.nextDouble() * 0.001));
        if (mort > 0) {
            sp.recordMortality(mort);
            result.mortalityCount += mort;
        }

        // Harvest every 6 sim-days if enough stock
        if (simDay % 6 == 0 && sp.getNumSpecies() > 10) {
            int harv = Math.min(sp.getNumSpecies() - 5, 5 + rng.nextInt(16));
            double kg = round2(harv * (0.5 + rng.nextDouble() * 2.0));
            try {
                sp.harvest(kg, harv, day);
                result.harvestsRecorded++;
                result.log.add("[" + day.toLocalDate() + "]  " + sp.getName()
                    + " harvest — " + harv + " fish, "
                    + String.format("%.1f kg", kg));
            } catch (Exception ignored) {}
        }
    }

    private void simulateNumericSensor(NumericSensor sensor, LocalDateTime day,
                                        Farm farm, SimulationResult result,
                                        String zoneName) {
        double min    = sensor.getMinThreshold();
        double max    = sensor.getMaxThreshold();
        double center = (min + max) / 2.0;
        double range  = max - min;

        double value;
        if (rng.nextInt(20) == 0) {
            // 5 % spike out of range
            value = rng.nextBoolean()
                ? max + range * (0.15 + rng.nextDouble() * 0.25)
                : min - range * (0.15 + rng.nextDouble() * 0.25);
        } else {
            value = center + (rng.nextDouble() - 0.5) * range * 0.6;
        }
        value = round2(value);

        NumericSensorReading reading = new NumericSensorReading(
            sensor, value, sensor.getUnit(), day);
        sensor.addReading(reading);
        sensor.setLastValue(value);

        if (sensor.isOutOfRange(value)) {
            AlertSeverity sev = reading.getSeverity() == ReadingLevel.CRITICAL
                ? AlertSeverity.Critical : AlertSeverity.Warning;
            AlertType type;
            if      (sensor instanceof BioSensor)   type = AlertType.BioSensorAlert;
            else if (sensor instanceof EnvSensor)   type = AlertType.EnvSensorAlert;
            else if (sensor instanceof SoilSensor)  type = AlertType.SoilSensorAlert;
            else if (sensor instanceof WaterSensor) type = AlertType.WaterSensorAlert;
            else                                    type = AlertType.BioSensorAlert;

            farm.registerAlert(new SensorAlert(reading, type, sev,
                String.format("Sensor %s out of range (%.2f %s) in %s",
                    sensor.getCode(), value, sensor.getUnit(), zoneName)));
            result.alertsGenerated++;
        }
    }

    private void simulateGPS(GPSCollarSensor gps, LocalDateTime day,
                               Farm farm, SimulationResult result,
                               String zoneName) {
        double lat = gps.getCurrentLatitude();
        double lon = gps.getCurrentLongitude();
        if (lat == 0.0 && lon == 0.0) return; // no initial position set

        lat = round4(lat + (rng.nextDouble() - 0.5) * 0.0002);
        lon = round4(lon + (rng.nextDouble() - 0.5) * 0.0002);
        boolean wasInside = gps.isInsideZone();
        gps.updateLocation(lat, lon);
        gps.addReading(new GPSSensorReading(gps, lat, lon, gps.isInsideZone(), day));

        // Alert only on the transition from inside → outside
        if (wasInside && !gps.isInsideZone()) {
            GPSSensorReading last = gps.getLastReading();
            if (last != null) {
                farm.registerAlert(new SensorAlert(last, AlertType.GPS_ESCAPE_ALERT,
                    AlertSeverity.Critical,
                    gps.getAnimal().getName() + " escaped zone bounds in " + zoneName));
                result.alertsGenerated++;
                result.log.add("[" + day.toLocalDate() + "]  "
                    + gps.getAnimal().getName() + " detected outside zone bounds!");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private double round2(double v) { return Math.round(v * 100.0)   / 100.0; }
    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
}
