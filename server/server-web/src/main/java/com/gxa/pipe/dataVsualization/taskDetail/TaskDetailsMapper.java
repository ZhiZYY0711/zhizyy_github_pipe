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
    
    /**
     * 根据区域ID范围和时间范围查询下一级区域任务对比
     * @param startRange 区域ID起始范围
     * @param endRange 区域ID结束范围
     * @param targetLevel 目标区域层级：CITY / DISTRICT / SELF
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 区域任务对比列表
     */
    List<AreaTaskResponse> selectAreaTaskContrast(@Param("startRange") Integer startRange,
                                                 @Param("endRange") Integer endRange,
                                                 @Param("targetLevel") String targetLevel,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime);
    
    /**
     * 查询完成任务数榜单
     * @param areaId 区域ID
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 完成任务数榜单列表
     */
    List<TaskKpiResponse> selectTaskCompletionRanking(@Param("areaId") Integer areaId,
                                                     @Param("startTime") Long startTime,
                                                     @Param("endTime") Long endTime);
    
    /**
     * 查询平均响应时间榜单
     * @param areaId 区域ID
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 平均响应时间榜单列表
     */
    List<TaskKpiResponse> selectAverageResponseTimeRanking(@Param("areaId") Integer areaId,
                                                          @Param("startTime") Long startTime,
                                                          @Param("endTime") Long endTime);
    
    /**
     * 查询平均完成时间榜单
     * @param areaId 区域ID
     * @param startTime 开始时间（时间戳，可为null）
     * @param endTime 结束时间（时间戳，可为null）
     * @return 平均完成时间榜单列表
     */
    List<TaskKpiResponse> selectAverageCompletionTimeRanking(@Param("areaId") Integer areaId,
                                                            @Param("startTime") Long startTime,
                                                            @Param("endTime") Long endTime);
}
