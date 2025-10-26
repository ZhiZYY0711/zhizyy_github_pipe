package com.gxa.pipe.pojo.dto.request;

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
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    @JsonProperty("page_size")
    private Integer pageSize;
    
    /**
     * 区域ID
     */
    @JsonProperty("area_id")
    private Long areaId;
    
    /**
     * 管道ID
     */
    @JsonProperty("pipeline_id")
    private Long pipelineId;
    
    /**
     * 检修员ID
     */
    @JsonProperty("repairman_id")
    private Long repairmanId;
    
    /**
     * 设备状态
     */
    private Integer status;
    
    /**
     * 设备类型
     */
    private Integer type;
}