package Reports;

import Alerts.Alert;
import ZONES.ZONE;
import java.time.LocalDateTime;
import java.util.List;

public class ProductionReport extends Report {

    public ProductionReport(ZONE zone, ReportType reportType,
                            LocalDateTime periodStart, LocalDateTime periodEnd,
                            List<Alert> alerts) {
        super(zone, reportType, periodStart, periodEnd, alerts);
    }

    @Override
    protected void computeSpecificStats(List<Alert> alerts) {
        // No production-specific stats yet.
    }

    @Override
    protected void computeSensorSummaries() {
        if (getSensorSummaries().isEmpty()) addSensorSummary("No sensor summaries available");
    }

    @Override
    protected String specificStatsBlock() {
        return "  PRODUCTION : N/A\n";
    }
}