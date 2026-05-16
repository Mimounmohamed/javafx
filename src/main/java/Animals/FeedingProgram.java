package Animals;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FeedingProgram {
    private String foodType;
    private double quantity; // in kg
    private List<String> schedule; // e.g., ["08:00", "12:00", "18:00"]
    private LocalDateTime lastFedTime;
    private LocalDateTime nextFeedingTime;
    private LocalTime wakeUpTime; // to be used for calculating feeding times based on animal activity patterns
    private LocalTime sleepTime; // to be used for calculating feeding times based on animal activity patterns

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public FeedingProgram(String foodType, double quantity, List<String> schedule, LocalTime wakeUpTime, LocalTime sleepTime) {
        if (quantity <= 0)
            throw new IllegalArgumentException("quantityKg must be greater than 0");
        if (schedule == null || schedule.isEmpty())
            throw new IllegalArgumentException("schedule cannot be empty");
        validateSchedule(schedule);
        if (!wakeUpTime.isBefore(sleepTime))
            throw new IllegalArgumentException("wakeUpTime must be before sleepTime");
        this.foodType = foodType;
        this.quantity = quantity;
        this.schedule = schedule;
        this.wakeUpTime = wakeUpTime;
        this.sleepTime = sleepTime;
        this.lastFedTime = null;
        this.nextFeedingTime = calculateNextFeedingTime();
    }

    private void validateSchedule(List<String> schedule) {
        for (String time : schedule) {
            LocalTime.parse(time, TIME_FORMAT);
        }
    }

    public void recordFeeding() {
        this.lastFedTime = LocalDateTime.now();
        this.nextFeedingTime = calculateNextFeedingTime();
    }

    private LocalDateTime calculateNextFeedingTime() {
        LocalTime now = LocalTime.now();

        for (String time : schedule) {
            LocalTime scheduledTime = LocalTime.parse(time, TIME_FORMAT);
            // find the first schedule time that is still in the future today
            if (scheduledTime.isAfter(now)) {
                return LocalDateTime.now().toLocalDate().atTime(scheduledTime);
            }
        }
        // all schedule times passed today next feeding is first slot tomorrow
        LocalTime firstSlot = LocalTime.parse(schedule.get(0), TIME_FORMAT);
        return LocalDateTime.now().toLocalDate().plusDays(1).atTime(firstSlot);
    }

    public boolean isDue() {
        LocalTime now = LocalTime.now();
        // don't feed during sleep hours
        if (now.isAfter(sleepTime) || now.isBefore(wakeUpTime))
            return false;
        return LocalDateTime.now().isAfter(nextFeedingTime);
    }

    public boolean isOverdue() {
        if (nextFeedingTime == null) return false;
        // overdue if current time passed the scheduled time by more than 30 minutes
        return java.time.Duration.between(nextFeedingTime, LocalDateTime.now())
                .toMinutes() > 30;
    }

    public long hoursUntilNextFeeding() {
        if (nextFeedingTime == null || isDue()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), nextFeedingTime).toHours();
    }

    public long minutesUntilNextFeeding() {
        if (nextFeedingTime == null || isDue()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), nextFeedingTime).toMinutes();
    }


    public void setFoodType(String foodType)     { this.foodType = foodType; }
    public void setQuantity(double quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("quantityKg must be greater than 0");
        this.quantity = quantity;
    }
    public void setSchedule(List<String> schedule) {
        if (schedule == null || schedule.isEmpty())
            throw new IllegalArgumentException("schedule cannot be empty");
        validateSchedule(schedule);
        this.schedule = schedule;
        this.nextFeedingTime = calculateNextFeedingTime();
    }


    public String getFoodType()               { return foodType; }
    public double getQuantity()               { return quantity; }
    public int getTimesPerDay()               { return schedule.size(); } // derived from schedule
    public List<String> getSchedule()         { return schedule; }
    public LocalDateTime getLastFedTime()     { return lastFedTime; }
    public LocalDateTime getNextFeedingTime() { return nextFeedingTime; }
    public LocalTime getWakeUpTime()          { return wakeUpTime; }
    public LocalTime getSleepTime()           { return sleepTime; }

    @Override
    public String toString() {
        return "FeedingProgram{" +
                "foodType=" + foodType +
                ", quantity=" + quantity + "kg" +
                ", schedule=" + schedule +
                ", lastFedTime=" + (lastFedTime != null ? lastFedTime : "never") +
                ", nextFeedingTime=" + nextFeedingTime +
                ", isDue=" + isDue() +
                '}';
    }
}
