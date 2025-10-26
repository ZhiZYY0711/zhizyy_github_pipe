package com.gxa.pipe.pojo.dto.response;

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
     * 传感器总数
     */
    private Integer sensorNumbers;
    
    /**
     * 异常/离线的传感器
     */
    private Integer abnormalSensor;
    
    /**
     * 今日高危/危险报警次数
     */
    private Integer warnings;
    
    /**
     * 进行中的任务数
     */
    private Integer underwayTask;
    
    /**
     * 超时/未处理的任务数
     */
    private Integer overtimeTask;
}