package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.virtualExpert.agent.dto.ToolQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/virtual-expert/tools")
public class VirtualExpertToolProxyController {

    private final VirtualExpertToolService toolService;

    @GetMapping("/monitoring-trend")
    public ToolQueryResponse queryMonitoringTrend(
            @RequestParam(value = "object_id", required = false) String objectId,
            @RequestParam(value = "metric", required = false) String metric) {
        return toolService.queryMonitoringTrend(objectId, metric);
    }

    @GetMapping("/pipe-segment-context")
    public ToolQueryResponse queryPipeSegmentContext(
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.queryPipeSegmentContext(objectId);
    }

    @GetMapping("/equipment-status")
    public ToolQueryResponse queryEquipmentStatus(
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.queryEquipmentStatus(objectId);
    }

    @GetMapping("/related-tasks")
    public ToolQueryResponse queryRelatedTasks(
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.queryRelatedTasks(objectId);
    }

    @GetMapping("/similar-cases")
    public ToolQueryResponse searchSimilarCases(
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.searchSimilarCases(objectId);
    }

    @GetMapping("/resolve-operational-scope")
    public ToolQueryResponse resolveOperationalScope(
            @RequestParam(value = "raw_input", required = false) String rawInput,
            @RequestParam(value = "scope_hint", required = false) String scopeHint,
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.resolveOperationalScope(rawInput, scopeHint, objectId);
    }

    @GetMapping("/monitoring-aggregate")
    public ToolQueryResponse queryMonitoringAggregate(
            @RequestParam(value = "scope_type", required = false) String scopeType,
            @RequestParam(value = "scope_id", required = false) String scopeId,
            @RequestParam(value = "metric", required = false) String metric,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime,
            @RequestParam(value = "aggregation", required = false) String aggregation) {
        return toolService.queryMonitoringAggregate(scopeType, scopeId, metric, startTime, endTime, aggregation);
    }

    @GetMapping("/monitoring-history")
    public ToolQueryResponse queryMonitoringHistory(
            @RequestParam(value = "object_type", required = false) String objectType,
            @RequestParam(value = "object_id", required = false) String objectId,
            @RequestParam(value = "metric", required = false) String metric,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime,
            @RequestParam(value = "granularity", required = false) String granularity) {
        return toolService.queryMonitoringHistory(objectType, objectId, metric, startTime, endTime, granularity);
    }

    @GetMapping("/area-monitoring-summary")
    public ToolQueryResponse queryAreaMonitoringSummary(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "metric", required = false) String metric,
            @RequestParam(value = "window", required = false) String window,
            @RequestParam(value = "aggregation", required = false) String aggregation) {
        return toolService.queryAreaMonitoringSummary(areaId, metric, window, aggregation);
    }

    @GetMapping("/alarm-events")
    public ToolQueryResponse queryAlarmEvents(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "severity", required = false) String severity) {
        return toolService.queryAlarmEvents(scope, timeRange, severity);
    }

    @GetMapping("/inspection-history")
    public ToolQueryResponse queryInspectionHistory(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange) {
        return toolService.queryInspectionHistory(scope, timeRange);
    }

    @GetMapping("/maintenance-history")
    public ToolQueryResponse queryMaintenanceHistory(
            @RequestParam(value = "equipment_id", required = false) String equipmentId,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange) {
        return toolService.queryMaintenanceHistory(equipmentId, scope, timeRange);
    }

    @GetMapping("/topology-impact")
    public ToolQueryResponse queryTopologyImpact(
            @RequestParam(value = "object_type", required = false) String objectType,
            @RequestParam(value = "object_id", required = false) String objectId) {
        return toolService.queryTopologyImpact(objectType, objectId);
    }

    @GetMapping("/emergency-plan")
    public ToolQueryResponse queryEmergencyPlan(
            @RequestParam(value = "incident_type", required = false) String incidentType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "severity", required = false) String severity) {
        return toolService.queryEmergencyPlan(incidentType, scope, severity);
    }

    @GetMapping("/operation-logs")
    public ToolQueryResponse queryOperationLogs(
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "object", required = false) String object,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "operation", required = false) String operation) {
        return toolService.queryOperationLogs(user, object, timeRange, operation);
    }

    @GetMapping("/report-inventory")
    public ToolQueryResponse queryReportInventory(
            @RequestParam(value = "session_id", required = false) String sessionId,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "format", required = false) String format) {
        return toolService.queryReportInventory(sessionId, timeRange, format);
    }
}
