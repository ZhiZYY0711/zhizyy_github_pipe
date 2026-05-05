package com.gxa.pipe.dataVsualization.dashboard;

import java.time.LocalDateTime;

public record DashboardFreshnessRow(
        LocalDateTime metricRefreshedAt,
        LocalDateTime currentRefreshedAt,
        LocalDateTime alarmRefreshedAt) {
}
