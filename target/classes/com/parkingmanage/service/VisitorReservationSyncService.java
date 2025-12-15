package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.mapper.VisitorReservationSyncMapper;
import com.parkingmanage.mapper.ReportCarInMapper;
import com.parkingmanage.mapper.ReportCarOutMapper;
import com.parkingmanage.util.VisitTimeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 访客预约同步服务（直接数据库去重模式）
 * 
 * 处理逻辑：
 * 1. 首次执行：最多三次拉取合并，用外部数据id对比数据库reservation_id去重插入
 * 2. 后续执行：每10秒按"当前时间前10秒-当前时间"只调用一次外部接口并直接入库
 * 
 * 特点：
 * - 直接查询数据库reservation_id进行去重，无需内存缓存
 * - 批量查询优化性能
 * - 支持多实例部署
 * 
 * @author System
 */
@Slf4j
@Service
public class VisitorReservationSyncService {

    @Autowired
    private VisitorReservationSyncMapper syncMapper;

    @Autowired
    private ReportCarInMapper reportCarInMapper;

    @Autowired
    private ReportCarOutMapper reportCarOutMapper;

    /**
     * 核心方法：同步外部数据（直接数据库去重模式）
     * 
     * 处理逻辑：
     * 1. 首次执行：最多三次拉取合并，用外部数据id对比数据库reservation_id去重插入
     * 2. 后续执行：每10秒按"当前时间前10秒-当前时间"只调用一次外部接口并直接入库
     * 
     * @param externalDataList 从外部接口查询到的数据
     * @return 变更统计
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncData(List<VisitorVipAutoService.VisitorReservation> externalDataList) {
        long startTime = System.currentTimeMillis();
        log.info("🔄 [同步开始] 开始同步数据，共{}条记录", 
            externalDataList != null ? externalDataList.size() : 0);
        
        if (externalDataList == null || externalDataList.isEmpty()) {
            return new SyncResult();
        }
        
        SyncResult result = new SyncResult();
        
        // Step 1: 查询数据库中已存在的reservation_id集合（批量查询优化性能）
        long queryStart = System.currentTimeMillis();
        Set<String> existingIds = queryExistingReservationIds(externalDataList);
        long queryTime = System.currentTimeMillis() - queryStart;
        log.info("📊 [数据库查询] 查询已存在的reservation_id: {}个，耗时: {}ms", existingIds.size(), queryTime);
        
        // Step 2: 对比外部数据和数据库，筛选需要插入的数据
        List<VisitorReservationSync> toInsert = new ArrayList<>();
        
        for (VisitorVipAutoService.VisitorReservation external : externalDataList) {
            String reservationId = external.getReservationId();
            if (reservationId == null || reservationId.isEmpty()) {
                log.warn("⚠️ [跳过] 预约ID为空，跳过该记录");
                continue;
            }
            
            // 检查数据库中是否已存在
            if (existingIds.contains(reservationId)) {
                // 数据库中已存在，跳过
                result.unchangedCount++;
                log.debug("✓ [已存在] 预约ID: {}, 访客: {} - 数据库中已存在，跳过", reservationId, external.getVisitorName());
            } else {
                // 数据库中不存在，需要插入
                VisitorReservationSync entity = convertToEntity(external);
                toInsert.add(entity);
                result.addedIds.add(reservationId);
                result.addedEntities.add(entity);
                log.info("🆕 [新增] 预约ID: {}, 访客: {}, 车牌: {}", reservationId, external.getVisitorName(), external.getCarNumber());
            }
        }
        
        // Step 3: 批量插入数据库
        long saveStart = System.currentTimeMillis();
        if (!toInsert.isEmpty()) {
            // 分批处理，每批100条
            List<List<VisitorReservationSync>> batches = splitList(toInsert, 100);
            int totalSaved = 0;
            int batchIndex = 0;
            
            for (List<VisitorReservationSync> batch : batches) {
                batchIndex++;
                try {
                    log.info("💾 [数据库] 开始保存第{}批，共{}条数据", batchIndex, batch.size());
                    
                    // 执行批量插入或更新（使用ON DUPLICATE KEY UPDATE处理重复）
                    int saved = syncMapper.batchInsertOrUpdate(batch);
                    totalSaved += saved;
                    
                    log.info("✅ [数据库] 第{}批保存完成: 本批{}条，影响行数: {}", batchIndex, batch.size(), saved);
                    
                } catch (Exception e) {
                    log.error("❌ [数据库] 第{}批保存失败: 本批{}条，错误: {}", batchIndex, batch.size(), e.getMessage(), e);
                    throw e; // 重新抛出异常，让事务回滚
                }
            }
            log.info("💾 [数据库] 批量保存完成: 总计{}条数据，累计影响行数: {}", toInsert.size(), totalSaved);
        } else {
            log.info("ℹ️ [数据库] 没有需要保存的数据");
        }
        
        // 总耗时统计
        long totalCost = System.currentTimeMillis() - startTime;
        result.totalCost = totalCost;
        
        log.info("✅ [同步完成] 新增:{}, 已存在:{}, 总耗时:{}ms", 
            result.addedIds.size(), 
            result.unchangedCount,
            totalCost);
        return result;
    }
    
    /**
     * 批量查询数据库中已存在的reservation_id集合
     * 
     * @param externalDataList 外部数据列表
     * @return 已存在的reservation_id集合
     */
    private Set<String> queryExistingReservationIds(List<VisitorVipAutoService.VisitorReservation> externalDataList) {
        Set<String> existingIds = new HashSet<>();
        
        if (externalDataList == null || externalDataList.isEmpty()) {
            return existingIds;
        }
        
        // 提取所有reservation_id
        List<String> reservationIds = new ArrayList<>();
        for (VisitorVipAutoService.VisitorReservation external : externalDataList) {
            String reservationId = external.getReservationId();
            if (reservationId != null && !reservationId.isEmpty()) {
                reservationIds.add(reservationId);
            }
        }
        
        if (reservationIds.isEmpty()) {
            return existingIds;
        }
        
        // 分批查询，每批500条
        int batchSize = 500;
        for (int i = 0; i < reservationIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, reservationIds.size());
            List<String> batch = reservationIds.subList(i, endIndex);
            
            // 查询这批reservation_id中哪些已存在
            QueryWrapper<VisitorReservationSync> query = new QueryWrapper<>();
            query.select("reservation_id")
                 .in("reservation_id", batch)
                 .eq("deleted", 0);
            
            List<VisitorReservationSync> existingList = syncMapper.selectList(query);
            for (VisitorReservationSync entity : existingList) {
                if (entity.getReservationId() != null) {
                    existingIds.add(entity.getReservationId());
                }
            }
        }
        
