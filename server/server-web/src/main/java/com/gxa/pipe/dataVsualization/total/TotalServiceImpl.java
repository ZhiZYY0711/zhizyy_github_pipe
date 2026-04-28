package com.gxa.pipe.dataVsualization.total;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TotalServiceImpl implements TotalService {

    private final TotalMapper totalMapper;

    @Override
    public WholeKpiResponse getWholeKpi(Long areaId) {
        log.info("获取全局KPI数据，区域ID: {}", areaId);

        Long sensorNumbers = totalMapper.countTotalSensors(areaId);
        Long abnormalSensorNumbers = totalMapper.countAbnormalSensors(areaId);
        Long warnings = totalMapper.countTodayWarnings(areaId);
        Long underwayTask = totalMapper.countUnderwayTasks(areaId);
        Long overtimeTask = totalMapper.countOvertimeTasks(areaId);

        WholeKpiResponse response = new WholeKpiResponse();
        response.setSensorNumbers(sensorNumbers != null ? sensorNumbers : 0L);
        response.setAbnormalSensorNumbers(abnormalSensorNumbers != null ? abnormalSensorNumbers : 0L);
        response.setWarnings(warnings != null ? warnings : 0L);
        response.setUnderwayTask(underwayTask != null ? underwayTask : 0L);
        response.setOvertimeTask(overtimeTask != null ? overtimeTask : 0L);
        return response;
    }

    @Override
    public RepairmanKpiResponse getRepairmanKpi(Long areaId) {
        log.info("获取检修员KPI数据，区域ID: {}", areaId);

        Long repairmanNumber = totalMapper.countRepairmanNumber(areaId);
        Long todayCompletedTasks = totalMapper.countTodayCompletedTasks(areaId);
        Long totalTasks = totalMapper.countTotalTasks(areaId);
        Long completedTasks = totalMapper.countCompletedTasks(areaId);
        Double avgTime = totalMapper.calculateAverageProcessingTime(areaId);

        RepairmanKpiResponse response = new RepairmanKpiResponse();
        response.setRepairmanNumber(repairmanNumber != null ? repairmanNumber.intValue() : 0);
        response.setTodayTaskNumber(todayCompletedTasks != null ? todayCompletedTasks.intValue() : 0);
        response.setTaskRate(calculateTaskRate(totalTasks, completedTasks));
        response.setAverageTime(avgTime != null && avgTime > 0 ? (int) Math.round(avgTime) : 0);
        return response;
    }

    @Override
    public List<RunningWaterResponse> getRunningWaterAlarm(Long areaId, Long startTime, Long endTime) {
        log.info("获取流水报警数据，区域ID: {}，开始时间: {}，结束时间: {}", areaId, startTime, endTime);
        return totalMapper.selectRunningWaterAlarm(areaId, startTime, endTime);
    }

    private int calculateTaskRate(Long totalTasks, Long completedTasks) {
        if (totalTasks == null || totalTasks <= 0 || completedTasks == null) {
            return 0;
        }

        double rate = (completedTasks.doubleValue() / totalTasks.doubleValue()) * 100;
        return (int) Math.round(rate);
    }
}
