package com.gxa.pipe.dataVsualization.dashboard;

import java.time.LocalDateTime;

public record DashboardCurrentSummaryRow(
        Long areaId,
        Long sensorCount,
        Long abnormalSensorCount,
        Long underwayTaskCount,
        Long overtimeTaskCount,
        LocalDateTime refreshedAt) {
}
