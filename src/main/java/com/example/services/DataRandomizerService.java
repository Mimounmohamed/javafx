package com.example.services;

import Additional_classes.Range;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Alerts.HealthAlert;
import Alerts.SensorAlert;
import Animals.Animal;
import Animals.AnimalHealthStatus;
import Animals.HealthEvent;
import Entities.AquacultureSpecies;
import Entities.Crop;
import Entities.CropType;
import Entities.GrowthStage;
import Entities.LIvestockType;
import Farm.Farm;
import Sensors.BioMeasureType;
import Sensors.BioSensor;
import Sensors.EnvMeasureType;
import Sensors.EnvSensor;
import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
import Sensors.SoilMeasureType;
import Sensors.SoilSensor;
import Sensors.WaterMeasureType;
import Sensors.WaterSensor;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.GoegraphicBoundries;
import ZONES.LivestockZONE;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Populates a brand-new empty farm with randomly generated zones, animals,
 * crops, aquaculture species, sensors, readings and alerts.
 * Called automatically after FarmService.initWithNewFarm().
 */
public class DataRandomizerService {

    private static DataRandomizerService instance;
    private final Random rng = new Random();

    // ── Name pools ────────────────────────────────────────────────────────

    private static final String[][] LS_TEMPLATES = {
        {"North Pasture","RUMINANT"}, {"South Paddock","RUMINANT"},
        {"East Grazing","RUMINANT"},  {"West Ranch","RUMINANT"},
        {"Highland Meadow","RUMINANT"},{"Valley Pasture","RUMINANT"},
        {"Poultry Barn A","POULTRY"}, {"Poultry Barn B","POULTRY"}
    };
    private static final String[] CROP_ZONE_NAMES = {
        "South Fields","Wheat Acre","Main Cropland","North Plots",
        "East Farm","River Plot","Hill Fields","Sunny Fields"
    };
    private static final String[] AQUA_ZONE_NAMES = {
        "Pond Alpha","Main Fish Pond","Trout Pool",
        "Salmon Basin","East Reservoir","Lakeside Farm"
    };

    private static final String[][] RUMINANT_POOL = {
        {"Bessie","Bovine"},{"Rex","Bovine"},{"Luna","Ovine"},
        {"Daisy","Bovine"},{"Molly","Bovine"},{"Rosie","Ovine"},
        {"Bella","Bovine"},{"Star","Bovine"},{"Dot","Ovine"},
        {"Flora","Bovine"},{"Clover","Ovine"},{"Ivy","Ovine"},
        {"Sage","Bovine"},{"Duchess","Bovine"},{"Pearl","Bovine"},
        {"Ruby","Ovine"},{"Blossom","Bovine"},{"Fern","Bovine"},
        {"Hazel","Ovine"},{"Maple","Bovine"},{"Birch","Bovine"},
        {"Cedar","Bovine"},{"Aspen","Ovine"},{"Willow","Bovine"},
        {"Poppy","Ovine"},{"Violet","Ovine"},{"Misty","Bovine"},
        {"Butterscotch","Bovine"},{"Caramel","Ovine"},{"Snowflake","Ovine"}
    };
    private static final String[] POULTRY_POOL = {
        "Henny","Penny","Cluck","Peck","Feathers","Nugget",
        "Squawk","Robin","Pebbles","Buttons","Freckles","Pip",
        "Speckles","Goldie","Rusty","Cinnamon","Ginger","Saffron",
        "Biscuit","Sandy","Tawny","Crimson","Dusty","Ember"
    };
    private static final String[] AQUA_SPECIES_POOL = {
        "Tilapia","Salmon","Catfish","Trout","Carp","Perch",
        "Bass","Bream","Mullet","Tench","Pike","Roach"
    };
    private static final String[][] CROP_POOL = {
        {"Anza","cereals"},{"DKC 63-84","cereals"},{"Roma","vegetables"},
        {"Golden Wheat","cereals"},{"Red Fife","cereals"},{"Black Beauty","vegetables"},
        {"Honeydew","fruits"},{"Valencia","fruits"},{"Cherry Belle","vegetables"},
        {"Sungold","vegetables"},{"Butternut","vegetables"},{"Fuji","fruits"},
        {"Gala","fruits"},{"Iceberg","vegetables"},{"Romanesco","vegetables"},
        {"Jubilee","fruits"},{"Crimson Sweet","fruits"},{"Chandler","fruits"}
    };

    // ── Singleton ─────────────────────────────────────────────────────────

