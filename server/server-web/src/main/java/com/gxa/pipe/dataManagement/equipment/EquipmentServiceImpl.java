package com.gxa.pipe.dataManagement.equipment;

import com.github.pagehelper.PageHelper;
import com.gxa.pipe.entity.Sensor;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentServiceImpl implements EquipmentService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EquipmentMapper equipmentMapper;

    @Override
    public List<EquipmentResponse> findEquipmentByConditions(EquipmentQueryRequest request) {
        log.info("根据条件查询设备信息，查询条件：{}", request);

        int page = StringUtils.hasText(request.getPage()) ? Integer.parseInt(request.getPage()) : 1;
        int pageSize = StringUtils.hasText(request.getPageSize()) ? Integer.parseInt(request.getPageSize()) : 50;

        PageHelper.startPage(Math.max(page, 1), Math.max(pageSize, 1));
        return equipmentMapper.selectEquipmentByConditions(request);
    }

    @Override
    public List<EquipmentResponse> findEquipmentById(String id) {
        log.info("根据ID查询设备信息，设备ID：{}", id);

        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        EquipmentResponse equipmentResponse = equipmentMapper.selectEquipmentById(Long.parseLong(id));
        if (equipmentResponse == null) {
            return new ArrayList<>();
        }

        List<EquipmentResponse> result = new ArrayList<>();
        result.add(equipmentResponse);
        return result;
    }

    @Override
    public boolean addEquipment(EquipmentAddRequest request) {
        log.info("添加设备信息，请求参数：{}", request);

        validateAddRequest(request);
        validateRelatedData(
                Long.parseLong(request.getAreaId()),
                Long.parseLong(request.getPipeId()),
                Long.parseLong(request.getPipeSegmentId()),
                Long.parseLong(request.getResponsible()));

        return equipmentMapper.insertEquipment(buildSensorFromAddRequest(request)) > 0;
    }

    @Override
    public boolean updateEquipment(EquipmentUpdateRequest request) {
        log.info("更新设备信息，请求参数：{}", request);

        validateUpdateRequest(request);

        Long equipmentId = Long.parseLong(request.getId());
        Sensor existingSensor = equipmentMapper.selectSensorById(equipmentId);
        if (existingSensor == null) {
            throw new IllegalArgumentException("设备不存在");
        }

        if (StringUtils.hasText(request.getAreaId())
                || StringUtils.hasText(request.getPipeId())
                || StringUtils.hasText(request.getPipeSegmentId())
                || StringUtils.hasText(request.getResponsible())) {
            Long areaId = StringUtils.hasText(request.getAreaId())
                    ? Long.parseLong(request.getAreaId())
                    : existingSensor.getAreaId();
            Long pipeId = StringUtils.hasText(request.getPipeId())
                    ? Long.parseLong(request.getPipeId())
                    : existingSensor.getPipelineId();
            Long pipeSegmentId = StringUtils.hasText(request.getPipeSegmentId())
                    ? Long.parseLong(request.getPipeSegmentId())
                    : existingSensor.getPipeSegmentId();
            Long repairmanId = StringUtils.hasText(request.getResponsible())
                    ? Long.parseLong(request.getResponsible())
                    : existingSensor.getRepairmanId();

            validateRelatedData(areaId, pipeId, pipeSegmentId, repairmanId);
        }

        return equipmentMapper.updateEquipment(buildSensorFromUpdateRequest(request, existingSensor)) > 0;
    }

    @Override
    public boolean deleteEquipment(String ids) {
        log.info("删除设备信息，设备IDs：{}", ids);

        if (!StringUtils.hasText(ids)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        for (Long id : idList) {
            Sensor sensor = equipmentMapper.selectSensorById(id);
            if (sensor == null) {
                throw new IllegalArgumentException("设备ID " + id + " 不存在");
            }
        }

        return equipmentMapper.deleteEquipmentByIds(idList) > 0;
    }

    @Override
    public EquipmentIndicatorResponse getEquipmentIndicator() {
        log.info("获取设备指标卡数据");

        EquipmentIndicatorResponse response = new EquipmentIndicatorResponse();
        response.setTotal(String.valueOf(equipmentMapper.countTotal()));
        response.setNormal(String.valueOf(equipmentMapper.countNormal()));
        response.setFault(String.valueOf(equipmentMapper.countFault()));
        response.setMaintenance(String.valueOf(equipmentMapper.countMaintenance()));
        response.setOffline(String.valueOf(equipmentMapper.countOffline()));
        return response;
    }

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
        if (!StringUtils.hasText(request.getPipeSegmentId())) {
            throw new IllegalArgumentException("所属管段不能为空");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            throw new IllegalArgumentException("设备状态不能为空");
        }
        if (!StringUtils.hasText(request.getResponsible())) {
            throw new IllegalArgumentException("负责人不能为空");
        }
    }

    private void validateUpdateRequest(EquipmentUpdateRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
    }

    private void validateRelatedData(Long areaId, Long pipeId, Long pipeSegmentId, Long repairmanId) {
        if (!equipmentMapper.existsAreaById(areaId)) {
            throw new IllegalArgumentException("区域不存在");
        }
        if (!equipmentMapper.existsPipeById(pipeId)) {
            throw new IllegalArgumentException("管道不存在");
        }
        if (!equipmentMapper.existsPipeSegmentById(pipeSegmentId, pipeId)) {
            throw new IllegalArgumentException("管段不存在或不属于所选管道");
        }
        if (!equipmentMapper.existsRepairmanById(repairmanId)) {
            throw new IllegalArgumentException("检修员不存在");
        }
    }

    private Sensor buildSensorFromAddRequest(EquipmentAddRequest request) {
        Sensor sensor = new Sensor();
        sensor.setAreaId(Long.parseLong(request.getAreaId()));
        sensor.setPipelineId(Long.parseLong(request.getPipeId()));
        sensor.setPipeSegmentId(Long.parseLong(request.getPipeSegmentId()));
        sensor.setRepairmanId(Long.parseLong(request.getResponsible()));
        sensor.setType(parseTypeFromString(request.getType()));
        sensor.setStatus(parseStatusFromString(request.getStatus()));
        sensor.setCreateTime(LocalDateTime.now());
        sensor.setUpdateTime(LocalDateTime.now());
        return sensor;
    }

    private Sensor buildSensorFromUpdateRequest(EquipmentUpdateRequest request, Sensor existingSensor) {
        Sensor sensor = new Sensor();
        sensor.setId(Long.parseLong(request.getId()));
        sensor.setAreaId(StringUtils.hasText(request.getAreaId())
                ? Long.parseLong(request.getAreaId())
                : existingSensor.getAreaId());
        sensor.setPipelineId(StringUtils.hasText(request.getPipeId())
                ? Long.parseLong(request.getPipeId())
                : existingSensor.getPipelineId());
        sensor.setPipeSegmentId(StringUtils.hasText(request.getPipeSegmentId())
                ? Long.parseLong(request.getPipeSegmentId())
                : existingSensor.getPipeSegmentId());
        sensor.setRepairmanId(StringUtils.hasText(request.getResponsible())
                ? Long.parseLong(request.getResponsible())
                : existingSensor.getRepairmanId());
        sensor.setType(StringUtils.hasText(request.getType())
                ? parseTypeFromString(request.getType())
                : existingSensor.getType());
        sensor.setStatus(StringUtils.hasText(request.getStatus())
                ? parseStatusFromString(request.getStatus())
                : existingSensor.getStatus());
        sensor.setLastOverhaulTime(StringUtils.hasText(request.getLastOverhaulTime())
                ? LocalDateTime.parse(request.getLastOverhaulTime(), DATE_TIME_FORMATTER)
                : existingSensor.getLastOverhaulTime());
        sensor.setCreateTime(existingSensor.getCreateTime());
        sensor.setUpdateTime(LocalDateTime.now());
        return sensor;
    }

    private Integer parseTypeFromString(String type) {
        return switch (type) {
            case "压力传感器" -> 0;
            case "温度传感器" -> 1;
            case "流量传感器" -> 2;
            case "振动传感器" -> 3;
            case "其他传感器" -> 4;
            default -> 0;
        };
    }

    private Integer parseStatusFromString(String status) {
        return switch (status) {
            case "正常" -> 0;
            case "故障", "维护中" -> 1;
            case "离线" -> 2;
            default -> 0;
        };
    }
}
