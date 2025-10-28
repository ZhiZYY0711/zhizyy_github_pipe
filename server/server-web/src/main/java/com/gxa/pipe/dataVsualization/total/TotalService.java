package com.gxa.pipe.dataVsualization.total;

import java.util.List;

import com.gxa.pipe.dataVsualization.total.WholeKpiResponse;

/**
 * 数据可视化总页面业务逻辑接口
 */
public interface TotalService {
    
    /**
     * 获取全局KPI指标数据
     * @param areaId 区域ID，可选参数，为null时查询全部区域
     * @return 全局KPI响应数据
     */
    WholeKpiResponse getWholeKpi(Long areaId);
    
    /**
     * 获取检修员KPI指标数据
     * @param areaId 区域ID，可选参数，为null时查询全部区域
     * @return 检修员KPI响应数据
     */
    RepairmanKpiResponse getRepairmanKpi(Long areaId);
    
    /**
     * 获取流水报警数据
     * @param areaId 区域ID，可选参数，为null时查询全部区域
     * @return 流水报警响应数据列表
     */
    List<RunningWaterResponse> getRunningWaterAlarm(Long areaId);
}