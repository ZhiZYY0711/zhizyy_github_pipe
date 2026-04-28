package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("data_visualization/data_monitoring")
@RequiredArgsConstructor
@Slf4j
public class DataMonitoringController {

  private final DataMonitoringService dataMonitoringService;

  @PostMapping("/area_data")
  public Result<List<AreaDetailResponse>> getAreaDetails(@RequestBody AreaDetailRequest request) {
    log.info("获取区域四维度数据，请求参数：{}", request);
    return Result.success(dataMonitoringService.getAreaDetails(request));
  }

  @PostMapping("/pipe_data")
  public Result<List<PipeDetailResponse>> getPipeDetails(@RequestBody PipeDetailRequest request) {
    log.info("获取管道四维度数据，请求参数：{}", request);
    return Result.success(dataMonitoringService.getPipeDetails(request));
  }

  @GetMapping("/key_indicators")
  public Result<PipeIndicatorResponse> getPipeKeyIndicators(
      @RequestParam(required = false) String id,
      @RequestParam(value = "segment_id", required = false) String segmentId,
      @RequestParam(value = "segment_ids", required = false) String segmentIds) {
    log.info("获取管道关键指标卡，管道ID：{}，管段ID：{}，连续管段ID：{}", id, segmentId, segmentIds);
    return Result.success(dataMonitoringService.getPipeKeyIndicators(id, segmentId, segmentIds));
  }
}
