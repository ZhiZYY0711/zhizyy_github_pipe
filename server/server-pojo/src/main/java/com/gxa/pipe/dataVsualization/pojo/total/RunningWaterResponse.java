package com.gxa.pipe.dataVsualization.pojo.total;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流水报警响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunningWaterResponse {
  /**
   * 唯一标识
   */
  private String id;

  /**
   * 时间戳
   */
  private Long time;

  /**
   * 传感器ID
   */
  @JsonProperty("sensor_id")
  private String sensorId;

  /**
   * 传感器名称
   */
  @JsonProperty("sensor_name")
  private String sensorName;

  /**
   * 区域ID
   */
  @JsonProperty("area_id")
  private String areaId;

  /**
   * 区域名称
   */
  @JsonProperty("area_name")
  private String areaName;

  /**
   * 消息内容
   */
  private String message;

  /**
   * 级别
   */
  private String level;
}
