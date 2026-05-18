package ZONES;
import Animals.FeedingProgram;
import Animals.Animal;
import Entities.LIvestockType;
import Sensors.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class LivestockZONE extends ZONE {
    private List<Animal> animals;
    private List<BioSensor> bioSensors;
    private List<GPSCollarSensor> gpsCollarSensors;
    private FeedingProgram feedingProgram;
    private LIvestockType type;

    public LivestockZONE(String name, LIvestockType type, GoegraphicBoundries boundries) {
        super(name, boundries);
        if (boundries == null) throw new IllegalArgumentException("LivestockZONE requires geographic boundaries");
        this.type = type;
        this.animals = new ArrayList<>();
        this.bioSensors = new ArrayList<>();
        this.gpsCollarSensors = new ArrayList<>();
    }

    /** Constructor without geographic boundaries — for UI-created zones. */
    public LivestockZONE(String name, LIvestockType type) {
        super(name);
        this.type = type;
        this.animals = new ArrayList<>();
        this.bioSensors = new ArrayList<>();
        this.gpsCollarSensors = new ArrayList<>();
    }

    public boolean contains(double lat, double lon) {
        return super.contains(lat, lon);
    }

    public void addAnimal(Animal animal) {
        animals.add(animal);
    }

    public void removeAnimal(Animal animal) {
        animals.remove(animal);
    }

    public void clearAnimals() { animals.clear(); }

    public List<Animal> getAnimals() {
        return Collections.unmodifiableList(animals);
    }

    public void setFeedingProgram(FeedingProgram feedingProgram) {
        this.feedingProgram = feedingProgram;
    }

    public FeedingProgram getFeedingProgram() {
        return feedingProgram;
    }

    public void addBioSensor(BioSensor sensor) {
        bioSensors.add(sensor);
    }

    public void removeBioSensor(BioSensor sensor) {
        bioSensors.remove(sensor);
    }

    public void clearBioSensors()       { bioSensors.clear(); }

    public List<BioSensor> getBioSensors() {
        return Collections.unmodifiableList(bioSensors);
    }

    public void addGpsCollarSensor(GPSCollarSensor sensor) {
        gpsCollarSensors.add(sensor);
    }

    public void removeGpsCollarSensor(GPSCollarSensor sensor) {
        gpsCollarSensors.remove(sensor);
    }

    public void clearGpsCollarSensors() { gpsCollarSensors.clear(); }

    public List<GPSCollarSensor> getGpsCollarSensors() {
        return Collections.unmodifiableList(gpsCollarSensors);
    }

    public LIvestockType getType() { return type; }

    public List<SensorReading> getAllBioReadings() {
        List<SensorReading> all = new ArrayList<>();
        for (BioSensor s : bioSensors) all.addAll(s.getReadingHistory());
        return Sensor.sortByTimestamp(all);
    }

    public List<SensorReading> getBioReadings(LocalDateTime from, LocalDateTime to) {
        return Sensor.filterByPeriod(getAllBioReadings(), from, to);
    }

    public List<NumericSensorReading> getBioReadings(ReadingLevel level) {
        List<NumericSensorReading> nums = new ArrayList<>();
        for (SensorReading r : getAllBioReadings())
            if (r instanceof NumericSensorReading nr) nums.add(nr);
        return NumericSensor.filterByLevel(nums, level);
    }

    public List<SensorReading> getAllGPSReadings() {
        List<SensorReading> all = new ArrayList<>();
        for (GPSCollarSensor s : gpsCollarSensors) all.addAll(s.getReadingHistory());
        return Sensor.sortByTimestamp(all);
    }

    public List<SensorReading> getGPSReadings(LocalDateTime from, LocalDateTime to) {
        return Sensor.filterByPeriod(getAllGPSReadings(), from, to);
    }

    public double getTotalMilkYield() {
        return animals.stream().mapToDouble(Animal::getMilkYieldLiters).sum();
    }

    public int getTotalEggCount() {
        return animals.stream().mapToInt(Animal::getEggCount).sum();
    }

    @Override
    public String toString() {
        return "LivestockZONE{" +
                "code=" + getCode() +
                ", name=" + getName() +
                ", type=" + type +
                ", animals=" + animals.size() +
                ", status=" + getStatus() +
                '}';
    }
}
