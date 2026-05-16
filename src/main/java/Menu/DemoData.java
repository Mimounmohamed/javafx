package Menu;

import Additional_classes.Range;
import Alerts.Alert;
import Alerts.AlertSeverity;
import Alerts.AlertType;
import Alerts.HealthAlert;
import Alerts.SensorAlert;
import Animals.*;
import Entities.*;
import Farm.Farm;
import Sensors.BioMeasureType;
import Sensors.BioSensor;
import Sensors.EnvMeasureType;
import Sensors.EnvSensor;
import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.SoilMeasureType;
import Sensors.SoilSensor;
import Sensors.WaterMeasureType;
import Sensors.WaterSensor;
import ZONES.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

public class DemoData {

    public static Farm create() {
        Farm farm = new Farm("Green Valley Farm", "Algiers, Algeria", "Prof Demo");

        // ── Zone 1: Livestock ────────────────────────────────────────────
        GoegraphicBoundries bounds = GoegraphicBoundries.createRectangle(
                36.700, 3.050, 36.710, 3.065);
        LivestockZONE northPasture = new LivestockZONE("North Pasture", LIvestockType.RUMINANT, bounds);

        // Animals
        Animal bessie = new Animal("Bovine", "Bessie", LIvestockType.RUMINANT, 4, 480.0, northPasture);
        Animal rex    = new Animal("Bovine", "Rex",    LIvestockType.RUMINANT, 3, 420.0, northPasture);
        Animal luna   = new Animal("Ovine",  "Luna",   LIvestockType.RUMINANT, 2, 65.0,  northPasture);
        bessie.recordMilkYield(32.5);
        rex.recordMilkYield(18.0);

        northPasture.addAnimal(bessie);
        northPasture.addAnimal(rex);
        northPasture.addAnimal(luna);

        // Health events
        HealthEvent rexSick = new HealthEvent(rex, AnimalHealthStatus.Sick,
                AnimalHealthStatus.Healthy, null, "Respiratory infection detected");
        rex.addHealthEvent(rexSick);

        HealthEvent lunaQuarantine = new HealthEvent(luna, AnimalHealthStatus.Quarantined,
                AnimalHealthStatus.Healthy, null, "Precautionary isolation — possible parasite exposure");
        luna.addHealthEvent(lunaQuarantine);

        // Feeding program
        FeedingProgram fp = new FeedingProgram(
                "Hay + Grain", 25.0,
                List.of("07:00", "12:30", "18:00"),
                LocalTime.of(6, 0), LocalTime.of(20, 0));
        northPasture.setFeedingProgram(fp);

        // Bio sensors — Bessie (Temperature: 15 normal, trend normal→critical→normal)
        BioSensor bessieTemp = new BioSensor(bessie, BioMeasureType.Temperature, 38.0, 39.5);
        bessie.attachBioSensor(bessieTemp);
        northPasture.addBioSensor(bessieTemp);
        double[] bessieTemps = {38.2, 38.4, 38.5, 38.3, 38.6, 38.7, 38.8, 39.0, 39.2,
                                39.4, 40.1, 40.8, 41.0, 39.3, 38.5};
        injectNumericReadings(bessieTemp, bessieTemps);

        BioSensor bessieActivity = new BioSensor(bessie, BioMeasureType.ActivityLevel, 40.0, 120.0);
        bessie.attachBioSensor(bessieActivity);
        northPasture.addBioSensor(bessieActivity);
        double[] bessieActs = {85, 90, 88, 92, 87, 95, 100, 102, 98, 88, 42, 35, 28, 75, 88};
        injectNumericReadings(bessieActivity, bessieActs);

        // Bio sensors — Rex (critical high temp throughout)
        BioSensor rexTemp = new BioSensor(rex, BioMeasureType.Temperature, 38.0, 39.5);
        rex.attachBioSensor(rexTemp);
        northPasture.addBioSensor(rexTemp);
        double[] rexTemps = {38.5, 38.8, 39.0, 39.3, 39.8, 40.2, 40.5, 40.9, 41.1,
                             41.3, 41.0, 40.7, 40.4, 40.1, 39.8};
        injectNumericReadings(rexTemp, rexTemps);

        // GPS sensors
        GPSCollarSensor bessieGPS = new GPSCollarSensor(bessie);
        bessie.attachGPSCollar(bessieGPS);
        northPasture.addGpsCollarSensor(bessieGPS);
        injectGPSReadings(bessieGPS, 36.705, 3.057, true);  // all inside

        GPSCollarSensor rexGPS = new GPSCollarSensor(rex);
        rex.attachGPSCollar(rexGPS);
        northPasture.addGpsCollarSensor(rexGPS);
        injectGPSReadings(rexGPS, 36.712, 3.070, false);    // last 2 outside (escape)

        // Health alerts
        HealthAlert rexAlert = new HealthAlert(rexSick, AlertType.HEALTH_ALERT,
                AlertSeverity.Critical, "Rex — critical temperature spike, respiratory infection");
        farm.registerAlert(rexAlert);

        HealthAlert lunaAlert = new HealthAlert(lunaQuarantine, AlertType.HEALTH_ALERT,
                AlertSeverity.Warning, "Luna — quarantined, monitor for parasite symptoms");
        farm.registerAlert(lunaAlert);

        farm.addZone(northPasture);

        // ── Zone 2: Crop ──────────────────────────────────────────────────
        CropZONE southFields = new CropZONE("South Fields");
        southFields.setSurfacePlanted(12.5);

        Date planted  = new Date(System.currentTimeMillis() - 60L * 86400_000);
        Date harvest  = new Date(System.currentTimeMillis() + 90L * 86400_000);
        Date harvest2 = new Date(System.currentTimeMillis() - 10L * 86400_000);

        Crop wheat = new Crop(CropType.cereals, "Anza",
                planted, harvest,
                new Range(6.0, 7.5), new Range(50, 70), southFields);
        wheat.updateGrowthStage(GrowthStage.maturity);

        Crop corn = new Crop(CropType.cereals, "DKC 63-84",
                planted, harvest2,
                new Range(5.8, 7.2), new Range(55, 75), southFields);
        corn.updateGrowthStage(GrowthStage.harvest);
        corn.recordHarvest(3800.0);

        Crop tomato = new Crop(CropType.vegetables, "Roma",
                new Date(System.currentTimeMillis() - 15L * 86400_000), harvest,
                new Range(6.0, 7.0), new Range(60, 80), southFields);
        tomato.updateGrowthStage(GrowthStage.germination);

        southFields.addField(wheat);
        southFields.addField(corn);
        southFields.addField(tomato);

        // Env sensors
        EnvSensor envTemp = new EnvSensor(southFields, EnvMeasureType.Temperature, 15.0, 30.0, "°C");
        EnvSensor envHum  = new EnvSensor(southFields, EnvMeasureType.Humidity,    40.0, 80.0, "%");
        southFields.addEnvSensor(envTemp);
        southFields.addEnvSensor(envHum);
        double[] temps = {18.0, 19.5, 21.0, 22.5, 23.0, 24.5, 25.0, 26.5, 28.0,
                          29.5, 31.0, 32.5, 33.0, 27.0, 24.0};
        double[] hums  = {55.0, 57.0, 60.0, 62.0, 65.0, 68.0, 70.0, 72.0, 75.0,
                          78.0, 82.0, 85.0, 88.0, 74.0, 66.0};
        injectNumericReadings(envTemp, temps);
        injectNumericReadings(envHum,  hums);

        // Soil sensors
        SoilSensor soilPH   = new SoilSensor(southFields, SoilMeasureType.PH,       6.0, 7.5, "pH");
        SoilSensor soilMois = new SoilSensor(southFields, SoilMeasureType.Moisture,  50.0, 70.0, "%");
        southFields.addSoilSensor(soilPH);
        southFields.addSoilSensor(soilMois);
        double[] phs  = {6.8, 6.9, 7.0, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.8, 8.0, 8.3, 8.5, 7.9, 7.2};
        double[] mois = {62.0, 64.0, 65.0, 66.0, 67.0, 68.0, 70.0, 71.0, 73.0, 75.0, 78.0, 42.0, 38.0, 55.0, 63.0};
        injectNumericReadings(soilPH,   phs);
        injectNumericReadings(soilMois, mois);

        // Soil pH critical alert (last reading was 8.5 pH)
        NumericSensorReading phReading = (NumericSensorReading)
                soilPH.getReadingHistory().get(soilPH.getReadingHistory().size() - 4);
        SensorAlert soilAlert = new SensorAlert(phReading, AlertType.SoilSensorAlert,
                AlertSeverity.Critical, "Soil pH critically high (8.5) in South Fields — wheat crop at risk");
        farm.registerAlert(soilAlert);

        farm.addZone(southFields);

        // ── Zone 3: Aquaculture ──────────────────────────────────────────
        AquacultureZONE pondAlpha = new AquacultureZONE("Pond Alpha");

        AquacultureSpecies tilapia = new AquacultureSpecies("Tilapia", 800, pondAlpha);
        tilapia.recordMortality(12);
        tilapia.harvest(120.5, 60);

        AquacultureSpecies salmon = new AquacultureSpecies("Salmon", 300, pondAlpha);
        salmon.harvest(85.0, 25);

        pondAlpha.addSpecies(tilapia);
        pondAlpha.addSpecies(salmon);

        // Water sensors
        WaterSensor waterTemp = new WaterSensor(pondAlpha, WaterMeasureType.Temperature,      18.0, 26.0, "°C");
        WaterSensor waterO2   = new WaterSensor(pondAlpha, WaterMeasureType.DissolvedOxygen,   5.0, 10.0, "mg/L");
        pondAlpha.addWaterSensor(waterTemp);
        pondAlpha.addWaterSensor(waterO2);
        double[] wTemps = {22.0, 22.5, 23.0, 23.5, 24.0, 24.5, 25.0, 25.5, 26.0,
                           26.5, 27.0, 27.5, 28.0, 25.0, 23.0};
        double[] wO2    = {8.5, 8.3, 8.0, 7.8, 7.5, 7.2, 6.8, 6.5, 6.0,
                           5.5, 4.8, 4.2, 3.8, 5.5, 7.0};
        injectNumericReadings(waterTemp, wTemps);
        injectNumericReadings(waterO2,   wO2);

        // Critical O2 alert (reading that dropped to 3.8 mg/L)
        NumericSensorReading o2Reading = (NumericSensorReading)
                waterO2.getReadingHistory().get(waterO2.getReadingHistory().size() - 3);
        SensorAlert o2Alert = new SensorAlert(o2Reading, AlertType.WaterSensorAlert,
                AlertSeverity.Critical, "Dissolved O2 critically low (3.8 mg/L) in Pond Alpha — fish at risk");
        farm.registerAlert(o2Alert);

        farm.addZone(pondAlpha);

        return farm;
    }

    private static void injectNumericReadings(NumericSensor sensor, double[] values) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(values.length - 1)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < values.length; i++) {
            NumericSensorReading r = new NumericSensorReading(
                    sensor, values[i], sensor.getUnit(), base.plusDays(i));
            sensor.addReading(r);
        }
        sensor.setLastValue(values[values.length - 1]);
    }

    private static void injectGPSReadings(GPSCollarSensor sensor,
                                          double baseLat, double baseLon,
                                          boolean allInside) {
        LocalDateTime base = LocalDateTime.now()
                .minusDays(14)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 15; i++) {
            boolean inside = allInside || (i < 13);
            double lat = baseLat + (i * 0.0001);
            double lon = baseLon + (i * 0.0001);
            GPSSensorReading r = new GPSSensorReading(
                    sensor, lat, lon, inside, base.plusDays(i));
            sensor.addReading(r);
        }
    }
}
