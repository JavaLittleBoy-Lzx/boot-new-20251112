package com.parkingmanage.utils;

import com.parkingmanage.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 活动日志工具类
 * 
 * @author System
 * @since 2024-12-19
 */
@Slf4j
@Component
public class ActivityLogUtil {

    @Resource
    private ActivityLogService activityLogService;
    
    private static ActivityLogUtil instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 记录成功操作日志
     */
    public static void logSuccess(String userId, String username, String module, String action, 
                                 String description) {
        logSuccess(userId, username, module, action, description, null, null, null, null);
    }

    /**
     * 记录成功操作日志（带目标对象）
     */
    public static void logSuccess(String userId, String username, String module, String action, 
                                 String description, String targetId, String targetType) {
        logSuccess(userId, username, module, action, description, targetId, targetType, null, null);
    }

    /**
     * 记录成功操作日志（完整版）
     */
    public static void logSuccess(String userId, String username, String module, String action, 
                                 String description, String targetId, String targetType, 
                                 String oldData, String newData) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = request != null ? getClientIpAddress(request) : "unknown";
            String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
            
            if (instance != null && instance.activityLogService != null) {
                instance.activityLogService.logSuccess(userId, username, module, action, description, 
                                                     targetId, targetType, oldData, newData, 
                                                     ipAddress, userAgent);
            }
        } catch (Exception e) {
            log.error("记录成功日志失败", e);
        }
    }

    /**
     * 记录失败操作日志
     */
    public static void logError(String userId, String username, String module, String action, 
                               String description, String errorMessage) {
        logError(userId, username, module, action, description, null, null, errorMessage);
    }

    /**
     * 记录失败操作日志（带目标对象）
     */
    public static void logError(String userId, String username, String module, String action, 
                               String description, String targetId, String targetType, 
                               String errorMessage) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = request != null ? getClientIpAddress(request) : "unknown";
            String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
            
            if (instance != null && instance.activityLogService != null) {
                instance.activityLogService.logError(userId, username, module, action, description, 
                                                   targetId, targetType, errorMessage, 
                                                   ipAddress, userAgent);
            }
        } catch (Exception e) {
            log.error("记录失败日志失败", e);
        }
    }

    /**
     * 记录车主管理操作
     */
    public static void logOwnerOperation(String userId, String username, String action, 
                                        String description, String ownerId) {
        logSuccess(userId, username, "车主管理", action, description, ownerId, "ownerinfo");
    }

    /**
     * 记录预约管理操作
     */
    public static void logAppointmentOperation(String userId, String username, String action, 
                                             String description, String appointmentId) {
        logSuccess(userId, username, "预约管理", action, description, appointmentId, "appointment");
    }

    /**
     * 记录违规管理操作
     */
    public static void logViolationOperation(String userId, String username, String action, 
                                           String description, String violationId) {
        logSuccess(userId, username, "违规管理", action, description, violationId, "violation");
    }

    /**
     * 记录用户管理操作
     */
    public static void logUserOperation(String userId, String username, String action, 
                                       String description, String targetUserId) {
        logSuccess(userId, username, "用户管理", action, description, targetUserId, "user");
    }

    /**
     * 记录系统操作
     */
    public static void logSystemOperation(String userId, String username, String action, 
                                        String description) {
        logSuccess(userId, username, "系统管理", action, description);
    }

    /**
     * 获取当前请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 