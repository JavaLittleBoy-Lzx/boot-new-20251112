package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.AcmsEventRecord;
import org.apache.ibatis.annotations.Param;

/**
 * ACMS数据推送事件记录Mapper接口
 * 
 * @author System
 */
public interface AcmsEventRecordMapper extends BaseMapper<AcmsEventRecord> {
    
    /**
     * 根据事件ID查询记录
     * @param eventId 事件ID
     * @return 事件记录
     */
    AcmsEventRecord selectByEventId(@Param("eventId") String eventId);
}

