package com.example.utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SaveHistoryRepository {

    public record SaveEntry(String farmId, String farmName, LocalDateTime timestamp, String type) {}

    private static final Path HISTORY_FILE;
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ENTRIES = 100;

    static {
        Path dir = Paths.get(System.getProperty("user.home"), ".smartfarm");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        HISTORY_FILE = dir.resolve("save_history.properties");
    }

    public static void record(String farmId, String farmName, String type) {
        List<SaveEntry> entries = new ArrayList<>(loadAll());
        entries.add(0, new SaveEntry(farmId, farmName, LocalDateTime.now(), type));
        if (entries.size() > MAX_ENTRIES) entries = entries.subList(0, MAX_ENTRIES);
        writeAll(entries);
    }

    public static List<SaveEntry> loadForFarm(String farmId) {
        return loadAll().stream()
                .filter(e -> farmId.equals(e.farmId()))
                .collect(Collectors.toList());
    }

    public static List<SaveEntry> loadAll() {
        Properties p = loadProps();
        int count = intProp(p, "entry.count", 0);
        List<SaveEntry> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String fid  = p.getProperty("entry." + i + ".farmId",    "");
            String name = p.getProperty("entry." + i + ".farmName",  "");
            String ts   = p.getProperty("entry." + i + ".timestamp", "");
            String type = p.getProperty("entry." + i + ".type",      "Scheduled");
            try { result.add(new SaveEntry(fid, name, LocalDateTime.parse(ts, FMT), type)); }
            catch (Exception ignored) {}
        }
        return result;
    }

    private static void writeAll(List<SaveEntry> entries) {
        Properties p = new Properties();
        p.setProperty("entry.count", String.valueOf(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            SaveEntry e = entries.get(i);
            p.setProperty("entry." + i + ".farmId",    e.farmId());
            p.setProperty("entry." + i + ".farmName",  e.farmName());
            p.setProperty("entry." + i + ".timestamp", e.timestamp().format(FMT));
            p.setProperty("entry." + i + ".type",      e.type());
        }
        try (OutputStream out = Files.newOutputStream(HISTORY_FILE)) {
            p.store(out, "Smart Farm Manager — save history");
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private static Properties loadProps() {
        Properties p = new Properties();
        if (Files.exists(HISTORY_FILE)) {
            try (InputStream in = Files.newInputStream(HISTORY_FILE)) { p.load(in); }
            catch (IOException ignored) {}
        }
        return p;
    }

    private static int intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}
