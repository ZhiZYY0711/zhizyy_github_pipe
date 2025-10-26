package com.gxa.pipe.controller.dataManagement;

import com.gxa.pipe.pojo.dto.request.dataManagement.equipment.*;
import com.gxa.pipe.pojo.dto.response.dataManagement.equipment.*;
import com.gxa.pipe.pojo.dto.response.CommonResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备信息管理控制器
 */
@RestController
@RequestMapping("/data_management/equipment")
@CrossOrigin
public class EquipmentController {

  /**
   * 查询设备信息（非id）
   * 查询设备信息 支持条件查询 通过非设备id的参数
   * 进入页面时触发一次 点击搜索按钮时触发一次
   */
  @PostMapping("/find_equipment_params")
  public CommonResponse<List<EquipmentResponse>> findEquipmentParams(@RequestBody EquipmentQueryRequest request) {
    try {
      // 1. 参数验证
      if (request.getPage() == null || request.getPageSize() == null) {
        return CommonResponse.error("页码和每页数据量不能为空");
      }

      // 2. 构建查询条件
      // 根据传入的参数构建SQL查询条件
      // 需要关联查询 sensor, area, pipe, repairman 表

      // 3. 执行查询
      List<EquipmentResponse> equipmentList = new ArrayList<>();

      // 示例数据（实际应从数据库查询）
      EquipmentResponse equipment = new EquipmentResponse();
      equipment.setId("1");
      equipment.setType("压力传感器");
      equipment.setAreaName("北京市朝阳区");
      equipment.setPipeName("京津管道");
      equipment.setStatus("正常");
      equipment.setLastCheck("2024-01-15 10:30:00");
      equipment.setResponsible("张三");
      equipmentList.add(equipment);

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
      // TODO: 实现具体的查询逻辑
      // 1. 参数验证
      if (request.getId() == null || request.getId().trim().isEmpty()) {
        return CommonResponse.error("设备ID不能为空");
      }

      // 2. 根据ID查询设备信息
      // 需要关联查询 sensor, area, pipe, repairman 表

      List<EquipmentResponse> equipmentList = new ArrayList<>();

      // 示例数据（实际应从数据库查询）
      EquipmentResponse equipment = new EquipmentResponse();
      equipment.setId(request.getId());
      equipment.setType("压力传感器");
      equipment.setAreaName("北京市朝阳区");
      equipment.setPipeName("京津管道");
      equipment.setStatus("正常");
      equipment.setLastCheck("2024-01-15 10:30:00");
      equipment.setResponsible("张三");
      equipmentList.add(equipment);

      return CommonResponse.success(equipmentList);
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
      // TODO: 实现具体的插入逻辑
      // 1. 参数验证
      if (request.getType() == null || request.getType().trim().isEmpty()) {
        return CommonResponse.error("设备类型不能为空");
      }
      if (request.getAreaId() == null || request.getAreaId().trim().isEmpty()) {
        return CommonResponse.error("安装位置不能为空");
      }
      if (request.getPipeId() == null || request.getPipeId().trim().isEmpty()) {
        return CommonResponse.error("所属管道不能为空");
      }
      if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
        return CommonResponse.error("设备状态不能为空");
      }
      if (request.getResponsible() == null || request.getResponsible().trim().isEmpty()) {
        return CommonResponse.error("负责人不能为空");
      }

      // 2. 验证关联数据是否存在
      // 验证 area_id 是否存在
      // 验证 pipe_id 是否存在
      // 验证 responsible（检修员）是否存在

      // 3. 插入设备信息到 sensor 表
      // INSERT INTO sensor (area_id, pipeline_id, repairman_id, status, type,
      // create_time, update_time)

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
      // TODO: 实现具体的更新逻辑
      // 1. 参数验证
      if (request.getId() == null || request.getId().trim().isEmpty()) {
        return CommonResponse.error("设备ID不能为空");
      }

      // 2. 验证设备是否存在
      // SELECT * FROM sensor WHERE id = ?

      // 3. 验证关联数据是否存在（如果有更新）
      // 验证 area_id, pipe_id, responsible

      // 4. 更新设备信息
      // UPDATE sensor SET ... WHERE id = ?

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
      // TODO: 实现具体的删除逻辑
      // 1. 参数验证
      if (ids == null || ids.trim().isEmpty()) {
        return CommonResponse.error("设备ID不能为空");
      }

      // 2. 解析ID数组
      String[] idArray = ids.split(",");

      // 3. 验证设备是否存在
      // 4. 检查是否有关联数据（如监测数据、任务等）
      // 5. 执行删除操作
      // DELETE FROM sensor WHERE id IN (?)

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
      // TODO: 实现具体的统计逻辑
      // 1. 构建查询条件
      // 如果有 area_id，则只统计该区域的设备

      // 2. 统计各种状态的设备数量
      // SELECT
      // COUNT(*) as total,
      // COUNT(CASE WHEN status = 0 THEN 1 END) as normal,
      // COUNT(CASE WHEN status = 1 THEN 1 END) as fault,
      // COUNT(CASE WHEN status = 3 THEN 1 END) as maintenance, -- 需要添加维护中状态
      // COUNT(CASE WHEN status = 2 THEN 1 END) as offline
      // FROM sensor
      // WHERE (area_id = ? OR ? IS NULL)

      EquipmentIndicatorResponse response = new EquipmentIndicatorResponse();
      response.setTotal("100");
      response.setNormal("85");
      response.setFault("5");
      response.setMaintenance("3");
      response.setOffline("7");

      return CommonResponse.success(response);
    } catch (Exception e) {
      return CommonResponse.error("获取设备指标卡失败：" + e.getMessage());
    }
  }
}