        return existingIds;
    }

    /**
     * 转换外部数据为数据库实体
     */
    private VisitorReservationSync convertToEntity(VisitorVipAutoService.VisitorReservation external) {
        VisitorReservationSync entity = new VisitorReservationSync();
        
        entity.setReservationId(external.getReservationId());
        entity.setVisitorName(external.getVisitorName());
        entity.setVisitorPhone(external.getVisitorPhone());
        entity.setVisitorIdCard(external.getVisitorIdCard());
        entity.setPassDep(external.getPassDep());
        entity.setCarNumber(external.getCarNumber());
        entity.setVipTypeName(external.getVipTypeName());
        entity.setParkName(external.getParkName());
        entity.setPassName(external.getPassName());
        entity.setApplyStateName(external.getApplyStateName());
        entity.setApplyFromName(external.getApplyFromName());
        
        // 添加调试日志，查看实际接收到的数据
        if (log.isDebugEnabled()) {
            log.debug("📋 [数据转换] 预约ID: {}, 访客: {}, 车牌: {}", 
                external.getReservationId(), external.getVisitorName(), external.getCarNumber());
            log.debug("📋 [网关通行时间] 开始时间原始值: {}, 结束时间原始值: {}", 
                external.getGatewayTransitBeginTime(), external.getGatewayTransitEndTime());
            log.debug("📋 [其他字段] 被访人: {}, 申请状态: {}, 发起渠道: {}", 
                external.getPassName(), external.getApplyStateName(), external.getApplyFromName());
        }
        entity.setRemark1(external.getRemark1());
        entity.setRemark2(external.getRemark2());
        entity.setRemark3(external.getRemark3());
        entity.setVipOpened(0); // 未开通
        entity.setApiCalled(0); // 未调用接口添加访客
        
        // 解析时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (external.getStartTime() != null && !external.getStartTime().isEmpty()) {
                entity.setStartTime(sdf.parse(external.getStartTime()));
            }
            if (external.getEndTime() != null && !external.getEndTime().isEmpty()) {
                entity.setEndTime(sdf.parse(external.getEndTime()));
            }
            if (external.getCreateTime() != null && !external.getCreateTime().isEmpty()) {
                entity.setExternalCreateTime(sdf.parse(external.getCreateTime()));
            }
            // 🔧 修复：添加网关通行时间的解析（添加详细的日志）
            if (external.getGatewayTransitBeginTime() != null && !external.getGatewayTransitBeginTime().isEmpty()) {
                try {
                    entity.setGatewayTransitBeginTime(sdf.parse(external.getGatewayTransitBeginTime()));
                    log.debug("✅ [网关通行时间] 预约ID: {}, 开始时间解析成功: {}", 
                        external.getReservationId(), external.getGatewayTransitBeginTime());
                } catch (ParseException e) {
                    log.warn("⚠️ [网关通行时间] 预约ID: {}, 开始时间解析失败: {}, 错误: {}", 
                        external.getReservationId(), external.getGatewayTransitBeginTime(), e.getMessage());
                }
            } else {
                log.debug("ℹ️ [网关通行时间] 预约ID: {}, 开始时间为空", external.getReservationId());
            }
            if (external.getGatewayTransitEndTime() != null && !external.getGatewayTransitEndTime().isEmpty()) {
                try {
                    entity.setGatewayTransitEndTime(sdf.parse(external.getGatewayTransitEndTime()));
                    log.debug("✅ [网关通行时间] 预约ID: {}, 结束时间解析成功: {}", 
                        external.getReservationId(), external.getGatewayTransitEndTime());
                } catch (ParseException e) {
                    log.warn("⚠️ [网关通行时间] 预约ID: {}, 结束时间解析失败: {}, 错误: {}", 
                        external.getReservationId(), external.getGatewayTransitEndTime(), e.getMessage());
                }
            } else {
                log.debug("ℹ️ [网关通行时间] 预约ID: {}, 结束时间为空", external.getReservationId());
            }
        } catch (ParseException e) {
            log.warn("⚠️ 时间解析失败: {}", e.getMessage());
        }
        
        // 计算数据指纹（包含网关通行时间）
        int dataHash = Objects.hash(
            external.getReservationId(),
            external.getVisitorName(),
            external.getCarNumber(),
            external.getStartTime(),
            external.getEndTime(),
            external.getGatewayTransitBeginTime(),
            external.getGatewayTransitEndTime(),
            external.getVipTypeName(),
            external.getVisitorPhone()
        );
        entity.setDataHash(dataHash);
        
        // 根据申请状态设置初始状态（3.1, 3.2, 3.3）
        initVisitStatus(entity);
        
        return entity;
    }

    /**
     * 初始化进出场状态（3.1, 3.2, 3.3）
     * 
     * @param entity 预约记录实体
     */
    private void initVisitStatus(VisitorReservationSync entity) {
        String applyStateName = entity.getApplyStateName();
        
        // 3.1 初始状态设置
        if ("待来访".equals(applyStateName)) {
            entity.setPersonVisitStatus("人未来访");
            entity.setPersonVisitTimes(null);
            entity.setCarVisitStatus("车未来访");
            entity.setCarVisitTimes(null);
            log.debug("✅ [初始状态] 预约ID: {}, 状态: 待来访 - 设置为未来访", entity.getReservationId());
        } 
        // 3.2 和 3.3 "来访中"或"已签离"状态的初始判断
        else if ("来访中".equals(applyStateName) || "已签离".equals(applyStateName)) {
            // 判断车辆状态
            initCarVisitStatus(entity);
            // 人员状态初始化为未来访
            entity.setPersonVisitStatus("人未来访");
            entity.setPersonVisitTimes(null);
        } else {
            // 其他状态，默认设置为未来访
            entity.setPersonVisitStatus("人未来访");
            entity.setPersonVisitTimes(null);
            entity.setCarVisitStatus("车未来访");
            entity.setCarVisitTimes(null);
        }
    }

    /**
     * 初始化车辆进出场状态（3.2, 3.3）
     * 
     * @param entity 预约记录实体
     */
    private void initCarVisitStatus(VisitorReservationSync entity) {
        String carNumber = entity.getCarNumber();
        Date externalCreateTime = entity.getExternalCreateTime();
        
        if (carNumber == null || carNumber.trim().isEmpty()) {
            entity.setCarVisitStatus("车未来访");
            entity.setCarVisitTimes(null);
            log.debug("ℹ️ [车辆状态] 预约ID: {}, 车牌为空，设置为车未来访", entity.getReservationId());
            return;
        }

        if (externalCreateTime == null) {
            entity.setCarVisitStatus("车未来访");
            entity.setCarVisitTimes(null);
            log.debug("ℹ️ [车辆状态] 预约ID: {}, 外部创建时间为空，设置为车未来访", entity.getReservationId());
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTimeStr = sdf.format(externalCreateTime);

        try {
            // 1. 查询 report_car_out 表，获取最新的离场记录
            LambdaQueryWrapper<ReportCarOut> outQuery = new LambdaQueryWrapper<>();
            outQuery.eq(ReportCarOut::getCarLicenseNumber, carNumber)
                    .gt(ReportCarOut::getLeaveTime, createTimeStr)
                    .orderByDesc(ReportCarOut::getLeaveTime)
                    .last("LIMIT 1");
            ReportCarOut latestOut = reportCarOutMapper.selectOne(outQuery);

            // 2. 查询 report_car_in 表，获取最新的进场记录
            LambdaQueryWrapper<ReportCarIn> inQuery = new LambdaQueryWrapper<>();
            inQuery.eq(ReportCarIn::getCarLicenseNumber, carNumber)
                   .gt(ReportCarIn::getEnterTime, createTimeStr)
                   .orderByDesc(ReportCarIn::getEnterTime)
                   .last("LIMIT 1");
            ReportCarIn latestIn = reportCarInMapper.selectOne(inQuery);

            // 情况1：存在离场记录
            if (latestOut != null) {
                String outEnterTime = latestOut.getEnterTime();
                String outLeaveTime = latestOut.getLeaveTime();

                if (latestIn != null) {
                    String inEnterTime = latestIn.getEnterTime();
                    
                    // 比较最新离场记录中的进场时间与最新进场记录中的进场时间
                    if (VisitTimeUtils.isTimeApproximate(outEnterTime, inEnterTime, 20 * 1000)) {
                        // 时间近似（相差不到20秒），说明车辆已经离场
                        entity.setCarVisitStatus("已离场");
                        List<VisitTimeUtils.VisitTimeRecord> records = new ArrayList<>();
                        records.add(new VisitTimeUtils.VisitTimeRecord(outEnterTime, outLeaveTime));
                        entity.setCarVisitTimes(VisitTimeUtils.toJsonString(records));
                        log.info("✅ [车辆状态] 预约ID: {}, 车牌: {}, 状态: 已离场", 
                            entity.getReservationId(), carNumber);
                    } else if (VisitTimeUtils.compareTime(outEnterTime, inEnterTime) < 0) {
                        // 最新离场记录中的进场时间 < 最新进场记录中的进场时间
                        // 说明这个进场记录的进场时间是最新的
                        entity.setCarVisitStatus("已进场");
                        List<VisitTimeUtils.VisitTimeRecord> records = new ArrayList<>();
                        records.add(new VisitTimeUtils.VisitTimeRecord(inEnterTime, null));
                        entity.setCarVisitTimes(VisitTimeUtils.toJsonString(records));
                        log.info("✅ [车辆状态] 预约ID: {}, 车牌: {}, 状态: 已进场", 
                            entity.getReservationId(), carNumber);
                    } else {
                        // 不会出现这种情况，但为了安全起见，设置为已离场
                        entity.setCarVisitStatus("已离场");
                        List<VisitTimeUtils.VisitTimeRecord> records = new ArrayList<>();
                        records.add(new VisitTimeUtils.VisitTimeRecord(outEnterTime, outLeaveTime));
                        entity.setCarVisitTimes(VisitTimeUtils.toJsonString(records));
                        log.warn("⚠️ [车辆状态] 预约ID: {}, 车牌: {}, 异常情况，设置为已离场", 
                            entity.getReservationId(), carNumber);
                    }
                } else {
                    // 没有进场记录，只有离场记录
                    entity.setCarVisitStatus("已离场");
                    List<VisitTimeUtils.VisitTimeRecord> records = new ArrayList<>();
                    records.add(new VisitTimeUtils.VisitTimeRecord(outEnterTime, outLeaveTime));
                    entity.setCarVisitTimes(VisitTimeUtils.toJsonString(records));
                    log.info("✅ [车辆状态] 预约ID: {}, 车牌: {}, 状态: 已离场（无进场记录）", 
                        entity.getReservationId(), carNumber);
                }
            } 
            // 情况2：不存在离场记录
            else {
                if (latestIn != null) {
                    // 有进场记录
                    String inEnterTime = latestIn.getEnterTime();
                    entity.setCarVisitStatus("已进场");
                    List<VisitTimeUtils.VisitTimeRecord> records = new ArrayList<>();
                    records.add(new VisitTimeUtils.VisitTimeRecord(inEnterTime, null));
                    entity.setCarVisitTimes(VisitTimeUtils.toJsonString(records));
                    log.info("✅ [车辆状态] 预约ID: {}, 车牌: {}, 状态: 已进场", 
                        entity.getReservationId(), carNumber);
                } else {
                    // 没有进出场记录
                    entity.setCarVisitStatus("车未来访");
                    entity.setCarVisitTimes(null);
                    log.info("ℹ️ [车辆状态] 预约ID: {}, 车牌: {}, 状态: 车未来访", 
                        entity.getReservationId(), carNumber);
                }
            }
        } catch (Exception e) {
            log.error("❌ [车辆状态] 预约ID: {}, 车牌: {}, 初始化车辆状态失败: {}", 
                entity.getReservationId(), carNumber, e.getMessage(), e);
            entity.setCarVisitStatus("车未来访");
            entity.setCarVisitTimes(null);
        }
    }

    /**
     * 检查预约记录是否存在
     * 
     * @param reservationId 预约ID
     * @return true-存在，false-不存在
     */
    public boolean checkReservationExists(String reservationId) {
        if (reservationId == null || reservationId.isEmpty()) {
            return false;
        }
        
        QueryWrapper<VisitorReservationSync> query = new QueryWrapper<>();
        query.eq("reservation_id", reservationId)
             .eq("deleted", 0);
        Integer count = syncMapper.selectCount(query);
        return count != null && count > 0;
    }
    
    /**
     * 如果不存在则插入
     * 
     * @param reservation 预约记录
     * @return true-插入成功，false-已存在或插入失败
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insertIfNotExists(VisitorVipAutoService.VisitorReservation reservation) {
        if (reservation == null || reservation.getReservationId() == null) {
            return false;
        }
        
        // 检查是否已存在
        if (checkReservationExists(reservation.getReservationId())) {
            return false;
        }
        
        // 转换为实体并插入
        VisitorReservationSync entity = convertToEntity(reservation);
        try {
            int result = syncMapper.insert(entity);
            return result > 0;
        } catch (Exception e) {
            log.error("❌ [插入失败] 预约ID: {}, 错误: {}", reservation.getReservationId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 如果不存在则插入（使用实体对象）
     * 
     * @param entity 预约记录实体
     * @return true-插入成功，false-已存在或插入失败
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insertIfNotExists(VisitorReservationSync entity) {
        if (entity == null || entity.getReservationId() == null) {
            return false;
        }
        
        // 检查是否已存在
        if (checkReservationExists(entity.getReservationId())) {
            return false;
        }
        
        // 插入
        try {
            int result = syncMapper.insert(entity);
            return result > 0;
        } catch (Exception e) {
            log.error("❌ [插入失败] 预约ID: {}, 错误: {}", entity.getReservationId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 分割列表
     */
    private <T> List<List<T>> splitList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    /**
     * 获取性能统计
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("mode", "直接数据库去重模式");
        stats.put("description", "使用数据库reservation_id直接去重，无需内存缓存");
        return stats;
    }

    /**
     * 处理车辆进场记录（4.1）
     * 
     * @param carLicenseNumber 车牌号
     * @param enterTime 进场时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCarIn(String carLicenseNumber, String enterTime) {
        if (carLicenseNumber == null || carLicenseNumber.trim().isEmpty() 
                || enterTime == null || enterTime.trim().isEmpty()) {
            log.warn("⚠️ [车辆进场] 车牌号或进场时间为空，跳过处理");
            return;
        }

        try {
            // 1. 查询预约记录
            List<VisitorReservationSync> reservations = findMatchingReservations(carLicenseNumber, enterTime, null);
            
            if (reservations.isEmpty()) {
                log.debug("ℹ️ [车辆进场] 车牌: {}, 时间: {}, 未找到匹配的预约记录", carLicenseNumber, enterTime);
                return;
            }

            // 2. 更新预约记录
            for (VisitorReservationSync reservation : reservations) {
                // 更新车辆状态为"已进场"
                reservation.setCarVisitStatus("已进场");
                
                // 添加进场记录到JSON数组
                String carVisitTimes = reservation.getCarVisitTimes();
                String updatedTimes = VisitTimeUtils.addEnterTime(carVisitTimes, enterTime);
                reservation.setCarVisitTimes(updatedTimes);
                
                // 保存到数据库
                syncMapper.updateById(reservation);
                log.info("✅ [车辆进场] 预约ID: {}, 车牌: {}, 时间: {}, 状态已更新为已进场", 
                    reservation.getReservationId(), carLicenseNumber, enterTime);
            }
        } catch (Exception e) {
            log.error("❌ [车辆进场] 车牌: {}, 时间: {}, 处理失败: {}", 
                carLicenseNumber, enterTime, e.getMessage(), e);
        }
    }

    /**
     * 处理车辆离场记录（4.2）
     * 
     * @param carLicenseNumber 车牌号
     * @param enterTime 进场时间（从离场记录中获取）
     * @param leaveTime 离场时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCarOut(String carLicenseNumber, String enterTime, String leaveTime) {
        if (carLicenseNumber == null || carLicenseNumber.trim().isEmpty() 
                || leaveTime == null || leaveTime.trim().isEmpty()) {
            log.warn("⚠️ [车辆离场] 车牌号或离场时间为空，跳过处理");
            return;
        }

        try {
            // 1. 查询预约记录
            List<VisitorReservationSync> reservations = findMatchingReservations(carLicenseNumber, null, leaveTime);
            
            if (reservations.isEmpty()) {
                log.debug("ℹ️ [车辆离场] 车牌: {}, 时间: {}, 未找到匹配的预约记录", carLicenseNumber, leaveTime);
                return;
            }

            // 2. 更新预约记录
            for (VisitorReservationSync reservation : reservations) {
                // 更新车辆状态为"已离场"
                reservation.setCarVisitStatus("已离场");
                
                // 更新离场时间
                String carVisitTimes = reservation.getCarVisitTimes();
                VisitTimeUtils.UpdateLeaveTimeResult result = VisitTimeUtils.updateLeaveTime(
                    carVisitTimes, enterTime, leaveTime);
                reservation.setCarVisitTimes(result.getJsonStr());
                
                // 3. 进场时间校验
                if (result.isFound() && enterTime != null) {
                    // 查找匹配的记录进行校验
                    List<VisitTimeUtils.VisitTimeRecord> records = VisitTimeUtils.parseVisitTimes(result.getJsonStr());
                    for (VisitTimeUtils.VisitTimeRecord record : records) {
                        if (record.getLeaveTime() != null && record.getLeaveTime().equals(leaveTime)) {
                            // 比较预约记录中的enterTime与离场记录中的enterTime
                            VisitTimeUtils.CorrectEnterTimeResult correctResult = 
                                VisitTimeUtils.correctEnterTime(result.getJsonStr(), record.getEnterTime(), enterTime);
                            if (correctResult.isCorrected()) {
                                reservation.setCarVisitTimes(correctResult.getJsonStr());
                                log.warn("⚠️ [车辆离场] 预约ID: {}, 车牌: {}, 修正了进场时间: {} -> {}", 
                                    reservation.getReservationId(), carLicenseNumber, 
                                    record.getEnterTime(), enterTime);
                            }
                            break;
                        }
                    }
                }
                
                // 保存到数据库
                syncMapper.updateById(reservation);
                log.info("✅ [车辆离场] 预约ID: {}, 车牌: {}, 时间: {}, 状态已更新为已离场", 
                    reservation.getReservationId(), carLicenseNumber, leaveTime);
            }
        } catch (Exception e) {
            log.error("❌ [车辆离场] 车牌: {}, 时间: {}, 处理失败: {}", 
                carLicenseNumber, leaveTime, e.getMessage(), e);
        }
    }

    /**
     * 查找匹配的预约记录
     * 
     * @param carLicenseNumber 车牌号
     * @param enterTime 进场时间（用于进场记录查询）
     * @param leaveTime 离场时间（用于离场记录查询）
     * @return 匹配的预约记录列表
     */
    private List<VisitorReservationSync> findMatchingReservations(String carLicenseNumber, 
                                                                  String enterTime, String leaveTime) {
        List<VisitorReservationSync> result = new ArrayList<>();
        
        try {
            // 根据车牌号查询预约记录
            LambdaQueryWrapper<VisitorReservationSync> query = new LambdaQueryWrapper<>();
            query.eq(VisitorReservationSync::getCarNumber, carLicenseNumber)
                 .eq(VisitorReservationSync::getDeleted, 0);
            
            List<VisitorReservationSync> allReservations = syncMapper.selectList(query);
            
            // 根据时间范围筛选匹配的记录
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date eventTime = null;
            if (enterTime != null) {
                try {
                    eventTime = sdf.parse(enterTime);
                } catch (ParseException e) {
                    log.warn("⚠️ [查找预约记录] 进场时间解析失败: {}", enterTime);
                }
            } else if (leaveTime != null) {
                try {
                    eventTime = sdf.parse(leaveTime);
                } catch (ParseException e) {
                    log.warn("⚠️ [查找预约记录] 离场时间解析失败: {}", leaveTime);
                }
            }
            
            if (eventTime != null) {
                for (VisitorReservationSync reservation : allReservations) {
                    Date beginTime = reservation.getGatewayTransitBeginTime();
                    Date endTime = reservation.getGatewayTransitEndTime();
                    
                    // 筛选条件：事件时间在gateway_transit_begin_time和gateway_transit_end_time范围内
                    if (beginTime != null && endTime != null) {
                        if (eventTime.compareTo(beginTime) >= 0 && eventTime.compareTo(endTime) <= 0) {
                            result.add(reservation);
                        }
                    }
                }
            } else {
                // 如果时间解析失败，返回所有匹配车牌号的记录
                result.addAll(allReservations);
            }
        } catch (Exception e) {
            log.error("❌ [查找预约记录] 车牌: {}, 查询失败: {}", carLicenseNumber, e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * 处理人员进出场记录（5.1, 5.2）
     * 
     * @param personName 人员姓名
     * @param eventTime 事件发生时间
     * @param direction 方向（"进"或"出"）
     * @param carNumber 车牌号（可为null，用于判断是否有车牌号）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePersonVisit(String personName, Date eventTime, String direction, String carNumber) {
        if (personName == null || personName.trim().isEmpty() || eventTime == null) {
            log.warn("⚠️ [人员进出场] 姓名或事件时间为空，跳过处理");
            return;
        }

        try {
            // 查询预约记录
            VisitorReservationSync reservation = syncMapper.selectByVisitorNameAndTimeRange(
                personName, eventTime);

            if (reservation == null) {
                log.debug("ℹ️ [人员进出场] 姓名: {}, 时间: {}, 未找到匹配的预约记录", personName, eventTime);
                return;
            }

            // 更新人员进出场状态
            String personVisitTimes = reservation.getPersonVisitTimes();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String eventTimeStr = sdf.format(eventTime);

            if ("进".equals(direction)) {
                // 进场
                reservation.setPersonVisitStatus("人已进场");
                personVisitTimes = VisitTimeUtils.addEnterTime(personVisitTimes, eventTimeStr);
                reservation.setPersonVisitTimes(personVisitTimes);
                log.info("✅ [人员进场] 预约ID: {}, 姓名: {}, 时间: {}, 状态已更新为人已进场", 
                    reservation.getReservationId(), personName, eventTimeStr);
            } else if ("出".equals(direction)) {
                // 离场
                reservation.setPersonVisitStatus("人已离场");
                // 查找对应的进场记录并更新离场时间
                List<VisitTimeUtils.VisitTimeRecord> records = VisitTimeUtils.parseVisitTimes(personVisitTimes);
                boolean found = false;
                for (int i = records.size() - 1; i >= 0; i--) {
                    VisitTimeUtils.VisitTimeRecord record = records.get(i);
                    if (record.getLeaveTime() == null || record.getLeaveTime().isEmpty()) {
                        // 找到最近的未离场记录
                        record.setLeaveTime(eventTimeStr);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // 未找到匹配的进场记录，添加新的完整记录
                    records.add(new VisitTimeUtils.VisitTimeRecord(eventTimeStr, eventTimeStr));
                }
                reservation.setPersonVisitTimes(VisitTimeUtils.toJsonString(records));
                log.info("✅ [人员离场] 预约ID: {}, 姓名: {}, 时间: {}, 状态已更新为人已离场", 
                    reservation.getReservationId(), personName, eventTimeStr);
            }

            // 如果有车牌号，同时更新车辆进出场状态（5.1.2）
            if (carNumber != null && !carNumber.trim().isEmpty() 
                    && carNumber.equals(reservation.getCarNumber())) {
                // 车辆进出处理逻辑同第四章（reportCarIn/reportCarOut）
                // 这里可以根据需要调用handleCarIn或handleCarOut
                // 但由于eventRcv中只有人员进出信息，车辆进出信息需要从reportCarIn/reportCarOut获取
                // 所以这里暂时不处理车辆进出场
            }

            // 保存到数据库
            syncMapper.updateById(reservation);
        } catch (Exception e) {
            log.error("❌ [人员进出场] 姓名: {}, 时间: {}, 处理失败: {}", 
                personName, eventTime, e.getMessage(), e);
        }
    }

    /**
     * 同步结果
     */
    @Data
    public static class SyncResult {
        private Set<String> addedIds = new HashSet<>();
        private int unchangedCount = 0;  // 已存在的记录数
        private long totalCost = 0;
        
        private List<VisitorReservationSync> addedEntities = new ArrayList<>();
        
        public int getTotalChanges() {
            return addedIds.size();
        }
    }
}

