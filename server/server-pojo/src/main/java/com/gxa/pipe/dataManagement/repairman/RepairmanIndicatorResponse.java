package com.gxa.pipe.dataManagement.repairman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员指标卡数据响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanIndicatorResponse {

    /**
     * 检修员总数
     */
    private String total;

    /**
     * 男性人数
     */
    private String male;

    /**
     * 女性人数
     */
    private String female;

    /**
     * 平均年龄
     */
    @JsonProperty("avg_age")
    private String avgAge;

    /**
     * 本月新增人数
     */
    @JsonProperty("new_this_month")
    private String newThisMonth;
}
