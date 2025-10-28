package com.gxa.pipe.manoeuvre;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演习查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreQueryRequest {
    /**
     * 任务区域
     */
    @JsonProperty("area_id")
    private long areaId;

    /**
     * 最晚结束时间，时间戳
     */
    @JsonProperty("end_time_max")
    private Long endTimeMax;

    /**
     * 最早结束时间，时间戳
     */
    @JsonProperty("end_time_min")
    private Long endTimeMin;

    /**
     * 最晚开始时间，时间戳
     */
    @JsonProperty("start_time_max")
    private Long startTimeMax;

    /**
     * 最早开始时间，时间戳
     */
    @JsonProperty("start_time_min")
    private Long startTimeMin;

    /**
     * 演练状态，0 成功 1 失败 2 进行中
     */
    private Long status;

    /**
     * 类型，0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
     */
    private Long type;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页数据量
     */
    @JsonProperty("page_size")
    private Integer pageSize;
}