package com.gxa.pipe.dataVsualization.taskDetail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 任务详情业务逻辑实现类
 */
@Service
public class TaskDetailsServiceImpl implements TaskDetailsService {
    
    @Autowired
    private TaskDetailsMapper taskDetailsMapper;
    
    /**
     * 获取任务状态分布
     * @param request 任务请求参数
     * @return 任务状态分布列表
     */
    @Override
    public List<TaskStatusResponse> getTaskStatusDistribution(TaskResquest request) {
        // 调用Mapper层查询数据
        return taskDetailsMapper.selectTaskStatusDistribution(
            request.getAreaId(), 
            request.getStartTime(), 
            request.getEndTime()
        );
    }
    
    /**
     * 获取任务数量趋势
     * @param request 任务请求参数
     * @return 任务数量趋势列表
     */
    @Override
    public List<TaskNumberResponse> getTaskNumberTrend(TaskResquest request) {
        // 调用Mapper层查询数据
        return taskDetailsMapper.selectTaskNumberTrend(
            request.getAreaId(), 
            request.getStartTime(), 
            request.getEndTime()
        );
    }
}