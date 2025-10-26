package com.gxa.pipe.pojo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员KPI响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanKpiResponse {
    
    /**
     * 检修员数量
     */
    @JsonProperty("repairman_number")
    private Integer repairmanNumber;
    
    /**
     * 今日任务数量
     */
    @JsonProperty("today_task_number")
    private Integer todayTaskNumber;
    
    /**
     * 任务完成率
     */
    @JsonProperty("task_rate")
    private Double taskRate;
    
    /**
     * 任务平均处理时间
     */
    @JsonProperty("average_time")
    private Integer averageTime;
}