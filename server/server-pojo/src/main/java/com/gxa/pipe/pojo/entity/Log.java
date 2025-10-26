package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日志表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    
    /**
     * 日志的唯一标识
     */
    private Long id;
    
    /**
     * 管理员或检修员的id
     */
    private Long userId;
    
    /**
     * 操作员的类型 0 管理员 1 检修员
     */
    private Integer type;
    
    /**
     * 进行的操作
     */
    private String operate;
    
    /**
     * 操作的状态 0 成功 1 失败 2 未知
     */
    private Integer status;
    
    /**
     * 操作者的ip地址
     */
    private String ipAddress;
    
    /**
     * 操作详情
     */
    private String details;
    
    /**
     * 持续时间
     */
    private LocalDateTime period;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}