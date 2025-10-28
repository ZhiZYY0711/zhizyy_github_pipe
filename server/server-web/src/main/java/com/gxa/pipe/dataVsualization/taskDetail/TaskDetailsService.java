package com.gxa.pipe.dataVsualization.taskDetail;

import java.util.List;

/**
 * 任务详情业务逻辑接口
 */
public interface TaskDetailsService {
    
    /**
     * 获取任务状态分布
     * @param request 任务请求参数
     * @return 任务状态分布列表
     */
    List<TaskStatusResponse> getTaskStatusDistribution(TaskResquest request);
    
    /**
     * 获取任务数量趋势
     * @param request 任务请求参数
     * @return 任务数量趋势列表
     */
    List<TaskNumberResponse> getTaskNumberTrend(TaskResquest request);
    
    /**
     * 获取区域任务对比
     * @param request 任务请求参数
     * @return 同级区域任务对比列表
     */
    List<AreaTaskResponse> getAreaTaskContrast(TaskResquest request);
    
    /**
     * 获取KPI榜单
     * @param request KPI榜单请求参数
     * @return KPI榜单列表
     */
    List<TaskKpiResponse> getKpiList(TaskKpiRequest request);
}