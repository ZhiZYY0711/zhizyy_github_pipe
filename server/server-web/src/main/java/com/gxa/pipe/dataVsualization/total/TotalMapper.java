package com.gxa.pipe.dataVsualization.total;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据可视化总页面数据访问层接口
 */
@Mapper
public interface TotalMapper {

    WholeKpiResponse selectWholeKpi(@Param("areaId") Long areaId);

    RepairmanKpiResponse selectRepairmanKpi(@Param("areaId") Long areaId);
    
    /**
     * 统计传感器总数
     * @param areaId 区域ID，可选参数
     * @return 传感器总数
     */
    Long countTotalSensors(@Param("areaId") Long areaId);
    
    /**
     * 统计异常/离线传感器数量
     * @param areaId 区域ID，可选参数
     * @return 异常传感器数量
     */
    Long countAbnormalSensors(@Param("areaId") Long areaId);
    
    /**
     * 统计今日高危/危险报警次数
     * @param areaId 区域ID，可选参数
     * @return 今日警告次数
     */
    Long countTodayWarnings(@Param("areaId") Long areaId);
    
    /**
     * 统计进行中的任务数量
     * @param areaId 区域ID，可选参数
     * @return 进行中任务数量
     */
    Long countUnderwayTasks(@Param("areaId") Long areaId);
    
    /**
     * 统计超时/未处理的任务数量
     * @param areaId 区域ID，可选参数
     * @return 超时任务数量
     */
    Long countOvertimeTasks(@Param("areaId") Long areaId);
    
    /**
     * 统计检修员总数
     * @param areaId 区域ID，可选参数
     * @return 检修员总数
     */
    Long countRepairmanNumber(@Param("areaId") Long areaId);
    
    /**
     * 统计今日完成任务数
     * @param areaId 区域ID，可选参数
     * @return 今日完成任务数
     */
    Long countTodayCompletedTasks(@Param("areaId") Long areaId);
    
    /**
     * 统计总任务数（用于计算完成率）
     * @param areaId 区域ID，可选参数
     * @return 总任务数
     */
    Long countTotalTasks(@Param("areaId") Long areaId);
    
    /**
     * 统计已完成任务数（用于计算完成率）
     * @param areaId 区域ID，可选参数
     * @return 已完成任务数
     */
    Long countCompletedTasks(@Param("areaId") Long areaId);
    
    /**
     * 计算任务平均处理时间（小时）
     * @param areaId 区域ID，可选参数
     * @return 平均处理时间
     */
    Double calculateAverageProcessingTime(@Param("areaId") Long areaId);
    
    /**
     * 查询流水报警数据
     * @param areaId 区域ID，可选参数
     * @param startTime 开始时间戳，可选参数，毫秒
     * @param endTime 结束时间戳，可选参数，毫秒
     * @return 流水报警数据列表
     */
    List<RunningWaterResponse> selectRunningWaterAlarm(
            @Param("areaId") Long areaId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
}
