package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演习查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreQueryRequest {
    
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
     * 演习状态
     */
    private Integer status;
    
    /**
     * 演习类型
     */
    private Integer type;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}