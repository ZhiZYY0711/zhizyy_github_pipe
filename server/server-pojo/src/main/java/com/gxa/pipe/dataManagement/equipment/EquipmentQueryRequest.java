package com.gxa.pipe.dataManagement.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentQueryRequest {

    /**
     * 设备编号
     */
    private String id;

    /**
     * 设备类型
     */
    private String type;

    /**
     * 设备状态（正常/故障/维护中/离线）
     */
    private String status;

    /**
     * 设备所属区域
     */
    @JsonProperty("area_id")
    private String areaId;

    /**
     * 设备所属管道
     */
    @JsonProperty("pipe_id")
    private String pipeId;

    /**
     * 负责人
     */
    private String responsible;

    /**
     * 最后检查起始时间
     */
    @JsonProperty("last_check_start")
    private String lastCheckStart;

    /**
     * 最后检查结束时间
     */
    @JsonProperty("last_check_end")
    private String lastCheckEnd;

    /**
     * 页码
     */
    private String page;

    /**
     * 每页数据量
     */
    @JsonProperty("page_size")
    private String pageSize;
}