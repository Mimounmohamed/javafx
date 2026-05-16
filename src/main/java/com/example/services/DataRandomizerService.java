package com.example.services;

import Alerts.Alert;
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
import Farm.Farm;
import Sensors.BioSensor;
import Sensors.EnvSensor;
import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
import Sensors.SoilSensor;
import Sensors.WaterSensor;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;

import java.time.LocalDateTime;
import java.util.Random;

public class DataRandomizerService {

    private static DataRandomizerService instance;
    private final Random rng = new Random();

    private DataRandomizerService() {}

    public static DataRandomizerService getInstance() {
        if (instance == null) instance = new DataRandomizerService();
        return instance;
    }

    /**
     * Re-seeds the entire farm with fresh random data.
     * Calls FarmService.initWithDemo() for a clean slate, then overlays random values.
     */
    public void randomize() {
        FarmService.initWithDemo();
        Farm farm = FarmService.getInstance().getFarm();

        for (LivestockZONE z : farm.getLivestockZones()) {
            for (BioSensor s        : z.getBioSensors())          randomizeNumericSensor(s);
            for (GPSCollarSensor s  : z.getGpsCollarSensors())    randomizeGPSSensor(s);
            for (Animal a           : z.getAnimals())             randomizeAnimal(a);
        }
        for (CropZONE z : farm.getCropZones()) {
            for (EnvSensor  s : z.getEnvSensors())  randomizeNumericSensor(s);
            for (SoilSensor s : z.getSoilSensors()) randomizeNumericSensor(s);
            for (Crop c       : z.getFields())      randomizeCrop(c);
        }
        for (AquacultureZONE z : farm.getAquacultureZones()) {
            for (WaterSensor s         : z.getWaterSensors()) randomizeNumericSensor(s);
            for (AquacultureSpecies sp : z.getSpeciesList())  randomizeSpecies(sp);
        }

        farm.clearAlerts();
        generateAlerts(farm);
    }

    // ── Sensor randomization ──────────────────────────────────────────────

