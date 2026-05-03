package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.dataManagement.equipment.EquipmentQueryRequest;
import com.gxa.pipe.dataManagement.equipment.EquipmentResponse;
import com.gxa.pipe.dataManagement.equipment.EquipmentService;
import com.gxa.pipe.common.area.AreaResponse;
import com.gxa.pipe.common.area.AreaService;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataIndicatorResponse;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataQueryRequest;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataQueryResponse;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataService;
import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailRequest;
import com.gxa.pipe.dataVsualization.dataMonitoring.AreaDetailResponse;
import com.gxa.pipe.dataManagement.task.TaskQueryRequest;
import com.gxa.pipe.dataManagement.task.TaskResponse;
import com.gxa.pipe.dataManagement.task.TaskService;
import com.gxa.pipe.dataVsualization.dataMonitoring.DataMonitoringService;
import com.gxa.pipe.dataVsualization.dataMonitoring.PipeIndicatorResponse;
import com.gxa.pipe.log.LogQueryRequest;
import com.gxa.pipe.log.LogQueryResponse;
import com.gxa.pipe.log.LogService;
import com.gxa.pipe.manoeuvre.ManoeuvreQueryRequest;
import com.gxa.pipe.manoeuvre.ManoeuvreQueryResponse;
import com.gxa.pipe.manoeuvre.ManoeuvreService;
import com.gxa.pipe.pipelineTopology.MonitoringFilterOptionsResponse;
import com.gxa.pipe.pipelineTopology.MonitoringPipeOption;
import com.gxa.pipe.pipelineTopology.MonitoringSegmentOption;
import com.gxa.pipe.pipelineTopology.PipelineTopologyService;
import com.gxa.pipe.utils.PageResult;
import com.gxa.pipe.virtualExpert.agent.dto.ToolQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VirtualExpertToolService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataMonitoringService dataMonitoringService;
    private final MonitoringDataService monitoringDataService;
    private final PipelineTopologyService pipelineTopologyService;
    private final EquipmentService equipmentService;
    private final TaskService taskService;
    private final LogService logService;
    private final ManoeuvreService manoeuvreService;
    private final VirtualExpertExportService exportService;
    private final AreaService areaService;

    public ToolQueryResponse queryMonitoringTrend(String objectId, String metric) {
        String resolvedMetric = valueOrDefault(metric, "overview");
        String segmentId = extractNumericId(objectId);
        PipeIndicatorResponse indicators = dataMonitoringService.getPipeKeyIndicators(null, segmentId, null);

        return response(
                resolvedMetric,
                "24h",
                buildIndicatorPoints(indicators),
                "Monitoring indicators read-only query completed.",
                Map.of("object_id", valueOrDefault(objectId, "unknown"), "segment_id", valueOrDefault(segmentId, "unknown")),
                List.of(indicatorRecord(indicators))
        );
    }

    public ToolQueryResponse queryPipeSegmentContext(String objectId) {
        String segmentId = extractNumericId(objectId);
        MonitoringFilterOptionsResponse options = monitoringOptions(null, null, null);
        List<Map<String, Object>> records = options.getPipes().stream()
                .flatMap(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).stream()
                        .filter(segment -> segmentId == null || Objects.equals(String.valueOf(segment.getId()), segmentId))
                        .map(segment -> segmentRecord(pipe, segment)))
                .toList();

        return response(
                "pipe_segment_context",
                "current",
                List.of(),
                "Pipe segment context read-only query completed.",
                Map.of(
                        "object_id", valueOrDefault(objectId, "unknown"),
                        "scope_level", valueOrDefault(options.getScopeLevel(), "all")
                ),
                records
        );
    }

    public ToolQueryResponse queryEquipmentStatus(String objectId) {
        String segmentId = extractNumericId(objectId);
        EquipmentQueryRequest request = new EquipmentQueryRequest();
        request.setPipeSegmentId(segmentId);
        request.setPage("1");
        request.setPageSize("10");
        List<Map<String, Object>> records = equipmentService.findEquipmentByConditions(request).stream()
                .map(this::equipmentRecord)
                .toList();

        return response(
                "equipment_status",
                "current",
                List.of(),
                "Equipment status read-only query completed.",
                Map.of("object_id", valueOrDefault(objectId, "unknown"), "segment_id", valueOrDefault(segmentId, "unknown")),
                records
        );
    }

    public ToolQueryResponse queryRelatedTasks(String objectId) {
        Long segmentId = parseLongOrNull(extractNumericId(objectId));
        TaskQueryRequest request = new TaskQueryRequest();
        request.setPipeSegmentId(segmentId);
        request.setPage(1);
        request.setPageSize(10);
        PageResult<TaskResponse> page = taskService.getByPage(request);
        List<Map<String, Object>> records = Optional.ofNullable(page.getRecords()).orElse(List.of()).stream()
                .map(this::taskRecord)
                .toList();

        return response(
                "related_tasks",
                "7d",
                List.of(),
                "Related tasks read-only query completed.",
                Map.of(
                        "object_id", valueOrDefault(objectId, "unknown"),
                        "segment_id", segmentId == null ? "unknown" : segmentId,
                        "total", Optional.ofNullable(page.getTotal()).orElse(0L)
                ),
                records
        );
    }

    public ToolQueryResponse searchSimilarCases(String objectId) {
        return response(
                "similar_cases",
                "all",
                List.of(),
                "Similar cases read-only query completed.",
                Map.of("object_id", valueOrDefault(objectId, "unknown")),
                List.of()
        );
    }

    public ToolQueryResponse resolveOperationalScope(String rawInput, String scopeHint, String objectId) {
        MonitoringFilterOptionsResponse options = monitoringOptions(null, null, null);
        List<Map<String, Object>> records = scopeCandidateRecords(options);

        return response(
                "scope_candidates",
                "current",
                List.of(),
                "Operational scope candidates read-only query completed.",
                Map.of(
                        "raw_input", valueOrDefault(rawInput, "unknown"),
                        "scope_hint", valueOrDefault(scopeHint, "unknown"),
                        "object_id", valueOrDefault(objectId, "unknown")
                ),
                records,
                Map.of("source", "server-web", "capability_status", "candidate_lookup")
        );
    }

    public ToolQueryResponse resolveAreaScope(String rawInput, String province, String city, String district) {
        AreaScope scope = resolveArea(rawInput, province, city, district);
        List<Map<String, Object>> records = scope == null ? List.of() : List.of(areaScopeRecord(scope));
        Map<String, Object> context = scope == null
                ? mapOf("raw_input", valueOrDefault(rawInput, "unknown"), "resolved", false)
                : mapOf(
                        "resolved", true,
                        "resolved_scope_id", scope.areaId(),
                        "resolved_scope_type", scope.scopeType(),
                        "province_code", scope.provinceCode(),
                        "city_code", scope.cityCode(),
                        "district_code", scope.districtCode()
                );
        return response(
                "area_scope",
                "current",
                List.of(),
                scope == null ? "未定位到匹配区域。" : "已定位到" + scope.displayName(),
                context,
                records,
                Map.of("source", "server-web", "capability_status", "area_scope_resolved")
        );
    }

    public ToolQueryResponse queryAreaDataCatalog(String areaId, String scopeType, String includeSamples) {
        String resolvedAreaId = valueOrDefault(areaId, "");
        MonitoringFilterOptionsResponse options = monitoringOptionsForArea(resolvedAreaId, scopeType);
        List<Map<String, Object>> candidates = scopeCandidateRecords(options);
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(mapOf(
                "record_type", "data_catalog",
                "area_id", valueOrDefault(resolvedAreaId, "unknown"),
                "scope_type", valueOrDefault(scopeType, "area"),
                "pipeline_count", options.getPipes().size(),
                "segment_count", options.getPipes().stream().mapToInt(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).size()).sum(),
                "available_data", List.of("monitoring", "alarm", "inspection", "equipment", "task", "maintenance", "operation_log"),
                "metrics", List.of("pressure", "flow", "temperature", "vibration")
        ));
        if (!"false".equalsIgnoreCase(valueOrDefault(includeSamples, "true"))) {
            records.addAll(candidates.stream().limit(10).toList());
        }
        return response(
                "area_data_catalog",
                "current",
                List.of(),
                "区域可查数据目录已生成。",
                mapOf("area_id", valueOrDefault(resolvedAreaId, "unknown"), "scope_level", valueOrDefault(options.getScopeLevel(), "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "area_data_catalog")
        );
    }

    public ToolQueryResponse queryAreaOperationalOverview(String areaId, String scopeType, String timeRange) {
        String resolvedAreaId = valueOrDefault(areaId, "");
        AreaDetailRequest request = new AreaDetailRequest();
        request.setAreaId(parseLongOrNull(extractNumericId(resolvedAreaId)));
        TimeRange range = parseTimeRange(timeRange);
        request.setStartTime(epochMillis(range.start()));
        request.setEndTime(epochMillis(range.end()));
        List<AreaDetailResponse> areaDetails = Optional.ofNullable(dataMonitoringService.getAreaDetails(request)).orElse(List.of());
        MonitoringFilterOptionsResponse options = monitoringOptionsForArea(resolvedAreaId, scopeType);
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(mapOf(
                "record_type", "area_overview",
                "area_id", valueOrDefault(resolvedAreaId, "unknown"),
                "scope_type", valueOrDefault(scopeType, "area"),
                "sample_count", areaDetails.size(),
                "pipeline_count", options.getPipes().size(),
                "segment_count", options.getPipes().stream().mapToInt(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).size()).sum()
        ));
        records.addAll(areaDetails.stream().limit(12).map(this::areaDetailRecord).toList());
        records.addAll(scopeCandidateRecords(options).stream().limit(5).toList());
        return response(
                "area_operational_overview",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "区域运行总览查询完成。",
                mapOf("area_id", valueOrDefault(resolvedAreaId, "unknown"), "scope_type", valueOrDefault(scopeType, "area")),
                records,
                Map.of("source", "server-web", "capability_status", "area_operational_overview")
        );
    }

    public ToolQueryResponse queryMonitoringAggregate(
            String scopeType,
            String scopeId,
            String metric,
            String startTime,
            String endTime,
            String aggregation) {
        String resolvedMetric = valueOrDefault(metric, "overview");
        MonitoringDataQueryRequest request = monitoringRequest(scopeType, scopeId, startTime, endTime);
        Map<String, Object> aggregate = monitoringDataService.aggregateByConditions(request);
        Object sampleCount = aggregateValue(aggregate, "sample_count", 0L);
        Object value = aggregateMetricValue(aggregate, resolvedMetric, valueOrDefault(aggregation, "avg"));
        List<Map<String, Object>> records = List.of(mapOf(
                "scope_type", valueOrDefault(scopeType, "unknown"),
                "scope_id", valueOrDefault(scopeId, "unknown"),
                "metric", resolvedMetric,
                "aggregation", valueOrDefault(aggregation, "avg"),
                "value", value,
                "avg", aggregateMetricValue(aggregate, resolvedMetric, "avg"),
                "min", aggregateMetricValue(aggregate, resolvedMetric, "min"),
                "max", aggregateMetricValue(aggregate, resolvedMetric, "max"),
                "sample_count", sampleCount,
                "danger_count", aggregateValue(aggregate, "danger_count", 0L),
                "critical_count", aggregateValue(aggregate, "critical_count", 0L)
        ));

        return response(
                resolvedMetric,
                valueOrDefault(startTime, "current") + "/" + valueOrDefault(endTime, "current"),
                List.of(),
                "Monitoring aggregate read-only query completed.",
                mapOf(
                        "scope_type", valueOrDefault(scopeType, "unknown"),
                        "scope_id", valueOrDefault(scopeId, "unknown"),
                        "start_time", valueOrDefault(startTime, "unknown"),
                        "end_time", valueOrDefault(endTime, "unknown")
                ),
                records,
                Map.of("source", "server-web", "capability_status", "historical_aggregate")
        );
    }

    public ToolQueryResponse queryMonitoringHistory(
            String objectType,
            String objectId,
            String metric,
            String startTime,
            String endTime,
            String granularity) {
        String resolvedMetric = valueOrDefault(metric, "overview");
        MonitoringDataQueryRequest request = monitoringRequest(objectType, objectId, startTime, endTime);
        request.setPage(1);
        request.setPageSize(50);
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        List<Map<String, Object>> records = monitoringRecords(page.getRecords());

        return response(
                resolvedMetric,
                valueOrDefault(startTime, "current") + "/" + valueOrDefault(endTime, "current"),
                monitoringPoints(records, resolvedMetric),
                "Monitoring history read-only query completed.",
                mapOf(
                        "object_type", valueOrDefault(objectType, "unknown"),
                        "object_id", valueOrDefault(objectId, "unknown"),
                        "granularity", valueOrDefault(granularity, "unknown"),
                        "total", Optional.ofNullable(page.getTotal()).orElse(0L)
                ),
                records,
                Map.of("source", "server-web", "capability_status", "historical_records")
        );
    }

    public ToolQueryResponse queryAreaMonitoringSummary(String areaId, String metric, String window, String aggregation) {
        MonitoringFilterOptionsResponse options = monitoringOptions(areaId, null, null);
        MonitoringDataIndicatorResponse indicators = Optional.ofNullable(monitoringDataService.getIndicatorCard(areaId))
                .orElseGet(MonitoringDataIndicatorResponse::new);
        List<Map<String, Object>> records = new ArrayList<>(options.getPipes().stream()
                .flatMap(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).stream().map(segment -> segmentRecord(pipe, segment)))
                .limit(20)
                .toList());
        records.add(0, mapOf(
                "record_type", "indicator_summary",
                "total_records", indicators.getTotalRecords(),
                "safe_count", indicators.getSafeCount(),
                "good_count", indicators.getGoodCount(),
                "danger_count", indicators.getDangerCount(),
                "critical_count", indicators.getCriticalCount(),
                "today", indicators.getToday()
        ));

        return response(
                valueOrDefault(metric, "overview"),
                valueOrDefault(window, "current"),
                List.of(),
                "Area monitoring summary read-only query completed.",
                Map.of(
                        "area_id", valueOrDefault(areaId, "unknown"),
                        "aggregation", valueOrDefault(aggregation, "summary"),
                        "scope_level", valueOrDefault(options.getScopeLevel(), "all")
                ),
                records,
                Map.of("source", "server-web", "capability_status", "area_monitoring_summary")
        );
    }

    public ToolQueryResponse queryAreaAlarmSummary(String areaId, String scopeType, String timeRange) {
        MonitoringDataQueryRequest request = monitoringRequest(valueOrDefault(scopeType, "area"), areaId, null, null);
        applyTimeRange(request, timeRange);
        request.setPage(1);
        request.setPageSize(100);
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        List<Map<String, Object>> records = monitoringRecords(page.getRecords());
        long warning = records.stream().filter(record -> Objects.equals(String.valueOf(record.get("data_status")), "2")).count();
        long critical = records.stream().filter(record -> Objects.equals(String.valueOf(record.get("data_status")), "3")).count();
        Map<String, Object> summary = mapOf(
                "record_type", "area_alarm_summary",
                "area_id", valueOrDefault(areaId, "unknown"),
                "warning_count", warning,
                "critical_count", critical,
                "unclosed_count", warning + critical,
                "total", Optional.ofNullable(page.getTotal()).orElse(0L)
        );
        List<Map<String, Object>> output = new ArrayList<>();
        output.add(summary);
        output.addAll(records.stream().filter(this::isAnomalyRecord).limit(10).toList());
        return response(
                "area_alarm_summary",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Area alarm summary read-only query completed.",
                mapOf("area_id", valueOrDefault(areaId, "unknown"), "scope_type", valueOrDefault(scopeType, "area")),
                output,
                Map.of("source", "server-web", "capability_status", "area_alarm_summary")
        );
    }

    public ToolQueryResponse findRecentAnomalies(String scope, String timeRange, String limit) {
        MonitoringDataQueryRequest request = monitoringRequest(scopeTypeFromScope(scope), scope, null, null);
        applyTimeRange(request, timeRange);
        request.setPage(1);
        request.setPageSize((int) parseLongOrDefault(valueOrDefault(limit, "20"), 20));
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        List<Map<String, Object>> records = monitoringRecords(page.getRecords()).stream()
                .filter(this::isAnomalyRecord)
                .limit(parseLongOrDefault(valueOrDefault(limit, "10"), 10))
                .toList();
        return response(
                "recent_anomalies",
                valueOrDefault(timeRange, "24h"),
                List.of(),
                "Recent anomalies read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "total", records.size()),
                records,
                Map.of("source", "server-web", "capability_status", "recent_anomalies")
        );
    }

    public ToolQueryResponse findTopRiskSegments(String scope, String timeRange, String limit) {
        MonitoringDataQueryRequest request = monitoringRequest(scopeTypeFromScope(scope), scope, null, null);
        applyTimeRange(request, timeRange);
        request.setPage(1);
        request.setPageSize(100);
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> record : monitoringRecords(page.getRecords())) {
            String segmentId = valueOrDefault(String.valueOf(record.get("pipe_segment_id")), "unknown");
            Map<String, Object> risk = grouped.computeIfAbsent(segmentId, key -> mapOf(
                    "record_type", "risk_segment",
                    "pipe_segment_id", key,
                    "pipe_segment_name", record.get("pipe_segment_name"),
                    "pipeline_name", record.get("pipeline_name"),
                    "sample_count", 0,
                    "warning_count", 0,
                    "critical_count", 0,
                    "risk_score", 0
            ));
            int sampleCount = ((Number) risk.get("sample_count")).intValue() + 1;
            int warningCount = ((Number) risk.get("warning_count")).intValue();
            int criticalCount = ((Number) risk.get("critical_count")).intValue();
            String status = String.valueOf(record.get("data_status"));
            if ("2".equals(status)) {
                warningCount += 1;
            }
            if ("3".equals(status)) {
                criticalCount += 1;
            }
            risk.put("sample_count", sampleCount);
            risk.put("warning_count", warningCount);
            risk.put("critical_count", criticalCount);
            risk.put("risk_score", criticalCount * 3 + warningCount);
        }
        List<Map<String, Object>> records = grouped.values().stream()
                .sorted(Comparator.comparingInt(record -> -((Number) record.get("risk_score")).intValue()))
                .limit(parseLongOrDefault(valueOrDefault(limit, "10"), 10))
                .toList();
        return response(
                "top_risk_segments",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Top risk segments read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "top_risk_segments")
        );
    }

    public ToolQueryResponse queryAlarmEvents(String scope, String timeRange, String severity) {
        MonitoringDataQueryRequest request = monitoringRequest(scopeTypeFromScope(scope), scope, null, null);
        applyTimeRange(request, timeRange);
        request.setDataStatus(valueOrDefault(dataStatusFromSeverity(severity), "3"));
        request.setPage(1);
        request.setPageSize(20);
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        List<Map<String, Object>> records = monitoringRecords(page.getRecords());
        return response(
                "alarm_events",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Alarm events read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "severity", valueOrDefault(severity, "unknown"), "total", Optional.ofNullable(page.getTotal()).orElse(0L)),
                records,
                Map.of("source", "server-web", "capability_status", "monitoring_alarm_records")
        );
    }

    public ToolQueryResponse findUnclosedAlarms(String scope, String timeRange, String severity) {
        ToolQueryResponse alarms = queryAlarmEvents(scope, timeRange, severity);
        List<Map<String, Object>> records = alarms.records().stream()
                .filter(this::isAnomalyRecord)
                .map(record -> {
                    Map<String, Object> next = new LinkedHashMap<>(record);
                    next.put("record_type", "unclosed_alarm");
                    next.put("closure_status", "unclosed");
                    return next;
                })
                .toList();
        return response(
                "unclosed_alarms",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Unclosed alarms read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "severity", valueOrDefault(severity, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "unclosed_alarms")
        );
    }

    public ToolQueryResponse findStaleInspections(String scope, String staleDays, String limit) {
        EquipmentQueryRequest request = new EquipmentQueryRequest();
        request.setPipeSegmentId(extractNumericId(scope));
        request.setPage("1");
        request.setPageSize(String.valueOf(parseLongOrDefault(valueOrDefault(limit, "20"), 20)));
        List<Map<String, Object>> records = Optional.ofNullable(equipmentService.findEquipmentByConditions(request)).orElse(List.of()).stream()
                .map(equipment -> {
                    Map<String, Object> record = new LinkedHashMap<>(equipmentRecord(equipment));
                    record.put("record_type", "stale_inspection_candidate");
                    record.put("last_check", equipment.getLastCheck());
                    record.put("stale_days_threshold", valueOrDefault(staleDays, "30"));
                    return record;
                })
                .limit(parseLongOrDefault(valueOrDefault(limit, "10"), 10))
                .toList();
        return response(
                "stale_inspections",
                "current",
                List.of(),
                "Stale inspections read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "stale_days", valueOrDefault(staleDays, "30")),
                records,
                Map.of("source", "server-web", "capability_status", "stale_inspection_candidates")
        );
    }

    public ToolQueryResponse queryAreaTaskSummary(String areaId, String timeRange) {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setAreaId(parseLongOrNull(extractNumericId(areaId)));
        request.setPage(1);
        request.setPageSize(100);
        applyTaskTimeRange(request, timeRange);
        PageResult<TaskResponse> page = taskService.getByPage(request);
        List<Map<String, Object>> tasks = Optional.ofNullable(page.getRecords()).orElse(List.of()).stream().map(this::taskRecord).toList();
        long pending = tasks.stream().filter(task -> String.valueOf(task.get("status")).contains("待")).count();
        long running = tasks.stream().filter(task -> String.valueOf(task.get("status")).contains("进行")).count();
        long completed = tasks.stream().filter(task -> String.valueOf(task.get("status")).contains("完成")).count();
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(mapOf(
                "record_type", "area_task_summary",
                "area_id", valueOrDefault(areaId, "unknown"),
                "pending_count", pending,
                "running_count", running,
                "completed_count", completed,
                "total", Optional.ofNullable(page.getTotal()).orElse(0L)
        ));
        records.addAll(tasks.stream().limit(20).toList());
        return response(
                "area_task_summary",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Area task summary read-only query completed.",
                mapOf("area_id", valueOrDefault(areaId, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "area_task_summary")
        );
    }

    public ToolQueryResponse traceOperationChain(String scope, String timeRange, String incidentId) {
        List<Map<String, Object>> records = new ArrayList<>();
        records.addAll(queryAlarmEvents(scope, timeRange, null).records().stream().limit(5).map(record -> chainRecord("alarm", record)).toList());
        records.addAll(queryRelatedTasks(scope).records().stream().limit(5).map(record -> chainRecord("task", record)).toList());
        records.addAll(queryOperationLogs(null, scope, timeRange, null).records().stream().limit(5).map(record -> chainRecord("operation_log", record)).toList());
        return response(
                "operation_chain",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Operation chain read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "incident_id", valueOrDefault(incidentId, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "operation_chain")
        );
    }

    public ToolQueryResponse queryInspectionHistory(String scope, String timeRange) {
        MonitoringDataQueryRequest request = monitoringRequest(scopeTypeFromScope(scope), scope, null, null);
        applyTimeRange(request, timeRange);
        request.setPage(1);
        request.setPageSize(20);
        PageResult<MonitoringDataQueryResponse> page = monitoringDataService.getByPage(request);
        List<Map<String, Object>> records = monitoringRecords(page.getRecords());
        return response(
                "inspection_history",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Inspection history read-only query completed.",
                mapOf("scope", valueOrDefault(scope, "unknown"), "total", Optional.ofNullable(page.getTotal()).orElse(0L)),
                records,
                Map.of("source", "server-web", "capability_status", "monitoring_history_records")
        );
    }

    public ToolQueryResponse resolveAssetScope(String rawInput, String assetHint, String objectType) {
        String hint = firstNonBlank(assetHint, rawInput, "");
        MonitoringFilterOptionsResponse options = monitoringOptions(null, null, null);
        List<Map<String, Object>> records = scopeCandidateRecords(options).stream()
                .filter(record -> matchesHint(record.get("scope_name"), hint) || matchesHint(record.get("scope_id"), hint))
                .limit(10)
                .toList();
        if (records.isEmpty()) {
            records = scopeCandidateRecords(options).stream().limit(10).toList();
        }
        return response(
                "asset_scope",
                "current",
                List.of(),
                "Asset scope candidates read-only query completed.",
                mapOf(
                        "raw_input", valueOrDefault(rawInput, "unknown"),
                        "asset_hint", valueOrDefault(assetHint, "unknown"),
                        "object_type", valueOrDefault(objectType, "unknown")
                ),
                records,
                Map.of("source", "server-web", "capability_status", "asset_scope_candidates")
        );
    }

    public ToolQueryResponse queryAreaAssetSummary(String areaId, String scopeType) {
        MonitoringFilterOptionsResponse options = monitoringOptionsForArea(valueOrDefault(areaId, ""), scopeType);
        EquipmentQueryRequest equipmentRequest = new EquipmentQueryRequest();
        equipmentRequest.setPage("1");
        equipmentRequest.setPageSize("100");
        List<EquipmentResponse> equipment = Optional.ofNullable(equipmentService.findEquipmentByConditions(equipmentRequest)).orElse(List.of());
        long abnormalEquipment = equipment.stream()
                .filter(item -> !valueOrDefault(item.getStatus(), "正常").contains("正常"))
                .count();
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(mapOf(
                "record_type", "area_asset_summary",
                "area_id", valueOrDefault(areaId, "unknown"),
                "scope_type", valueOrDefault(scopeType, "area"),
                "pipeline_count", options.getPipes().size(),
                "segment_count", options.getPipes().stream().mapToInt(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).size()).sum(),
                "equipment_count", equipment.size(),
                "abnormal_equipment_count", abnormalEquipment
        ));
        records.addAll(scopeCandidateRecords(options).stream().limit(10).toList());
        records.addAll(equipment.stream().limit(10).map(this::equipmentRecord).toList());
        return response(
                "area_asset_summary",
                "current",
                List.of(),
                "Area asset summary read-only query completed.",
                mapOf("area_id", valueOrDefault(areaId, "unknown"), "scope_level", valueOrDefault(options.getScopeLevel(), "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "area_asset_summary")
        );
    }

    public ToolQueryResponse queryAssetRelationships(String objectType, String objectId, String includeEquipment) {
        MonitoringFilterOptionsResponse options = monitoringOptions(null, null, null);
        String numericId = extractNumericId(objectId);
        List<Map<String, Object>> records = new ArrayList<>();
        for (MonitoringPipeOption pipe : options.getPipes()) {
            boolean pipeMatched = numericId == null || Objects.equals(String.valueOf(pipe.getId()), numericId);
            for (MonitoringSegmentOption segment : Optional.ofNullable(pipe.getSegments()).orElse(List.of())) {
                boolean segmentMatched = numericId == null || Objects.equals(String.valueOf(segment.getId()), numericId);
                if (pipeMatched || segmentMatched) {
                    Map<String, Object> relationship = new LinkedHashMap<>(segmentRecord(pipe, segment));
                    relationship.put("record_type", "asset_relationship");
                    relationship.put("relationship", "pipeline_segment");
                    records.add(relationship);
                }
            }
        }
        if (!"false".equalsIgnoreCase(valueOrDefault(includeEquipment, "true"))) {
            EquipmentQueryRequest request = new EquipmentQueryRequest();
            request.setPipeSegmentId(numericId);
            request.setPage("1");
            request.setPageSize("20");
            records.addAll(Optional.ofNullable(equipmentService.findEquipmentByConditions(request)).orElse(List.of()).stream().map(equipment -> {
                Map<String, Object> record = new LinkedHashMap<>(equipmentRecord(equipment));
                record.put("record_type", "asset_relationship");
                record.put("relationship", "segment_equipment");
                return record;
            }).toList());
        }
        return response(
                "asset_relationships",
                "current",
                List.of(),
                "Asset relationships read-only query completed.",
                mapOf("object_type", valueOrDefault(objectType, "unknown"), "object_id", valueOrDefault(objectId, "unknown")),
                records.stream().limit(30).toList(),
                Map.of("source", "server-web", "capability_status", "asset_relationships")
        );
    }

    public ToolQueryResponse queryMaintenanceHistory(String equipmentId, String scope, String timeRange) {
        List<EquipmentResponse> equipment = queryEquipmentForMaintenance(equipmentId, scope, timeRange);
        List<Map<String, Object>> records = new ArrayList<>(equipment.stream().map(this::equipmentRecord).toList());
        for (EquipmentResponse item : equipment.stream().limit(5).toList()) {
            Long segmentId = parseLongOrNull(extractNumericId(item.getPipeSegmentId()));
            if (segmentId == null) {
                continue;
            }
            TaskQueryRequest taskRequest = new TaskQueryRequest();
            taskRequest.setPipeSegmentId(segmentId);
            taskRequest.setPage(1);
            taskRequest.setPageSize(10);
            applyTaskTimeRange(taskRequest, timeRange);
            records.addAll(Optional.ofNullable(taskService.getByPage(taskRequest).getRecords()).orElse(List.of()).stream()
                    .map(task -> {
                        Map<String, Object> record = new LinkedHashMap<>(taskRecord(task));
                        record.put("record_type", "maintenance_task");
                        return record;
                    })
                    .toList());
        }
        return response(
                "maintenance_history",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Maintenance history read-only query completed.",
                mapOf("equipment_id", valueOrDefault(equipmentId, "unknown"), "scope", valueOrDefault(scope, "unknown"), "equipment_count", equipment.size()),
                records,
                Map.of("source", "server-web", "capability_status", "equipment_and_task_records")
        );
    }

    public ToolQueryResponse queryTopologyImpact(String objectType, String objectId) {
        String segmentId = extractNumericId(objectId);
        MonitoringFilterOptionsResponse options = monitoringOptions(null, null, null);
        List<Map<String, Object>> records = options.getPipes().stream()
                .flatMap(pipe -> Optional.ofNullable(pipe.getSegments()).orElse(List.of()).stream()
                        .filter(segment -> segmentId == null || Objects.equals(String.valueOf(segment.getId()), segmentId))
                        .map(segment -> segmentRecord(pipe, segment)))
                .limit(10)
                .toList();

        return response(
                "topology_impact",
                "current",
                List.of(),
                "Topology impact read-only query completed.",
                Map.of("object_type", valueOrDefault(objectType, "unknown"), "object_id", valueOrDefault(objectId, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "topology_context")
        );
    }

    public ToolQueryResponse queryEmergencyPlan(String incidentType, String scope, String severity) {
        ManoeuvreQueryRequest request = new ManoeuvreQueryRequest();
        request.setAreaId(parseLongOrNull(extractNumericId(scope)));
        request.setType(2L);
        request.setPage(1);
        request.setPageSize(10);
        List<Map<String, Object>> records = manoeuvreService.findManoeuvreByParams(request).stream()
                .map(this::manoeuvreRecord)
                .toList();
        return response(
                "emergency_plan",
                "current",
                List.of(),
                "Emergency plan read-only query completed.",
                mapOf(
                        "incident_type", valueOrDefault(incidentType, "unknown"),
                        "scope", valueOrDefault(scope, "unknown"),
                        "severity", valueOrDefault(severity, "unknown")
                ),
                records,
                Map.of("source", "server-web", "capability_status", "emergency_manoeuvre_records")
        );
    }

    public ToolQueryResponse queryCaseLibrary(String scenario, String metric, String scope, String limit) {
        List<Map<String, Object>> records = List.of(
                mapOf(
                        "record_type", "case",
                        "title", "压力异常先复核传感器再判断停输",
                        "scenario", "pressure_anomaly",
                        "trigger", "压力高危或连续波动",
                        "action", "复核传感器、检查上游阀室、确认现场压力表",
                        "similarity", matchesHint(scenario, "压力") || matchesHint(metric, "pressure") ? 0.92 : 0.72
                ),
                mapOf(
                        "record_type", "case",
                        "title", "未闭环告警复盘要求同步任务责任人",
                        "scenario", "unclosed_alarm",
                        "trigger", "告警恢复但工单未关闭",
                        "action", "核对工单状态、通知责任班组、补充复盘记录",
                        "similarity", matchesHint(scenario, "告警") ? 0.9 : 0.7
                )
        );
        return response(
                "case_library",
                "all",
                List.of(),
                "Case library read-only query completed.",
                mapOf("scenario", valueOrDefault(scenario, "unknown"), "metric", valueOrDefault(metric, "unknown"), "scope", valueOrDefault(scope, "unknown")),
                records.stream().limit(parseLongOrDefault(valueOrDefault(limit, "5"), 5)).toList(),
                Map.of("source", "server-web", "capability_status", "case_library_seed")
        );
    }

    public ToolQueryResponse querySopByScenario(String scenario, String severity) {
        String normalizedScenario = valueOrDefault(scenario, "通用异常");
        List<Map<String, Object>> records = List.of(
                mapOf("record_type", "sop_step", "order", 1, "scenario", normalizedScenario, "action", "确认监测值是否连续异常，排除单点采样或传感器离线。"),
                mapOf("record_type", "sop_step", "order", 2, "scenario", normalizedScenario, "action", "核对上下游管段、设备状态、近期巡检和未闭环任务。"),
                mapOf("record_type", "sop_step", "order", 3, "scenario", normalizedScenario, "action", "高危时通知现场复核并准备隔离、降压或应急预案。")
        );
        return response(
                "sop_by_scenario",
                "current",
                List.of(),
                "SOP by scenario read-only query completed.",
                mapOf("scenario", normalizedScenario, "severity", valueOrDefault(severity, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "sop_seed")
        );
    }

    public ToolQueryResponse queryOperationLogs(String user, String object, String timeRange, String operation) {
        LogQueryRequest request = new LogQueryRequest();
        request.setPage(1);
        request.setPageSize(20);
        applyLogTimeRange(request, timeRange);
        PageResult<LogQueryResponse> page = logService.queryLogs(request);
        List<Map<String, Object>> records = Optional.ofNullable(page.getRecords()).orElse(List.of()).stream()
                .map(this::logRecord)
                .toList();
        return response(
                "operation_logs",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Operation logs read-only query completed.",
                mapOf(
                        "user", valueOrDefault(user, "unknown"),
                        "object", valueOrDefault(object, "unknown"),
                        "operation", valueOrDefault(operation, "unknown"),
                        "total", Optional.ofNullable(page.getTotal()).orElse(0L)
                ),
                records,
                Map.of("source", "server-web", "capability_status", "operation_log_records")
        );
    }

    public ToolQueryResponse queryReportInventory(String sessionId, String timeRange, String format) {
        List<Map<String, Object>> records = exportService.listExports(format, 20).stream()
                .map(this::exportRecord)
                .toList();
        return response(
                "report_inventory",
                valueOrDefault(timeRange, "current"),
                List.of(),
                "Report inventory read-only query completed.",
                Map.of("session_id", valueOrDefault(sessionId, "unknown"), "format", valueOrDefault(format, "unknown")),
                records,
                Map.of("source", "server-web", "capability_status", "export_inventory")
        );
    }

    public ToolQueryResponse createExport(Map<String, Object> request) {
        Map<String, Object> exportRequest = new LinkedHashMap<>(request);
        if (!exportRequest.containsKey("sessionId")) {
            exportRequest.put("sessionId", request.get("session_id"));
        }
        if (!exportRequest.containsKey("runId")) {
            exportRequest.put("runId", request.get("run_id"));
        }
        if (!exportRequest.containsKey("exportPlan")) {
            exportRequest.put("exportPlan", request.get("export_plan"));
        }
        ExportFile file = exportService.createExport(exportRequest);
        Map<String, Object> context = mapOf(
                "exportId", file.exportId(),
                "export_id", file.exportId(),
                "fileName", file.fileName(),
                "file_name", file.fileName(),
                "contentType", file.contentType(),
                "content_type", file.contentType(),
                "size", file.size(),
                "downloadUrl", "/manager/virtual-expert/agent/exports/" + file.exportId() + "/download",
                "download_url", "/manager/virtual-expert/agent/exports/" + file.exportId() + "/download"
        );
        return response(
                "export_report",
                "current",
                List.of(),
                "Export file generated.",
                context,
                List.of(exportRecord(file)),
                Map.of("source", "server-web", "capability_status", "export_created")
        );
    }

    private ToolQueryResponse response(
            String metric,
            String window,
            List<Map<String, Object>> points,
            String summary,
            Map<String, Object> context,
            List<Map<String, Object>> records) {
        return response(metric, window, points, summary, context, records, Map.of("source", "server-web"));
    }

    private ToolQueryResponse response(
            String metric,
            String window,
            List<Map<String, Object>> points,
            String summary,
            Map<String, Object> context,
            List<Map<String, Object>> records,
            Map<String, Object> metadata) {
        Map<String, Object> mergedMetadata = new HashMap<>(metadata);
        mergedMetadata.putIfAbsent("source", "server-web");
        return new ToolQueryResponse(
                metric,
                window,
                points,
                summary,
                context,
                records,
                List.of(Map.of("metric", metric, "window", window, "context", context, "records", records)),
                0.9,
                mergedMetadata
        );
    }

    private MonitoringDataQueryRequest monitoringRequest(String scopeType, String scopeId, String startTime, String endTime) {
        MonitoringDataQueryRequest request = new MonitoringDataQueryRequest();
        String normalizedScopeType = valueOrDefault(scopeType, "").toLowerCase();
        String numericId = extractNumericId(scopeId);
        if (StringUtils.hasText(numericId)) {
            switch (normalizedScopeType) {
                case "province", "city", "district", "area" -> request.setAreaId(numericId);
                case "pipeline", "pipe" -> request.setPipelineId(numericId);
                case "equipment", "sensor" -> request.setSensorId(numericId);
                default -> request.setPipeSegmentId(numericId);
            }
        }
        if (StringUtils.hasText(startTime)) {
            request.setMonitorStartTime(startTime);
        }
        if (StringUtils.hasText(endTime)) {
            request.setMonitorEndTime(endTime);
        }
        request.setPage(1);
        request.setPageSize(20);
        return request;
    }

    private MonitoringFilterOptionsResponse monitoringOptionsForArea(String areaId, String scopeType) {
        String normalizedScope = valueOrDefault(scopeType, "area").toLowerCase();
        return switch (normalizedScope) {
            case "province" -> monitoringOptions(areaId, null, null);
            case "city" -> monitoringOptions(null, areaId, null);
            case "district" -> monitoringOptions(null, null, areaId);
            default -> monitoringOptions(null, areaId, null);
        };
    }

    private AreaScope resolveArea(String rawInput, String province, String city, String district) {
        String searchable = String.join(" ", valueOrDefault(rawInput, ""), valueOrDefault(province, ""), valueOrDefault(city, ""), valueOrDefault(district, ""));
        AreaResponse matchedProvince = findArea(areaService.getProvinces(), firstNonBlank(province, searchable));
        if (matchedProvince == null) {
            return null;
        }
        AreaResponse matchedCity = findArea(areaService.getCitiesByProvinceCode(matchedProvince.getCode()), firstNonBlank(city, searchable));
        AreaResponse matchedDistrict = matchedCity == null
                ? null
                : findArea(areaService.getDistrictsByCityCode(matchedCity.getCode()), firstNonBlank(district, searchable));
        String scopeType = matchedDistrict != null ? "district" : matchedCity != null ? "city" : "province";
        String areaId = switch (scopeType) {
            case "district" -> matchedDistrict.getCode();
            case "city" -> matchedCity.getCode();
            default -> matchedProvince.getCode();
        };
        return new AreaScope(
                scopeType,
                areaId,
                matchedProvince.getCode(),
                matchedProvince.getName(),
                matchedCity == null ? "" : matchedCity.getCode(),
                matchedCity == null ? "" : matchedCity.getName(),
                matchedDistrict == null ? "" : matchedDistrict.getCode(),
                matchedDistrict == null ? "" : matchedDistrict.getName()
        );
    }

    private AreaResponse findArea(List<AreaResponse> areas, String hint) {
        String normalizedHint = normalizeName(hint);
        return Optional.ofNullable(areas).orElse(List.of()).stream()
                .filter(area -> {
                    String name = normalizeName(area.getName());
                    return !name.isBlank() && (normalizedHint.contains(name) || name.contains(normalizedHint));
                })
                .findFirst()
                .orElse(null);
    }

    private String normalizeName(String value) {
        return valueOrDefault(value, "")
                .replace("省", "")
                .replace("市", "")
                .replace("区", "")
                .replace("县", "")
                .replace(" ", "");
    }

    private Map<String, Object> areaScopeRecord(AreaScope scope) {
        return mapOf(
                "scope_type", scope.scopeType(),
                "area_id", scope.areaId(),
                "province_code", scope.provinceCode(),
                "province_name", scope.provinceName(),
                "city_code", scope.cityCode(),
                "city_name", scope.cityName(),
                "district_code", scope.districtCode(),
                "district_name", scope.districtName(),
                "scope_name", scope.displayName()
        );
    }

    private Map<String, Object> areaDetailRecord(AreaDetailResponse detail) {
        return mapOf(
                "record_type", "area_metric",
                "ave_flow", detail.getAveFlow(),
                "ave_pressure", detail.getAvePressure(),
                "ave_temperature", detail.getAveTemperature(),
                "ave_vibration", detail.getAveVibration(),
                "time", detail.getTime()
        );
    }

    private String scopeTypeFromScope(String scope) {
        String normalized = valueOrDefault(scope, "").toLowerCase();
        if (normalized.startsWith("area") || normalized.startsWith("province") || normalized.startsWith("city") || normalized.startsWith("district")) {
            return "area";
        }
        if (normalized.startsWith("pipeline") || normalized.startsWith("pipe:")) {
            return "pipeline";
        }
        if (normalized.startsWith("sensor") || normalized.startsWith("equipment")) {
            return "sensor";
        }
        return "segment";
    }

    private void applyTimeRange(MonitoringDataQueryRequest request, String timeRange) {
        TimeRange range = parseTimeRange(timeRange);
        if (range.start() != null) {
            request.setMonitorStartTime(range.start());
        }
        if (range.end() != null) {
            request.setMonitorEndTime(range.end());
        }
    }

    private void applyTaskTimeRange(TaskQueryRequest request, String timeRange) {
        TimeRange range = parseTimeRange(timeRange);
        request.setStartTime(range.start());
        request.setEndTime(range.end());
    }

    private void applyLogTimeRange(LogQueryRequest request, String timeRange) {
        TimeRange range = parseTimeRange(timeRange);
        request.setOperationTimeMin(epochMillis(range.start()));
        request.setOperationTimeMax(epochMillis(range.end()));
    }

    private TimeRange parseTimeRange(String timeRange) {
        if (!StringUtils.hasText(timeRange)) {
            return new TimeRange(null, null);
        }
        String value = timeRange.trim().toLowerCase();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = null;
        if (value.endsWith("h")) {
            start = end.minusHours(parseLongOrDefault(value.substring(0, value.length() - 1), 24));
        } else if (value.endsWith("d")) {
            start = end.minusDays(parseLongOrDefault(value.substring(0, value.length() - 1), 7));
        } else if (value.contains("/")) {
            String[] parts = value.split("/", 2);
            return new TimeRange(parts[0].isBlank() ? null : parts[0], parts.length < 2 || parts[1].isBlank() ? null : parts[1]);
        }
        return new TimeRange(start == null ? null : DATE_TIME.format(start), DATE_TIME.format(end));
    }

    private Long epochMillis(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (RuntimeException ignored) {
            try {
                return Instant.parse(value).toEpochMilli();
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }

    private long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Object aggregateMetricValue(Map<String, Object> aggregate, String metric, String aggregation) {
        String normalizedMetric = switch (valueOrDefault(metric, "temperature")) {
            case "pressure" -> "pressure";
            case "flow", "traffic" -> "flow";
            case "vibration", "shake" -> "vibration";
            default -> "temperature";
        };
        String normalizedAggregation = switch (valueOrDefault(aggregation, "avg")) {
            case "min" -> "min";
            case "max" -> "max";
            default -> "avg";
        };
        return aggregateValue(aggregate, normalizedAggregation + "_" + normalizedMetric, 0);
    }

    private Object aggregateValue(Map<String, Object> aggregate, String key, Object fallback) {
        Object value = aggregate.get(key);
        if (value == null) {
            value = aggregate.get(key.toUpperCase());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        return value == null ? fallback : value;
    }

    private List<Map<String, Object>> monitoringRecords(List<MonitoringDataQueryResponse> records) {
        return Optional.ofNullable(records).orElse(List.of()).stream()
                .map(this::monitoringRecord)
                .toList();
    }

    private Map<String, Object> monitoringRecord(MonitoringDataQueryResponse record) {
        return mapOf(
                "id", record.getId(),
                "pressure", record.getPressure(),
                "flow", record.getFlow(),
                "temperature", record.getTemperature(),
                "vibration", record.getVibration(),
                "data_status", record.getDataStatus(),
                "status_text", record.getStatusText(),
                "sensor_id", record.getSensorId(),
                "sensor_name", record.getSensorName(),
                "area_name", record.getAreaName(),
                "pipeline_name", record.getPipelineName(),
                "pipe_segment_id", record.getPipeSegmentId(),
                "pipe_segment_name", record.getPipeSegmentName(),
                "monitor_time", record.getMonitorTime()
        );
    }

    private List<Map<String, Object>> monitoringPoints(List<Map<String, Object>> records, String metric) {
        return records.stream()
                .map(record -> mapOf(
                        "metric", valueOrDefault(metric, "overview"),
                        "value", record.get(valueOrDefault(metric, "temperature")),
                        "time", record.get("monitor_time"),
                        "status", record.get("data_status")
                ))
                .toList();
    }

    private String dataStatusFromSeverity(String severity) {
        String normalized = valueOrDefault(severity, "").toLowerCase();
        if (List.of("critical", "high", "高危", "严重", "3").contains(normalized)) {
            return "3";
        }
        if (List.of("danger", "medium", "危险", "中风险", "2").contains(normalized)) {
            return "2";
        }
        return null;
    }

    private List<EquipmentResponse> queryEquipmentForMaintenance(String equipmentId, String scope, String timeRange) {
        if (StringUtils.hasText(extractNumericId(equipmentId))) {
            return Optional.ofNullable(equipmentService.findEquipmentById(extractNumericId(equipmentId))).orElse(List.of());
        }
        EquipmentQueryRequest request = new EquipmentQueryRequest();
        request.setPipeSegmentId(extractNumericId(scope));
        TimeRange range = parseTimeRange(timeRange);
        request.setLastCheckStart(range.start());
        request.setLastCheckEnd(range.end());
        request.setPage("1");
        request.setPageSize("20");
        return Optional.ofNullable(equipmentService.findEquipmentByConditions(request)).orElse(List.of());
    }

    private Map<String, Object> manoeuvreRecord(ManoeuvreQueryResponse manoeuvre) {
        return mapOf(
                "id", manoeuvre.getId(),
                "name", manoeuvre.getName(),
                "area_id", manoeuvre.getAreaId(),
                "area_name", manoeuvre.getAreaName(),
                "location", manoeuvre.getLocation(),
                "start_time", manoeuvre.getStartTime(),
                "end_time", manoeuvre.getEndTime(),
                "organizer", manoeuvre.getOrganizer(),
                "status", manoeuvre.getStatus(),
                "type", manoeuvre.getType(),
                "details", manoeuvre.getDetails(),
                "repairmans", manoeuvre.getRepairmans()
        );
    }

    private Map<String, Object> logRecord(LogQueryResponse log) {
        return mapOf(
                "id", log.getId(),
                "user_id", log.getUserId(),
                "username", log.getUsername(),
                "user_type", log.getType(),
                "user_type_text", log.getUserTypeText(),
                "operate", log.getOperate(),
                "status", log.getStatus(),
                "status_text", log.getStatusText(),
                "ip_address", log.getIpAddress(),
                "details", log.getDetails(),
                "period", log.getPeriod(),
                "operate_time", log.getOperateTime()
        );
    }

    private Map<String, Object> exportRecord(ExportFile file) {
        return mapOf(
                "export_id", file.exportId(),
                "file_name", file.fileName(),
                "content_type", file.contentType(),
                "size", file.size(),
                "path", file.path().toString()
        );
    }

    private boolean isAnomalyRecord(Map<String, Object> record) {
        String status = String.valueOf(record.get("data_status"));
        String statusText = String.valueOf(record.get("status_text"));
        return "2".equals(status)
                || "3".equals(status)
                || statusText.contains("危险")
                || statusText.contains("高危")
                || statusText.contains("异常");
    }

    private Map<String, Object> chainRecord(String chainType, Map<String, Object> record) {
        Map<String, Object> chained = new LinkedHashMap<>(record);
        chained.put("record_type", "operation_chain");
        chained.put("chain_type", chainType);
        return chained;
    }

    private boolean matchesHint(Object value, String hint) {
        String normalizedHint = normalizeName(hint);
        String normalizedValue = normalizeName(String.valueOf(value == null ? "" : value));
        return normalizedHint.isBlank()
                || (!normalizedValue.isBlank() && (normalizedHint.contains(normalizedValue) || normalizedValue.contains(normalizedHint)));
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            Object key = values[index];
            Object value = values[index + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String extractNumericId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private Long parseLongOrNull(String value) {
        return StringUtils.hasText(value) ? Long.parseLong(value) : null;
    }

    private List<Map<String, Object>> buildIndicatorPoints(PipeIndicatorResponse indicators) {
        if (indicators == null) {
            return List.of();
        }
        return List.of(
                Map.of("metric", "temperature", "value", nullToZero(indicators.getAveTemperature()), "status", indicators.getTemperatureStatus()),
                Map.of("metric", "flow", "value", nullToZero(indicators.getAveFlow()), "status", indicators.getFlowStatus()),
                Map.of("metric", "pressure", "value", nullToZero(indicators.getAvePressure()), "status", indicators.getPressureStatus()),
                Map.of("metric", "vibration", "value", nullToZero(indicators.getAveVibration()), "status", indicators.getVibrationStatus())
        );
    }

    private Map<String, Object> indicatorRecord(PipeIndicatorResponse indicators) {
        if (indicators == null) {
            return Map.of();
        }
        return Map.of(
                "ave_temperature", nullToZero(indicators.getAveTemperature()),
                "temperature_status", indicators.getTemperatureStatus(),
                "ave_flow", nullToZero(indicators.getAveFlow()),
                "flow_status", indicators.getFlowStatus(),
                "ave_pressure", nullToZero(indicators.getAvePressure()),
                "pressure_status", indicators.getPressureStatus(),
                "ave_vibration", nullToZero(indicators.getAveVibration()),
                "vibration_status", indicators.getVibrationStatus()
        );
    }

    private Map<String, Object> segmentRecord(MonitoringPipeOption pipe, MonitoringSegmentOption segment) {
        return Map.of(
                "pipe_id", pipe.getId(),
                "pipe_name", valueOrDefault(pipe.getName(), "unknown"),
                "pipe_level", valueOrDefault(pipe.getPipeLevel(), "unknown"),
                "segment_id", segment.getId(),
                "segment_name", valueOrDefault(segment.getName(), "unknown"),
                "segment_order", Optional.ofNullable(segment.getSegmentOrder()).orElse(0),
                "start_area_id", Optional.ofNullable(segment.getStartAreaId()).orElse(0L),
                "end_area_id", Optional.ofNullable(segment.getEndAreaId()).orElse(0L)
        );
    }

    private List<Map<String, Object>> scopeCandidateRecords(MonitoringFilterOptionsResponse options) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (MonitoringPipeOption pipe : options.getPipes()) {
            records.add(Map.of(
                    "scope_type", "pipeline",
                    "scope_id", Optional.ofNullable(pipe.getId()).orElse(0L),
                    "scope_name", valueOrDefault(pipe.getName(), "unknown"),
                    "pipe_level", valueOrDefault(pipe.getPipeLevel(), "unknown")
            ));
            for (MonitoringSegmentOption segment : Optional.ofNullable(pipe.getSegments()).orElse(List.of())) {
                records.add(Map.of(
                        "scope_type", "segment",
                        "scope_id", Optional.ofNullable(segment.getId()).orElse(0L),
                        "scope_name", valueOrDefault(segment.getName(), "unknown"),
                        "pipe_id", Optional.ofNullable(pipe.getId()).orElse(0L),
                        "pipe_name", valueOrDefault(pipe.getName(), "unknown")
                ));
            }
            if (records.size() >= 20) {
                break;
            }
        }
        return records.size() > 20 ? records.subList(0, 20) : records;
    }

    private MonitoringFilterOptionsResponse monitoringOptions(String provinceCode, String cityCode, String districtCode) {
        MonitoringFilterOptionsResponse options = pipelineTopologyService.getMonitoringFilterOptions(provinceCode, cityCode, districtCode);
        if (options == null) {
            return new MonitoringFilterOptionsResponse("unknown", List.of());
        }
        if (options.getPipes() == null) {
            options.setPipes(List.of());
        }
        return options;
    }

    private double metricValue(PipeIndicatorResponse indicators, String metric) {
        if (indicators == null) {
            return 0;
        }
        return switch (metric) {
            case "temperature" -> nullToZero(indicators.getAveTemperature());
            case "flow" -> nullToZero(indicators.getAveFlow());
            case "pressure" -> nullToZero(indicators.getAvePressure());
            case "vibration" -> nullToZero(indicators.getAveVibration());
            default -> 0;
        };
    }

    private Map<String, Object> equipmentRecord(EquipmentResponse equipment) {
        return Map.of(
                "id", valueOrDefault(equipment.getId(), "unknown"),
                "type", valueOrDefault(equipment.getType(), "unknown"),
                "status", valueOrDefault(equipment.getStatus(), "unknown"),
                "pipe_segment_id", valueOrDefault(equipment.getPipeSegmentId(), "unknown"),
                "pipe_segment_name", valueOrDefault(equipment.getPipeSegmentName(), "unknown"),
                "responsible", valueOrDefault(equipment.getResponsible(), "unknown")
        );
    }

    private Map<String, Object> taskRecord(TaskResponse task) {
        return Map.of(
                "id", valueOrDefault(task.getId(), "unknown"),
                "title", valueOrDefault(task.getTitle(), "unknown"),
                "type", valueOrDefault(task.getType(), "unknown"),
                "priority", valueOrDefault(task.getPriority(), "unknown"),
                "status", valueOrDefault(task.getStatus(), "unknown"),
                "pipe_segment_id", valueOrDefault(task.getPipeSegmentId(), "unknown"),
                "assignee", valueOrDefault(task.getAssignee(), "unknown")
        );
    }

    private double nullToZero(Double value) {
        return value == null ? 0 : value;
    }

    private record TimeRange(String start, String end) {
    }

    private record AreaScope(
            String scopeType,
            String areaId,
            String provinceCode,
            String provinceName,
            String cityCode,
            String cityName,
            String districtCode,
            String districtName
    ) {
        String displayName() {
            return String.join("", List.of(provinceName, cityName, districtName).stream().filter(value -> value != null && !value.isBlank()).toList());
        }
    }
}
