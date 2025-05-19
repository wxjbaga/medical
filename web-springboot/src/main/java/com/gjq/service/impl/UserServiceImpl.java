package com.gjq.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjq.client.FileClient;
import com.gjq.common.exception.BusinessException;
import com.gjq.dto.user.*;
import com.gjq.vo.user.*;
import com.gjq.entity.User;
import com.gjq.mapper.UserMapper;
import com.gjq.service.UserService;
import com.gjq.utils.SecurityUtils;
import com.gjq.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor  // 自动为所有标记了 final 的字段生成构造函数并进行依赖注入
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final FileClient fileClient;

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        // 查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .one();
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验密码
        if (!SecureUtil.md5(dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 校验状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 创建登录响应对象
        UserLoginVO loginVO = new UserLoginVO(user, null, fileClient);
        // 生成token
        String token = JwtUtils.generateToken(loginVO.getUserInfo().getId());
        // 设置token
        loginVO.setToken(token);
        return loginVO;
    }

    @Override
    public void register(UserRegisterDTO dto) {
        // 校验用户名是否存在
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = BeanUtil.copyProperties(dto, User.class);
        // 设置密码
        user.setPassword(SecureUtil.md5(dto.getPassword()));
        // 设置角色
        user.setRole(0);
        // 设置状态
        user.setStatus(1);

        // 保存用户
        save(user);
    }

    @Override
    public void updatePassword(UserPasswordDTO dto) {
        // 查询用户
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验原密码
        if (!SecureUtil.md5(dto.getOldPassword()).equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 更新密码
        user.setPassword(SecureUtil.md5(dto.getNewPassword()));
        updateById(user);
    }

    @Override
    public void updateUser(UserUpdateDTO dto) {
        // 查询用户
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 如果更新了头像，需要删除旧头像
        if ((dto.getAvatarBucket() != null && !dto.getAvatarBucket().equals(user.getAvatarBucket())) ||
            (dto.getAvatarObjectKey() != null && !dto.getAvatarObjectKey().equals(user.getAvatarObjectKey()))) {
            if (StrUtil.isNotBlank(user.getAvatarBucket()) && StrUtil.isNotBlank(user.getAvatarObjectKey())) {
                try {
                    fileClient.delete(user.getAvatarBucket(), user.getAvatarObjectKey());
                } catch (Exception e) {
                    // 删除失败不影响更新
                    log.error("删除旧头像失败", e);
                }
            }
        }

        // 更新用户信息
        BeanUtil.copyProperties(dto, user);
        updateById(user);
    }

    @Override
    public IPage<User> getUserPage(UserQueryDTO dto) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(dto.getUsername()), User::getUsername, dto.getUsername())
                .like(StrUtil.isNotBlank(dto.getRealName()), User::getRealName, dto.getRealName())
                .like(StrUtil.isNotBlank(dto.getPhone()), User::getPhone, dto.getPhone())
                .like(StrUtil.isNotBlank(dto.getEmail()), User::getEmail, dto.getEmail())
                .eq(dto.getRole() != null, User::getRole, dto.getRole())
                .eq(dto.getStatus() != null, User::getStatus, dto.getStatus());

        // 执行分页查询
        return page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
    }

    @Override
    public UserLoginVO getCurrentUser() {
        // 使用 SecurityUtils 获取当前用户
        User user = SecurityUtils.getCurrentUser();
        // 返回用户信息（不需要返回 token，因为 token 已经在客户端了）
        return new UserLoginVO(user, null, fileClient);
    }

    @Override
    public void logout() {
        // 前端清除 token 即可，后端不需要处理
    }

    @Override
    public void addUser(UserAddDTO dto) {
        // 校验用户名是否存在
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = BeanUtil.copyProperties(dto, User.class);
        // 设置密码（使用MD5加密）
        user.setPassword(SecureUtil.md5(dto.getPassword()));
        // 设置状态为正常
        user.setStatus(1);

        // 保存用户
        save(user);
    }

    @Override
    public void resetPassword(Long id) {
        // 查询用户
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 重置密码为123456
        user.setPassword(SecureUtil.md5("123456"));
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 查询用户
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能修改管理员状态
        if (user.getRole() == 1) {
            throw new BusinessException("不能修改管理员状态");
        }

        // 更新状态
        user.setStatus(status);
        updateById(user);
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        // 查询用户
        User user = getById(id);
        if (user == null) {
            return false;
        }

        // 不能删除管理员
        if (user.getRole() == 1) {
            throw new BusinessException("不能删除管理员");
        }

        // 删除用户头像
        if (StrUtil.isNotBlank(user.getAvatarBucket()) && StrUtil.isNotBlank(user.getAvatarObjectKey())) {
            try {
                fileClient.delete(user.getAvatarBucket(), user.getAvatarObjectKey());
            } catch (Exception e) {
                log.error("删除用户头像失败", e);
            }
        }

        // 删除用户
        return super.removeById(id);
    }
} 