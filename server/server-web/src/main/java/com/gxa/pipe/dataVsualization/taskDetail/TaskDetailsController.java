package com.gxa.pipe.dataVsualization.taskDetail;

import com.gxa.pipe.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 任务详情控制器
 */
@RestController
@RequestMapping("/data_visualization/task_details")
public class TaskDetailsController {
    
    @Autowired
    private TaskDetailsService taskDetailsService;
    
    /**
     * 获取任务状态分布
     * @param request 任务请求参数
     * @return 统一响应结果
     */
    @PostMapping("/task_status")
    public Result<List<TaskStatusResponse>> getTaskStatusDistribution(@RequestBody TaskResquest request) {
        try {
            // 参数校验
            if (request.getAreaId() <= 0) {
                return Result.error(400, "区域ID不能为空或小于等于0");
            }
            
            // 调用业务逻辑层获取数据
            List<TaskStatusResponse> taskStatusList = taskDetailsService.getTaskStatusDistribution(request);
            
            // 返回成功结果
            return Result.success(taskStatusList);
            
        } catch (Exception e) {
            // 异常处理
            return Result.error("获取任务状态分布失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取任务数量趋势
     * @param request 任务请求参数
     * @return 统一响应结果
     */
    @PostMapping("/task_nums")
    public Result<List<TaskNumberResponse>> getTaskNumberTrend(@RequestBody TaskResquest request) {
        try {
            // 参数校验
            if (request.getAreaId() <= 0) {
                return Result.error(400, "区域ID不能为空或小于等于0");
            }
            
            // 时间参数校验
            if (request.getStartTime() > 0 && request.getEndTime() > 0 && request.getStartTime() >= request.getEndTime()) {
                return Result.error(400, "开始时间不能大于等于结束时间");
            }
            
            // 调用业务逻辑层获取数据
            List<TaskNumberResponse> taskNumberList = taskDetailsService.getTaskNumberTrend(request);
            
            // 返回成功结果
            return Result.success(taskNumberList);
            
        } catch (Exception e) {
            // 异常处理
            return Result.error("获取任务数量趋势失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取区域任务对比
     * @param request 任务请求参数
     * @return 统一响应结果
     */
    @PostMapping("/task_contrast")
    public Result<List<AreaTaskResponse>> getAreaTaskContrast(@RequestBody TaskResquest request) {
        try {
            // 参数校验
            if (request.getAreaId() <= 0) {
                return Result.error(400, "区域ID不能为空或小于等于0");
            }
            
            // 区域ID格式校验（必须是6位数字）
            if (request.getAreaId() < 100000 || request.getAreaId() > 999999) {
                return Result.error(400, "区域ID格式错误，必须是6位数字");
            }
            
            // 时间参数校验
            if (request.getStartTime() > 0 && request.getEndTime() > 0 && request.getStartTime() >= request.getEndTime()) {
                return Result.error(400, "开始时间不能大于等于结束时间");
            }
            
            // 调用业务逻辑层获取数据
            List<AreaTaskResponse> areaTaskList = taskDetailsService.getAreaTaskContrast(request);
            
            // 返回成功结果
            return Result.success(areaTaskList);
            
        } catch (Exception e) {
            // 异常处理
            return Result.error("获取区域任务对比失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取KPI榜单
     * @param request KPI榜单请求参数
     * @return 统一响应结果
     */
    @PostMapping("/KPI_list")
    public Result<List<TaskKpiResponse>> getKpiList(@RequestBody TaskKpiRequest request) {
        try {
            // 参数校验
            if (request.getAreaId() <= 0) {
                return Result.error(400, "区域ID不能为空或小于等于0");
            }
            
            // 榜单类型校验
            if (request.getType() < 0 || request.getType() > 2) {
                return Result.error(400, "榜单类型错误，必须是0-2之间的数字");
            }
            
            // 时间参数校验
            if (request.getStartTime() > 0 && request.getEndTime() > 0 && request.getStartTime() >= request.getEndTime()) {
                return Result.error(400, "开始时间不能大于等于结束时间");
            }
            
            // 调用业务逻辑层获取数据
            List<TaskKpiResponse> kpiList = taskDetailsService.getKpiList(request);
            
            // 返回成功结果
            return Result.success(kpiList);
            
        } catch (Exception e) {
            // 异常处理
            return Result.error("获取KPI榜单失败：" + e.getMessage());
        }
    }
}
