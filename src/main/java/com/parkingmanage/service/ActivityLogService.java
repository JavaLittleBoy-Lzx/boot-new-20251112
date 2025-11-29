package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ActivityLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 活动日志服务接口
 */
public interface ActivityLogService extends IService<ActivityLog> {

    /**
     * 获取用户操作记录
     */
    List<ActivityLog> getUserOperationLogs(Integer userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户操作记录总数
     */
    long getUserOperationCount(Integer userId);

    /**
     * 记录用户操作日志
     */
    void recordUserOperation(Integer userId, String module, String action, String description, String ipAddress, String userAgent);

    /**
     * 记录系统操作日志
     */
    void recordSystemOperation(String module, String action, String description, String ipAddress, String userAgent);

    /**
     * 获取所有日志
     */
    List<ActivityLog> getAllLogs(Integer pageNum, Integer pageSize);

    /**
     * 根据模块获取日志
     */
    List<ActivityLog> getLogsByModule(String module, Integer pageNum, Integer pageSize);

    /**
     * 获取日志统计信息
     */
    Map<String, Object> getLogStatistics();

    /**
     * 清理过期日志
     */
    int cleanupExpiredLogs(Integer days);

    /**
     * 分页查询活动日志
     */
    IPage<ActivityLog> getActivityLogPage(Page<ActivityLog> page, String userId, String module, 
                                        String action, String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 记录活动日志
     */
    boolean logActivity(String userId, String username, String module, String action, String description,
                       String targetId, String targetType, String oldData, String newData,
                       String ipAddress, String userAgent, String status, String errorMessage,
                       Long duration, String remark);

    /**
     * 记录成功操作日志
     */
    boolean logSuccess(String userId, String username, String module, String action, String description,
                      String targetId, String targetType, String oldData, String newData,
                      String ipAddress, String userAgent);

    /**
     * 记录失败操作日志
     */
    boolean logError(String userId, String username, String module, String action, String description,
                    String targetId, String targetType, String errorMessage, String ipAddress, String userAgent);

    /**
     * 统计用户操作次数
     */
    Long countByUserId(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计各模块操作次数
     */
    List<Map<String, Object>> countByModule(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计各操作类型次数
     */
    List<Map<String, Object>> countByAction(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取活动统计数据
     */
    Map<String, Object> getActivityStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理过期日志
     */
    int cleanExpiredLogs(Integer days);
}