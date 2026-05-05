package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.dataVsualization.dashboard.DashboardSummaryResponse;
import com.gxa.pipe.dataVsualization.dashboard.DashboardTooltipResponse;
import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("data_visualization/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardReadService dashboardReadService;

    @GetMapping("/summary")
    public Result<DashboardSummaryResponse> getSummary(
            @RequestParam(value = "area_id", required = false) Long areaId,
            @RequestParam(value = "start_time", required = false) Long startTime,
            @RequestParam(value = "end_time", required = false) Long endTime) {
        return Result.success("获取大屏汇总数据成功", dashboardReadService.getSummary(areaId, startTime, endTime));
    }

    @GetMapping("/area_tooltip")
    public Result<DashboardTooltipResponse> getAreaTooltip(
            @RequestParam(value = "area_id", required = false) Long areaId,
            @RequestParam(value = "start_time", required = false) Long startTime,
            @RequestParam(value = "end_time", required = false) Long endTime) {
        return Result.success("获取大屏区域提示数据成功", dashboardReadService.getTooltip(areaId, startTime, endTime));
    }
}
