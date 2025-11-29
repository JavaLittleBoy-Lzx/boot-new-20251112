package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.VisitorReservationSync;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 访客预约同步Mapper接口
 * 
 * @author System
 */
public interface VisitorReservationSyncMapper extends BaseMapper<VisitorReservationSync> {
    
    /**
     * 批量插入或更新
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertOrUpdate(@Param("list") List<VisitorReservationSync> list);
    
    /**
     * 根据预约ID列表查询（用于快速加载到HashMap）
     * @param reservationIds 预约ID列表
     * @return 数据列表
     */
    List<VisitorReservationSync> selectByReservationIds(@Param("reservationIds") List<String> reservationIds);
    
    /**
     * 获取所有预约ID和数据指纹的映射（用于快速对比）
     * @return Map<预约ID, 数据指纹>
     */
    List<Map<String, Object>> selectAllIdAndHash();
    
    /**
     * 批量删除（逻辑删除）
     * @param reservationIds 预约ID列表
     * @return 影响行数
     */
    int batchDeleteByReservationIds(@Param("reservationIds") List<String> reservationIds);
    
    /**
     * 根据访客姓名查询车牌号
     * @param visitorName 访客姓名
     * @return 车牌号（如果有多个，返回第一个）
     */
    String selectCarNumberByVisitorName(@Param("visitorName") String visitorName);
    
    /**
     * 根据访客姓名和时间范围查询访客预约记录
     * 查询条件：姓名匹配，且指定时间在gateway_transit_begin_time和gateway_transit_end_time范围内
     * @param visitorName 访客姓名
     * @param eventTime 事件时间
     * @return 访客预约记录（如果有多个，返回第一个）
     */
    VisitorReservationSync selectByVisitorNameAndTimeRange(@Param("visitorName") String visitorName, @Param("eventTime") java.util.Date eventTime);
}

