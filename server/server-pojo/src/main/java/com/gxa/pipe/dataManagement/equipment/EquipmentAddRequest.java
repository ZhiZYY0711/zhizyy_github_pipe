package com.gxa.pipe.dataManagement.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentAddRequest {

    /**
     * 设备类型
     */
    private String type;

    /**
     * 安装位置（区域ID）
     */
    @JsonProperty("area_id")
    private String areaId;

    /**
     * 任务所属管道
     */
    @JsonProperty("pipe_id")
    private String pipeId;

    /**
     * 设备所属管段
     */
    @JsonProperty("pipe_segment_id")
    private String pipeSegmentId;

    /**
     * 设备状态（正常/故障/维护中/离线）
     */
    private String status;

    /**
     * 负责人
     */
    private String responsible;
}
