package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dashboard.DashboardSummaryResponse;
import com.gxa.pipe.dataVsualization.dashboard.DashboardTooltipResponse;

public interface DashboardReadService {

    DashboardSummaryResponse getSummary(Long areaId, Long startTime, Long endTime);

    DashboardTooltipResponse getTooltip(Long areaId, Long startTime, Long endTime);

    boolean isReadModelAvailable();
}
