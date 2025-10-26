package com.gxa.pipe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.TaskMapper;
import com.gxa.pipe.pojo.entity.Task;
import com.gxa.pipe.pojo.dto.request.TaskQueryRequest;
import com.gxa.pipe.service.TaskService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    
    private final TaskMapper taskMapper;
    
    @Override
    public PageResult<Task> getByPage(TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);
        
        // 设置分页参数
-        PageHelper.startPage(request.getPageNum(), request.getPageSize());
+        PageHelper.startPage(request.getPage(), request.getPageSize());
        
        // 查询数据
        Page<Task> page = (Page<Task>) taskMapper.selectByPage(request);
        
        // 构建分页结果
        PageResult<Task> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
-        result.setPageNum(request.getPageNum());
+        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());
        
        return result;
    }
    
    @Override
    public Task getById(Long id) {
        return taskMapper.selectById(id);
    }
    
    @Override
    public boolean create(Task task) {
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        return taskMapper.insert(task) > 0;
    }
    
    @Override
    public boolean update(Task task) {
        task.setUpdateTime(LocalDateTime.now());
        
        return taskMapper.update(task) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return taskMapper.deleteById(id) > 0;
    }
    
    @Override
    public int countUnderwayTasks() {
        return taskMapper.countUnderwayTasks();
    }
    
    @Override
    public int countOvertimeTasks() {
        return taskMapper.countOvertimeTasks();
    }
    
    @Override
    public int countTodayTasks() {
        return taskMapper.countTodayTasks();
    }
    
    @Override
    public int countCompletedTasks() {
        return taskMapper.countCompletedTasks();
    }
}