package Animals;
import Entities.LIvestockType;
import Sensors.BioSensor;
import Sensors.GPSCollarSensor;
import ZONES.ZONE;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

public class Animal {

    public static class WeightRecord {
        private final double weight;
        private final LocalDateTime timestamp;

        WeightRecord(double weight) {
            this.weight = weight;
            this.timestamp = LocalDateTime.now();
        }

        WeightRecord(double weight, LocalDateTime timestamp) {
            this.weight = weight;
            this.timestamp = timestamp;
        }

        public double getWeight()            { return weight; }
        public LocalDateTime getTimestamp()  { return timestamp; }

        @Override
        public String toString() {
            return timestamp.toLocalDate() + " " + timestamp.toLocalTime().toString().substring(0, 5) + " — " + weight + " kg";
        }
    }

    public static class MilkRecord {
        private final double liters;
        private final LocalDateTime timestamp;

        MilkRecord(double liters) {
            this.liters = liters;
            this.timestamp = LocalDateTime.now();
        }

        MilkRecord(double liters, LocalDateTime timestamp) {
            this.liters = liters;
            this.timestamp = timestamp;
        }

        public double getLiters()            { return liters; }
        public LocalDateTime getTimestamp()  { return timestamp; }

        @Override
        public String toString() {
            return timestamp.toLocalDate() + " " + timestamp.toLocalTime().toString().substring(0, 5) + " — " + String.format("%.2f L", liters);
        }
    }

    public static class EggRecord {
        private final int count;
        private final LocalDateTime timestamp;

        EggRecord(int count) {
            this.count = count;
            this.timestamp = LocalDateTime.now();
        }

        EggRecord(int count, LocalDateTime timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }

        public int getCount()                { return count; }
        public LocalDateTime getTimestamp()  { return timestamp; }

        @Override
        public String toString() {
            return timestamp.toLocalDate() + " " + timestamp.toLocalTime().toString().substring(0, 5) + " — " + count + " eggs";
        }
    }

    private String id;
    private String name;
    private String species;
    private LIvestockType type;
    private int age;
    private double weight;
    private AnimalHealthStatus healthStatus;
    private boolean hasGPSCollar;
    private GPSCollarSensor gpsCollarSensor;
    private ZONE zone;
    private List<HealthEvent> healthHistory;
    private List<BioSensor> bioSensors = new ArrayList<>();
    private double milkYieldLiters;
    private int eggCount;
    private List<WeightRecord> weightHistory  = new ArrayList<>();
    private List<MilkRecord>   milkHistory    = new ArrayList<>();
    private List<EggRecord>    eggHistory     = new ArrayList<>();


    public Animal(String species, String name, LIvestockType type, int age, double weight, ZONE zone) {
        this.id = UUID.randomUUID().toString();
        this.species = species;
        this.name = name;
        this.type = type;
        this.age = age;
        this.weight = weight;
        this.healthStatus = AnimalHealthStatus.Healthy;
        this.hasGPSCollar = false;
        this.gpsCollarSensor = null;
        this.zone = zone;
        this.healthHistory = new ArrayList<>();
        this.weightHistory.add(new WeightRecord(weight));
    }

    public void attachGPSCollar(GPSCollarSensor gps) {
        this.gpsCollarSensor = gps;
        this.hasGPSCollar = true;
    }

    public void attachBioSensor(BioSensor sensor) {
        bioSensors.add(sensor);
    }

    public void removeGPSCollar() {
        this.gpsCollarSensor = null;
        this.hasGPSCollar = false;
    }

    public void updateWeight(double w) {
        this.weight = w;
        this.weightHistory.add(new WeightRecord(w));
    }

    public void updateWeight(double w, LocalDateTime timestamp) {
        this.weight = w;
        this.weightHistory.add(new WeightRecord(w, timestamp));
    }

    public void setHealthStatus(AnimalHealthStatus h) {
        this.healthStatus = h;
    }

    public void addHealthEvent(HealthEvent event) {
        healthHistory.add(event);
        this.healthStatus = event.getEventType();
    }

    public void resolveLastHealthEvent(AnimalHealthStatus statusAfter) {
        if (healthHistory.isEmpty())
            throw new IllegalStateException(name + " has no health events to resolve");
        for (int i = healthHistory.size() - 1; i >= 0; i--) {
            if (!healthHistory.get(i).isResolved()) {
                healthHistory.get(i).markResolved(statusAfter);
                this.healthStatus = statusAfter;
                return;
            }
        }
        throw new IllegalStateException("No unresolved health events found for " + name);
    }

    public List<HealthEvent> getHealthHistory()  { return Collections.unmodifiableList(healthHistory); }

    public List<HealthEvent> getUnresolvedEvents() {
        List<HealthEvent> unresolved = new ArrayList<>();
        for (HealthEvent e : healthHistory) {
            if (!e.isResolved()) unresolved.add(e);
        }
        return unresolved;
    }

    public boolean isSick()        { return healthStatus == AnimalHealthStatus.Sick; }
    public boolean isQuarantined() { return healthStatus == AnimalHealthStatus.Quarantined; }

    public String getId()                          { return id; }
    public String getSpecies()                     { return species; }
    public LIvestockType getType()                 { return type; }
    public int getAge()                            { return age; }
    public double getWeight()                      { return weight; }
    public AnimalHealthStatus getHealthStatus()    { return healthStatus; }
    public boolean hasGPSCollar()                  { return hasGPSCollar; }
    public GPSCollarSensor getGpsCollarSensor()    { return gpsCollarSensor; }
    public ZONE getZone()                          { return zone; }
    public String getName()                        { return name; }
    public List<BioSensor> getBioSensors()         { return Collections.unmodifiableList(bioSensors); }
    public List<WeightRecord> getWeightHistory()   { return Collections.unmodifiableList(weightHistory); }
    public List<MilkRecord>   getMilkHistory()     { return Collections.unmodifiableList(milkHistory); }
    public List<EggRecord>    getEggHistory()      { return Collections.unmodifiableList(eggHistory); }

    public void recordMilkYield(double liters) {
        if (liters < 0) throw new IllegalArgumentException("liters cannot be negative");
        this.milkYieldLiters += liters;
        this.milkHistory.add(new MilkRecord(liters));
    }

    public void recordMilkYield(double liters, LocalDateTime timestamp) {
        if (liters < 0) throw new IllegalArgumentException("liters cannot be negative");
        this.milkYieldLiters += liters;
        this.milkHistory.add(new MilkRecord(liters, timestamp));
    }

    public void recordEgg(int count) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
        this.eggCount += count;
        this.eggHistory.add(new EggRecord(count));
    }

    public void recordEgg(int count, LocalDateTime timestamp) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
        this.eggCount += count;
        this.eggHistory.add(new EggRecord(count, timestamp));
    }

    public void resetProductionStats() {
        this.milkYieldLiters = 0;
        this.eggCount = 0;
        this.milkHistory.clear();
        this.eggHistory.clear();
    }

    public double getMilkYieldLiters() { return milkYieldLiters; }
    public int    getEggCount()        { return eggCount; }
}
