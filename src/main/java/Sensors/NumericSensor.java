package Sensors;
import ZONES.ZONE;
import java.util.ArrayList;
import java.util.List;

public abstract class NumericSensor extends Sensor{
    private double minThreshold;
    private double maxThreshold;
    private String unit;
    private double lastValue;

    public NumericSensor(ZONE zone, double minThreshold, double maxThreshold, String unit) {
        super(zone);
        if (minThreshold > maxThreshold)
            throw new IllegalArgumentException("minThreshold cannot exceed maxThreshold");
        if (unit == null || unit.isBlank())
            throw new IllegalArgumentException("unit cannot be null or blank");
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.unit = unit;
        this.lastValue = Double.NaN;
    }

    public void setMinThreshold(double minThreshold) {
        if (minThreshold > this.maxThreshold)
            throw new IllegalArgumentException("minThreshold cannot exceed maxThreshold");
        this.minThreshold = minThreshold;
    }
    public void setMaxThreshold(double maxThreshold) {
        if (maxThreshold < this.minThreshold)
            throw new IllegalArgumentException("maxThreshold cannot be less than minThreshold");
        this.maxThreshold = maxThreshold;
    }

    public boolean isOutOfRange(double value) {
        return value < minThreshold || value > maxThreshold;
    }

    public double getMinThreshold() { return minThreshold; }
    public double getMaxThreshold() { return maxThreshold; }
    public String getUnit() { return unit; }

    public void setLastValue(double value) {
        this.lastValue = value;
    }

    public double getLastValue() {
        return lastValue;
    }

    public static List<NumericSensorReading> filterByLevel(List<NumericSensorReading> readings, ReadingLevel level) {
        List<NumericSensorReading> result = new ArrayList<>();
        for (NumericSensorReading r : readings)
            if (r.getSeverity() == level) result.add(r);
        return result;
    }
}
