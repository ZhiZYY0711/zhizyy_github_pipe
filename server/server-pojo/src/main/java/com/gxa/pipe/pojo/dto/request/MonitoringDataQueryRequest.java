package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控数据查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataQueryRequest {
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 传感器ID
     */
    private String sensorId;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 数据状态
     */
    private Integer dataStatus;
}