package com.gxa.pipe.dataManagement.task;

import com.gxa.pipe.entity.Task;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    @Override
    public PageResult<TaskResponse> getByPage(TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);

        int pageNum = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;
        int offset = (pageNum - 1) * pageSize;

        Long total = taskMapper.countByPageConditions(request);
        List<TaskResponse> records =
                total != null && total > 0
                        ? taskMapper.selectByPage(request, offset, pageSize)
                        : Collections.emptyList();

        PageResult<TaskResponse> result = new PageResult<>();
        result.setTotal(total != null ? total : 0L);
        result.setRecords(records);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
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
