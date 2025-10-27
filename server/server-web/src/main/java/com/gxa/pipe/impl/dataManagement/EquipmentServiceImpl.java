package com.gxa.pipe.impl.dataManagement;

import com.gxa.pipe.mapper.dataManagement.EquipmentMapper;

import com.gxa.pipe.pojo.dto.dataManagement.equipment.EquipmentAddRequest;
import com.gxa.pipe.pojo.dto.dataManagement.equipment.EquipmentQueryRequest;
import com.gxa.pipe.pojo.dto.dataManagement.equipment.EquipmentUpdateRequest;

import com.gxa.pipe.pojo.vo.dataManagement.equipment.EquipmentIndicatorResponse;
import com.gxa.pipe.pojo.vo.dataManagement.equipment.EquipmentResponse;
import com.gxa.pipe.pojo.entity.Sensor;
import com.gxa.pipe.service.dataManagement.EquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备信息管理服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EquipmentResponse> findEquipmentByConditions(EquipmentQueryRequest request) {
        try {
            log.info("根据条件查询设备信息，查询条件：{}", request);

            // 直接查询设备信息并返回EquipmentResponse
            return equipmentMapper.selectEquipmentByConditions(request);
        } catch (Exception e) {
            log.error("根据条件查询设备信息失败", e);
            throw new RuntimeException("查询设备信息失败：" + e.getMessage());
        }
    }

    @Override
    public List<EquipmentResponse> findEquipmentById(String id) {
        try {
            log.info("根据ID查询设备信息，设备ID：{}", id);

            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("设备ID不能为空");
            }

            Long equipmentId = Long.parseLong(id);
            EquipmentResponse equipmentResponse = equipmentMapper.selectEquipmentById(equipmentId);

            if (equipmentResponse == null) {
                return new ArrayList<>();
            }

            List<EquipmentResponse> result = new ArrayList<>();
            result.add(equipmentResponse);
            return result;
        } catch (NumberFormatException e) {
            log.error("设备ID格式错误：{}", id);
            throw new IllegalArgumentException("设备ID格式错误");
        } catch (Exception e) {
            log.error("根据ID查询设备信息失败", e);
            throw new RuntimeException("查询设备信息失败：" + e.getMessage());
        }
    }

    @Override
    public boolean addEquipment(EquipmentAddRequest request) {
        try {
            log.info("添加设备信息，请求参数：{}", request);

            // 参数验证
            validateAddRequest(request);

            // 验证关联数据是否存在
            validateRelatedData(Long.parseLong(request.getAreaId()),
                    Long.parseLong(request.getPipeId()),
                    Long.parseLong(request.getResponsible()));

            // 构建传感器实体
            Sensor sensor = buildSensorFromAddRequest(request);

            // 插入数据库
            int result = equipmentMapper.insertEquipment(sensor);

            log.info("添加设备信息完成，影响行数：{}", result);
            return result > 0;
        } catch (Exception e) {
            log.error("添加设备信息失败", e);
            throw new RuntimeException("添加设备信息失败：" + e.getMessage());
        }
    }

    @Override
    public boolean updateEquipment(EquipmentUpdateRequest request) {
        try {
            log.info("更新设备信息，请求参数：{}", request);

            // 参数验证
            validateUpdateRequest(request);

            Long equipmentId = Long.parseLong(request.getId());

            // 验证设备是否存在
            Sensor existingSensor = equipmentMapper.selectSensorById(equipmentId);
            if (existingSensor == null) {
                throw new IllegalArgumentException("设备不存在");
            }

            // 验证关联数据是否存在（如果有更新）
            if (StringUtils.hasText(request.getAreaId()) ||
                    StringUtils.hasText(request.getPipeId()) ||
                    StringUtils.hasText(request.getResponsible())) {

                Long areaId = StringUtils.hasText(request.getAreaId()) ? Long.parseLong(request.getAreaId())
                        : existingSensor.getAreaId();
                Long pipeId = StringUtils.hasText(request.getPipeId()) ? Long.parseLong(request.getPipeId())
                        : existingSensor.getPipelineId();
                Long repairmanId = StringUtils.hasText(request.getResponsible())
                        ? Long.parseLong(request.getResponsible())
                        : existingSensor.getRepairmanId();

                validateRelatedData(areaId, pipeId, repairmanId);
            }

            // 构建更新的传感器实体
            Sensor sensor = buildSensorFromUpdateRequest(request, existingSensor);

            // 更新数据库
            int result = equipmentMapper.updateEquipment(sensor);

            log.info("更新设备信息完成，影响行数：{}", result);
            return result > 0;
        } catch (Exception e) {
            log.error("更新设备信息失败", e);
            throw new RuntimeException("更新设备信息失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteEquipment(String ids) {
        try {
            log.info("删除设备信息，设备IDs：{}", ids);

            if (!StringUtils.hasText(ids)) {
                throw new IllegalArgumentException("设备ID不能为空");
            }

            // 解析ID数组
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // 验证设备是否存在
            for (Long id : idList) {
                Sensor sensor = equipmentMapper.selectSensorById(id);
                if (sensor == null) {
                    throw new IllegalArgumentException("设备ID " + id + " 不存在");
                }
            }

            // 执行删除操作
            int result = equipmentMapper.deleteEquipmentByIds(idList);

            log.info("删除设备信息完成，影响行数：{}", result);
            return result > 0;
        } catch (NumberFormatException e) {
            log.error("设备ID格式错误：{}", ids);
            throw new IllegalArgumentException("设备ID格式错误");
        } catch (Exception e) {
            log.error("删除设备信息失败", e);
            throw new RuntimeException("删除设备信息失败：" + e.getMessage());
        }
    }

    @Override
    public EquipmentIndicatorResponse getEquipmentIndicator() {
        try {
            log.info("获取设备指标卡数据");

            int total = equipmentMapper.countTotal();
            int normal = equipmentMapper.countNormal();
            int fault = equipmentMapper.countFault();
            int maintenance = equipmentMapper.countMaintenance();
            int offline = equipmentMapper.countOffline();

            EquipmentIndicatorResponse response = new EquipmentIndicatorResponse();
            response.setTotal(String.valueOf(total));
            response.setNormal(String.valueOf(normal));
            response.setFault(String.valueOf(fault));
            response.setMaintenance(String.valueOf(maintenance));
            response.setOffline(String.valueOf(offline));

            log.info("获取设备指标卡数据完成：{}", response);
            return response;
        } catch (Exception e) {
            log.error("获取设备指标卡数据失败", e);
            throw new RuntimeException("获取设备指标卡数据失败：" + e.getMessage());
        }
    }

    /**
     * 验证添加请求参数
     */
    private void validateAddRequest(EquipmentAddRequest request) {
        if (!StringUtils.hasText(request.getType())) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (!StringUtils.hasText(request.getAreaId())) {
            throw new IllegalArgumentException("安装位置不能为空");
        }
        if (!StringUtils.hasText(request.getPipeId())) {
            throw new IllegalArgumentException("所属管道不能为空");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            throw new IllegalArgumentException("设备状态不能为空");
        }
        if (!StringUtils.hasText(request.getResponsible())) {
            throw new IllegalArgumentException("负责人不能为空");
        }
    }

    /**
     * 验证更新请求参数
     */
    private void validateUpdateRequest(EquipmentUpdateRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
    }

    /**
     * 验证关联数据是否存在
     */
    private void validateRelatedData(Long areaId, Long pipeId, Long repairmanId) {
        if (!equipmentMapper.existsAreaById(areaId)) {
            throw new IllegalArgumentException("区域不存在");
        }
        if (!equipmentMapper.existsPipeById(pipeId)) {
            throw new IllegalArgumentException("管道不存在");
        }
        if (!equipmentMapper.existsRepairmanById(repairmanId)) {
            throw new IllegalArgumentException("检修员不存在");
        }
    }

    /**
     * 从添加请求构建传感器实体
     */
    private Sensor buildSensorFromAddRequest(EquipmentAddRequest request) {
        Sensor sensor = new Sensor();
        sensor.setAreaId(Long.parseLong(request.getAreaId()));
        sensor.setPipelineId(Long.parseLong(request.getPipeId()));
        sensor.setRepairmanId(Long.parseLong(request.getResponsible()));
        sensor.setType(parseTypeFromString(request.getType()));
        sensor.setStatus(parseStatusFromString(request.getStatus()));
        sensor.setCreateTime(LocalDateTime.now());
        sensor.setUpdateTime(LocalDateTime.now());
        return sensor;
    }

    /**
     * 从更新请求构建传感器实体
     */
    private Sensor buildSensorFromUpdateRequest(EquipmentUpdateRequest request, Sensor existingSensor) {
        Sensor sensor = new Sensor();
        sensor.setId(Long.parseLong(request.getId()));

        // 只更新非空字段
        sensor.setAreaId(StringUtils.hasText(request.getAreaId()) ? Long.parseLong(request.getAreaId())
                : existingSensor.getAreaId());
        sensor.setPipelineId(StringUtils.hasText(request.getPipeId()) ? Long.parseLong(request.getPipeId())
                : existingSensor.getPipelineId());
        sensor.setRepairmanId(StringUtils.hasText(request.getResponsible()) ? Long.parseLong(request.getResponsible())
                : existingSensor.getRepairmanId());
        sensor.setType(StringUtils.hasText(request.getType()) ? parseTypeFromString(request.getType())
                : existingSensor.getType());
        sensor.setStatus(StringUtils.hasText(request.getStatus()) ? parseStatusFromString(request.getStatus())
                : existingSensor.getStatus());

        // 处理检修时间
        if (StringUtils.hasText(request.getLastOverhaulTime())) {
            sensor.setLastOverhaulTime(LocalDateTime.parse(request.getLastOverhaulTime(), DATE_TIME_FORMATTER));
        } else {
            sensor.setLastOverhaulTime(existingSensor.getLastOverhaulTime());
        }

        sensor.setCreateTime(existingSensor.getCreateTime());
        sensor.setUpdateTime(LocalDateTime.now());
        return sensor;
    }

    /**
     * 解析设备类型字符串为数字
     */
    private Integer parseTypeFromString(String type) {
        // 根据业务需求定义类型映射
        switch (type) {
            case "压力传感器":
                return 0;
            case "温度传感器":
                return 1;
            case "流量传感器":
                return 2;
            case "振动传感器":
                return 3;
            case "其他传感器":
                return 4;
            default:
                return 0;
        }
    }

    /**
     * 解析数字类型为字符串
     */
    private String parseTypeToString(Integer type) {
        switch (type) {
            case 0:
                return "压力传感器";
            case 1:
                return "温度传感器";
            case 2:
                return "流量传感器";
            case 3:
                return "振动传感器";
            case 4:
                return "其他传感器";
            default:
                return "未知类型";
        }
    }

    /**
     * 解析设备状态字符串为数字
     */
    private Integer parseStatusFromString(String status) {
        switch (status) {
            case "正常":
                return 0;
            case "故障":
                return 1;
            case "离线":
                return 2;
            case "维护中":
                return 1; // 维护中归类为异常状态
            default:
                return 0;
        }
    }

    /**
     * 解析数字状态为字符串
     */
    private String parseStatusToString(Integer status) {
        switch (status) {
            case 0:
                return "正常";
            case 1:
                return "故障";
            case 2:
                return "离线";
            default:
                return "未知状态";
        }
    }
}
