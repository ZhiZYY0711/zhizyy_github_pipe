package com.gxa.pipe.pojo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {
  /**
   * 区域代码
   */
  private String code;
  /**
   * 区域名称
   */
  private String name;
}
