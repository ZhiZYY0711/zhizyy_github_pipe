package com.gxa.pipe.pojo.dto.request;

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
    private Integer pageSize;
    
    /**
     * 区域ID
     */
    private Long areaId;
    
    /**
     * 管道ID
     */
    private Long pipelineId;
    
    /**
     * 检修员ID
     */
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