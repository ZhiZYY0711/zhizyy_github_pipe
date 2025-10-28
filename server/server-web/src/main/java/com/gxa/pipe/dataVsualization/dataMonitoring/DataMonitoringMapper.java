package com.gxa.pipe.dataVsualization.dataMonitoring;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据监控Mapper接口
 */
@Mapper
public interface DataMonitoringMapper {

    /**
     * 查询区域四维度数据
     * 
     * @param request 查询请求参数
     * @return 区域四维度数据列表
     */
    List<AreaDetailResponse> selectAreaDetails(AreaDetailRequest request);
}