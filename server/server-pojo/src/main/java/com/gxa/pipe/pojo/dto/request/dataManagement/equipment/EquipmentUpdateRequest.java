package com.gxa.pipe.pojo.dto.request.dataManagement.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {

    /**
     * 设备的唯一标识
     */
    private String id;

    /**
     * 设备类型
     */
    private String type;

    /**
     * 传感器安装位置（区域ID）
     */
    @JsonProperty("area_id")
    private String areaId;

    /**
     * 传感器所属管道
     */
    @JsonProperty("pipe_id")
    private String pipeId;

    /**
     * 设备状态（正常/故障/维护中/离线）
     */
    private String status;

    /**
     * 负责人
     */
    private String responsible;

    /**
     * 上次检修时间
     */
    @JsonProperty("last_overhaul_time")
    private String lastOverhaulTime;
}