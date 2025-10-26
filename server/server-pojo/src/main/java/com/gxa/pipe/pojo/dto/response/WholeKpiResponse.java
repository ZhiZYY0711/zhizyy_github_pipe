package com.gxa.pipe.pojo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局KPI响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WholeKpiResponse {
    
    /**
     * 传感器数量
     */
    @JsonProperty("sensor_numbers")
    private Integer sensorNumbers;
    
    /**
     * 异常传感器数量
     */
    @JsonProperty("abnormal_sensor")
    private Integer abnormalSensor;
    
    /**
     * 今日高危/危险报警次数
     */
    private Integer warnings;
    
    /**
     * 进行中任务数量
     */
    @JsonProperty("underway_task")
    private Integer underwayTask;
    
    /**
     * 超时任务数量
     */
    @JsonProperty("overtime_task")
    private Integer overtimeTask;
}