package com.example.services;

import Sensors.BioSensor;
import Sensors.EnvSensor;
import Sensors.GPSCollarSensor;
import Sensors.GPSSensorReading;
import Sensors.NumericSensor;
import Sensors.NumericSensorReading;
import Sensors.ReadingLevel;
import Sensors.Sensor;
import Sensors.SensorReading;
import Sensors.SensorStatus;
import Sensors.SoilSensor;
import Sensors.WaterSensor;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;

import java.util.ArrayList;
import java.util.List;

public class SensorService {

    private static SensorService instance;

    private SensorService() {}

    public static SensorService getInstance() {
        if (instance == null) instance = new SensorService();
        return instance;
    }

    private static FarmService fs() { return FarmService.getInstance(); }

    public List<Sensor> getAllSensors() {
        List<Sensor> all = new ArrayList<>();
        for (LivestockZONE z : fs().getFarm().getLivestockZones()) {
            all.addAll(z.getBioSensors());
            all.addAll(z.getGpsCollarSensors());
        }
        for (CropZONE z : fs().getFarm().getCropZones()) {
            all.addAll(z.getEnvSensors());
            all.addAll(z.getSoilSensors());
        }
        for (AquacultureZONE z : fs().getFarm().getAquacultureZones())
            all.addAll(z.getWaterSensors());
        return all;
    }

    public List<BioSensor> getAllBioSensors() {
        List<BioSensor> r = new ArrayList<>();
        fs().getFarm().getLivestockZones().forEach(z -> r.addAll(z.getBioSensors()));
        return r;
    }

    public List<GPSCollarSensor> getAllGPSSensors() {
        List<GPSCollarSensor> r = new ArrayList<>();
        fs().getFarm().getLivestockZones().forEach(z -> r.addAll(z.getGpsCollarSensors()));
        return r;
    }

    public List<EnvSensor> getAllEnvSensors() {
        List<EnvSensor> r = new ArrayList<>();
        fs().getFarm().getCropZones().forEach(z -> r.addAll(z.getEnvSensors()));
        return r;
    }

    public List<SoilSensor> getAllSoilSensors() {
        List<SoilSensor> r = new ArrayList<>();
        fs().getFarm().getCropZones().forEach(z -> r.addAll(z.getSoilSensors()));
        return r;
    }

    public List<WaterSensor> getAllWaterSensors() {
        List<WaterSensor> r = new ArrayList<>();
        fs().getFarm().getAquacultureZones().forEach(z -> r.addAll(z.getWaterSensors()));
        return r;
    }

    public int getActiveSensorCount() {
        return (int) getAllSensors().stream()
            .filter(s -> s.getStatus() == SensorStatus.Active)
            .count();
    }

    public ReadingLevel getLastReadingLevel(Sensor sensor) {
        if (sensor instanceof GPSCollarSensor gps) {
            GPSSensorReading last = gps.getLastReading();
            return (last != null && !last.isInsideZone()) ? ReadingLevel.CRITICAL : ReadingLevel.NORMAL;
        }
        List<SensorReading> history = sensor.getReadingHistory();
        if (history.isEmpty()) return ReadingLevel.NORMAL;
        SensorReading last = history.get(history.size() - 1);
        return (last instanceof NumericSensorReading nr) ? nr.getSeverity() : ReadingLevel.NORMAL;
    }

    public String getSensorTypeLabel(Sensor s) {
        if (s instanceof BioSensor b)         return "Bio · " + b.getMeasureType();
        if (s instanceof GPSCollarSensor g)   return "GPS · " + g.getAnimal().getName();
        if (s instanceof EnvSensor e)         return "Env · " + e.getMeasureType();
        if (s instanceof SoilSensor so)       return "Soil · " + so.getMeasureType();
        if (s instanceof WaterSensor w)       return "Water · " + w.getMeasureType();
        return s.getClass().getSimpleName();
    }

    public String getLastReadingDisplay(Sensor s) {
        if (s instanceof GPSCollarSensor gps) {
            GPSSensorReading r = gps.getLastReading();
            if (r == null) return "No data";
            return String.format("%.4f°, %.4f° [%s]",
                r.getLat(), r.getLon(), r.isInsideZone() ? "INSIDE" : "OUTSIDE");
        }
        if (s instanceof NumericSensor ns) {
            double v = ns.getLastValue();
            return Double.isNaN(v) ? "No data" : String.format("%.2f %s", v, ns.getUnit());
        }
        return "No data";
    }
}
