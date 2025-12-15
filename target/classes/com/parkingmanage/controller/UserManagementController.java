package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.SysUser;
import com.parkingmanage.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户的增删改查功能
 * 
 * @author parking-system
 * @since 2024-12-06
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserManagementController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Result<List<SysUser>> getUserList(@RequestParam(required = false) String keyword) {
        log.info("获取用户列表, 关键字: {}", keyword);
        
        try {
            List<SysUser> userList = userService.getUserList(keyword);
            return Result.success(userList);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping("/create")
    public Result<String> createUser(@RequestBody SysUser user) {
        log.info("创建用户: {}", user.getUsername());
        log.info("接收到的用户对象 - username: {}, role: {}, status: {}", 
                 user.getUsername(), user.getRole(), user.getStatus());
        log.info("密码字段检查 - password: {}, isNull: {}, isEmpty: {}", 
                 user.getPassword() != null ? "***" : "NULL", 
                 user.getPassword() == null, 
                 user.getPassword() != null && user.getPassword().isEmpty());
        
        try {
            boolean success = userService.createUser(user);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public Result<String> updateUser(@RequestBody SysUser user) {
        log.info("更新用户: userId={}", user.getId());
        
        try {
            boolean success = userService.updateUser(user);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{userId}")
    public Result<String> deleteUser(@PathVariable Long userId) {
        log.info("删除用户: userId={}", userId);
        
        try {
            boolean success = userService.deleteUser(userId);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        String newPassword = params.get("newPassword").toString();
        
        log.info("重置密码: userId={}", userId);
        
        try {
            boolean success = userService.resetPassword(userId, newPassword);
            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.error("密码重置失败");
            }
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public Result<SysUser> getUserDetail(@PathVariable Long userId) {
        log.info("获取用户详情: userId={}", userId);
        
        try {
            SysUser user = userService.findById(userId);
            if (user != null) {
                // 清除密码字段
                user.setPassword(null);
                return Result.success(user);
            } else {
                return Result.error("用户不存在");
            }
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
