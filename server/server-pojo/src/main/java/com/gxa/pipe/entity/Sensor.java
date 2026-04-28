package com.gxa.pipe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 传感器表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    /**
     * 传感器的唯一标识
     */
    private Long id;

    /**
     * 传感器所属的区域的id
     */
    private Long areaId;

    /**
     * 传感器所属的管道的id
     */
    private Long pipelineId;

    /**
     * 传感器所属的管段的id
     */
    private Long pipeSegmentId;

    /**
     * 负责的检修员
     */
    private Long repairmanId;

    /**
     * 当前传感器的状态 0 正常 1 异常 2 离线
     */
    private Integer status;

    /**
     * 传感器类型
     */
    private Integer type;

    /**
     * 上次检修时间
     */
    private LocalDateTime lastOverhaulTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
