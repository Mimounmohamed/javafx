package Animals;
import java.time.LocalDateTime;

public class HealthEvent {
    private Animal animal;
    private AnimalHealthStatus eventType;
    private AnimalHealthStatus statusBefore;
    private AnimalHealthStatus statusAfter;
    private LocalDateTime date;
    private String description;
    private boolean resolved;

    public HealthEvent(Animal animal, AnimalHealthStatus eventType, AnimalHealthStatus statusBefore, AnimalHealthStatus statusAfter, String description) {
        if (animal == null) throw new IllegalArgumentException("animal cannot be null");
        if (eventType == null) throw new IllegalArgumentException("eventType cannot be null");
        if (statusBefore == null) throw new IllegalArgumentException("statusBefore cannot be null");
        this.animal = animal;
        this.eventType = eventType;
        this.statusBefore = statusBefore;
        this.statusAfter = statusAfter;
        this.date = LocalDateTime.now();
        this.description = description;
        this.resolved = statusAfter != null;
    }

    public void markResolved(AnimalHealthStatus statusAfter){
        this.resolved = true;
        this.statusAfter = statusAfter;
    }

    public Animal getAnimal()                 { return animal; }
    public AnimalHealthStatus getEventType()    { return eventType; }
    public AnimalHealthStatus getStatusBefore() { return statusBefore; }
    public AnimalHealthStatus getStatusAfter()  { return statusAfter; }
    public String getDescription()              { return description; }
    public LocalDateTime getDate()              { return date; }
    public boolean isResolved()                 { return resolved; }

    @Override
    public String toString() {
        return "HealthEvent{" +
                "animalID=" + animal.getId() +
                ", eventType=" + eventType +
                ", statusBefore=" + statusBefore +
                ", statusAfter=" + (statusAfter != null ? statusAfter : "pending") +
                ", date=" + date +
                ", resolved=" + resolved +
                '}';
    }
}
