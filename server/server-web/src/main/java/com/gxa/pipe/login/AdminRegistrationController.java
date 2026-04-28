package com.gxa.pipe.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.gxa.pipe.utils.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
  public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
    log.info("收到管理员登录请求: {}", request.getUsername());
    return Result.success(adminRegistrationService.login(request));
  }
}
