package com.gxa.pipe.virtualExpert.agent;

import com.gxa.pipe.virtualExpert.agent.dto.ToolQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    @GetMapping("/resolve-area-scope")
    public ToolQueryResponse resolveAreaScope(
            @RequestParam(value = "raw_input", required = false) String rawInput,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "district", required = false) String district) {
        return toolService.resolveAreaScope(rawInput, province, city, district);
    }

    @GetMapping("/area-data-catalog")
    public ToolQueryResponse queryAreaDataCatalog(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "scope_type", required = false) String scopeType,
            @RequestParam(value = "include_samples", required = false) String includeSamples) {
        return toolService.queryAreaDataCatalog(areaId, scopeType, includeSamples);
    }

    @GetMapping("/area-operational-overview")
    public ToolQueryResponse queryAreaOperationalOverview(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "scope_type", required = false) String scopeType,
            @RequestParam(value = "time_range", required = false) String timeRange) {
        return toolService.queryAreaOperationalOverview(areaId, scopeType, timeRange);
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

    @GetMapping("/area-alarm-summary")
    public ToolQueryResponse queryAreaAlarmSummary(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "scope_type", required = false) String scopeType,
            @RequestParam(value = "time_range", required = false) String timeRange) {
        return toolService.queryAreaAlarmSummary(areaId, scopeType, timeRange);
    }

    @GetMapping("/recent-anomalies")
    public ToolQueryResponse findRecentAnomalies(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "limit", required = false) String limit) {
        return toolService.findRecentAnomalies(scope, timeRange, limit);
    }

    @GetMapping("/top-risk-segments")
    public ToolQueryResponse findTopRiskSegments(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "limit", required = false) String limit) {
        return toolService.findTopRiskSegments(scope, timeRange, limit);
    }

    @GetMapping("/alarm-events")
    public ToolQueryResponse queryAlarmEvents(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "severity", required = false) String severity) {
        return toolService.queryAlarmEvents(scope, timeRange, severity);
    }

    @GetMapping("/unclosed-alarms")
    public ToolQueryResponse findUnclosedAlarms(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "severity", required = false) String severity) {
        return toolService.findUnclosedAlarms(scope, timeRange, severity);
    }

    @GetMapping("/stale-inspections")
    public ToolQueryResponse findStaleInspections(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "stale_days", required = false) String staleDays,
            @RequestParam(value = "limit", required = false) String limit) {
        return toolService.findStaleInspections(scope, staleDays, limit);
    }

    @GetMapping("/area-task-summary")
    public ToolQueryResponse queryAreaTaskSummary(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "time_range", required = false) String timeRange) {
        return toolService.queryAreaTaskSummary(areaId, timeRange);
    }

    @GetMapping("/operation-chain")
    public ToolQueryResponse traceOperationChain(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "time_range", required = false) String timeRange,
            @RequestParam(value = "incident_id", required = false) String incidentId) {
        return toolService.traceOperationChain(scope, timeRange, incidentId);
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

    @GetMapping("/resolve-asset-scope")
    public ToolQueryResponse resolveAssetScope(
            @RequestParam(value = "raw_input", required = false) String rawInput,
            @RequestParam(value = "asset_hint", required = false) String assetHint,
            @RequestParam(value = "object_type", required = false) String objectType) {
        return toolService.resolveAssetScope(rawInput, assetHint, objectType);
    }

    @GetMapping("/area-asset-summary")
    public ToolQueryResponse queryAreaAssetSummary(
            @RequestParam(value = "area_id", required = false) String areaId,
            @RequestParam(value = "scope_type", required = false) String scopeType) {
        return toolService.queryAreaAssetSummary(areaId, scopeType);
    }

    @GetMapping("/asset-relationships")
    public ToolQueryResponse queryAssetRelationships(
            @RequestParam(value = "object_type", required = false) String objectType,
            @RequestParam(value = "object_id", required = false) String objectId,
            @RequestParam(value = "include_equipment", required = false) String includeEquipment) {
        return toolService.queryAssetRelationships(objectType, objectId, includeEquipment);
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

    @GetMapping("/case-library")
    public ToolQueryResponse queryCaseLibrary(
            @RequestParam(value = "scenario", required = false) String scenario,
            @RequestParam(value = "metric", required = false) String metric,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "limit", required = false) String limit) {
        return toolService.queryCaseLibrary(scenario, metric, scope, limit);
    }

    @GetMapping("/sop-by-scenario")
    public ToolQueryResponse querySopByScenario(
            @RequestParam(value = "scenario", required = false) String scenario,
            @RequestParam(value = "severity", required = false) String severity) {
        return toolService.querySopByScenario(scenario, severity);
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

    @PostMapping("/create-export")
    public ToolQueryResponse createExport(@RequestBody Map<String, Object> request) {
        return toolService.createExport(request);
    }
}
