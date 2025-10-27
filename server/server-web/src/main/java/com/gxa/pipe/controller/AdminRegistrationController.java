package com.gxa.pipe.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gxa.pipe.pojo.dto.AdminLoginRequest;
import com.gxa.pipe.pojo.vo.AdminLoginResponse;
import com.gxa.pipe.service.AdminRegistrationService;

import jakarta.validation.Valid;

/**
 * 管理员注册控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminRegistrationController {
  private final AdminRegistrationService adminRegistrationService;

  /**
   * 管理员登录
   * 
   * @param request 管理员登录请求
   * @return 管理员登录响应
   */
  @PostMapping("/login")
  public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
    log.info("收到管理员登录请求: {}", request.getUsername());
    return adminRegistrationService.login(request);
  }
}