    private DataRandomizerService() {}

    public static DataRandomizerService getInstance() {
        if (instance == null) instance = new DataRandomizerService();
        return instance;
    }

    // ── Entry point ───────────────────────────────────────────────────────

    /**
     * Builds random zones, entities and sensor data on the current (empty) farm.
     * Assumes FarmService.initWithNewFarm() was already called.
     */
    public void populateNewFarm() {
        String savedId = FarmService.getInstance().getSavedId();
        if (savedId != null) rng.setSeed(deriveSeed(savedId));
        System.out.println("[DataRandomizer] START — farm id: " + savedId);

        Farm farm = FarmService.getInstance().getFarm();

        double baseLat = 36.65 + rng.nextDouble() * 0.10;
        double baseLon = 2.95  + rng.nextDouble() * 0.15;

        int lsCount   = 1 + rng.nextInt(3);
        int cropCount = 1 + rng.nextInt(2);
        int aquaCount = 1 + rng.nextInt(2);
        System.out.println("[DataRandomizer] plan: " + lsCount + " livestock, " + cropCount + " crop, " + aquaCount + " aqua zones");

        String[][] lsPool = shuffled(LS_TEMPLATES);

        for (int i = 0; i < lsCount; i++) {
            String[] tpl  = lsPool[i % lsPool.length];
            String name   = tpl[0] + (lsCount > 1 ? " " + (i + 1) : "");
            LIvestockType type = LIvestockType.valueOf(tpl[1]);

            double zLat1  = baseLat + i * 0.018;
            double zLon1  = baseLon;
            double zLat2  = zLat1 + 0.008 + rng.nextDouble() * 0.007;
            double zLon2  = zLon1 + 0.012 + rng.nextDouble() * 0.008;
            GoegraphicBoundries bounds = irregularBounds(zLat1, zLon1, zLat2, zLon2);

            LivestockZONE lz = new LivestockZONE(name, type, bounds);
            farm.addZone(lz);
            System.out.println("[DataRandomizer] livestock zone '" + name + "' created, populating...");
            populateLivestockZone(lz);
            System.out.println("[DataRandomizer]   → " + lz.getAnimals().size() + " animals, " + lz.getBioSensors().size() + " bio sensors");
        }

        for (int i = 0; i < cropCount; i++) {
            String name  = CROP_ZONE_NAMES[rng.nextInt(CROP_ZONE_NAMES.length)]
                           + (cropCount > 1 ? " " + (i + 1) : "");
            double zLat1 = baseLat - 0.020 - i * 0.020;
            double zLon1 = baseLon + 0.025 + i * 0.022;
            double zLat2 = zLat1 + 0.010 + rng.nextDouble() * 0.008;
            double zLon2 = zLon1 + 0.016 + rng.nextDouble() * 0.010;

            CropZONE cz = new CropZONE(name);
            cz.setBoundaries(irregularBounds(zLat1, zLon1, zLat2, zLon2));
            farm.addZone(cz);
            System.out.println("[DataRandomizer] crop zone '" + name + "' created, populating...");
            populateCropZone(cz);
            System.out.println("[DataRandomizer]   → " + cz.getFields().size() + " fields");
        }

        for (int i = 0; i < aquaCount; i++) {
            String name  = AQUA_ZONE_NAMES[rng.nextInt(AQUA_ZONE_NAMES.length)]
                           + (aquaCount > 1 ? " " + (i + 1) : "");
            double zLat1 = baseLat + 0.008;
            double zLon1 = baseLon - 0.018 - i * 0.014;
            double zLat2 = zLat1 + 0.006 + rng.nextDouble() * 0.004;
            double zLon2 = zLon1 + 0.010 + rng.nextDouble() * 0.006;

            AquacultureZONE az = new AquacultureZONE(name);
            az.setBoundaries(irregularBounds(zLat1, zLon1, zLat2, zLon2));
            farm.addZone(az);
            System.out.println("[DataRandomizer] aqua zone '" + name + "' created, populating...");
            populateAquaZone(az);
            System.out.println("[DataRandomizer]   → " + az.getSpeciesList().size() + " species");
        }

        // Compute farm boundary as bounding box of all zone boundaries + 18 % padding
        double fbMinLat = Double.MAX_VALUE, fbMaxLat = -Double.MAX_VALUE;
        double fbMinLon = Double.MAX_VALUE, fbMaxLon = -Double.MAX_VALUE;
        for (LivestockZONE z : farm.getLivestockZones()) {
            if (!z.hasBoundaries()) continue;
            for (double[] p : z.getBoundaries().getPoints()) {
                if (p[0] < fbMinLat) fbMinLat = p[0]; if (p[0] > fbMaxLat) fbMaxLat = p[0];
                if (p[1] < fbMinLon) fbMinLon = p[1]; if (p[1] > fbMaxLon) fbMaxLon = p[1];
            }
        }
        for (CropZONE z : farm.getCropZones()) {
            if (!z.hasBoundaries()) continue;
            for (double[] p : z.getBoundaries().getPoints()) {
                if (p[0] < fbMinLat) fbMinLat = p[0]; if (p[0] > fbMaxLat) fbMaxLat = p[0];
                if (p[1] < fbMinLon) fbMinLon = p[1]; if (p[1] > fbMaxLon) fbMaxLon = p[1];
            }
        }
        for (AquacultureZONE z : farm.getAquacultureZones()) {
            if (!z.hasBoundaries()) continue;
            for (double[] p : z.getBoundaries().getPoints()) {
                if (p[0] < fbMinLat) fbMinLat = p[0]; if (p[0] > fbMaxLat) fbMaxLat = p[0];
                if (p[1] < fbMinLon) fbMinLon = p[1]; if (p[1] > fbMaxLon) fbMaxLon = p[1];
            }
        }
        if (fbMinLat < Double.MAX_VALUE) {
            double padLat = (fbMaxLat - fbMinLat) * 0.18;
            double padLon = (fbMaxLon - fbMinLon) * 0.18;
            farm.setFarmBoundary(GoegraphicBoundries.createRectangle(
                fbMinLat - padLat, fbMinLon - padLon,
                fbMaxLat + padLat, fbMaxLon + padLon));
            System.out.println("[DataRandomizer] farm boundary set");
        }

        System.out.println("[DataRandomizer] generating alerts...");
        generateAlerts(farm);
        System.out.println("[DataRandomizer] DONE — zones=" + farm.getTotalZoneCount()
            + " animals=" + farm.getStats().totalAnimals
            + " alerts=" + farm.getAllAlerts().size());
        FarmService.getInstance().markAsRandomized();
        FarmService.getInstance().autoSave();
    }

