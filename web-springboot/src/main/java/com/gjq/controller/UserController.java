package com.gjq.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gjq.common.Result;
import com.gjq.dto.user.*;
import com.gjq.vo.user.*;
import com.gjq.entity.User;
import com.gjq.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录接口")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "用户注册接口")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改用户密码接口")
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody @Valid UserPasswordDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新用户基本信息接口")
    @PutMapping
    public Result<Void> updateUser(@RequestBody @Valid UserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.success();
    }

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表接口")
    @GetMapping("/page")
    public Result<IPage<User>> getUserPage(UserQueryDTO dto) {
        return Result.success(userService.getUserPage(dto));
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户信息接口")
    @GetMapping("/current")
    public Result<UserLoginVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录", description = "用户退出登录接口")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
    @GetMapping("/{id}")
    public Result<User> getUserInfo(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户", description = "新增用户接口")
    @PostMapping
    public Result<Void> addUser(@RequestBody @Valid UserAddDTO dto) {
        userService.addUser(dto);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除指定用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        if (!userService.removeById(id)) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置指定用户的密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态", description = "更新指定用户的状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody @Valid UserStatusDTO dto) {
        userService.updateStatus(id, dto.getStatus());
        return Result.success();
    }
} 