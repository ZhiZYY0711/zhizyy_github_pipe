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

        WholeKpiResponse response = totalMapper.selectWholeKpi(areaId);
        return response != null ? response : new WholeKpiResponse();
    }

    @Override
    public RepairmanKpiResponse getRepairmanKpi(Long areaId) {
        log.info("获取检修员KPI数据，区域ID: {}", areaId);

        RepairmanKpiResponse response = totalMapper.selectRepairmanKpi(areaId);
        return response != null ? response : new RepairmanKpiResponse();
    }

    @Override
    public List<RunningWaterResponse> getRunningWaterAlarm(Long areaId, Long startTime, Long endTime) {
        log.info("获取流水报警数据，区域ID: {}，开始时间: {}，结束时间: {}", areaId, startTime, endTime);
        return totalMapper.selectRunningWaterAlarm(areaId, startTime, endTime);
    }

}