    // ── Zone populators ───────────────────────────────────────────────────

    private void populateLivestockZone(LivestockZONE z) {
        double area  = boundingBoxArea(z);
        int    count = animalCount(area, z.getType());

        for (int i = 0; i < count; i++) {
            Animal a = makeAnimal(z);
            z.addAnimal(a);

            // Temperature bio sensor on first ~30 % of animals
            if (i < Math.max(1, count / 3)) {
                double tMin = z.getType() == LIvestockType.POULTRY ? 40.5 : 38.0;
                double tMax = z.getType() == LIvestockType.POULTRY ? 42.0 : 39.5;
                BioSensor temp = new BioSensor(a, BioMeasureType.Temperature, tMin, tMax);
                a.attachBioSensor(temp);
                z.addBioSensor(temp);
                randomizeNumericSensor(temp);
            }

            if (z.getType() == LIvestockType.POULTRY)
                randomizeEggHistory(a);

            randomizeWeightHistory(a);

            // GPS on every animal
            GPSCollarSensor gps = new GPSCollarSensor(a);
            a.attachGPSCollar(gps);
            z.addGpsCollarSensor(gps);
            randomizeGPSSensor(gps, z);
        }
    }

    private void populateCropZone(CropZONE z) {
        double area       = boundingBoxArea(z);
        int    fieldCount = subCount(area, 2, 8);
        boolean splitV    = splitVertical(z);

        for (int i = 0; i < fieldCount; i++) {
            GoegraphicBoundries sub = subPolygon(z.getBoundaries(), i, fieldCount, splitV);
            String[] cropData = CROP_POOL[rng.nextInt(CROP_POOL.length)];
            CropType type = switch (cropData[1]) {
                case "cereals"    -> CropType.cereals;
                case "vegetables" -> CropType.vegetables;
                default           -> CropType.fruits;
            };
            Date planted = new Date(System.currentTimeMillis()
                    - (long)(rng.nextInt(90) + 10) * 86_400_000L);
            Date harvest = new Date(System.currentTimeMillis()
                    + (long)(rng.nextInt(120) + 30) * 86_400_000L);
            Crop crop = new Crop(type, cropData[0], planted, harvest,
                    new Range(6.0 + rng.nextDouble(), 7.0 + rng.nextDouble()),
                    new Range(40 + rng.nextInt(20), 65 + rng.nextInt(20)), z);
            // Weight stages toward productive ones — sowing/germination are rare
            int sr = rng.nextInt(10);
            GrowthStage stage = sr < 1 ? GrowthStage.sowing
                              : sr < 2 ? GrowthStage.germination
                              : sr < 4 ? GrowthStage.growth
                              : sr < 7 ? GrowthStage.maturity
                              :          GrowthStage.harvest;
            crop.updateGrowthStage(stage);
            // Scale yield by per-sub-field area (sq-deg → approximate ha at ~36° N)
            double subAreaHa  = (area / fieldCount) * 1_100_000.0;
            double maxYieldKg = subAreaHa * switch (type) {
                case cereals    -> 2000 + rng.nextDouble() * 6000;   // 2–8 t/ha
                case vegetables -> 1500 + rng.nextDouble() * 3500;   // 1.5–5 t/ha
                case fruits     ->  800 + rng.nextDouble() * 2200;   // 0.8–3 t/ha
            };
            if (stage == GrowthStage.harvest)
                randomizeCropYieldHistory(crop, maxYieldKg, 3 + rng.nextInt(3));
            else if (stage == GrowthStage.maturity)
                randomizeCropYieldHistory(crop, maxYieldKg * (0.5 + rng.nextDouble() * 0.3), 2);
            crop.setBoundary(sub);
            z.addField(crop);
        }

        z.setSurfacePlanted(round2(area * 1_000_000));

        EnvSensor eTemp = new EnvSensor(z, EnvMeasureType.Temperature,
                15.0 + rng.nextDouble() * 3, 28.0 + rng.nextDouble() * 4, "°C");
        EnvSensor eHum  = new EnvSensor(z, EnvMeasureType.Humidity,
                38.0 + rng.nextDouble() * 5, 75.0 + rng.nextDouble() * 5, "%");
        z.addEnvSensor(eTemp);
        z.addEnvSensor(eHum);
        randomizeNumericSensor(eTemp);
        randomizeNumericSensor(eHum);

        SoilSensor sPH   = new SoilSensor(z, SoilMeasureType.PH,
                5.8 + rng.nextDouble() * 0.5, 7.0 + rng.nextDouble() * 0.5, "pH");
        SoilSensor sMois = new SoilSensor(z, SoilMeasureType.Moisture,
                45.0 + rng.nextDouble() * 10, 68.0 + rng.nextDouble() * 8, "%");
        z.addSoilSensor(sPH);
        z.addSoilSensor(sMois);
        randomizeNumericSensor(sPH);
        randomizeNumericSensor(sMois);
    }

