package com.parkingmanage.service;

import com.parkingmanage.entity.FocusAlertRecord;
import com.parkingmanage.entity.FocusWatchList;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.mapper.FocusWatchListMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 关注提醒处理服务
 * 
 * @author System
 */
@Slf4j
@Service
public class FocusAlertService {

    @Autowired
    private FocusWatchService focusWatchService;

    @Autowired
    private FocusWatchListMapper watchListMapper;
    
    @Autowired(required = false)
    private com.parkingmanage.websocket.VehicleWebSocketHandler webSocketHandler;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");

    /**
     * 处理车辆进场关注提醒
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleVehicleEntryAlert(String plateNumber, Date enterTime, String channelName, 
                                       String photoUrl, VisitorReservationSync reservation) {
        try {
            // 检查是否有用户关注这辆车
            List<Integer> matchedUsers = focusWatchService.checkFocusMatch("plate", plateNumber);
            
            if (matchedUsers.isEmpty()) {
                log.debug("ℹ️ [关注追踪] 车牌 {} 未被关注", plateNumber);
                return;
            }
            
            log.info("🔔 [关注追踪] 车牌 {} 进场，匹配到 {} 个关注用户", plateNumber, matchedUsers.size());
            
            // 为每个关注用户创建提醒记录
            for (Integer userId : matchedUsers) {
                FocusWatchList watchInfo = watchListMapper.findByUserAndValue(userId, "plate", plateNumber);
                
                if (watchInfo == null) {
                    continue;
                }
                
                FocusAlertRecord alert = new FocusAlertRecord();
                alert.setUserId(userId);
                alert.setWatchId(watchInfo.getId());
                alert.setAlertType("vehicle");
                alert.setWatchValue(plateNumber);
                alert.setEventType("entry");
                alert.setEventTime(enterTime);
                alert.setChannelName(channelName);
                alert.setPhotoUrl(photoUrl);
                alert.setRemark(watchInfo.getRemark());
                alert.setIsConfirmed(0);
                alert.setCreatedAt(new Date());
                
                // 如果有预约信息，填充预约字段和访客信息
                if (reservation != null) {
                    // 预约信息
                    alert.setReservationPerson(reservation.getVisitorName());
                    alert.setReservationPhone(reservation.getVisitorPhone());
                    alert.setReservationReason(reservation.getPassName());
                    
                    // 访客信息（用于列表显示）
                    alert.setPersonName(reservation.getVisitorName());
                    alert.setPhoneNo(reservation.getVisitorPhone());
                    alert.setDepartment(reservation.getPassDep());
                    
                    // 访客类型和园区信息
                    alert.setVisitorPassName(reservation.getPassName());
                    alert.setVisitorVipType(reservation.getVipTypeName());
                    alert.setVisitorParkName(reservation.getParkName());
                    
                    // 格式化预约时间段
                    if (reservation.getGatewayTransitBeginTime() != null && reservation.getGatewayTransitEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getGatewayTransitBeginTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getGatewayTransitEndTime());
                        alert.setReservationTimeRange(timeRange);
                        alert.setVisitorReservationTimeRange(timeRange);
                    } else if (reservation.getStartTime() != null && reservation.getEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getStartTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getEndTime());
                        alert.setReservationTimeRange(timeRange);
                        alert.setVisitorReservationTimeRange(timeRange);
                    }
                }
                
                // 保存提醒记录
                focusWatchService.saveAlertRecord(alert);
                
                // WebSocket推送关注提醒
                if (webSocketHandler != null) {
                    pushFocusAlert(alert, "entry");
                }
            }
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 处理车辆进场提醒失败: 车牌={}", plateNumber, e);
        }
    }

    /**
     * 处理车辆出场关注提醒
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleVehicleExitAlert(String plateNumber, Date leaveTime, String leaveChannelName,
                                      String enterChannelName, String stoppingTime, String photoUrl,
                                      VisitorReservationSync reservation) {
        try {
            // 检查是否有用户关注这辆车
            List<Integer> matchedUsers = focusWatchService.checkFocusMatch("plate", plateNumber);
            
            if (matchedUsers.isEmpty()) {
                log.debug("ℹ️ [关注追踪] 车牌 {} 未被关注", plateNumber);
                return;
            }
            
            log.info("🔔 [关注追踪] 车牌 {} 出场，匹配到 {} 个关注用户", plateNumber, matchedUsers.size());
            
            // 为每个关注用户创建提醒记录
            for (Integer userId : matchedUsers) {
                FocusWatchList watchInfo = watchListMapper.findByUserAndValue(userId, "plate", plateNumber);
                
                if (watchInfo == null) {
                    continue;
                }
                
                FocusAlertRecord alert = new FocusAlertRecord();
                alert.setUserId(userId);
                alert.setWatchId(watchInfo.getId());
                alert.setAlertType("vehicle");
                alert.setWatchValue(plateNumber);
                alert.setEventType("exit");
                alert.setEventTime(leaveTime);
                alert.setChannelName(leaveChannelName);
                alert.setEnterChannelName(enterChannelName);
                alert.setStoppingTime(stoppingTime);
                alert.setPhotoUrl(photoUrl);
                alert.setRemark(watchInfo.getRemark());
                alert.setIsConfirmed(0);
                alert.setCreatedAt(new Date());
                
                // 如果有预约信息，填充预约字段和访客信息
                if (reservation != null) {
                    // 预约信息
                    alert.setReservationPerson(reservation.getVisitorName());
                    alert.setReservationPhone(reservation.getVisitorPhone());
                    alert.setReservationReason(reservation.getPassName());
                    
                    // 访客信息（用于列表显示）
                    alert.setPersonName(reservation.getVisitorName());
                    alert.setPhoneNo(reservation.getVisitorPhone());
                    alert.setDepartment(reservation.getPassDep());
                    
                    // 访客类型和园区信息
                    alert.setVisitorPassName(reservation.getPassName());
                    alert.setVisitorVipType(reservation.getVipTypeName());
                    alert.setVisitorParkName(reservation.getParkName());
                    
                    // 格式化预约时间段
                    if (reservation.getGatewayTransitBeginTime() != null && reservation.getGatewayTransitEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getGatewayTransitBeginTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getGatewayTransitEndTime());
                        alert.setReservationTimeRange(timeRange);
                        alert.setVisitorReservationTimeRange(timeRange);
                    } else if (reservation.getStartTime() != null && reservation.getEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getStartTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getEndTime());
                        alert.setReservationTimeRange(timeRange);
                        alert.setVisitorReservationTimeRange(timeRange);
                    }
                }
                
                // 保存提醒记录
                focusWatchService.saveAlertRecord(alert);
                
                // WebSocket推送关注提醒
                if (webSocketHandler != null) {
                    pushFocusAlert(alert, "exit");
                }
            }
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 处理车辆出场提醒失败: 车牌={}", plateNumber, e);
        }
    }

    /**
     * 处理人员进出场关注提醒
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePersonAlert(String idCard, String personName, String eventType, Date eventTime,
                                  String channelName, String department, String phoneNo, String photoUrl,
                                  VisitorReservationSync reservation) {
        try {
            // 检查是否有用户关注这个人
            List<Integer> matchedUsers = focusWatchService.checkFocusMatch("idcard", idCard);
            
            if (matchedUsers.isEmpty()) {
                log.debug("ℹ️ [关注追踪] 身份证 {} 未被关注", idCard);
                return;
            }
            
            log.info("🔔 [关注追踪] 身份证 {} ({}) {}，匹配到 {} 个关注用户", 
                idCard, personName, "entry".equals(eventType) ? "进场" : "出场", matchedUsers.size());
            
            // 为每个关注用户创建提醒记录
            for (Integer userId : matchedUsers) {
                FocusWatchList watchInfo = watchListMapper.findByUserAndValue(userId, "idcard", idCard);
                
                if (watchInfo == null) {
                    continue;
                }
                
                FocusAlertRecord alert = new FocusAlertRecord();
                alert.setUserId(userId);
                alert.setWatchId(watchInfo.getId());
                alert.setAlertType("person");
                alert.setWatchValue(idCard);
                alert.setEventType(eventType);
                alert.setEventTime(eventTime);
                alert.setChannelName(channelName);
                alert.setPersonName(personName);
                alert.setDepartment(department);
                alert.setPhoneNo(phoneNo);
                alert.setPhotoUrl(photoUrl);
                alert.setRemark(watchInfo.getRemark());
                alert.setIsConfirmed(0);
                alert.setCreatedAt(new Date());
                
                // 如果有预约信息，填充预约字段
                if (reservation != null) {
                    alert.setVisitorPassName(reservation.getPassName());
                    alert.setVisitorVipType(reservation.getVipTypeName());
                    alert.setVisitorParkName(reservation.getParkName());
                    
                    // 格式化预约时间段
                    if (reservation.getGatewayTransitBeginTime() != null && reservation.getGatewayTransitEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getGatewayTransitBeginTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getGatewayTransitEndTime());
                        alert.setVisitorReservationTimeRange(timeRange);
                    } else if (reservation.getStartTime() != null && reservation.getEndTime() != null) {
                        String timeRange = DATE_FORMAT.format(reservation.getStartTime()) + 
                                         " - " + DATE_FORMAT.format(reservation.getEndTime());
                        alert.setVisitorReservationTimeRange(timeRange);
                    }
                }
                
                // 保存提醒记录
                focusWatchService.saveAlertRecord(alert);
                
                // WebSocket推送关注提醒
                if (webSocketHandler != null) {
                    pushFocusAlert(alert, eventType);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ [关注追踪] 处理人员进出场提醒失败: 身份证={}", idCard, e);
        }
    }
    
    /**
     * 推送关注提醒到前端
     */
    private void pushFocusAlert(FocusAlertRecord alert, String eventType) {
        try {
            com.alibaba.fastjson.JSONObject message = new com.alibaba.fastjson.JSONObject();
            message.put("type", "focusAlert");
            message.put("alertType", alert.getAlertType());
            message.put("eventType", eventType);
            message.put("watchValue", alert.getWatchValue());
            message.put("channelName", alert.getChannelName());
            message.put("eventTime", alert.getEventTime());
            message.put("photoUrl", alert.getPhotoUrl());
            
            // 访客基本信息
            message.put("personName", alert.getPersonName());
            message.put("department", alert.getDepartment());
            message.put("phoneNo", alert.getPhoneNo());
            message.put("remark", alert.getRemark());
            
            // 预约信息
            message.put("reservationPerson", alert.getReservationPerson());
            message.put("reservationPhone", alert.getReservationPhone());
            message.put("reservationReason", alert.getReservationReason());
            message.put("reservationTimeRange", alert.getReservationTimeRange());
            
            // 访客详细信息
            message.put("visitorPassName", alert.getVisitorPassName());
            message.put("visitorVipType", alert.getVisitorVipType());
            message.put("visitorParkName", alert.getVisitorParkName());
            message.put("visitorReservationTimeRange", alert.getVisitorReservationTimeRange());
            
            message.put("alertId", alert.getId());
            message.put("timestamp", System.currentTimeMillis());
            
            // 详细日志：输出完整的推送数据
            log.info("📢 [关注追踪] WebSocket推送数据详情:");
            log.info("   type: focusAlert");
            log.info("   alertType: {}", alert.getAlertType());
            log.info("   watchValue: {}", alert.getWatchValue());
            log.info("   预约人: {}", alert.getReservationPerson());
            log.info("   预约电话: {}", alert.getReservationPhone());
            log.info("   预约事由: {}", alert.getReservationReason());
            log.info("   预约时段: {}", alert.getReservationTimeRange());
            log.info("   访客姓名: {}", alert.getPersonName());
            log.info("   被访部门: {}", alert.getDepartment());
            log.info("   照片URL: {}", alert.getPhotoUrl());
            
            webSocketHandler.broadcastMessage(message);
            log.info("✅ [关注追踪] WebSocket消息已发送");
        } catch (Exception e) {
            log.error("❌ [关注追踪] WebSocket推送失败", e);
        }
    }
}
