package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.gxa.pipe.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据监控控制器
 */
@RestController
@RequestMapping("data_visualization/data_monitoring")
public class DataMonitoringController {

  @Autowired
  private DataMonitoringService dataMonitoringService;

  /**
   * 获取区域四维度数据
   * 
   * @param request 查询请求参数
   * @return 区域四维度数据列表
   */
  @PostMapping("/area_data")
  public Result<List<AreaDetailResponse>> getAreaDetails(@RequestBody AreaDetailRequest request) {
    try {
      List<AreaDetailResponse> data = dataMonitoringService.getAreaDetails(request);
      return Result.success(data);
    } catch (IllegalArgumentException e) {
      return Result.error(400, e.getMessage());
    } catch (Exception e) {
      return Result.error("查询区域四维度数据失败：" + e.getMessage());
    }
  }

  /**
   * 获取管道四维度数据
   * 
   * @param request 查询请求参数
   * @return 管道四维度数据列表
   */
  @PostMapping("/pipe_data")
  public Result<List<PipeDetailResponse>> getPipeDetails(@RequestBody PipeDetailRequest request) {
    try {
      List<PipeDetailResponse> data = dataMonitoringService.getPipeDetails(request);
      return Result.success(data);
    } catch (IllegalArgumentException e) {
      return Result.error(400, e.getMessage());
    } catch (Exception e) {
      return Result.error("查询管道四维度数据失败：" + e.getMessage());
    }
  }

  /**
   * 获取管道关键指标卡数据
   * 
   * @param id 管道的唯一标识
   * @return 管道关键指标卡数据
   */
  @GetMapping("/key_indicators")
  public Result<PipeIndicatorResponse> getPipeKeyIndicators(@RequestParam(required = false) String id) {
    try {
      PipeIndicatorResponse data = dataMonitoringService.getPipeKeyIndicators(id);
      return Result.success(data);
    } catch (IllegalArgumentException e) {
      return Result.error(400, e.getMessage());
    } catch (Exception e) {
      return Result.error("查询管道关键指标卡数据失败：" + e.getMessage());
    }
  }
}
