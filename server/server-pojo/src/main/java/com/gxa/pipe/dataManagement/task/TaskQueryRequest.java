package com.gxa.pipe.dataManagement.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryRequest {

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    @JsonProperty("page_size")
    private Integer pageSize;

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
     * 管段ID
     */
    @JsonProperty("pipe_segment_id")
    private Long pipeSegmentId;

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
     * 开始时间
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * 结束时间
     */
    @JsonProperty("end_time")
    private String endTime;
}
