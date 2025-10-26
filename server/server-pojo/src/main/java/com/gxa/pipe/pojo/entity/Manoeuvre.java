package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 演习表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manoeuvre {
    
    /**
     * 演习记录的唯一标识
     */
    private Long id;
    
    /**
     * 演练所属区域
     */
    private Long areaId;
    
    /**
     * 演练开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 演练结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 演练状态 0 成功 1 失败 2 进行中
     */
    private Integer status;
    
    /**
     * 演练类型 0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
     */
    private Integer type;
    
    /**
     * 演练描述
     */
    private String details;
    
    /**
     * 演习创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 演习更新时间
     */
    private LocalDateTime updateTime;
}