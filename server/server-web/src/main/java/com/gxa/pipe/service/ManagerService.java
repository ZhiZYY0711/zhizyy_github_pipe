package com.gxa.pipe.service;

import com.gxa.pipe.pojo.entity.Manager;
import com.gxa.pipe.pojo.dto.request.ManagerLoginRequest;
import com.gxa.pipe.pojo.dto.response.ManagerLoginResponse;

/**
 * 管理员服务接口
 */
public interface ManagerService {
    
    /**
     * 管理员登录
     * @param request 登录请求
     * @return 登录响应
     */
    ManagerLoginResponse login(ManagerLoginRequest request);
    
    /**
     * 根据用户名查询管理员
     * @param username 用户名
     * @return 管理员信息
     */
    Manager getByUsername(String username);
    
    /**
     * 根据ID查询管理员
     * @param id 管理员ID
     * @return 管理员信息
     */
    Manager getById(Long id);
    
    /**
     * 创建管理员
     * @param manager 管理员信息
     * @return 是否成功
     */
    boolean create(Manager manager);
    
    /**
     * 更新管理员
     * @param manager 管理员信息
     * @return 是否成功
     */
    boolean update(Manager manager);
    
    /**
     * 删除管理员
     * @param id 管理员ID
     * @return 是否成功
     */
    boolean delete(Long id);
}