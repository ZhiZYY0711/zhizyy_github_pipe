package com.gxa.pipe.dataManagement.mapper;

import com.gxa.pipe.entity.Task;
import com.gxa.pipe.dataManagement.pojo.task.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper {

    /**
     * 分页查询任务
     * 
     * @param request 查询请求
     * @return 任务列表
     */
    List<TaskResponse> selectByPage(TaskQueryRequest request);

    /**
     * 根据ID查询任务
     * 
     * @param id 任务ID
     * @return 任务信息
     */
    TaskResponse selectById(@Param("id") Long id);

    /**
     * 插入任务
     * 
     * @param task 任务信息
     * @return 影响行数
     */
    int insert(Task task);

    /**
     * 更新任务
     * 
     * @param task 任务信息
     * @return 影响行数
     */
    int update(Task task);

    /**
     * 删除任务
     * 
     * @param id 任务ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计进行中的任务数量
     * 
     * @param areaId 区域ID
     * @return 进行中的任务数量
     */
    int countUnderwayTasks(@Param("areaId") Long areaId);

    /**
     * 统计超时任务数量
     * 
     * @return 超时任务数量
     */
    int countOvertimeTasks();

    /**
     * 统计今日任务数量
     * 
     * @return 今日任务数量
     */
    int countTodayTasks();

    /**
     * 统计已完成任务数量
     * 
     * @param areaId 区域ID
     * @return 已完成任务数量
     */
    int countCompletedTasks(@Param("areaId") Long areaId);

    /**
     * 统计任务总数
     * 
     * @param areaId 区域ID
     * @return 任务总数
     */
    int countTotalTasks(@Param("areaId") Long areaId);

    /**
     * 统计待处理任务数量
     * 
     * @param areaId 区域ID
     * @return 待处理任务数量
     */
    int countPendingTasks(@Param("areaId") Long areaId);

    /**
     * 统计紧急任务数量
     * 
     * @param areaId 区域ID
     * @return 紧急任务数量
     */
    int countUrgentTasks(@Param("areaId") Long areaId);
}