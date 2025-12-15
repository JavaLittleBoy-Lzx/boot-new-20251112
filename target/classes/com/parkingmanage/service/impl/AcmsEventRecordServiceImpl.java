package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.AcmsEventRecord;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.mapper.AcmsEventRecordMapper;
import com.parkingmanage.mapper.VisitorReservationSyncMapper;
import com.parkingmanage.service.AcmsEventRecordService;
import com.parkingmanage.service.HikvisionPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * ACMS数据推送事件记录服务实现类
 * 
 * @author System
 */
@Slf4j
@Service
public class AcmsEventRecordServiceImpl extends ServiceImpl<AcmsEventRecordMapper, AcmsEventRecord> implements AcmsEventRecordService {

    @Autowired
    private HikvisionPersonService hikvisionPersonService;
    
    @Autowired
    private VisitorReservationSyncMapper visitorReservationSyncMapper;
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEventRecord(AcmsEventRecord record) {
        try {
            // 检查是否已存在相同的事件ID
            if (record.getEventId() != null && !record.getEventId().isEmpty()) {
                QueryWrapper<AcmsEventRecord> query = new QueryWrapper<>();
                query.eq("event_id", record.getEventId())
                     .eq("deleted", 0);
                AcmsEventRecord existing = this.getOne(query);
                
                if (existing != null) {
                    log.warn("⚠️ [ACMS事件记录] 事件ID已存在，跳过保存 - eventId: {}", record.getEventId());
                    return false;
                }
            }
            
            // 保存记录
            boolean result = this.save(record);
            if (result) {
                log.info("✅ [ACMS事件记录] 保存成功 - eventId: {}, eventType: {}, personName: {}", 
                    record.getEventId(), record.getEventType(), record.getPersonName());
                
                // 如果是刷身份证进出的记录，自动更新访客预约状态
                if (record.getEventType() != null && record.getEventType() == 197162) {
                    try {
                        // 确保记录有ID才能更新（MyBatis-Plus保存后会自动填充ID）
                        // 如果ID仍然为空，通过eventId重新查询
                        if (record.getId() == null && record.getEventId() != null) {
                            AcmsEventRecord savedRecord = this.getByEventId(record.getEventId());
                            if (savedRecord != null && savedRecord.getId() != null) {
                                record.setId(savedRecord.getId());
                                log.debug("🔍 [ACMS事件记录] 通过eventId获取记录ID: {}", record.getId());
                            }
                        }
                        
                        // 如果记录有ID，更新访客预约状态（包括进场和离场）
                        if (record.getId() != null) {
                            this.updateVisitorReservationStatus(record);
                        } else {
                            log.warn("⚠️ [ACMS事件记录] 记录ID为空，无法更新访客预约状态 - eventId: {}", 
                                record.getEventId());
                        }
                    } catch (Exception e) {
                        // 更新预约状态失败不影响保存结果，只记录日志
                        log.error("❌ [ACMS事件记录] 保存后更新访客预约状态异常 - eventId: {}, error: {}", 
                            record.getEventId(), e.getMessage(), e);
                    }
                }
            } else {
                log.warn("⚠️ [ACMS事件记录] 保存失败 - eventId: {}", record.getEventId());
            }
            return result;
            
        } catch (Exception e) {
            log.error("❌ [ACMS事件记录] 保存异常 - eventId: {}", record.getEventId(), e);
            return false;
        }
    }

    @Override
    public AcmsEventRecord getByEventId(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return null;
        }
        
