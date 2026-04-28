package com.gxa.pipe.dataVsualization.taskDetail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务数量响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskNumberResponse {

  /**
   * 任务日期 时间戳
   */
  private long time;

  /**
   * 任务数量
   */
  private long count;
}