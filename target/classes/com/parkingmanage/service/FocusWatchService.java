package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.FocusAlertRecord;
import com.parkingmanage.entity.FocusWatchList;
import com.parkingmanage.mapper.FocusAlertRecordMapper;
import com.parkingmanage.mapper.FocusWatchListMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注追踪服务
 * 
 * @author System
 */
@Slf4j
@Service
public class FocusWatchService {

    @Autowired
    private FocusWatchListMapper watchListMapper;

    @Autowired
    private FocusAlertRecordMapper alertRecordMapper;

    /**
     * 添加关注对象
     */
    public FocusWatchList addWatch(Integer userId, String watchType, String watchValue, String remark) {
        FocusWatchList watch = new FocusWatchList();
        watch.setUserId(userId);
        watch.setWatchType(watchType);
        watch.setWatchValue(watchValue);
        watch.setRemark(remark);
        watch.setCreatedAt(new Date());
        
        watchListMapper.insert(watch);
        log.info("✅ [关注追踪] 用户ID: {}, 添加关注: {} = {}", userId, watchType, watchValue);
        return watch;
    }

    /**
     * 删除关注对象
     */
    public boolean deleteWatch(Long id, Integer userId) {
        LambdaQueryWrapper<FocusWatchList> query = new LambdaQueryWrapper<>();
        query.eq(FocusWatchList::getId, id).eq(FocusWatchList::getUserId, userId);
        
        int deleted = watchListMapper.delete(query);
        if (deleted > 0) {
            log.info("✅ [关注追踪] 用户ID: {}, 删除关注ID: {}", userId, id);
            return true;
        }
        return false;
    }

    /**
     * 获取用户的关注列表
     */
    public Map<String, Object> getWatchList(Integer userId, String watchType, int page, int limit) {
        LambdaQueryWrapper<FocusWatchList> query = new LambdaQueryWrapper<>();
        query.eq(FocusWatchList::getUserId, userId);
        
        if (watchType != null && !watchType.isEmpty() && !"all".equals(watchType)) {
            query.eq(FocusWatchList::getWatchType, watchType);
        }
        
        query.orderByDesc(FocusWatchList::getCreatedAt);
        
        Page<FocusWatchList> pageObj = new Page<>(page, limit);
        IPage<FocusWatchList> result = watchListMapper.selectPage(pageObj, query);
        
        // 统计各类型数量
        LambdaQueryWrapper<FocusWatchList> countQuery = new LambdaQueryWrapper<>();
        countQuery.eq(FocusWatchList::getUserId, userId);
        
        LambdaQueryWrapper<FocusWatchList> idcardQuery = new LambdaQueryWrapper<>();
        idcardQuery.eq(FocusWatchList::getUserId, userId).eq(FocusWatchList::getWatchType, "idcard");
        
        LambdaQueryWrapper<FocusWatchList> plateQuery = new LambdaQueryWrapper<>();
        plateQuery.eq(FocusWatchList::getUserId, userId).eq(FocusWatchList::getWatchType, "plate");
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotal());
        response.put("list", result.getRecords());
        response.put("idcard_count", watchListMapper.selectCount(idcardQuery));
        response.put("plate_count", watchListMapper.selectCount(plateQuery));
        
