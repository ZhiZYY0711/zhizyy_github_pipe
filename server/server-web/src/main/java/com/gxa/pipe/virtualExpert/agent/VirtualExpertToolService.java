package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.dataManagement.equipment.EquipmentQueryRequest;
import com.gxa.pipe.dataManagement.equipment.EquipmentResponse;
import com.gxa.pipe.dataManagement.equipment.EquipmentService;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataIndicatorResponse;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataQueryRequest;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataQueryResponse;
import com.gxa.pipe.dataManagement.monitoring.MonitoringDataService;
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
            return equipmentService.findEquipmentById(extractNumericId(equipmentId));
        }
        EquipmentQueryRequest request = new EquipmentQueryRequest();
        request.setPipeSegmentId(extractNumericId(scope));
        TimeRange range = parseTimeRange(timeRange);
        request.setLastCheckStart(range.start());
        request.setLastCheckEnd(range.end());
        request.setPage("1");
        request.setPageSize("20");
        return equipmentService.findEquipmentByConditions(request);
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
}
