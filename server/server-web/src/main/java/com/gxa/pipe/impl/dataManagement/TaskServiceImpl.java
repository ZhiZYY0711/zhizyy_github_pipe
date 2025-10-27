package com.gxa.pipe.impl.dataManagement;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.gxa.pipe.mapper.dataManagement.TaskMapper;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskAddRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskQueryRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskUpdateRequest;
import com.gxa.pipe.pojo.dto.response.dataManagement.task.TaskIndicatorResponse;
import com.gxa.pipe.pojo.dto.response.dataManagement.task.TaskResponse;
import com.gxa.pipe.pojo.entity.Task;
import com.gxa.pipe.service.dataManagement.TaskService;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    @Override
    public PageResult<TaskResponse> getByPage(TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询数据
        Page<TaskResponse> page = (Page<TaskResponse>) taskMapper.selectByPage(request);

        // 构建分页结果
        PageResult<TaskResponse> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());

        return result;
    }

    @Override
    public TaskResponse getById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public boolean create(TaskAddRequest request) {
        Task task = new Task();
        BeanUtils.copyProperties(request, task);

        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setPublicTime(LocalDateTime.now());

        return taskMapper.insert(task) > 0;
    }

    @Override
    public boolean update(TaskUpdateRequest request) {
        log.info("更新任务，请求参数：{}", request);

        Task task = new Task();
        BeanUtils.copyProperties(request, task);
        task.setUpdateTime(LocalDateTime.now());

        return taskMapper.update(task) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    public int countUnderwayTasks() {
        return taskMapper.countUnderwayTasks(null);
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
        return taskMapper.countCompletedTasks(null);
    }

    @Override
    public TaskIndicatorResponse getTaskIndicators(Long areaId) {
        log.info("获取任务指标卡数据，区域ID：{}", areaId);

        TaskIndicatorResponse response = new TaskIndicatorResponse();
        response.setTotal(String.valueOf(taskMapper.countTotalTasks(areaId)));
        response.setPending(String.valueOf(taskMapper.countPendingTasks(areaId)));
        response.setInProgress(String.valueOf(taskMapper.countUnderwayTasks(areaId)));
        response.setCompleted(String.valueOf(taskMapper.countCompletedTasks(areaId)));
        response.setUrgent(String.valueOf(taskMapper.countUrgentTasks(areaId)));

        return response;
    }
}