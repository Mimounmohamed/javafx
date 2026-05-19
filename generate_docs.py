#!/usr/bin/env python3
"""Farm Management System — Technical Documentation Generator"""

import datetime
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY, TA_RIGHT
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, HRFlowable, KeepTogether
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.platypus import BaseDocTemplate, Frame, PageTemplate, NextPageTemplate

# ── Colours ──────────────────────────────────────────────────────────────────
C_DARK_GREEN   = colors.HexColor("#1a5c2a")
C_MED_GREEN    = colors.HexColor("#2e7d32")
C_LIGHT_GREEN  = colors.HexColor("#e8f5e9")
C_ALT_ROW      = colors.HexColor("#f9fbe7")
C_WHITE        = colors.white
C_BORDER       = colors.HexColor("#c8e6c9")
C_MUTED        = colors.HexColor("#546e7a")
C_HEADER_LINE  = colors.HexColor("#2e7d32")
C_DANGER       = colors.HexColor("#ef4444")
C_BLUE         = colors.HexColor("#0ea5e9")

W, H = A4

def esc(s):
    """Escape XML special characters for Paragraph."""
    return str(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

# ── Styles ────────────────────────────────────────────────────────────────────
BASE = getSampleStyleSheet()

def make_styles():
    styles = {}
    styles["cover_title"] = ParagraphStyle("cover_title",
        fontName="Helvetica-Bold", fontSize=28, leading=36,
        alignment=TA_CENTER, textColor=C_DARK_GREEN, spaceAfter=10)
    styles["cover_sub"] = ParagraphStyle("cover_sub",
        fontName="Helvetica", fontSize=14, leading=20,
        alignment=TA_CENTER, textColor=C_MUTED, spaceAfter=6)
    styles["cover_info"] = ParagraphStyle("cover_info",
        fontName="Helvetica", fontSize=11, leading=16,
        alignment=TA_CENTER, textColor=colors.black, spaceAfter=4)
    styles["section"] = ParagraphStyle("section",
        fontName="Helvetica-Bold", fontSize=16, leading=22,
        textColor=C_DARK_GREEN, spaceBefore=18, spaceAfter=8)
    styles["h2"] = ParagraphStyle("h2",
        fontName="Helvetica-Bold", fontSize=13, leading=18,
        textColor=C_DARK_GREEN, spaceBefore=10, spaceAfter=4)
    styles["h3"] = ParagraphStyle("h3",
        fontName="Helvetica-Bold", fontSize=10, leading=14,
        textColor=colors.black, spaceBefore=6, spaceAfter=2)
    styles["body"] = ParagraphStyle("body",
        fontName="Helvetica", fontSize=10, leading=14,
        alignment=TA_JUSTIFY, spaceAfter=4)
    styles["bullet"] = ParagraphStyle("bullet",
        fontName="Helvetica", fontSize=10, leading=14,
        leftIndent=14, spaceAfter=2)
    styles["toc1"] = ParagraphStyle("toc1",
        fontName="Helvetica-Bold", fontSize=11, leading=16,
        textColor=C_DARK_GREEN)
    styles["toc2"] = ParagraphStyle("toc2",
        fontName="Helvetica", fontSize=10, leading=14,
        leftIndent=20, textColor=colors.black)
    styles["muted"] = ParagraphStyle("muted",
        fontName="Helvetica", fontSize=9, leading=12,
        textColor=C_MUTED, spaceAfter=2)
    styles["code"] = ParagraphStyle("code",
        fontName="Courier", fontSize=9, leading=12,
        textColor=C_DARK_GREEN)
    return styles

ST = make_styles()

# ── Table helpers ─────────────────────────────────────────────────────────────

def tbl_style(extra=None):
    base = [
        ("BACKGROUND",  (0,0), (-1,0), C_DARK_GREEN),
        ("TEXTCOLOR",   (0,0), (-1,0), C_WHITE),
        ("FONTNAME",    (0,0), (-1,0), "Helvetica-Bold"),
        ("FONTSIZE",    (0,0), (-1,0), 9),
        ("FONTNAME",    (0,1), (-1,-1), "Helvetica"),
        ("FONTSIZE",    (0,1), (-1,-1), 9),
        ("ROWBACKGROUNDS", (0,1), (-1,-1), [C_WHITE, C_ALT_ROW]),
        ("GRID",        (0,0), (-1,-1), 0.5, C_BORDER),
        ("VALIGN",      (0,0), (-1,-1), "TOP"),
        ("TOPPADDING",  (0,0), (-1,-1), 4),
        ("BOTTOMPADDING",(0,0),(-1,-1), 4),
        ("LEFTPADDING", (0,0), (-1,-1), 5),
        ("RIGHTPADDING",(0,0),(-1,-1), 5),
        ("WORDWRAP",    (0,0), (-1,-1), True),
        ("SPLITBYROW",  (0,0), (-1,-1), True),
    ]
    if extra:
        base.extend(extra)
    return TableStyle(base)

def make_table(headers, rows, col_widths):
    pw = sum(col_widths)
    data = [[Paragraph(f"<b>{esc(h)}</b>", ST["body"]) for h in headers]]
    for row in rows:
        data.append([Paragraph(esc(str(c)), ST["body"]) for c in row])
    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(tbl_style())
    return t

def class_header(name, kind, pkg, extends=""):
    bg_data = [[Paragraph(
        f'<font color="#1a5c2a"><b>{esc(name)}</b></font>'
        + f'  <font color="#546e7a" size="9">({esc(kind)})</font>',
        ST["h2"])]]
    hdr = Table(bg_data, colWidths=[16*cm])
    hdr.setStyle(TableStyle([
        ("BACKGROUND",(0,0),(-1,-1), C_LIGHT_GREEN),
        ("BOX",(0,0),(-1,-1),0.8, C_BORDER),
        ("LEFTPADDING",(0,0),(-1,-1),8),
        ("TOPPADDING",(0,0),(-1,-1),5),
        ("BOTTOMPADDING",(0,0),(-1,-1),5),
    ]))
    meta = []
    meta.append(Paragraph(f"<b>Package:</b> <font name='Courier'>{esc(pkg)}</font>", ST["muted"]))
    if extends:
        meta.append(Paragraph(f"<b>Extends / Implements:</b> {esc(extends)}", ST["muted"]))
    return [hdr] + meta

def subsection(title):
    return Paragraph(f"<b>{esc(title)}</b>", ST["h3"])

# ── Header / Footer callbacks ──────────────────────────────────────────────────

def draw_header_footer(canvas, doc):
    canvas.saveState()
    # header
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(C_MUTED)
    canvas.drawString(2.5*cm, H - 1.8*cm, "Farm Management System — Technical Documentation")
    canvas.drawRightString(W - 2.5*cm, H - 1.8*cm, "ESI Alger — 2CP — 2025/2026")
    canvas.setStrokeColor(C_HEADER_LINE)
    canvas.setLineWidth(0.5)
    canvas.line(2.5*cm, H - 2.0*cm, W - 2.5*cm, H - 2.0*cm)
    # footer
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(C_MUTED)
    canvas.drawCentredString(W/2, 1.5*cm, f"Page {doc.page}")
    canvas.drawRightString(W - 2.5*cm, 1.5*cm, "ESI Alger — 2CP")
    canvas.restoreState()

def draw_cover(canvas, doc):
    canvas.saveState()
    canvas.restoreState()

# ── Document builder ───────────────────────────────────────────────────────────

class DocBuilder(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(filename, pagesize=A4,
                         leftMargin=2.5*cm, rightMargin=2.5*cm,
                         topMargin=2.8*cm, bottomMargin=2.5*cm)
        cover_frame = Frame(0, 0, W, H, id="cover")
        normal_frame = Frame(2.5*cm, 2.5*cm, W-5*cm, H-5.3*cm, id="normal")
        self.addPageTemplates([
            PageTemplate(id="Cover",  frames=[cover_frame],  onPage=draw_cover),
            PageTemplate(id="Normal", frames=[normal_frame], onPage=draw_header_footer),
        ])
        self.toc = TableOfContents()
        self.toc.levelStyles = [ST["toc1"], ST["toc2"]]

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph):
            style = flowable.style.name
            txt   = flowable.getPlainText()
            if style == "section":
                self.notify("TOCEntry", (0, txt, self.page))
            elif style == "h2":
                self.notify("TOCEntry", (1, txt, self.page))

# ── Content builders ──────────────────────────────────────────────────────────

def cover_page():
    elems = []
    elems.append(Spacer(1, 5*cm))
    elems.append(Paragraph("Farm Management System", ST["cover_title"]))
    elems.append(Paragraph("Complete Technical Documentation", ST["cover_sub"]))
    elems.append(Spacer(1, 0.5*cm))
    elems.append(HRFlowable(width="80%", color=C_DARK_GREEN, thickness=2))
    elems.append(Spacer(1, 0.8*cm))

    info = [
        ("Project",   "Farm Intelligent Management Desktop App"),
        ("Framework", "JavaFX 21 (Maven build)"),
        ("Pattern",   "MVC + Service Layer"),
        ("School",    "Ecole Nationale Superieure d'Informatique (ESI Alger)"),
        ("Level",     "2CP -- Academic Year 2025/2026"),
        ("Generated", datetime.datetime.now().strftime("%d %B %Y")),
    ]
    tdata = [[Paragraph(f"<b>{esc(k)}</b>", ST["cover_info"]),
              Paragraph(esc(v), ST["cover_info"])] for k,v in info]
    t = Table(tdata, colWidths=[5*cm, 10*cm])
    t.setStyle(TableStyle([
        ("ALIGN",(0,0),(0,-1),"RIGHT"),
        ("ALIGN",(1,0),(1,-1),"LEFT"),
        ("FONTNAME",(0,0),(-1,-1),"Helvetica"),
        ("FONTSIZE",(0,0),(-1,-1),11),
        ("TOPPADDING",(0,0),(-1,-1),4),
        ("BOTTOMPADDING",(0,0),(-1,-1),4),
        ("TEXTCOLOR",(0,0),(0,-1),C_DARK_GREEN),
    ]))
    elems.append(t)
    elems.append(PageBreak())
    return elems

def toc_section(toc):
    return [
        Paragraph("Table of Contents", ST["section"]),
        toc,
        PageBreak()
    ]

def sec_overview():
    e = [PageBreak(), Paragraph("1.  Project Overview", ST["section"])]
    e.append(Paragraph(
        "The Farm Management System is a desktop application built with JavaFX 21 and Maven. "
        "It provides an integrated platform for managing all aspects of a modern farm: "
        "livestock zone management and animal health monitoring, crop field tracking from sowing to harvest, "
        "aquaculture species population and harvest records, a distributed sensor network (bio, environmental, "
        "soil, water, and GPS collar sensors), a real-time alert system with severity classification, "
        "structured production reports, and a time-step simulation engine that advances farm state day by day.",
        ST["body"]))
    e.append(Spacer(1, 0.3*cm))
    rows = [
        ("Model",      "Java domain classes",  "Represent farm entities, sensors, alerts, reports"),
        ("Service",    "Java singleton classes","Business logic, data filtering, auto-save"),
        ("Controller", "JavaFX + FXML",         "Handle UI events, call services, update views"),
        ("View",       "FXML + CSS",            "Declare UI layout, bind to controller methods"),
        ("Utility",    "Java (SceneManager etc)","SPA navigation, persistence, random data generation"),
    ]
    e.append(make_table(["Layer","Technology","Role"], rows,
                         [3.5*cm, 4.5*cm, 8.5*cm]))
    return e

def sec_architecture():
    e = [PageBreak(), Paragraph("2.  Architecture and MVC Pattern", ST["section"])]
    e.append(Paragraph(
        "The application follows a strict MVC + Service Layer pattern. "
        "The Model layer comprises all domain Java classes (Animals, ZONES, Sensors, Reports, etc.) "
        "that hold data and enforce invariants through constructor validation and immutable return types. "
        "The Service layer (com.example.services) provides singleton facades over the Model, "
        "encapsulating business logic so that controllers never manipulate domain objects directly. "
        "Each Controller (com.example.controllers) is paired with exactly one FXML view file; "
        "it reads from Services, updates JavaFX controls, and handles user events via @FXML-annotated methods. "
        "Navigation between screens is managed by SceneManager, a singleton that loads FXML views into the "
        "center of a BorderPane shell (SPA-style — no new windows).",
        ST["body"]))
    e.append(Spacer(1, 0.2*cm))
    e.append(subsection("Request Flow"))
    e.append(Paragraph(
        "User action  →  @FXML handler in Controller  →  Service method  →  "
        "Domain model mutation  →  autoSave()  →  Controller refreshes ObservableList / chart / label",
        ST["body"]))
    e.append(Spacer(1,0.2*cm))
    e.append(subsection("SPA Navigation (SceneManager)"))
    e.append(Paragraph(
        "SceneManager.initStartup() loads startup.fxml into the primary Stage. "
        "Once the user selects a farm, loadMainApp() loads main.fxml (containing the sidebar and top-bar shell) "
        "and calls navigateTo('dashboard'). Every sidebar button calls navigateTo(route), which looks up the "
        "FXML path in the ROUTES map, loads it fresh via FXMLLoader, and sets it as the center of the "
        "root BorderPane. A short FadeTransition is played on each page load.",
        ST["body"]))
    ascii_box = (
        "  +------------------------- App Shell (main.fxml) ---------------------------+\n"
        "  |  Sidebar (MainController)    |  Top-bar (MainController)                  |\n"
        "  |  [Dashboard] [Zones] ...     |  Farm Name  |  Clock  |  Alert Badge       |\n"
        "  |------------------------------|--------------------------------------------|\n"
        "  |                                                                            |\n"
        "  |            Center (set by SceneManager.navigateTo(route))                  |\n"
        "  |            dashboard.fxml / zones.fxml / animals.fxml / ...               |\n"
        "  |                                                                            |\n"
        "  +----------------------------------------------------------------------------+"
    )
    e.append(Paragraph(f"<font name='Courier' size='8'>{esc(ascii_box)}</font>", ST["body"]))
    return e

# ── Generic package intro ─────────────────────────────────────────────────────

def pkg_intro(num, pkg_name, description):
    return [PageBreak(),
            Paragraph(f"{num}.  Package: {pkg_name}", ST["section"]),
            Paragraph(description, ST["body"]),
            Spacer(1, 0.2*cm)]

def HR():
    return HRFlowable(width="100%", color=C_BORDER, thickness=0.5, spaceAfter=6, spaceBefore=6)

# ═══════════════════════════════════════════════════════════════════════════════
# PACKAGE SECTIONS
# ═══════════════════════════════════════════════════════════════════════════════

def sec_additional_classes():
    e = pkg_intro("3", "Additional_classes",
        "Contains shared utility classes used across the domain layer. "
        "Currently holds the Range class, a simple numeric interval used for sensor thresholds "
        "and crop optimal conditions.")

    e += class_header("Range", "Concrete Class", "Additional_classes")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Represents a closed numeric interval [min, max]. Used by NumericSensor subclasses to "
        "define normal operating thresholds, and by Crop to store optimal pH and moisture ranges. "
        "Enforces that min never exceeds max through constructor and setter validation.",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Visibility","Description"],
        [("min","double","private","Lower bound of the interval"),
         ("max","double","private","Upper bound of the interval")],
        [3*cm, 2.5*cm, 2.5*cm, 8.5*cm]))
    e.append(subsection("CONSTRUCTOR"))
    e.append(make_table(["Parameters","Behaviour"],
        [("double min, double max",
          "Throws IllegalArgumentException if min > max. Sets both fields.")],
        [5*cm, 11.5*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Parameters","Logic"],
        [("getMin","double","—","Returns the lower bound."),
         ("setMin","void","double min","Validates min <= max then updates field."),
         ("getMax","double","—","Returns the upper bound."),
         ("setMax","void","double max","Validates max >= min then updates field."),
         ("isInRange","boolean","double value","Returns true if value >= min && value <= max.")],
        [3.5*cm, 2*cm, 3*cm, 8*cm]))
    e.append(subsection("RELATIONSHIPS"))
    e.append(Paragraph("• Used by → NumericSensor (thresholds), Crop (pH and moisture ranges)", ST["bullet"]))
    return e

def sec_alerts():
    e = pkg_intro("4", "Alerts",
        "The Alerts package models the alert subsystem. An abstract Alert base class captures "
        "common state (id, type, severity, message, timestamp, resolution). Two concrete subclasses "
        "specialise it: HealthAlert (linked to a HealthEvent) and SensorAlert (linked to a SensorReading). "
        "AlertManager provides list-management and static filter/sort utilities. "
        "Three enums encode the alert taxonomy: AlertType, AlertSeverity, AlertResolution.")

    # Alert
    e += class_header("Alert", "Abstract Class", "Alerts")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Base class for all farm alerts. Generates a UUID on construction, records the current "
        "timestamp, and starts in ACTIVE resolution state. Subclasses must implement getZone() "
        "to return the zone associated with the alert.",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Vis","Description"],
        [("id","String","private","UUID auto-generated on creation"),
         ("type","AlertType","private","Category of alert (sensor type or health)"),
         ("severity","AlertSeverity","private","Warning or Critical"),
         ("message","String","private","Human-readable description"),
         ("timestamp","LocalDateTime","private","When the alert was raised"),
         ("resolution","AlertResolution","private","ACTIVE / ACKNOWLEDGED / RESOLVED / DISMISSED")],
        [3*cm, 3*cm, 2*cm, 8.5*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("acknowledge()","void","Sets resolution to ACKNOWLEDGED."),
         ("resolve()","void","Sets resolution to RESOLVED."),
         ("dismiss()","void","Sets resolution to DISMISSED."),
         ("isActive()","boolean","True if resolution == ACTIVE."),
         ("isClosed()","boolean","True if RESOLVED or DISMISSED."),
         ("getFormattedTimestamp()","String","Formats timestamp as yyyy-MM-dd HH:mm:ss."),
         ("getZoneName()","String","Calls getZone(); returns zone name or 'N/A'."),
         ("getZone()","ZONE","Abstract — subclass returns the affected zone.")],
        [4.5*cm, 2.5*cm, 9.5*cm]))
    e.append(HR())

    # AlertManager
    e += class_header("AlertManager", "Concrete Class", "Alerts")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Manages two lists of Alert objects: activeAlerts (open alerts) and alertHistory (all-time). "
        "Provides lifecycle methods (receive, acknowledge, resolve, dismiss), filter queries, "
        "and static utility methods that can be applied to any alert list.",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("activeAlerts","List<Alert>","Currently open alerts (ACTIVE or ACKNOWLEDGED)"),
         ("alertHistory","List<Alert>","Full immutable history of every alert ever received")],
        [4*cm, 4*cm, 8.5*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("receiveAlert(Alert)","void","Adds to both lists; prints to console."),
         ("acknowledgeAlert(id)","void","Finds active alert by id, calls acknowledge()."),
         ("resolveAlert(id)","void","Finds active alert, calls resolve(), removes from activeAlerts."),
         ("dismissAlert(id)","void","Finds active alert, calls dismiss(), removes from activeAlerts."),
         ("getCriticalAlerts()","List<Alert>","Filters activeAlerts by AlertSeverity.Critical."),
         ("getWarningAlerts()","List<Alert>","Filters activeAlerts by AlertSeverity.Warning."),
         ("getAlertsByType(type)","List<Alert>","Filters activeAlerts by AlertType."),
         ("getByResolution(res)","List<Alert>","Filters alertHistory by AlertResolution."),
         ("getStatsSummary()","String","Returns formatted counts: total, active, critical, warning, resolved, dismissed."),
         ("filterBySeverity(list, sev)","List<Alert>","Static. Returns alerts with matching severity."),
         ("filterByType(list, type)","List<Alert>","Static. Returns alerts with matching type."),
         ("filterByZone(list, zone)","List<Alert>","Static. Returns alerts whose getZone() equals the given zone."),
         ("filterByPeriod(list,from,to)","List<Alert>","Static. Returns alerts with timestamp in [from, to]."),
         ("sortBySeverity(list)","List<Alert>","Static. Critical first, then by timestamp descending.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    # Enums
    e += class_header("AlertResolution", "Enum", "Alerts")
    e.append(Paragraph("Values: <b>ACTIVE</b>, <b>ACKNOWLEDGED</b>, <b>RESOLVED</b>, <b>DISMISSED</b>. "
        "Represents the lifecycle state of an alert from creation to closure.", ST["body"]))
    e.append(HR())

    e += class_header("AlertSeverity", "Enum", "Alerts")
    e.append(Paragraph("Values: <b>Warning</b>, <b>Critical</b>. "
        "Warning indicates a condition requiring attention; Critical requires immediate intervention.", ST["body"]))
    e.append(HR())

    e += class_header("AlertType", "Enum", "Alerts")
    e.append(Paragraph(
        "Values: <b>BioSensorAlert</b>, <b>WaterSensorAlert</b>, <b>SoilSensorAlert</b>, "
        "<b>EnvSensorAlert</b>, <b>GPS_ESCAPE_ALERT</b>, <b>HEALTH_ALERT</b>. "
        "Each value corresponds to the sensor category or event source that generated the alert.",
        ST["body"]))
    e.append(HR())

    e += class_header("HealthAlert", "Concrete Class", "Alerts", "extends Alert")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "An alert triggered by an animal health event (illness, quarantine). "
        "Stores a reference to the HealthEvent that triggered it. "
        "Validates that the AlertType is exactly HEALTH_ALERT.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("triggeringEvent","HealthEvent","The health event that caused this alert")],
        [4*cm, 4*cm, 8.5*cm]))
    e.append(make_table(["Method","Return","Logic"],
        [("getZone()","ZONE","Returns triggeringEvent.getAnimal().getZone()."),
         ("getTriggerReading()","HealthEvent","Returns the stored HealthEvent.")],
        [4.5*cm, 2.5*cm, 9.5*cm]))
    e.append(HR())

    e += class_header("SensorAlert", "Concrete Class", "Alerts", "extends Alert")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "An alert triggered by a sensor reading that exceeded its threshold. "
        "Stores the SensorReading that caused the breach. "
        "Validates that the AlertType is one of the four sensor-type values or GPS_ESCAPE_ALERT.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("triggeringEvent","SensorReading","The out-of-range reading that raised the alert")],
        [4*cm, 4*cm, 8.5*cm]))
    e.append(make_table(["Method","Return","Logic"],
        [("getZone()","ZONE","Returns triggeringEvent.getSensor().getZone()."),
         ("getTriggerReading()","SensorReading","Returns the stored SensorReading.")],
        [4.5*cm, 2.5*cm, 9.5*cm]))
    return e

