package com.gxa.pipe.Manoeuvre.dto;

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
     * 页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    @JsonProperty("page_size")
    private Integer pageSize;

    /**
     * 区域ID
     */
    @JsonProperty("area_id")
    private Long areaId;

    /**
     * 演习状态
     */
    private Integer status;

    /**
     * 演习类型
     */
    private Integer type;

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