    private void randomizeNumericSensor(NumericSensor sensor) {
        sensor.clearReadingHistory();
        double min    = sensor.getMinThreshold();
        double max    = sensor.getMaxThreshold();
        double range  = max - min;
        double center = (min + max) / 2.0;

        int spikeStart = 5 + rng.nextInt(6);
        int spikeLen   = 2 + rng.nextInt(3);
        boolean goHigh = rng.nextBoolean();

        double[] values = new double[15];
        for (int i = 0; i < 15; i++) {
            if (i >= spikeStart && i < spikeStart + spikeLen) {
                double excess = range * (0.15 + rng.nextDouble() * 0.35);
                values[i] = goHigh ? max + excess : min - excess;
            } else {
                values[i] = center + (rng.nextDouble() - 0.5) * range * 0.6;
                values[i] = clamp(values[i], min - range * 0.02, max + range * 0.02);
            }
        }

        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 15; i++) {
            sensor.addReading(new NumericSensorReading(
                    sensor, round2(values[i]), sensor.getUnit(), base.plusDays(i)));
        }
        sensor.setLastValue(round2(values[14]));
    }

    private void randomizeGPSSensor(GPSCollarSensor sensor) {
        sensor.clearReadingHistory();
        double baseLat   = 36.700 + rng.nextDouble() * 0.010;
        double baseLon   = 3.050  + rng.nextDouble() * 0.015;
        boolean hasEscape = rng.nextBoolean();

        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 15; i++) {
            boolean inside = !hasEscape || i < 13;
            double lat = baseLat + i * 0.0001 + (rng.nextDouble() - 0.5) * 0.0003;
            double lon = baseLon + i * 0.0001 + (rng.nextDouble() - 0.5) * 0.0003;
            sensor.addReading(new GPSSensorReading(sensor, lat, lon, inside, base.plusDays(i)));
        }
    }

    // ── Entity randomization ──────────────────────────────────────────────

    private void randomizeAnimal(Animal animal) {
        animal.resetProductionStats();
        double milk = 8 + rng.nextDouble() * 35;
        animal.recordMilkYield(round2(milk));

        int roll = rng.nextInt(10);
        AnimalHealthStatus newStatus = roll < 6 ? AnimalHealthStatus.Healthy
                : roll < 8 ? AnimalHealthStatus.Sick
                : AnimalHealthStatus.Quarantined;
        animal.setHealthStatus(newStatus);
    }

    private void randomizeCrop(Crop crop) {
        GrowthStage[] stages = GrowthStage.values();
        GrowthStage stage = stages[rng.nextInt(stages.length)];
        crop.updateGrowthStage(stage);
        if (stage == GrowthStage.harvest) {
            double kg = 800 + rng.nextDouble() * 6000;
            crop.recordHarvest(round2(kg));
        }
    }

    private void randomizeSpecies(AquacultureSpecies species) {
        int current = species.getNumSpecies();
        if (current <= 0) return;

        int mortality = rng.nextInt(Math.max(1, current / 5 + 1));
        if (mortality > 0 && mortality <= current)
            species.recordMortality(mortality);

        int remaining = species.getNumSpecies();
        if (remaining > 0) {
            int harvested = rng.nextInt(Math.max(1, remaining / 4 + 1));
            if (harvested > 0 && harvested <= remaining) {
                double weight = harvested * (0.25 + rng.nextDouble() * 0.75);
                species.harvest(round2(weight), harvested);
            }
        }
    }

    // ── Alert generation ──────────────────────────────────────────────────

    private void generateAlerts(Farm farm) {
        for (LivestockZONE z : farm.getLivestockZones()) {
            for (BioSensor s : z.getBioSensors()) {
                NumericSensorReading last = lastNumericReading(s);
                if (last != null && s.isOutOfRange(last.getValue())) {
                    AlertSeverity sev = last.getSeverity() == ReadingLevel.CRITICAL
                            ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new SensorAlert(last, AlertType.BioSensorAlert, sev,
                            String.format("BioSensor %s out of range: %.2f %s in %s",
                                    s.getCode(), last.getValue(), s.getUnit(), z.getName())));
                }
            }
            for (Animal a : z.getAnimals()) {
                if (a.isSick() || a.isQuarantined()) {
                    String desc = a.isSick() ? "illness detected" : "precautionary quarantine";
                    HealthEvent event = new HealthEvent(a, a.getHealthStatus(),
                            AnimalHealthStatus.Healthy, null, desc);
                    a.addHealthEvent(event);
                    AlertSeverity sev = a.isSick() ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new HealthAlert(event, AlertType.HEALTH_ALERT, sev,
                            a.getName() + " — " + desc + " in " + z.getName()));
                }
            }
        }
        for (CropZONE z : farm.getCropZones()) {
            for (EnvSensor s : z.getEnvSensors()) {
                NumericSensorReading last = lastNumericReading(s);
                if (last != null && s.isOutOfRange(last.getValue())) {
                    AlertSeverity sev = last.getSeverity() == ReadingLevel.CRITICAL
                            ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new SensorAlert(last, AlertType.EnvSensorAlert, sev,
                            String.format("EnvSensor %s out of range: %.2f %s in %s",
                                    s.getCode(), last.getValue(), s.getUnit(), z.getName())));
                }
            }
            for (SoilSensor s : z.getSoilSensors()) {
                NumericSensorReading last = lastNumericReading(s);
                if (last != null && s.isOutOfRange(last.getValue())) {
                    AlertSeverity sev = last.getSeverity() == ReadingLevel.CRITICAL
                            ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new SensorAlert(last, AlertType.SoilSensorAlert, sev,
                            String.format("SoilSensor %s out of range: %.2f %s in %s",
                                    s.getCode(), last.getValue(), s.getUnit(), z.getName())));
                }
            }
        }
        for (AquacultureZONE z : farm.getAquacultureZones()) {
            for (WaterSensor s : z.getWaterSensors()) {
                NumericSensorReading last = lastNumericReading(s);
                if (last != null && s.isOutOfRange(last.getValue())) {
                    AlertSeverity sev = last.getSeverity() == ReadingLevel.CRITICAL
                            ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new SensorAlert(last, AlertType.WaterSensorAlert, sev,
                            String.format("WaterSensor %s out of range: %.2f %s in %s",
                                    s.getCode(), last.getValue(), s.getUnit(), z.getName())));
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private NumericSensorReading lastNumericReading(NumericSensor sensor) {
        var history = sensor.getReadingHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i) instanceof NumericSensorReading nr) return nr;
        }
        return null;
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