def sec_animals():
    e = pkg_intro("5", "Animals",
        "The Animals package models individual farm animals and their health, feeding, "
        "and production data. The Animal class is the central entity, carrying inner record classes "
        "for production history (WeightRecord, MilkRecord, EggRecord). FeedingProgram encapsulates "
        "a scheduled feeding regime. HealthEvent logs a health status change. "
        "AnimalHealthStatus is an enum with three states.")

    e += class_header("Animal", "Concrete Class", "Animals")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Represents a single farm animal. Tracks identity (id, name, species), "
        "physical attributes (age, weight), health status, optional GPS collar and bio sensors, "
        "and production history for milk, eggs, and weight. "
        "Three static inner classes (WeightRecord, MilkRecord, EggRecord) represent immutable "
        "timestamped production observations.",
        ST["body"]))
    e.append(subsection("INNER CLASSES"))
    e.append(make_table(["Class","Fields","Description"],
        [("WeightRecord","double weight, LocalDateTime timestamp","One weight measurement with timestamp"),
         ("MilkRecord","double liters, LocalDateTime timestamp","One milk-yield measurement with timestamp"),
         ("EggRecord","int count, LocalDateTime timestamp","One egg-count measurement with timestamp")],
        [3.5*cm, 6*cm, 7*cm]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("id","String","UUID generated at construction"),
         ("name","String","Animal's display name"),
         ("species","String","Biological species label (e.g. 'Bovine')"),
         ("type","LIvestockType","RUMINANT or POULTRY"),
         ("age","int","Age in years"),
         ("weight","double","Current weight in kg"),
         ("healthStatus","AnimalHealthStatus","Healthy / Sick / Quarantined"),
         ("hasGPSCollar","boolean","True when a GPS collar sensor is attached"),
         ("gpsCollarSensor","GPSCollarSensor","The attached GPS sensor, or null"),
         ("zone","ZONE","The livestock zone this animal belongs to"),
         ("healthHistory","List<HealthEvent>","All recorded health events"),
         ("bioSensors","List<BioSensor>","Attached bio-metric sensors"),
         ("milkYieldLiters","double","Cumulative milk yield since last reset"),
         ("eggCount","int","Cumulative egg count since last reset"),
         ("weightHistory","List<WeightRecord>","All recorded weight measurements"),
         ("milkHistory","List<MilkRecord>","All recorded milk yield measurements"),
         ("eggHistory","List<EggRecord>","All recorded egg count measurements")],
        [4*cm, 4.5*cm, 8*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("attachGPSCollar(GPSCollarSensor)","void","Sets gpsCollarSensor and hasGPSCollar=true."),
         ("attachBioSensor(BioSensor)","void","Appends sensor to bioSensors list."),
         ("removeGPSCollar()","void","Nullifies gpsCollarSensor, sets hasGPSCollar=false."),
         ("updateWeight(double)","void","Updates weight field and appends a WeightRecord with current time."),
         ("updateWeight(double,LocalDateTime)","void","Same but uses supplied timestamp for history record."),
         ("addHealthEvent(HealthEvent)","void","Appends to healthHistory and updates healthStatus to event's type."),
         ("resolveLastHealthEvent(status)","void","Scans history in reverse for the first unresolved event, marks it resolved, updates healthStatus."),
         ("getUnresolvedEvents()","List<HealthEvent>","Returns events where isResolved() is false."),
         ("recordMilkYield(double)","void","Accumulates to milkYieldLiters, adds MilkRecord at current time."),
         ("recordMilkYield(double,LocalDateTime)","void","Same with explicit timestamp."),
         ("recordEgg(int)","void","Accumulates to eggCount, adds EggRecord at current time."),
         ("recordEgg(int,LocalDateTime)","void","Same with explicit timestamp."),
         ("resetProductionStats()","void","Zeros milkYieldLiters and eggCount; clears milkHistory and eggHistory."),
         ("isSick()","boolean","Returns healthStatus == Sick."),
         ("isQuarantined()","boolean","Returns healthStatus == Quarantined.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("AnimalHealthStatus", "Enum", "Animals")
    e.append(Paragraph("Values: <b>Healthy</b>, <b>Sick</b>, <b>Quarantined</b>. "
        "Represents the current overall health condition of an animal. "
        "An animal begins Healthy; health events drive transitions.", ST["body"]))
    e.append(HR())

    e += class_header("FeedingProgram", "Concrete Class", "Animals")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Encapsulates a zone-level feeding schedule: the food type, daily quantity, "
        "a list of feeding times (HH:mm strings), and wake/sleep time bounds that prevent "
        "feeding outside active hours. Computes whether a feeding is currently due or overdue.",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("foodType","String","E.g. 'Hay + Grain'"),
         ("quantity","double","Daily quantity in kg per feeding"),
         ("schedule","List<String>","Ordered list of HH:mm feeding times"),
         ("lastFedTime","LocalDateTime","Timestamp of the most recent recordFeeding() call"),
         ("nextFeedingTime","LocalDateTime","Next computed feeding slot"),
         ("wakeUpTime","LocalTime","Earliest time feeding is allowed"),
         ("sleepTime","LocalTime","Latest time feeding is allowed")],
        [4*cm, 4*cm, 8.5*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("recordFeeding()","void","Sets lastFedTime to now; recomputes nextFeedingTime."),
         ("isDue()","boolean","True if now is past nextFeedingTime and within wake/sleep hours."),
         ("isOverdue()","boolean","True if isDue() and more than 30 minutes have passed since nextFeedingTime."),
         ("hoursUntilNextFeeding()","long","Duration in hours until nextFeedingTime; 0 if due."),
         ("minutesUntilNextFeeding()","long","Same in minutes."),
         ("getTimesPerDay()","int","Derived: returns schedule.size().")],
        [5*cm, 2.5*cm, 9*cm]))
    e.append(HR())

    e += class_header("HealthEvent", "Concrete Class", "Animals")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Records a single health status change for an animal, including what the status was before "
        "and after, a description, and whether it has been resolved. "
        "A HealthEvent is considered resolved when statusAfter is non-null at construction or "
        "after markResolved() is called.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("animal","Animal","The animal this event belongs to"),
         ("eventType","AnimalHealthStatus","The new health state (Sick, Quarantined, or Healthy for recovery)"),
         ("statusBefore","AnimalHealthStatus","Health state immediately before this event"),
         ("statusAfter","AnimalHealthStatus","Health state after resolution (null if unresolved)"),
         ("date","LocalDateTime","When the event was recorded"),
         ("description","String","Free-text note about the event"),
         ("resolved","boolean","True if statusAfter != null at construction or markResolved() called")],
        [4*cm, 4.5*cm, 8*cm]))
    e.append(make_table(["Method","Return","Logic"],
        [("markResolved(status)","void","Sets resolved=true and statusAfter to the supplied status.")],
        [5*cm, 2.5*cm, 9*cm]))
    return e

