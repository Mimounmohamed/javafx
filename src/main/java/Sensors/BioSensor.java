package Sensors;
import Animals.Animal;

public class BioSensor extends NumericSensor{
    private Animal animal;
    private BioMeasureType measureType;


    private static String unitFor(BioMeasureType type) {
        return switch (type) {
            case Temperature   -> "°C";
            case ActivityLevel -> "steps/min";
        };
    }

    public BioSensor(Animal animal, BioMeasureType measureType, double minThreshold, double maxThreshold) {
        super(animal.getZone(), minThreshold, maxThreshold, unitFor(measureType));
        this.animal = animal;
        this.measureType = measureType;
    }

    @Override
    public NumericSensorReading sendReading() {
        NumericSensorReading record = new NumericSensorReading(this, getLastValue(), getUnit());
        addReading(record);
        return record;
    }

    public boolean isAnimalInDistress() {
        return isOutOfRange(getLastValue());
    }

    public Animal getAnimal() { return animal; }
    public BioMeasureType getMeasureType() { return measureType; }

    @Override
    public String toString() {
        return "BioSensor{" +
                "animal=" + animal.getName() +
                ", measureType=" + measureType +
                ", lastValue=" + getLastValue() + " " + getUnit() +
                ", status=" + getStatus() +
                '}';
    }


}
