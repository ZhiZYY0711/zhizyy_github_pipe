package com.gxa.pipe.dataVsualization.dataMonitoring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<AreaDetailResponse> selectAreaDetailsFromDailyAggregate(AreaDetailRequest request);
    
    /**
     * 查询管道四维度数据
     * 
     * @param request 查询请求参数
     * @return 管道四维度数据列表
     */
    List<PipeDetailResponse> selectPipeDetails(PipeDetailRequest request);

    List<PipeDetailResponse> selectPipeDetailsFromDailyAggregate(PipeDetailRequest request);

    List<PipeSegmentSelection> selectPipeSegmentSelections(
            @Param("pipeId") Long pipeId,
            @Param("segmentIds") List<Long> segmentIds);
    
    /**
     * 查询管道关键指标卡数据
     * 
     * @param pipeId 管道的唯一标识
     * @return 管道关键指标卡数据
     */
    PipeIndicatorResponse selectPipeKeyIndicators(
            @Param("pipeId") String pipeId,
            @Param("segmentId") String segmentId,
            @Param("segmentIds") List<Long> segmentIds);
}
