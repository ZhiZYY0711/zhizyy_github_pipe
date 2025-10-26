package com.gxa.pipe.controller;

import com.gxa.pipe.pojo.entity.Log;
import com.gxa.pipe.pojo.dto.request.LogQueryRequest;
import com.gxa.pipe.service.LogService;
import com.gxa.pipe.utils.Result;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 日志控制器
 */
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogService logService;

    /**
     * 分页查询日志
     */
    @GetMapping("/page")
    public Result<PageResult<Log>> getLogsByPage(@Valid LogQueryRequest request) {
        log.info("分页查询日志，请求参数：{}", request);

        try {
            PageResult<Log> result = logService.getByPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询日志失败：{}", e.getMessage());
            return Result.error("查询日志失败");
        }
    }

    /**
     * 根据ID查询日志
     */
    @GetMapping("/{id}")
    public Result<Log> getLogById(@PathVariable Long id) {
        log.info("根据ID查询日志，ID：{}", id);

        try {
            Log logData = logService.getById(id);
            if (logData == null) {
                return Result.error("日志不存在");
            }
            return Result.success(logData);
        } catch (Exception e) {
            log.error("查询日志失败：{}", e.getMessage());
            return Result.error("查询日志失败");
        }
    }

    /**
     * 创建日志
     */
    @PostMapping
    public Result<String> createLog(@Valid @RequestBody Log logData) {
        log.info("创建日志，日志信息：{}", logData);

        try {
            boolean success = logService.create(logData);
            if (success) {
                return Result.success("日志创建成功");
            } else {
                return Result.error("日志创建失败");
            }
        } catch (Exception e) {
            log.error("创建日志失败：{}", e.getMessage());
            return Result.error("创建日志失败");
        }
    }

    /**
     * 更新日志
     */
    @PutMapping("/{id}")
    public Result<String> updateLog(@PathVariable Long id, @Valid @RequestBody Log logData) {
        log.info("更新日志，ID：{}，日志信息：{}", id, logData);

        try {
            logData.setId(id);
            boolean success = logService.update(logData);
            if (success) {
                return Result.success("日志更新成功");
            } else {
                return Result.error("日志更新失败");
            }
        } catch (Exception e) {
            log.error("更新日志失败：{}", e.getMessage());
            return Result.error("更新日志失败");
        }
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteLog(@PathVariable Long id) {
        log.info("删除日志，ID：{}", id);

        try {
            boolean success = logService.delete(id);
            if (success) {
                return Result.success("日志删除成功");
            } else {
                return Result.error("日志删除失败");
            }
        } catch (Exception e) {
            log.error("删除日志失败：{}", e.getMessage());
            return Result.error("删除日志失败");
        }
    }
}