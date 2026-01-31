package com.parkingmanage.controller;

import com.parkingmanage.entity.FocusWatchList;
import com.parkingmanage.service.FocusWatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注追踪控制器
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/focus")
@Api(tags = "关注追踪管理")
public class FocusWatchController {

    @Autowired
    private FocusWatchService focusWatchService;

    /**
     * 添加关注对象
     */
    @PostMapping("/watch/add")
    @ApiOperation("添加关注对象")
    public Map<String, Object> addWatch(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID，这里暂时使用固定值
            Integer userId = 1;
            
            String watchType = (String) params.get("watch_type");
            String watchValue = (String) params.get("watch_value");
            String remark = (String) params.get("remark");
            
            // 参数验证
            if (watchType == null || watchValue == null || watchValue.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "参数错误：关注类型和关注值不能为空");
                return result;
            }
            
            if (!"idcard".equals(watchType) && !"plate".equals(watchType)) {
                result.put("code", 400);
                result.put("message", "参数错误：关注类型必须是idcard或plate");
                return result;
            }
            
            FocusWatchList watch = focusWatchService.addWatch(userId, watchType, watchValue, remark);
            
            result.put("code", 200);
            result.put("message", "添加成功");
            result.put("data", watch);
            
        } catch (DuplicateKeyException e) {
            log.warn("⚠️ [关注追踪] 重复添加关注对象", e);
            result.put("code", 409);
            result.put("message", "该对象已在关注列表中");
        } catch (Exception e) {
            // 检查是否是数据库唯一键约束冲突（可能被包装在其他异常中）
            Throwable cause = e.getCause();
            if (cause != null && (cause instanceof java.sql.SQLIntegrityConstraintViolationException 
                || cause.getMessage().contains("Duplicate entry"))) {
                log.warn("⚠️ [关注追踪] 重复添加关注对象（从Exception链中检测）", e);
                result.put("code", 409);
                result.put("message", "该对象已在关注列表中");
            } else {
                log.error("❌ [关注追踪] 添加关注失败", e);
                result.put("code", 500);
                result.put("message", "添加失败：" + e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/watch/list")
    @ApiOperation("获取关注列表")
    public Map<String, Object> getWatchList(
            @RequestParam(required = false) String watch_type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            Map<String, Object> data = focusWatchService.getWatchList(userId, watch_type, page, limit);
            
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 获取关注列表失败", e);
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 删除关注对象
     */
    @DeleteMapping("/watch/{id}")
    @ApiOperation("删除关注对象")
    public Map<String, Object> deleteWatch(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            boolean deleted = focusWatchService.deleteWatch(id, userId);
            
            if (deleted) {
                result.put("code", 200);
                result.put("message", "删除成功");
            } else {
                result.put("code", 404);
                result.put("message", "关注对象不存在或无权删除");
            }
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 删除关注失败", e);
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取未确认提醒数量
     */
    @GetMapping("/alerts/pending-count")
    @ApiOperation("获取未确认提醒数量")
    public Map<String, Object> getPendingCount() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            Map<String, Object> counts = focusWatchService.getPendingCount(userId);
            
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", counts);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 获取未确认数量失败", e);
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取未确认提醒列表
     */
    @GetMapping("/alerts/pending")
    @ApiOperation("获取未确认提醒列表")
    public Map<String, Object> getPendingAlerts(
            @RequestParam(required = false) String alert_type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            Map<String, Object> data = focusWatchService.getPendingAlerts(userId, alert_type, page, limit);
            
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 获取未确认提醒失败", e);
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取历史记录列表
     */
    @GetMapping("/alerts/history")
    @ApiOperation("获取历史记录列表")
    public Map<String, Object> getHistoryAlerts(
            @RequestParam(required = false) String alert_type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            Map<String, Object> data = focusWatchService.getHistoryAlerts(userId, alert_type, page, limit);
            
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 获取历史记录失败", e);
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 确认提醒
     */
    @PostMapping("/alerts/confirm/{id}")
    @ApiOperation("确认提醒")
    public Map<String, Object> confirmAlert(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            boolean confirmed = focusWatchService.confirmAlert(id, userId);
            
            if (confirmed) {
                result.put("code", 200);
                result.put("message", "确认成功");
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", id);
                data.put("confirmed_at", new java.util.Date());
                result.put("data", data);
            } else {
                result.put("code", 404);
                result.put("message", "提醒记录不存在或已确认");
            }
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 确认提醒失败", e);
            result.put("code", 500);
            result.put("message", "确认失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 批量确认提醒
     */
    @PostMapping("/alerts/confirm-batch")
    @ApiOperation("批量确认提醒")
    public Map<String, Object> confirmBatchAlerts(@RequestBody Map<String, List<Long>> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            List<Long> ids = params.get("ids");
            
            if (ids == null || ids.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请提供要确认的记录ID");
                return result;
            }
            
            int count = focusWatchService.confirmBatchAlerts(ids, userId);
            
            result.put("code", 200);
            result.put("message", "批量确认成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("confirmed_count", count);
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 批量确认提醒失败", e);
            result.put("code", 500);
            result.put("message", "批量确认失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 根据关注对象查询进出场记录
     */
    @GetMapping("/alerts/records")
    @ApiOperation("查询关注对象的进出场记录")
    public Map<String, Object> getRecordsByWatch(
            @RequestParam("watch_type") String watchType,
            @RequestParam("watch_value") String watchValue,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // TODO: 从Session或Token获取当前登录用户ID
            Integer userId = 1;
            
            // 参数验证
            if (watchType == null || watchValue == null || watchValue.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "参数错误：关注类型和关注值不能为空");
                return result;
            }
            
            if (!"idcard".equals(watchType) && !"plate".equals(watchType)) {
                result.put("code", 400);
                result.put("message", "参数错误：关注类型必须是idcard或plate");
                return result;
            }
            
            Map<String, Object> data = focusWatchService.getRecordsByWatch(userId, watchType, watchValue, page, limit);
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", data);
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 查询进出场记录失败", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        
        return result;
    }
}
