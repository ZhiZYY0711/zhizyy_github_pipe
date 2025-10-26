package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 监控数据添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataAddRequest {
    
    /**
     * 压力
     */
    @NotNull(message = "压力不能为空")
    private BigDecimal pressure;
    
    /**
     * 温度
     */
    @NotNull(message = "温度不能为空")
    private BigDecimal temperature;
    
    /**
     * 流量
     */
    @NotNull(message = "流量不能为空")
    private BigDecimal traffic;
    
    /**
     * 震动
     */
    @NotNull(message = "震动不能为空")
    private BigDecimal shake;
    
    /**
     * 数据状态
     */
    private Integer dataStatus;
    
    /**
     * 实时图片
     */
    private String realtimePicture;
    
    /**
     * 传感器ID
     */
    @NotNull(message = "传感器ID不能为空")
    private Long sensorId;
}