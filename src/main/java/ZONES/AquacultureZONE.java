package ZONES;
import Entities.AquacultureSpecies;
import Sensors.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class AquacultureZONE extends ZONE {
    private List<AquacultureSpecies> speciesList;
    private List<WaterSensor> waterSensors;

    public AquacultureZONE(String name) {
        super(name);
        this.speciesList = new ArrayList<>();
        this.waterSensors = new ArrayList<>();
    }

    public void addSpecies(AquacultureSpecies species) {
        speciesList.add(species);
    }

    public void removeSpecies(AquacultureSpecies species) {
        speciesList.remove(species);
    }

    public List<AquacultureSpecies> getSpeciesList() {
        return Collections.unmodifiableList(speciesList);
    }

    public void addWaterSensor(WaterSensor sensor) {
        waterSensors.add(sensor);
    }

    public List<WaterSensor> getWaterSensors() {
        return Collections.unmodifiableList(waterSensors);
    }

    public double getTotalHarvestWeight() {
        return speciesList.stream().mapToDouble(AquacultureSpecies::getTotalHarvestWeightKg).sum();
    }

    public int getTotalSpeciesCount() {
        return speciesList.stream().mapToInt(AquacultureSpecies::getNumSpecies).sum();
    }

    public List<SensorReading> getAllWaterReadings() {
        List<SensorReading> all = new ArrayList<>();
        for (WaterSensor s : waterSensors) all.addAll(s.getReadingHistory());
        return Sensor.sortByTimestamp(all);
    }

    public List<SensorReading> getWaterReadings(LocalDateTime from, LocalDateTime to) {
        return Sensor.filterByPeriod(getAllWaterReadings(), from, to);
    }

    public List<NumericSensorReading> getWaterReadings(ReadingLevel level) {
        List<NumericSensorReading> nums = new ArrayList<>();
        for (SensorReading r : getAllWaterReadings())
            if (r instanceof NumericSensorReading nr) nums.add(nr);
        return NumericSensor.filterByLevel(nums, level);
    }
}
