package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管道详情四维度数据请求类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipeDetailRequest {
  /**
   * 区域ID
   */
  @JsonProperty("area_id")
  private String areaId;

  /**
   * 开始时间
   */
  @JsonProperty("start_time")
  private String startTime;

  /**
   * 结束时间
   */
  @JsonProperty("end_time")
  private String endTime;
}
