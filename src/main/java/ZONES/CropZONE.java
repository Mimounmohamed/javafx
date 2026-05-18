package ZONES;
import Entities.Crop;
import Sensors.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CropZONE extends ZONE {
    private List<Crop> fields;
    private List<EnvSensor> envSensors;
    private List<SoilSensor> soilSensors;
    private double surfacePlanted;

    public CropZONE(String name) {
        super(name);
        this.fields = new ArrayList<>();
        this.envSensors = new ArrayList<>();
        this.soilSensors = new ArrayList<>();
        this.surfacePlanted = 0.0;
    }

    public void addField(Crop crop) {
        fields.add(crop);
    }

    public void removeField(Crop crop) {
        fields.remove(crop);
    }

    public void clearFields()     { fields.clear(); }
    public void clearEnvSensors() { envSensors.clear(); }
    public void clearSoilSensors(){ soilSensors.clear(); }

    public List<Crop> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void addEnvSensor(EnvSensor sensor) {
        envSensors.add(sensor);
    }

    public void removeEnvSensor(EnvSensor sensor) {
        envSensors.remove(sensor);
    }

    public void addSoilSensor(SoilSensor sensor) {
        soilSensors.add(sensor);
    }

    public void removeSoilSensor(SoilSensor sensor) {
        soilSensors.remove(sensor);
    }

    public List<EnvSensor> getEnvSensors()   { return Collections.unmodifiableList(envSensors); }
    public List<SoilSensor> getSoilSensors() { return Collections.unmodifiableList(soilSensors); }

    public double getTotalCropYield() {
        return fields.stream().mapToDouble(Entities.Crop::getYieldKg).sum();
    }

    public double getSurfacePlanted() { return surfacePlanted; }
    public void setSurfacePlanted(double surface) { this.surfacePlanted = surface; }

    public List<SensorReading> getAllEnvReadings() {
        List<SensorReading> all = new ArrayList<>();
        for (EnvSensor s : envSensors) all.addAll(s.getReadingHistory());
        return Sensor.sortByTimestamp(all);
    }

    public List<SensorReading> getEnvReadings(LocalDateTime from, LocalDateTime to) {
        return Sensor.filterByPeriod(getAllEnvReadings(), from, to);
    }

    public List<NumericSensorReading> getEnvReadings(ReadingLevel level) {
        List<NumericSensorReading> nums = new ArrayList<>();
        for (SensorReading r : getAllEnvReadings())
            if (r instanceof NumericSensorReading nr) nums.add(nr);
        return NumericSensor.filterByLevel(nums, level);
    }

    public List<SensorReading> getAllSoilReadings() {
        List<SensorReading> all = new ArrayList<>();
        for (SoilSensor s : soilSensors) all.addAll(s.getReadingHistory());
        return Sensor.sortByTimestamp(all);
    }

    public List<SensorReading> getSoilReadings(LocalDateTime from, LocalDateTime to) {
        return Sensor.filterByPeriod(getAllSoilReadings(), from, to);
    }

    public List<NumericSensorReading> getSoilReadings(ReadingLevel level) {
        List<NumericSensorReading> nums = new ArrayList<>();
        for (SensorReading r : getAllSoilReadings())
            if (r instanceof NumericSensorReading nr) nums.add(nr);
        return NumericSensor.filterByLevel(nums, level);
    }
}
