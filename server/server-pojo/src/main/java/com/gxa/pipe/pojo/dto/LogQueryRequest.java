package com.gxa.pipe.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryRequest {

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
     * 用户ID
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 操作员类型 0 管理员 1 检修员
     */
    private Integer type;

    /**
     * 操作状态 0 成功 1 失败 2 未知
     */
    private Integer status;

    /**
     * IP地址
     */
    @JsonProperty("ip_address")
    private String ipAddress;

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