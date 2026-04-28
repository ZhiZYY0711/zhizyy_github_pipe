package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管道详情四维度数据请求类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipeDetailRequest {
  /**
   * 管道ID
   */
  private Long id;

  /**
   * 管段ID
   */
  @JsonProperty("segment_id")
  private Long segmentId;

  /**
   * 连续管段ID列表
   */
  @JsonProperty("segment_ids")
  private List<Long> segmentIds;

  /**
   * 开始时间
   */
  @JsonProperty("start_time")
  private Long startTime;

  /**
   * 结束时间
   */
  @JsonProperty("end_time")
  private Long endTime;
}
