package com.gxa.pipe.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogIndicardResponse {
  /**
   * 调试日志
   */
  private long debugging;
  /**
   * 错误日志
   */
  private long error;
  /**
   * 全部日志
   */
  private long all;

  /**
   * 日志总数（前端展示别名）
   */
  private long total;

  /**
   * 成功日志数量
   */
  private long success;

  /**
   * 失败日志数量（前端展示别名）
   */
  private long failed;

  /**
   * 平均耗时
   */
  @JsonProperty("avg_duration")
  private long avgDuration;

  /**
   * 今日日志
   */
  private long today;
  /**
   * 警告日志
   */
  private long warning;
}
