package com.gxa.pipe.interceptor;

import com.gxa.pipe.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT拦截器
 * 用于验证请求中的JWT令牌
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String requestURI = request.getRequestURI();
        log.info("JWT拦截器拦截到请求：{}", requestURI);

        // 对于OPTIONS请求直接放行（预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        
        // 如果没有token，尝试从参数中获取
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        // 如果token以"Bearer "开头，去掉前缀
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证token
        if (!StringUtils.hasText(token)) {
            log.warn("请求中未包含JWT令牌：{}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问，请先登录\",\"data\":null}");
            return false;
        }

        try {
            // 验证token是否有效
            if (!JwtUtils.validateToken(token)) {
                log.warn("JWT令牌验证失败：{}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"令牌无效或已过期\",\"data\":null}");
                return false;
            }

            // 从token中获取用户信息并设置到请求属性中
            String username = JwtUtils.getUsernameFromToken(token);
            request.setAttribute("currentUser", username);
            
            log.info("JWT验证成功，用户：{}", username);
            return true;

        } catch (Exception e) {
            log.error("JWT令牌验证异常：{}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"令牌验证异常\",\"data\":null}");
            return false;
        }
    }
}