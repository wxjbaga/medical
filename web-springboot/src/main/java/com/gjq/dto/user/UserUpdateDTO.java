package com.gjq.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO {
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 头像存储桶
     */
    private String avatarBucket;

    /**
     * 头像对象键
     */
    private String avatarObjectKey;

    /**
     * 角色(0:普通用户 1:管理员)
     */
    private Integer role;

    /**
     * 状态(0:禁用 1:启用)
     */
    private Integer status;
} 