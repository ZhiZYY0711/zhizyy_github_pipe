package com.gxa.pipe.dataVsualization.taskDetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 任务详情数据访问接口
 */
@Mapper
public interface TaskDetailsMapper {
    
    /**
     * 根据区域ID和时间范围查询任务状态分布
     * @param areaId 区域ID
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 任务状态分布列表
     */
    List<TaskStatusResponse> selectTaskStatusDistribution(@Param("areaId") Integer areaId, 
                                                         @Param("startTime") Long startTime, 
                                                         @Param("endTime") Long endTime);
    
    /**
     * 根据区域ID和时间范围查询任务数量趋势
     * @param areaId 区域ID
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 任务数量趋势列表
     */
    List<TaskNumberResponse> selectTaskNumberTrend(@Param("areaId") Integer areaId, 
                                                  @Param("startTime") Long startTime, 
                                                  @Param("endTime") Long endTime);
}