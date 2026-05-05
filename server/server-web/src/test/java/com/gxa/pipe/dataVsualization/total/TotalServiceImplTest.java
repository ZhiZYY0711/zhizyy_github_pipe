package com.gxa.pipe.dataVsualization.total;

import com.gxa.pipe.dataVsualization.dashboard.DashboardSummaryResponse;
import com.gxa.pipe.dataVsualization.dashboard.DashboardReadService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TotalServiceImplTest {

    @Test
    void wholeKpiFallsBackToLegacyMapperWhenReadModelIsUnavailable() {
        TotalMapper totalMapper = mock(TotalMapper.class);
        DashboardReadService dashboardReadService = mock(DashboardReadService.class);
        TotalServiceImpl service = new TotalServiceImpl(totalMapper, dashboardReadService);
        WholeKpiResponse legacyKpi = new WholeKpiResponse();
        legacyKpi.setSensorNumbers(12L);

        when(dashboardReadService.isReadModelAvailable()).thenReturn(false);
        when(totalMapper.selectWholeKpi(370100L)).thenReturn(legacyKpi);

        WholeKpiResponse result = service.getWholeKpi(370100L);

        assertThat(result).isSameAs(legacyKpi);
        verify(dashboardReadService, never()).getSummary(370100L, null, null);
    }

    @Test
    void runningWaterAlarmFallsBackToLegacyMapperWhenReadModelIsUnavailable() {
        TotalMapper totalMapper = mock(TotalMapper.class);
        DashboardReadService dashboardReadService = mock(DashboardReadService.class);
        TotalServiceImpl service = new TotalServiceImpl(totalMapper, dashboardReadService);
        RunningWaterResponse alarm = new RunningWaterResponse(
                "1", 1767225600000L, "2", "传感器-2", "370102", "山东济南历下", "检测到异常数据", "危险");

        when(dashboardReadService.isReadModelAvailable()).thenReturn(false);
        when(totalMapper.selectRunningWaterAlarm(370100L, 1767225600000L, 1767311999999L)).thenReturn(List.of(alarm));

        List<RunningWaterResponse> result = service.getRunningWaterAlarm(370100L, 1767225600000L, 1767311999999L);

        assertThat(result).containsExactly(alarm);
        verify(dashboardReadService, never()).getSummary(370100L, 1767225600000L, 1767311999999L);
    }

    @Test
    void runningWaterAlarmFallsBackToLegacyMapperWhenReadModelReturnsNoAlarms() {
        TotalMapper totalMapper = mock(TotalMapper.class);
        DashboardReadService dashboardReadService = mock(DashboardReadService.class);
        TotalServiceImpl service = new TotalServiceImpl(totalMapper, dashboardReadService);
        RunningWaterResponse alarm = new RunningWaterResponse(
                "1", 1767225600000L, "2", "传感器-2", "370102", "山东济南历下", "检测到异常数据", "危险");

        when(dashboardReadService.isReadModelAvailable()).thenReturn(true);
        when(dashboardReadService.getSummary(370100L, null, null)).thenReturn(new DashboardSummaryResponse());
        when(totalMapper.selectRunningWaterAlarm(370100L, null, null)).thenReturn(List.of(alarm));

        List<RunningWaterResponse> result = service.getRunningWaterAlarm(370100L, null, null);

        assertThat(result).containsExactly(alarm);
    }
}
