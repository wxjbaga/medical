package com.gjq.vo.user;

import com.gjq.client.FileClient;
import com.gjq.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 用户信息
 */
@Data
@NoArgsConstructor
public class UserInfo {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像桶 
     */
    private String avatarBucket;

    /**
     * 头像对象键
     */
    private String avatarObjectKey;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色(0:普通用户 1:管理员)
     */
    private Integer role;

    /**
     * 状态(0:禁用 1:启用)
     */
    private Integer status;

    public UserInfo(User user, FileClient fileClient) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.realName = user.getRealName();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.avatarBucket = user.getAvatarBucket();
        this.avatarObjectKey = user.getAvatarObjectKey();
        if (StringUtils.hasText(user.getAvatarBucket()) && StringUtils.hasText(user.getAvatarObjectKey())) {
            this.avatarUrl = fileClient.getFileUrl(user.getAvatarBucket(), user.getAvatarObjectKey());
        }
        this.role = user.getRole();
        this.status = user.getStatus();
    }
} 