def sec_entities():
    e = pkg_intro("6", "Entities",
        "The Entities package contains the non-animal farm entities: Crop (a cultivated field), "
        "AquacultureSpecies (a fish/seafood population group), and supporting enums. "
        "Each entity tracks its own production history through inner record classes.")

    e += class_header("AquacultureSpecies", "Concrete Class", "Entities")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Models a single population group of aquatic species within an AquacultureZONE. "
        "Tracks population count, harvest records, and mortality. Supports restock cycles: "
        "each restock resets the cycleBaseline so that within-cycle survival rates are computed "
        "independently of the total operational history.",
        ST["body"]))
    e.append(subsection("INNER CLASS — HarvestRecord"))
    e.append(make_table(["Field","Type","Description"],
        [("weightKg","double","Harvest weight in kilograms"),
         ("countHarvested","int","Number of individuals harvested"),
         ("countBefore","int","Population before this harvest"),
         ("date","LocalDateTime","Timestamp of the harvest")],
        [4*cm, 3*cm, 9.5*cm]))
    e.append(Paragraph(
        "HarvestRecord.getCycleSurvivalRatePercent() computes the percentage of the pre-harvest "
        "population that was NOT harvested (i.e. remained alive).", ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("id","String","UUID"),
         ("name","String","Species name (e.g. 'Tilapia')"),
         ("initialTotalIndividuals","int","Original stock count — never changes"),
         ("cycleBaseline","int","Population at the start of the current restock cycle"),
         ("numSpecies","int","Current live population count"),
         ("zone","ZONE","Parent aquaculture zone"),
         ("harvestHistory","List<HarvestRecord>","All harvests ever recorded"),
         ("cycleStartHarvestIndex","int","Index into harvestHistory marking the start of current cycle")],
        [4*cm, 3.5*cm, 9*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("harvest(kg,count)","void","Validates count <= numSpecies, records HarvestRecord, decrements numSpecies."),
         ("harvest(kg,count,date)","void","Same with explicit timestamp."),
         ("recordMortality(count)","void","Decrements numSpecies; does not add a harvest record."),
         ("restock(count)","void","Adds count to numSpecies, resets cycleBaseline, advances cycleStartHarvestIndex."),
         ("getCycleMortality()","int","cycleBaseline - numSpecies - harvestedInCycle; never negative."),
         ("getTotalMortality()","int","initialTotal - numSpecies - totalHarvested; never negative."),
         ("getCycleSurvivalRatePercent()","double","numSpecies / cycleBaseline * 100; capped at 100."),
         ("getOverallSurvivalRatePercent()","double","numSpecies / initialTotal * 100; capped at 100."),
         ("getTotalHarvestWeightKg()","double","Sum of all HarvestRecord.weightKg values."),
         ("getStatusReport()","String","Formatted multi-field summary string.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("Crop", "Concrete Class", "Entities")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Represents a single cultivated field (crop instance) within a CropZONE. "
        "Tracks crop type, variety, planting/expected harvest dates, current growth stage, "
        "pH and moisture requirements, and cumulative yield. "
        "A YieldRecord inner class stores individual harvest events.",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("id","String","UUID"),
         ("cropType","CropType","cereals / vegetables / fruits"),
         ("variety","String","Cultivar name (e.g. 'Anza')"),
         ("plantingDate","Date","When seeds were planted"),
         ("expectedHarvestDate","Date","Target harvest date"),
         ("growthStage","GrowthStage","Current stage: sowing → germination → growth → maturity → harvest"),
         ("zone","ZONE","Parent crop zone"),
         ("optimalPHRange","Range","Acceptable soil pH range"),
         ("optimalMoistureRange","Range","Acceptable soil moisture % range"),
         ("yieldKg","double","Cumulative harvested weight"),
         ("harvestDate","Date","Date of most recent harvest call"),
         ("boundary","GoegraphicBoundries","Optional polygon boundary for this field"),
         ("yieldHistory","List<YieldRecord>","All individual harvest records")],
        [4.5*cm, 4*cm, 8*cm]))
    e.append(subsection("METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("updateGrowthStage(GrowthStage)","void","Validates non-null, sets growthStage."),
         ("isReadyForHarvest()","boolean","True if growthStage == harvest."),
         ("recordHarvest(double)","void","Accumulates yieldKg, sets harvestDate to now, adds YieldRecord."),
         ("recordHarvest(double,LocalDateTime)","void","Same with explicit timestamp."),
         ("wasHarvested()","boolean","True if yieldKg > 0."),
         ("getStatusReport()","String","'Crop: variety | Stage: stage | Type: type'.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    for enum_name, values, desc in [
        ("CropType",       "cereals, vegetables, fruits",
         "Classifies the crop category for production reporting and yield-by-type aggregation."),
        ("GrowthStage",    "sowing, germination, growth, maturity, harvest",
         "Represents the lifecycle stage of a Crop field. Used by simulation and reports."),
        ("LIvestockType",  "RUMINANT, POULTRY",
         "Differentiates the type of livestock zone and animal. RUMINANT animals can produce milk; POULTRY produce eggs."),
        ("SoilRequirements","Optimal_PH, Optimal_moisture, Both",
         "Describes which soil conditions are critical for a crop. Used in reporting and sensor alert classification."),
    ]:
        e += class_header(enum_name, "Enum", "Entities")
        e.append(Paragraph(f"Values: <b>{esc(values)}</b>. {esc(desc)}", ST["body"]))
        e.append(HR())
    return e

def sec_farm():
    e = pkg_intro("7", "Farm",
        "Contains a single class, Farm, which is the aggregate root of the entire domain model. "
        "All zones, alerts, and report history are owned by and accessed through the Farm instance. "
        "It also exposes factory methods for generating all types of zone and farm-level reports.")

    e += class_header("Farm", "Concrete Class", "Farm")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "The Farm is the central aggregate root. It holds three typed zone lists "
        "(LivestockZONE, CropZONE, AquacultureZONE), an alert list, and four report history lists. "
        "Zone-type dispatch uses Java pattern-matching instanceof. "
        "The inner FarmStats class is a data-transfer object returned by getStats().",
        ST["body"]))
    e.append(subsection("FIELDS"))
    e.append(make_table(["Name","Type","Description"],
        [("id","String","UUID generated at construction"),
         ("name","String","Farm display name"),
         ("location","String","Geographic location string"),
         ("ownerName","String","Owner/manager name"),
         ("createdAt","LocalDateTime","Timestamp of farm creation"),
         ("livestockZones","List<LivestockZONE>","All livestock zones"),
         ("cropZones","List<CropZONE>","All crop zones"),
         ("aquacultureZones","List<AquacultureZONE>","All aquaculture zones"),
         ("alerts","List<Alert>","Complete alert registry for this farm"),
         ("zoneReports","List<Report>","History of generated zone-level reports"),
         ("zoneProductionReports","List<ProductionReport>","History of zone production reports"),
         ("farmReports","List<FarmReport>","History of farm-level summary reports"),
         ("farmProductionReports","List<FarmProductionReport>","History of farm production reports")],
        [4.5*cm, 4.5*cm, 7.5*cm]))
    e.append(subsection("KEY METHODS"))
    e.append(make_table(["Method","Return","Logic"],
        [("addZone(ZONE)","void","Dispatches to the correct typed list using instanceof pattern matching."),
         ("removeZone(ZONE)","void","Removes from the matching typed list."),
         ("getZoneByCode(code)","ZONE","Streams getAllZones(), finds first matching code."),
         ("getAllZones()","List<ZONE>","Concatenates all three zone lists into an unmodifiable list."),
         ("registerAlert(Alert)","void","Adds alert to the alerts list."),
         ("getActiveAlerts()","List<Alert>","Returns alerts in ACTIVE or ACKNOWLEDGED state."),
         ("getAlertsByZonePeriod(start,end)","List<Alert>","Filters alerts whose timestamp falls in [start,end]."),
         ("generateReportForZone(zone,type,start,end)","Report","Creates a typed Report subclass based on zone type; stores in history."),
         ("generateFarmReport(type,start,end)","FarmReport","Generates reports for all zones, aggregates into FarmReport."),
         ("generateFarmProductionReport(type,start,end)","FarmProductionReport","Generates production reports for all zones, aggregates."),
         ("getStats()","FarmStats","Computes live counts: zones, animals, sick animals, fields, species, alerts.")],
        [6*cm, 3.5*cm, 7*cm]))
    e.append(subsection("INNER CLASS — FarmStats"))
    e.append(Paragraph(
        "Public final fields: totalZones, totalAnimals, sickAnimals, totalFields, totalSpecies, "
        "totalAlerts, activeAlerts, criticalAlerts. A plain data record with no behaviour.", ST["body"]))
    return e

