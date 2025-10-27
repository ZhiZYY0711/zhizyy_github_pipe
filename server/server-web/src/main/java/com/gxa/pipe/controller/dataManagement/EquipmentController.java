package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.dto.dataManagement.equipment.*;
import com.gxa.pipe.pojo.vo.dataManagement.equipment.*;
import com.gxa.pipe.pojo.vo.CommonResponse;
import com.gxa.pipe.service.dataManagement.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备信息管理控制器
 */
@RestController
@RequestMapping("/data_management/equipment")
@CrossOrigin
public class EquipmentController {

  @Autowired
  private EquipmentService equipmentService;

  /**
   * 查询设备信息（非id）
   * 查询设备信息 支持条件查询 通过非设备id的参数
   * 进入页面时触发一次 点击搜索按钮时触发一次
   */
  @PostMapping("/find_equipment_params")
  public CommonResponse<List<EquipmentResponse>> findEquipmentParams(@RequestBody EquipmentQueryRequest request) {
    try {
      List<EquipmentResponse> equipmentList = equipmentService.findEquipmentByConditions(request);
      return CommonResponse.success(equipmentList);
    } catch (Exception e) {
      return CommonResponse.error("查询设备信息失败：" + e.getMessage());
    }
  }

  /**
   * 查询设备信息（id）
   * 查询设备信息 通过设备的唯一标识
   * 点击搜索按钮时触发一次
   * 返回结果正常情况应为一个
   */
  @PostMapping("/find_equipment_id")
  public CommonResponse<List<EquipmentResponse>> findEquipmentById(@RequestBody EquipmentIdRequest request) {
    try {
      List<EquipmentResponse> equipment = equipmentService.findEquipmentById(request.getId());
      return CommonResponse.success(equipment);
    } catch (Exception e) {
      return CommonResponse.error("查询设备信息失败：" + e.getMessage());
    }
  }

  /**
   * 插入设备信息
   * 插入新设备的信息
   */
  @PostMapping("/add_equipment")
  public CommonResponse<Object> addEquipment(@RequestBody EquipmentAddRequest request) {
    try {
      equipmentService.addEquipment(request);
      return CommonResponse.success();
    } catch (Exception e) {
      return CommonResponse.error("添加设备信息失败：" + e.getMessage());
    }
  }

  /**
   * 修改设备信息
   * 修改设备的信息
   */
  @PostMapping("/update_equipment")
  public CommonResponse<Object> updateEquipment(@RequestBody EquipmentUpdateRequest request) {
    try {
      equipmentService.updateEquipment(request);
      return CommonResponse.success();
    } catch (Exception e) {
      return CommonResponse.error("修改设备信息失败：" + e.getMessage());
    }
  }

  /**
   * 删除设备信息
   * 删除设备信息
   */
  @GetMapping("/delete_equipment/{ids}")
  public CommonResponse<Object> deleteEquipment(@PathVariable String ids) {
    try {
      equipmentService.deleteEquipment(ids);
      return CommonResponse.success();
    } catch (Exception e) {
      return CommonResponse.error("删除设备信息失败：" + e.getMessage());
    }
  }

  /**
   * 设备信息指标卡
   * 获取设备信息的指标卡 进入页面时触发一次
   */
  @GetMapping("/Indicator_card")
  public CommonResponse<EquipmentIndicatorResponse> getIndicatorCard(@RequestParam(required = false) String area_id) {
    try {
      EquipmentIndicatorResponse response = equipmentService.getEquipmentIndicator();
      return CommonResponse.success(response);
    } catch (Exception e) {
      return CommonResponse.error("获取设备指标卡失败：" + e.getMessage());
    }
  }
}
