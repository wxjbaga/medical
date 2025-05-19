package com.gjq.utils;

import com.gjq.common.exception.BusinessException;
import com.gjq.entity.User;
import com.gjq.service.UserService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全工具类
 */
@Component
public class SecurityUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取当前登录用户
     */
    public static User getCurrentUser() {
        Long userId = getUserId();
        UserService userService = applicationContext.getBean(UserService.class);
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        Long userId = (Long) ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && user.getRole() == 1;
    }
} 