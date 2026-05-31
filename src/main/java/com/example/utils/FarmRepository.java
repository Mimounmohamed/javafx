package com.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Persists farm metadata + zones + animals to ~/.smartfarm/farms.properties.
 * Uses flat Java Properties keys so no external JSON library is needed.
 *
 * Key schema:
 *   farm.count          = N
 *   farm.i.id           = uuid
 *   farm.i.name         = ...
 *   farm.i.location     = ...
 *   farm.i.owner        = ...
 *   farm.i.createdAt    = yyyy-MM-ddTHH:mm
 *   farm.i.isDemo       = true|false
 *   farm.i.zone.count   = Z
 *   farm.i.zone.j.type  = LIVESTOCK|CROP|AQUACULTURE
 *   farm.i.zone.j.name  = ...
 *   farm.i.zone.j.lstType = RUMINANT|POULTRY
 *   farm.i.animal.count = A
 *   farm.i.animal.j.*   = name, species, type, age, weight, zoneName, health
 */
public class FarmRepository {

    private static final Path DATA_FILE;

    static {
        Path dir = Paths.get(System.getProperty("user.home"), ".smartfarm");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        DATA_FILE = dir.resolve("farms.properties");
    }

    // ── Public API ────────────────────────────────────────────────────

    public static List<SavedFarm> loadAll() {
        Properties p = loadProps();
        int count = intProp(p, "farm.count", 0);
        List<SavedFarm> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SavedFarm sf = readFarm(p, i);
            if (sf != null) result.add(sf);
        }
        return result;
    }

    /** Insert or replace a farm in the file (matched by id). */
    public static void save(SavedFarm farm) {
        List<SavedFarm> all = loadAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(farm.id)) { all.set(i, farm); replaced = true; break; }
        }
        if (!replaced) all.add(farm);
        writeAll(all);
    }

    /** Remove a farm by id. */
    public static void delete(String id) {
        List<SavedFarm> all = loadAll();
        all.removeIf(f -> f.id.equals(id));
        writeAll(all);
    }

    // ── Serialise / Deserialise ───────────────────────────────────────

    private static Properties loadProps() {
        Properties p = new Properties();
        if (Files.exists(DATA_FILE)) {
            try (InputStream in = Files.newInputStream(DATA_FILE)) { p.load(in); }
            catch (IOException ignored) {}
        }
        return p;
    }

    private static void writeAll(List<SavedFarm> farms) {
        Properties p = new Properties();
        p.setProperty("farm.count", String.valueOf(farms.size()));
        for (int i = 0; i < farms.size(); i++) writeFarm(p, i, farms.get(i));
        try (OutputStream out = Files.newOutputStream(DATA_FILE)) {
            p.store(out, "Smart Farm Manager — do not edit manually");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static SavedFarm readFarm(Properties p, int i) {
        String id = p.getProperty("farm." + i + ".id");
        if (id == null) return null;
        SavedFarm sf  = new SavedFarm();
        sf.id         = id;
        sf.name       = p.getProperty("farm." + i + ".name",      "");
        sf.location   = p.getProperty("farm." + i + ".location",  "");
        sf.owner      = p.getProperty("farm." + i + ".owner",     "");
        sf.createdAt  = p.getProperty("farm." + i + ".createdAt", "");
        sf.isDemo         = Boolean.parseBoolean(p.getProperty("farm." + i + ".isDemo",         "false"));
        sf.wasRandomized  = Boolean.parseBoolean(p.getProperty("farm." + i + ".wasRandomized",  "false"));

        int zc = intProp(p, "farm." + i + ".zone.count", 0);
        for (int j = 0; j < zc; j++) {
            SavedZone sz  = new SavedZone();
            sz.type       = p.getProperty("farm." + i + ".zone." + j + ".type",    "CROP");
            sz.name       = p.getProperty("farm." + i + ".zone." + j + ".name",    "");
            sz.lstType    = p.getProperty("farm." + i + ".zone." + j + ".lstType", "RUMINANT");
            int bc = intProp(p, "farm." + i + ".zone." + j + ".boundary.count", 0);
            for (int k = 0; k < bc; k++) {
                double lat = doubleProp(p, "farm." + i + ".zone." + j + ".boundary." + k + ".lat", 0);
                double lon = doubleProp(p, "farm." + i + ".zone." + j + ".boundary." + k + ".lon", 0);
                sz.boundaryPoints.add(new double[]{lat, lon});
            }
            sf.zones.add(sz);
        }

        int fbc = intProp(p, "farm." + i + ".farmBoundary.count", 0);
        for (int k = 0; k < fbc; k++) {
            double lat = doubleProp(p, "farm." + i + ".farmBoundary." + k + ".lat", 0);
            double lon = doubleProp(p, "farm." + i + ".farmBoundary." + k + ".lon", 0);
            sf.farmBoundaryPoints.add(new double[]{lat, lon});
        }

        int ac = intProp(p, "farm." + i + ".animal.count", 0);
        for (int j = 0; j < ac; j++) {
            SavedAnimal sa = new SavedAnimal();
            sa.name        = p.getProperty("farm." + i + ".animal." + j + ".name",     "");
            sa.species     = p.getProperty("farm." + i + ".animal." + j + ".species",  "");
            sa.type        = p.getProperty("farm." + i + ".animal." + j + ".type",     "RUMINANT");
            sa.age         = intProp   (p, "farm." + i + ".animal." + j + ".age",    1);
            sa.weight      = doubleProp(p, "farm." + i + ".animal." + j + ".weight", 100.0);
            sa.zoneName    = p.getProperty("farm." + i + ".animal." + j + ".zoneName", "");
            sa.health      = p.getProperty("farm." + i + ".animal." + j + ".health",   "Healthy");
            sf.animals.add(sa);
        }
        return sf;
    }

    private static void writeFarm(Properties p, int i, SavedFarm sf) {
        String pre = "farm." + i + ".";
        p.setProperty(pre + "id",        sf.id);
        p.setProperty(pre + "name",      sf.name);
        p.setProperty(pre + "location",  sf.location);
        p.setProperty(pre + "owner",     sf.owner);
        p.setProperty(pre + "createdAt", sf.createdAt);
        p.setProperty(pre + "isDemo",        String.valueOf(sf.isDemo));
        p.setProperty(pre + "wasRandomized", String.valueOf(sf.wasRandomized));

        p.setProperty(pre + "zone.count", String.valueOf(sf.zones.size()));
        for (int j = 0; j < sf.zones.size(); j++) {
            SavedZone sz = sf.zones.get(j);
            p.setProperty(pre + "zone." + j + ".type", sz.type);
            p.setProperty(pre + "zone." + j + ".name", sz.name);
            if (sz.lstType != null) p.setProperty(pre + "zone." + j + ".lstType", sz.lstType);
            p.setProperty(pre + "zone." + j + ".boundary.count", String.valueOf(sz.boundaryPoints.size()));
            for (int k = 0; k < sz.boundaryPoints.size(); k++) {
                double[] pt = sz.boundaryPoints.get(k);
                p.setProperty(pre + "zone." + j + ".boundary." + k + ".lat", String.valueOf(pt[0]));
                p.setProperty(pre + "zone." + j + ".boundary." + k + ".lon", String.valueOf(pt[1]));
            }
        }

        p.setProperty(pre + "farmBoundary.count", String.valueOf(sf.farmBoundaryPoints.size()));
        for (int k = 0; k < sf.farmBoundaryPoints.size(); k++) {
            double[] pt = sf.farmBoundaryPoints.get(k);
            p.setProperty(pre + "farmBoundary." + k + ".lat", String.valueOf(pt[0]));
            p.setProperty(pre + "farmBoundary." + k + ".lon", String.valueOf(pt[1]));
        }

        p.setProperty(pre + "animal.count", String.valueOf(sf.animals.size()));
        for (int j = 0; j < sf.animals.size(); j++) {
            SavedAnimal sa = sf.animals.get(j);
            String ap = pre + "animal." + j + ".";
            p.setProperty(ap + "name",     sa.name);
            p.setProperty(ap + "species",  sa.species);
            p.setProperty(ap + "type",     sa.type);
            p.setProperty(ap + "age",      String.valueOf(sa.age));
            p.setProperty(ap + "weight",   String.valueOf(sa.weight));
            p.setProperty(ap + "zoneName", sa.zoneName);
            p.setProperty(ap + "health",   sa.health);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static int intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static double doubleProp(Properties p, String key, double def) {
        try { return Double.parseDouble(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    // ── Data classes ──────────────────────────────────────────────────

    public static class SavedFarm {
        public String id, name, location, owner, createdAt;
        public boolean isDemo;
        public boolean wasRandomized;
        public List<SavedZone>   zones   = new ArrayList<>();
        public List<SavedAnimal> animals = new ArrayList<>();
        public List<double[]> farmBoundaryPoints = new ArrayList<>();

        /** Build an entry for a brand-new user farm (not yet saved). */
        public static SavedFarm fromNew(String name, String location, String owner) {
            SavedFarm sf  = new SavedFarm();
            sf.id         = UUID.randomUUID().toString();
            sf.name       = name;
            sf.location   = location == null ? "" : location;
            sf.owner      = owner;
            sf.createdAt  = LocalDateTime.now().toString().substring(0, 16);
            sf.isDemo     = false;
            return sf;
        }

        /** Canonical demo-farm entry (always the same stable id). */
        public static SavedFarm forDemo() {
            SavedFarm sf  = new SavedFarm();
            sf.id         = "demo-farm-built-in";
            sf.name       = "Demo Farm";
            sf.location   = "Algiers, Algeria";
            sf.owner      = "Admin";
            sf.createdAt  = LocalDateTime.now().toString().substring(0, 10);
            sf.isDemo     = true;
            return sf;
        }

        public int zoneCount()   { return zones.size(); }
        public int animalCount() { return animals.size(); }
        public String subtitle() {
            return owner + (location.isBlank() ? "" : " · " + location);
        }
    }

    public static class SavedZone {
        public String type;     // LIVESTOCK | CROP | AQUACULTURE
        public String name;
        public String lstType;  // RUMINANT | POULTRY  (only used for LIVESTOCK)
        public List<double[]> boundaryPoints = new ArrayList<>();
    }

    public static class SavedAnimal {
        public String name, species, type, zoneName, health;
        public int    age;
        public double weight;
    }
}
