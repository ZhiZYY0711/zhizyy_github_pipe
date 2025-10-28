package com.gxa.pipe.dataVsualization.taskDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusResponse {
  /**
   * 任务状态
   */
  @JsonProperty()
  private int status;

  /**
   * 任务数量
   */
  private long count;
}
