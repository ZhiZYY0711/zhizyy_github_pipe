package com.gxa.pipe.controller;

import com.gxa.pipe.pojo.dto.request.ManagerLoginRequest;
import com.gxa.pipe.pojo.dto.response.ManagerLoginResponse;
import com.gxa.pipe.service.ManagerService;
import com.gxa.pipe.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    
    private final ManagerService managerService;
    
    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<ManagerLoginResponse> login(@Valid @RequestBody ManagerLoginRequest request) {
        log.info("管理员登录请求: {}", request);
        
        try {
            ManagerLoginResponse response = managerService.login(request);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("管理员登录失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}