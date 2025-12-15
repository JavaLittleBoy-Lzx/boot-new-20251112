package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.service.ActivityLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 活动日志 前端控制器
 * </p>
 *
 * @author System
 * @since 2024-12-19
 */
@Slf4j
@Api(tags = "活动日志管理")
@RestController
@RequestMapping("/parking/activity-log")
public class ActivityLogController {

    @Resource
    private ActivityLogService activityLogService;

    /**
     * 分页查询活动日志
     */
    @ApiOperation("分页查询活动日志")
    @GetMapping("/page")
    public Result<IPage<ActivityLog>> getActivityLogPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @ApiParam("用户ID") @RequestParam(required = false) String userId,
            @ApiParam("操作模块") @RequestParam(required = false) String module,
            @ApiParam("操作动作") @RequestParam(required = false) String action,
            @ApiParam("操作状态") @RequestParam(required = false) String status,
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            Page<ActivityLog> page = new Page<>(pageNum, pageSize);
            IPage<ActivityLog> result = activityLogService.getActivityLogPage(
                    page, userId, module, action, status, startTime, endTime);
            return Result.<IPage<ActivityLog>>success(result);
        } catch (Exception e) {
            log.error("查询活动日志失败", e);
            return Result.error("查询活动日志失败：" + e.getMessage());
        }
    }

    /**
     * 记录活动日志
     */
    @ApiOperation("记录活动日志")
    @PostMapping("/log")
    public Result<Boolean> logActivity(@RequestBody ActivityLog activityLog, HttpServletRequest request) {
        try {
            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            boolean success = activityLogService.logActivity(
                    activityLog.getUserId(),
                    activityLog.getUsername(),
                    activityLog.getModule(),
                    activityLog.getAction(),
                    activityLog.getDescription(),
                    activityLog.getTargetId(),
                    activityLog.getTargetType(),
                    activityLog.getOldData(),
                    activityLog.getNewData(),
                    ipAddress,
                    userAgent,
                    activityLog.getStatus(),
                    activityLog.getErrorMessage(),
                    activityLog.getDuration(),
                    activityLog.getRemark()
            );
            
            return success ? Result.<Boolean>success(true) : Result.error("记录日志失败");
        } catch (Exception e) {
            log.error("记录活动日志失败", e);
            return Result.error("记录活动日志失败：" + e.getMessage());
        }
    }

    /**
     * 记录成功操作日志
     */
    @ApiOperation("记录成功操作日志")
    @PostMapping("/log-success")
    public Result<Boolean> logSuccess(@RequestBody Map<String, Object> logData, HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            boolean success = activityLogService.logSuccess(
                    convertToString(logData.get("userId")),
                    convertToString(logData.get("username")),
                    convertToString(logData.get("module")),
                    convertToString(logData.get("action")),
                    convertToString(logData.get("description")),
                    convertToString(logData.get("targetId")),
                    convertToString(logData.get("targetType")),
                    convertToString(logData.get("oldData")),
                    convertToString(logData.get("newData")),
                    ipAddress,
                    userAgent
            );
            
            return success ? Result.<Boolean>success(true) : Result.error("记录日志失败");
        } catch (Exception e) {
            log.error("记录成功日志失败", e);
            return Result.error("记录成功日志失败：" + e.getMessage());
        }
    }

    /**
     * 记录失败操作日志
     */
    @ApiOperation("记录失败操作日志")
    @PostMapping("/log-error")
    public Result<Boolean> logError(@RequestBody Map<String, Object> logData, HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            boolean success = activityLogService.logError(
                    convertToString(logData.get("userId")),
                    convertToString(logData.get("username")),
                    convertToString(logData.get("module")),
                    convertToString(logData.get("action")),
                    convertToString(logData.get("description")),
                    convertToString(logData.get("targetId")),
                    convertToString(logData.get("targetType")),
                    convertToString(logData.get("errorMessage")),
                    ipAddress,
                    userAgent
            );
            
            return success ? Result.<Boolean>success(true) : Result.error("记录日志失败");
        } catch (Exception e) {
            log.error("记录失败日志失败", e);
            return Result.error("记录失败日志失败：" + e.getMessage());
        }
    }

    /**
     * 统计用户操作次数
     */
    @ApiOperation("统计用户操作次数")
    @GetMapping("/count-by-user/{userId}")
    public Result<Long> countByUserId(
            @ApiParam("用户ID") @PathVariable String userId,
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            Long count = activityLogService.countByUserId(userId, startTime, endTime);
            return Result.<Long>success(count);
        } catch (Exception e) {
            log.error("统计用户操作次数失败", e);
            return Result.error("统计用户操作次数失败：" + e.getMessage());
        }
    }

    /**
     * 统计各模块操作次数
     */
    @ApiOperation("统计各模块操作次数")
    @GetMapping("/count-by-module")
    public Result<List<Map<String, Object>>> countByModule(
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            List<Map<String, Object>> result = activityLogService.countByModule(startTime, endTime);
            return Result.<List<Map<String, Object>>>success(result);
        } catch (Exception e) {
            log.error("统计模块操作次数失败", e);
            return Result.error("统计模块操作次数失败：" + e.getMessage());
        }
    }

    /**
     * 统计各操作类型次数
     */
    @ApiOperation("统计各操作类型次数")
    @GetMapping("/count-by-action")
    public Result<List<Map<String, Object>>> countByAction(
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            List<Map<String, Object>> result = activityLogService.countByAction(startTime, endTime);
            return Result.<List<Map<String, Object>>>success(result);
        } catch (Exception e) {
            log.error("统计操作类型次数失败", e);
            return Result.error("统计操作类型次数失败：" + e.getMessage());
        }
    }

    /**
     * 获取活动统计数据
     */
    @ApiOperation("获取活动统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getActivityStatistics(
            @ApiParam("开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            Map<String, Object> statistics = activityLogService.getActivityStatistics(startTime, endTime);
            return Result.<Map<String, Object>>success(statistics);
        } catch (Exception e) {
            log.error("获取活动统计数据失败", e);
            return Result.error("获取活动统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 清理过期日志
     */
    @ApiOperation("清理过期日志")
    @DeleteMapping("/clean-expired")
    public Result<Integer> cleanExpiredLogs(@ApiParam("保留天数") @RequestParam(defaultValue = "90") Integer days) {
        try {
            int cleanedCount = activityLogService.cleanExpiredLogs(days);
            return Result.<Integer>success(cleanedCount);
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            return Result.error("清理过期日志失败：" + e.getMessage());
        }
    }

    /**
     * 安全地将对象转换为字符串
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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