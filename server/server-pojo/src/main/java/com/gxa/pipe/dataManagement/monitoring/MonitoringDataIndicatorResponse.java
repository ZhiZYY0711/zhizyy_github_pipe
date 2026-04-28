package com.gxa.pipe.dataManagement.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监测数据指标卡响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataIndicatorResponse {

    /**
     * 总记录数
     */
    @JsonProperty("total_records")
    private String totalRecords;

    /**
     * 安全数量
     */
    @JsonProperty("safe_count")
    private String safeCount;

    /**
     * 良好数量
     */
    @JsonProperty("good_count")
    private String goodCount;

    /**
     * 危险数量
     */
    @JsonProperty("danger_count")
    private String dangerCount;

    /**
     * 今日新增
     */
    private String today;

    /**
     * 高危数量
     */
    @JsonProperty("critical_count")
    private String criticalCount;
}
