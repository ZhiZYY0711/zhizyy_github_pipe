package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryRequest {
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 检查ID
     */
    private Long inspectionId;
    
    /**
     * 检修员ID
     */
    private Long repairmanId;
    
    /**
     * 区域ID
     */
    private Long areaId;
    
    /**
     * 管道ID
     */
    private Long pipeId;
    
    /**
     * 任务类型
     */
    private Integer type;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 任务状态
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}