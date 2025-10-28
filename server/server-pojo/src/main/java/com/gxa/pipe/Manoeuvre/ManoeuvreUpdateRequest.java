package com.gxa.pipe.manoeuvre;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演习更新请求类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManoeuvreUpdateRequest {
  /**
   * 区域id
   */
  @JsonProperty("areaId")
  private Long areaId;

  /**
   * 详情
   */
  private String detail;

  /**
   * 结束时间，时间戳
   */
  @JsonProperty("endTime")
  private Long endTime;

  /**
   * 唯一标识
   */
  private long id;

  /**
   * 参与演习的检修员的id
   */
  private long[] repairmans;

  /**
   * 开始时间，时间戳
   */
  @JsonProperty("startTime")
  private Long startTime;

  /**
   * 状态，0 成功 1 失败 2 进行中
   */
  private Long status;

  /**
   * 类型，0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
   */
  private Long type;
}
