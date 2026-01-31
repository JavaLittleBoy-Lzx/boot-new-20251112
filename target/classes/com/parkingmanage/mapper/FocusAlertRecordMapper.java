package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.FocusAlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 关注提醒记录Mapper
 * 
 * @author System
 */
@Mapper
public interface FocusAlertRecordMapper extends BaseMapper<FocusAlertRecord> {

    /**
     * 统计用户的未确认提醒数量
     * 
     * @param userId 用户ID
     * @return 统计结果
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN alert_type = 'person' THEN 1 ELSE 0 END) as person_count, " +
            "SUM(CASE WHEN alert_type = 'vehicle' THEN 1 ELSE 0 END) as vehicle_count " +
            "FROM focus_alert_records WHERE user_id = #{userId} AND is_confirmed = 0")
    Map<String, Object> countPendingAlerts(@Param("userId") Integer userId);

    /**
     * 统计用户的历史记录数量
     * 
     * @param userId 用户ID
     * @return 统计结果
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN alert_type = 'person' THEN 1 ELSE 0 END) as person_count, " +
            "SUM(CASE WHEN alert_type = 'vehicle' THEN 1 ELSE 0 END) as vehicle_count " +
            "FROM focus_alert_records WHERE user_id = #{userId} AND is_confirmed = 1")
    Map<String, Object> countHistoryAlerts(@Param("userId") Integer userId);
}
