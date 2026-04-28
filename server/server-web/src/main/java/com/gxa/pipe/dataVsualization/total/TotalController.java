package com.gxa.pipe.dataVsualization.total;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("data_visualization/all")
@RequiredArgsConstructor
@Slf4j
public class TotalController {

    private final TotalService totalService;

    @GetMapping("/whole_kpi")
    public Result<WholeKpiResponse> getWholeKpi(@RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取全局KPI，区域ID: {}", areaId);
        return Result.success("获取全局KPI数据成功", totalService.getWholeKpi(areaId));
    }

    @GetMapping("/repairman_kpi")
    public Result<RepairmanKpiResponse> getRepairmanKpi(
            @RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取检修员KPI，区域ID: {}", areaId);
        return Result.success("获取检修员KPI数据成功", totalService.getRepairmanKpi(areaId));
    }

    @GetMapping("/running_water_alarm")
    public Result<List<RunningWaterResponse>> getRunningWaterAlarm(
            @RequestParam(value = "area_id", required = false) Long areaId,
            @RequestParam(value = "start_time", required = false) Long startTime,
            @RequestParam(value = "end_time", required = false) Long endTime) {
        log.info("获取流水报警，区域ID: {}，开始时间: {}，结束时间: {}", areaId, startTime, endTime);
        return Result.success("获取流水报警数据成功", totalService.getRunningWaterAlarm(areaId, startTime, endTime));
    }
}
