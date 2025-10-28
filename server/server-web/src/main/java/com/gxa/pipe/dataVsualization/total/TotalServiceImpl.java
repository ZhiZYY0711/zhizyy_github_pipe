package com.gxa.pipe.dataVsualization.total;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据可视化总页面业务逻辑实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotalServiceImpl implements TotalService {
    
    private final TotalMapper totalMapper;
    
    @Override
    public WholeKpiResponse getWholeKpi(Long areaId) {
        log.info("开始获取全局KPI数据，区域ID: {}", areaId);
        
        try {
            // 统计传感器总数
            Long sensorNumbers = totalMapper.countTotalSensors(areaId);
            
            // 统计异常/离线传感器数量
            Long abnormalSensorNumbers = totalMapper.countAbnormalSensors(areaId);
            
            // 统计今日高危/危险报警次数
            Long warnings = totalMapper.countTodayWarnings(areaId);
            
            // 统计进行中的任务数量
            Long underwayTask = totalMapper.countUnderwayTasks(areaId);
            
            // 统计超时/未处理的任务数量
            Long overtimeTask = totalMapper.countOvertimeTasks(areaId);
            
            // 构建响应对象
            WholeKpiResponse response = new WholeKpiResponse();
            response.setSensorNumbers(sensorNumbers != null ? sensorNumbers : 0L);
            response.setAbnormalSensorNumbers(abnormalSensorNumbers != null ? abnormalSensorNumbers : 0L);
            response.setWarnings(warnings != null ? warnings : 0L);
            response.setUnderwayTask(underwayTask != null ? underwayTask : 0L);
            response.setOvertimeTask(overtimeTask != null ? overtimeTask : 0L);
            
            log.info("全局KPI数据获取成功: 传感器总数={}, 异常传感器={}, 警告数={}, 进行中任务={}, 超时任务={}", 
                    response.getSensorNumbers(), response.getAbnormalSensorNumbers(), 
                    response.getWarnings(), response.getUnderwayTask(), response.getOvertimeTask());
            
            return response;
            
        } catch (Exception e) {
            log.error("获取全局KPI数据失败，区域ID: {}", areaId, e);
            throw new RuntimeException("获取全局KPI数据失败", e);
        }
    }
    
    @Override
    public RepairmanKpiResponse getRepairmanKpi(Long areaId) {
        log.info("开始获取检修员KPI数据，区域ID: {}", areaId);
        
        try {
            // 统计检修员总数
            Long repairmanNumber = totalMapper.countRepairmanNumber(areaId);
            
            // 统计今日完成任务数
            Long todayCompletedTasks = totalMapper.countTodayCompletedTasks(areaId);
            
            // 计算任务完成率（百分比整数）
            Long totalTasks = totalMapper.countTotalTasks(areaId);
            Long completedTasks = totalMapper.countCompletedTasks(areaId);
            Integer taskRate = 0;
            if (totalTasks != null && totalTasks > 0 && completedTasks != null) {
                double rate = (completedTasks.doubleValue() / totalTasks.doubleValue()) * 100;
                taskRate = (int) Math.round(rate);
            }
            
            // 计算平均处理时间（小时，取整）
            Double avgTime = totalMapper.calculateAverageProcessingTime(areaId);
            Integer averageTime = 0;
            if (avgTime != null && avgTime > 0) {
                averageTime = (int) Math.round(avgTime);
            }
            
            // 构建响应对象
            RepairmanKpiResponse response = new RepairmanKpiResponse();
            response.setRepairmanNumber(repairmanNumber != null ? repairmanNumber.intValue() : 0);
            response.setTodayTaskNumber(todayCompletedTasks != null ? todayCompletedTasks.intValue() : 0);
            response.setTaskRate(taskRate);
            response.setAverageTime(averageTime);
            
            log.info("检修员KPI数据获取成功");
            return response;
            
        } catch (Exception e) {
            log.error("获取检修员KPI数据失败", e);
            throw new RuntimeException("获取检修员KPI数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<RunningWaterResponse> getRunningWaterAlarm(Long areaId) {
        log.info("开始获取流水报警数据，区域ID: {}", areaId);
        
        try {
            List<RunningWaterResponse> alarmList = totalMapper.selectRunningWaterAlarm(areaId);
            
            log.info("流水报警数据获取成功，共{}条记录", alarmList != null ? alarmList.size() : 0);
            return alarmList;
            
        } catch (Exception e) {
            log.error("获取流水报警数据失败，区域ID: {}", areaId, e);
            throw new RuntimeException("获取流水报警数据失败", e);
        }
    }
}