def sec_reports():
    e = pkg_intro("8", "Reports",
        "The Reports package provides a three-tier report hierarchy: Report (abstract zone-level "
        "base), FarmReport (aggregated across all zones), ProductionReport (production-specific "
        "zone report), and FarmProductionReport (aggregated production across the farm). "
        "Three concrete zone report subclasses exist: ReportLiveStockZone, ReportCropZone, "
        "ReportAquacultureZone. Three concrete production report subclasses exist: "
        "LivestockProductionReport, CropProductionReport, AquacultureProductionReport. "
        "ReportType is a supporting enum.")

    e += class_header("ReportType", "Enum", "Reports")
    e.append(Paragraph("Values: <b>Daily, Weekly, Monthly, Quarterly, Yearly</b>. "
        "Used by ReportService to compute the period start date via LocalDateTime arithmetic.", ST["body"]))
    e.append(HR())

    e += class_header("Report", "Abstract Class", "Reports")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Base class for all zone-level reports. The constructor computes alert statistics "
        "(total, critical, warning, resolved, unresolved) from the passed list, then delegates "
        "to two abstract methods: computeSpecificStats() for domain-specific counts, "
        "and computeSensorSummaries() for sensor-threshold summaries.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("id","String","UUID"),
         ("zone","ZONE","The zone this report covers"),
         ("generatedAt / periodStart / periodEnd","LocalDateTime","Report temporal metadata"),
         ("reportType","ReportType","Daily / Weekly / ..."),
         ("totalAlerts / criticalAlerts / warningAlerts","int","Alert counts for the period"),
         ("resolvedAlerts / unresolvedAlerts","int","Resolution status counts"),
         ("sensorSummaries","List<String>","Human-readable sensor anomaly descriptions"),
         ("notes","List<String>","Auto-generated advisory notes")],
        [5*cm, 3*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("ReportLiveStockZone", "Concrete Class", "Reports", "extends Report")
    e.append(Paragraph(
        "Computes: total, healthy, sick, quarantined animal counts; GPS escape count from alert list; "
        "feeding on-time flag from FeedingProgram.isOverdue(). "
        "computeSensorSummaries() scans each animal's BioSensors and GPS collar for out-of-threshold readings.",
        ST["body"]))
    e.append(HR())

    e += class_header("ReportCropZone", "Concrete Class", "Reports", "extends Report")
    e.append(Paragraph(
        "Computes: total fields, ready-for-harvest count, growing count, dominant growth stage "
        "(most frequent GrowthStage across all fields). Sensor summaries come from EnvSensor and SoilSensor history.",
        ST["body"]))
    e.append(HR())

    e += class_header("ReportAquacultureZone", "Concrete Class", "Reports", "extends Report")
    e.append(Paragraph(
        "Computes: species group count, total current individuals, total initial individuals. "
        "Sensor summaries from WaterSensor history.",
        ST["body"]))
    e.append(HR())

    e += class_header("FarmReport", "Concrete Class", "Reports")
    e.append(Paragraph(
        "Farm-level aggregated zone report. Iterates over zoneReports, casting each to its concrete "
        "type to extract typed stats. Computes GPS escapes from alert types. "
        "Generates advisory notes for sick animals, overdue feeding, critical alerts, GPS escapes. "
        "Provides a detailed toString() grouped by zone type.",
        ST["body"]))
    e.append(HR())

    e += class_header("LivestockProductionReport", "Concrete Class", "Reports",
                      "extends ProductionReport")
    e.append(Paragraph(
        "Iterates zone animals, accumulates total milk yield, egg count, and counts producing animals. "
        "Computes average milk per producing animal and average eggs per laying animal. "
        "Adds a sensor summary entry per producing animal.",
        ST["body"]))
    e.append(HR())

    e += class_header("CropProductionReport", "Concrete Class", "Reports",
                      "extends ProductionReport")
    e.append(Paragraph(
        "Iterates zone fields, filters those whose harvestDate falls within the report period, "
        "accumulates total yield and maps it by CropType. Computes yield per hectare using zone surface. "
        "Counts pending (not yet harvested) fields.",
        ST["body"]))
    e.append(HR())

    e += class_header("AquacultureProductionReport", "Concrete Class", "Reports",
                      "extends ProductionReport")
    e.append(Paragraph(
        "Aggregates harvest weight, current/initial populations, cycle/overall mortality, and "
        "average survival rates across all species in the zone. "
        "Computes waterQualityScore as (in-range readings / total readings) * 100 "
        "from all WaterSensor reading histories.",
        ST["body"]))
    e.append(HR())

    e += class_header("FarmProductionReport", "Concrete Class", "Reports")
    e.append(Paragraph(
        "Top-level production aggregation. Iterates zone production reports (cast by type), "
        "sums all livestock, crop, and aquaculture KPIs, computes overall yield/ha and "
        "average survival/water quality. Generates farm-wide advisory notes.",
        ST["body"]))
    return e

def sec_sensors():
    e = pkg_intro("9", "Sensors",
        "The Sensors package defines the complete sensor hierarchy. "
        "Sensor is the abstract base with an auto-generated code, zone reference, status, and reading history. "
        "NumericSensor extends it with min/max thresholds and a unit. "
        "Four concrete numeric sensor types (BioSensor, EnvSensor, SoilSensor, WaterSensor) and "
        "one position sensor (GPSCollarSensor) extend the hierarchy. "
        "Two reading classes (NumericSensorReading, GPSSensorReading) extend the abstract SensorReading. "
        "Supporting enums encode measure types, status, and severity levels.")

    e += class_header("Sensor", "Abstract Class", "Sensors")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Base for all sensors. Maintains a static integer counter to generate zero-padded 4-digit "
        "codes (0001, 0002, ...). Provides reading history management and static utility methods "
        "for filtering and sorting reading lists.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("code","String","Zero-padded 4-digit auto-generated code"),
         ("zone","ZONE","The zone this sensor monitors"),
         ("status","SensorStatus","Active / Faulty / Suspended"),
         ("readingHistory","List<SensorReading>","All readings recorded on this sensor")],
        [3.5*cm, 3*cm, 10*cm]))
    e.append(make_table(["Method","Return","Logic"],
        [("sendReading()","SensorReading","Abstract — subclass creates and stores a new reading."),
         ("suspend() / reactivate() / markAsFaulty()","void","Set status field."),
         ("addReading(SensorReading)","void","Appends to readingHistory."),
         ("filterByPeriod(list,from,to)","List","Static. Returns readings with timestamp in [from,to]."),
         ("sortByTimestamp(list)","List","Static. Returns sorted copy ascending by timestamp.")],
        [5*cm, 2.5*cm, 9*cm]))
    e.append(HR())

    e += class_header("NumericSensor", "Abstract Class", "Sensors", "extends Sensor")
    e.append(Paragraph(
        "Adds threshold bounds (minThreshold, maxThreshold) and a measurement unit. "
        "isOutOfRange(value) checks whether a value falls outside the interval. "
        "lastValue caches the most recent measurement for quick display without scanning history. "
        "filterByLevel() is a static utility to filter NumericSensorReading lists by ReadingLevel.",
        ST["body"]))
    e.append(HR())

    e += class_header("BioSensor", "Concrete Class", "Sensors", "extends NumericSensor")
    e.append(Paragraph(
        "Monitors a biological metric (Temperature or ActivityLevel) for a specific animal. "
        "The unit is derived automatically from the BioMeasureType (degrees C or steps/min). "
        "isAnimalInDistress() delegates to isOutOfRange(lastValue) for quick health checks.",
        ST["body"]))
    e.append(HR())

    e += class_header("EnvSensor", "Concrete Class", "Sensors", "extends NumericSensor")
    e.append(Paragraph(
        "Monitors environmental conditions (Temperature, Humidity, or Rainfall) in a CropZONE. "
        "sendReading() creates a NumericSensorReading from lastValue and the configured unit. "
        "measure() is a convenience method that returns lastValue.",
        ST["body"]))
    e.append(HR())

    e += class_header("SoilSensor", "Concrete Class", "Sensors", "extends NumericSensor")
    e.append(Paragraph(
        "Monitors soil conditions (PH, Moisture, or Nitrogen) in a CropZONE. "
        "Identical structure to EnvSensor, differing only in measure type enum.",
        ST["body"]))
    e.append(HR())

    e += class_header("WaterSensor", "Concrete Class", "Sensors", "extends NumericSensor")
    e.append(Paragraph(
        "Monitors water quality (Temperature or DissolvedOxygen) in an AquacultureZONE. "
        "DissolvedOxygen readings below threshold trigger WaterSensorAlert with Critical severity.",
        ST["body"]))
    e.append(HR())

    e += class_header("GPSCollarSensor", "Concrete Class", "Sensors", "extends Sensor")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Tracks the geographic position of a specific animal. Maintains current latitude and longitude. "
        "updateLocation() recomputes isInsideZone by calling the zone's contains() method (ray-casting "
        "algorithm via GoegraphicBoundries). sendReading() creates a GPSSensorReading snapshot.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("updateLocation(lat,lon)","void","Updates coordinates, calls checkIfInsideZone() to recompute isInsideZone."),
         ("sendReading()","GPSSensorReading","Creates GPSSensorReading with current position and zone-status."),
         ("hasEscaped()","boolean","Returns !isInsideZone."),
         ("getLastReading()","GPSSensorReading","Returns the most recent GPSSensorReading from history, or null.")],
        [5*cm, 3.5*cm, 8*cm]))
    e.append(HR())

    e += class_header("SensorReading", "Abstract Class", "Sensors")
    e.append(Paragraph(
        "Base reading class. Stores the sensor reference and an auto-set timestamp. "
        "A second constructor accepts an explicit timestamp for historical injection.", ST["body"]))
    e.append(HR())

    e += class_header("NumericSensorReading", "Concrete Class", "Sensors", "extends SensorReading")
    e.append(Paragraph(
        "Stores a numeric value, its unit, and a computed ReadingLevel severity. "
        "determineSeverity() compares value to sensor thresholds: if out of range and excess > 20% of range, "
        "severity is CRITICAL; otherwise WARNING; otherwise NORMAL.",
        ST["body"]))
    e.append(HR())

    e += class_header("GPSSensorReading", "Concrete Class", "Sensors", "extends SensorReading")
    e.append(Paragraph(
        "Stores latitude, longitude, and a boolean isInsideZone. "
        "isOutsideZone() returns !isInsideZone for convenience.", ST["body"]))
    e.append(HR())

    for enum_n, vals, desc in [
        ("SensorStatus",      "Active, Faulty, Suspended",
         "Lifecycle state of a sensor. Faulty sensors are flagged but remain in lists."),
        ("ReadingLevel",      "NORMAL, WARNING, CRITICAL",
         "Severity of a NumericSensorReading determined by how far the value exceeds thresholds."),
        ("BioMeasureType",    "Temperature, ActivityLevel",
         "What a BioSensor measures. Determines unit label automatically."),
        ("EnvMeasureType",    "Temperature, Humidity, Rainfall",
         "What an EnvSensor measures in a crop zone."),
        ("SoilMeasureType",   "PH, Moisture, Nitrogen",
         "What a SoilSensor measures in a crop zone."),
        ("WaterMeasureType",  "Temperature, DissolvedOxygen",
         "What a WaterSensor measures in an aquaculture zone."),
        ("AlertSeverity (Sensors pkg)","Warning, Critical",
         "Duplicate of Alerts.AlertSeverity — kept in Sensors package for local use."),
    ]:
        e += class_header(enum_n, "Enum", "Sensors")
        e.append(Paragraph(f"Values: <b>{esc(vals)}</b>. {esc(desc)}", ST["body"]))
        e.append(HR())
    return e

