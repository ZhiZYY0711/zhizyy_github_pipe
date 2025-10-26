package com.gxa.pipe.controller;

import com.gxa.pipe.pojo.dto.response.RepairmanKpiResponse;
import com.gxa.pipe.pojo.dto.response.RunningWaterAlarmResponse;
import com.gxa.pipe.pojo.dto.response.WholeKpiResponse;
import com.gxa.pipe.service.MonitoringDataService;
import com.gxa.pipe.service.TaskService;
import com.gxa.pipe.service.RepairmanService;
import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 数据可视化控制器
 */
@Slf4j
@RestController
@RequestMapping("/domain/manager/data_visualization")
@RequiredArgsConstructor
public class DataVisualizationController {
    
    private final MonitoringDataService monitoringDataService;
    private final TaskService taskService;
    private final RepairmanService repairmanService;
    
    /**
     * 获取全局KPI数据
     */
    @GetMapping("/all/whole_kpi")
    public Result<WholeKpiResponse> getWholeKpi() {
        log.info("获取全局KPI数据");
        
        try {
            WholeKpiResponse response = new WholeKpiResponse();
            
            // 获取传感器数据
            response.setSensorNumbers(monitoringDataService.countTotalSensors());
            response.setAbnormalSensor(monitoringDataService.countAbnormalSensors());
            
            // 获取任务数据
            response.setWarnings(monitoringDataService.countAbnormalSensors()); // 使用异常传感器数量作为警告数
            response.setUnderwayTask(taskService.countUnderwayTasks());
            response.setOvertimeTask(taskService.countOvertimeTasks());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取全局KPI数据失败：{}", e.getMessage());
            return Result.error("获取全局KPI数据失败");
        }
    }
    
    /**
     * 获取维修工KPI数据
     */
    @GetMapping("/all/repairman_kpi")
    public Result<RepairmanKpiResponse> getRepairmanKpi() {
        log.info("获取维修工KPI数据");
        
        try {
            RepairmanKpiResponse response = new RepairmanKpiResponse();
            
            // 获取维修工数据
            response.setRepairmanNumber(repairmanService.countTotal());
            response.setTodayTaskNumber(taskService.countTodayTasks());
            
            // 计算任务完成率
            int totalTasks = taskService.countTodayTasks();
            int completedTasks = taskService.countCompletedTasks();
            double taskRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0;
            response.setTaskRate(taskRate);
            
            // 获取平均完成时间
            BigDecimal avgTime = repairmanService.calculateAverageCompletionTime();
            response.setAverageTime(avgTime != null ? avgTime.doubleValue() : 0.0);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取维修工KPI数据失败：{}", e.getMessage());
            return Result.error("获取维修工KPI数据失败");
        }
    }
    
    /**
     * 获取流水报警数据
     */
    @GetMapping("/all/running_water_alarm")
    public Result<List<RunningWaterAlarmResponse>> getRunningWaterAlarm() {
        log.info("获取流水报警数据");
        
        try {
            // 构建模拟报警数据（实际项目中应该从数据库获取）
            RunningWaterAlarmResponse alarm1 = new RunningWaterAlarmResponse();
            alarm1.setId(1L);
            alarm1.setTime(LocalDateTime.now().minusHours(2));
            alarm1.setSensorId(101L);
            alarm1.setLocation("管道A区段1");
            alarm1.setLevel("高");
            alarm1.setType("压力异常");
            
            RunningWaterAlarmResponse alarm2 = new RunningWaterAlarmResponse();
            alarm2.setId(2L);
            alarm2.setTime(LocalDateTime.now().minusHours(1));
            alarm2.setSensorId(102L);
            alarm2.setLocation("管道B区段3");
            alarm2.setLevel("中");
            alarm2.setType("温度异常");
            
            List<RunningWaterAlarmResponse> alarms = Arrays.asList(alarm1, alarm2);
            
            return Result.success(alarms);
        } catch (Exception e) {
            log.error("获取流水报警数据失败：{}", e.getMessage());
            
            // 如果获取失败，返回模拟数据作为降级处理
            RunningWaterAlarmResponse alarm1 = new RunningWaterAlarmResponse();
            alarm1.setId(1L);
            alarm1.setTime(LocalDateTime.now().minusHours(2));
            alarm1.setSensorId(101L);
            alarm1.setLocation("管道A区段1");
            alarm1.setLevel("高");
            alarm1.setType("压力异常");
            
            RunningWaterAlarmResponse alarm2 = new RunningWaterAlarmResponse();
            alarm2.setId(2L);
            alarm2.setTime(LocalDateTime.now().minusHours(1));
            alarm2.setSensorId(102L);
            alarm2.setLocation("管道B区段3");
            alarm2.setLevel("中");
            alarm2.setType("温度异常");
            
            List<RunningWaterAlarmResponse> alarms = Arrays.asList(alarm1, alarm2);
            
            return Result.success(alarms);
        }
    }
}