package com.gxa.pipe.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员登录请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码 前端进行加密后传输
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码 6位数
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}