def sec_zones():
    e = pkg_intro("10", "ZONES",
        "The ZONES package defines the geographic and operational farm zones. "
        "ZONE is an abstract base with auto-generated code, name, status, and optional boundaries. "
        "Three concrete subclasses specialise it: LivestockZONE (animals, bio sensors, GPS sensors), "
        "CropZONE (crop fields, env/soil sensors), and AquacultureZONE (species groups, water sensors). "
        "GoegraphicBoundries implements polygon point-in-polygon (ray-casting) logic. "
        "ZoneStatus is a two-value enum.")

    e += class_header("ZONE", "Abstract Class", "ZONES")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "The common base for all farm zones. Uses a static counter to assign sequential 4-digit "
        "codes (0001, 0002, ...). Starts in ACTIVE status. "
        "Optional GoegraphicBoundries can be set post-construction for point-in-polygon testing.",
        ST["body"]))
    e.append(make_table(["Field","Type","Description"],
        [("idCounter","int (static)","Shared counter across all ZONE subclasses for unique codes"),
         ("code","String","Zero-padded 4-digit sequential code"),
         ("name","String","Zone display name"),
         ("status","ZoneStatus","ACTIVE or SUSPENDED"),
         ("boundaries","GoegraphicBoundries","Optional polygon boundary; null if not set")],
        [3.5*cm, 3.5*cm, 9.5*cm]))
    e.append(make_table(["Method","Return","Logic"],
        [("activate() / suspend()","void","Set status to ACTIVE / SUSPENDED."),
         ("contains(lat,lon)","boolean","Delegates to boundaries.contains(); false if no boundaries."),
         ("hasBoundaries()","boolean","True if boundaries != null."),
         ("setBoundaries(GoegraphicBoundries)","void","Assigns the polygon boundary.")],
        [5*cm, 2.5*cm, 9*cm]))
    e.append(HR())

    e += class_header("LivestockZONE", "Concrete Class", "ZONES", "extends ZONE")
    e.append(Paragraph(
        "Holds a list of Animal objects, BioSensor list, GPSCollarSensor list, "
        "an optional FeedingProgram, and a LIvestockType (RUMINANT or POULTRY). "
        "Provides aggregate reading queries (getAllBioReadings, getAllGPSReadings) "
        "and production aggregates (getTotalMilkYield, getTotalEggCount). "
        "Two constructors: one requires GoegraphicBoundries (throws if null), "
        "one accepts no boundaries for UI-created zones.",
        ST["body"]))
    e.append(HR())

    e += class_header("CropZONE", "Concrete Class", "ZONES", "extends ZONE")
    e.append(Paragraph(
        "Holds a list of Crop (fields), EnvSensor list, SoilSensor list, "
        "and a surfacePlanted area (hectares). "
        "Aggregate reading queries: getAllEnvReadings(), getAllSoilReadings(), "
        "plus level-filtered versions. getTotalCropYield() sums all field yields.",
        ST["body"]))
    e.append(HR())

    e += class_header("AquacultureZONE", "Concrete Class", "ZONES", "extends ZONE")
    e.append(Paragraph(
        "Holds a list of AquacultureSpecies and WaterSensor list. "
        "getTotalHarvestWeight() and getTotalSpeciesCount() aggregate across species. "
        "getAllWaterReadings() collects all water sensor histories.",
        ST["body"]))
    e.append(HR())

    e += class_header("GoegraphicBoundries", "Concrete Class", "ZONES")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Represents a geographic polygon as an ordered list of [latitude, longitude] pairs. "
        "Implements the ray-casting algorithm in contains(lat,lon): a point is inside if a "
        "horizontal ray from the point crosses an odd number of polygon edges. "
        "Three factory methods create standard shapes: rectangle (4 points), triangle (3 points), "
        "and circle (approximated by N points using trigonometry with latitude correction).",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("addPoint(lat,lon)","void","Appends [lat,lon] array to the points list."),
         ("createRectangle(lat1,lon1,lat2,lon2)","GoegraphicBoundries","Static. Creates a 4-point polygon from two corners."),
         ("createTriangle(...)","GoegraphicBoundries","Static. Creates a 3-point polygon."),
         ("createCircle(cLat,cLon,radiusM,n)","GoegraphicBoundries","Static. Approximates a circle with n points, correcting longitude radius for latitude."),
         ("contains(lat,lon)","boolean","Ray-casting point-in-polygon algorithm."),
         ("size()","int","Returns points.size().")],
        [5.5*cm, 3.5*cm, 7.5*cm]))
    e.append(HR())

    e += class_header("ZoneStatus", "Enum", "ZONES")
    e.append(Paragraph("Values: <b>ACTIVE</b>, <b>SUSPENDED</b>. "
        "An ACTIVE zone is operational; a SUSPENDED zone is temporarily taken offline.", ST["body"]))
    return e

