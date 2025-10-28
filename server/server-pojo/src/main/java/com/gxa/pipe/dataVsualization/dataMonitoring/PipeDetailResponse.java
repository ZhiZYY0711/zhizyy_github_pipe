package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管道详情四维度数据响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipeDetailResponse {
  /**
   * 流量
   */
  @JsonProperty("ave_flow")
  private Double aveFlow;

  /**
   * 压力
   */
  @JsonProperty("ave_pressure")
  private Double avePressure;

  /**
   * 温度
   */
  @JsonProperty("ave_temperature")
  private Double aveTemperature;

  /**
   * 震动
   */
  @JsonProperty("ave_vibration")
  private Double aveVibration;

  /**
   * 时间戳
   */
  private Long time;
}
