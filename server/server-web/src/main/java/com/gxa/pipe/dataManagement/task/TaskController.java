package com.gxa.pipe.dataManagement.task;

import com.gxa.pipe.utils.PageResult;
import com.gxa.pipe.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data_management/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/find_task_params")
    public Result<PageResult<TaskResponse>> getTasksByPage(@Valid @RequestBody TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);
        return Result.success(taskService.getByPage(request));
    }

    @PostMapping("/find_task_id")
    public Result<TaskResponse> getTaskById(@Valid @RequestBody TaskIdRequest request) {
        log.info("根据ID查询任务，ID：{}", request.getId());

        TaskResponse task = taskService.getById(request.getId());
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(task);
    }

    @PostMapping("/add_task")
    public Result<String> createTask(@Valid @RequestBody TaskAddRequest request) {
        log.info("创建任务，任务信息：{}", request);

        boolean success = taskService.create(request);
        return success ? Result.success("任务创建成功") : Result.error("任务创建失败");
    }

    @PostMapping("/update_task")
    public Result<String> updateTask(@Valid @RequestBody TaskUpdateRequest request) {
        log.info("更新任务，ID：{}，任务信息：{}", request.getId(), request);

        boolean success = taskService.update(request);
        return success ? Result.success("任务更新成功") : Result.error("任务更新失败");
    }

    @GetMapping("/delete_task/{id}")
    public Result<String> deleteTask(@PathVariable Long id) {
        log.info("删除任务，ID：{}", id);

        boolean success = taskService.delete(id);
        return success ? Result.success("任务删除成功") : Result.error("任务删除失败");
    }

    @GetMapping("/Indicator_card")
    public Result<TaskIndicatorResponse> getTaskIndicators(
            @RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取任务指标卡数据，区域ID：{}", areaId);
        return Result.success(taskService.getTaskIndicators(areaId));
    }
}