    private void populateAquaZone(AquacultureZONE z) {
        double area      = boundingBoxArea(z);
        int    tankCount = subCount(area, 2, 5);
        boolean splitV   = splitVertical(z);

        for (int i = 0; i < tankCount; i++) {
            GoegraphicBoundries tank = subPolygon(z.getBoundaries(), i, tankCount, splitV);
            String name  = AQUA_SPECIES_POOL[rng.nextInt(AQUA_SPECIES_POOL.length)];
            int    stock = 200 + rng.nextInt(1200);
            AquacultureSpecies sp = new AquacultureSpecies(name, stock, z);
            sp.setBoundary(tank);

            int mort = rng.nextInt(Math.max(1, stock / 10));
            if (mort > 0 && mort <= sp.getNumSpecies())
                sp.recordMortality(mort);

            randomizeAquaHarvestHistory(sp, name);
            z.addSpecies(sp);
        }

        WaterSensor wTemp = new WaterSensor(z, WaterMeasureType.Temperature,
                17.0 + rng.nextDouble() * 3, 25.0 + rng.nextDouble() * 3, "°C");
        WaterSensor wO2   = new WaterSensor(z, WaterMeasureType.DissolvedOxygen,
                4.5 + rng.nextDouble() * 1.5, 9.0 + rng.nextDouble() * 2, "mg/L");
        z.addWaterSensor(wTemp);
        z.addWaterSensor(wO2);
        randomizeNumericSensor(wTemp);
        randomizeNumericSensor(wO2);
    }

    // ── Animal factory ────────────────────────────────────────────────────

