package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.mapper.VisitorReservationSyncMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划看板控制器
 * 统计访客预约和车辆预计数据，支持不同时间范围（今日、本周、本月、本年度）
 *
 * @author System
 */
@Api(tags = "计划看板")
@RestController
@RequestMapping("/parking/visitor/plan-dashboard")
@Slf4j
public class VisitorPlanDashboardController {

    @Autowired
    private VisitorReservationSyncMapper visitorReservationSyncMapper;

    /**
     * 获取计划看板统计数据
     * @param timeRange 时间范围：today-今日, week-本周, month-本月, year-本年度
     */
    @GetMapping("/statistics")
    @ApiOperation("获取计划看板统计数据")
    public Result<?> getStatistics(@RequestParam(defaultValue = "today") String timeRange) {
        try {
//            log.info("📊 [计划看板] 开始统计，时间范围: {}", timeRange);

            // 根据时间范围计算开始和结束时间
            Date[] timeRangeDates = getTimeRangeDates(timeRange);
            Date startDate = timeRangeDates[0];
            Date endDate = timeRangeDates[1];
            
//            log.info("📊 [计划看板] 查询范围: {} 到 {}", startDate, endDate);
            
            // 查询指定时间范围内的所有预约记录
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("start_time", startDate)
                       .le("start_time", endDate)
                       .eq("deleted", 0);
            
            List<VisitorReservationSync> currentReservations = visitorReservationSyncMapper.selectList(queryWrapper);
            
//            log.info("✅ [计划看板] 查询到 {} 条{}预约", currentReservations.size(), getTimeRangeLabel(timeRange));
            
            // 统计数据
            Map<String, Object> result = new HashMap<>();
            
            // 1. 计划访客 = 指定时间范围内的记录数
            int plannedVisitors = currentReservations.size();
            
            // 2. 已来访 = 所有曾经进场过的访客（累计数，只增不减）
            // 包括：已进场、来访中、已离场等状态，排除：未进场、已取消
            long arrivedVisitors = currentReservations.stream()
                .filter(r -> {
                    String status = r.getPersonVisitStatus();
                    if (!StringUtils.hasText(status)) {
                        return false;
                    }
                    // 只要状态包含"已进场"、"来访中"、"已离场"、"已出场"等，就算曾经进场过
                    return status.contains("已进场") || 
                           status.contains("来访中") || 
                           status.contains("已离场") ||
                           status.contains("已出场");
                })
                .count();
            
            // 3. 预计车辆 = 指定时间范围内且有车牌号的记录数
            long expectedVehicles = currentReservations.stream()
                .filter(r -> StringUtils.hasText(r.getCarNumber()))
                .count();
            
            // 4. 已到车辆 = 所有曾经进场过的车辆（累计数，只增不减）
            // 包括：已进场、已出场等状态，排除：未进场、已取消
            long arrivedVehicles = currentReservations.stream()
                .filter(r -> {
                    String status = r.getCarVisitStatus();
                    if (!StringUtils.hasText(status)) {
                        return false;
                    }
                    // 只要状态包含"已进场"、"已出场"等，就算曾经进场过
                    return status.contains("已进场") || 
                           status.contains("已出场");
                })
                .count();
            
            // 访客统计
            Map<String, Object> visitorStats = new HashMap<>();
            visitorStats.put("total", plannedVisitors);
            visitorStats.put("completed", arrivedVisitors);
            visitorStats.put("pending", plannedVisitors - arrivedVisitors);
            visitorStats.put("percentage", plannedVisitors > 0 ? 
                String.format("%.1f", (arrivedVisitors * 100.0 / plannedVisitors)) : "0.0");
            
            // 车辆统计
            Map<String, Object> vehicleStats = new HashMap<>();
            vehicleStats.put("expected", expectedVehicles);
            vehicleStats.put("arrived", arrivedVehicles);
            vehicleStats.put("current", expectedVehicles - arrivedVehicles);
            
            result.put("visitorStats", visitorStats);
            result.put("vehicleStats", vehicleStats);
            result.put("dataSource", "DATABASE");
            result.put("updateTime", new Date());
            
//            log.info("📊 [计划看板] {}访客: {}/{}, 车辆: {}/{}",
//                getTimeRangeLabel(timeRange), arrivedVisitors, plannedVisitors, arrivedVehicles, expectedVehicles);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [计划看板] 统计失败", e);
            return Result.error("获取计划数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取访客预约详细列表
     * @param type 类型：visitor-访客，vehicle-车辆
     * @param timeRange 时间范围：today-今日, week-本周, month-本月, year-本年度
     */
    @GetMapping("/detail-list")
    @ApiOperation("获取预约详细列表")
    public Result<?> getDetailList(
            @RequestParam(defaultValue = "visitor") String type,
            @RequestParam(defaultValue = "today") String timeRange,
            @RequestParam(defaultValue = "false") Boolean showAll) {
        try {
            log.info("📋 [计划看板] 查询详细列表，类型: {}, 时间范围: {}, 显示所有: {}", type, timeRange, showAll);

            // 根据时间范围计算开始和结束时间
            Date[] timeRangeDates = getTimeRangeDates(timeRange);
            Date startDate = timeRangeDates[0];
            Date endDate = timeRangeDates[1];
            
            // 查询指定时间范围内的所有预约记录
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("start_time", startDate)
                       .le("start_time", endDate)
                       .eq("deleted", 0)
                       .orderByDesc("start_time");
            
            List<VisitorReservationSync> reservations = visitorReservationSyncMapper.selectList(queryWrapper);
            
            // 根据类型和showAll参数过滤
            if (showAll) {
                // showAll=true：返回所有记录，不进行状态过滤
                if ("vehicle".equals(type)) {
                    // 车辆：只要有车牌号就返回
                    reservations = reservations.stream()
                        .filter(r -> StringUtils.hasText(r.getCarNumber()))
                        .collect(Collectors.toList());
                    log.info("🚗 [车辆全部] 所有预约车辆: {} 辆", reservations.size());
                } else {
                    // 访客：返回所有曾经来访过的记录（包括已离场的），排除未来访和未进场的
                    reservations = reservations.stream()
                        .filter(r -> {
                            String status = r.getPersonVisitStatus();
                            if (!StringUtils.hasText(status)) {
                                return false;
                            }
                            // 只返回曾经来访过的：已进场、来访中、已离场、已出场等
                            return status.contains("已进场") || 
                                   status.contains("来访中") || 
                                   status.contains("已离场") ||
                                   status.contains("已出场");
                        })
                        .collect(Collectors.toList());
                    log.info("👤 [访客全部] 所有曾经来访过的访客: {} 人", reservations.size());
                }
            } else {
                // showAll=false：保持原有逻辑，只返回未离场的记录
                if ("vehicle".equals(type)) {
                    // 车辆：只返回有车牌号且车辆已进场（未离场）的记录
                    reservations = reservations.stream()
                        .filter(r -> StringUtils.hasText(r.getCarNumber()))
                        .filter(r -> {
                            String status = r.getCarVisitStatus();
                            // 只返回“已进场”且不包含“已离场”的记录（即当前在场的车辆）
                            return StringUtils.hasText(status) && 
                                   status.contains("已进场") && 
                                   !status.contains("已离场");
                        })
                        .collect(Collectors.toList());
                    log.info("🚗 [车辆过滤] 已进场未离场的车辆: {} 辆", reservations.size());
                } else {
                    // 访客：只返回人已进场（未离场）或来访中的记录
                    reservations = reservations.stream()
                        .filter(r -> {
                            String status = r.getPersonVisitStatus();
                            // 只返回“已进场”且未离场，或“来访中”的记录
                            return StringUtils.hasText(status) && 
                                   ((status.contains("已进场") && !status.contains("已离场")) ||
                                    status.contains("来访中"));
                        })
                        .collect(Collectors.toList());
                    log.info("👤 [访客过滤] 已进场未离场的访客: {} 人", reservations.size());
                }
            }
            
            // 转换为前端需要的格式
            List<Map<String, Object>> detailList = reservations.stream()
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", r.getId());
                    item.put("reservationId", r.getReservationId());
                    item.put("visitorName", r.getVisitorName());
                    item.put("visitorPhone", r.getVisitorPhone());
                    item.put("visitorIdCard", r.getVisitorIdCard());
                    item.put("carNumber", r.getCarNumber());
                    item.put("startTime", r.getStartTime());
                    item.put("endTime", r.getEndTime());
                    item.put("passName", r.getPassName());
                    item.put("passDep", r.getPassDep());
                    item.put("personVisitStatus", r.getPersonVisitStatus());
                    item.put("carVisitStatus", r.getCarVisitStatus());
                    item.put("personVisitTimes", r.getPersonVisitTimes());
                    item.put("carVisitTimes", r.getCarVisitTimes());
                    item.put("vipTypeName", r.getVipTypeName());
                    item.put("applyStateName", r.getApplyStateName());
                    return item;
                })
                .collect(Collectors.toList());
            
            log.info("✅ [计划看板] 返回 {} 条{}记录，类型: {}", detailList.size(), getTimeRangeLabel(timeRange), type);
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", detailList);
            result.put("total", detailList.size());
            result.put("type", type);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [计划看板] 查询详细列表失败", e);
            return Result.error("获取详细列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据时间范围计算开始和结束时间
     * @param timeRange 时间范围：today, week, month, year
     * @return Date数组，[0]是开始时间，[1]是结束时间
     */
    private Date[] getTimeRangeDates(String timeRange) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        LocalDate today = LocalDate.now();
        
        switch (timeRange.toLowerCase()) {
            case "week":
                // 本周：从本周一00:00:00到本周日23:59:59
                startDateTime = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                    .atTime(LocalTime.MAX);
                break;
                
            case "month":
                // 本月：从本月1日00:00:00到本月最后一天23:59:59
                startDateTime = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                break;
                
            case "year":
                // 本年度：从本年1月1日00:00:00到本年12月31日23:59:59
                startDateTime = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
                break;
                
            case "today":
            default:
                // 今日：从今天00:00:00到今天23:59:59
                startDateTime = today.atStartOfDay();
                endDateTime = today.atTime(LocalTime.MAX);
                break;
        }
        
        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
        
        return new Date[]{startDate, endDate};
    }
    
    /**
     * 获取时间范围的标签
     * @param timeRange 时间范围
     * @return 标签文本
     */
    private String getTimeRangeLabel(String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "week":
                return "本周";
            case "month":
                return "本月";
            case "year":
                return "本年度";
            case "today":
            default:
                return "今日";
        }
    }
}
