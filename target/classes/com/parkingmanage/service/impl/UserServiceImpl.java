package com.parkingmanage.service.impl;

import com.parkingmanage.dto.LoginRequest;
import com.parkingmanage.dto.LoginResponse;
import com.parkingmanage.entity.SysUser;
import com.parkingmanage.mapper.SysUserMapper;
import com.parkingmanage.service.UserService;
import com.parkingmanage.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 * 
 * @author parking-system
 * @since 2024-12-06
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest loginRequest, String ipAddress) {
        log.info("用户登录请求: username={}, ip={}", loginRequest.getUsername(), ipAddress);

        // 1. 查询用户
        SysUser user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            log.warn("用户不存在: {}", loginRequest.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 检查用户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("用户已被禁用: {}", loginRequest.getUsername());
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("密码错误: {}", loginRequest.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        
        String token = jwtUtil.generateToken(claims);

        // 5. 更新登录信息
        userMapper.updateLoginInfo(user.getId(), ipAddress);

        // 6. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L); // 24小时

        // 7. 构建用户信息（不包含密码）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("name", user.getUsername()); // 简化版：使用username作为显示名称
        userInfo.put("role", user.getRole());
        userInfo.put("permissions", getRolePermissions(user.getRole())); // 根据角色返回权限
        userInfo.put("loginTime", LocalDateTime.now());
        
        response.setUser(userInfo);

        log.info("用户登录成功: {}", loginRequest.getUsername());
        return response;
    }

    @Override
    public SysUser findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public SysUser findById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(SysUser user) {
        log.info("创建用户: {}", user.getUsername());
        
        // 检查用户名是否已存在
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 验证密码不为空
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.error("创建用户失败：密码为空, username={}", user.getUsername());
            throw new RuntimeException("密码不能为空");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认启用
        }
        if (user.getLoginCount() == null) {
            user.setLoginCount(0);
        }
        if (user.getIsDeleted() == null) {
            user.setIsDeleted(0);
        }
        
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        return userMapper.insert(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) {
        log.info("更新用户信息: userId={}", user.getId());
        
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        log.info("删除用户: userId={}", userId);
        
        SysUser user = new SysUser();
        user.setId(userId);
        user.setIsDeleted(1);
        user.setUpdateTime(LocalDateTime.now());
        
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);
        
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        log.info("重置密码: userId={}", userId);
        
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        
        return userMapper.updateById(user) > 0;
    }

    @Override
    public String refreshToken(String token) {
        return jwtUtil.refreshToken(token);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public java.util.List<SysUser> getUserList(String keyword) {
        log.info("获取用户列表, 关键字: {}", keyword);
        return userMapper.getUserList(keyword);
    }

    /**
     * 根据角色获取权限
     * 
     * @param role 用户角色
     * @return 权限数组
     */
    private String[] getRolePermissions(String role) {
        if (role == null) {
            return new String[]{"read"};
        }
        
        switch (role) {
            case "admin":
                return new String[]{"*"}; // 管理员拥有所有权限
            case "user":
                return new String[]{"read", "write"}; // 普通用户读写权限
            case "guest":
                return new String[]{"read"}; // 访客只读权限
            default:
                return new String[]{"read"};
        }
    }
}
