package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员登录表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegistration {
    
    /**
     * 管理员的唯一标识
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码哈希值（增强安全性）
     */
    private String password;
    
    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 上次修改密码时间
     */
    private LocalDateTime passwordUpdatedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}