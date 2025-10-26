package com.gxa.pipe.service;

import com.gxa.pipe.pojo.entity.Task;
import com.gxa.pipe.pojo.dto.request.TaskQueryRequest;
import com.gxa.pipe.utils.PageResult;

/**
 * 任务服务接口
 */
public interface TaskService {
    
    /**
     * 分页查询任务
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<Task> getByPage(TaskQueryRequest request);
    
    /**
     * 根据ID查询任务
     * @param id 任务ID
     * @return 任务信息
     */
    Task getById(Long id);
    
    /**
     * 创建任务
     * @param task 任务信息
     * @return 是否成功
     */
    boolean create(Task task);
    
    /**
     * 更新任务
     * @param task 任务信息
     * @return 是否成功
     */
    boolean update(Task task);
    
    /**
     * 删除任务
     * @param id 任务ID
     * @return 是否成功
     */
    boolean delete(Long id);
    
    /**
     * 统计进行中的任务数量
     * @return 进行中的任务数量
     */
    int countUnderwayTasks();
    
    /**
     * 统计超时任务数量
     * @return 超时任务数量
     */
    int countOvertimeTasks();
    
    /**
     * 统计今日任务数量
     * @return 今日任务数量
     */
    int countTodayTasks();
    
    /**
     * 统计已完成任务数量
     * @return 已完成任务数量
     */
    int countCompletedTasks();
}