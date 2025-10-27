package com.gxa.pipe.dataVsualization.pojo.total;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检修员KPI响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepairmanKpiResponse {

  /**
   * 检修员数量
   */
  @JsonProperty("repairman_number")
  private String repairmanNumber;
  /**
   * 今日任务数量
   */
  @JsonProperty("today_task_number")
  private String todayTaskNumber;
  /**
   * 任务完成率
   */
  @JsonProperty("task_rate")
  private String taskRate;
  /**
   * 平均完成时间
   */
  @JsonProperty("average_time")
  private String averageTime;
}
