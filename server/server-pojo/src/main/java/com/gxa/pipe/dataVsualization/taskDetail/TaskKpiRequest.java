package com.gxa.pipe.dataVsualization.taskDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务榜单请求类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskKpiRequest {
  /**
   * 区域id
   */
  @JsonProperty("area_id")
  private int areaId;

  /**
   * 开始时间 为空时不限制开始时间 时间戳
   */
  @JsonProperty("start_time")
  private long startTime;

  /**
   * 结束时间 为空时不限制结束时间 时间戳
   */
  @JsonProperty("end_time")
  private long endTime;

  /**
   * 榜单类型 0 完成任务数 1 平均响应时间 2 平均完成时间
   */
  private int type;
}
