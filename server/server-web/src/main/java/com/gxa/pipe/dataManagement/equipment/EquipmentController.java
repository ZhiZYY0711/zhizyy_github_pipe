package com.gxa.pipe.dataManagement.equipment;

import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data_management/equipment")
@RequiredArgsConstructor
@Slf4j
public class EquipmentController {

  private final EquipmentService equipmentService;

  @PostMapping("/find_equipment_params")
  public Result<List<EquipmentResponse>> findEquipmentParams(@RequestBody EquipmentQueryRequest request) {
    log.info("查询设备信息，请求参数：{}", request);
    return Result.success(equipmentService.findEquipmentByConditions(request));
  }

  @PostMapping("/find_equipment_id")
  public Result<List<EquipmentResponse>> findEquipmentById(@RequestBody EquipmentIdRequest request) {
    log.info("按ID查询设备信息，请求参数：{}", request);
    return Result.success(equipmentService.findEquipmentById(request.getId()));
  }

  @PostMapping("/add_equipment")
  public Result<Object> addEquipment(@RequestBody EquipmentAddRequest request) {
    log.info("新增设备信息，请求参数：{}", request);

    boolean success = equipmentService.addEquipment(request);
    return success ? Result.success() : Result.error("添加设备信息失败");
  }

  @PostMapping("/update_equipment")
  public Result<Object> updateEquipment(@RequestBody EquipmentUpdateRequest request) {
    log.info("修改设备信息，请求参数：{}", request);

    boolean success = equipmentService.updateEquipment(request);
    return success ? Result.success() : Result.error("修改设备信息失败");
  }

  @GetMapping("/delete_equipment/{ids}")
  public Result<Object> deleteEquipment(@PathVariable String ids) {
    log.info("删除设备信息，请求参数：{}", ids);

    boolean success = equipmentService.deleteEquipment(ids);
    return success ? Result.success() : Result.error("删除设备信息失败");
  }

  @GetMapping("/Indicator_card")
  public Result<EquipmentIndicatorResponse> getIndicatorCard(@RequestParam(required = false) String area_id) {
    log.info("获取设备指标卡，区域ID：{}", area_id);
    return Result.success(equipmentService.getEquipmentIndicator());
  }
}
