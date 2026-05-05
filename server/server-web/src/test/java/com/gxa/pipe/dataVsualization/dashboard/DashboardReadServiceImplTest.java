package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailResponse;
import com.gxa.pipe.dataVsualization.total.RunningWaterResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardReadServiceImplTest {

    @Test
    void summaryReadsDashboardReadModels() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        AreaScopeResolver resolver = new AreaScopeResolver();
        DashboardReadServiceImpl service = new DashboardReadServiceImpl(mapper, resolver, new DashboardLocalCache());
        DashboardCurrentSummaryRow current = new DashboardCurrentSummaryRow(
                370100L, 100L, 4L, 2L, 1L, LocalDateTime.parse("2026-05-04T08:01:00"));
        DashboardFreshnessRow freshness = new DashboardFreshnessRow(
                LocalDateTime.parse("2026-05-04T08:00:00"),
                LocalDateTime.parse("2026-05-04T08:01:00"),
                LocalDateTime.parse("2026-05-04T08:01:15"));
        AreaDetailResponse trend = new AreaDetailResponse(11.5, 6.25, 32.5, 2.5, 1767225600000L);
        RunningWaterResponse alarm = new RunningWaterResponse(
                "9", 1767225600000L, "12", "传感器-12", "370100", "山东济南", "高危状态检测到严重异常数据", "高危");

        when(mapper.selectCurrentSummary(370100L)).thenReturn(current);
        when(mapper.selectMetricTrend(370100L, 1767225600000L, 1767311999999L)).thenReturn(List.of(trend));
        when(mapper.selectTodayWarningCount(370100L)).thenReturn(7L);
        when(mapper.selectRecentAlarms(370100L, 370199L, 1767225600000L, 1767311999999L, 100)).thenReturn(List.of(alarm));
        when(mapper.selectLatestFreshness()).thenReturn(freshness);

        var summary = service.getSummary(370100L, 1767225600000L, 1767311999999L);

        assertThat(summary.getKpi().getSensorNumbers()).isEqualTo(100L);
        assertThat(summary.getKpi().getWarnings()).isEqualTo(7L);
        assertThat(summary.getAlarms()).containsExactly(alarm);
        assertThat(summary.getAreaTrend()).containsExactly(trend);
        assertThat(summary.getFreshness().getAlarmRefreshedAt()).isEqualTo("2026-05-04T08:01:15");
        assertThat(summary.getGeneratedAt()).isNotBlank();
    }

    @Test
    void tooltipReturnsZeroValuesWhenReadModelRowsAreMissing() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        DashboardReadServiceImpl service = new DashboardReadServiceImpl(mapper, new AreaScopeResolver(), new DashboardLocalCache());

        var tooltip = service.getTooltip(370102L, null, null);

        assertThat(tooltip.getAreaId()).isEqualTo("370102");
        assertThat(tooltip.getSensorNumbers()).isZero();
        assertThat(tooltip.getPressure()).isZero();
        assertThat(tooltip.getWarnings()).isZero();
    }

    @Test
    void tooltipUsesPrecomputedWeightedAverage() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        DashboardReadServiceImpl service = new DashboardReadServiceImpl(mapper, new AreaScopeResolver(), new DashboardLocalCache());
        AreaDetailResponse average = new AreaDetailResponse(12.5, 6.5, 31.5, 2.5, 0L);

        when(mapper.selectMetricAverage(370100L, 1767225600000L, 1767311999999L)).thenReturn(average);

        var tooltip = service.getTooltip(370100L, 1767225600000L, 1767311999999L);

        assertThat(tooltip.getAreaId()).isEqualTo("370100");
        assertThat(tooltip.getAreaName()).isNull();
        assertThat(tooltip.getFlow()).isEqualTo(12.5);
        assertThat(tooltip.getPressure()).isEqualTo(6.5);
        assertThat(tooltip.getTemperature()).isEqualTo(31.5);
        assertThat(tooltip.getVibration()).isEqualTo(2.5);
    }

    @Test
    void readModelIsAvailableOnlyWhenAllModelsHaveFreshness() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        DashboardReadServiceImpl service = new DashboardReadServiceImpl(mapper, new AreaScopeResolver(), new DashboardLocalCache());

        when(mapper.selectLatestFreshness()).thenReturn(new DashboardFreshnessRow(
                LocalDateTime.parse("2026-05-04T08:00:00"),
                LocalDateTime.parse("2026-05-04T08:01:00"),
                null));
        assertThat(service.isReadModelAvailable()).isFalse();

        when(mapper.selectLatestFreshness()).thenReturn(new DashboardFreshnessRow(
                LocalDateTime.parse("2026-05-04T08:00:00"),
                LocalDateTime.parse("2026-05-04T08:01:00"),
                LocalDateTime.parse("2026-05-04T08:02:00")));
        assertThat(service.isReadModelAvailable()).isTrue();
    }
}
