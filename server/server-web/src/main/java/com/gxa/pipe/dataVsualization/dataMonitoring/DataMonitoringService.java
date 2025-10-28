package com.gxa.pipe.dataVsualization.dataMonitoring;

import java.util.List;

/**
 * 数据监控服务接口
 */
public interface DataMonitoringService {
    
    /**
     * 获取区域四维度数据
     * @param request 查询请求参数
     * @return 区域四维度数据列表
     */
    List<AreaDetailResponse> getAreaDetails(AreaDetailRequest request);
    
    /**
     * 获取管道四维度数据
     * @param request 查询请求参数
     * @return 管道四维度数据列表
     */
    List<PipeDetailResponse> getPipeDetails(PipeDetailRequest request);
    
    /**
     * 获取管道关键指标卡数据
     * @param pipeId 管道的唯一标识
     * @return 管道关键指标卡数据
     */
    PipeIndicatorResponse getPipeKeyIndicators(String pipeId);
}