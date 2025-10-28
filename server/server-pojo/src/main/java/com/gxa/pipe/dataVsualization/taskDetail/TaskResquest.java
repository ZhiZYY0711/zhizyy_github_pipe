package com.gxa.pipe.dataVsualization.taskDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态、任务数量、区域任务 都通过这个请求类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResquest {
  /**
   * 区域id
   */
  @JsonProperty("area_id")
  private int areaId;

  /**
   * 开始时间
   */
  @JsonProperty("start_time")
  private long startTime;

  /**
   * 结束时间
   */
  @JsonProperty("end_time")
  private long endTime;
}
