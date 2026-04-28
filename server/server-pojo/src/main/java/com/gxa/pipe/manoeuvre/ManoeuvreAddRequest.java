package com.gxa.pipe.manoeuvre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

/**
 * 演习添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreAddRequest {
    /**
     * 生成的演习ID（仅用于接收数据库生成的主键，不参与JSON序列化）
     */
    @JsonIgnore
    private Long generatedId;
    /**
     * 区域id
     */
    @NotNull(message = "区域ID不能为空")
    @JsonProperty("area_id")
    private long areaId;

    /**
     * 详情
     */
    private String detail;

    /**
     * 参加演习的检修员id
     */
    @NotEmpty(message = "参加演习的检修员不能为空")
    private long[] repairmans;

    /**
     * 开始时间，时间戳
     */
    @NotNull(message = "开始时间不能为空")
    @JsonProperty("start_time")
    private long startTime;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private long status;

    /**
     * 类型
     */
    @NotNull(message = "类型不能为空")
    private long type;
}