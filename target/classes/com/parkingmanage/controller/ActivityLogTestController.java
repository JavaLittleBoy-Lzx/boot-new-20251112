package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.service.ActivityLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 活动日志测试控制器
 * 用于测试和验证日志记录功能
 */
@Slf4j
@RestController
@RequestMapping("/parking/test/log")
@CrossOrigin
@Api(tags = "活动日志测试")
public class ActivityLogTestController {

    @Resource
    private ActivityLogService activityLogService;

    @ApiOperation("测试记录日志")
    @PostMapping("/test-record")
    public ResponseEntity<Result> testRecordLog(@RequestBody Map<String, String> params, HttpServletRequest request) {
        try {
            String module = params.getOrDefault("module", "测试模块");
            String action = params.getOrDefault("action", "测试操作");
            String description = params.getOrDefault("description", "这是一个测试日志记录");
            String userId = params.getOrDefault("userId", "test_user");
            
            // 记录测试日志
            ActivityLog log = new ActivityLog();
            log.setUserId(userId);
            log.setUsername("测试用户");
            log.setModule(module);
            log.setAction(action);
            log.setDescription(description);
            log.setStatus("success");
            log.setCreatedAt(LocalDateTime.now());
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            activityLogService.save(log);
            
            return ResponseEntity.ok(Result.success("测试日志记录成功"));
        } catch (Exception e) {
            log.error("测试日志记录失败", e);
            return ResponseEntity.ok(Result.error("测试日志记录失败：" + e.getMessage()));
        }
    }

    @ApiOperation("获取所有日志")
    @GetMapping("/all")
    public ResponseEntity<Result> getAllLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            List<ActivityLog> logs = activityLogService.getAllLogs(pageNum, pageSize);
            return ResponseEntity.ok(Result.success(logs));
        } catch (Exception e) {
            log.error("获取日志失败", e);
            return ResponseEntity.ok(Result.error("获取日志失败"));
        }
    }

    @ApiOperation("根据用户ID获取日志")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result> getLogsByUser(@PathVariable String userId,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            List<ActivityLog> logs = activityLogService.getUserOperationLogs(Integer.parseInt(userId), pageNum, pageSize);
            return ResponseEntity.ok(Result.success(logs));
        } catch (Exception e) {
            log.error("获取用户日志失败", e);
            return ResponseEntity.ok(Result.error("获取用户日志失败"));
        }
    }

    @ApiOperation("根据模块获取日志")
    @GetMapping("/module/{module}")
    public ResponseEntity<Result> getLogsByModule(@PathVariable String module,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            List<ActivityLog> logs = activityLogService.getLogsByModule(module, pageNum, pageSize);
            return ResponseEntity.ok(Result.success(logs));
        } catch (Exception e) {
            log.error("获取模块日志失败", e);
            return ResponseEntity.ok(Result.error("获取模块日志失败"));
        }
    }

    @ApiOperation("获取日志统计信息")
    @GetMapping("/statistics")
    public ResponseEntity<Result> getLogStatistics() {
        try {
            Map<String, Object> statistics = activityLogService.getLogStatistics();
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            return ResponseEntity.ok(Result.error("获取日志统计失败"));
        }
    }

    @ApiOperation("清理过期日志")
    @DeleteMapping("/cleanup")
    public ResponseEntity<Result> cleanupExpiredLogs(@RequestParam(defaultValue = "30") Integer days) {
        try {
            int deletedCount = activityLogService.cleanupExpiredLogs(days);
            return ResponseEntity.ok(Result.success("清理了 " + deletedCount + " 条过期日志"));
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            return ResponseEntity.ok(Result.error("清理过期日志失败"));
        }
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
