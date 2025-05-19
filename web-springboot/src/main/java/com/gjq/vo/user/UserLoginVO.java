package com.gjq.vo.user;

import com.gjq.client.FileClient;
import com.gjq.entity.User;
import lombok.Data;

/**
 * 用户登录响应DTO
 */
@Data
public class UserLoginVO {
    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * token
     */
    private String token;

    public UserLoginVO(User user, String token, FileClient fileClient) {
        this.userInfo = new UserInfo(user, fileClient);
        this.token = token;
    }
} 