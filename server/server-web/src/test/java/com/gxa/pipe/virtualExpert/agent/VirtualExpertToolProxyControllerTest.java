package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.dataManagement.equipment.EquipmentService;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataQueryResponse;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataService;
import com.gxa.pipe.dataManagement.task.TaskService;
import com.gxa.pipe.dataVsualization.dataMonitoring.DataMonitoringService;
import com.gxa.pipe.dataVsualization.dataMonitoring.PipeIndicatorResponse;
import com.gxa.pipe.log.LogQueryResponse;
import com.gxa.pipe.log.LogService;
import com.gxa.pipe.manoeuvre.ManoeuvreQueryResponse;
import com.gxa.pipe.manoeuvre.ManoeuvreService;
import com.gxa.pipe.pipelineTopology.MonitoringFilterOptionsResponse;
import com.gxa.pipe.pipelineTopology.MonitoringPipeOption;
import com.gxa.pipe.pipelineTopology.MonitoringSegmentOption;
import com.gxa.pipe.pipelineTopology.PipelineTopologyService;
import com.gxa.pipe.utils.PageResult;
import com.gxa.pipe.virtualExpert.agent.dto.ToolQueryResponse;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirtualExpertToolProxyControllerTest {

    @Test
    void monitoringTrendReturnsNormalizedReadOnlyResponse() {
        DataMonitoringService dataMonitoringService = mock(DataMonitoringService.class);
        PipelineTopologyService pipelineTopologyService = mock(PipelineTopologyService.class);
        PipeIndicatorResponse indicators = new PipeIndicatorResponse(22.5, 0, 120.0, 0, 1.8, 1, 0.02, 0);
        when(dataMonitoringService.getPipeKeyIndicators(isNull(), eq("001"), isNull())).thenReturn(indicators);

        VirtualExpertToolProxyController controller = newController(dataMonitoringService, mock(MonitoringDataService.class), pipelineTopologyService);

        ToolQueryResponse response = controller.queryMonitoringTrend("segment_001", "cp_voltage");

        assertThat(response.metric()).isEqualTo("cp_voltage");
        assertThat(response.window()).isEqualTo("24h");
        assertThat(response.summary()).contains("Monitoring indicators");
        assertThat(response.facts()).hasSize(1);
        assertThat(response.points()).hasSize(4);
        assertThat(response.metadata()).containsEntry("source", "server-web");
    }

    @Test
    void resolveOperationalScopeReturnsCandidateCatalog() {
        DataMonitoringService dataMonitoringService = mock(DataMonitoringService.class);
        MonitoringDataService monitoringDataService = mock(MonitoringDataService.class);
        PipelineTopologyService pipelineTopologyService = mock(PipelineTopologyService.class);
        MonitoringSegmentOption segment = new MonitoringSegmentOption(3L, "三号管段", 1, 370100L, 370200L, "city");
        MonitoringPipeOption pipe = new MonitoringPipeOption(1L, "山东干线", "province", "city", List.of(segment));
        when(pipelineTopologyService.getMonitoringFilterOptions(isNull(), isNull(), isNull()))
                .thenReturn(new MonitoringFilterOptionsResponse("all", List.of(pipe)));

        VirtualExpertToolProxyController controller = newController(dataMonitoringService, monitoringDataService, pipelineTopologyService);

        ToolQueryResponse response = controller.resolveOperationalScope("山东省过去一年平均温度", "山东省", null);

        assertThat(response.metric()).isEqualTo("scope_candidates");
        assertThat(response.records()).hasSize(2);
        assertThat(response.records().get(0)).containsEntry("scope_type", "pipeline");
        assertThat(response.metadata()).containsEntry("capability_status", "candidate_lookup");
    }

    @Test
    void monitoringAggregateReturnsHistoricalAggregateResult() {
        DataMonitoringService dataMonitoringService = mock(DataMonitoringService.class);
        MonitoringDataService monitoringDataService = mock(MonitoringDataService.class);
        PipelineTopologyService pipelineTopologyService = mock(PipelineTopologyService.class);
        when(monitoringDataService.aggregateByConditions(any())).thenReturn(Map.of(
                "sample_count", 24L,
                "avg_temperature", 22.5,
                "min_temperature", 18.2,
                "max_temperature", 27.1
        ));

        VirtualExpertToolProxyController controller = newController(dataMonitoringService, monitoringDataService, pipelineTopologyService);

        ToolQueryResponse response = controller.queryMonitoringAggregate(
                "segment",
                "segment_001",
                "temperature",
                "2025-04-27",
                "2026-04-27",
                "avg");

        assertThat(response.metric()).isEqualTo("temperature");
        assertThat(response.records().get(0)).containsEntry("value", 22.5);
        assertThat(response.records().get(0)).containsEntry("sample_count", 24L);
        assertThat(response.metadata()).containsEntry("capability_status", "historical_aggregate");
    }

    @Test
    void dataSourceToolsReturnRealBackendRecords() {
        DataMonitoringService dataMonitoringService = mock(DataMonitoringService.class);
        MonitoringDataService monitoringDataService = mock(MonitoringDataService.class);
        PipelineTopologyService pipelineTopologyService = mock(PipelineTopologyService.class);
        LogService logService = mock(LogService.class);
        ManoeuvreService manoeuvreService = mock(ManoeuvreService.class);
        VirtualExpertExportService exportService = mock(VirtualExpertExportService.class);

        MonitoringDataQueryResponse highRiskRecord = new MonitoringDataQueryResponse(
                "9001",
                "1.80",
                "120.00",
                "35.60",
                "0.80",
                "3",
                "高危",
                "1001",
                "温度传感器1001",
                "山东省济南市历下区",
                "山东干线",
                "3",
                "三号管段",
                "2026-04-27 10:00:00");
        when(monitoringDataService.getByPage(any())).thenReturn(new PageResult<>(1L, List.of(highRiskRecord), 1, 20));

        when(logService.queryLogs(any())).thenReturn(new PageResult<>(1L, List.of(new LogQueryResponse(
                "用户发起导出",
                7L,
                "127.0.0.1",
                "EXPORT_REPORT",
                1714200000000L,
                120L,
                0L,
                0L,
                "管理员",
                1L,
                "admin001",
                "成功")), 1, 20));

        when(manoeuvreService.findManoeuvreByParams(any())).thenReturn(List.of(new ManoeuvreQueryResponse(
                11L,
                "高温泄漏应急演练",
                370100L,
                "山东省济南市",
                "济南站",
                1714100000000L,
                1714103600000L,
                "应急指挥部",
                2,
                2,
                "温度异常和泄漏联合处置预案演练",
                List.of())));

        when(exportService.listExports("pdf", 20)).thenReturn(List.of(new ExportFile(
                "exp_001",
                "virtual-expert.pdf",
                "application/pdf",
                Path.of("/tmp/virtual-expert.pdf"),
                2048L)));

        VirtualExpertToolProxyController controller = newController(
                dataMonitoringService,
                monitoringDataService,
                pipelineTopologyService,
                mock(EquipmentService.class),
                mock(TaskService.class),
                logService,
                manoeuvreService,
                exportService);

        assertThat(controller.queryMonitoringHistory("segment", "segment_3", "temperature", null, null, "raw")
                .records().get(0)).containsEntry("id", "9001");
        assertThat(controller.queryAlarmEvents("segment_3", "24h", "critical")
                .records().get(0)).containsEntry("status_text", "高危");
        assertThat(controller.queryInspectionHistory("segment_3", "7d")
                .records().get(0)).containsEntry("sensor_id", "1001");
        assertThat(controller.queryEmergencyPlan("leak", "370100", "critical")
                .records().get(0)).containsEntry("name", "高温泄漏应急演练");
        assertThat(controller.queryOperationLogs("admin001", "report", "24h", "EXPORT_REPORT")
                .records().get(0)).containsEntry("operate", "EXPORT_REPORT");
        assertThat(controller.queryReportInventory(null, null, "pdf")
                .records().get(0)).containsEntry("export_id", "exp_001");
    }

    private VirtualExpertToolProxyController newController(
            DataMonitoringService dataMonitoringService,
            MonitoringDataService monitoringDataService,
            PipelineTopologyService pipelineTopologyService) {
        return newController(
                dataMonitoringService,
                monitoringDataService,
                pipelineTopologyService,
                mock(EquipmentService.class),
                mock(TaskService.class),
                mock(LogService.class),
                mock(ManoeuvreService.class),
                mock(VirtualExpertExportService.class));
    }

    private VirtualExpertToolProxyController newController(
            DataMonitoringService dataMonitoringService,
            MonitoringDataService monitoringDataService,
            PipelineTopologyService pipelineTopologyService,
            EquipmentService equipmentService,
            TaskService taskService,
            LogService logService,
            ManoeuvreService manoeuvreService,
            VirtualExpertExportService exportService) {
        return new VirtualExpertToolProxyController(
                new VirtualExpertToolService(
                        dataMonitoringService,
                        monitoringDataService,
                        pipelineTopologyService,
                        equipmentService,
                        taskService,
                        logService,
                        manoeuvreService,
                        exportService));
    }
}
