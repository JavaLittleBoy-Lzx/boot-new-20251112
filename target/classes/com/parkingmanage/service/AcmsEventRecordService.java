package com.parkingmanage.service;

import com.parkingmanage.entity.AcmsEventRecord;

/**
 * ACMS数据推送事件记录服务接口
 * 
 * @author System
 */
public interface AcmsEventRecordService {
    
    /**
     * 保存事件记录
     * @param record 事件记录
     * @return 是否保存成功
     */
    boolean saveEventRecord(AcmsEventRecord record);
    
    /**
     * 根据事件ID查询记录
     * @param eventId 事件ID
     * @return 事件记录
     */
    AcmsEventRecord getByEventId(String eventId);
    
    /**
     * 更新访客预约状态
     * 对于刷身份证进出的记录，通过海康接口查询姓名，然后匹配预约记录
     * @param record 事件记录
     * @return 是否更新成功
     */
    boolean updateVisitorReservationStatus(AcmsEventRecord record);
    
    /**
     * 批量更新访客预约状态
     * @param records 事件记录列表
     * @return 更新成功的数量
     */
    int batchUpdateVisitorReservationStatus(java.util.List<AcmsEventRecord> records);
    
    /**
     * 检查时间范围内是否存在同一人的记录
     * 用于过滤重复数据（1分钟内同一人的重复事件）
     * 
     * @param personName 人员姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true-存在重复记录，false-不存在
     */
    boolean checkDuplicateInTimeRange(String personName, java.util.Date startTime, java.util.Date endTime);
}