def sec_services():
    e = [PageBreak(), Paragraph("11.  Layer: Services", ST["section"]),
         Paragraph(
        "All services follow the singleton pattern (private constructor + static getInstance()). "
        "Each service delegates to FarmService.getInstance().getFarm() for all domain data access, "
        "ensuring that a farm switch (selecting a different farm from the startup screen) "
        "automatically propagates to all services without re-initialization.",
        ST["body"]), Spacer(1, 0.2*cm)]

    e += class_header("FarmService", "Singleton Service", "com.example.services")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "The primary singleton that holds the active Farm instance. "
        "Three factory methods initialise it: initWithDemo() loads the built-in DemoData farm, "
        "initWithNewFarm() creates a blank farm, and initFromSaved() reconstructs a farm from a "
        "FarmRepository snapshot. autoSave() serialises the current farm state to disk via "
        "FarmRepository (no-op for the demo farm).",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("initWithDemo()","void","Static. Creates DemoData.create() farm, saves a demo entry, sets instance."),
         ("initWithNewFarm(name,loc,owner)","void","Static. Creates empty Farm, saves to repository, sets instance."),
         ("initFromSaved(SavedFarm)","void","Static. Reconstructs farm from snapshot: zones + animals + optional DataRandomizerService."),
         ("getInstance()","FarmService","Throws if not initialised."),
         ("autoSave()","void","Serialises zones and animals to FarmRepository (skipped for demo farm)."),
         ("getTotalSensorCount()","int","Counts all sensors across all zone types."),
         ("getStats()","Farm.FarmStats","Delegates to farm.getStats().")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("AnimalService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "Provides CRUD operations over animals across all livestock zones. "
        "All mutating methods call fs().autoSave() after the domain mutation. "
        "resolveHealthEvent() both resolves the last unresolved event AND adds a new recovery HealthEvent "
        "to the history (so that the recovery appears as a discrete timeline entry).",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("getAllAnimals()","List<Animal>","Collects all animals from all livestock zones."),
         ("getAnimalsByZone(zone)","List<Animal>","Returns animals in the specified zone."),
         ("getAnimalsByHealthStatus(status)","List<Animal>","Filters all animals by health status."),
         ("getAnimalsByType(type)","List<Animal>","Filters all animals by LIvestockType."),
         ("addAnimal(name,species,type,age,wt,zone)","Animal","Creates Animal, adds to zone, auto-saves."),
         ("removeAnimal(Animal)","void","Finds and removes from its zone, auto-saves."),
         ("recordWeight/MilkYield/Egg(...)","void","Delegates to animal, auto-saves."),
         ("resolveHealthEvent(animal,status)","void","Resolves last event + adds recovery HealthEvent.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("ZoneService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "CRUD and status management for zones. All zone-list queries delegate to Farm. "
        "addZone() calls farm.addZone() then autoSave(). "
        "setFeedingProgram() and recordFeeding() update livestock zone feeding data.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("getLivestockZones()","List<LivestockZONE>","Direct delegation to farm."),
         ("getActiveZoneCount()","int","Counts zones with ZoneStatus.ACTIVE."),
         ("addZone(ZONE) / removeZone(ZONE)","void","Delegates to farm, then auto-saves."),
         ("renameZone(zone,name)","void","Calls zone.setName(), auto-saves."),
         ("setFeedingProgram(zone,fp)","void","Assigns FeedingProgram to zone, auto-saves."),
         ("recordFeeding(zone)","void","Calls fp.recordFeeding() if program exists, auto-saves.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(HR())

    e += class_header("SensorService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "Aggregates and classifies all sensors across the farm. "
        "getLastReadingLevel() returns ReadingLevel for any sensor type (GPS escape = CRITICAL). "
        "getSensorTypeLabel() produces a human-readable string (e.g. 'Bio · Temperature'). "
        "getLastReadingDisplay() formats the last reading value for UI cards.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("getAllSensors()","List<Sensor>","Collects all sensor types from all zones."),
         ("getAllBioSensors() / getAllGPS... / getAllEnv... / ...","typed lists","Collect specific sensor types only."),
         ("getActiveSensorCount()","int","Counts sensors with SensorStatus.Active."),
         ("getLastReadingLevel(sensor)","ReadingLevel","GPS: checks last reading isInsideZone; Numeric: returns reading severity."),
         ("getSensorTypeLabel(sensor)","String","Pattern-matches sensor type, returns 'Type · detail'."),
         ("getLastReadingDisplay(sensor)","String","GPS: formatted coordinates; Numeric: value + unit; else 'No data'.")],
        [6.5*cm, 2.5*cm, 7.5*cm]))
    e.append(HR())

    e += class_header("AlertService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "Thin facade over Farm's alert list. All queries delegate to AlertManager static filters. "
        "Provides active alert count for the top-bar badge, and resolution methods for the UI.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("getAllAlerts()","List<Alert>","Returns all alerts sorted by severity (Critical first)."),
         ("getActiveAlerts()","List<Alert>","Only ACTIVE or ACKNOWLEDGED alerts."),
         ("getAlertsByType/Severity/Zone/Period/Resolution(...)","List<Alert>","Various filter delegates."),
         ("getActiveAlertCount() / getCriticalAlertCount()","int","Size of filtered lists."),
         ("acknowledge/resolve/dismiss(Alert)","void","Direct delegation to alert object.")],
        [6*cm, 2.5*cm, 8*cm]))
    e.append(HR())

    e += class_header("ReportService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "Generates reports by computing the period start from the ReportType enum (Daily=-1 day, "
        "Weekly=-1 week, Monthly=-1 month, Quarterly=-3 months, Yearly=-1 year) and delegating "
        "to Farm's report factory methods.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("generateFarmReport(type)","FarmReport","Computes period, calls farm.generateFarmReport()."),
         ("generateFarmProductionReport(type)","FarmProductionReport","Same for production."),
         ("generateZoneReport(zone,type)","Report","Zone-level report for one zone."),
         ("generateZoneProductionReport(zone,type)","ProductionReport","Zone production report.")],
        [6*cm, 3.5*cm, 7*cm]))
    e.append(HR())

    e += class_header("DataRandomizerService", "Singleton Service", "com.example.services")
    e.append(Paragraph(
        "Populates a newly created empty farm with realistic randomised data: "
        "one LivestockZONE with RUMINANT animals (bio sensors, GPS collars, milk records), "
        "one CropZONE with cereal and vegetable crops (env and soil sensors), "
        "and one AquacultureZONE with two species (water sensors). "
        "Used when the user clicks 'Randomize Data' in the startup screen for a new farm.",
        ST["body"]))
    e.append(HR())

    e += class_header("SimulationService", "Singleton Service", "com.example.services")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Implements a discrete-time simulation that advances farm state by one or more days. "
        "Each call to simulateDays(n) iterates n days; within each day it simulates every animal, "
        "crop, aquaculture species, numeric sensor, and GPS sensor. "
        "An inner class SimulationResult accumulates KPIs for the step.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("simulateDays(int days)","SimulationResult","Main entry: loops days, calls all simulateX() helpers, auto-saves."),
         ("simulateAnimal(animal,day,result)","void","Weight drift (healthy:+0.2%, sick:-0.3%), milk/egg if applicable, 1% illness/10% recovery chance."),
         ("simulateCrop(crop,day,result)","void","Maps elapsed/total days to GrowthStage; harvests on Wednesdays at harvest stage."),
         ("simulateAquaculture(sp,day,result)","void","0.05-0.15% daily mortality; harvest every 6 sim-days."),
         ("simulateNumericSensor(sensor,day,result)","void","Gaussian reading with 5% spike; creates NumericSensorReading; generates SensorAlert if out of range."),
         ("simulateGPS(gps,day,result)","void","Drifts lat/lon by +-0.0002 degrees; calls updateLocation(); generates GPS_ESCAPE_ALERT on inside-to-outside transition."),
         ("reset()","void","Resets simulationDate and totalDaysSimulated."),
         ("getSimulationDate()","LocalDate","Current simulated date."),
         ("getTotalDaysSimulated()","int","Cumulative days simulated since last reset.")],
        [6*cm, 3*cm, 7.5*cm]))
    e.append(Paragraph(
        "SimulationResult fields: List<String> log, int daysSimulated, int healthEvents, "
        "double milkLitersTotal, int eggsTotal, int harvestsRecorded, int alertsGenerated, int mortalityCount.",
        ST["body"]))
    return e

def sec_controllers():
    e = [PageBreak(), Paragraph("12.  Layer: Controllers (JavaFX)", ST["section"]),
         Paragraph(
        "Each controller is a JavaFX FXML controller: the @FXML fields are injected by FXMLLoader, "
        "and @FXML-annotated methods are bound to FXML onAction/onKeyReleased attributes. "
        "Controllers are stateless between navigation events — they read fresh data from services "
        "in initialize() every time the view is loaded.",
        ST["body"]), Spacer(1,0.2*cm)]

    e += class_header("MainController", "FXML Controller", "com.example.controllers")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Controls the persistent app shell (main.fxml): the collapsible sidebar with navigation "
        "buttons and the top-bar with farm name, live clock, and alert badge. "
        "The sidebar toggles between 64px (icon-only) and 240px (icon + label) using a Timeline "
        "animation on the VBox prefWidthProperty.",
        ST["body"]))
    e.append(make_table(["@FXML Field","Type","UI Role"],
        [("sidebar","VBox","The collapsible left sidebar container"),
         ("logoLabel","Label","'FarmManager' brand text (hidden when collapsed)"),
         ("farmNameLabel","Label","Top-bar farm name from FarmService.getFarmName()"),
         ("clockLabel","Label","Live clock updated every second"),
         ("alertBadge","Label","Red badge showing active alert count"),
         ("btnDashboard/Zones/Animals/...","Button","Navigation buttons, 8 total")],
        [4*cm, 3*cm, 9.5*cm]))
    e.append(make_table(["Method","Trigger","Logic"],
        [("initialize()","FXMLLoader","Sets farm name, starts clock, updates badge, marks Dashboard active."),
         ("startClock()","internal","Timeline fires every 1s, updates clockLabel with LocalDateTime.now()."),
         ("updateAlertBadge()","internal","Gets alert count from AlertService; shows/hides badge."),
         ("toggleSidebar()","☰ button","Animates sidebar width 240px↔64px, toggles logo visibility, updates button text."),
         ("setActive(Button)","internal","Removes 'nav-active' CSS class from previous button, adds to new."),
         ("navDashboard/Zones/Animals/...()","nav buttons","Calls setActive() then SceneManager.navigateTo(route).")],
        [4.5*cm, 3*cm, 9*cm]))
    e.append(HR())

    e += class_header("DashboardController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Displays the main dashboard: four KPI cards (total animals, active zones, active sensors, "
        "open alerts), a BarChart of production by zone type (milk L, egg count, harvest kg), "
        "a recent-alerts table (top 5), and quick-action buttons. "
        "All data is read from FarmService, ZoneService, SensorService, and AlertService in initialize().",
        ST["body"]))
    e.append(make_table(["Method","Trigger","Logic"],
        [("initialize()","FXMLLoader","Calls buildKpis(), buildChart(), buildAlertTable()."),
         ("buildKpis()","internal","Sets 4 KPI label texts from service counts."),
         ("buildChart()","internal","Creates BarChart series for each zone type using aggregated production data."),
         ("buildAlertTable()","internal","Populates TableView with top-5 alerts, colour-coded by severity."),
         ("navToAnimals/Zones/Reports()","quick-action buttons","Calls SceneManager.navigateTo(route).")],
        [4.5*cm, 3*cm, 9*cm]))
    e.append(HR())

    e += class_header("ZonesController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Manages the Zones screen with a TabPane (Livestock | Crop | Aquaculture tabs). "
        "Each tab contains a TableView backed by FilteredList. "
        "A search field filters by name. Add/Edit open dialogs. "
        "The detail panel shows zone boundaries, sensor counts, and zone-type-specific stats. "
        "ZoneMapDialog shows a canvas-drawn boundary polygon.",
        ST["body"]))
    e.append(make_table(["Method","Trigger","Logic"],
        [("initialize()","FXMLLoader","Builds all three TableViews, wires search filter, loads data."),
         ("handleAddZone()","Add button","Opens type-selection dialog, then zone creation dialog."),
         ("handleEditZone()","Edit button","Opens edit dialog pre-filled with selected zone data."),
         ("handleDeleteZone()","Delete button","Confirms, removes via ZoneService.removeZone(), refreshes list."),
         ("showZoneDetail(ZONE)","row selection","Populates detail panel with zone-specific information."),
         ("handleViewMap()","Map button","Opens ZoneMapDialog for the selected zone.")],
        [4.5*cm, 3*cm, 9*cm]))
    e.append(HR())

    e += class_header("AnimalsController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Displays all animals in a TableView with filter dropdowns (by zone, type, health status). "
        "The detail panel shows feeding program, health event timeline, and GPS coordinates if equipped. "
        "Dialog buttons open history dialogs for weight, milk, egg, and health event records. "
        "Milk/egg/weight history dialogs use ListView with Animal inner record toString() formatting.",
        ST["body"]))
    e.append(make_table(["Method","Trigger","Logic"],
        [("initialize()","FXMLLoader","Populates table, sets up filter comboboxes, wires selection listener."),
         ("showDetail(Animal)","row click","Fills detail pane: health badge, GPS info, feeding program, history buttons."),
         ("handleAddAnimal()","Add button","Opens animal creation dialog, adds via AnimalService."),
         ("handleMarkHealth()","Mark Health button","Dialog to log new HealthEvent for selected animal."),
         ("handleResolve()","Resolve button","Calls AnimalService.resolveHealthEvent(); refreshes detail."),
         ("showWeightHistory()","Weight History button","Dialog with ListView of WeightRecord strings."),
         ("showMilkHistory()","Milk History button","Dialog with ListView of MilkRecord strings."),
         ("showEggHistory()","Egg History button","Dialog with ListView of EggRecord strings."),
         ("showHealthHistory()","Health History button","Dialog with ListView of HealthEvent descriptions.")],
        [4.5*cm, 3*cm, 9*cm]))
    e.append(HR())

    e += class_header("SensorsController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Displays a scrollable FlowPane of sensor cards. Each card shows the sensor code, "
        "type label, last reading, and a colour-coded ReadingLevel badge (green/yellow/red). "
        "A filter ComboBox filters by sensor category. "
        "Clicking a card opens a SensorChartDialog with a line chart of reading history.",
        ST["body"]))
    e.append(HR())

    e += class_header("AlertsController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Shows all alerts in a TableView with filter dropdowns for type, severity, and resolution. "
        "Selecting a row populates the detail panel with full alert info and resolution status. "
        "The 'Mark as Resolved' button calls AlertService.resolve() and refreshes the table.",
        ST["body"]))
    e.append(HR())

    e += class_header("ReportsController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Left panel: ListView of report types. Right panel: dynamically built content. "
        "Supports Farm Report (summary cards + zone table), Farm Production Report "
        "(livestock/crop/aquaculture KPIs), and per-zone production reports with trend line charts "
        "(milk, eggs, weight, crop yield, aquaculture harvest, GPS inside/outside timeline). "
        "Export button writes the current report to a .txt file.",
        ST["body"]))
    e.append(HR())

    e += class_header("SettingsController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Allows editing farm name, location, and owner. Changes are committed via FarmService setters "
        "which auto-save. Provides a theme toggle (dark/light) via SceneManager.applyStylesheet(). "
        "Shows project version and school info in an About section.",
        ST["body"]))
    e.append(HR())

    e += class_header("SimulationController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Controls the Simulation screen. Three step buttons (+1 Day, +7 Days, +30 Days) call "
        "SimulationService.simulateDays(n), then update six KPI labels (milk, eggs, harvests, "
        "health events, alerts, mortality) and prepend new log entries to the ListView "
        "(newest first). Quick-navigation buttons (Reports, Sensors, Animals) jump to those screens.",
        ST["body"]))
    e.append(HR())

    e += class_header("StartupController", "FXML Controller", "com.example.controllers")
    e.append(Paragraph(
        "Startup/farm-selection screen. Displays a ListView of saved farms loaded from FarmRepository. "
        "Allows creating a new farm with two distinct initialisation modes, selecting and loading an "
        "existing farm, or deleting a saved farm. Calls the appropriate FarmService factory method then "
        "SceneManager.loadMainApp() to transition to the main application shell.",
        ST["body"]))
    e.append(subsection("FARM CREATION MODES"))
    e.append(make_table(["Button","Handler","Behaviour"],
        [("📋  Sample Data","createWithSampleData()","Creates the farm then calls DataRandomizerService.populateNewFarm() — "
          "produces zones, animals, sensors, and readings ready to explore."),
         ("⬜  Empty Farm","createEmptyFarm()","Creates the farm with no zones or animals — "
          "the user builds everything from scratch."),
         ("🎲  Generate Random Farm","generateRandomFarm()","Auto-generates random name/location/owner AND "
          "populates the farm with randomised data in one click.")],
        [3.5*cm, 5*cm, 8*cm]))
    e.append(subsection("SHARED HELPER"))
    e.append(Paragraph(
        "Both createWithSampleData() and createEmptyFarm() delegate to the private doCreateFarm(boolean populate) method. "
        "When populate=true the method calls FarmService.markAsRandomized() before auto-saving, then invokes "
        "DataRandomizerService.populateNewFarm(). When populate=false both calls are skipped and the farm "
        "opens completely empty.",
        ST["body"]))
    e.append(HR())

    e += class_header("BoundaryEditorDialog", "Dialog Controller", "com.example.controllers")
    e.append(Paragraph(
        "Opens a dialog allowing the user to define a zone's geographic boundary by choosing a shape "
        "(Rectangle, Circle, Triangle) and entering coordinate inputs. "
        "Calls GoegraphicBoundries factory methods and saves the boundary to the zone via ZoneService.",
        ST["body"]))
    e.append(HR())

    e += class_header("ZoneMapDialog / ZoneAlertHistoryDialog / SensorChartDialog / SensorHistoryDialog",
                      "Dialog Controllers", "com.example.controllers")
    e.append(Paragraph(
        "ZoneMapDialog: draws the zone's boundary polygon on a JavaFX Canvas. "
        "ZoneAlertHistoryDialog: lists all alerts filtered to the selected zone. "
        "SensorChartDialog: shows a LineChart of numeric sensor readings over time; "
        "GPS sensors get an inside/outside stepped chart. "
        "SensorHistoryDialog: displays the raw reading history in a TableView.",
        ST["body"]))
    return e

