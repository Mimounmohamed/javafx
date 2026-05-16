package Sensors;
import ZONES.ZONE;

public class WaterSensor extends NumericSensor {
    private WaterMeasureType measureType;

    public WaterSensor(ZONE zone, WaterMeasureType measureType, double minThreshold, double maxThreshold, String unit){
        super(zone, minThreshold, maxThreshold, unit);
        this.measureType = measureType;
    }

    @Override
    public NumericSensorReading sendReading() {
        NumericSensorReading record = new NumericSensorReading(this, getLastValue(), getUnit());
        addReading(record);
        return record;
    }

    public NumericSensorReading getLastReading(){
        return new NumericSensorReading(this, getLastValue(), getUnit());
    }

    public double measure() {
        return getLastValue();
    }

    public WaterMeasureType getMeasureType() { return measureType; }
}
