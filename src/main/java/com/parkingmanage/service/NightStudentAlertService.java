package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.entity.NightStudentAlertConfig;
import com.parkingmanage.entity.NightStudentAlertRecord;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 夜间学生出校提醒服务接口
 *
 * @author System
 */
public interface NightStudentAlertService {

    // ==================== 配置相关 ====================

    /**
     * 获取配置（单例，ID=1）
     *
     * @return 配置对象
     */
    NightStudentAlertConfig getConfig();

    /**
     * 更新配置
     *
     * @param config 配置对象
     * @return 是否成功
     */
    boolean updateConfig(NightStudentAlertConfig config);

    // ==================== 提醒记录相关 ====================

    /**
     * 创建夜间学生出校提醒记录并推送WebSocket
     *
     * @param record 记录对象
     * @return 是否成功
     */
    boolean createAlertAndPush(NightStudentAlertRecord record);

    /**
     * 获取未读数量
     *
     * @return 未读数量
     */
    int getUnreadCount();

    /**
     * 分页查询记录
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param channelName 通道名称（筛选）
     * @param gender 性别（筛选）
     * @param college 学院（筛选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    IPage<NightStudentAlertRecord> getRecords(int pageNum, int pageSize, String channelName,
                                              String gender, String college, Date startTime, Date endTime);

    /**
     * 标记单条记录为已读
     *
     * @param id 记录ID
     * @return 是否成功
     */
    boolean markAsRead(int id);

    /**
     * 标记全部记录为已读
     *
     * @return 成功数量
     */
    int markAllAsRead();

    // ==================== 统计相关 ====================

    /**
     * 按通道统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 通道统计数据
     */
    Map<String, Object> getStatisticsByChannel(Date startTime, Date endTime);

    /**
     * 按性别统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性别统计数据
     */
    Map<String, Object> getStatisticsByGender(Date startTime, Date endTime);

    /**
     * 按学院统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学院统计数据
     */
    Map<String, Object> getStatisticsByCollege(Date startTime, Date endTime);

    /**
     * 综合统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 综合统计数据
     */
    Map<String, Object> getStatistics(Date startTime, Date endTime);

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
    boolean shouldTriggerAlert(String organization, String jobNo, String direction,
                               Date eventTime, String channelName);

    /**
     * 获取所有可用的通道名称列表
     *
     * @return 通道名称列表
     */
    List<String> getAllChannelNames();

    /**
     * 获取所有学院名称列表
     *
     * @return 学院名称列表
     */
    List<String> getAllColleges();
}
