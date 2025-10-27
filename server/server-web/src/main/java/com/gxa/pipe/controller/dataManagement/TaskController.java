package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskAddRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskIdRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskQueryRequest;
import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskUpdateRequest;
import com.gxa.pipe.pojo.dto.response.dataManagement.task.TaskIndicatorResponse;
import com.gxa.pipe.pojo.dto.response.dataManagement.task.TaskResponse;
import com.gxa.pipe.pojo.entity.Task;
import com.gxa.pipe.service.dataManagement.TaskService;
import com.gxa.pipe.utils.Result;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/data_management/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    /**
     * 分页查询任务
     */
    @PostMapping("/find_task_params")
    public Result<PageResult<TaskResponse>> getTasksByPage(@Valid @RequestBody TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);

        try {
            PageResult<TaskResponse> result = taskService.getByPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询任务失败：{}", e.getMessage());
            return Result.error("查询任务失败");
        }
    }

    /**
     * 根据ID查询任务
     */
    @PostMapping("/find_task_id")
    public Result<TaskResponse> getTaskById(@Valid @RequestBody TaskIdRequest request) {
        log.info("根据ID查询任务，ID：{}", request.getId());

        try {
            TaskResponse task = taskService.getById(request.getId());
            if (task == null) {
                return Result.error("任务不存在");
            }
            return Result.success(task);
        } catch (Exception e) {
            log.error("查询任务失败：{}", e.getMessage());
            return Result.error("查询任务失败");
        }
    }

    /**
     * 创建任务
     */
    @PostMapping("/add_task")
    public Result<String> createTask(@Valid @RequestBody TaskAddRequest request) {
        log.info("创建任务，任务信息：{}", request);

        try {
            boolean success = taskService.create(request);
            if (success) {
                return Result.success("任务创建成功");
            } else {
                return Result.error("任务创建失败");
            }
        } catch (Exception e) {
            log.error("创建任务失败：{}", e.getMessage());
            return Result.error("创建任务失败");
        }
    }

    /**
     * 更新任务
     */
    @PostMapping("/update_task")
    public Result<String> updateTask(@Valid @RequestBody TaskUpdateRequest request) {
        log.info("更新任务，ID：{}，任务信息：{}", request.getId(), request);

        try {
            boolean success = taskService.update(request);
            if (success) {
                return Result.success("任务更新成功");
            } else {
                return Result.error("任务更新失败");
            }
        } catch (Exception e) {
            log.error("更新任务失败：{}", e.getMessage());
            return Result.error("更新任务失败");
        }
    }

    /**
     * 删除任务
     */
    @GetMapping("/delete_task/{id}")
    public Result<String> deleteTask(@PathVariable Long id) {
        log.info("删除任务，ID：{}", id);

        try {
            boolean success = taskService.delete(id);
            if (success) {
                return Result.success("任务删除成功");
            } else {
                return Result.error("任务删除失败");
            }
        } catch (Exception e) {
            log.error("删除任务失败：{}", e.getMessage());
            return Result.error("删除任务失败");
        }
    }

    /**
     * 获取任务指标卡数据
     */
    @GetMapping("/Indicator_card")
    public Result<TaskIndicatorResponse> getTaskIndicators(
            @RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取任务指标卡数据，区域ID：{}", areaId);

        try {
            TaskIndicatorResponse indicators = taskService.getTaskIndicators(areaId);
            return Result.success(indicators);
        } catch (Exception e) {
            log.error("获取任务指标卡数据失败：{}", e.getMessage());
            return Result.error("获取任务指标卡数据失败");
        }
    }
}