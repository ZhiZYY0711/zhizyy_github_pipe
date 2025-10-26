package com.gxa.pipe.pojo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加监测数据请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataAddRequest {
    
    /**
     * 传感器id
     */
    @JsonProperty("sensor_id")
    private String sensorId;
    
    /**
     * 流量
     */
    private String flow;
    
    /**
     * 压力
     */
    private String pressure;
    
    /**
     * 温度
     */
    private String temperature;
    
    /**
     * 震动
     */
    private String vibration;
}