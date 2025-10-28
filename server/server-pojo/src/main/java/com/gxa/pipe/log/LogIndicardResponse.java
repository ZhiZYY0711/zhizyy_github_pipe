package com.gxa.pipe.log;

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
   * 今日日志
   */
  private long today;
  /**
   * 警告日志
   */
  private long warning;
}
