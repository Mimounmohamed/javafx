package Reports;

import Alerts.Alert;
import Alerts.AlertType;
import Animals.Animal;
import Animals.AnimalHealthStatus;
import Animals.FeedingProgram;
import Sensors.*;
import ZONES.LivestockZONE;

import java.time.LocalDateTime;
import java.util.List;

public class ReportLiveStockZone extends Report {

    private int totalAnimals;
    private int healthyAnimals;
    private int sickAnimals;
    private int quarantinedAnimals;
    private int gpsEscapes;
    private boolean feedingOnTime;

    public ReportLiveStockZone(LivestockZONE zone, ReportType reportType,
                           LocalDateTime periodStart, LocalDateTime periodEnd,
                           List<Alert> alerts) {
        super(zone, reportType, periodStart, periodEnd, alerts);
    }

    @Override
    protected void computeSpecificStats(List<Alert> alerts) {
        LivestockZONE zone = (LivestockZONE) getZone();

        // animal health
        totalAnimals     = zone.getAnimals().size();
        healthyAnimals   = 0;
        sickAnimals      = 0;
        quarantinedAnimals = 0;

        for (Animal a : zone.getAnimals()) {
            switch (a.getHealthStatus()) {
                case Healthy     -> healthyAnimals++;
                case Sick        -> sickAnimals++;
                case Quarantined -> quarantinedAnimals++;
            }
        }

        // GPS escapes from alerts
        gpsEscapes = 0;
        for (Alert a : alerts)
            if (a.getType() == AlertType.GPS_ESCAPE_ALERT) gpsEscapes++;

        // feeding
        FeedingProgram program = zone.getFeedingProgram();
        if (program == null) {
            feedingOnTime = false;
            addNote("No feeding program assigned to this zone");
        } else {
            feedingOnTime = !program.isOverdue();
            if (!feedingOnTime) addNote("Feeding was overdue during this period");
        }
    }

    @Override
    protected void computeSensorSummaries() {
        LivestockZONE zone = (LivestockZONE) getZone();

        for (Animal animal : zone.getAnimals()) {

            // BioSensors
            for (BioSensor sensor : animal.getBioSensors()) {
                List<SensorReading> history = sensor.getReadingHistory();
                if (history.isEmpty()) continue;
                SensorReading last = history.get(history.size() - 1);
                if (last instanceof NumericSensorReading numeric && numeric.isOutOfThreshold()) {
                    addSensorSummary(String.format(
                            "[BioSensor | %s] %s — %.2f %s | %s (%s)",
                            sensor.getMeasureType(),
                            numeric.getSeverity(),
                            numeric.getValue(),
                            numeric.getUnit(),
                            animal.getName(),
                            numeric.getTimestamp()
                    ));
                }
            }

            // GPS collar
            if (animal.hasGPSCollar()) {
                GPSCollarSensor gps = animal.getGpsCollarSensor();
                List<SensorReading> history = gps.getReadingHistory();
                if (!history.isEmpty()) {
                    SensorReading last = history.get(history.size() - 1);
                    if (last instanceof GPSSensorReading gpsReading && gpsReading.isOutsideZone()) {
                        addSensorSummary(String.format(
                                "[GPS] %s escaped — lat %.5f, lon %.5f (%s)",
                                animal.getName(),
                                gpsReading.getLat(),
                                gpsReading.getLon(),
                                gpsReading.getTimestamp()
                        ));
                    }
                }
            }
        }

        if (getSensorSummaries().isEmpty()) addSensorSummary("All sensors normal");
    }

    @Override
    protected String specificStatsBlock() {
        return  "  ANIMALS  : " + totalAnimals + " total\n" +
                "    Healthy     : " + healthyAnimals + "\n" +
                "    Sick        : " + sickAnimals + "\n" +
                "    Quarantined : " + quarantinedAnimals + "\n" +
                "  GPS ESCAPES  : " + gpsEscapes + "\n" +
                "  FEEDING      : " + (feedingOnTime ? "On time ✅" : "Overdue ⚠️") + "\n";
    }

    // Getters
    public int getTotalAnimals()       { return totalAnimals; }
    public int getHealthyAnimals()     { return healthyAnimals; }
    public int getSickAnimals()        { return sickAnimals; }
    public int getQuarantinedAnimals() { return quarantinedAnimals; }
    public int getGpsEscapes()         { return gpsEscapes; }
    public boolean isFeedingOnTime()   { return feedingOnTime; }
}