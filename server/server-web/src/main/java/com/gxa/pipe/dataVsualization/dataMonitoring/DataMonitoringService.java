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
}