package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管道表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pipe {
    
    /**
     * 管道名的唯一标识
     */
    private Long id;
    
    /**
     * 管道的起点区域的id
     */
    private Long startAreaId;
    
    /**
     * 管道的终点区域的id
     */
    private Long endAreaId;
    
    /**
     * 管道名称
     */
    private String name;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}