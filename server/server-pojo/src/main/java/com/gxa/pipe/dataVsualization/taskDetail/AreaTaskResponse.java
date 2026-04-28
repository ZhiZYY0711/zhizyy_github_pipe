package com.gxa.pipe.dataVsualization.taskDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域任务响应类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaTaskResponse {

  /**
   * 区域名称
   */
  @JsonProperty("area_name")
  private String areaName;

  /**
   * 任务数量
   */
  private long count;
}
