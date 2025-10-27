package com.gxa.pipe.dataManagement.equipment;

import java.util.List;

import com.gxa.pipe.dataManagement.equipment.*;

/**
 * 设备信息管理服务接口
 */
public interface EquipmentService {

    /**
     * 根据条件查询设备信息
     * 
     * @param request 查询条件
     * @return 设备信息列表
     */
    List<EquipmentResponse> findEquipmentByConditions(EquipmentQueryRequest request);

    /**
     * 根据ID查询设备信息
     * 
     * @param id 设备ID
     * @return 设备信息列表（单个元素）
     */
    List<EquipmentResponse> findEquipmentById(String id);

    /**
     * 添加设备信息
     * 
     * @param request 添加请求
     * @return 是否成功
     */
    boolean addEquipment(EquipmentAddRequest request);

    /**
     * 更新设备信息
     * 
     * @param request 更新请求
     * @return 是否成功
     */
    boolean updateEquipment(EquipmentUpdateRequest request);

    /**
     * 删除设备信息
     * 
     * @param ids 设备ID字符串（逗号分隔）
     * @return 是否成功
     */
    boolean deleteEquipment(String ids);

    /**
     * 获取设备指标卡数据
     * 
     * @return 设备指标响应
     */
    EquipmentIndicatorResponse getEquipmentIndicator();
}
