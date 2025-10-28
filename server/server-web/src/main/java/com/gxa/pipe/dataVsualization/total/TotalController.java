package com.gxa.pipe.dataVsualization.total;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gxa.pipe.utils.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data_visualization/all")
@RequiredArgsConstructor
@Slf4j
public class TotalController {
    
    private final TotalService totalService;
    
    /**
     * 获取全局KPI指标数据
     * @param areaId 区域ID，可选参数
     * @return 全局KPI响应数据
     */
    @GetMapping("/whole_kpi")
    public Result<WholeKpiResponse> getWholeKpi(@RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("接收到获取全局KPI请求，区域ID: {}", areaId);
        
        try {
            WholeKpiResponse response = totalService.getWholeKpi(areaId);
            log.info("全局KPI数据获取成功");
            return Result.success("获取全局KPI数据成功", response);
        } catch (Exception e) {
            log.error("获取全局KPI数据失败", e);
            return Result.error("获取全局KPI数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取检修员KPI指标数据
     * @param areaId 区域ID，可选参数
     * @return 检修员KPI响应数据
     */
    @GetMapping("/repairman_kpi")
    public Result<RepairmanKpiResponse> getRepairmanKpi(@RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("接收到获取检修员KPI请求，区域ID: {}", areaId);
        
        try {
            RepairmanKpiResponse response = totalService.getRepairmanKpi(areaId);
            log.info("检修员KPI数据获取成功");
            return Result.success("获取检修员KPI数据成功", response);
        } catch (Exception e) {
            log.error("获取检修员KPI数据失败", e);
            return Result.error("获取检修员KPI数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取流水报警数据
     * @param areaId 区域ID，可选参数
     * @return 流水报警响应数据
     */
    @GetMapping("/running_water_alarm")
    public Result<List<RunningWaterResponse>> getRunningWaterAlarm(@RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("接收到获取流水报警请求，区域ID: {}", areaId);
        
        try {
            List<RunningWaterResponse> response = totalService.getRunningWaterAlarm(areaId);
            log.info("流水报警数据获取成功，共{}条记录", response != null ? response.size() : 0);
            return Result.success("获取流水报警数据成功", response);
        } catch (Exception e) {
            log.error("获取流水报警数据失败", e);
            return Result.error("获取流水报警数据失败: " + e.getMessage());
        }
    }
}
