package com.gxa.pipe.pojo.vo.dataManagement.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    /**
     * 任务唯一标识
     */
    private String id;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务类型
     */
    private String type;

    /**
     * 任务区域
     */
    @JsonProperty("area_name")
    private String areaName;

    /**
     * 任务所属管道
     */
    @JsonProperty("pipe_name")
    private String pipeName;

    /**
     * 优先级（紧急/高/中/低）
     */
    private String priority;

    /**
     * 任务状态（待处理/进行中/已完成/已取消）
     */
    private String status;

    /**
     * 指派检修员姓名
     */
    private String assignee;

    /**
     * 联系方式
     */
    private String phone;

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
     * 任务完成时间
     */
    @JsonProperty("accomplish_time")
    private String accomplishTime;

    /**
     * 检修员反馈信息
     */
    @JsonProperty("feedback_information")
    private String feedbackInformation;
}