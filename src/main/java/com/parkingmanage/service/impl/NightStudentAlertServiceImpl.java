package com.parkingmanage.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.NightStudentAlertConfig;
import com.parkingmanage.entity.NightStudentAlertRecord;
import com.parkingmanage.mapper.NightStudentAlertConfigMapper;
import com.parkingmanage.mapper.NightStudentAlertRecordMapper;
import com.parkingmanage.service.HikvisionChannelService;
import com.parkingmanage.service.NightStudentAlertService;
import com.parkingmanage.websocket.VehicleWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夜间学生出校提醒服务实现
 *
 * @author System
 */
@Slf4j
@Service
public class NightStudentAlertServiceImpl implements NightStudentAlertService {

    private static final int CONFIG_ID = 1; // 配置单例ID

    @Resource
    private NightStudentAlertConfigMapper configMapper;

    @Resource
    private NightStudentAlertRecordMapper recordMapper;

    @Autowired(required = false)
    private VehicleWebSocketHandler vehicleWebSocketHandler;

    @Resource
    private HikvisionChannelService hikvisionChannelService;

    // ==================== 配置相关 ====================

    @Override
    public NightStudentAlertConfig getConfig() {
        NightStudentAlertConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null) {
            // 如果配置不存在，创建默认配置
            config = new NightStudentAlertConfig();
            config.setId(CONFIG_ID);
            config.setEnabled(1);
            config.setNightStartTime("22:00");
            config.setNightEndTime("06:00");
            configMapper.insert(config);
        }
        return config;
    }

    @Override
    public boolean updateConfig(NightStudentAlertConfig config) {
        config.setId(CONFIG_ID);
        return configMapper.updateById(config) > 0;
    }

    // ==================== 提醒记录相关 ====================

    @Override
    public boolean createAlertAndPush(NightStudentAlertRecord record) {
        try {
            // 1. 保存记录
            record.setIsRead(0);
            record.setCreatedAt(new Date());
            int result = recordMapper.insert(record);

            if (result > 0) {
                // 2. 推送WebSocket
                pushAlertToWebSocket(record);
                log.info("✅ [夜间学生出校提醒] 创建成功 - 姓名: {}, 通道: {}, 时间: {}",
                        record.getPersonName(), record.getChannelName(), record.getEventTime());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("❌ [夜间学生出校提醒] 创建失败 - {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int getUnreadCount() {
        return recordMapper.countUnread();
    }

    @Override
    public IPage<NightStudentAlertRecord> getRecords(int pageNum, int pageSize, String channelName,
                                                      String gender, String college, Date startTime, Date endTime) {
        Page<NightStudentAlertRecord> page = new Page<>(pageNum, pageSize);
        return recordMapper.selectByPage(page, channelName, gender, college, startTime, endTime);
    }

    @Override
    public boolean markAsRead(int id) {
        NightStudentAlertRecord record = new NightStudentAlertRecord();
        record.setId(id);
        record.setIsRead(1);
        record.setReadAt(new Date());
        return recordMapper.updateById(record) > 0;
    }

    @Override
    public int markAllAsRead() {
        NightStudentAlertRecord record = new NightStudentAlertRecord();
        record.setIsRead(1);
        record.setReadAt(new Date());

        QueryWrapper<NightStudentAlertRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_read", 0);

        return recordMapper.update(record, wrapper);
    }

    // ==================== 统计相关 ====================

    @Override
    public Map<String, Object> getStatisticsByChannel(Date startTime, Date endTime) {
        List<Map<String, Object>> data = recordMapper.selectGroupByChannel(startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("channelStats", data);
        return result;
    }

    @Override
    public Map<String, Object> getStatisticsByGender(Date startTime, Date endTime) {
        List<Map<String, Object>> data = recordMapper.selectGroupByGender(startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("genderStats", data);
        return result;
    }

    @Override
    public Map<String, Object> getStatisticsByCollege(Date startTime, Date endTime) {
        List<Map<String, Object>> data = recordMapper.selectGroupByCollege(startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("collegeStats", data);
        return result;
    }

    @Override
    public Map<String, Object> getStatistics(Date startTime, Date endTime) {
        Map<String, Object> result = new HashMap<>();

        // 按通道统计
        List<Map<String, Object>> channelData = recordMapper.selectGroupByChannel(startTime, endTime);
        result.put("channelStats", channelData);

        // 按性别统计
        List<Map<String, Object>> genderData = recordMapper.selectGroupByGender(startTime, endTime);
        result.put("genderStats", genderData);

        // 按学院统计
        List<Map<String, Object>> collegeData = recordMapper.selectGroupByCollege(startTime, endTime);
        result.put("collegeStats", collegeData);

        // 按小时统计（时段分布）
        List<Map<String, Object>> hourlyData = recordMapper.selectGroupByHour(startTime, endTime);
        result.put("hourlyStats", hourlyData);

        // 按日期统计（日出校趋势）
        List<Map<String, Object>> dailyData = recordMapper.selectGroupByDay(startTime, endTime);
        result.put("dailyTrendStats", dailyData);

        // 总记录数
        long totalCount = 0;
        if (channelData != null) {
            for (Map<String, Object> item : channelData) {
                Object count = item.get("count");
                if (count instanceof Number) {
                    totalCount += ((Number) count).longValue();
                }
            }
        }
        result.put("totalCount", totalCount);

        return result;
    }

    // ==================== WebSocket推送 ====================

    /**
     * 推送夜间学生出校提醒到WebSocket
     */
    private void pushAlertToWebSocket(NightStudentAlertRecord record) {
        try {
            if (vehicleWebSocketHandler == null) {
                log.warn("⚠️ [夜间学生出校提醒] WebSocket处理器未注入，跳过推送");
                return;
            }

            JSONObject message = new JSONObject(true);
            message.put("type", "nightStudentAlert");
            message.put("alertType", "night_student_exit");
            message.put("id", record.getId());
            message.put("personName", record.getPersonName());
            message.put("gender", record.getGender());
            message.put("college", record.getCollege());
            message.put("channelName", record.getChannelName());
            message.put("eventTime", record.getEventTime());  // 直接用字符串，不转换
            message.put("photoUrl", record.getPhotoUrl());
            message.put("timestamp", System.currentTimeMillis());

            vehicleWebSocketHandler.broadcastMessage(message);
            log.info("📢 [夜间学生出校提醒] WebSocket推送成功 - 姓名: {}", record.getPersonName());
        } catch (Exception e) {
            log.error("❌ [夜间学生出校提醒] WebSocket推送失败 - {}", e.getMessage(), e);
        }
    }

    // ==================== 夜间检测工具方法 ====================

    /**
     * 判断是否在夜间时间段（支持跨天）
     *
     * @param eventTime 事件时间
     * @param nightStart 夜间开始时间（如22:00）
     * @param nightEnd 夜间结束时间（如06:00）
     * @return 是否在夜间时间段
     */
    public static boolean isInNightPeriod(LocalTime eventTime, LocalTime nightStart, LocalTime nightEnd) {
        if (nightStart.isAfter(nightEnd)) {
            // 跨天情况：如22:00-06:00
            return eventTime.isAfter(nightStart) || eventTime.isBefore(nightEnd);
        } else {
            // 同一天：如08:00-18:00
            return eventTime.isAfter(nightStart) && eventTime.isBefore(nightEnd);
        }
    }

    /**
     * 判断是否是学生
     *
     * @param organization 所属单位
     * @param jobNo 工号/学号
     * @return 是否是学生
     */
    public static boolean isStudent(String organization, String jobNo) {
        // organization包含"学生" 或 jobNo长度>8 则为学生
        if (StringUtils.hasText(organization) && organization.contains("学生")) {
            return true;
        }
        if (StringUtils.hasText(jobNo) && jobNo.length() > 8) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否是指定通道
     *
     * @param channelName 通道名称
     * @param alertChannels 配置的通道列表（逗号分隔）
     * @return 是否是指定通道
     */
    public static boolean isTargetChannel(String channelName, String alertChannels) {
        if (!StringUtils.hasText(alertChannels)) {
            // 空表示全部通道
            return true;
        }
        if (!StringUtils.hasText(channelName)) {
            return false;
        }
        String[] channels = alertChannels.split(",");
        for (String channel : channels) {
            if (channelName.contains(channel.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否应该触发夜间学生出校提醒
     *
     * @param organization 所属单位
     * @param jobNo 工号/学号
     * @param direction 进出方向
     * @param eventTime 事件时间
     * @param channelName 通道名称
     * @return 是否应该触发提醒
     */
    public boolean shouldTriggerAlert(String organization, String jobNo, String direction,
                                       java.util.Date eventTime, String channelName) {
        try {
            // 1. 获取配置
            NightStudentAlertConfig config = getConfig();
            if (config.getEnabled() == null || config.getEnabled() != 1) {
                return false;
            }

            // 2. 判断是否是学生出校
            if (!"出".equals(direction)) {
                return false;
            }
            if (!isStudent(organization, jobNo)) {
                return false;
            }

            // 3. 判断是否在夜间时间段
            LocalTime eventLocalTime = eventTime.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalTime();
            LocalTime nightStart = LocalTime.parse(config.getNightStartTime());
            LocalTime nightEnd = LocalTime.parse(config.getNightEndTime());

            if (!isInNightPeriod(eventLocalTime, nightStart, nightEnd)) {
                return false;
            }

            // 4. 判断是否是指定通道
            if (!isTargetChannel(channelName, config.getAlertChannels())) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("❌ [夜间学生出校提醒] 检查是否触发时出错 - {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> getAllChannelNames() {
        // 直接从数据库获取通道列表
        try {
            List<String> channelList = recordMapper.selectAllChannelNames();
            log.info("✅ [夜间学生出校提醒] 从数据库获取通道列表成功 - {} 个通道", channelList != null ? channelList.size() : 0);
            return channelList != null ? channelList : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("❌ [夜间学生出校提醒] 获取通道列表失败 - {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public List<String> getAllColleges() {
        // 直接从数据库获取学院列表
        try {
            List<String> collegeList = recordMapper.selectAllColleges();
            log.info("✅ [夜间学生出校提醒] 获取学院列表成功 - {} 个学院", collegeList != null ? collegeList.size() : 0);
            return collegeList != null ? collegeList : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("❌ [夜间学生出校提醒] 获取学院列表失败 - {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
}