        return response;
    }

    /**
     * 检查是否有用户关注该对象
     */
    public List<Integer> checkFocusMatch(String watchType, String watchValue) {
        return watchListMapper.findMatchedUsers(watchType, watchValue);
    }

    /**
     * 获取未确认提醒数量
     */
    public Map<String, Object> getPendingCount(Integer userId) {
        return alertRecordMapper.countPendingAlerts(userId);
    }

    /**
     * 获取未确认提醒列表
     */
    public Map<String, Object> getPendingAlerts(Integer userId, String alertType, int page, int limit) {
        LambdaQueryWrapper<FocusAlertRecord> query = new LambdaQueryWrapper<>();
        query.eq(FocusAlertRecord::getUserId, userId)
             .eq(FocusAlertRecord::getIsConfirmed, 0);
        
        if (alertType != null && !alertType.isEmpty() && !"all".equals(alertType)) {
            query.eq(FocusAlertRecord::getAlertType, alertType);
        }
        
        query.orderByDesc(FocusAlertRecord::getCreatedAt);
        
        Page<FocusAlertRecord> pageObj = new Page<>(page, limit);
        IPage<FocusAlertRecord> result = alertRecordMapper.selectPage(pageObj, query);
        
        Map<String, Object> counts = alertRecordMapper.countPendingAlerts(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", counts.get("total"));
        response.put("person_count", counts.get("person_count"));
        response.put("vehicle_count", counts.get("vehicle_count"));
        response.put("list", result.getRecords());
        
        return response;
    }

    /**
     * 获取历史记录列表
     */
    public Map<String, Object> getHistoryAlerts(Integer userId, String alertType, int page, int limit) {
        LambdaQueryWrapper<FocusAlertRecord> query = new LambdaQueryWrapper<>();
        query.eq(FocusAlertRecord::getUserId, userId)
             .eq(FocusAlertRecord::getIsConfirmed, 1);
        
        if (alertType != null && !alertType.isEmpty() && !"all".equals(alertType)) {
            query.eq(FocusAlertRecord::getAlertType, alertType);
        }
        
        query.orderByDesc(FocusAlertRecord::getConfirmedAt);
        
        Page<FocusAlertRecord> pageObj = new Page<>(page, limit);
        IPage<FocusAlertRecord> result = alertRecordMapper.selectPage(pageObj, query);
        
        Map<String, Object> counts = alertRecordMapper.countHistoryAlerts(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", counts.get("total"));
        response.put("person_count", counts.get("person_count"));
        response.put("vehicle_count", counts.get("vehicle_count"));
        response.put("list", result.getRecords());
        
        return response;
    }

    /**
     * 确认提醒
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmAlert(Long id, Integer userId) {
        FocusAlertRecord record = new FocusAlertRecord();
        record.setId(id);
        record.setIsConfirmed(1);
        record.setConfirmedAt(new Date());
        
        LambdaQueryWrapper<FocusAlertRecord> query = new LambdaQueryWrapper<>();
        query.eq(FocusAlertRecord::getId, id)
             .eq(FocusAlertRecord::getUserId, userId)
             .eq(FocusAlertRecord::getIsConfirmed, 0);
        
        int updated = alertRecordMapper.update(record, query);
        if (updated > 0) {
            log.info("✅ [关注追踪] 用户ID: {}, 确认提醒ID: {}", userId, id);
            return true;
        }
        return false;
    }

    /**
     * 批量确认提醒
     */
    @Transactional(rollbackFor = Exception.class)
    public int confirmBatchAlerts(List<Long> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        FocusAlertRecord record = new FocusAlertRecord();
        record.setIsConfirmed(1);
        record.setConfirmedAt(new Date());
        
        LambdaQueryWrapper<FocusAlertRecord> query = new LambdaQueryWrapper<>();
        query.in(FocusAlertRecord::getId, ids)
             .eq(FocusAlertRecord::getUserId, userId)
             .eq(FocusAlertRecord::getIsConfirmed, 0);
        
        int updated = alertRecordMapper.update(record, query);
        log.info("✅ [关注追踪] 用户ID: {}, 批量确认提醒数量: {}", userId, updated);
        return updated;
    }

    /**
     * 保存关注提醒记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAlertRecord(FocusAlertRecord record) {
        alertRecordMapper.insert(record);
        log.info("✅ [关注追踪] 保存提醒记录: userId={}, alertType={}, watchValue={}, eventType={}", 
            record.getUserId(), record.getAlertType(), record.getWatchValue(), record.getEventType());
    }

    /**
     * 根据关注对象查询进出场记录
     */
    public Map<String, Object> getRecordsByWatch(Integer userId, String watchType, String watchValue, int page, int limit) {
        Map<String, Object> result = new HashMap<>();
        
        // 构造查询条件
        LambdaQueryWrapper<FocusAlertRecord> query = new LambdaQueryWrapper<>();
        query.eq(FocusAlertRecord::getUserId, userId);
        
        // 根据类型设置查询条件
        if ("idcard".equals(watchType)) {
            query.eq(FocusAlertRecord::getAlertType, "person")
                 .eq(FocusAlertRecord::getWatchValue, watchValue);
        } else if ("plate".equals(watchType)) {
            query.eq(FocusAlertRecord::getAlertType, "vehicle")
                 .eq(FocusAlertRecord::getWatchValue, watchValue);
        }
        
        // 按时间倒序
        query.orderByDesc(FocusAlertRecord::getEventTime);
        
        // 分页查询
        Page<FocusAlertRecord> pageQuery = new Page<>(page, limit);
        IPage<FocusAlertRecord> pageResult = alertRecordMapper.selectPage(pageQuery, query);
        
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("limit", limit);
        
        log.info("✅ [关注追踪] 查询进出场记录: userId={}, watchType={}, watchValue={}, 结果数={}", 
            userId, watchType, watchValue, pageResult.getRecords().size());
        
        return result;
    }
}
