package com.gxa.pipe.dataVsualization.taskDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务榜单响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskKpiResponse {
  /**
   * 具体数据
   */
  private String data;
  /**
   * 检修员姓名
   */
  private String name;
  /**
   * 检修员的唯一标识
   */
  @JsonProperty("repairman_id")
  private long repairmanID;
}
