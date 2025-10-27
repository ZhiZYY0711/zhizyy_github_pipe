package com.gxa.pipe.pojo.vo.dataManagement.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务指标卡响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskIndicatorResponse {

    /**
     * 任务总数
     */
    private String total;

    /**
     * 待处理数量
     */
    private String pending;

    /**
     * 进行中数量
     */
    @JsonProperty("in_progress")
    private String inProgress;

    /**
     * 已完成数量
     */
    private String completed;

    /**
     * 紧急任务数量
     */
    private String urgent;
}