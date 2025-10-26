package com.gxa.pipe.controller;

import com.gxa.pipe.pojo.entity.MonitoringData;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import com.gxa.pipe.service.MonitoringDataService;
import com.gxa.pipe.utils.Result;
import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 监控数据控制器
 */
@RestController
@RequestMapping("/monitoring-data")
@RequiredArgsConstructor
@Slf4j
public class MonitoringDataController {
    
    private final MonitoringDataService monitoringDataService;
    
    /**
     * 分页查询监控数据
     */
    @GetMapping("/page")
    public Result<PageResult<MonitoringData>> getMonitoringDataByPage(@Valid MonitoringDataQueryRequest request) {
        log.info("分页查询监控数据，请求参数：{}", request);
        
        try {
            PageResult<MonitoringData> result = monitoringDataService.getByPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询监控数据失败：{}", e.getMessage());
            return Result.error("查询监控数据失败");
        }
    }
    
    /**
     * 根据ID查询监控数据
     */
    @GetMapping("/{id}")
    public Result<MonitoringData> getMonitoringDataById(@PathVariable Long id) {
        log.info("根据ID查询监控数据，ID：{}", id);
        
        try {
            MonitoringData data = monitoringDataService.getById(id);
            if (data == null) {
                return Result.error("监控数据不存在");
            }
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询监控数据失败：{}", e.getMessage());
            return Result.error("查询监控数据失败");
        }
    }
    
    /**
     * 创建监控数据
     */
    @PostMapping
    public Result<String> createMonitoringData(@Valid @RequestBody MonitoringData data) {
        log.info("创建监控数据，数据信息：{}", data);
        
        try {
            boolean success = monitoringDataService.create(data);
            if (success) {
                return Result.success("监控数据创建成功");
            } else {
                return Result.error("监控数据创建失败");
            }
        } catch (Exception e) {
            log.error("创建监控数据失败：{}", e.getMessage());
            return Result.error("创建监控数据失败");
        }
    }
    
    /**
     * 更新监控数据
     */
    @PutMapping("/{id}")
    public Result<String> updateMonitoringData(@PathVariable Long id, @Valid @RequestBody MonitoringData data) {
        log.info("更新监控数据，ID：{}，数据信息：{}", id, data);
        
        try {
            data.setId(id);
            boolean success = monitoringDataService.update(data);
            if (success) {
                return Result.success("监控数据更新成功");
            } else {
                return Result.error("监控数据更新失败");
            }
        } catch (Exception e) {
            log.error("更新监控数据失败：{}", e.getMessage());
            return Result.error("更新监控数据失败");
        }
    }
    
    /**
     * 删除监控数据
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteMonitoringData(@PathVariable Long id) {
        log.info("删除监控数据，ID：{}", id);
        
        try {
            boolean success = monitoringDataService.delete(id);
            if (success) {
                return Result.success("监控数据删除成功");
            } else {
                return Result.error("监控数据删除失败");
            }
        } catch (Exception e) {
            log.error("删除监控数据失败：{}", e.getMessage());
            return Result.error("删除监控数据失败");
        }
    }
}