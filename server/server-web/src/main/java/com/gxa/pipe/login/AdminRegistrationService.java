package com.gxa.pipe.login;

public interface AdminRegistrationService {

  /**
   * 管理员登录
   * 
   * @param request 管理员登录请求
   * @return 管理员登录响应
   */
  AdminLoginResponse login(AdminLoginRequest request);
}