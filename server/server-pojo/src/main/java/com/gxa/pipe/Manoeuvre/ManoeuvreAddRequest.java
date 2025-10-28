package com.gxa.pipe.manoeuvre;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 演习添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreAddRequest {
    /**
     * 区域id
     */
    @JsonProperty("area_id")
    private long areaID;
    /**
     * 详情
     */
    private String detail;
    /**
     * 参加演习的检修员id
     */
    private long[] repairmans;
    /**
     * 开始时间，时间戳
     */
    @JsonProperty("start_time")
    private long startTime;
    /**
     * 状态
     */
    private long status;
    /**
     * 类型
     */
    private long type;
}