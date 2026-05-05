package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailResponse;
import com.gxa.pipe.dataVsualization.total.RunningWaterResponse;
import com.gxa.pipe.dataVsualization.total.WholeKpiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {
    private WholeKpiResponse kpi = new WholeKpiResponse();
    private List<RunningWaterResponse> alarms = new ArrayList<>();
    private List<AreaDetailResponse> areaTrend = new ArrayList<>();
    private DashboardFreshness freshness = new DashboardFreshness();
    private String generatedAt;
}