def sec_views():
    e = [PageBreak(), Paragraph("13.  Layer: Views (FXML)", ST["section"]),
         Paragraph(
        "All FXML files reside in src/main/resources/com/example/views/. "
        "They declare the UI layout; controller classes are referenced via fx:controller attributes. "
        "No business logic appears in FXML — all onAction handlers are @FXML methods in the controller.",
        ST["body"]), Spacer(1,0.2*cm)]

    views = [
        ("startup.fxml", "StartupController",
         "Two-panel VBox layout. Left panel: ListView of saved farms with Open/Delete actions per row. "
         "Right panel: GridPane form (Name, Location, Owner fields), then a labelled pair of creation "
         "buttons — '📋 Sample Data' and '⬜ Empty Farm' — plus a '🎲 Generate Random Farm' button below. "
         "Selecting Sample Data pre-populates the farm via DataRandomizerService; Empty Farm skips "
         "population and opens a blank farm.",
         [("farmList","ListView","Saved farm entries with Open/Delete buttons"),
          ("newFarmName / newFarmLocation / newFarmOwner","TextField","New farm input fields"),
          ("errorLabel","Label","Validation error message (hidden until triggered)")]),
        ("main.fxml","MainController",
         "BorderPane shell. Left contains sidebar VBox with logo, toggle button, and 8 navigation buttons. "
         "Top contains HBox topbar with farmNameLabel, spacer, clockLabel, bell button, alertBadge. "
         "Center is left empty and set programmatically by SceneManager.navigateTo().",
         [("sidebar","VBox","Collapsible navigation sidebar"),
          ("logoLabel","Label","Brand label hidden when collapsed"),
          ("farmNameLabel","Label","Farm name in topbar"),
          ("clockLabel","Label","Live clock"),
          ("alertBadge","Label","Alert count badge"),
          ("btnDashboard/Zones/Animals/...","Button x8","Navigation buttons")]),
        ("dashboard.fxml","DashboardController",
         "ScrollPane > VBox. Header row with page title. Four KPI cards in HBox. "
         "BarChart for production. TableView for recent alerts. Quick-action HBox.",
         [("kpiAnimals/kpiZones/kpiSensors/kpiAlerts","Label","KPI numeric values"),
          ("productionChart","BarChart","Production by zone type"),
          ("alertsTable","TableView","Top-5 recent alerts")]),
        ("zones.fxml","ZonesController",
         "ScrollPane > VBox. TabPane with three tabs (Livestock, Crop, Aquaculture). "
         "Each tab: search HBox, TableView, toolbar buttons. Right detail panel in SplitPane.",
         [("tabPane","TabPane","Three zone-type tabs"),
          ("livestockTable/cropTable/aquaTable","TableView","Zone tables"),
          ("searchField","TextField","Live name filter"),
          ("detailPanel","VBox","Zone detail on right side")]),
        ("animals.fxml","AnimalsController",
         "ScrollPane > VBox. Filter bar (zone, type, health ComboBoxes). "
         "TableView of animals. Detail VBox on right with health badge, GPS info, history buttons.",
         [("animalTable","TableView","Full animal list"),
          ("zoneFilter/typeFilter/healthFilter","ComboBox","Filter dropdowns"),
          ("detailPane","VBox","Selected animal details")]),
        ("sensors.fxml","SensorsController",
         "ScrollPane > VBox. Filter ComboBox. FlowPane of dynamically built sensor cards. "
         "Each card is a VBox with code, type label, last reading, and severity badge.",
         [("sensorFlow","FlowPane","Dynamic grid of sensor cards"),
          ("typeFilter","ComboBox","Filter by sensor category")]),
        ("alerts.fxml","AlertsController",
         "ScrollPane > VBox. Filter HBox (type, severity, resolution ComboBoxes). "
         "TableView of all alerts. Detail VBox below showing selected alert's full information.",
         [("alertsTable","TableView","Full alert list"),
          ("typeFilter/severityFilter/resolutionFilter","ComboBox","Filters"),
          ("detailPanel","VBox","Selected alert details")]),
        ("reports.fxml","ReportsController",
         "SplitPane. Left: ListView of report types. Right: ScrollPane > VBox "
         "dynamically populated with report content (KPI cards, tables, line charts). "
         "Export button at the bottom.",
         [("reportTypeList","ListView","Report type options"),
          ("reportContent","VBox","Dynamically built report output"),
          ("exportBtn","Button","Export to .txt file")]),
        ("settings.fxml","SettingsController",
         "ScrollPane > VBox. Farm info edit section (TextFields for name/location/owner, Save button). "
         "Theme toggle (RadioButtons dark/light). About section (project info labels).",
         [("farmNameField/locationField/ownerField","TextField","Editable farm info"),
          ("darkTheme/lightTheme","RadioButton","Theme selection"),
          ("versionLabel","Label","App version display")]),
        ("simulation.fxml","SimulationController",
         "ScrollPane > VBox. Header with sim date and day count. "
         "Step controls (+1/+7/+30 Day buttons, Reset). KPI summary HBox (6 cards). "
         "Quick-nav buttons (Reports, Sensors, Animals). Activity log ListView.",
         [("simDateLabel/simDayLabel","Label","Current simulation date and day count"),
          ("kpiMilk/kpiEggs/kpiHarvests/kpiHealthEvents/kpiAlerts/kpiMortality","Label","Step KPIs"),
          ("activityLog","ListView","Chronological event log (newest first)")]),
    ]

    for fname, ctrl, desc, fields in views:
        e += class_header(fname, "FXML View", f"resources/com/example/views/", f"Controller: {ctrl}")
        e.append(Paragraph(desc, ST["body"]))
        if fields:
            e.append(subsection("KEY FX:ID COMPONENTS"))
            e.append(make_table(["fx:id","Type","Role"],
                fields, [4*cm, 4*cm, 8.5*cm]))
        e.append(HR())
    return e

def sec_styles():
    e = [PageBreak(), Paragraph("14.  Layer: Styles (CSS)", ST["section"]),
         Paragraph(
        "The application ships with two CSS files: main.css (dark theme, loaded by default) "
        "and light.css (light overrides toggled via SettingsController). "
        "A dark.css file duplicates main.css for explicit switching.",
        ST["body"]), Spacer(1,0.2*cm)]

    e.append(subsection("Dark Theme Colour Palette"))
    e.append(make_table(["Token","Hex","Used For"],
        [("slate-900","#0f172a","Root background, page background"),
         ("slate-800","#1e293b","Sidebar, card surfaces"),
         ("green-500","#22c55e","Primary accent: buttons, active states, borders"),
         ("sky-500","#0ea5e9","Secondary accent: info badges, links"),
         ("text-primary","#f1f5f9","Main text colour"),
         ("text-muted","#94a3b8","Secondary labels, placeholders"),
         ("danger","#ef4444","Critical alerts, error states"),
         ("warning","#f59e0b","Warning alerts, caution indicators"),
         ("card-bg","#1e293b","Content card background"),
         ("topbar-bg","#0f172a","Top navigation bar background"),
         ("sidebar-bg","#1e293b","Left sidebar background")],
        [3.5*cm, 3*cm, 10*cm]))

    e.append(subsection("Key CSS Class Definitions"))
    e.append(make_table(["Class","Applied To","Effect"],
        [(".root-pane","BorderPane root","Sets background-color to slate-900."),
         (".sidebar","VBox sidebar","Background slate-800, no border radius."),
         (".sidebar-logo","Label","Large green brand label, padding 20px."),
         (".nav-btn","Button","Transparent background, muted text; full-width; left-aligned."),
         (".nav-active","Button (active)","Green-500 background, white bold text."),
         (".topbar","HBox","Slate-900 background, bottom border, fixed height ~52px."),
         (".topbar-title","Label","White bold 16pt font for farm name."),
         (".topbar-clock","Label","Muted text 11pt monospace-style."),
         (".topbar-alert-badge","Label","Red pill badge: danger background, white text, 10px border-radius."),
         (".content-root","VBox","Page content wrapper, padding 24px all sides."),
         (".page-title","Label","Bold 22pt white text for page headings."),
         (".section-title","Label","Green-500 bold 11pt uppercase labels."),
         (".kpi-card","VBox","Slate-800 rounded card, 12px radius, padding 16px."),
         (".kpi-value","Label","Bold 24pt white number."),
         (".kpi-label","Label","Muted 10pt label below value."),
         (".btn-primary","Button","Green-500 filled, white text, 8px radius, hover darkens."),
         (".btn-secondary","Button","Slate-700 filled, muted text, 8px radius."),
         (".badge-healthy","Label","Green pill badge."),
         (".badge-sick","Label","Red pill badge."),
         (".badge-quarantined","Label","Orange pill badge."),
         (".badge-normal","Label","Green pill badge for sensor reading level."),
         (".badge-warning","Label","Yellow pill badge."),
         (".badge-critical","Label","Red pill badge.")],
        [4*cm, 4*cm, 8.5*cm]))

    e.append(subsection("Light Theme (light.css)"))
    e.append(Paragraph(
        "Overrides background colours to white/light-grey tones, text colours to near-black, "
        "and card backgrounds to #f8fafc. Green accent is retained. "
        "Applied by SettingsController via SceneManager.applyStylesheet('/com/example/styles/light.css').",
        ST["body"]))
    return e

