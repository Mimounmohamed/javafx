# Smart Farm Management System

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-007ACC?style=flat-square&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Apache PDFBox](https://img.shields.io/badge/PDFBox-3.0.2-F04E23?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

A full-featured **desktop farm management application** built with JavaFX 21. It lets you monitor and manage livestock, crops, and aquaculture across multiple zones — with real-time sensor dashboards, GPS animal tracking, automated alerts, interactive zone maps, and professional PDF report export.

Designed as a complete OOP showcase: abstract type hierarchies, service-layer singletons, file-based persistence, and a polished two-theme UI (light & dark).

---

## Table of Contents

1. [Features](#features)
2. [Screenshots / UI Overview](#screenshots--ui-overview)
3. [Project Structure](#project-structure)
4. [Architecture](#architecture)
   - [Domain Model](#domain-model)
   - [Service Layer](#service-layer)
   - [Persistence](#persistence)
   - [Navigation & Scenes](#navigation--scenes)
   - [Sensor & Alert Pipeline](#sensor--alert-pipeline)
   - [PDF Export Engine](#pdf-export-engine)
5. [Installation](#installation)
6. [Usage](#usage)
7. [Module & Class Reference](#module--class-reference)
   - [Domain Packages](#domain-packages)
   - [Services](#services)
   - [Controllers](#controllers)
   - [Utilities](#utilities)
8. [Configuration](#configuration)
9. [Dependencies](#dependencies)
10. [Contributing](#contributing)
11. [License](#license)

---

## Features

**Farm Management**
- Create multiple named farms with location and owner metadata
- Persist farms to `~/.smartfarm/farms.properties` (no database required)
- Generate realistic demo farms with one click (seeded random data)

**Zone System**
- Three zone types: **Livestock**, **Crop**, and **Aquaculture**
- Draw and edit geographic boundaries (polygon-based, point-in-polygon containment)
- Activate / suspend individual zones
- Per-zone sensor assignment, animal assignment, and production tracking

**Livestock Management**
- Track animals by species, type (Ruminant / Poultry), age, and weight
- Full health lifecycle: Healthy → Sick → Quarantined, with health event history
- Record milk yield, egg counts, and weight trends per animal
- Attach bio sensors and GPS collars to individual animals

**Sensor Monitoring**
- Five sensor types: Bio (animal health), GPS Collar, Environmental (crops), Soil, Water
- Timestamped numeric reading history with NORMAL / WARNING / CRITICAL classification
- Live pulse animations on active / faulty sensors
- Per-sensor threshold editing and reading injection
- Date-range and zone/type filtering on the sensor history dashboard

**Crop Management**
- Track crop fields through six growth stages (Sowing → Harvest)
- Record yield per harvest event; cumulative and per-hectare statistics
- Assign soil requirement ranges (pH, moisture) per crop type
- Environmental and soil sensors linked to crop zones

**Aquaculture Management**
- Manage multiple species per aquaculture zone
- Track stock counts, harvest events (weight + count), and mortality
- Per-cycle and overall survival-rate calculations
- Water quality sensors (temperature, dissolved O₂, pH, salinity)

**Alerts System**
- Two alert types: `SensorAlert` (out-of-range reading) and `HealthAlert` (health event)
- Severity levels: Critical and Warning
- Full lifecycle: Active → Acknowledged → Resolved / Dismissed
- Filter alerts by type, severity, status, zone, or time range

**Reports & Analytics**
- Dynamic charts: bar, line, and pie (8-color series, dark-mode aware)
- Per-section overview: Farm, Livestock, Crops, Aquaculture, Alerts, Sensors
- Trend lines with synchronized X-axes and inline alert markers
- **Export any chart** as a PDF image with a single button click
- **Full PDF reports** per section or as a combined document (via Apache PDFBox)
- Sensor readings embedded as rendered chart snapshots in PDFs

**UI & Theming**
- Light and Dark themes — switch at runtime from Settings
- KPI strip cards adapt to both themes (no hardcoded colors)
- Redesigned startup screen with brand panel + farm picker/creator
- Smooth fade transitions between navigation pages
- Fullscreen-safe scene swapping (no flicker on farm open)

**Simulation**
- Inject synthetic readings for any sensor on demand
- Random farm generator for rapid prototyping / demos

---

## Screenshots / UI Overview

| Screen | Description |
|--------|-------------|
| **Startup** | Dark-green brand panel + farm list + new-farm form |
| **Dashboard** | 8 KPI cards, recent-alerts table, farm map, quick actions |
| **Sensors** | Filterable card grid, reading chart dialog, action panel |
| **Reports** | Left-nav sections, live charts with PDF export per card |
| **Zones** | Split list/detail, GPS row tracker, distress banner |
| **Animals** | TableView + 3-tab detail (Info / Sensors / Actions) |

---

## Project Structure

```
smart-farm/
├── pom.xml                          # Maven build — dependencies, JavaFX plugin
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java     # JPMS module descriptor
│       │   │
│       │   ├── com/example/
│       │   │   ├── App.java         # Application entry point (extends Application)
│       │   │   ├── controllers/     # JavaFX MVC controllers (26 classes)
│       │   │   │   ├── MainController.java          # Shell: sidebar, clock, alert badge
│       │   │   │   ├── StartupController.java       # Farm selection & creation
│       │   │   │   ├── DashboardController.java     # Overview KPIs & recent alerts
│       │   │   │   ├── ZonesController.java         # Zone CRUD, detail panel, GPS rows
│       │   │   │   ├── AnimalsController.java       # Animal table + 3-tab detail
│       │   │   │   ├── CropsController.java         # Crop field management
│       │   │   │   ├── AquacultureController.java   # Species, harvest, mortality
│       │   │   │   ├── SensorsController.java       # Sensor grid, reading actions
│       │   │   │   ├── AlertsController.java        # Alert table + detail panel
│       │   │   │   ├── ReportsController.java       # Charts, KPIs, PDF export
│       │   │   │   ├── SettingsController.java      # Farm metadata, theme toggle
│       │   │   │   ├── SimulationController.java    # Data injection & randomization
│       │   │   │   │
│       │   │   │   ├── AddAnimalDialog.java         # Dialog: add livestock
│       │   │   │   ├── AddZoneDialog.java           # Dialog: add zone (type picker)
│       │   │   │   ├── AddCropDialog.java           # Dialog: add crop field
│       │   │   │   ├── AddSpeciesDialog.java        # Dialog: add aquaculture species
│       │   │   │   ├── RenameZoneDialog.java        # Dialog: rename zone
│       │   │   │   ├── EditFeedingProgramDialog.java
│       │   │   │   ├── RecordMilkDialog.java
│       │   │   │   ├── BoundaryEditorDialog.java    # Interactive polygon boundary drawing
│       │   │   │   ├── SensorChartDialog.java       # Full chart + readings table
│       │   │   │   ├── SensorHistoryDialog.java     # Historical data browser
│       │   │   │   ├── ZoneAlertHistoryDialog.java  # Per-zone alert timeline
│       │   │   │   └── FarmMapDialog.java           # Geographic overview map
│       │   │   │
│       │   │   ├── services/        # Business-logic singletons
│       │   │   │   ├── FarmService.java             # Farm state, init, auto-save
│       │   │   │   ├── AnimalService.java           # Animal CRUD + health actions
│       │   │   │   ├── ZoneService.java             # Zone CRUD + activation
│       │   │   │   ├── SensorService.java           # Sensor queries + display helpers
│       │   │   │   ├── AlertService.java            # Alert filtering + lifecycle
│       │   │   │   ├── ReportService.java           # Report object generation
│       │   │   │   ├── PdfReportService.java        # PDF document builder (PDFBox)
│       │   │   │   ├── DataRandomizerService.java   # Synthetic farm data population
│       │   │   │   └── SimulationService.java       # Simulation step logic
│       │   │   │
│       │   │   └── utils/
│       │   │       ├── FarmRepository.java          # File-based persistence (~/.smartfarm/)
│       │   │       ├── SceneManager.java            # SPA navigation + fullscreen-safe swapping
│       │   │       └── RandomFarmGenerator.java     # Quick random farm metadata
│       │   │
│       │   ├── Animals/             # Domain: livestock
│       │   │   ├── Animal.java      # Core entity: identity, health, history, sensors
│       │   │   ├── AnimalHealthStatus.java  # Enum: Healthy | Sick | Quarantined
│       │   │   ├── FeedingProgram.java      # Schedule, last-fed timestamp
│       │   │   └── HealthEvent.java         # Immutable health transition record
│       │   │
│       │   ├── ZONES/               # Domain: farm zones
│       │   │   ├── ZONE.java                # Abstract base: code, name, status, boundary
│       │   │   ├── LivestockZONE.java       # Animals + bio/GPS sensors
│       │   │   ├── CropZONE.java            # Crop fields + env/soil sensors
│       │   │   ├── AquacultureZONE.java     # Species + water sensors
│       │   │   ├── GoegraphicBoundries.java # Polygon with point-in-polygon test
│       │   │   └── ZoneStatus.java          # Enum: ACTIVE | SUSPENDED
│       │   │
│       │   ├── Sensors/             # Domain: sensor hardware model
│       │   │   ├── Sensor.java              # Abstract: code, zone, status, history
│       │   │   ├── NumericSensor.java       # Abstract: thresholds, unit, lastValue
│       │   │   ├── BioSensor.java           # Animal health (temperature, HR, activity)
│       │   │   ├── GPSCollarSensor.java     # Animal GPS (lat/lon + escape detection)
│       │   │   ├── EnvSensor.java           # Crop env (temperature, humidity)
│       │   │   ├── SoilSensor.java          # Crop soil (pH, moisture, N/P/K)
│       │   │   ├── WaterSensor.java         # Aqua water (temp, DO, pH, salinity)
│       │   │   ├── SensorReading.java       # Abstract reading base (timestamp)
│       │   │   ├── NumericSensorReading.java  # value + ReadingLevel severity
│       │   │   ├── GPSSensorReading.java      # lat, lon, isInsideZone
│       │   │   ├── ReadingLevel.java        # Enum: NORMAL | WARNING | CRITICAL
│       │   │   ├── SensorStatus.java        # Enum: Active | Suspended | Faulty
│       │   │   ├── BioMeasureType.java      # Enum: Temperature, HeartRate, …
│       │   │   ├── EnvMeasureType.java      # Enum: Temperature, Humidity, …
│       │   │   ├── SoilMeasureType.java     # Enum: PH, Moisture, Nitrogen, …
│       │   │   └── WaterMeasureType.java    # Enum: Temperature, DissolvedOxygen, …
│       │   │
│       │   ├── Alerts/              # Domain: farm alerts
│       │   │   ├── Alert.java               # Abstract: id, type, severity, lifecycle
│       │   │   ├── HealthAlert.java         # Triggered by HealthEvent
│       │   │   ├── SensorAlert.java         # Triggered by out-of-range reading
│       │   │   ├── AlertManager.java        # Static filter/sort utilities
│       │   │   ├── AlertType.java           # Enum: SensorThreshold, AnimalHealth, …
│       │   │   ├── AlertSeverity.java       # Enum: Critical | Warning
│       │   │   └── AlertResolution.java     # Enum: ACTIVE | ACKNOWLEDGED | RESOLVED | DISMISSED
│       │   │
│       │   ├── Entities/            # Domain: produce & requirements
│       │   │   ├── Crop.java                # Field: variety, growth stage, yield, history
│       │   │   ├── AquacultureSpecies.java  # Species: stock, cycles, harvest records
│       │   │   ├── CropType.java            # Enum: WHEAT, CORN, TOMATO, …
│       │   │   ├── GrowthStage.java         # Enum: SOWING → GERMINATION → … → HARVEST
│       │   │   ├── LIvestockType.java       # Enum: RUMINANT | POULTRY
│       │   │   └── SoilRequirements.java    # pH range, moisture range, drainage flags
│       │   │
│       │   ├── Reports/             # Domain: generated reports
│       │   │   ├── Report.java              # Abstract: period, alert stats, notes
│       │   │   ├── ProductionReport.java    # Abstract: production metrics
│       │   │   ├── FarmReport.java          # Farm-wide zone + animal + alert summary
│       │   │   ├── FarmProductionReport.java # Aggregated production totals
│       │   │   ├── ReportLiveStockZone.java # Per-livestock-zone health & feeding
│       │   │   ├── ReportCropZone.java      # Per-crop-zone stages & soil
│       │   │   ├── ReportAquacultureZone.java # Per-aqua-zone survival & quality
│       │   │   ├── LivestockProductionReport.java
│       │   │   ├── CropProductionReport.java
│       │   │   ├── AquacultureProductionReport.java
│       │   │   └── ReportType.java          # Enum: Daily | Weekly | Monthly | Quarterly | Yearly
│       │   │
│       │   ├── Farm/
│       │   │   └── Farm.java        # Aggregate root: owns all zones, alerts, reports
│       │   │
│       │   ├── Menu/
│       │   │   └── DemoData.java    # Hand-crafted example farm (preset data)
│       │   │
│       │   └── Additional_classes/
│       │       └── Range.java       # Generic min/max pair (pH range, moisture range)
│       │
│       └── resources/com/example/
│           ├── styles/
│           │   ├── main.css         # Light theme (default)
│           │   └── dark.css         # Dark theme (swapped at runtime)
│           └── views/               # FXML layout files (13)
│               ├── startup.fxml
│               ├── main.fxml
│               ├── dashboard.fxml
│               ├── zones.fxml
│               ├── animals.fxml
│               ├── crops.fxml
│               ├── aquaculture.fxml
│               ├── sensors.fxml
│               ├── alerts.fxml
│               ├── reports.fxml
│               ├── simulation.fxml
│               └── settings.fxml
```

---

## Architecture

### Domain Model

The domain is organized into five top-level packages. `Farm.java` is the **aggregate root** — it owns everything:

```
Farm
 ├─ List<LivestockZONE>
 │    ├─ List<Animal>
 │    │    ├─ List<BioSensor>
 │    │    └─ GPSCollarSensor
 │    ├─ List<BioSensor>  (zone-level)
 │    └─ List<GPSCollarSensor>
 ├─ List<CropZONE>
 │    ├─ List<Crop>
 │    ├─ List<EnvSensor>
 │    └─ List<SoilSensor>
 ├─ List<AquacultureZONE>
 │    ├─ List<AquacultureSpecies>
 │    └─ List<WaterSensor>
 └─ List<Alert>          (HealthAlert | SensorAlert)
```

**Polymorphism is used throughout:**

| Base Class | Concrete Types |
|-----------|---------------|
| `ZONE` | `LivestockZONE`, `CropZONE`, `AquacultureZONE` |
| `Sensor` | `NumericSensor` → `BioSensor`, `EnvSensor`, `SoilSensor`, `WaterSensor`; `GPSCollarSensor` |
| `SensorReading` | `NumericSensorReading`, `GPSSensorReading` |
| `Alert` | `HealthAlert`, `SensorAlert` |
| `Report` | `ReportLiveStockZone`, `ReportCropZone`, `ReportAquacultureZone`, `FarmReport` |
| `ProductionReport` | `LivestockProductionReport`, `CropProductionReport`, `AquacultureProductionReport` |

**Sensor inheritance tree:**

```java
Sensor (abstract)
 └─ NumericSensor (abstract)   // adds: minThreshold, maxThreshold, unit, lastValue
     ├─ BioSensor              // + BioMeasureType, Animal reference
     ├─ EnvSensor              // + EnvMeasureType, CropZONE reference
     ├─ SoilSensor             // + SoilMeasureType, CropZONE reference
     └─ WaterSensor            // + WaterMeasureType, AquacultureZONE reference
 └─ GPSCollarSensor            // lat, lon, isInsideZone, Animal reference
```

---

### Service Layer

All services are **singletons** accessed via `Service.getInstance()`. They hold no state themselves — they delegate to the `Farm` object owned by `FarmService`.

```
FarmService          ← central hub; holds the live Farm object
 ├─ AnimalService    ← animal CRUD, health actions, production recording
 ├─ ZoneService      ← zone CRUD, activation, feeding programs
 ├─ SensorService    ← sensor queries, display helpers (does not add readings)
 ├─ AlertService     ← alert queries, lifecycle (acknowledge/resolve/dismiss)
 ├─ ReportService    ← constructs Report objects from current farm state
 └─ PdfReportService ← serializes reports to PDF files via PDFBox
```

`DataRandomizerService` is only called at farm creation time. It uses a **seeded `Random`** derived from the farm's UUID so the same farm ID always regenerates the same data layout.

**Auto-save strategy:** Every mutation method in `AnimalService`, `ZoneService`, etc. calls `FarmService.autoSave()` at the end. `autoSave()` serializes only the structural metadata (zones, animals, boundaries) to `FarmRepository` — sensor reading history is **not persisted** and is regenerated on next session.

---

### Persistence

Data is stored in `~/.smartfarm/farms.properties` (Java `Properties` format). `FarmRepository` is the only class that reads/writes this file.

**Key schema:**

```properties
farm.count=2
farm.0.id=3f8a...
farm.0.name=Green Valley Farm
farm.0.location=Algiers, Algeria
farm.0.owner=Ahmed Bensalem
farm.0.createdAt=2025-03-15
farm.0.isDemo=false
farm.0.wasRandomized=true

farm.0.zone.count=3
farm.0.zone.0.type=LIVESTOCK
farm.0.zone.0.name=North Pasture
farm.0.zone.0.lstType=RUMINANT
farm.0.zone.0.boundary.0.lat=36.7
farm.0.zone.0.boundary.0.lon=3.06
...

farm.0.animal.count=12
farm.0.animal.0.name=Bessie
farm.0.animal.0.species=Holstein
farm.0.animal.0.type=RUMINANT
farm.0.animal.0.age=4
farm.0.animal.0.weight=480.0
farm.0.animal.0.zoneName=North Pasture
farm.0.animal.0.health=Healthy
```

**What is NOT saved:** sensor readings, alert objects, report objects, milk/egg/weight histories. All of this is re-generated by `DataRandomizerService` on every session using the seeded RNG — the seed is derived from the farm's UUID so output is deterministic.

---

### Navigation & Scenes

`SceneManager` is a **SPA-style singleton router** with two root states:

```
App.start()
  └─ SceneManager.initStartup()   → loads startup.fxml into a Scene
       └─ [User picks/creates farm]
            └─ SceneManager.loadMainApp()  → calls scene.setRoot(mainFxml)
                 └─ navigateTo("dashboard")
                      └─ navigateTo("zones") | "animals" | … | "settings"
```

**Fullscreen-safe swapping:** `loadMainApp()` and `navigateToStartup()` call `scene.setRoot(newRoot)` on the **existing Scene** rather than `stage.setScene(newScene)`. This prevents the Windows fullscreen-exit/reenter flicker that occurs when a new Scene is created.

Page navigation calls `BorderPane.setCenter(view)` with a 180 ms fade-in `FadeTransition`.

---

### Sensor & Alert Pipeline

1. **Reading created** — either by `DataRandomizerService` (synthetic) or injected manually via `SensorsController` dialogs.
2. **Level classification** — `NumericSensorReading` sets `ReadingLevel` at construction time:
   ```java
   if (value < sensor.getMinThreshold() || value > sensor.getMaxThreshold())
       severity = ReadingLevel.CRITICAL (or WARNING within 10% of threshold);
   ```
3. **Alert generation** — `DataRandomizerService` checks sensors after populating readings and calls `farm.registerAlert(new SensorAlert(reading))` for CRITICAL readings.
4. **GPS escape detection** — `GPSCollarSensor.addReading()` computes `isInsideZone` by calling `zone.contains(lat, lon)` which uses the **ray-casting point-in-polygon algorithm** in `GoegraphicBoundries`.
5. **Alert display** — `AlertService` provides filtered views; `AlertsController` binds table rows with color-coded severity stripes.

---

### PDF Export Engine

`PdfReportService` wraps Apache PDFBox 3.x. The inner class `PdfWriter` tracks the current Y-position and **auto-creates new pages** when content overflows the bottom margin — callers never manage pagination.

```java
// Typical export flow
try (PdfWriter w = open()) {
    writeDocHeader(w, "Livestock Report");   // title, farm name, date, divider
    w.writeSectionTitle("Summary");          // bold heading + underline
    w.writeRow("Total animals", "42");       // key/value row at fixed offsets
    w.addImage(chartSnapshot, 495f, 230f);  // embed BufferedImage from JavaFX snapshot
    w.save(dest);
}
```

**Chart embedding:** Before opening the `FileChooser`, the controller calls `card.snapshot(new SnapshotParameters(), null)` on the JavaFX Application Thread to produce a `WritableImage`. This is converted to `BufferedImage` via `SwingFXUtils` then embedded with `LosslessFactory.createFromImage()`.

Font: standard `Helvetica` (PDType1Font) — no font embedding needed; works in all PDF viewers.

---

## Installation

### Prerequisites

| Requirement | Version |
|------------|---------|
| JDK | 17 or 21 (LTS) |
| Maven | 3.8+ |
| OS | Windows / macOS / Linux |

> **Note:** JavaFX is bundled as a Maven dependency — no separate JavaFX SDK installation needed.

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/smart-farm-manager.git
cd smart-farm-manager

# 2. Build the project
mvn compile

# 3. Run the application
mvn javafx:run
```

On first launch, the app creates `~/.smartfarm/farms.properties` automatically.

### Build the executable JAR

```bash
mvn package
# Produces target/javafx-app-1.0-SNAPSHOT.jar
# Run with:
java -jar target/javafx-app-1.0-SNAPSHOT.jar
```

---

## Usage

### First Launch

1. The **startup screen** opens. Choose either:
   - **📋 With Sample Data** — fills a new farm with 3 zones, 10+ animals, sensors, and historical data
   - **⬜ Empty Farm** — blank farm, add zones manually
   - **🎲 Random** — one-click random farm with auto-generated name/location/owner
2. Give the farm a **name** and **owner** (required), optional location.
3. Click **Open →** on any saved farm to return to it in future sessions.

### Navigating the App

Use the **left sidebar** to switch between pages. All state is auto-saved on every mutation.

```
🏠 Dashboard      → KPI overview, recent alerts, farm map, quick actions
🗺  Zones          → Add/edit zones, draw boundaries, view GPS trackers
🐄 Animals        → Add animals, update health, record production
🌾 Crops          → Manage crop fields, record harvests, update growth stage
🐟 Aquaculture    → Manage species, record harvests and mortality
📡 Sensors        → Live sensor grid, history charts, inject readings
🔔 Alerts         → View/acknowledge/resolve farm alerts
📊 Reports        → Interactive charts + one-click PDF export
🎮 Simulation     → Mass data injection, timeline acceleration
⚙  Settings       → Farm metadata, theme toggle (Light / Dark)
```

### Exporting a PDF Report

Every chart card in the **Reports** section has a **↓ PDF** button:

```
Reports → Livestock → [chart card "Milk Yield per Zone"] → ↓ PDF
```

This snapshots the visible chart, embeds it in a PDFBox document, and opens a save dialog. The file is written on a background thread — the button shows `⏳` during generation.

To export a full section report (all data, no charts):

```
Reports → [section title row] → 📄 Export PDF
```

### Switching Theme

```
Settings → Theme toggle → 🌙 Dark Mode / ☀ Light Mode
```

The stylesheet is hot-swapped at runtime — all open windows update immediately.

### Programmatic Data Injection (Simulation)

```
Simulation → select sensor → "Inject Reading"
```

Or directly from the **Sensors** page: select a sensor card → **📥 Inject Reading** → enter value → Inject.

---

## Module & Class Reference

### Domain Packages

#### `Farm.Farm`

The aggregate root. Owns all domain objects.

| Method | Returns | Description |
|--------|---------|-------------|
| `addZone(ZONE z)` | `void` | Adds zone to correct typed list |
| `removeZone(ZONE z)` | `boolean` | Removes from list, detaches sensors/animals |
| `getZoneByCode(String code)` | `Optional<ZONE>` | Find by 4-digit code |
| `registerAlert(Alert a)` | `void` | Adds to internal alert list |
| `getAllAlerts()` | `List<Alert>` | Unmodifiable view |
| `getActiveAlerts()` | `List<Alert>` | Filtered: `isActive()` only |
| `generateReportForZone(ZONE, ReportType)` | `Report` | Polymorphic: dispatches to correct Report subclass |
| `getStats()` | `FarmStats` | Counts: zones, animals, sensors, active alerts |

#### `ZONES.ZONE` (abstract)

| Field | Type | Description |
|-------|------|-------------|
| `code` | `String` | Auto-incremented 4-digit (e.g. "0042") |
| `name` | `String` | Human-readable name |
| `status` | `ZoneStatus` | ACTIVE or SUSPENDED |
| `boundary` | `GoegraphicBoundries` | Nullable polygon |

| Method | Description |
|--------|-------------|
| `activate()` | Sets status ACTIVE |
| `suspend()` | Sets status SUSPENDED |
| `contains(double lat, double lon)` | Ray-casting point-in-polygon test on boundary |
| `hasBoundaries()` | True if boundary polygon is set |

#### `Animals.Animal`

| Constructor | `Animal(name, species, LIvestockType, age, weight)` |
|-------------|---|

| Method | Description |
|--------|-------------|
| `updateWeight(double kg)` | Adds `WeightRecord` with current timestamp |
| `recordMilkYield(double liters)` | Adds `MilkRecord` |
| `recordEgg(int count)` | Adds `EggRecord` |
| `addHealthEvent(HealthEvent)` | Appends to history; updates `healthStatus` |
| `resolveLastHealthEvent()` | Marks last event resolved; sets status Healthy |
| `isQuarantined()` | Shortcut: `healthStatus == Quarantined` |
| `getMilkYieldLiters()` | Sum of all MilkRecords |
| `getTotalEggCount()` | Sum of all EggRecords |
| `getWeightHistory()` | Unmodifiable `List<WeightRecord>` |

#### `Sensors.NumericSensor` (abstract)

| Method | Description |
|--------|-------------|
| `sendReading()` | Creates `NumericSensorReading(lastValue, timestamp)` — classify severity |
| `setLastValue(double)` | Updates current reading value |
| `setMinThreshold(double)` | Validates min < max |
| `setMaxThreshold(double)` | Validates max > min |
| `isOutOfRange(double value)` | `value < min || value > max` |
| `getReadingHistory()` | Unmodifiable `List<SensorReading>` |
| `filterByLevel(List, ReadingLevel)` | Static utility: filter readings by severity |

#### `Entities.AquacultureSpecies`

| Method | Description |
|--------|-------------|
| `harvest(double kg, int count)` | Appends `HarvestRecord`; decrements `numSpecies` |
| `recordMortality(int count)` | Decrements `numSpecies` without a harvest record |
| `restock(int count)` | Adds stock; sets new cycle baseline |
| `getCycleSurvivalRatePercent()` | `(cycleBaseline - cycleMortality) / cycleBaseline * 100` |
| `getTotalSurvivalRatePercent()` | Based on original `initialTotalIndividuals` |
| `getTotalHarvestWeightKg()` | Sum of all `HarvestRecord.weightKg` |

#### `Alerts.Alert` (abstract)

| Method | Description |
|--------|-------------|
| `acknowledge()` | Transitions ACTIVE → ACKNOWLEDGED |
| `resolve()` | Transitions * → RESOLVED |
| `dismiss()` | Transitions * → DISMISSED |
| `isActive()` | `resolution == ACTIVE` |
| `getZone()` | Abstract — subclass derives zone from animal/sensor |
| `getZoneName()` | `getZone().getName()` |

---

### Services

#### `FarmService`

```java
// One-time initialization (call before getInstance())
FarmService.initWithNewFarm(String name, String location, String owner)
FarmService.initFromSaved(FarmRepository.SavedFarm sf)

// Singleton access
FarmService svc = FarmService.getInstance();
svc.getFarm();               // Farm aggregate root
svc.getFarmName();           // String
svc.autoSave();              // writes ~/.smartfarm/farms.properties
svc.setFarmBoundary(GoegraphicBoundries b);
svc.hasFarmBoundary();       // boolean
```

#### `AnimalService`

```java
AnimalService svc = AnimalService.getInstance();

List<Animal> all      = svc.getAllAnimals();
List<Animal> byZone   = svc.getAnimalsByZone(zone);
List<Animal> bySick   = svc.getAnimalsByHealthStatus(AnimalHealthStatus.Sick);

svc.addAnimal(animal, zone);       // auto-saves
svc.removeAnimal(animal);          // auto-saves
svc.recordWeight(animal, 320.5);   // adds WeightRecord + auto-saves
svc.recordMilkYield(animal, 12.0); // adds MilkRecord + auto-saves
```

#### `ZoneService`

```java
ZoneService svc = ZoneService.getInstance();

List<ZONE>           all  = svc.getAllZones();
List<LivestockZONE>  ls   = svc.getLivestockZones();
List<CropZONE>       crop = svc.getCropZones();
List<AquacultureZONE>aq   = svc.getAquacultureZones();

svc.addZone(zone);              // auto-saves
svc.removeZone(zone);           // auto-saves
svc.renameZone(zone, "New Name");
svc.activateZone(zone);
svc.suspendZone(zone);
```

#### `SensorService`

```java
SensorService svc = SensorService.getInstance();

List<Sensor>          all   = svc.getAllSensors();
List<BioSensor>       bio   = svc.getAllBioSensors();
List<GPSCollarSensor> gps   = svc.getAllGPSSensors();
List<EnvSensor>       env   = svc.getAllEnvSensors();
List<WaterSensor>     water = svc.getAllWaterSensors();

ReadingLevel level  = svc.getLastReadingLevel(sensor);    // NORMAL | WARNING | CRITICAL
String       label  = svc.getSensorTypeLabel(sensor);     // "Bio - Temperature"
String       display= svc.getLastReadingDisplay(sensor);  // "38.4 °C"
```

#### `AlertService`

```java
AlertService svc = AlertService.getInstance();

List<Alert> all      = svc.getAllAlerts();
List<Alert> active   = svc.getActiveAlerts();
List<Alert> critical = svc.getAlertsBySeverity(AlertSeverity.Critical);
List<Alert> byZone   = svc.getAlertsByZone(zone);

long count = svc.getActiveAlertCount();
svc.acknowledge(alert);
svc.resolve(alert);
```

#### `PdfReportService`

```java
PdfReportService pdf = PdfReportService.getInstance();

// Section exports (opens FileChooser internally via ReportsController)
pdf.exportOverview(File dest);
pdf.exportLivestock(File dest);
pdf.exportCrops(File dest);
pdf.exportAquaculture(File dest);
pdf.exportAlerts(File dest);
pdf.exportSensors(File dest);
pdf.exportFull(File dest);

// Chart-image exports (dest from FileChooser, chartImage from JavaFX snapshot)
pdf.exportSensorChart(NumericSensor sensor,
                      List<NumericSensorReading> readings,
                      BufferedImage chartImage, File dest);
pdf.exportGpsChart(GPSCollarSensor sensor,
                   List<GPSSensorReading> readings,
                   BufferedImage chartImage, File dest);
pdf.exportChartImage(String title, BufferedImage chartImage, File dest);
```

---

### Controllers

Each controller is instantiated by `FXMLLoader` when `SceneManager.navigateTo(route)` is called. Controllers must **not** hold inter-session state — re-loading the FXML resets them.

| Controller | Key `@FXML` methods |
|-----------|---------------------|
| `DashboardController` | `quickAddAnimal()`, `quickAddZone()`, `quickViewReports()` |
| `ZonesController` | `showAddZoneDialog()`, `activateZone()`, `suspendZone()`, `showBoundaryEditor()` |
| `AnimalsController` | `showAddAnimalDialog()`, `removeAnimal()`, `recordWeight()` |
| `CropsController` | `showAddCropDialog()`, `updateGrowthStage()`, `recordHarvest()` |
| `AquacultureController` | `showAddSpeciesDialog()`, `recordHarvest()`, `recordMortality()` |
| `SensorsController` | `applyFilters()`, `showDetail()`, `showInjectReadingDialog()` |
| `AlertsController` | `acknowledge()`, `resolve()`, `dismiss()` |
| `ReportsController` | `showOverview()`, `showLivestock()`, … `showExport()`, `exportChartNodePdf()` |
| `SettingsController` | `toggleTheme()`, `saveChanges()`, `navigateToStartup()` |

---

### Utilities

#### `SceneManager`

```java
// Step 1 — App.start()
SceneManager.initStartup(Stage stage);

// Step 2 — StartupController (after farm chosen)
SceneManager.getInstance().loadMainApp();

// Anywhere
SceneManager.getInstance().navigateTo("sensors");
SceneManager.getInstance().navigateToStartup();
SceneManager.getInstance().applyStylesheet("/com/example/styles/dark.css");
SceneManager.getInstance().getRoot();  // BorderPane
```

#### `FarmRepository`

```java
List<FarmRepository.SavedFarm> all = FarmRepository.loadAll();
FarmRepository.save(savedFarm);
FarmRepository.delete(savedFarm.id);

// SavedFarm fields
savedFarm.id          // String UUID
savedFarm.name        // String
savedFarm.isDemo      // boolean
savedFarm.zoneCount() // int (number of zones)
savedFarm.subtitle()  // e.g. "Algiers · Ahmed"
```

---

## Configuration

The application has **no config files**. All user-facing options are in the **Settings** page and all data lives in one properties file.

| Setting | Location | Default | Effect |
|---------|----------|---------|--------|
| Farm data file | `~/.smartfarm/farms.properties` | Created on first launch | Stores all saved farms |
| Active theme | Runtime only (not persisted) | Light | Controls which CSS file is loaded |
| Farm name / location / owner | Settings page → Save | From creation form | Shown in header bar and reports |
| Sensor thresholds | Sensors page → Edit Thresholds | Set during randomization | Determines WARNING/CRITICAL level |

> **Persistence location:** The properties file is placed in the user's home directory under `.smartfarm/`. On Windows this is `C:\Users\<name>\.smartfarm\farms.properties`; on Linux/macOS `~/.smartfarm/farms.properties`.

---

## Dependencies

| Dependency | Version | Why |
|-----------|---------|-----|
| `javafx-controls` | 21 | UI controls (Button, TableView, ComboBox, charts…) |
| `javafx-fxml` | 21 | Declarative XML layouts + `FXMLLoader` |
| `javafx-swing` | 21 | `SwingFXUtils.fromFXImage()` for JavaFX→AWT image conversion (PDF embedding) |
| `ikonli-javafx` | 12.3.1 | Icon font integration — `FontIcon` nodes in FXML |
| `ikonli-fontawesome5-pack` | 12.3.1 | FontAwesome 5 icon set (paw, bell, satellite-dish…) |
| `pdfbox` | 3.0.2 | PDF document creation — `PDDocument`, `PDPageContentStream`, `LosslessFactory` |
| Java `java.desktop` | 21 | `BufferedImage` for chart snapshot conversion |

**Maven plugin:** `javafx-maven-plugin 0.0.8` — runs `mvn javafx:run` and packages the modular application.

---

## Contributing

```bash
# 1. Fork the repository on GitHub

# 2. Clone your fork
git clone https://github.com/<your-username>/smart-farm-manager.git

# 3. Create a feature branch (naming convention: feature/description)
git checkout -b feature/add-weather-sensor

# 4. Make changes — follow the existing conventions:
#    - New domain types: extend the appropriate abstract base (ZONE, Sensor, Alert, Report)
#    - New pages: add FXML + Controller + register route in SceneManager.ROUTES
#    - CSS: add to main.css AND dark.css (no hardcoded colors in Java/FXML)
#    - Auto-save: call FarmService.autoSave() after any mutation

# 5. Compile and test manually
mvn compile
mvn javafx:run

# 6. Commit with a descriptive message
git commit -m "feat: add WeatherSensor type with humidity/wind readings"

# 7. Push and open a Pull Request
git push origin feature/add-weather-sensor
```

### Code Conventions

- **No inline styles in Java/FXML** — use CSS classes so both themes work
- **Singleton services** — all business logic through `Service.getInstance()`
- **Auto-save on mutation** — every write method must call `FarmService.autoSave()`
- **Deterministic seeding** — `DataRandomizerService` derives its seed from `farm.getId().hashCode()` for reproducible results
- **History objects** — use immutable inner record classes (e.g. `WeightRecord`, `YieldRecord`) with a timestamp field
- **No test suite exists** — manual testing on the demo farm is the verification method

### Adding a New Zone Type

1. Create `ZONES/GreenhouseZONE.java` extending `ZONE`
2. Add sensors list (e.g. `List<EnvSensor>`)
3. Update `Farm.java` to hold `List<GreenhouseZONE>`
4. Update `ZoneService` with `getGreenhouseZones()`
5. Update `AddZoneDialog` type picker
6. Update `DataRandomizerService` with a `populateGreenhouseZone()` method
7. Add a `ReportGreenhouseZone` class extending `Report`
8. Add FXML card entries in `zones.fxml` / `reports.fxml`

### Adding a New Sensor Type

1. Create the sensor class extending `NumericSensor` (or `Sensor` for non-numeric)
2. Add a `MeasureType` enum if needed
3. Wire it into the relevant zone class
4. Add a sensor type key in `SensorsController.sensorTypeKey()`
5. Add `sensor-type-<key>` CSS class in both `main.css` and `dark.css`
6. Update `SensorService` getter methods
7. Update `PdfReportService.exportSensors()` to include the new type

---

## License

```
MIT License

Copyright (c) 2025 Mahieddine Mohamed Mimoun

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
