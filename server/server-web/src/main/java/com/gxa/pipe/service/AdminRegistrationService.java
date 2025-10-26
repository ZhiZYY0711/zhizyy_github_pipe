package com.gxa.pipe.service;

import com.gxa.pipe.pojo.dto.request.AdminLoginRequest;
import com.gxa.pipe.pojo.dto.response.AdminLoginResponse;

public interface AdminRegistrationService {

  /**
   * 管理员登录
   * 
   * @param request 管理员登录请求
   * @return 管理员登录响应
   */
  AdminLoginResponse login(AdminLoginRequest request);
}