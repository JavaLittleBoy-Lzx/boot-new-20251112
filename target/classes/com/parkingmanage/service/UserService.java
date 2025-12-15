package com.parkingmanage.service;

import com.parkingmanage.dto.LoginRequest;
import com.parkingmanage.dto.LoginResponse;
import com.parkingmanage.entity.SysUser;

/**
 * 用户服务接口
 * 
 * @author parking-system
 * @since 2024-12-06
 */
public interface UserService {

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @param ipAddress IP地址
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest, String ipAddress);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    SysUser findByUsername(String username);

    /**
     * 根据ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    SysUser findById(Long userId);

    /**
     * 创建用户
     * 
     * @param user 用户信息
     * @return 是否成功
     */
    boolean createUser(SysUser user);

    /**
     * 更新用户信息
     * 
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUser(SysUser user);

    /**
     * 删除用户（逻辑删除）
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);

    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     * 
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 刷新Token
     * 
     * @param token 原Token
     * @return 新Token
     */
    String refreshToken(String token);

    /**
     * 验证Token
     * 
     * @param token Token
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 获取用户列表
     * 
     * @param keyword 搜索关键字（可选）
     * @return 用户列表
     */
    java.util.List<SysUser> getUserList(String keyword);
}
