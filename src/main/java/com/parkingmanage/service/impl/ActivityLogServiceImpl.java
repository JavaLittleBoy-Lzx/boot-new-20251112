package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ActivityLog;
import com.parkingmanage.mapper.ActivityLogMapper;
import com.parkingmanage.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 活动日志服务实现
 */
@Slf4j
@Service
public class ActivityLogServiceImpl extends ServiceImpl<ActivityLogMapper, ActivityLog> implements ActivityLogService {

    @Override
    public List<ActivityLog> getUserOperationLogs(Integer userId, Integer pageNum, Integer pageSize) {
        Page<ActivityLog> page = new Page<>(pageNum, pageSize);
        return this.page(
            page,
            new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getUserId, userId.toString())
                .orderByDesc(ActivityLog::getCreatedAt)
        ).getRecords();
    }

    @Override
    public long getUserOperationCount(Integer userId) {
        return this.count(
            new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getUserId, userId.toString())
        );
    }

    @Override
    public void recordUserOperation(Integer userId, String module, String action, String description, String ipAddress, String userAgent) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId(userId.toString());
            log.setModule(module);
            log.setAction(action);
            log.setDescription(description);
            log.setStatus("success");
            log.setCreatedAt(LocalDateTime.now());
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            
            this.save(log);
        } catch (Exception e) {
            log.warn("记录用户操作日志失败", e);
        }
    }

    @Override
    public void recordSystemOperation(String module, String action, String description, String ipAddress, String userAgent) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId("system");
            log.setUsername("系统管理员");
            log.setModule(module);
            log.setAction(action);
            log.setDescription(description);
            log.setStatus("success");
            log.setCreatedAt(LocalDateTime.now());
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            
            this.save(log);
        } catch (Exception e) {
            log.warn("记录系统操作日志失败", e);
        }
    }

    @Override
    public List<ActivityLog> getAllLogs(Integer pageNum, Integer pageSize) {
        Page<ActivityLog> page = new Page<>(pageNum, pageSize);
        return this.page(
            page,
            new LambdaQueryWrapper<ActivityLog>()
                .orderByDesc(ActivityLog::getCreatedAt)
        ).getRecords();
    }

    @Override
    public List<ActivityLog> getLogsByModule(String module, Integer pageNum, Integer pageSize) {
        Page<ActivityLog> page = new Page<>(pageNum, pageSize);
        return this.page(
            page,
            new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getModule, module)
                .orderByDesc(ActivityLog::getCreatedAt)
        ).getRecords();
    }

    @Override
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总日志数
        long totalLogs = this.count();
        statistics.put("totalLogs", totalLogs);
        
        // 成功日志数
        long successLogs = this.count(
            new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getStatus, "success")
        );
        statistics.put("successLogs", successLogs);
        
        // 失败日志数
        long failedLogs = this.count(
            new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getStatus, "failed")
        );
        statistics.put("failedLogs", failedLogs);
        
        // 今日日志数
        long todayLogs = this.count(
            new LambdaQueryWrapper<ActivityLog>()
                .ge(ActivityLog::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())
        );
        statistics.put("todayLogs", todayLogs);
        
        return statistics;
    }

    @Override
    public int cleanupExpiredLogs(Integer days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return this.baseMapper.delete(
            new LambdaQueryWrapper<ActivityLog>()
                .lt(ActivityLog::getCreatedAt, cutoffDate)
        );
    }

    @Override
    public IPage<ActivityLog> getActivityLogPage(Page<ActivityLog> page, String userId, String module, 
                                               String action, String status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (userId != null && !userId.trim().isEmpty()) {
            queryWrapper.eq(ActivityLog::getUserId, userId);
        }
        if (module != null && !module.trim().isEmpty()) {
            queryWrapper.eq(ActivityLog::getModule, module);
        }
        if (action != null && !action.trim().isEmpty()) {
            queryWrapper.eq(ActivityLog::getAction, action);
        }
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq(ActivityLog::getStatus, status);
        }
        if (startTime != null) {
            queryWrapper.ge(ActivityLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ActivityLog::getCreatedAt, endTime);
        }
        
        queryWrapper.orderByDesc(ActivityLog::getCreatedAt);
        
        return this.page(page, queryWrapper);
    }

    @Override
    public boolean logActivity(String userId, String username, String module, String action, String description,
                              String targetId, String targetType, String oldData, String newData,
                              String ipAddress, String userAgent, String status, String errorMessage,
                              Long duration, String remark) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setModule(module);
            log.setAction(action);
            log.setDescription(description);
            log.setTargetId(targetId);
            log.setTargetType(targetType);
            log.setOldData(oldData);
            log.setNewData(newData);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setStatus(status);
            log.setErrorMessage(errorMessage);
            log.setDuration(duration);
            log.setRemark(remark);
            log.setCreatedAt(LocalDateTime.now());
            
            return this.save(log);
        } catch (Exception e) {
            log.error("记录活动日志失败", e);
            return false;
        }
    }

    @Override
    public boolean logSuccess(String userId, String username, String module, String action, String description,
                             String targetId, String targetType, String oldData, String newData,
                             String ipAddress, String userAgent) {
        return logActivity(userId, username, module, action, description, targetId, targetType, 
                          oldData, newData, ipAddress, userAgent, "success", null, null, null);
    }

    @Override
    public boolean logError(String userId, String username, String module, String action, String description,
                           String targetId, String targetType, String errorMessage, String ipAddress, String userAgent) {
        return logActivity(userId, username, module, action, description, targetId, targetType, 
                          null, null, ipAddress, userAgent, "failed", errorMessage, null, null);
    }

    @Override
    public Long countByUserId(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityLog::getUserId, userId);
        
        if (startTime != null) {
            queryWrapper.ge(ActivityLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ActivityLog::getCreatedAt, endTime);
        }
        
        return (long) this.count(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> countByModule(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (startTime != null) {
            queryWrapper.ge(ActivityLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ActivityLog::getCreatedAt, endTime);
        }
        
        // 这里需要根据实际的数据库查询来实现
        // 暂时返回空列表，实际应该使用SQL查询来统计
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> countByAction(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (startTime != null) {
            queryWrapper.ge(ActivityLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ActivityLog::getCreatedAt, endTime);
        }
        
        // 这里需要根据实际的数据库查询来实现
        // 暂时返回空列表，实际应该使用SQL查询来统计
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getActivityStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();
        
        LambdaQueryWrapper<ActivityLog> queryWrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            queryWrapper.ge(ActivityLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ActivityLog::getCreatedAt, endTime);
        }
        
        // 总日志数
        long totalLogs = this.count(queryWrapper);
        statistics.put("totalLogs", totalLogs);
        
        // 成功日志数
        LambdaQueryWrapper<ActivityLog> successWrapper = queryWrapper.clone();
        successWrapper.eq(ActivityLog::getStatus, "success");
        long successLogs = this.count(successWrapper);
        statistics.put("successLogs", successLogs);
        
        // 失败日志数
        LambdaQueryWrapper<ActivityLog> failedWrapper = queryWrapper.clone();
        failedWrapper.eq(ActivityLog::getStatus, "failed");
        long failedLogs = this.count(failedWrapper);
        statistics.put("failedLogs", failedLogs);
        
        // 成功率
        double successRate = totalLogs > 0 ? (double) successLogs / totalLogs * 100 : 0;
        statistics.put("successRate", successRate);
        
        return statistics;
    }

    @Override
    public int cleanExpiredLogs(Integer days) {
        return cleanupExpiredLogs(days);
    }
}