package com.example.utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class AppPreferences {

    private static AppPreferences instance;

    private static final Path PREFS_FILE;
    static {
        Path dir = Paths.get(System.getProperty("user.home"), ".smartfarm");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        PREFS_FILE = dir.resolve("preferences.properties");
    }

    private final Properties props = new Properties();

    private AppPreferences() { load(); }

    public static AppPreferences getInstance() {
        if (instance == null) instance = new AppPreferences();
        return instance;
    }

    public boolean isAutoSaveEnabled() {
        return Boolean.parseBoolean(props.getProperty("autoSave.enabled", "true"));
    }

    public void setAutoSaveEnabled(boolean v) {
        props.setProperty("autoSave.enabled", String.valueOf(v));
        persist();
    }

    public int getAutoSaveIntervalSeconds() {
        try { return Integer.parseInt(props.getProperty("autoSave.intervalSeconds", "60")); }
        catch (NumberFormatException e) { return 60; }
    }

    public void setAutoSaveIntervalSeconds(int s) {
        props.setProperty("autoSave.intervalSeconds", String.valueOf(s));
        persist();
    }

    public boolean isDarkTheme() {
        return Boolean.parseBoolean(props.getProperty("theme.dark", "false"));
    }

    public void setDarkTheme(boolean dark) {
        props.setProperty("theme.dark", String.valueOf(dark));
        persist();
    }

    private void load() {
        if (Files.exists(PREFS_FILE)) {
            try (InputStream in = Files.newInputStream(PREFS_FILE)) { props.load(in); }
            catch (IOException ignored) {}
        }
    }

    private void persist() {
        try (OutputStream out = Files.newOutputStream(PREFS_FILE)) {
            props.store(out, "Smart Farm Manager — preferences");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
