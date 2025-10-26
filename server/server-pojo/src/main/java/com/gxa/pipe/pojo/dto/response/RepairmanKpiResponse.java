package com.gxa.pipe.pojo.dto.response;

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
     * 检修员总数
     */
    private Integer repairmanNumber;
    
    /**
     * 今日完成任务数
     */
    private Integer todayTaskNumber;
    
    /**
     * 任务完成率
     */
    private Integer taskRate;
    
    /**
     * 任务平均处理时间
     */
    private Integer averageTime;
}