package com.gxa.pipe.dataManagement.repairman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员指标查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanIndicatorRequest {

    /**
     * 区域ID（可选）
     */
    @JsonProperty("area_id")
    private Long areaId;

    /**
     * 开始时间（可选）
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * 结束时间（可选）
     */
    @JsonProperty("end_time")
    private String endTime;
}