package com.gxa.pipe.dataVsualization.pojo.dataMonitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管道关键指标卡响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipeIndicatorResponse {
  /**
   * 平均温度
   */
  @JsonProperty("ave_temperature")
  private Double aveTemperature;
  /**
   * 温度状态
   */
  @JsonProperty("temperature_status")
  private int temperatureStatus;

  /**
   * 平均流量
   */
  @JsonProperty("ave_flow")
  private Double aveFlow;

  /**
   * 流量状态
   */
  @JsonProperty("flow_status")
  private int flowStatus;

  /**
   * 平均压力
   */
  @JsonProperty("ave_pressure")
  private Double avePressure;

  /**
   * 压力状态
   */
  @JsonProperty("pressure_status")
  private int pressureStatus;

  /**
   * 平均震动
   */
  @JsonProperty("ave_vibration")
  private Double aveVibration;

  /**
   * 震动状态
   */
  @JsonProperty("vibration_status")
  private int vibrationStatus;
}
