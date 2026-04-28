package com.gxa.pipe.log;

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
     * 区域id
     */
    @JsonProperty("area_id")
    private long areaId;
    /**
     * 操作时间最小值，为空时不限制最大时间 时间戳
     */
    @JsonProperty("operation_time_max")
    private Long operationTimeMax;
    /**
     * 操作时间最小值，为空时不限制最小时间 时间戳
     */
    @JsonProperty("operation_time_min")
    private Long operationTimeMin;
    /**
     * 页码
     */
    private Integer page;
    /**
     * 每页数据量
     */
    @JsonProperty("page_size")
    private Integer pageSize;
    /**
     * 操作的状态
     */
    private Long status;
    /**
     * 操作员类型
     */
    private Long type;
}