        QueryWrapper<AcmsEventRecord> query = new QueryWrapper<>();
        query.eq("event_id", eventId)
             .eq("deleted", 0);
        return this.getOne(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVisitorReservationStatus(AcmsEventRecord record) {
        // 仅处理刷身份证进出的记录（event_type = 197162）
        if (record == null || record.getEventType() == null || record.getEventType() != 197162) {
            log.debug("⏭️ [访客预约状态] 跳过处理 - eventType: {}", 
                record != null ? record.getEventType() : null);
            return false;
        }
        
        try {
            String personId = record.getPersonId();
            String personName = record.getPersonName();
            Date eventTime = record.getEventTime();
            
            // 如果姓名为空，尝试通过海康接口查询
            if ((personName == null || personName.trim().isEmpty()) && personId != null && !personId.trim().isEmpty()) {
                log.info("🔍 [访客预约状态] 姓名为空，通过海康接口查询 - personId: {}", personId);
                try {
                    HikvisionPersonService.PersonInfo personInfo = hikvisionPersonService.queryPersonInfo(personId);
                    if (personInfo != null && personInfo.getPersonName() != null && !personInfo.getPersonName().trim().isEmpty()) {
                        personName = personInfo.getPersonName().trim();
                        // 同时更新记录中的姓名
                        record.setPersonName(personName);
                        log.info("✅ [访客预约状态] 通过海康接口获取姓名成功 - personId: {}, personName: {}", personId, personName);
                    } else {
                        log.warn("⚠️ [访客预约状态] 通过海康接口未查询到姓名 - personId: {}", personId);
                    }
                } catch (Exception e) {
                    log.error("❌ [访客预约状态] 调用海康接口查询姓名异常 - personId: {}", personId, e);
                }
            }
            
            // 如果姓名仍为空，无法继续处理
            if (personName == null || personName.trim().isEmpty()) {
                log.warn("⚠️ [访客预约状态] 姓名为空，无法匹配预约记录 - eventId: {}, personId: {}", 
                    record.getEventId(), personId);
                // 标记为未预约的纯访客
                record.setIsUnreservedVisitor(1);
                record.setVipTypeName(null);
                record.setReservationTimeRange(null);
                
                // 确保记录有ID才能更新
                if (record.getId() == null && record.getEventId() != null) {
                    AcmsEventRecord existingRecord = this.getByEventId(record.getEventId());
                    if (existingRecord != null && existingRecord.getId() != null) {
                        record.setId(existingRecord.getId());
                    }
                }
                
                if (record.getId() == null) {
                    log.error("❌ [访客预约状态] 记录ID为空，无法更新 - eventId: {}", record.getEventId());
                    return false;
                }
                
                boolean result = this.updateById(record);
                if (result) {
                    log.info("✅ [访客预约状态] 姓名为空，标记为纯访客成功 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                } else {
                    log.error("❌ [访客预约状态] 姓名为空，标记为纯访客失败 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                }
                return result;
            }
            
            // 如果事件时间为空，无法继续处理
            if (eventTime == null) {
                log.warn("⚠️ [访客预约状态] 事件时间为空，无法匹配预约记录 - eventId: {}, personName: {}", 
                    record.getEventId(), personName);
                // 标记为未预约的纯访客
                record.setIsUnreservedVisitor(1);
                record.setVipTypeName(null);
                record.setReservationTimeRange(null);
                
                // 确保记录有ID才能更新
                if (record.getId() == null && record.getEventId() != null) {
                    AcmsEventRecord existingRecord = this.getByEventId(record.getEventId());
                    if (existingRecord != null && existingRecord.getId() != null) {
                        record.setId(existingRecord.getId());
                    }
                }
                
                if (record.getId() == null) {
                    log.error("❌ [访客预约状态] 记录ID为空，无法更新 - eventId: {}", record.getEventId());
                    return false;
                }
                
                boolean result = this.updateById(record);
                if (result) {
                    log.info("✅ [访客预约状态] 事件时间为空，标记为纯访客成功 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                } else {
                    log.error("❌ [访客预约状态] 事件时间为空，标记为纯访客失败 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                }
                return result;
            }
            
            // 通过姓名和时间查询预约记录
            VisitorReservationSync reservation = visitorReservationSyncMapper.selectByVisitorNameAndTimeRange(personName, eventTime);
            
            if (reservation != null) {
                // 找到匹配的预约记录
                log.info("✅ [访客预约状态] 找到匹配的预约记录 - eventId: {}, personName: {}, reservationId: {}", 
                    record.getEventId(), personName, reservation.getReservationId());
                
                // 获取预约时间段（优先使用网关通行时间，否则使用预约时间）
                Date startTime = reservation.getGatewayTransitBeginTime() != null 
                    ? reservation.getGatewayTransitBeginTime() 
                    : reservation.getStartTime();
                Date endTime = reservation.getGatewayTransitEndTime() != null 
                    ? reservation.getGatewayTransitEndTime() 
                    : reservation.getEndTime();
                
                // 构建预约时间段字符串
                String timeRange = null;
                if (startTime != null && endTime != null) {
                    timeRange = dateFormat.format(startTime) + "-" + dateFormat.format(endTime);
                }
                
                // 更新记录
                record.setIsUnreservedVisitor(0);
                record.setVipTypeName(reservation.getVipTypeName());
                record.setReservationTimeRange(timeRange);
                
                // 确保记录有ID才能更新
                if (record.getId() == null && record.getEventId() != null) {
                    AcmsEventRecord existingRecord = this.getByEventId(record.getEventId());
                    if (existingRecord != null && existingRecord.getId() != null) {
                        record.setId(existingRecord.getId());
                    }
                }
                
                if (record.getId() == null) {
                    log.error("❌ [访客预约状态] 记录ID为空，无法更新 - eventId: {}", record.getEventId());
                    return false;
                }
                
                boolean result = this.updateById(record);
                if (result) {
                    log.info("✅ [访客预约状态] 更新成功 - eventId: {}, id: {}, vipTypeName: {}, timeRange: {}", 
                        record.getEventId(), record.getId(), reservation.getVipTypeName(), timeRange);
                } else {
                    log.error("❌ [访客预约状态] 更新失败 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                }
                return result;
            } else {
                // 未找到匹配的预约记录，标记为未预约的纯访客
                log.info("ℹ️ [访客预约状态] 未找到匹配的预约记录，标记为纯访客 - eventId: {}, personName: {}, eventTime: {}", 
                    record.getEventId(), personName, eventTime != null ? dateFormat.format(eventTime) : "null");
                
                record.setIsUnreservedVisitor(1);
                record.setVipTypeName(null);
                record.setReservationTimeRange(null);
                
                // 确保记录有ID才能更新
                if (record.getId() == null) {
                    log.warn("⚠️ [访客预约状态] 记录ID为空，无法更新 - eventId: {}", record.getEventId());
                    // 尝试通过eventId重新查询获取ID
                    if (record.getEventId() != null) {
                        AcmsEventRecord existingRecord = this.getByEventId(record.getEventId());
                        if (existingRecord != null && existingRecord.getId() != null) {
                            record.setId(existingRecord.getId());
                            log.info("✅ [访客预约状态] 通过eventId获取到记录ID: {}", record.getId());
                        } else {
                            log.error("❌ [访客预约状态] 无法找到记录，无法更新 - eventId: {}", record.getEventId());
                            return false;
                        }
                    } else {
                        log.error("❌ [访客预约状态] eventId也为空，无法更新");
                        return false;
                    }
                }
                
                boolean result = this.updateById(record);
                if (result) {
                    log.info("✅ [访客预约状态] 标记为纯访客成功 - eventId: {}, id: {}, isUnreservedVisitor: {}", 
                        record.getEventId(), record.getId(), record.getIsUnreservedVisitor());
                } else {
                    log.error("❌ [访客预约状态] 标记为纯访客失败 - eventId: {}, id: {}", 
                        record.getEventId(), record.getId());
                }
                return result;
            }
            
        } catch (Exception e) {
            log.error("❌ [访客预约状态] 更新异常 - eventId: {}", record.getEventId(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateVisitorReservationStatus(List<AcmsEventRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (AcmsEventRecord record : records) {
            if (updateVisitorReservationStatus(record)) {
                successCount++;
            }
        }
        
        log.info("📊 [访客预约状态] 批量更新完成 - 总数: {}, 成功: {}", records.size(), successCount);
        return successCount;
    }
    
    @Override
    public boolean checkDuplicateInTimeRange(String personName, Date startTime, Date endTime) {
        if (personName == null || personName.trim().isEmpty() || startTime == null || endTime == null) {
            return false;
        }
        
        try {
            QueryWrapper<AcmsEventRecord> query = new QueryWrapper<>();
            query.eq("person_name", personName)
                 .between("event_time", startTime, endTime)
                 .eq("deleted", 0);
            
            // 查询是否存在记录
            long count = this.count(query);
            
            if (count > 0) {
                log.debug("🔍 [重复检查] 发现重复记录 - 姓名: {}, 时间范围: {} ~ {}, 记录数: {}", 
                    personName, dateFormat.format(startTime), dateFormat.format(endTime), count);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("❌ [重复检查] 查询异常 - 姓名: {}, error: {}", personName, e.getMessage(), e);
            // 查询异常时返回false，允许保存（避免误判）
            return false;
        }
    }
}

