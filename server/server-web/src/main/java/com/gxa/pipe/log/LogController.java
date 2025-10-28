package com.gxa.pipe.log;

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
     * 查询日志（非id）
     * 查询日志信息 支持条件查询 通过非日志id的参数
     * 进入页面时触发一次 点击搜索按钮时触发一次
     */
    @PostMapping("/find_logs_params")
    public Result<PageResult<LogQueryResponse>> queryLogs(@Valid @RequestBody LogQueryRequest request) {
        log.info("分页查询日志，请求参数：{}", request);

        try {
            PageResult<LogQueryResponse> result = logService.queryLogs(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询日志失败：{}", e.getMessage(), e);
            return Result.error("查询日志失败");
        }
    }

    /**
     * 查询日志信息（id）
     * 通过日志ID查询日志详细信息
     */
    @PostMapping("/find_logs_id")
    public Result<LogQueryResponse> getLogById(@RequestParam("id") Long id) {
        log.info("根据ID查询日志详细信息，日志ID：{}", id);

        try {
            LogQueryResponse result = logService.getLogById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("参数错误：{}", e.getMessage());
            return Result.error("参数错误：" + e.getMessage());
        } catch (RuntimeException e) {
            log.warn("查询失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("根据ID查询日志失败：{}", e.getMessage(), e);
            return Result.error("查询日志失败");
        }
    }

    /**
     * 获取日志指标卡
     * 获取日志记录的统计指标
     */
    @GetMapping("/indicator_card")
    public Result<LogIndicardResponse> getLogIndicatorCard(
            @RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取日志指标卡，区域ID：{}", areaId);

        try {
            LogIndicardResponse result = logService.getLogIndicatorCard(areaId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取日志指标卡失败：{}", e.getMessage(), e);
            return Result.error("获取日志指标卡失败");
        }
    }
}