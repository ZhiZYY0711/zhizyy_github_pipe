package com.gxa.pipe.dataVsualization.taskDetail;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data_visualization/task_details")
@RequiredArgsConstructor
@Slf4j
public class TaskDetailsController {

    private final TaskDetailsService taskDetailsService;

    @PostMapping("/task_status")
    public Result<List<TaskStatusResponse>> getTaskStatusDistribution(@RequestBody TaskResquest request) {
        log.info("获取任务状态分布，请求参数：{}", request);

        Result<Void> validation = validateAreaId(request.getAreaId());
        if (validation != null) {
            return Result.error(validation.getCode(), validation.getMessage());
        }

        return Result.success(taskDetailsService.getTaskStatusDistribution(request));
    }

    @PostMapping("/task_nums")
    public Result<List<TaskNumberResponse>> getTaskNumberTrend(@RequestBody TaskResquest request) {
        log.info("获取任务数量趋势，请求参数：{}", request);

        Result<Void> validation = validateTaskRequest(request, false);
        if (validation != null) {
            return Result.error(validation.getCode(), validation.getMessage());
        }

        return Result.success(taskDetailsService.getTaskNumberTrend(request));
    }

    @PostMapping("/task_contrast")
    public Result<List<AreaTaskResponse>> getAreaTaskContrast(@RequestBody TaskResquest request) {
        log.info("获取区域任务对比，请求参数：{}", request);

        Result<Void> validation = validateTaskRequest(request, true);
        if (validation != null) {
            return Result.error(validation.getCode(), validation.getMessage());
        }

        return Result.success(taskDetailsService.getAreaTaskContrast(request));
    }

    @PostMapping("/KPI_list")
    public Result<List<TaskKpiResponse>> getKpiList(@RequestBody TaskKpiRequest request) {
        log.info("获取KPI榜单，请求参数：{}", request);

        Result<Void> areaValidation = validateAreaId(request.getAreaId());
        if (areaValidation != null) {
            return Result.error(areaValidation.getCode(), areaValidation.getMessage());
        }
        if (request.getType() < 0 || request.getType() > 2) {
            return Result.error(400, "榜单类型错误，必须是0-2之间的数字");
        }
        Result<Void> timeValidation = validateTimeRange(request.getStartTime(), request.getEndTime());
        if (timeValidation != null) {
            return Result.error(timeValidation.getCode(), timeValidation.getMessage());
        }

        return Result.success(taskDetailsService.getKpiList(request));
    }

    private Result<Void> validateTaskRequest(TaskResquest request, boolean requireAreaCodeFormat) {
        Result<Void> areaValidation = validateAreaId(request.getAreaId());
        if (areaValidation != null) {
            return areaValidation;
        }
        if (requireAreaCodeFormat && request.getAreaId() != 0 && (request.getAreaId() < 100000 || request.getAreaId() > 999999)) {
            return Result.error(400, "区域ID格式错误，必须是6位数字");
        }
        return validateTimeRange(request.getStartTime(), request.getEndTime());
    }

    private Result<Void> validateAreaId(long areaId) {
        if (areaId < 0) {
            return Result.error(400, "区域ID不能小于0");
        }
        return null;
    }

    private Result<Void> validateTimeRange(long startTime, long endTime) {
        if (startTime > 0 && endTime > 0 && startTime >= endTime) {
            return Result.error(400, "开始时间不能大于等于结束时间");
        }
        return null;
    }
}