def sec_utilities():
    e = [PageBreak(), Paragraph("15.  Utilities and Entry Point", ST["section"])]

    e += class_header("App", "JavaFX Application Entry Point", "com.example")
    e.append(Paragraph(
        "Extends javafx.application.Application. The start() method calls SceneManager.initStartup() "
        "to load the startup screen, sets the window title, enforces minimum dimensions (920x600), "
        "and shows the stage. The static main() method calls launch(args) to initialise the "
        "JavaFX platform and invoke start() on the FX Application Thread.",
        ST["body"]))
    e.append(HR())

    e += class_header("SceneManager", "Singleton Utility", "com.example.utils")
    e.append(subsection("PURPOSE"))
    e.append(Paragraph(
        "Central navigation hub for the SPA-style single-window application. "
        "Manages the JavaFX Stage and root BorderPane. Holds a static ROUTES map "
        "mapping string route names to FXML resource paths. "
        "A comment block at the top of the file documents the four-step workflow for adding new screens.",
        ST["body"]))
    e.append(make_table(["Method","Return","Logic"],
        [("initStartup(Stage)","void","Static. Creates instance, loads startup.fxml into a new Scene (920x600), applies CSS."),
         ("loadMainApp()","void","Loads main.fxml into a new Scene (1280x780), preserves fullscreen state, calls navigateTo('dashboard')."),
         ("navigateTo(route)","void","Looks up FXML path in ROUTES, loads via FXMLLoader, plays 180ms FadeTransition, sets as root center."),
         ("navigateToStartup()","void","Transitions back to the startup screen; resets min dimensions."),
         ("applyStylesheet(path)","void","Clears and replaces scene stylesheets (used for theme switching)."),
         ("getInstance()","SceneManager","Returns the singleton."),
         ("getRoot()","BorderPane","Returns the main shell BorderPane.")],
        [5.5*cm, 2.5*cm, 8.5*cm]))
    e.append(subsection("Route Registry"))
    routes = [
        ("dashboard","dashboard.fxml","Main KPI overview"),
        ("zones","zones.fxml","Zone management (tabbed)"),
        ("animals","animals.fxml","Animal management"),
        ("sensors","sensors.fxml","Sensor grid with readings"),
        ("alerts","alerts.fxml","Alert centre"),
        ("reports","reports.fxml","Report generation"),
        ("simulation","simulation.fxml","Time-step simulation"),
        ("settings","settings.fxml","Settings and about"),
    ]
    e.append(make_table(["Route Key","FXML File","Screen Purpose"], routes,
                         [3.5*cm, 4*cm, 9*cm]))
    e.append(HR())

    e += class_header("FarmRepository", "Utility Class", "com.example.utils")
    e.append(Paragraph(
        "Persists farm metadata to ~/.smartfarm/farms.properties using Java's Properties API "
        "(no external library required). The flat key schema prefixes all entries with 'farm.i.' "
        "where i is the farm index. Zones use 'farm.i.zone.j.*' and animals use 'farm.i.animal.j.*'. "
        "save() performs an upsert (replace by id or append). delete() removes by id.",
        ST["body"]))
    e.append(Paragraph(
        "Inner classes: SavedFarm (id, name, location, owner, createdAt, isDemo, wasRandomized, "
        "List<SavedZone>, List<SavedAnimal>), SavedZone (type, name, lstType), "
        "SavedAnimal (name, species, type, age, weight, zoneName, health).",
        ST["body"]))
    e.append(HR())

    e += class_header("RandomFarmGenerator", "Utility Class", "com.example.utils")
    e.append(Paragraph(
        "Provides random name lists (farm names, animal names, zone names) and helper methods "
        "used by DataRandomizerService to build realistic farm data for new user farms.",
        ST["body"]))
    e.append(HR())

    e += class_header("DemoData", "Utility / Seed Data Class", "Menu")
    e.append(Paragraph(
        "A static factory class with a single create() method that returns a fully populated demo Farm. "
        "The demo includes: North Pasture (LivestockZONE, 3 animals with bio sensors, GPS collars, "
        "health events, feeding program), South Fields (CropZONE, 3 crops at different growth stages, "
        "env and soil sensors), Pond Alpha (AquacultureZONE, tilapia and salmon, water sensors). "
        "Historical readings are injected via private injectNumericReadings() and injectGPSReadings() "
        "helpers that create 15 timestamped readings spaced one day apart.",
        ST["body"]))
    return e

def sec_relationships():
    e = [PageBreak(), Paragraph("16.  Class Relationship Summary", ST["section"])]
    rows = [
        ("Farm","LivestockZONE, CropZONE, AquacultureZONE, Alert, Report subtypes","FarmService, FarmReport, ReportService"),
        ("ZONE (abstract)","GoegraphicBoundries","Farm, LivestockZONE, CropZONE, AquacultureZONE"),
        ("LivestockZONE","Animal, BioSensor, GPSCollarSensor, FeedingProgram","Farm, AnimalService, ZoneService, ReportLiveStockZone"),
        ("CropZONE","Crop, EnvSensor, SoilSensor","Farm, ZoneService, ReportCropZone"),
        ("AquacultureZONE","AquacultureSpecies, WaterSensor","Farm, ZoneService, ReportAquacultureZone"),
        ("Animal","LIvestockType, AnimalHealthStatus, HealthEvent, BioSensor, GPSCollarSensor, WeightRecord, MilkRecord, EggRecord","LivestockZONE, AnimalService, HealthAlert"),
        ("Sensor (abstract)","ZONE, SensorReading","LivestockZONE, CropZONE, AquacultureZONE, SensorService"),
        ("NumericSensor","—","BioSensor, EnvSensor, SoilSensor, WaterSensor, NumericSensorReading"),
        ("GPSCollarSensor","Animal, GPSSensorReading","LivestockZONE, Animal, SensorService"),
        ("Alert (abstract)","AlertType, AlertSeverity, AlertResolution, ZONE","Farm, AlertService, AlertsController"),
        ("SensorAlert","SensorReading","Farm, SimulationService"),
        ("HealthAlert","HealthEvent","Farm, AnimalService"),
        ("Report (abstract)","ZONE, Alert","Farm, FarmReport, ReportService"),
        ("FarmService","Farm, FarmRepository, DemoData, DataRandomizerService","All services and controllers (via getInstance())"),
        ("SceneManager","Stage, BorderPane, FXMLLoader","App, MainController, all nav methods"),
        ("FarmRepository","Properties, Path","FarmService, StartupController"),
        ("SimulationService","FarmService, all domain classes","SimulationController"),
    ]
    e.append(make_table(["Class","Depends On","Depended on By"], rows,
                         [4*cm, 6.5*cm, 6*cm]))
    return e

def sec_dataflow():
    e = [PageBreak(), Paragraph("17.  Data Flow Examples", ST["section"])]

    scenarios = [
        ("Scenario A — Farm Manager Adds a New Crop Zone",
         [("1","User fills zone name in the Add Zone dialog in zones.fxml."),
          ("2","ZonesController.handleAddZone() reads the dialog result."),
          ("3","Controller calls ZoneService.addZone(new CropZONE(name))."),
          ("4","ZoneService delegates to farm.addZone(zone), which adds to cropZones list."),
          ("5","ZoneService calls FarmService.autoSave(), writing the new zone to farms.properties."),
          ("6","Controller clears and reloads the ObservableList; new zone appears in the table."),
          ("7","User can now click the zone to see the detail panel or add crops/sensors."),
         ]),
        ("Scenario B — A Soil Sensor Reading Exceeds its Threshold",
         [("1","SimulationService.simulateNumericSensor(soilSensor, day, result) is called during a simulation step."),
          ("2","A random reading value is generated; it exceeds the sensor's maxThreshold."),
          ("3","A NumericSensorReading is created; determineSeverity() classifies it as WARNING or CRITICAL."),
          ("4","An instanceof check identifies it as a SoilSensor; a SensorAlert with AlertType.SoilSensorAlert is created."),
          ("5","farm.registerAlert(alert) adds the alert to the farm's alert list."),
          ("6","result.alertsGenerated is incremented; the event is appended to result.log."),
          ("7","After the simulation step, SimulationController updates kpiAlerts label."),
          ("8","Navigating to Alerts page shows the new alert in the table with CRITICAL badge."),
         ]),
        ("Scenario C — Farm Manager Generates a Production Report",
         [("1","User selects 'Farm Production — Monthly' in the reports.fxml ListView."),
          ("2","ReportsController calls ReportService.generateFarmProductionReport(ReportType.Monthly)."),
          ("3","ReportService computes periodStart = now.minusMonths(1), calls farm.generateFarmProductionReport()."),
          ("4","Farm calls generateAllZoneProductionReports(), creating LivestockProductionReport, CropProductionReport, AquacultureProductionReport for each zone."),
          ("5","Each zone report iterates its entities (animals / crops / species) accumulating production KPIs."),
          ("6","FarmProductionReport aggregates all zone reports, computes averages, generates advisory notes."),
          ("7","ReportsController receives the FarmProductionReport and renders KPI cards + zone breakdown table in reportContent VBox."),
          ("8","User clicks Export; controller calls FileChooser, writes report.toString() to the selected .txt file."),
         ]),
    ]

    for title, steps in scenarios:
        e.append(Paragraph(title, ST["h2"]))
        rows = [(n, Paragraph(esc(desc), ST["body"])) for n,desc in steps]
        t = Table([[Paragraph(f"<b>{esc(n)}</b>", ST["body"]), d] for n,d in rows],
                  colWidths=[1*cm, 15.5*cm])
        t.setStyle(TableStyle([
            ("GRID",(0,0),(-1,-1),0.3,C_BORDER),
            ("ROWBACKGROUNDS",(0,0),(-1,-1),[C_WHITE, C_ALT_ROW]),
            ("VALIGN",(0,0),(-1,-1),"TOP"),
            ("TOPPADDING",(0,0),(-1,-1),4),
            ("BOTTOMPADDING",(0,0),(-1,-1),4),
            ("LEFTPADDING",(0,0),(-1,-1),5),
        ]))
        e.append(t)
        e.append(Spacer(1, 0.4*cm))
    return e

# ── Main ───────────────────────────────────────────────────────────────────────

def build():
    out = "c:/Users/catal/OneDrive/Documents/tp poo/javafx/farm_management_documentation.pdf"
    doc = DocBuilder(out)

    story = []
    story.append(NextPageTemplate("Cover"))
    story += cover_page()

    story.append(NextPageTemplate("Normal"))
    story += toc_section(doc.toc)
    story += sec_overview()
    story += sec_architecture()
    story += sec_additional_classes()
    story += sec_alerts()
    story += sec_animals()
    story += sec_entities()
    story += sec_farm()
    story += sec_reports()
    story += sec_sensors()
    story += sec_zones()
    story += sec_services()
    story += sec_controllers()
    story += sec_views()
    story += sec_styles()
    story += sec_utilities()
    story += sec_relationships()
    story += sec_dataflow()

    doc.multiBuild(story)
    print(f"PDF generated: {out}")
    print(f"Total pages: {doc.page}")

if __name__ == "__main__":
    build()