    private Animal makeAnimal(LivestockZONE z) {
        String name, species;
        if (z.getType() == LIvestockType.POULTRY) {
            name    = POULTRY_POOL[rng.nextInt(POULTRY_POOL.length)] + "-" + (rng.nextInt(900) + 100);
            species = "Gallus";
        } else {
            String[] entry = RUMINANT_POOL[rng.nextInt(RUMINANT_POOL.length)];
            name    = entry[0] + "-" + (rng.nextInt(900) + 100);
            species = entry[1];
        }
        int    age    = 1 + rng.nextInt(8);
        double weight = z.getType() == LIvestockType.POULTRY
                ? 1.5 + rng.nextDouble() * 3.5
                : 80 + rng.nextDouble() * 450;

        Animal a = new Animal(species, name, z.getType(), age, weight, z);

        int roll = rng.nextInt(10);
        a.setHealthStatus(roll < 7 ? AnimalHealthStatus.Healthy
                : roll < 9 ? AnimalHealthStatus.Sick
                : AnimalHealthStatus.Quarantined);

        if (z.getType() == LIvestockType.RUMINANT)
            randomizeMilkHistory(a);

        return a;
    }

    // ── Animal history generators ─────────────────────────────────────────

    private void randomizeWeightHistory(Animal a) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(8).withMinute(0).withSecond(0).withNano(0);
        double w = a.getWeight();
        for (int d = 1; d <= 14; d++) {
            // Small daily drift: gentle growth trend + ±1.5% noise
            w = round2(w * (1.0 + 0.002 + (rng.nextDouble() - 0.5) * 0.03));
            a.updateWeight(w, base.plusDays(d));
        }
    }

    private void randomizeMilkHistory(Animal a) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(6).withMinute(0).withSecond(0).withNano(0);
        // Daily base: 0.8–5 L depending on the individual animal
        double dailyBase = 0.8 + rng.nextDouble() * 4.2;
        for (int d = 0; d < 15; d++) {
            double milk = round2(dailyBase * (0.80 + rng.nextDouble() * 0.40));
            a.recordMilkYield(milk, base.plusDays(d));
        }
    }

    private void randomizeEggHistory(Animal a) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(7).withMinute(0).withSecond(0).withNano(0);
        // Laying hens: 0–1 egg/day with ~80% success rate
        for (int d = 0; d < 15; d++) {
            int eggs = rng.nextInt(10) < 8 ? 1 : 0;
            a.recordEgg(eggs, base.plusDays(d));
        }
    }

    private void randomizeAquaHarvestHistory(AquacultureSpecies sp, String name) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(8).withMinute(0).withSecond(0).withNano(0);
        int harvests = 2 + rng.nextInt(3);   // 2–4 harvest events over 14 days
        int dayOffset = rng.nextInt(3);
        for (int h = 0; h < harvests; h++) {
            int rem = sp.getNumSpecies();
            if (rem < 5) break;
            int maxBatch = Math.max(1, rem / (harvests - h + 1));
            int harv = 1 + rng.nextInt(maxBatch);
            harv = Math.min(harv, rem);
            sp.harvest(round2(harv * avgFishWeightKg(name)), harv, base.plusDays(dayOffset));
            dayOffset += 3 + rng.nextInt(4);   // 3–6 days between events
        }
    }

    private void randomizeCropYieldHistory(Crop crop, double totalKg, int parts) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(10).withMinute(0).withSecond(0).withNano(0);
        // Spread partial harvests across the 14-day window with randomised gaps
        double remaining = totalKg;
        int dayOffset = 0;
        for (int p = 0; p < parts; p++) {
            boolean last = (p == parts - 1);
            double kg = last ? remaining
                             : round2(remaining * (0.25 + rng.nextDouble() * 0.35));
            kg = Math.min(kg, remaining);
            if (kg <= 0) break;
            crop.recordHarvest(kg, base.plusDays(dayOffset));
            remaining -= kg;
            dayOffset += 3 + rng.nextInt(4);  // 3–6 days between batches
        }
    }

    // ── Sensor data ───────────────────────────────────────────────────────

    private void randomizeNumericSensor(NumericSensor sensor) {
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
                values[i] = clamp(center + (rng.nextDouble() - 0.5) * range * 0.6,
                        min - range * 0.02, max + range * 0.02);
            }
        }

        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 15; i++)
            sensor.addReading(new NumericSensorReading(
                    sensor, round2(values[i]), sensor.getUnit(), base.plusDays(i)));
        sensor.setLastValue(round2(values[14]));
    }

    private void randomizeGPSSensor(GPSCollarSensor sensor, LivestockZONE z) {
        List<double[]> pts = z.getBoundaries().getPoints();
        double minLat = pts.stream().mapToDouble(p -> p[0]).min().orElse(36.700);
        double maxLat = pts.stream().mapToDouble(p -> p[0]).max().orElse(36.710);
        double minLon = pts.stream().mapToDouble(p -> p[1]).min().orElse(3.050);
        double maxLon = pts.stream().mapToDouble(p -> p[1]).max().orElse(3.065);
        double latR = maxLat - minLat;
        double lonR = maxLon - minLon;
        double stepLat = latR / 40.0;
        double stepLon = lonR / 40.0;

        // Start well inside the zone (20–80% of bounding box from each edge)
        double baseLat = minLat + (0.2 + rng.nextDouble() * 0.6) * latR;
        double baseLon = minLon + (0.2 + rng.nextDouble() * 0.6) * lonR;

        // ~10% chance this animal has drifted outside (last 2 readings outside)
        boolean hasEscape = rng.nextInt(10) == 0;
        double escapeLat = 0, escapeLon = 0;
        if (hasEscape) {
            double excess = 0.15 + rng.nextDouble() * 0.25;
            int dir = rng.nextInt(4);
            if      (dir == 0) { escapeLat = maxLat + excess * latR; escapeLon = minLon + rng.nextDouble() * lonR; }
            else if (dir == 1) { escapeLat = minLat - excess * latR; escapeLon = minLon + rng.nextDouble() * lonR; }
            else if (dir == 2) { escapeLat = minLat + rng.nextDouble() * latR; escapeLon = maxLon + excess * lonR; }
            else               { escapeLat = minLat + rng.nextDouble() * latR; escapeLon = minLon - excess * lonR; }
        }

        LocalDateTime base = LocalDateTime.now()
                .minusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);
        double lastLat = baseLat, lastLon = baseLon;
        for (int i = 0; i < 15; i++) {
            boolean outside = hasEscape && i >= 13;
            double lat, lon;
            if (outside) {
                // Readings near the escape position (small jitter so it looks like a real track)
                lat = escapeLat + jitter(stepLat);
                lon = escapeLon + jitter(stepLon);
            } else {
                // Wander inside the zone, clamped away from the edges
                lat = clamp(baseLat + jitter(stepLat), minLat + stepLat, maxLat - stepLat);
                lon = clamp(baseLon + jitter(stepLon), minLon + stepLon, maxLon - stepLon);
            }
            sensor.addReading(new GPSSensorReading(sensor, lat, lon, !outside, base.plusDays(i)));
            lastLat = lat;
            lastLon = lon;
        }
        // Sync the sensor's live state so hasEscaped() reflects the last reading
        sensor.updateLocation(lastLat, lastLon);
    }

    // ── Boundary helpers ──────────────────────────────────────────────────

    /**
     * Clean convex quadrilateral — each corner independently jittered by ±6% so the
     * shape looks like a natural field parcel without becoming concave or jagged.
     * Corner order: SW, SE, NE, NW (matches what subPolygon expects).
     */
    private GoegraphicBoundries irregularBounds(double lat1, double lon1,
                                                 double lat2, double lon2) {
        double jLat = (lat2 - lat1) * 0.06;
        double jLon = (lon2 - lon1) * 0.06;
        GoegraphicBoundries g = new GoegraphicBoundries();
        g.addPoint(lat1 + jitter(jLat), lon1 + jitter(jLon));  // SW
        g.addPoint(lat1 + jitter(jLat), lon2 + jitter(jLon));  // SE
        g.addPoint(lat2 + jitter(jLat), lon2 + jitter(jLon));  // NE
        g.addPoint(lat2 + jitter(jLat), lon1 + jitter(jLon));  // NW
        return g;
    }

    /**
     * Divides the parent zone quad into N sub-polygons by interpolating along the
     * zone's actual edges — guaranteeing every sub-field / tank stays inside the zone.
     * Expects parent points in [SW, SE, NE, NW] order (produced by irregularBounds).
     */
    private GoegraphicBoundries subPolygon(GoegraphicBoundries parent,
                                            int index, int total, boolean splitV) {
        List<double[]> pts = parent.getPoints();
        if (pts.size() < 4) return parent;

        double[] sw = pts.get(0), se = pts.get(1), ne = pts.get(2), nw = pts.get(3);

        // 1.5 % inset from zone edges + 1 % gap between adjacent sub-fields
        double inset = 0.015;
        double gap   = 0.010;
        double span  = 1.0 - 2.0 * inset;
        double t0 = inset + (double) index      / total * span + (index > 0       ? gap / 2.0 : 0.0);
        double t1 = inset + (double)(index + 1) / total * span - (index < total-1 ? gap / 2.0 : 0.0);

        GoegraphicBoundries g = new GoegraphicBoundries();
        if (splitV) {
            // Vertical strips — interpolate along the south edge (SW→SE) and north edge (NW→NE)
            double[] sL = lerp(sw, se, t0), sR = lerp(sw, se, t1);
            double[] nL = lerp(nw, ne, t0), nR = lerp(nw, ne, t1);
            g.addPoint(sL[0], sL[1]);  // SW of strip
            g.addPoint(sR[0], sR[1]);  // SE of strip
            g.addPoint(nR[0], nR[1]);  // NE of strip
            g.addPoint(nL[0], nL[1]);  // NW of strip
        } else {
            // Horizontal strips — interpolate along the west edge (SW→NW) and east edge (SE→NE)
            double[] wB = lerp(sw, nw, t0), wT = lerp(sw, nw, t1);
            double[] eB = lerp(se, ne, t0), eT = lerp(se, ne, t1);
            g.addPoint(wB[0], wB[1]);  // SW of strip
            g.addPoint(eB[0], eB[1]);  // SE of strip
            g.addPoint(eT[0], eT[1]);  // NE of strip
            g.addPoint(wT[0], wT[1]);  // NW of strip
        }
        return g;
    }

    private double[] lerp(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    // ── Alert generation ──────────────────────────────────────────────────

    private void generateAlerts(Farm farm) {
        for (var z : farm.getLivestockZones()) {
            for (BioSensor s : z.getBioSensors()) checkAlert(farm, s, AlertType.BioSensorAlert, z.getName());
            for (Animal a : z.getAnimals()) {
                if (a.isSick() || a.isQuarantined()) {
                    String desc = a.isSick() ? "illness detected" : "precautionary quarantine";
                    HealthEvent ev = new HealthEvent(a, a.getHealthStatus(),
                            AnimalHealthStatus.Healthy, null, desc);
                    a.addHealthEvent(ev);
                    AlertSeverity sev = a.isSick() ? AlertSeverity.Critical : AlertSeverity.Warning;
                    farm.registerAlert(new HealthAlert(ev, AlertType.HEALTH_ALERT, sev,
                            a.getName() + " — " + desc + " in " + z.getName()));
                }
            }
            for (var gps : z.getGpsCollarSensors()) {
                if (gps.hasEscaped()) {
                    var last = gps.getLastReading();
                    if (last != null)
                        farm.registerAlert(new SensorAlert(last, AlertType.GPS_ESCAPE_ALERT,
                                AlertSeverity.Critical,
                                gps.getAnimal().getName() + " outside zone bounds in " + z.getName()
                                + String.format(" (%.5f, %.5f)", last.getLat(), last.getLon())));
                }
            }
        }
        for (var z : farm.getCropZones()) {
            for (EnvSensor  s : z.getEnvSensors())  checkAlert(farm, s, AlertType.EnvSensorAlert,  z.getName());
            for (SoilSensor s : z.getSoilSensors()) checkAlert(farm, s, AlertType.SoilSensorAlert, z.getName());
        }
        for (var z : farm.getAquacultureZones())
            for (WaterSensor s : z.getWaterSensors()) checkAlert(farm, s, AlertType.WaterSensorAlert, z.getName());
    }

    private void checkAlert(Farm farm, NumericSensor s, AlertType type, String zoneName) {
        NumericSensorReading last = lastReading(s);
        if (last != null && s.isOutOfRange(last.getValue())) {
            AlertSeverity sev = last.getSeverity() == ReadingLevel.CRITICAL
                    ? AlertSeverity.Critical : AlertSeverity.Warning;
            farm.registerAlert(new SensorAlert(last, type, sev,
                    String.format("Sensor %s out of range: %.2f %s in %s",
                            s.getCode(), last.getValue(), s.getUnit(), zoneName)));
        }
    }

    // ── Scaling helpers ───────────────────────────────────────────────────

    private double boundingBoxArea(Object z) {
        GoegraphicBoundries b = null;
        if (z instanceof LivestockZONE lz && lz.hasBoundaries()) b = lz.getBoundaries();
        else if (z instanceof CropZONE cz && cz.hasBoundaries()) b = cz.getBoundaries();
        else if (z instanceof AquacultureZONE az && az.hasBoundaries()) b = az.getBoundaries();
        if (b == null) return 0.00015;
        List<double[]> pts = b.getPoints();
        double minLat = pts.stream().mapToDouble(p -> p[0]).min().orElse(0);
        double maxLat = pts.stream().mapToDouble(p -> p[0]).max().orElse(0);
        double minLon = pts.stream().mapToDouble(p -> p[1]).min().orElse(0);
        double maxLon = pts.stream().mapToDouble(p -> p[1]).max().orElse(0);
        return (maxLat - minLat) * (maxLon - minLon);
    }

    private int animalCount(double area, LIvestockType type) {
        int base = clampInt((int)(area * 120_000), 6, 250);
        int count = base + rng.nextInt(Math.max(1, base / 4));
        if (type == LIvestockType.POULTRY) count = count * 6 + rng.nextInt(20);
        return clampInt(count, 5, 500);
    }

    private int subCount(double area, int min, int max) {
        return clampInt(min + (int)((area / 0.00020) * (max - min)), min, max);
    }

    private boolean splitVertical(Object z) {
        GoegraphicBoundries b = null;
        if (z instanceof CropZONE cz && cz.hasBoundaries()) b = cz.getBoundaries();
        else if (z instanceof AquacultureZONE az && az.hasBoundaries()) b = az.getBoundaries();
        if (b == null) return rng.nextBoolean();
        List<double[]> pts = b.getPoints();
        double latR = pts.stream().mapToDouble(p -> p[0]).max().orElse(0.01)
                    - pts.stream().mapToDouble(p -> p[0]).min().orElse(0);
        double lonR = pts.stream().mapToDouble(p -> p[1]).max().orElse(0.01)
                    - pts.stream().mapToDouble(p -> p[1]).min().orElse(0);
        return lonR > latR;
    }

    private long deriveSeed(String farmId) {
        // Mix UUID hex into a 64-bit seed — deterministic across JVM restarts
        String hex = farmId.replace("-", "");
        long hi = Long.parseUnsignedLong(hex.substring(0, 16), 16);
        long lo = Long.parseUnsignedLong(hex.substring(16, 32), 16);
        return hi ^ (lo * 0x9e3779b97f4a7c15L);
    }

    private double avgFishWeightKg(String name) {
        return switch (name) {
            case "Salmon"  -> 3.0 + rng.nextDouble() * 2.0;   // 3–5 kg
            case "Trout"   -> 1.5 + rng.nextDouble() * 2.0;   // 1.5–3.5 kg
            case "Catfish" -> 1.0 + rng.nextDouble() * 2.0;   // 1–3 kg
            case "Carp"    -> 0.8 + rng.nextDouble() * 2.5;   // 0.8–3.3 kg
            case "Bass"    -> 0.6 + rng.nextDouble() * 1.5;   // 0.6–2.1 kg
            case "Pike"    -> 1.5 + rng.nextDouble() * 3.0;   // 1.5–4.5 kg
            case "Perch"   -> 0.3 + rng.nextDouble() * 0.7;   // 0.3–1 kg
            case "Bream"   -> 0.3 + rng.nextDouble() * 0.8;   // 0.3–1.1 kg
            case "Tilapia" -> 0.4 + rng.nextDouble() * 0.8;   // 0.4–1.2 kg
            case "Mullet"  -> 0.4 + rng.nextDouble() * 0.6;   // 0.4–1 kg
            default        -> 0.5 + rng.nextDouble() * 1.5;   // 0.5–2 kg
        };
    }

    // ── Low-level helpers ─────────────────────────────────────────────────

    private NumericSensorReading lastReading(NumericSensor sensor) {
        var h = sensor.getReadingHistory();
        for (int i = h.size() - 1; i >= 0; i--)
            if (h.get(i) instanceof NumericSensorReading nr) return nr;
        return null;
    }

    private String[][] shuffled(String[][] src) {
        String[][] copy = src.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            String[] tmp = copy[i]; copy[i] = copy[j]; copy[j] = tmp;
        }
        return copy;
    }

    private double jitter(double scale)              { return (rng.nextDouble() - 0.5) * 2 * scale; }
    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private int    clampInt(int v, int lo, int hi)   { return Math.max(lo, Math.min(hi, v)); }
    private double round2(double v)                  { return Math.round(v * 100.0) / 100.0; }
}
