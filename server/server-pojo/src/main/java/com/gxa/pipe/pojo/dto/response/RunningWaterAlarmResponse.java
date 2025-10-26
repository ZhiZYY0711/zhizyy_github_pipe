package com.gxa.pipe.pojo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流水报警响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunningWaterAlarmResponse {
    
    /**
     * id 唯一标识
     */
    private String id;
    
    /**
     * 时间
     */
    private String time;
    
    /**
     * 传感器ID
     */
    @JsonProperty("sensor_id")
    private String sensorId;
    
    /**
     * 位置
     */
    private String location;
    
    /**
     * 报警级别
     */
    private String level;
    
    /**
     * 报警类型
     */
    private String type;
}