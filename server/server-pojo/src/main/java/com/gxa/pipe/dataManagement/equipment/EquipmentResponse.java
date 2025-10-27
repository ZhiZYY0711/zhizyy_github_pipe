package com.gxa.pipe.dataManagement.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {

    /**
     * 设备的唯一标识
     */
    private String id;

    /**
     * 设备类型
     */
    private String type;

    /**
     * 设备所属区域名称
     */
    @JsonProperty("area_name")
    private String areaName;

    /**
     * 设备所属管道名称
     */
    @JsonProperty("pipe_name")
    private String pipeName;

    /**
     * 设备状态
     */
    private String status;

    /**
     * 最后检查时间
     */
    @JsonProperty("last_check")
    private String lastCheck;

    /**
     * 负责人
     */
    private String responsible;
}