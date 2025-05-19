package com.gjq.interceptor;

import com.gjq.common.exception.BusinessException;
import com.gjq.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Token拦截器
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);
    
    @Value("${system.internal-token}")
    private String internalToken;  // 内部服务之间通信的令牌
    
    @Value("${system.admin-user-id}")
    private Long systemUserId;  // 系统用户ID（管理员）

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String remoteAddr = request.getRemoteAddr();
        String requestURI = request.getRequestURI();
        String internalAuth = request.getHeader("X-Internal-Auth");
        
        logger.debug("请求来源IP: {}, URI: {}, Internal-Auth: {}", remoteAddr, requestURI, internalAuth);
        
        // 如果是内部服务（算法服务）的请求，验证内部认证令牌
        if (internalAuth != null && internalToken.equals(internalAuth)) {
            // 设置系统用户ID
            request.setAttribute("userId", systemUserId);
            return true;
        }

        // 获取token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(401, "未登录");
        }

        // 处理Bearer token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // 验证token
            if (!JwtUtils.validateToken(token)) {
                throw new BusinessException(401, "token无效");
            }
            // 获取用户ID并存入request
            Long userId = JwtUtils.getUserId(token);
            request.setAttribute("userId", userId);
            return true;
        } catch (Exception e) {
            throw new BusinessException(401, "token无效");
        }
    }
} 