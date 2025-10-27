package com.gxa.pipe.dataManagement.service;

import com.gxa.pipe.dataManagement.pojo.task.*;
import com.gxa.pipe.utils.PageResult;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 分页查询任务
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<TaskResponse> getByPage(TaskQueryRequest request);

    /**
     * 根据ID查询任务
     * 
     * @param id 任务ID
     * @return 任务信息
     */
    TaskResponse getById(Long id);

    /**
     * 创建任务
     * 
     * @param request 任务创建请求
     * @return 是否成功
     */
    boolean create(TaskAddRequest request);

    /**
     * 更新任务
     * 
     * @param request 任务更新请求
     * @return 是否成功
     */
    boolean update(TaskUpdateRequest request);

    /**
     * 删除任务
     * 
     * @param id 任务ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 统计进行中的任务数量
     * 
     * @return 进行中的任务数量
     */
    int countUnderwayTasks();

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
     * @return 已完成任务数量
     */
    int countCompletedTasks();

    /**
     * 获取任务指标卡数据
     * 
     * @param areaId 区域ID（可选）
     * @return 任务指标卡数据
     */
    TaskIndicatorResponse getTaskIndicators(Long areaId);
}