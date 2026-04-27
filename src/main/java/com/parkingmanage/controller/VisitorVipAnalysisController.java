package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.mapper.ReportCarInMapper;
import com.parkingmanage.mapper.ReportCarOutMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访客与VIP车辆进出统计分析控制器
 * 基于本地数据库提供实时车辆数据统计分析
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/analysis/visitor-vip")
@Api(tags = "访客与VIP车辆进出统计分析")
public class VisitorVipAnalysisController {

    @Resource
    private ReportCarInMapper reportCarInMapper;
    
    @Resource
    private ReportCarOutMapper reportCarOutMapper;

    /**
     * 获取访客与VIP车辆进出统计数据
     * 
     * @param request 请求参数
     * @return 统计数据
     */
    @PostMapping("/statistics")
    @ApiOperation(value = "获取访客与VIP车辆进出统计数据", notes = "根据时间范围和停车场获取统计数据")
    public ResponseEntity<Result<Map<String, Object>>> getVisitorVipStatistics(@RequestBody StatisticsRequest request) {
//        log.info("📊 [访客VIP统计] 开始查询 - 车场: {}, 时间范围: {}", request.getParkName(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [访客VIP统计] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimeRange())) {
                log.warn("⚠️ [访客VIP统计] 时间范围不能为空");
                return ResponseEntity.ok(Result.error("时间范围不能为空"));
            }
            
            // 获取统计数据
            Map<String, Object> statistics = calculateVisitorVipStatistics(
                request.getParkName(), 
                request.getTimeRange(),
                request.getStartTime(),
                request.getEndTime()
            );
            
//            log.info("✅ [访客VIP统计] 查询成功 - 车场: {}", request.getParkName());
            
