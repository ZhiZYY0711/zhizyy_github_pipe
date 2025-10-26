package com.gxa.pipe.service.impl;

import com.gxa.pipe.mapper.ManagerMapper;
import com.gxa.pipe.pojo.entity.Manager;
import com.gxa.pipe.pojo.dto.request.ManagerLoginRequest;
import com.gxa.pipe.pojo.dto.response.ManagerLoginResponse;
import com.gxa.pipe.service.ManagerService;
import com.gxa.pipe.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerServiceImpl implements ManagerService {
    
    private final ManagerMapper managerMapper;
    
    @Override
    public ManagerLoginResponse login(ManagerLoginRequest request) {
        log.info("管理员登录，用户名：{}", request.getUsername());
        
        // TODO: 验证验证码
        
        // 根据用户名查询管理员
        Manager manager = managerMapper.selectByUsername(request.getUsername());
        if (manager == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码（使用MD5加密）
        String encryptedPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!encryptedPassword.equals(manager.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", manager.getId());
        claims.put("username", manager.getUsername());
        claims.put("name", manager.getName());
        String token = JwtUtils.generateToken(claims);
        
        // 构建响应
        ManagerLoginResponse response = new ManagerLoginResponse();
        response.setUsername(manager.getUsername());
        response.setJwt(token);
        
        log.info("管理员登录成功，用户名：{}", request.getUsername());
        return response;
    }
    
    @Override
    public Manager getByUsername(String username) {
        return managerMapper.selectByUsername(username);
    }
    
    @Override
    public Manager getById(Long id) {
        return managerMapper.selectById(id);
    }
    
    @Override
    public boolean create(Manager manager) {
        // 加密密码
        if (manager.getPassword() != null) {
            manager.setPassword(DigestUtils.md5DigestAsHex(manager.getPassword().getBytes()));
        }
        
        manager.setCreateTime(LocalDateTime.now());
        manager.setUpdateTime(LocalDateTime.now());
        
        return managerMapper.insert(manager) > 0;
    }
    
    @Override
    public boolean update(Manager manager) {
        // 如果有密码更新，需要加密
        if (manager.getPassword() != null) {
            manager.setPassword(DigestUtils.md5DigestAsHex(manager.getPassword().getBytes()));
        }
        
        manager.setUpdateTime(LocalDateTime.now());
        
        return managerMapper.update(manager) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return managerMapper.deleteById(id) > 0;
    }
}