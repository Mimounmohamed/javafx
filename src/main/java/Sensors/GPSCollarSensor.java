package Sensors;
import Animals.Animal;
import ZONES.LivestockZONE;

public class GPSCollarSensor extends Sensor{
    private Animal animal;
    private double currentLatitude;
    private double currentLongitude;
    private boolean isInsideZone;

    public GPSCollarSensor(Animal animal) {
        super(animal.getZone());
        this.animal = animal;
        this.currentLatitude = 0.0;
        this.currentLongitude = 0.0;
        this.isInsideZone = true; // Assume starts inside the zone
    }

    @Override
    public GPSSensorReading sendReading(){
     GPSSensorReading record = new GPSSensorReading(this, currentLatitude, currentLongitude, isInsideZone);
     addReading(record);
     return record;
    }

    public void updateLocation(double latitude, double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.isInsideZone = checkIfInsideZone();
    }

    private boolean checkIfInsideZone() {
        if(animal.getZone() instanceof LivestockZONE) {
            LivestockZONE zone = (LivestockZONE) animal.getZone();
            if (!zone.hasBoundaries()) return true;
            return zone.contains(currentLatitude, currentLongitude);
        }
        return true;
    }

    @Override
    public String toString() {
        return "GPSCollarSensor{" +
                "animal=" + animal.getName() +
                ", lat=" + currentLatitude +
                ", lon=" + currentLongitude +
                ", insideZone=" + isInsideZone +
                ", status=" + getStatus() +
                '}';
    }

     public Animal getAnimal() { return animal; }
     public double getCurrentLatitude() { return currentLatitude; }
     public double getCurrentLongitude() { return currentLongitude; }
     public boolean isInsideZone() { return isInsideZone; }
     public boolean hasEscaped()      { return !isInsideZone; }

    public GPSSensorReading getLastReading() {
        if (getReadingHistory().isEmpty()) {
            return null;
        }
        SensorReading lastReading = getReadingHistory().get(getReadingHistory().size() - 1);
        if (lastReading instanceof GPSSensorReading) {
            return (GPSSensorReading) lastReading;
        }
        return null;
    }
}
