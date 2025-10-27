package com.gxa.pipe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    /**
     * 任务的唯一标识
     */
    private Long id;

    /**
     * 出现危险的监测记录的id
     */
    private Long inspectionId;

    /**
     * 被分配任务的检修员的id
     */
    private Long repairmanId;

    /**
     * 任务所属区域
     */
    private Long areaId;

    /**
     * 任务所属管道
     */
    private Long pipeId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型
     */
    private Integer type;

    /**
     * 任务优先级 0 紧急 1 高 2 中
     */
    private Integer priority;

    /**
     * 任务结果 0 已发布 1 已接取 2已完成 3 异常
     */
    private Integer status;

    /**
     * 任务发布时间
     */
    private LocalDateTime publicTime;

    /**
     * 任务响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 任务完成时间
     */
    private LocalDateTime accomplishTime;

    /**
     * 检修员的反馈信息（扩展长度）
     */
    private String feedbackInformation;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}