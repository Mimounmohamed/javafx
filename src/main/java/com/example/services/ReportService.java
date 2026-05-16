package com.example.services;

import Reports.FarmProductionReport;
import Reports.FarmReport;
import Reports.ProductionReport;
import Reports.Report;
import Reports.ReportType;
import ZONES.ZONE;

import java.time.LocalDateTime;

public class ReportService {

    private static ReportService instance;
    private final FarmService farmService = FarmService.getInstance();

    private ReportService() {}

    public static ReportService getInstance() {
        if (instance == null) instance = new ReportService();
        return instance;
    }

    public FarmReport generateFarmReport(ReportType type) {
        LocalDateTime end   = LocalDateTime.now();
        LocalDateTime start = periodStart(type, end);
        return farmService.getFarm().generateFarmReport(type, start, end);
    }

    public FarmProductionReport generateFarmProductionReport(ReportType type) {
        LocalDateTime end   = LocalDateTime.now();
        LocalDateTime start = periodStart(type, end);
        return farmService.getFarm().generateFarmProductionReport(type, start, end);
    }

    public Report generateZoneReport(ZONE zone, ReportType type) {
        LocalDateTime end   = LocalDateTime.now();
        LocalDateTime start = periodStart(type, end);
        return farmService.getFarm().generateReportForZone(zone, type, start, end);
    }

    public ProductionReport generateZoneProductionReport(ZONE zone, ReportType type) {
        LocalDateTime end   = LocalDateTime.now();
        LocalDateTime start = periodStart(type, end);
        return farmService.getFarm().generateProductionReportForZone(zone, type, start, end);
    }

    private LocalDateTime periodStart(ReportType type, LocalDateTime end) {
        return switch (type) {
            case Daily     -> end.minusDays(1);
            case Weekly    -> end.minusWeeks(1);
            case Monthly   -> end.minusMonths(1);
            case Quarterly -> end.minusMonths(3);
            case Yearly    -> end.minusYears(1);
        };
    }
}
