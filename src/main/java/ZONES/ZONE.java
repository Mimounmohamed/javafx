package ZONES;

/*
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
 */

public abstract class ZONE {
    private static int idCounter = 1; // Static counter for unique IDs
    private String code;
    private String name;
    private ZoneStatus status;
    private GoegraphicBoundries boundaries;

    protected ZONE(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name cannot be null or blank");
        this.code = String.format("%04d", idCounter++);
        this.name = name;
        this.status = ZoneStatus.ACTIVE;
        this.boundaries = null;
    }

    protected ZONE(String name , GoegraphicBoundries boundries) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name cannot be null or blank");
        if (boundries == null)
            throw new IllegalArgumentException("boundaries cannot be null");
        this.code = String.format("%04d", idCounter++);
        this.name = name;
        this.status = ZoneStatus.ACTIVE;
        this.boundaries = boundries;
    }

   /* public static ZONE fromJson(String filePath) throws Exception { //cordinates from json file to be implemented in future
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject json = new JSONObject(content);
        String name = json.getString("name");
        ZONE zone = new ZONE(name);
        if (json.has("points") || json.has("centerLat")) {
            zone.boundaries = GeographicBoundaries.fromJson(filePath);
        }
        return zone;
    }*/

    public void activate() {
        this.status = ZoneStatus.ACTIVE;
    }

    public void suspend() {
        this.status = ZoneStatus.SUSPENDED;
    }

    public boolean hasBoundaries()  { return boundaries != null; }
    public GoegraphicBoundries getBoundaries() { return boundaries; }
    public void setBoundaries(GoegraphicBoundries boundaries) { this.boundaries = boundaries; }

    public boolean contains(double lat, double lon) {
        return boundaries != null && boundaries.contains(lat, lon);
    }

    public ZoneStatus getStatus() {
        return this.status;
    }

    public String getCode() {
        return code;
    }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name cannot be null or blank");
        this.name = name;
    }
}
