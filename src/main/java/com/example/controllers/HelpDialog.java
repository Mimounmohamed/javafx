package com.example.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class HelpDialog extends Dialog<Void> {

    public HelpDialog(List<String> stylesheets) {
        setTitle("Help Guide — Smart Farm Manager");
        setHeaderText(null);
        setResizable(true);

        getDialogPane().setPrefSize(740, 560);
        getDialogPane().setMinSize(600, 440);
        if (stylesheets != null)
            getDialogPane().getStylesheets().addAll(stylesheets);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("help-tab-pane");
        tabs.getTabs().addAll(
            makeTab("🚀  Getting Started", buildGettingStarted()),
            makeTab("🗺  Feature Tour",     buildFeatureTour()),
            makeTab("📡  Sensors & Alerts", buildSensorsAlerts()),
            makeTab("📋  Reports",          buildReports()),
            makeTab("💡  Tips",             buildTips())
        );

        getDialogPane().setContent(tabs);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    // ── Tab builder ───────────────────────────────────────────────────

    private Tab makeTab(String title, VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return new Tab(title, sp);
    }

    // ── Content sections ──────────────────────────────────────────────

    private VBox buildGettingStarted() {
        return page(
            section("Creating your first farm"),
            item("📝", "Fill in the form",
                "On the startup screen enter a Farm Name and Owner (required), plus an optional Location. " +
                "Then click one of the three create buttons below the form."),
            item("📋", "With Sample Data",
                "Generates a complete farm with zones, animals, crops, aquaculture, sensors, and historical " +
                "readings — perfect for exploring all features immediately."),
            item("⬜", "Empty Farm",
                "Creates a blank farm. Add zones via the Zones section, then populate them with animals, " +
                "crops, or aquaculture species manually."),
            item("🎲", "Random Farm",
                "Auto-generates a farm with a random name, location, and owner, then fills it with data. " +
                "Equivalent to 'With Sample Data' but no typing required."),
            section("Opening an existing farm"),
            item("▶", "Select from the list",
                "Your saved farms appear in the list on the startup screen. Click 'Open →' on any row " +
                "to load it and jump straight to the Dashboard."),
            item("🗂", "Demo Farm",
                "The Demo Farm is always present and resets each time it is opened. Nothing done in Demo " +
                "mode is saved permanently — use it for exploration."),
            section("Navigating the app"),
            item("☰", "Sidebar",
                "Use the left sidebar to switch between Dashboard, Zones, Animals, Crops, Aquaculture, " +
                "Sensors, Alerts, Reports, Simulation, and Settings. Click ☰ to collapse to icon-only mode."),
            item("🔔", "Alert badge",
                "A red badge on the bell icon shows the number of active alerts. Clicking it navigates " +
                "directly to the Alerts section.")
        );
    }

    private VBox buildFeatureTour() {
        return page(
            section("Core sections"),
            item("📊", "Dashboard",
                "Live farm overview: total zones, animals, sensors, and active alerts. Charts for livestock " +
                "health distribution, crop stage breakdown, and a recent events feed."),
            item("🗺", "Zones",
                "Create and manage Livestock, Crop, and Aquaculture zones. Each zone has a detail panel " +
                "with its animals or species, sensor readings, and a GPS map view. Draw geographic boundaries " +
                "on the interactive canvas."),
            item("🐄", "Animals",
                "Track every animal: health status, age, weight, and zone assignment. Open the detail panel " +
                "to see bio-sensor readings, GPS collar history, feeding program, and milk production records."),
            item("🌾", "Crops",
                "View all crops by zone. Each entry shows growth stage, expected harvest date, yield estimate, " +
                "and the environmental/soil sensors attached to that zone."),
            item("🐟", "Aquaculture",
                "Manage fish species in aquaculture zones. Track population, average weight, feeding schedule, " +
                "and water quality sensor readings (pH, dissolved oxygen, temperature)."),
            section("Monitoring and control"),
            item("📡", "Sensors",
                "All sensors from every zone in one view. Filter by type or status, sort by name or reading, " +
                "click a card to see full details and reading history. Export individual or all sensors as PDF."),
            item("🔔", "Alerts",
                "All generated alerts in a sortable table. Click a row to see the full alert card showing " +
                "which sensor triggered it, when, and the reading vs. the threshold."),
            item("📋", "Reports",
                "PDF reports for every section: Overview, Livestock, Crops, Aquaculture, Alerts, and " +
                "individual sensor charts. Each section has its own export button."),
            item("⏩", "Simulation",
                "Advance time to generate new sensor readings. Pick a tick interval and step count, " +
                "press Play, and watch alerts appear as readings cross thresholds in real time.")
        );
    }

    private VBox buildSensorsAlerts() {
        return page(
            section("Sensor types"),
            item("💓", "Bio Sensor",
                "Attached to livestock animals. Measures heart rate (bpm) and body temperature (°C). " +
                "Generates a Critical alert if either value leaves the safe range."),
            item("📍", "GPS Collar",
                "Tracks animal coordinates (lat/lon). If the animal moves outside its zone boundary, " +
                "a 'GPS Escape' alert is generated immediately."),
            item("🌡", "Environmental",
                "Measures air temperature (°C) and humidity (%) inside crop or livestock zones. " +
                "Triggers Warning or Critical alerts when readings exceed configured thresholds."),
            item("🌱", "Soil Sensor",
                "Placed in crop zones. Monitors soil moisture (%) and pH. Low moisture or pH drift " +
                "raises alerts before crops are affected."),
            item("💧", "Water Quality",
                "In aquaculture zones. Tracks water pH, dissolved oxygen (mg/L), and temperature (°C). " +
                "All three parameters are critical for fish survival."),
            section("Reading the sensor cards"),
            item("🟢", "Normal  (green stripe)",
                "All readings are within safe bounds. No action needed."),
            item("🟡", "Warning  (amber stripe)",
                "At least one reading has crossed the warning threshold. Monitor closely."),
            item("🔴", "Critical  (red stripe)",
                "A reading has crossed the critical threshold and an alert was generated. " +
                "Investigate immediately."),
            item("⬜", "Suspended / Faulty  (gray stripe)",
                "The sensor is suspended or has reported a fault. No new readings are being collected."),
            section("Alerts"),
            item("🔍", "Filter and sort",
                "Use the filter bar in Alerts to narrow down by severity, sensor type, or date range. " +
                "Click any column header to sort."),
            item("✅", "Active vs Resolved",
                "An alert stays Active until the sensor reading returns to the normal range. The KPI " +
                "strip at the top of Alerts shows both Active and Resolved counts.")
        );
    }

    private VBox buildReports() {
        return page(
            section("Report sections"),
            item("📊", "Overview",
                "Farm-wide KPIs and charts for animal health distribution, crop stage breakdown, " +
                "and sensor health summary."),
            item("🐄", "Livestock",
                "All animals grouped by zone, with health status distribution charts and a full animal roster."),
            item("🌾", "Crops",
                "All crops grouped by zone, with growth stage charts and harvest timeline."),
            item("🐟", "Aquaculture",
                "Species per zone with population, average weight, and water quality summary."),
            item("🔔", "Alerts",
                "All alerts with severity breakdown charts and a complete detail table."),
            section("Exporting to PDF"),
            item("📄", "Single chart",
                "Every chart card in Reports has a '↓ PDF' button. Clicking it saves that chart " +
                "with title and data table as a standalone PDF."),
            item("📡", "Sensor chart PDF",
                "In the Sensors report each sensor card has its own PDF button. The PDF contains " +
                "sensor metadata and an embedded screenshot of the reading history chart."),
            item("📦", "Export All Sensors",
                "The 'Export All PDF' button in the Sensors toolbar creates one PDF containing all " +
                "sensors grouped by zone."),
            item("💾", "Choosing the file",
                "All PDF exports open a Save File dialog. Navigate to your folder, enter a filename, " +
                "and click Save. The .pdf extension is added automatically.")
        );
    }

    private VBox buildTips() {
        return page(
            section("Appearance"),
            item("🌙", "Dark mode",
                "Go to Settings → Appearance and toggle Dark Mode. The choice is remembered — " +
                "the same theme loads automatically the next time you open the app."),
            item("◀", "Collapse sidebar",
                "Click ☰ at the top of the sidebar to collapse it to icon-only mode and gain " +
                "more horizontal space for the main content."),
            section("Saving your data"),
            item("💾", "What is saved",
                "Zones, animals, farm boundaries, and farm metadata are persisted to disk. Sensor " +
                "readings, alerts, and simulation history are in-memory only and reset on restart."),
            item("⏱", "Auto-save",
                "By default the farm is saved every 60 seconds in the background. Change the " +
                "interval or turn auto-save off entirely in Settings → Auto-Save."),
            item("⚡", "Save Now",
                "Click 'Save Now' in Settings → Auto-Save for an immediate manual save at any time."),
            item("📜", "Save history",
                "Settings → Save History shows a log of recent saves with their timestamps and " +
                "the trigger type (Scheduled, Manual, or Settings)."),
            section("Simulation"),
            item("▶", "Speed vs accuracy",
                "Short tick intervals (1 s) create readings quickly for testing. Longer intervals " +
                "(10–30 s) produce more realistic data evolution over simulated time."),
            item("📈", "Generating alerts",
                "Run several simulation ticks, then open Alerts. Critical readings appear when " +
                "randomised data crosses the configured sensor thresholds."),
            section("Farm management"),
            item("🗺", "Farm boundary",
                "Draw the overall farm boundary in Settings → Farm Boundary. Zone boundaries must " +
                "fit inside it. The map uses a relative coordinate system."),
            item("🔄", "Switch farm",
                "Settings → Farm Selection → 'Switch Farm' returns you to the startup screen to " +
                "open a different farm without restarting the application.")
        );
    }

    // ── Layout helpers ────────────────────────────────────────────────

    private VBox page(Object... elements) {
        VBox box = new VBox(0);
        box.getStyleClass().add("help-page");
        box.setPadding(new Insets(20, 24, 28, 24));
        for (Object el : elements) {
            if (el instanceof Node node) box.getChildren().add(node);
        }
        return box;
    }

    private Label section(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.getStyleClass().add("help-section-header");
        VBox.setMargin(lbl, new Insets(18, 0, 8, 0));
        return lbl;
    }

    private HBox item(String icon, String title, String desc) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("help-item-icon");
        iconLbl.setMinWidth(36);
        iconLbl.setAlignment(Pos.TOP_CENTER);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("help-item-title");

        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("help-item-desc");
        descLbl.setWrapText(true);

        VBox text = new VBox(3, titleLbl, descLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, iconLbl, text);
        row.getStyleClass().add("help-item-row");
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(row, new Insets(0, 0, 10, 0));
        return row;
    }
}
