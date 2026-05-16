package Additional_classes;

public class Range {
    private double min;
    private double max;

    public Range(double min, double max) {
        if (min > max) throw new IllegalArgumentException("min cannot exceed max");
        this.min = min;
        this.max = max;
    }
    public double getMin() {
        return min;
    }
    public void setMin(double min) {
        if (min > this.max) throw new IllegalArgumentException("min cannot exceed max");
        this.min = min;
    }
    public double getMax() {
        return max;
    }
    public void setMax(double max) {
        if (max < this.min) throw new IllegalArgumentException("max cannot be less than min");
        this.max = max;
    }
    public boolean isInRange(double value) {
        return value >= min && value <= max;
    }
}
