package com.gxa.pipe.log;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryResponse {

  /**
   * 操作详情
   */
  private String details;

  /**
   * 日志的唯一标识
   */
  private Long id;

  /**
   * 操作者的ip地址
   */
  @JsonProperty("ip_address")
  private String ipAddress;

  /**
   * 进行的操作
   */
  private String operate;

  /**
   * 操作时间
   */
  @JsonProperty("operate_time")
  private Long operateTime;

  /**
   * 持续时间
   */
  private Long period;

  /**
   * 操作的状态
   */
  private Long status;

  /**
   * 类型，0 管理员 1 检修员
   */
  private Long type;

  /**
   * 管理员或检修员的id
   */
  @JsonProperty("user_id")
  private Long userId;

  /**
   * 管理员或检修员的用户名
   */
  private String username;
}
