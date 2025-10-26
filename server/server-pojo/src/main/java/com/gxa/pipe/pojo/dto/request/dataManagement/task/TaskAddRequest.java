package com.gxa.pipe.pojo.dto.request.dataManagement.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 任务添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAddRequest {

    /**
     * 检查ID
     */
    @JsonProperty("inspection_id")
    private Long inspectionId;

    /**
     * 检修员ID
     */
    @NotNull(message = "检修员ID不能为空")
    @JsonProperty("repairman_id")
    private Long repairmanId;

    /**
     * 区域ID
     */
    @NotNull(message = "区域ID不能为空")
    @JsonProperty("area_id")
    private Long areaId;

    /**
     * 管道ID
     */
    @NotNull(message = "管道ID不能为空")
    @JsonProperty("pipe_id")
    private Long pipeId;

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @JsonProperty("task_name")
    private String taskName;

    /**
     * 任务类型
     */
    @NotNull(message = "任务类型不能为空")
    private Integer type;

    /**
     * 优先级
     */
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 反馈信息
     */
    @JsonProperty("feedback_information")
    private String feedbackInformation;
}