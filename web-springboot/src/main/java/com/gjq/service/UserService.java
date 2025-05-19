package com.gjq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjq.dto.user.*;
import com.gjq.vo.user.*;
import com.gjq.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户登录
     *
     * @param dto 登录参数
     * @return 登录用户信息
     */
    UserLoginVO login(UserLoginDTO dto);

    /**
     * 用户注册
     *
     * @param dto 注册参数
     */
    void register(UserRegisterDTO dto);

    /**
     * 修改密码
     *
     * @param dto 修改密码参数
     */
    void updatePassword(UserPasswordDTO dto);

    /**
     * 更新用户信息
     *
     * @param dto 用户信息
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 分页查询用户列表
     *
     * @param dto 查询参数
     * @return 用户列表
     */
    IPage<User> getUserPage(UserQueryDTO dto);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    UserLoginVO getCurrentUser();

    /**
     * 退出登录
     */
    void logout();

    /**
     * 新增用户
     */
    void addUser(UserAddDTO dto);

    /**
     * 重置密码
     */
    void resetPassword(Long id);

    /**
     * 更新用户状态
     */
    void updateStatus(Long id, Integer status);
} 