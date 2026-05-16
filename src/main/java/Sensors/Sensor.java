package Sensors;
import ZONES.ZONE;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

public abstract class Sensor {
    private static int idCounter = 1;
    private String code;
    private ZONE zone;
    private SensorStatus status;
    private List<SensorReading> readingHistory;

    public Sensor(ZONE zone) {
        this.code = String.format("%04d", idCounter++);
        this.zone = zone;
        this.status = SensorStatus.Active;
        this.readingHistory = new ArrayList<>();
    }

    public abstract SensorReading sendReading();//Let the subclass handle this

    public void suspend() {
        this.status = SensorStatus.Suspended;
    }

    public void reactivate() {
        this.status = SensorStatus.Active;
    }

    public void markAsFaulty() {
        this.status = SensorStatus.Faulty;
    }

    public SensorStatus getStatus() {
        return this.status;
    }


    public List<SensorReading> getReadingHistory() {
        return Collections.unmodifiableList(readingHistory);
    }

    public void addReading(SensorReading reading){
        readingHistory.add(reading);
    }

    public void clearReadingHistory() {
        readingHistory.clear();
    }



    public String getCode() { return code; }
    public ZONE getZone() { return zone; }

    public static List<SensorReading> filterByPeriod(List<SensorReading> readings, LocalDateTime from, LocalDateTime to) {
        List<SensorReading> result = new ArrayList<>();
        for (SensorReading r : readings)
            if (!r.getTimestamp().isBefore(from) && !r.getTimestamp().isAfter(to))
                result.add(r);
        return result;
    }

    public static List<SensorReading> sortByTimestamp(List<SensorReading> readings) {
        List<SensorReading> result = new ArrayList<>(readings);
        result.sort(Comparator.comparing(SensorReading::getTimestamp));
        return result;
    }
}
