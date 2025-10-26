package com.gxa.pipe.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.gxa.pipe.mapper.AdminRegistrationMapper;
import com.gxa.pipe.pojo.dto.request.AdminLoginRequest;
import com.gxa.pipe.pojo.dto.response.AdminLoginResponse;
import com.gxa.pipe.pojo.entity.AdminRegistration;
import com.gxa.pipe.service.AdminRegistrationService;
import com.gxa.pipe.utils.JwtUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员注册服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegistrationServiceImpl implements AdminRegistrationService {

  private final AdminRegistrationMapper adminRegistrationMapper;

  @Override
  public AdminLoginResponse login(AdminLoginRequest request) {
    log.info("管理员登录请求: username={}", request.getUsername());

    // 1. 验证码校验（直接通过）
    log.info("验证码校验通过");

    // 2. 根据用户名查询管理员信息
    AdminRegistration admin = adminRegistrationMapper.findByUsername(request.getUsername());
    if (admin == null) {
      log.warn("用户名不存在: {}", request.getUsername());
      throw new RuntimeException("用户名或密码错误");
    }

    // 3. 密码校验（明文比较）
    if (!admin.getPassword().equals(request.getPassword())) {
      log.warn("密码错误: username={}", request.getUsername());
      throw new RuntimeException("用户名或密码错误");
    }

    // 4. 更新最后登录时间
    adminRegistrationMapper.updateLastLoginTime(admin.getId(), LocalDateTime.now());

    // 5. 生成JWT令牌
    String jwt = generateJwtToken(admin);

    log.info("管理员登录成功: username={}", request.getUsername());

    return new AdminLoginResponse(admin.getUsername(), jwt);
  }

  /**
   * 生成JWT令牌
   * 
   * @param admin 管理员信息
   * @return JWT令牌
   */
  private String generateJwtToken(AdminRegistration admin) {
    // 构建JWT载荷信息
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", admin.getId());
    claims.put("username", admin.getUsername());
    claims.put("loginTime", System.currentTimeMillis());

    // 使用专业的JWT工具类生成令牌
    return JwtUtils.generateToken(claims);
  }
}