            return ResponseEntity.ok(Result.success(statistics));
            
        } catch (Exception e) {
            log.error("❌ [访客VIP统计] 查询失败 - 车场: {}", request.getParkName(), e);
            return ResponseEntity.ok(Result.error("查询统计数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定时间点的详细VIP类型统计信息
     * 用于弹窗显示
     * 
     * @param request 请求参数
     * @return 详细统计信息
     */
    @PostMapping("/detail-statistics")
    @ApiOperation(value = "获取指定时间点的详细VIP类型统计", notes = "用于弹窗显示详细的VIP类型分布")
    public ResponseEntity<Result<Map<String, Object>>> getDetailStatistics(@RequestBody DetailStatisticsRequest request) {
        log.info("📈 [详细统计] 开始查询 - 车场: {}, 时间点: {}", request.getParkName(), request.getTimePoint());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [详细统计] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimePoint())) {
                log.warn("⚠️ [详细统计] 时间点不能为空");
                return ResponseEntity.ok(Result.error("时间点不能为空"));
            }
            
            // 获取详细统计数据
            Map<String, Object> detailStats = calculateDetailStatistics(
                request.getParkName(), 
                request.getTimePoint(),
                request.getTimeRange()
            );
            
            log.info("✅ [详细统计] 查询成功 - 车场: {}, 时间点: {}", request.getParkName(), request.getTimePoint());
            
            return ResponseEntity.ok(Result.success(detailStats));
            
        } catch (Exception e) {
            log.error("❌ [详细统计] 查询失败 - 车场: {}, 时间点: {}", request.getParkName(), request.getTimePoint(), e);
            return ResponseEntity.ok(Result.error("查询详细统计失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式获取访客与VIP车辆进出统计数据（简化接口）
     * 
     * @param parkName 车场名称
     * @param timeRange 时间范围
     * @return 统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "GET方式获取访客与VIP车辆进出统计数据", notes = "根据车场名称和时间范围获取统计数据（GET方式）")
    public ResponseEntity<Result<Map<String, Object>>> getVisitorVipStatisticsByGet(
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName,
            @ApiParam(value = "时间范围", required = true) @RequestParam String timeRange) {
        
        StatisticsRequest request = new StatisticsRequest();
        request.setParkName(parkName);
        request.setTimeRange(timeRange);
        
        return getVisitorVipStatistics(request);
    }

    /**
     * 计算访客与VIP车辆进出统计数据
     */
    private Map<String, Object> calculateVisitorVipStatistics(String parkName, String timeRange, 
                                                              String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取时间范围
            TimeRangeInfo timeInfo = getTimeRangeInfo(timeRange, startTime, endTime);
            
            // 使用计算出的时间范围，如果传入的startTime和endTime为null，则使用timeInfo中的值
            String actualStartTime = (startTime != null) ? startTime : timeInfo.getStartTime();
            String actualEndTime = (endTime != null) ? endTime : timeInfo.getEndTime();
            
            // 从本地数据库获取进场车辆数据
//            log.info("📤 [数据库查询] 获取进场车辆数据 - 车场: {}, 时间范围: {} ~ {}", parkName, actualStartTime, actualEndTime);
            QueryWrapper<ReportCarIn> inQueryWrapper = new QueryWrapper<>();
            inQueryWrapper.ge("enter_time", actualStartTime)
                         .le("enter_time", actualEndTime)
                         .eq("deleted", 0)
                         .orderByDesc("enter_time");
            List<ReportCarIn> carInList = reportCarInMapper.selectList(inQueryWrapper);
            
            // 从本地数据库获取离场车辆数据
//            log.info("📤 [数据库查询] 获取离场车辆数据 - 车场: {}, 时间范围: {} ~ {}", parkName, actualStartTime, actualEndTime);
            QueryWrapper<ReportCarOut> outQueryWrapper = new QueryWrapper<>();
            outQueryWrapper.ge("leave_time", actualStartTime)
                          .le("leave_time", actualEndTime)
                          .eq("deleted", 0)
                          .orderByDesc("leave_time");
            List<ReportCarOut> carOutList = reportCarOutMapper.selectList(outQueryWrapper);
            
            // 如果数据为空，返回空结果
            if ((carInList == null || carInList.isEmpty()) && 
                (carOutList == null || carOutList.isEmpty())) {
                log.info("📭 [访客VIP统计] 数据库数据为空，返回空结果");
                result.put("hourlyData", new ArrayList<>());
                result.put("vipTypes", new ArrayList<>());
                result.put("visitorTypes", new ArrayList<>());
                result.put("summary", new HashMap<String, Object>() {{
                    put("totalVisitorEntry", 0);
                    put("totalVisitorExit", 0);
                    put("totalVipEntry", 0);
                    put("totalVipExit", 0);
                }});
                result.put("timeRange", timeRange);
                result.put("parkName", parkName);
                result.put("dataSource", "EMPTY");
                return result;
            }
            
            // 分别处理进场和离场数据
            List<CarInfo> entryCars = convertCarInToCarInfo(carInList);
            List<CarInfo> exitCars = convertCarOutToCarInfo(carOutList);
            
            // 按时间分组统计（分别统计进场和离场）
            Map<String, HourlyStats> hourlyStats = groupByHourSeparated(entryCars, exitCars, timeInfo);
            
            // 分析VIP类型分布（基于enter_custom_vip_name）
            List<VipTypeStats> vipTypes = analyzeVipTypesSeparated(entryCars, exitCars);
            
            // 分析访客类型分布（基于enter_custom_vip_name和leave_custom_vip_name）
            List<VisitorTypeStats> visitorTypes = analyzeVisitorTypesSeparated(entryCars, exitCars);
            
            // 构建返回数据
            result.put("hourlyData", hourlyStats.values().stream()
                .sorted(Comparator.comparing(HourlyStats::getHour))
                .collect(Collectors.toList()));
            result.put("vipTypes", vipTypes);
            result.put("visitorTypes", visitorTypes);
            result.put("summary", calculateSummary(hourlyStats.values()));
            result.put("timeRange", timeRange);
            result.put("parkName", parkName);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [访客VIP统计] 计算统计数据失败", e);
            // 发生异常时返回空结果
            result.put("hourlyData", new ArrayList<>());
            result.put("vipTypes", new ArrayList<>());
            result.put("visitorTypes", new ArrayList<>());
            result.put("summary", new HashMap<String, Object>() {{
                put("totalVisitorEntry", 0);
                put("totalVisitorExit", 0);
                put("totalVipEntry", 0);
                put("totalVipExit", 0);
            }});
            result.put("timeRange", timeRange);
            result.put("error", e.getMessage());
            result.put("dataSource", "ERROR");
            return result;
        }
        
        return result;
    }

    /**
     * 计算详细统计数据（用于弹窗）
     */
    private Map<String, Object> calculateDetailStatistics(String parkName, String timePoint, String timeRange) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("parkName: " + parkName);
            System.out.println("timePoint: " + timePoint);
            System.out.println("timeRange: " + timeRange);
            
            // 基于timePoint与timeRange对齐柱状图颗粒度，计算查询范围
            String[] timeRangeResult = deriveRangeFromTimePoint(timePoint, timeRange);
            String startTime = timeRangeResult[0];
            String endTime = timeRangeResult[1];
            
            log.info("📅 [详细统计] 基于时间点计算范围: {} ~ {}", startTime, endTime);
            
            // 从本地数据库获取进场车辆数据
            QueryWrapper<ReportCarIn> inQueryWrapper = new QueryWrapper<>();
            inQueryWrapper.ge("enter_time", startTime)
                         .le("enter_time", endTime)
                         .eq("deleted", 0);
            List<ReportCarIn> carInList = reportCarInMapper.selectList(inQueryWrapper);
            
            // 从本地数据库获取离场车辆数据
            QueryWrapper<ReportCarOut> outQueryWrapper = new QueryWrapper<>();
            outQueryWrapper.ge("leave_time", startTime)
                          .le("leave_time", endTime)
                          .eq("deleted", 0);
            List<ReportCarOut> carOutList = reportCarOutMapper.selectList(outQueryWrapper);
            
            // 如果数据为空，返回模拟数据
            if ((carInList == null || carInList.isEmpty()) && 
                (carOutList == null || carOutList.isEmpty())) {
                log.info("📭 [详细统计] 数据库数据为空，返回模拟数据");
                // return getMockDetailStatistics(timePoint);
                return null;
            }
            
            // 处理真实数据 - 转换为统一格式
            List<CarInfo> allCars = new ArrayList<>();
            if (carInList != null) allCars.addAll(convertCarInToCarInfo(carInList));
            if (carOutList != null) allCars.addAll(convertCarOutToCarInfo(carOutList));
            
            // 分析VIP类型分布（详细）
            List<VipTypeStats> vipTypes = analyzeVipTypes(allCars);
            
            // 分析访客类型分布（详细）
            List<VisitorTypeStats> visitorTypes = analyzeVisitorTypes(allCars);
            
            // 构建返回数据
            result.put("vipTypes", vipTypes);
            result.put("visitorTypes", visitorTypes);
            result.put("timePoint", timePoint);
            result.put("parkName", parkName);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [详细统计] 计算详细统计数据失败", e);
            // 发生异常时返回模拟数据
            // return getMockDetailStatistics(timePoint);
            return null;
        }
        
        return result;
    }

    /**
     * 按小时分组统计车辆数据（分别处理进场和离场）
     */
    private Map<String, HourlyStats> groupByHourSeparated(List<CarInfo> entryCars, 
                                                          List<CarInfo> exitCars, 
                                                          TimeRangeInfo timeInfo) {
        Map<String, HourlyStats> hourlyStats = new HashMap<>();
        
        // 初始化每小时数据
        for (int i = 0; i < timeInfo.getHourCount(); i++) {
            String hour = timeInfo.getHourLabels().get(i);
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            stats.setVisitorEntry(0);
            stats.setVisitorExit(0);
            stats.setVipEntry(0);
            stats.setVipExit(0);
            hourlyStats.put(hour, stats);
        }
        
        // 统计进场车辆数据
        for (CarInfo car : entryCars) {
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRange(car, timeInfo)) {
                continue;
            }
            
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromEnterStats(car)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getEnterTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                if (isVisitorByEnterCustomVipName(car)) {
                    stats.setVisitorEntry(stats.getVisitorEntry() + 1);
                } else {
                    stats.setVipEntry(stats.getVipEntry() + 1);
                }
            }
        }
        
        // 统计离场车辆数据
        for (CarInfo car : exitCars) {
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRange(car, timeInfo)) {
                continue;
            }
            
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromExitStats(car)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getLeaveTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                if (isVisitorByExitLogic(car)) {
                    stats.setVisitorExit(stats.getVisitorExit() + 1);
                } else {
                    stats.setVipExit(stats.getVipExit() + 1);
                }
            }
        }
        
        return hourlyStats;
    }

    /**
     * 按小时分组统计（不再使用，已被 groupByHourSeparated 替代）
     * 支持自定义时间范围过滤
     * @deprecated 使用 groupByHourSeparated 替代，该方法能正确区分进场和离场
     */
    @Deprecated
    private Map<String, HourlyStats> groupByHour(List<CarInfo> cars, TimeRangeInfo timeInfo) {
        Map<String, HourlyStats> hourlyStats = new HashMap<>();
        
        // 初始化每小时数据
        for (int i = 0; i < timeInfo.getHourCount(); i++) {
            String hour = timeInfo.getHourLabels().get(i);
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            stats.setVisitorEntry(0);
            stats.setVisitorExit(0);
            stats.setVipEntry(0);
            stats.setVipExit(0);
            hourlyStats.put(hour, stats);
        }
        
        // 统计车辆数据
        for (CarInfo car : cars) {
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRange(car, timeInfo)) {
                continue;
            }
            
            // 统计进场数据
            if (car.getEnterTime() != null) {
            String hour = extractHourFromTime(car.getEnterTime(), timeInfo.getTimeRange());
            if (hour != null && hourlyStats.containsKey(hour)) {
                HourlyStats stats = hourlyStats.get(hour);
                
                    if (!shouldExcludeFromEnterStats(car)) {
                        if (isVipByEnterVipType(car)) {
                        stats.setVipEntry(stats.getVipEntry() + 1);
                        } else if (isVisitorByEnterCustomVipName(car)) {
                            stats.setVisitorEntry(stats.getVisitorEntry() + 1);
                        }
                    }
                }
            }
            
            // 统计离场数据
                    if (car.getLeaveTime() != null) {
                String hour = extractHourFromTime(car.getLeaveTime(), timeInfo.getTimeRange());
                if (hour != null && hourlyStats.containsKey(hour)) {
                    HourlyStats stats = hourlyStats.get(hour);
                    
                    if (!shouldExcludeFromExitStats(car)) {
                        if (isVipByLeaveVipType(car)) {
                            stats.setVipExit(stats.getVipExit() + 1);
                        } else if (isVisitorByExitLogic(car)) {
                        stats.setVisitorExit(stats.getVisitorExit() + 1);
                        }
                    }
                }
            }
        }
        
        return hourlyStats;
    }
    
    /**
     * 检查车辆时间是否在指定范围内
     */
    private boolean isCarInTimeRange(CarInfo car, TimeRangeInfo timeInfo) {
        // 如果没有自定义时间范围，返回true
        if (!StringUtils.hasText(timeInfo.getStartTime()) || !StringUtils.hasText(timeInfo.getEndTime())) {
            return true;
        }
        
        try {
            LocalDateTime startTime = LocalDateTime.parse(timeInfo.getStartTime(), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime endTime = LocalDateTime.parse(timeInfo.getEndTime(), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // 检查进场时间
            if (StringUtils.hasText(car.getEnterTime())) {
                LocalDateTime enterTime = LocalDateTime.parse(car.getEnterTime(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (enterTime.isAfter(startTime) && enterTime.isBefore(endTime)) {
                    return true;
                }
            }
            
            // 检查离场时间
            if (StringUtils.hasText(car.getLeaveTime())) {
                LocalDateTime leaveTime = LocalDateTime.parse(car.getLeaveTime(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (leaveTime.isAfter(startTime) && leaveTime.isBefore(endTime)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("⚠️ 解析车辆时间失败: {}", e.getMessage());
            return true; // 解析失败时不过滤
        }
    }

    /**
     * 分析VIP类型分布（分别处理进场和离场）
     */
    private List<VipTypeStats> analyzeVipTypesSeparated(List<CarInfo> entryCars, 
                                                       List<CarInfo> exitCars) {
        Map<String, VipTypeStats> vipTypeMap = new HashMap<>();
        
//        log.info("📊 [VIP分析] 开始分析VIP类型 - 进场车辆数: {}, 出场车辆数: {}", entryCars.size(), exitCars.size());
        
        // 处理进场VIP数据（基于enter_vip_type判断，使用enter_custom_vip_name分组）
        for (CarInfo car : entryCars) {
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromEnterStats(car)) {
                continue;
            }
            
            if (!isVisitorByEnterCustomVipName(car)) {
                // 直接使用enter_custom_vip_name作为VIP类型
                String vipType = car.getEnterCustomVipName();
                if (StringUtils.hasText(vipType)) { // 只统计有VIP名称的
                    VipTypeStats stats = vipTypeMap.computeIfAbsent(vipType, k -> {
                        VipTypeStats s = new VipTypeStats();
                        s.setName(k);
                        s.setEntry(0);
                        s.setExit(0);
                        return s;
                    });
                    stats.setEntry(stats.getEntry() + 1);
                    log.debug("  ✅ 进场VIP: {} (enter_vip_type={}, enter_custom_vip_name={})", 
                             car.getCarLicenseNumber(), car.getEnterVipType(), vipType);
                }
            }
        }
        
        // 处理离场VIP数据（基于leave_vip_type判断，使用leave_custom_vip_name分组）
        for (CarInfo car : exitCars) {
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromExitStats(car)) {
                continue;
            }
            
            if (!isVisitorByExitLogic(car)) {
                // 直接使用leave_custom_vip_name作为VIP类型
                String vipType = car.getLeaveCustomVipName();
                if (StringUtils.hasText(vipType)) { // 只统计有VIP名称的
                    VipTypeStats stats = vipTypeMap.computeIfAbsent(vipType, k -> {
                        VipTypeStats s = new VipTypeStats();
                        s.setName(k);
                        s.setEntry(0);
                        s.setExit(0);
                        return s;
                    });
                    stats.setExit(stats.getExit() + 1);
                    log.debug("  ✅ 离场VIP: {} (leave_vip_type={}, leave_custom_vip_name={})", 
                             car.getCarLicenseNumber(), car.getLeaveVipType(), vipType);
                }
            }
        }
        
        log.info("📊 [VIP分析] VIP类型统计完成 - 共{}种类型", vipTypeMap.size());
        
        return vipTypeMap.values().stream()
            .filter(stats -> stats.getEntry() > 0 || stats.getExit() > 0)
            .sorted(Comparator.comparing(VipTypeStats::getName))
            .collect(Collectors.toList());
    }

    /**
     * 分析访客类型分布（分别处理进场和离场）
     */
    private List<VisitorTypeStats> analyzeVisitorTypesSeparated(List<CarInfo> entryCars, 
                                                               List<CarInfo> exitCars) {
        Map<String, VisitorTypeStats> visitorTypeMap = new HashMap<>();
        
        log.info("👥 [访客分析] 开始分析访客类型 - 进场车辆数: {}, 出场车辆数: {}", entryCars.size(), exitCars.size());
        
        // 处理进场访客数据（基于enter_vip_type判断，使用enter_custom_vip_name分组）
        for (CarInfo car : entryCars) {
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromEnterStats(car)) {
                continue;
            }
            
            if (isVisitorByEnterCustomVipName(car)) {
                // 直接使用enter_custom_vip_name作为访客类型
                String visitorType = car.getEnterCustomVipName();
                if (StringUtils.hasText(visitorType)) { // 只统计有访客名称的
                    VisitorTypeStats stats = visitorTypeMap.computeIfAbsent(visitorType, k -> {
                        VisitorTypeStats s = new VisitorTypeStats();
                        s.setName(k);
                        s.setEntry(0);
                        s.setExit(0);
                        return s;
                    });
                    stats.setEntry(stats.getEntry() + 1);
                    log.debug("  ✅ 进场访客: {} (enter_vip_type={}, type={})", 
                             car.getCarLicenseNumber(), car.getEnterVipType(), visitorType);
                }
            }
        }
        
        // 处理离场访客数据（基于leave_vip_type判断，使用leave_custom_vip_name分组）
        for (CarInfo car : exitCars) {
            // 排除临时车和未定义(警车)
            if (shouldExcludeFromExitStats(car)) {
                continue;
            }
            
            if (isVisitorByExitLogic(car)) {
                // 直接使用leave_custom_vip_name作为访客类型
                String visitorType = car.getLeaveCustomVipName();
                if (StringUtils.hasText(visitorType)) { // 只统计有访客名称的
                    VisitorTypeStats stats = visitorTypeMap.computeIfAbsent(visitorType, k -> {
                        VisitorTypeStats s = new VisitorTypeStats();
                        s.setName(k);
                        s.setEntry(0);
                        s.setExit(0);
                        return s;
                    });
                    stats.setExit(stats.getExit() + 1);
                    log.debug("  ✅ 离场访客: {} (leave_vip_type={}, leave_custom_vip_name={}, type={})", 
                             car.getCarLicenseNumber(), car.getLeaveVipType(), 
                             car.getLeaveCustomVipName(), visitorType);
                }
            }
        }
        
        log.info("👥 [访客分析] 访客类型统计完成 - 共{}种类型", visitorTypeMap.size());
        
        return visitorTypeMap.values().stream()
            .filter(stats -> stats.getEntry() > 0 || stats.getExit() > 0)
            .sorted(Comparator.comparing(VisitorTypeStats::getName))
            .collect(Collectors.toList());
    }

    /**
     * 分析VIP类型分布（基于 enter_vip_type 和 leave_vip_type 判断）
     */
    private List<VipTypeStats> analyzeVipTypes(List<CarInfo> cars) {
        Map<String, VipTypeStats> vipTypeMap = new HashMap<>();
        
        for (CarInfo car : cars) {
            // 进场VIP统计（基于 enter_vip_type = "本地VIP"）
            if (car.getEnterTime() != null && isVipByEnterVipType(car)) {
                if (!shouldExcludeFromEnterStats(car)) {
                    String vipType = car.getEnterCustomVipName();
                    if (StringUtils.hasText(vipType)) {
                VipTypeStats stats = vipTypeMap.computeIfAbsent(vipType, k -> {
                    VipTypeStats s = new VipTypeStats();
                    s.setName(k);
                    s.setEntry(0);
                    s.setExit(0);
                    return s;
                });
                        stats.setEntry(stats.getEntry() + 1);
                    }
                }
            }
            
            // 离场VIP统计（基于 leave_vip_type = "本地VIP"）
            if (car.getLeaveTime() != null && isVipByLeaveVipType(car)) {
                if (!shouldExcludeFromExitStats(car)) {
                    String vipType = car.getLeaveCustomVipName();
                    if (StringUtils.hasText(vipType)) {
                        VipTypeStats stats = vipTypeMap.computeIfAbsent(vipType, k -> {
                            VipTypeStats s = new VipTypeStats();
                            s.setName(k);
                            s.setEntry(0);
                            s.setExit(0);
                            return s;
                        });
                    stats.setExit(stats.getExit() + 1);
                    }
                }
            }
        }
        
        return vipTypeMap.values().stream()
            .peek(stats -> stats.setValue(stats.getEntry() + stats.getExit()))
            .filter(stats -> stats.getValue() > 0)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * 分析访客类型分布（基于vip_type字段判断）
     */
    private List<VisitorTypeStats> analyzeVisitorTypes(List<CarInfo> cars) {
        Map<String, VisitorTypeStats> visitorTypeMap = new HashMap<>();
        
        for (CarInfo car : cars) {
            // 判断是否为访客（基于vip_type字段）
            boolean isVisitorEntry = isVisitorByEnterCustomVipName(car);
            boolean isVisitorExit = isVisitorByExitLogic(car);
            
            // 进场访客统计
            if (isVisitorEntry && car.getEnterTime() != null) {
                if (!shouldExcludeFromEnterStats(car)) {
                    // 直接使用enter_custom_vip_name作为访客类型
                    String visitorType = car.getEnterCustomVipName();
                    if (StringUtils.hasText(visitorType)) {
                    VisitorTypeStats stats = visitorTypeMap.computeIfAbsent(visitorType, k -> {
                        VisitorTypeStats s = new VisitorTypeStats();
                        s.setName(k);
                        s.setEntry(0);
                        s.setExit(0);
                        return s;
                    });
                        stats.setEntry(stats.getEntry() + 1);
                    }
                }
            }
            
            // 离场访客统计
            if (isVisitorExit && car.getLeaveTime() != null) {
                if (!shouldExcludeFromExitStats(car)) {
                    // 直接使用leave_custom_vip_name作为访客类型
                    String visitorType = car.getLeaveCustomVipName();
                    if (StringUtils.hasText(visitorType)) {
                        VisitorTypeStats stats = visitorTypeMap.computeIfAbsent(visitorType, k -> {
                            VisitorTypeStats s = new VisitorTypeStats();
                            s.setName(k);
                            s.setEntry(0);
                            s.setExit(0);
                            return s;
                        });
                        stats.setExit(stats.getExit() + 1);
                    }
                }
            }
        }
        
        return visitorTypeMap.values().stream()
            .peek(stats -> stats.setValue(stats.getEntry() + stats.getExit()))
            .filter(stats -> stats.getValue() > 0)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * 判断进场车辆是否为访客（基于enter_vip_type）
     * 实际数据值：访客、本地VIP、临时车(不统计)、未定义(警车,不统计)
     */
    private boolean isVisitorByEnterCustomVipName(CarInfo car) {
        String enterVipType = car.getEnterVipType();
        
        // 只有"访客"才算访客
        return "访客".equals(enterVipType);
    }
    
    /**
     * 判断进场车辆是否为VIP（基于enter_vip_type）
     */
    private boolean isVipByEnterVipType(CarInfo car) {
        String enterVipType = car.getEnterVipType();
        
        // 只有"本地VIP"才算VIP
        return "本地VIP".equals(enterVipType);
    }
    
    /**
     * 判断是否需要排除进场统计（临时车、未定义、警车等）
     */
    private boolean shouldExcludeFromEnterStats(CarInfo car) {
        String enterVipType = car.getEnterVipType();
        
        // 排除：临时车、未定义(警车)、空值
        return !StringUtils.hasText(enterVipType) || 
               "临时车".equals(enterVipType) || 
               "未定义".equals(enterVipType);
    }

    /**
     * 判断离场车辆是否为访客（基于leave_vip_type）
     * 实际数据值：访客、本地VIP、临时车(不统计)、未定义(警车,不统计)
     */
    private boolean isVisitorByExitLogic(CarInfo car) {
        String leaveVipType = car.getLeaveVipType();
        
        // 只有"访客"才算访客
        return "访客".equals(leaveVipType);
    }
    
    /**
     * 判断离场车辆是否为VIP（基于leave_vip_type）
     */
    private boolean isVipByLeaveVipType(CarInfo car) {
        String leaveVipType = car.getLeaveVipType();
        
        // 只有"本地VIP"才算VIP
        return "本地VIP".equals(leaveVipType);
    }
    
    /**
     * 判断是否需要排除离场统计（临时车、未定义、警车等）
     */
    private boolean shouldExcludeFromExitStats(CarInfo car) {
        String leaveVipType = car.getLeaveVipType();
        
        // 排除：临时车、未定义(警车)、空值
        return !StringUtils.hasText(leaveVipType) || 
               "临时车".equals(leaveVipType) || 
               "未定义".equals(leaveVipType);
    }

    /**
     * 从时间字符串中提取小时
     */
    private String extractHourFromTime(String timeStr, String timeRange) {
        if (!StringUtils.hasText(timeStr)) return null;
        
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            switch (timeRange) {
                case "daily":
                    return String.format("%02d:00", dateTime.getHour());
                case "weekly":
                    return getWeekDay(dateTime.getDayOfWeek().getValue());
                case "monthly":
                    return String.format("%d日", dateTime.getDayOfMonth());
                case "yearly":
                    return String.format("%d月", dateTime.getMonthValue());
                default:
                    return String.format("%02d:00", dateTime.getHour());
            }
        } catch (Exception e) {
            log.warn("⚠️ 解析时间失败: {}", timeStr);
            return null;
        }
    }

    /**
     * 获取星期几
     */
    private String getWeekDay(int dayOfWeek) {
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekDays[dayOfWeek - 1];
    }

    /**
     * 基于点击的时间点与时间范围，推导查询开始/结束时间
     * 目标：与柱状图颗粒度保持一致
     * daily: HH(:mm) -> 当天该小时 00-59:59
     * weekly: 周X -> 本周对应一天 00:00:00-23:59:59
     * monthly: N日 -> 本月对应日期 00:00:00-23:59:59
     * yearly: N月 -> 对应月份整月 00:00:00-当月末 23:59:59
     */
    private String[] deriveRangeFromTimePoint(String timePoint, String timeRange) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        try {
            // 如果是完整的日期时间，按小时对齐
            if (timePoint != null && timePoint.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                LocalDateTime dt = LocalDateTime.parse(timePoint, dtf);
                LocalDateTime start = dt.withMinute(0).withSecond(0);
                LocalDateTime end = dt.withMinute(59).withSecond(59);
                return new String[]{ start.format(dtf), end.format(dtf) };
            }
        } catch (Exception ignored) { }

        try {
            switch (timeRange) {
                case "daily": {
                    // 期望如 08:00 或 8:00 或 08
                    int hour = 0;
                    if (timePoint != null) {
                        String h = timePoint.replace("点", ":");
                        if (h.contains(":")) {
                            h = h.split(":")[0];
                        }
                        hour = Integer.parseInt(h);
                    }
                    LocalDateTime start = now.withHour(hour).withMinute(0).withSecond(0);
                    LocalDateTime end = now.withHour(hour).withMinute(59).withSecond(59);
                    return new String[]{ start.format(dtf), end.format(dtf) };
                }
                case "weekly": {
                    // 周一..周日
                    DayOfWeek target;
                    if (timePoint != null && timePoint.startsWith("周")) {
                        String c = timePoint.substring(1);
                        int idx = Arrays.asList("一","二","三","四","五","六","日").indexOf(c);
                        if (idx >= 0) {
                            target = DayOfWeek.of(idx + 1);
                        } else {
                            target = now.getDayOfWeek();
                        }
                    } else {
                        target = now.getDayOfWeek();
                    }
                    LocalDateTime startOfWeek = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
                    LocalDateTime start = startOfWeek.with(target).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime end = start.withHour(23).withMinute(59).withSecond(59);
                    return new String[]{ start.format(dtf), end.format(dtf) };
                }
                case "monthly": {
                    // N日 -> 当月第N天
                    int day = now.getDayOfMonth();
                    if (timePoint != null) {
                        String d = timePoint.replace("日", "").trim();
                        day = Integer.parseInt(d);
                    }
                    LocalDateTime start = now.withDayOfMonth(day).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime end = start.withHour(23).withMinute(59).withSecond(59);
                    return new String[]{ start.format(dtf), end.format(dtf) };
                }
                case "yearly": {
                    // N月 -> 当年第N月
                    int month = now.getMonthValue();
                    if (timePoint != null) {
                        String m = timePoint.replace("月", "").trim();
                        month = Integer.parseInt(m);
                    }
                    LocalDateTime start = now.withMonth(month).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime end = start.withDayOfMonth(start.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
                    return new String[]{ start.format(dtf), end.format(dtf) };
                }
                default: {
                    // 回退到今天全量
                    LocalDateTime start = now.toLocalDate().atStartOfDay();
                    LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
                    return new String[]{ start.format(dtf), end.format(dtf) };
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 解析timePoint/timeRange失败，使用今天整日: {}", e.getMessage());
            LocalDateTime start = now.toLocalDate().atStartOfDay();
            LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
            return new String[]{ start.format(dtf), end.format(dtf) };
        }
    }

    /**
     * 获取时间范围信息
     * 支持自定义日期范围
     */
    private TimeRangeInfo getTimeRangeInfo(String timeRange, String startTime, String endTime) {
        TimeRangeInfo info = new TimeRangeInfo();
        info.setTimeRange(timeRange);
        
        List<String> hourLabels = new ArrayList<>();
        int hourCount = 0;
        
        // 如果提供了自定义的开始和结束时间，使用自定义范围
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // 验证格式
                
                // 根据时间范围类型生成标签
                switch (timeRange) {
                    case "daily":
                        hourCount = 24;
                        for (int i = 0; i < 24; i++) {
                            hourLabels.add(String.format("%02d:00", i));
                        }
                        break;
                    case "weekly":
                        hourCount = 7;
                        hourLabels = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
                        break;
                    case "monthly":
                        // 计算月份中的天数
                        int daysInMonth = start.toLocalDate().lengthOfMonth();
                        hourCount = daysInMonth;
                        for (int i = 1; i <= daysInMonth; i++) {
                            hourLabels.add(String.format("%d日", i));
                        }
                        break;
                    case "yearly":
                        hourCount = 12;
                        for (int i = 1; i <= 12; i++) {
                            hourLabels.add(String.format("%d月", i));
                        }
                        break;
                    default:
                        hourCount = 24;
                        for (int i = 0; i < 24; i++) {
                            hourLabels.add(String.format("%02d:00", i));
                        }
                }
                
                info.setStartTime(startTime);
                info.setEndTime(endTime);
                
            } catch (Exception e) {
                log.warn("⚠️ 解析自定义时间范围失败，使用默认范围: {}", e.getMessage());
                // 解析失败时使用默认逻辑
                return getDefaultTimeRangeInfo(timeRange);
            }
        } else {
            // 使用默认时间范围逻辑
            return getDefaultTimeRangeInfo(timeRange);
        }
        
        info.setHourCount(hourCount);
        info.setHourLabels(hourLabels);
        
        return info;
    }
    
    /**
     * 获取默认时间范围信息
     */
    private TimeRangeInfo getDefaultTimeRangeInfo(String timeRange) {
        TimeRangeInfo info = new TimeRangeInfo();
        info.setTimeRange(timeRange);
        
        List<String> hourLabels = new ArrayList<>();
        int hourCount = 0;
        
        switch (timeRange) {
            case "daily":
                hourCount = 24;
                for (int i = 0; i < 24; i++) {
                    hourLabels.add(String.format("%02d:00", i));
                }
                break;
            case "weekly":
                hourCount = 7;
                hourLabels = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
                break;
            case "monthly":
                LocalDateTime now = LocalDateTime.now();
                hourCount = now.toLocalDate().lengthOfMonth();
                for (int i = 1; i <= hourCount; i++) {
                    hourLabels.add(String.format("%d日", i));
                }
                break;
            case "yearly":
                hourCount = 12;
                for (int i = 1; i <= 12; i++) {
                    hourLabels.add(String.format("%d月", i));
                }
                break;
            default:
                hourCount = 24;
                for (int i = 0; i < 24; i++) {
                    hourLabels.add(String.format("%02d:00", i));
                }
        }
        
        info.setHourCount(hourCount);
        info.setHourLabels(hourLabels);
        
        // 根据时间范围类型设置实际的时间范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end;
        
        switch (timeRange) {
            case "daily":
                // 今天 00:00:00 到 23:59:59
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "weekly":
                // 本周一到周日
                start = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
                end = now.toLocalDate().with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
                break;
            case "monthly":
                // 本月1日到月末
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);
                break;
            case "yearly":
                // 今年1月1日到12月31日
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now.toLocalDate().withDayOfYear(now.toLocalDate().lengthOfYear()).atTime(23, 59, 59);
                break;
            default:
                // 默认今天
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
        }
        
        info.setStartTime(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.setEndTime(end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return info;
    }

    /**
     * 计算汇总统计
     */
    private Map<String, Object> calculateSummary(Collection<HourlyStats> hourlyStats) {
        Map<String, Object> summary = new HashMap<>();
        
        int totalVisitorEntry = 0;
        int totalVisitorExit = 0;
        int totalVipEntry = 0;
        int totalVipExit = 0;
        
        for (HourlyStats stats : hourlyStats) {
            totalVisitorEntry += stats.getVisitorEntry();
            totalVisitorExit += stats.getVisitorExit();
            totalVipEntry += stats.getVipEntry();
            totalVipExit += stats.getVipExit();
        }
        
        summary.put("totalVisitorEntry", totalVisitorEntry);
        summary.put("totalVisitorExit", totalVisitorExit);
        summary.put("totalVipEntry", totalVipEntry);
        summary.put("totalVipExit", totalVipExit);
        summary.put("visitorNetFlow", totalVisitorEntry - totalVisitorExit);
        summary.put("vipNetFlow", totalVipEntry - totalVipExit);
        
        return summary;
    }

    // ==================== 请求参数对象 ====================

    /**
     * 统计数据请求
     */
    @Data
    public static class StatisticsRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
        
        @ApiParam(value = "时间范围", required = true)
        private String timeRange;
        
        @ApiParam(value = "开始时间", required = false)
        private String startTime;
        
        @ApiParam(value = "结束时间", required = false)
        private String endTime;
    }

    /**
     * 详细统计数据请求
     */
    @Data
    public static class DetailStatisticsRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
        
        @ApiParam(value = "时间点", required = true)
        private String timePoint;
        
        @ApiParam(value = "时间范围", required = false)
        private String timeRange;
    }

    // ==================== 数据对象 ====================

    /**
     * 时间范围信息
     */
    @Data
    public static class TimeRangeInfo {
        private String timeRange;
        private int hourCount;
        private List<String> hourLabels;
        private String startTime;
        private String endTime;
    }

    /**
     * 每小时统计数据
     */
    @Data
    public static class HourlyStats {
        private String hour;
        private int visitorEntry;
        private int visitorExit;
        private int vipEntry;
        private int vipExit;
    }

    /**
     * VIP类型统计数据
     */
    @Data
    public static class VipTypeStats {
        private String name;
        private int value;
        private int entry;
        private int exit;
    }

    /**
     * 访客类型统计数据
     */
    @Data
    public static class VisitorTypeStats {
        private String name;
        private int value;
        private int entry;
        private int exit;
    }

    /**
     * 车辆信息统一数据对象
     * 用于统一进场和离场记录的数据格式
     */
    @Data
    public static class CarInfo {
        private String carLicenseNumber;
        private String enterTime;
        private String leaveTime;
        private String enterType;
        private String enterVipType;
        private String leaveVipType;
        private String enterCustomVipName;
        private String leaveCustomVipName;
        private String enterChannelName;
        private String leaveChannelName;
        private String enterCarType;
        private String leaveCarType;
        private String enterCarLicenseColor;
        private String leaveCarLicenseColor;
        private String amountReceivable;
        private String stoppingTime;
    }

    /**
     * 将进场记录列表转换为CarInfo列表
     */
    private List<CarInfo> convertCarInToCarInfo(List<ReportCarIn> carInList) {
        if (carInList == null) {
            return new ArrayList<>();
        }
        
        return carInList.stream().map(carIn -> {
            CarInfo carInfo = new CarInfo();
            carInfo.setCarLicenseNumber(carIn.getCarLicenseNumber());
            carInfo.setEnterTime(carIn.getEnterTime());
            carInfo.setLeaveTime(null); // 进场记录没有离场时间
            carInfo.setEnterType(carIn.getEnterType());
            carInfo.setEnterVipType(carIn.getEnterVipType());
            carInfo.setEnterCustomVipName(carIn.getEnterCustomVipName());
            carInfo.setEnterChannelName(carIn.getEnterChannelName());
            carInfo.setEnterCarType(carIn.getEnterCarType());
            carInfo.setEnterCarLicenseColor(carIn.getEnterCarLicenseColor());
            return carInfo;
        }).collect(Collectors.toList());
    }

    /**
     * 将离场记录列表转换为CarInfo列表
     */
    private List<CarInfo> convertCarOutToCarInfo(List<ReportCarOut> carOutList) {
        if (carOutList == null) {
            return new ArrayList<>();
        }
        
        return carOutList.stream().map(carOut -> {
            CarInfo carInfo = new CarInfo();
            carInfo.setCarLicenseNumber(carOut.getCarLicenseNumber());
            carInfo.setEnterTime(carOut.getEnterTime());
            carInfo.setLeaveTime(carOut.getLeaveTime());
            carInfo.setEnterType(carOut.getEnterType());
            carInfo.setEnterVipType(carOut.getEnterVipType());
            carInfo.setLeaveVipType(carOut.getLeaveVipType());
            carInfo.setLeaveCustomVipName(carOut.getLeaveCustomVipName());
            // 注意：ReportCarOut不再有enterCustomVipName字段
            carInfo.setEnterCustomVipName(null); 
            carInfo.setEnterChannelName(carOut.getEnterChannelName());
            carInfo.setLeaveChannelName(carOut.getLeaveChannelName());
            carInfo.setEnterCarType(carOut.getEnterCarType());
            carInfo.setLeaveCarType(carOut.getLeaveCarType());
            carInfo.setEnterCarLicenseColor(carOut.getEnterCarLicenseColor());
            carInfo.setLeaveCarLicenseColor(carOut.getLeaveCarLicenseColor());
            carInfo.setAmountReceivable(carOut.getAmountReceivable());
            carInfo.setStoppingTime(carOut.getStoppingTime());
            return carInfo;
        }).collect(Collectors.toList());
    }
}
