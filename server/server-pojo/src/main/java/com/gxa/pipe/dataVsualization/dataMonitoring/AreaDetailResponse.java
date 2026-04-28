package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域详情四维度数据响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaDetailResponse {
  /**
   * 平均流量
   */
  @JsonProperty("ave_flow")
  private Double aveFlow;

  /**
   * 平均压力
   */
  @JsonProperty("ave_pressure")
  private Double avePressure;

  /**
   * 平均温度
   */
  @JsonProperty("ave_temperature")
  private Double aveTemperature;

  /**
   * 平均震动
   */
  @JsonProperty("ave_vibration")
  private Double aveVibration;

  /**
   * 时间戳
   */
  private Long time;
}
