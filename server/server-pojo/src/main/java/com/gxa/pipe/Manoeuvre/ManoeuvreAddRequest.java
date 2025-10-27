package com.gxa.pipe.Manoeuvre;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 演习添加请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManoeuvreAddRequest {

    /**
     * 演练所属区域
     */
    @NotNull(message = "区域ID不能为空")
    @JsonProperty("area_id")
    private Long areaId;

    /**
     * 演练开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonProperty("start_time")
    private LocalDateTime startTime;

    /**
     * 演练结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @JsonProperty("end_time")
    private LocalDateTime endTime;

    /**
     * 演练状态 0 成功 1 失败 2 进行中
     */
    private Integer status;

    /**
     * 演练类型 0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
     */
    @NotNull(message = "演练类型不能为空")
    private Integer type;

    /**
     * 演练描述
     */
    private String details;
}