package com.gxa.pipe.log;

import com.gxa.pipe.utils.PageResult;
import com.gxa.pipe.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogService logService;

    @PostMapping("/find_logs_params")
    public Result<PageResult<LogQueryResponse>> queryLogs(@Valid @RequestBody LogQueryRequest request) {
        log.info("分页查询日志，请求参数：{}", request);
        return Result.success(logService.queryLogs(request));
    }

    @PostMapping("/find_logs_id")
    public Result<LogQueryResponse> getLogById(@RequestParam("id") Long id) {
        log.info("根据ID查询日志详情，日志ID：{}", id);
        return Result.success(logService.getLogById(id));
    }

    @GetMapping("/indicator_card")
    public Result<LogIndicardResponse> getLogIndicatorCard(
            @RequestParam(value = "area_id", required = false) Long areaId) {
        log.info("获取日志指标卡，区域ID：{}", areaId);
        return Result.success(logService.getLogIndicatorCard(areaId));
    }
}
