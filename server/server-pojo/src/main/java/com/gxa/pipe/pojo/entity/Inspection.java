package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监测数据表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inspection {
    
    /**
     * 监测数据的唯一标识
     */
    private Long id;
    
    /**
     * 压力值
     */
    private BigDecimal pressure;
    
    /**
     * 温度
     */
    private BigDecimal temperature;
    
    /**
     * 流量
     */
    private BigDecimal traffic;
    
    /**
     * 震动值
     */
    private BigDecimal shake;
    
    /**
     * 当前数据的状态 0 安全 1 良好 2 危险 3 高危
     */
    private Integer dataStatus;
    
    /**
     * 实时图片
     */
    private String realtimePicture;
    
    /**
     * 传感器的id
     */
    private Long sensorId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}