package com.gxa.pipe.pojo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerLoginResponse {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * jwt令牌
     */
    private String jwt;
}