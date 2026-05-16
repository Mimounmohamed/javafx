package ZONES;
/*import org.json.JSONArray;
import org.json.JSONObject;*/
import java.util.ArrayList;
import java.util.List;

public class GoegraphicBoundries {
    private final List<double[]> points;

    public GoegraphicBoundries() {
        this.points = new ArrayList<>();
    }

    public GoegraphicBoundries(List<double[]> points) { // Constructor that accepts a list of points
        this.points = points;
    }

    public void addPoint(double latitude, double longitude) {
        points.add(new double[]{latitude, longitude});
    }

    public static GoegraphicBoundries createRectangle(double lat1, double lon1, double lat2, double lon2) {
        GoegraphicBoundries boundries = new GoegraphicBoundries();
        boundries.addPoint(lat1, lon1);
        boundries.addPoint(lat1, lon2);
        boundries.addPoint(lat2, lon2);
        boundries.addPoint(lat2, lon1);
        return boundries;
    }

    public static GoegraphicBoundries createTriangle(double lat1, double lon1, double lat2, double lon2, double lat3, double lon3) {
        GoegraphicBoundries boundries = new GoegraphicBoundries();
        boundries.addPoint(lat1, lon1);
        boundries.addPoint(lat2, lon2);
        boundries.addPoint(lat3, lon3);
        return boundries;
    }

    public static GoegraphicBoundries createCircle(double centerLat, double centerLon, double radiusMeters, int numPoints) {
        if (numPoints < 3)
            throw new IllegalArgumentException("numPoints must be at least 3");
        GoegraphicBoundries g = new GoegraphicBoundries();
        double angularStep = 2 * Math.PI / numPoints;
        double latradiusDeg = radiusMeters / 111320.0; // Approximate conversion from meters to degrees

        double cosLat = Math.cos(Math.toRadians(centerLat));
        double lonRadiusDeg = (cosLat > 0) ? radiusMeters / (111320.0 * cosLat) : 0; // Adjust longitude radius based on latitude

        for (int i = 0; i < numPoints; i++) {
            double angle = i * angularStep;
            double dLat = latradiusDeg * Math.sin(angle);
            double dLon = lonRadiusDeg * Math.cos(angle);
            g.addPoint(centerLat + dLat, centerLon + dLon);
        }
        return g;
    }

    public boolean contains(double lat, double lon) {
        if (points.isEmpty()) return false;
        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0], yi = points.get(i)[1];
            double xj = points.get(j)[0], yj = points.get(j)[1];

            if ((yi > lon) != (yj > lon) && (lat < (xj - xi) * (lon - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

  /*  public JSONObject toJson(String name) { //all the shapes get saved into a json file
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("shape", "POLYGON");

        JSONArray ptsArray = new JSONArray();
        for (double[] pt : points) {
            JSONObject ptObj = new JSONObject();
            ptObj.put("lat", pt[0]);
            ptObj.put("lon", pt[1]);
            ptsArray.put(ptObj);
        }

        obj.put("points", ptsArray);
        return obj;
    }

    public static ZoneGeometry fromJson(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject json = new JSONObject(content);

        // FORMAT 1 — Circle detected by presence of "centerLat"
        if (json.has("centerLat")) {
            double centerLat    = json.getDouble("centerLat");
            double centerLon    = json.getDouble("centerLon");
            double radius       = json.getDouble("radiusMeters");
            int    numPoints    = json.optInt("numPoints", 36); // default 36 if not specified
            return createCircle(centerLat, centerLon, radius, numPoints);
        }

        // FORMAT 2 — Polygon / rectangle / triangle — read points array
        ZoneGeometry geom = new ZoneGeometry();
        JSONArray pts = json.getJSONArray("points");
        for (int i = 0; i < pts.length(); i++) {
            JSONObject pt = pts.getJSONObject(i);
            geom.addPoint(pt.getDouble("lat"), pt.getDouble("lon"));
        }
        return geom;
    }*/

    public List<double[]> getPoints() {
        return points;
    }
    public int size() {
        return points.size();
    }
    @Override
    public String toString() {
        return "ZoneGeometry{points=" + points.size() + "}";
    }
}

