package Sensors;

import java.time.LocalDateTime;

public class NumericSensorReading extends SensorReading{
    private ReadingLevel severity;
    private double value;
    private String unit;

    public NumericSensorReading(NumericSensor sensor, double value, String unit) {
        super(sensor);
        this.value = value;
        this.unit = unit;
        this.severity = determineSeverity(sensor, value);
    }

    public NumericSensorReading(NumericSensor sensor, double value, String unit, LocalDateTime timestamp) {
        super(sensor, timestamp);
        this.value = value;
        this.unit = unit;
        this.severity = determineSeverity(sensor, value);
    }

    private ReadingLevel determineSeverity(NumericSensor sensor, double value) {
        if(sensor.isOutOfRange(value)){
            double range = sensor.getMaxThreshold() - sensor.getMinThreshold();
            double excess = Math.max(
                    sensor.getMinThreshold() - value,
                    value - sensor.getMaxThreshold()
            );
            if(excess > range * 0.2){
                return ReadingLevel.CRITICAL;
            } else {
                return ReadingLevel.WARNING;
            }
        }
        return ReadingLevel.NORMAL;
    }

    public boolean isOutOfThreshold(){
        return severity != ReadingLevel.NORMAL;
    }

    @Override
    public String toString() {
        return "NumericSensorReading{" +
                "value=" + value + " " + unit +
                ", severity=" + severity +
                ", timestamp=" + getTimestamp() +
                '}';
    }

    public double getValue()          { return value; }
    public String getUnit()           { return unit; }
    public ReadingLevel getSeverity() { return severity; }
}
