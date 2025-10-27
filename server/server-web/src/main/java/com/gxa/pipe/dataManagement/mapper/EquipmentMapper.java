package com.gxa.pipe.dataManagement.mapper;

import com.gxa.pipe.entity.Sensor;
import com.gxa.pipe.pojo.dto.dataManagement.equipment.EquipmentQueryRequest;
import com.gxa.pipe.pojo.vo.dataManagement.equipment.EquipmentResponse;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备信息管理Mapper接口
 */
@Mapper
public interface EquipmentMapper {

    /**
     * 根据条件查询设备信息（关联查询）
     * 
     * @param request 查询条件
     * @return 设备信息列表
     */
    List<EquipmentResponse> selectEquipmentByConditions(EquipmentQueryRequest request);

    /**
     * 根据ID查询设备信息（关联查询）
     * 
     * @param id 设备ID
     * @return 设备信息
     */
    EquipmentResponse selectEquipmentById(@Param("id") Long id);

    /**
     * 插入设备信息
     * 
     * @param sensor 传感器实体
     * @return 影响行数
     */
    int insertEquipment(Sensor sensor);

    /**
     * 更新设备信息
     * 
     * @param sensor 传感器实体
     * @return 影响行数
     */
    int updateEquipment(Sensor sensor);

    /**
     * 根据ID删除设备信息
     * 
     * @param id 设备ID
     * @return 影响行数
     */
    int deleteEquipmentById(@Param("id") Long id);

    /**
     * 批量删除设备信息
     * 
     * @param ids 设备ID列表
     * @return 影响行数
     */
    int deleteEquipmentByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询传感器实体
     * 
     * @param id 设备ID
     * @return 传感器实体
     */
    Sensor selectSensorById(@Param("id") Long id);

    /**
     * 统计设备总数
     * 
     * @return 设备总数
     */
    int countTotal();

    /**
     * 统计正常设备数
     * 
     * @return 正常设备数
     */
    int countNormal();

    /**
     * 统计故障设备数
     * 
     * @return 故障设备数
     */
    int countFault();

    /**
     * 统计维护中设备数
     * 
     * @return 维护中设备数
     */
    int countMaintenance();

    /**
     * 统计离线设备数
     * 
     * @return 离线设备数
     */
    int countOffline();

    /**
     * 验证区域是否存在
     * 
     * @param areaId 区域ID
     * @return 是否存在
     */
    boolean existsAreaById(@Param("areaId") Long areaId);

    /**
     * 验证管道是否存在
     * 
     * @param pipeId 管道ID
     * @return 是否存在
     */
    boolean existsPipeById(@Param("pipeId") Long pipeId);

    /**
     * 验证检修员是否存在
     * 
     * @param repairmanId 检修员ID
     * @return 是否存在
     */
    boolean existsRepairmanById(@Param("repairmanId") Long repairmanId);
}
