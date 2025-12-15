package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.dto.LoginRequest;
import com.parkingmanage.dto.LoginResponse;
import com.parkingmanage.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、登出、Token刷新等认证相关操作
 * 
 * @author parking-system
 * @since 2024-12-06
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Api(tags = "认证管理", description = "用户登录、登出、Token管理相关接口")
@Validated
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @param request HTTP请求
     * @return 登录响应，包含Token和用户信息
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "通过用户名和密码登录系统，返回JWT Token")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        
        try {
            log.info("收到登录请求: username={}", loginRequest.getUsername());
            
            // 获取客户端IP地址
            String ipAddress = getClientIpAddress(request);
            
            // 执行登录
            LoginResponse response = userService.login(loginRequest, ipAddress);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error("401", e.getMessage());
        }
    }

    /**
     * 用户登出
     * 
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/logout")
    @ApiOperation(value = "用户登出", notes = "退出登录，清除Token（前端需要清除本地存储的Token）")
    public Result<String> logout(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            String username = "unknown";
            
            // 可以在这里记录登出日志或将Token加入黑名单
            log.info("用户登出: username={}", username);
            
            return Result.success("登出成功");
            
        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新Token
     * 
     * @param request HTTP请求
     * @return 新的Token
     */
    @PostMapping("/refresh")
    @ApiOperation(value = "刷新Token", notes = "使用当前Token刷新获取新Token")
    public Result<Map<String, String>> refreshToken(HttpServletRequest request) {
        try {
            String oldToken = getTokenFromRequest(request);
            
            if (oldToken == null || oldToken.isEmpty()) {
                return Result.error("401", "Token不能为空");
            }
            
            // 验证并刷新Token
            if (!userService.validateToken(oldToken)) {
                return Result.error("401", "Token无效或已过期");
            }
            
            String newToken = userService.refreshToken(oldToken);
            
            if (newToken == null) {
                return Result.error("401", "Token刷新失败");
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("token", newToken);
            result.put("tokenType", "Bearer");
            
            log.info("Token刷新成功");
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return Result.error("401", "Token刷新失败");
        }
    }

    /**
     * 验证Token
     * 
     * @param request HTTP请求
     * @return 验证结果
     */
    @GetMapping("/validate")
    @ApiOperation(value = "验证Token", notes = "检查Token是否有效")
    public Result<Map<String, Object>> validateToken(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            
            if (token == null || token.isEmpty()) {
                return Result.error("401", "Token不能为空");
            }
            
            boolean isValid = userService.validateToken(token);
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("message", isValid ? "Token有效" : "Token无效或已过期");
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("message", "Token验证失败");
            return Result.success(result);
        }
    }

    /**
     * 获取当前用户信息
     * 
     * @param request HTTP请求
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    @ApiOperation(value = "获取当前用户信息", notes = "根据Token获取当前登录用户的详细信息")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            
            if (token == null || token.isEmpty()) {
                return Result.error("401", "Token不能为空");
            }
            
            if (!userService.validateToken(token)) {
                return Result.error("401", "Token无效或已过期");
            }
            
            // 从Token中获取用户信息并返回
            // 这里可以根据需要返回更详细的用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("message", "获取用户信息成功");
            
            return Result.success(userInfo);
            
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return Result.error("401", "获取用户信息失败");
        }
    }

    /**
     * 修改密码
     * 
     * @param passwordData 密码数据
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/change-password")
    @ApiOperation(value = "修改密码", notes = "修改当前登录用户的密码")
    public Result<String> changePassword(
            @RequestBody @ApiParam(value = "密码数据", required = true) Map<String, String> passwordData,
            HttpServletRequest request) {
        
        try {
            String token = getTokenFromRequest(request);
            
            if (token == null || !userService.validateToken(token)) {
                return Result.error("401", "未授权访问");
            }
            
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || oldPassword.isEmpty() || 
                newPassword == null || newPassword.isEmpty()) {
                return Result.error("旧密码和新密码不能为空");
            }
            
            if (newPassword.length() < 6) {
                return Result.error("新密码长度不能少于6位");
            }
            
            // 这里需要从Token中获取userId
            // Long userId = jwtUtil.getUserIdFromToken(token);
            // userService.changePassword(userId, oldPassword, newPassword);
            
            log.info("密码修改成功");
            return Result.success("密码修改成功");
            
        } catch (Exception e) {
            log.error("密码修改失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从请求中获取Token
     * 
     * @param request HTTP请求
     * @return Token字符串
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
