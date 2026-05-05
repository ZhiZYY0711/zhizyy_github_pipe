package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailResponse;
import com.gxa.pipe.dataVsualization.total.RunningWaterResponse;
import com.gxa.pipe.dataVsualization.total.WholeKpiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardReadServiceImpl implements DashboardReadService {

    private static final int ALARM_LIMIT = 100;

    private final DashboardMapper mapper;
    private final AreaScopeResolver areaScopeResolver;
    private final DashboardLocalCache cache;

    @Value("${performance.dashboard-read-model.summary-cache-ttl-ms:15000}")
    private long summaryTtlMillis = 15_000L;

    @Value("${performance.dashboard-read-model.tooltip-cache-ttl-ms:30000}")
    private long tooltipTtlMillis = 30_000L;

    @Override
    public DashboardSummaryResponse getSummary(Long areaId, Long startTime, Long endTime) {
        String cacheKey = "summary:" + areaId + ":" + startTime + ":" + endTime;
        return cache.get(cacheKey, summaryTtlMillis, () -> loadSummary(areaId, startTime, endTime));
    }

    @Override
    public DashboardTooltipResponse getTooltip(Long areaId, Long startTime, Long endTime) {
        String cacheKey = "tooltip:" + areaId + ":" + startTime + ":" + endTime;
        return cache.get(cacheKey, tooltipTtlMillis, () -> loadTooltip(areaId, startTime, endTime));
    }

    @Override
    public boolean isReadModelAvailable() {
        DashboardFreshnessRow freshness = mapper.selectLatestFreshness();
        return freshness != null
                && freshness.metricRefreshedAt() != null
                && freshness.currentRefreshedAt() != null
                && freshness.alarmRefreshedAt() != null;
    }

    private DashboardSummaryResponse loadSummary(Long areaId, Long startTime, Long endTime) {
        AreaScope scope = areaScopeResolver.resolve(areaId);
        Long rollupAreaId = scope.rollupAreaId();
        DashboardCurrentSummaryRow current = mapper.selectCurrentSummary(rollupAreaId);
        List<AreaDetailResponse> trend = nullToEmpty(mapper.selectMetricTrend(rollupAreaId, startTime, endTime));
        Long warnings = valueOrZero(mapper.selectTodayWarningCount(rollupAreaId));
        List<RunningWaterResponse> alarms = nullToEmpty(mapper.selectRecentAlarms(
                scope.areaStart(), scope.areaEnd(), startTime, endTime, ALARM_LIMIT));
        DashboardFreshness freshness = toFreshness(mapper.selectLatestFreshness());
        WholeKpiResponse kpi = toKpi(current, warnings);

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setKpi(kpi);
        response.setAlarms(alarms);
        response.setAreaTrend(trend);
        response.setFreshness(freshness);
        response.setGeneratedAt(LocalDateTime.now().toString());
        return response;
    }

    private DashboardTooltipResponse loadTooltip(Long areaId, Long startTime, Long endTime) {
        AreaScope scope = areaScopeResolver.resolve(areaId);
        Long rollupAreaId = scope.rollupAreaId();
        DashboardCurrentSummaryRow current = mapper.selectCurrentSummary(rollupAreaId);
        Long warnings = valueOrZero(mapper.selectWarningCount(rollupAreaId, startTime, endTime));
        AreaDetailResponse average = emptyAverageIfNull(mapper.selectMetricAverage(rollupAreaId, startTime, endTime));

        DashboardTooltipResponse response = new DashboardTooltipResponse();
        response.setAreaId(String.valueOf(rollupAreaId));
        response.setAreaName(null);
        response.setSensorNumbers(current == null ? 0L : valueOrZero(current.sensorCount()));
        response.setAbnormalSensorNumbers(current == null ? 0L : valueOrZero(current.abnormalSensorCount()));
        response.setWarnings(warnings);
        response.setFlow(nullToZero(average.getAveFlow()));
        response.setPressure(nullToZero(average.getAvePressure()));
        response.setTemperature(nullToZero(average.getAveTemperature()));
        response.setVibration(nullToZero(average.getAveVibration()));
        return response;
    }

    private WholeKpiResponse toKpi(DashboardCurrentSummaryRow current, Long warnings) {
        WholeKpiResponse response = new WholeKpiResponse();
        response.setSensorNumbers(current == null ? 0L : valueOrZero(current.sensorCount()));
        response.setAbnormalSensorNumbers(current == null ? 0L : valueOrZero(current.abnormalSensorCount()));
        response.setWarnings(valueOrZero(warnings));
        response.setUnderwayTask(current == null ? 0L : valueOrZero(current.underwayTaskCount()));
        response.setOvertimeTask(current == null ? 0L : valueOrZero(current.overtimeTaskCount()));
        return response;
    }

    private DashboardFreshness toFreshness(DashboardFreshnessRow row) {
        if (row == null) {
            return new DashboardFreshness();
        }

        return new DashboardFreshness(
                format(row.metricRefreshedAt()),
                format(row.currentRefreshedAt()),
                format(row.alarmRefreshedAt()));
    }

    private AreaDetailResponse emptyAverageIfNull(AreaDetailResponse average) {
        return average == null ? new AreaDetailResponse(0.0, 0.0, 0.0, 0.0, 0L) : average;
    }

    private <T> List<T> nullToEmpty(List<T> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
