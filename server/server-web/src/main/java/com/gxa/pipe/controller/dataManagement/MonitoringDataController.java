package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.dto.request.MonitoringDataAddRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataByIdRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataQueryRequest;
import com.gxa.pipe.pojo.dto.request.MonitoringDataUpdateRequest;
import com.gxa.pipe.pojo.dto.response.MonitoringDataIndicatorResponse;
import com.gxa.pipe.pojo.dto.response.MonitoringDataQueryResponse;
import com.gxa.pipe.service.MonitoringDataService;
import com.gxa.pipe.utils.PageResult;
import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 监测数据控制器
 */
@RestController
@RequestMapping("/data_management/monitoring_data")
@RequiredArgsConstructor
@Slf4j
public class MonitoringDataController {

  private final MonitoringDataService monitoringDataService;

  /**
   * 查询监测数据（非id）
   * 
   * @param request 查询请求
   * @return 分页查询结果
   */
  @PostMapping("/find_data_params")
  public Result<PageResult<MonitoringDataQueryResponse>> queryMonitoringData(
      @RequestBody MonitoringDataQueryRequest request) {
    log.info("查询监测数据，请求参数：{}", request);

    PageResult<MonitoringDataQueryResponse> result = monitoringDataService.getByPage(request);

    return Result.success(result);
  }

  /**
   * 查询监测数据（通过id）
   * 
   * @param request 查询请求
   * @return 查询结果
   */
  @PostMapping("/find_data_id")
  public Result<List<MonitoringDataQueryResponse>> queryMonitoringDataById(
      @RequestBody MonitoringDataByIdRequest request) {
    log.info("通过ID查询监测数据，请求参数：{}", request);

    List<MonitoringDataQueryResponse> result = monitoringDataService.getById(request);

    return Result.success(result);
  }

  /**
   * 插入监测数据
   * 
   * @param request 添加请求
   * @return 操作结果
   */
  @PostMapping("/add_data")
  public Result<Object> addMonitoringData(@RequestBody MonitoringDataAddRequest request) {
    log.info("插入监测数据，请求参数：{}", request);

    monitoringDataService.addData(request);

    return Result.success();
  }

  /**
   * 修改监测数据
   * 
   * @param request 修改请求
   * @return 操作结果
   */
  @PostMapping("/update_data")
  public Result<Object> updateMonitoringData(@RequestBody MonitoringDataUpdateRequest request) {
    log.info("修改监测数据，请求参数：{}", request);

    monitoringDataService.updateData(request);

    return Result.success();
  }

  /**
   * 删除监测数据
   * 
   * @param ids 删除的监测数据的id数组
   * @return 操作结果
   */
  @GetMapping("/delete_data/{ids}")
  public Result<Object> deleteMonitoringData(@PathVariable String ids) {
    log.info("删除监测数据，ids：{}", ids);

    monitoringDataService.deleteData(ids);

    return Result.success();
  }

  /**
   * 监测数据指标卡
   * 
   * @param areaId 区域id（可选）
   * @return 指标卡数据
   */
  @GetMapping("/Indicator_card")
  public Result<MonitoringDataIndicatorResponse> getIndicatorCard(
      @RequestParam(required = false) String areaId) {
    log.info("获取监测数据指标卡，区域ID：{}", areaId);

    MonitoringDataIndicatorResponse result = monitoringDataService.getIndicatorCard(areaId);

    return Result.success(result);
  }
}
