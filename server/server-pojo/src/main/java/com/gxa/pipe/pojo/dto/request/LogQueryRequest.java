package com.gxa.pipe.pojo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryRequest {
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 操作员类型 0 管理员 1 检修员
     */
    private Integer type;
    
    /**
     * 操作状态 0 成功 1 失败 2 未知
     */
    private Integer status;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}