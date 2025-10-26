package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.dto.request.dataManagement.task.TaskQueryRequest;
import com.gxa.pipe.pojo.entity.Task;
import com.gxa.pipe.service.TaskService;
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
@RequestMapping("/task")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    /**
     * 分页查询任务
     */
    @GetMapping("/page")
    public Result<PageResult<Task>> getTasksByPage(@Valid TaskQueryRequest request) {
        log.info("分页查询任务，请求参数：{}", request);

        try {
            PageResult<Task> result = taskService.getByPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询任务失败：{}", e.getMessage());
            return Result.error("查询任务失败");
        }
    }

    /**
     * 根据ID查询任务
     */
    @GetMapping("/{id}")
    public Result<Task> getTaskById(@PathVariable Long id) {
        log.info("根据ID查询任务，ID：{}", id);

        try {
            Task task = taskService.getById(id);
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
    @PostMapping
    public Result<String> createTask(@Valid @RequestBody Task task) {
        log.info("创建任务，任务信息：{}", task);

        try {
            boolean success = taskService.create(task);
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
    @PutMapping("/{id}")
    public Result<String> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        log.info("更新任务，ID：{}，任务信息：{}", id, task);

        try {
            task.setId(id);
            boolean success = taskService.update(task);
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
    @DeleteMapping("/{id}")
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
}