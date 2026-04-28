package com.gxa.pipe.dataVsualization.total;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局KPI响应响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WholeKpiResponse {

  /**
   * 传感器数量
   */
  @JsonProperty("sensor_numbers")
  private long sensorNumbers;

  /**
   * 异常传感器数量
   */
  @JsonProperty("abnormal_sensor_numbers")
  private long abnormalSensorNumbers;

  /**
   * 警告数量
   */
  @JsonProperty("warnings")
  private long warnings;

  /**
   * 进行中的任务数量
   */
  @JsonProperty("underway_task")
  private long underwayTask;

  /**
   * 超时任务数量
   */
  @JsonProperty("overtime_task")
  private long overtimeTask;
}