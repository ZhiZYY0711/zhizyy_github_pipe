package com.gxa.pipe.dataManagement.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 任务更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private Long id;

    /**
     * 检查ID
     */
    @JsonProperty("inspection_id")
    private Long inspectionId;

    /**
     * 检修员ID
     */
    @JsonProperty("repairman_id")
    private Long repairmanId;

    /**
     * 区域ID
     */
    @JsonProperty("area_id")
    private Long areaId;

    /**
     * 管道ID
     */
    @JsonProperty("pipe_id")
    private Long pipeId;

    /**
     * 任务名称
     */
    @JsonProperty("task_name")
    private String taskName;

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
     * 发布时间
     */
    @JsonProperty("public_time")
    private String publicTime;

    /**
     * 响应时间
     */
    @JsonProperty("response_time")
    private String responseTime;

    /**
     * 完成时间
     */
    @JsonProperty("accomplish_time")
    private String accomplishTime;

    /**
     * 反馈信息
     */
    @JsonProperty("feedback_information")
    private String feedbackInformation;
}
