package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.PaymentRecord;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.mapper.PaymentRecordMapper;
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
 * 进出口车辆统计分析控制器
 * 基于本地数据库提供车辆进出数据统计分析
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/analysis/vehicle-flow")
@Api(tags = "进出口车辆统计分析")
public class VehicleFlowAnalysisController {

    @Resource
    private ReportCarInMapper reportCarInMapper;
    
    @Resource
    private ReportCarOutMapper reportCarOutMapper;
    
    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    /**
     * 获取进出口车辆统计数据
     * 
     * @param request 请求参数
     * @return 统计数据
     */
    @PostMapping("/statistics")
    @ApiOperation(value = "获取进出口车辆统计数据", notes = "根据时间范围和停车场获取车辆进出统计数据")
    public ResponseEntity<Result<Map<String, Object>>> getVehicleFlowStatistics(@RequestBody VehicleFlowStatisticsRequest request) {
        log.info("📊 [进出口车辆统计] 开始查询 - 车场: {}, 时间范围: {}", request.getParkName(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
//                log.warn("⚠️ [进出口车辆统计] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimeRange())) {
//                log.warn("⚠️ [进出口车辆统计] 时间范围不能为空");
                return ResponseEntity.ok(Result.error("时间范围不能为空"));
            }
            
            // 获取统计数据
            Map<String, Object> statistics = calculateVehicleFlowStatistics(
                request.getParkName(), 
                request.getTimeRange(),
                request.getStartTime(),
                request.getEndTime()
            );
            
//            log.info("✅ [进出口车辆统计] 查询成功 - 车场: {}", request.getParkName());
            
            return ResponseEntity.ok(Result.success(statistics));
            
        } catch (Exception e) {
            log.error("❌ [进出口车辆统计] 查询失败 - 车场: {}", request.getParkName(), e);
            return ResponseEntity.ok(Result.error("查询统计数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定时间点的详细通道统计信息
     * 用于弹窗显示
     * 
     * @param request 请求参数
     * @return 详细统计信息
     */
    @PostMapping("/detail-statistics")
    @ApiOperation(value = "获取指定时间点的详细通道统计", notes = "用于弹窗显示详细的通道分布")
    public ResponseEntity<Result<Map<String, Object>>> getDetailStatistics(@RequestBody DetailStatisticsRequest request) {
//        log.info("📈 [详细统计] 开始查询 - 车场: {}, 时间点: {}", request.getParkName(), request.getTimePoint());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
//                log.warn("⚠️ [详细统计] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimePoint())) {
//                log.warn("⚠️ [详细统计] 时间点不能为空");
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
     * 获取通道详情统计数据
     * 用于弹窗显示通道分布、流量趋势和对比分析
     * 
     * @param request 请求参数
     * @return 通道详情统计信息
     */
    @PostMapping("/channel-detail-statistics")
    @ApiOperation(value = "获取通道详情统计数据", notes = "用于弹窗显示通道分布、流量趋势和对比分析")
    public ResponseEntity<Result<Map<String, Object>>> getChannelDetailStatistics(@RequestBody ChannelDetailStatisticsRequest request) {
        log.info("📊 [通道详情统计] 开始查询 - 车场: {}, 通道类型: {}, 时间范围: {}", 
                request.getParkName(), request.getChannelType(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [通道详情统计] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getChannelType())) {
                log.warn("⚠️ [通道详情统计] 通道类型不能为空");
                return ResponseEntity.ok(Result.error("通道类型不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimeRange())) {
                log.warn("⚠️ [通道详情统计] 时间范围不能为空");
                return ResponseEntity.ok(Result.error("时间范围不能为空"));
            }
            
            // 获取通道详情统计数据
            Map<String, Object> channelDetailStats = calculateChannelDetailStatistics(
                request.getParkName(), 
                request.getChannelType(),
                request.getTimeRange(),
                request.getStartTime(),
                request.getEndTime()
            );
            
            log.info("✅ [通道详情统计] 查询成功 - 车场: {}, 通道类型: {}", request.getParkName(), request.getChannelType());
            
            return ResponseEntity.ok(Result.success(channelDetailStats));
            
        } catch (Exception e) {
            log.error("❌ [通道详情统计] 查询失败 - 车场: {}, 通道类型: {}", request.getParkName(), request.getChannelType(), e);
            return ResponseEntity.ok(Result.error("查询通道详情统计失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式获取进出口车辆统计数据（简化接口）
     * 
     * @param parkName 车场名称
     * @param timeRange 时间范围
     * @return 统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "GET方式获取进出口车辆统计数据", notes = "根据车场名称和时间范围获取统计数据（GET方式）")
    public ResponseEntity<Result<Map<String, Object>>> getVehicleFlowStatisticsByGet(
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName,
            @ApiParam(value = "时间范围", required = true) @RequestParam String timeRange) {
        
        VehicleFlowStatisticsRequest request = new VehicleFlowStatisticsRequest();
        request.setParkName(parkName);
        request.setTimeRange(timeRange);
        
        return getVehicleFlowStatistics(request);
    }

    /**
     * 计算进出口车辆统计数据
     */
    private Map<String, Object> calculateVehicleFlowStatistics(String parkName, String timeRange, 
                                                              String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取时间范围
            TimeRangeInfo timeInfo = getTimeRangeInfo(timeRange, startTime, endTime);
            
            // 使用计算出的时间范围，如果传入的startTime和endTime为null，则使用timeInfo中的值
            String actualStartTime = (startTime != null) ? startTime : timeInfo.getStartTime();
            String actualEndTime = (endTime != null) ? endTime : timeInfo.getEndTime();
            
            log.info("📅 [时间范围] 原始参数: startTime={}, endTime={}", startTime, endTime);
            log.info("📅 [时间范围] 实际使用: startTime={}, endTime={}", actualStartTime, actualEndTime);
            
            // 从本地数据库获取进场车辆数据
            log.info("📤 [数据库查询] 获取进场车辆数据 - 车场: {}, 时间范围: {} ~ {}", parkName, actualStartTime, actualEndTime);
            QueryWrapper<ReportCarIn> inQueryWrapper = new QueryWrapper<>();
            inQueryWrapper.ge("enter_time", actualStartTime)
                         .le("enter_time", actualEndTime)
                         .eq("deleted", 0)
                         .orderByDesc("enter_time");
            List<ReportCarIn> carInList = reportCarInMapper.selectList(inQueryWrapper);
            
            // 从本地数据库获取离场车辆数据
            log.info("📤 [数据库查询] 获取离场车辆数据 - 车场: {}, 时间范围: {} ~ {}", parkName, actualStartTime, actualEndTime);
            QueryWrapper<ReportCarOut> outQueryWrapper = new QueryWrapper<>();
            outQueryWrapper.ge("leave_time", actualStartTime)
                          .le("leave_time", actualEndTime)
                          .eq("deleted", 0)
                          .orderByDesc("leave_time");
            List<ReportCarOut> carOutList = reportCarOutMapper.selectList(outQueryWrapper);
            
            // 如果数据库数据为空，返回模拟数据
            if ((carInList == null || carInList.isEmpty()) && 
                (carOutList == null || carOutList.isEmpty())) {
                log.info("📭 [进出口车辆统计] 数据库数据为空，返回模拟数据");
                return getMockStatistics(timeRange);
            }
            
            // 分别处理进场和离场数据
            List<ReportCarIn> entryCars = carInList != null ? carInList : new ArrayList<>();
            List<ReportCarOut> exitCars = carOutList != null ? carOutList : new ArrayList<>();
            
            log.info("✅ [数据库查询] 进场数据: {} 条, 离场数据: {} 条", entryCars.size(), exitCars.size());
            
            // 按时间分组统计（分别统计进场和离场）
            Map<String, HourlyStats> hourlyStats = groupByHourSeparatedFromDB(entryCars, exitCars, timeInfo);
            
            // 分析通道分布（基于channel字段）
            List<ChannelStats> channelStats = analyzeChannelDistributionFromDB(entryCars, exitCars);
            
            // 分析车辆类型分布
            List<VehicleTypeStats> vehicleTypeStats = analyzeVehicleTypeDistributionFromDB(entryCars, exitCars);
            
            // 构建返回数据
            result.put("hourlyData", hourlyStats.values().stream()
                .sorted(Comparator.comparing(HourlyStats::getHour))
                .collect(Collectors.toList()));
            result.put("channelStats", channelStats);
            result.put("vehicleTypeStats", vehicleTypeStats);
            result.put("summary", calculateSummary(hourlyStats.values()));
            result.put("timeRange", timeRange);
            result.put("parkName", parkName);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [进出口车辆统计] 计算统计数据失败", e);
            // 发生异常时返回模拟数据
            return getMockStatistics(timeRange);
        }
        
        return result;
    }

    /**
     * 计算详细统计数据（用于弹窗）
     */
    private Map<String, Object> calculateDetailStatistics(String parkName, String timePoint, String timeRange) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("📊 [详细统计] parkName: {}, timePoint: {}, timeRange: {}", parkName, timePoint, timeRange);
            
            // 基于timePoint与timeRange对齐柱状图颗粒度，计算查询范围
            String[] timeRangeResult = deriveRangeFromTimePoint(timePoint, timeRange);
            String startTime = timeRangeResult[0];
            String endTime = timeRangeResult[1];
            
            log.info("📅 [详细统计] 基于时间点计算范围: {} ~ {}", startTime, endTime);
            
            // 从本地数据库获取进场车辆数据
            QueryWrapper<ReportCarIn> inQueryWrapper = new QueryWrapper<>();
            inQueryWrapper.ge("enter_time", startTime)
                         .le("enter_time", endTime)
                         .eq("deleted", 0)
                         .orderByDesc("enter_time");
            List<ReportCarIn> carInList = reportCarInMapper.selectList(inQueryWrapper);
            
            // 从本地数据库获取离场车辆数据
            QueryWrapper<ReportCarOut> outQueryWrapper = new QueryWrapper<>();
            outQueryWrapper.ge("leave_time", startTime)
                          .le("leave_time", endTime)
                          .eq("deleted", 0)
                          .orderByDesc("leave_time");
            List<ReportCarOut> carOutList = reportCarOutMapper.selectList(outQueryWrapper);
            
            // 如果数据库数据为空，返回模拟数据
            if ((carInList == null || carInList.isEmpty()) && 
                (carOutList == null || carOutList.isEmpty())) {
                log.info("📭 [详细统计] 数据库数据为空，返回模拟数据");
                return getMockDetailStatistics(timePoint);
            }
            
            log.info("✅ [详细统计] 进场数据: {} 条, 离场数据: {} 条", 
                     carInList != null ? carInList.size() : 0, 
                     carOutList != null ? carOutList.size() : 0);
            
            // 分析通道分布（详细）
            List<ChannelStats> channelStats = analyzeChannelDistributionFromDB(carInList, carOutList);
            
            // 分析车辆类型分布（详细）
            List<VehicleTypeStats> vehicleTypeStats = analyzeVehicleTypeDistributionFromDB(carInList, carOutList);
            
            // 构建返回数据
            result.put("channelStats", channelStats);
            result.put("vehicleTypeStats", vehicleTypeStats);
            result.put("timePoint", timePoint);
            result.put("parkName", parkName);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [详细统计] 计算详细统计数据失败", e);
            // 发生异常时返回模拟数据
            return getMockDetailStatistics(timePoint);
        }
        
        return result;
    }

    /**
     * 计算通道详情统计数据（用于弹窗）
     */
    private Map<String, Object> calculateChannelDetailStatistics(String parkName, String channelType, 
                                                               String timeRange, String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取时间范围
            TimeRangeInfo timeInfo = getTimeRangeInfo(timeRange, startTime, endTime);
            
            // 使用计算出的时间范围，如果传入的startTime和endTime为null，则使用timeInfo中的值
            String actualStartTime = (startTime != null) ? startTime : timeInfo.getStartTime();
            String actualEndTime = (endTime != null) ? endTime : timeInfo.getEndTime();
            
            log.info("📅 [通道详情统计] 时间范围: {} ~ {}", actualStartTime, actualEndTime);
            
            // 从本地数据库获取进场车辆数据
            QueryWrapper<ReportCarIn> inQueryWrapper = new QueryWrapper<>();
            inQueryWrapper.ge("enter_time", actualStartTime)
                         .le("enter_time", actualEndTime)
                         .eq("deleted", 0)
                         .orderByDesc("enter_time");
            List<ReportCarIn> carInList = reportCarInMapper.selectList(inQueryWrapper);
            
            // 从本地数据库获取离场车辆数据
            QueryWrapper<ReportCarOut> outQueryWrapper = new QueryWrapper<>();
            outQueryWrapper.ge("leave_time", actualStartTime)
                          .le("leave_time", actualEndTime)
                          .eq("deleted", 0)
                          .orderByDesc("leave_time");
            List<ReportCarOut> carOutList = reportCarOutMapper.selectList(outQueryWrapper);
            
            // 如果数据库数据为空，返回模拟数据
            if ((carInList == null || carInList.isEmpty()) && 
                (carOutList == null || carOutList.isEmpty())) {
                log.info("📭 [通道详情统计] 数据库数据为空，返回模拟数据");
                return getMockChannelDetailStatistics(channelType, timeRange);
            }
            
            // 分别处理进场和离场数据
            List<ReportCarIn> entryCars = carInList != null ? carInList : new ArrayList<>();
            List<ReportCarOut> exitCars = carOutList != null ? carOutList : new ArrayList<>();
            
            // 根据通道类型过滤数据
            List<ReportCarIn> filteredEntryCars = new ArrayList<>();
            List<ReportCarOut> filteredExitCars = new ArrayList<>();
            
            if ("entry".equals(channelType)) {
                // 只处理进场数据
                filteredEntryCars = entryCars;
            } else if ("exit".equals(channelType)) {
                // 只处理离场数据
                filteredExitCars = exitCars;
            } else {
                // 处理所有数据
                filteredEntryCars = entryCars;
                filteredExitCars = exitCars;
            }
            
            log.info("✅ [通道详情统计] 过滤后 - 进场数据: {} 条, 离场数据: {} 条", 
                     filteredEntryCars.size(), filteredExitCars.size());
            
            // 分析通道分布（详细）
            List<ChannelStats> channelStats = analyzeChannelDistributionFromDB(filteredEntryCars, filteredExitCars);
            
            // 为每个通道统计按小时的数据
            for (ChannelStats stats : channelStats) {
                List<HourlyStats> channelHourlyData = groupByHourForChannelFromDB(
                    filteredEntryCars, filteredExitCars, timeInfo, stats.getName()
                );
                stats.setHourlyData(channelHourlyData);
            }
            
            // 按时间分组统计（分别统计进场和离场）
            Map<String, HourlyStats> hourlyStats = groupByHourSeparatedFromDB(filteredEntryCars, filteredExitCars, timeInfo);
            
            // 分析车辆类型分布
            List<VehicleTypeStats> vehicleTypeStats = analyzeVehicleTypeDistributionFromDB(filteredEntryCars, filteredExitCars);
            
            // 构建返回数据
            result.put("channelStats", channelStats);
            result.put("hourlyData", hourlyStats.values().stream()
                .sorted(Comparator.comparing(HourlyStats::getHour))
                .collect(Collectors.toList()));
            result.put("vehicleTypeStats", vehicleTypeStats);
            result.put("summary", calculateSummary(hourlyStats.values()));
            result.put("timeRange", timeRange);
            result.put("channelType", channelType);
            result.put("parkName", parkName);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [通道详情统计] 计算通道详情统计数据失败", e);
            // 发生异常时返回模拟数据
            return getMockChannelDetailStatistics(channelType, timeRange);
        }
        
        return result;
    }

    /**
     * 按小时分组统计车辆数据（数据库版本 - 分别处理进场和离场）
     */
    private Map<String, HourlyStats> groupByHourSeparatedFromDB(List<ReportCarIn> entryCars, 
                                                                List<ReportCarOut> exitCars, 
                                                                TimeRangeInfo timeInfo) {
        Map<String, HourlyStats> hourlyStats = new HashMap<>();
        
        // 初始化每小时数据
        for (int i = 0; i < timeInfo.getHourCount(); i++) {
            String hour = timeInfo.getHourLabels().get(i);
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            stats.setEntry(0);
            stats.setExit(0);
            hourlyStats.put(hour, stats);
        }
        
        // 统计进场车辆数据
        for (ReportCarIn car : entryCars) {
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRangeForDB(car.getEnterTime(), timeInfo)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getEnterTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                stats.setEntry(stats.getEntry() + 1);
            }
        }
        
        // 统计离场车辆数据
        for (ReportCarOut car : exitCars) {
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRangeForDB(car.getLeaveTime(), timeInfo)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getLeaveTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                stats.setExit(stats.getExit() + 1);
            }
        }
        
        return hourlyStats;
    }

    /**
     * 为指定通道按小时分组统计车辆数据（数据库版本）
     */
    private List<HourlyStats> groupByHourForChannelFromDB(List<ReportCarIn> entryCars, 
                                                          List<ReportCarOut> exitCars, 
                                                          TimeRangeInfo timeInfo,
                                                          String channelName) {
        Map<String, HourlyStats> hourlyStats = new HashMap<>();
        
        // 初始化每小时数据
        for (int i = 0; i < timeInfo.getHourCount(); i++) {
            String hour = timeInfo.getHourLabels().get(i);
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            stats.setEntry(0);
            stats.setExit(0);
            hourlyStats.put(hour, stats);
        }
        
        // 统计指定通道的进场车辆数据
        for (ReportCarIn car : entryCars) {
            String carChannel = getChannelNameFromDB(car.getEnterChannelName());
            if (!channelName.equals(carChannel)) {
                continue;
            }
            
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRangeForDB(car.getEnterTime(), timeInfo)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getEnterTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                stats.setEntry(stats.getEntry() + 1);
            }
        }
        
        // 统计指定通道的离场车辆数据
        for (ReportCarOut car : exitCars) {
            String carChannel = getChannelNameFromDB(car.getLeaveChannelName());
            if (!channelName.equals(carChannel)) {
                continue;
            }
            
            // 检查车辆时间是否在指定范围内
            if (!isCarInTimeRangeForDB(car.getLeaveTime(), timeInfo)) {
                continue;
            }
            
            String hour = extractHourFromTime(car.getLeaveTime(), timeInfo.getTimeRange());
            HourlyStats stats = hourlyStats.get(hour);
            if (stats != null) {
                stats.setExit(stats.getExit() + 1);
            }
        }
        
        // 按时间顺序返回列表
        return hourlyStats.values().stream()
            .sorted(Comparator.comparing(HourlyStats::getHour))
            .collect(Collectors.toList());
    }


    /**
     * 分析通道分布（数据库版本）
     */
    private List<ChannelStats> analyzeChannelDistributionFromDB(List<ReportCarIn> entryCars, 
                                                                List<ReportCarOut> exitCars) {
        Map<String, ChannelStats> channelMap = new HashMap<>();
        
        // 处理进场车辆数据 - 使用进场通道名称
        for (ReportCarIn car : entryCars) {
            String channel = getChannelNameFromDB(car.getEnterChannelName());
            ChannelStats stats = channelMap.computeIfAbsent(channel, k -> {
                ChannelStats s = new ChannelStats();
                s.setName(k);
                s.setEntry(0);
                s.setExit(0);
                s.setTotal(0);
                return s;
            });
            stats.setEntry(stats.getEntry() + 1);
            stats.setTotal(stats.getTotal() + 1);
        }
        
        // 处理离场车辆数据 - 使用离场通道名称
        for (ReportCarOut car : exitCars) {
            String channel = getChannelNameFromDB(car.getLeaveChannelName());
            ChannelStats stats = channelMap.computeIfAbsent(channel, k -> {
                ChannelStats s = new ChannelStats();
                s.setName(k);
                s.setEntry(0);
                s.setExit(0);
                s.setTotal(0);
                return s;
            });
            stats.setExit(stats.getExit() + 1);
            stats.setTotal(stats.getTotal() + 1);
        }
        
        return channelMap.values().stream()
            .filter(stats -> stats.getEntry() > 0 || stats.getExit() > 0)
            .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
            .collect(Collectors.toList());
    }


    /**
     * 分析车辆类型分布（数据库版本）
     */
    private List<VehicleTypeStats> analyzeVehicleTypeDistributionFromDB(List<ReportCarIn> entryCars, 
                                                                        List<ReportCarOut> exitCars) {
        Map<String, VehicleTypeStats> typeMap = new HashMap<>();
        
        // 处理进场车辆数据
        for (ReportCarIn car : entryCars) {
            String vehicleType = getVehicleTypeFromDB(car.getCarLicenseNumber());
            VehicleTypeStats stats = typeMap.computeIfAbsent(vehicleType, k -> {
                VehicleTypeStats s = new VehicleTypeStats();
                s.setName(k);
                s.setEntry(0);
                s.setExit(0);
                return s;
            });
            stats.setEntry(stats.getEntry() + 1);
        }
        
        // 处理离场车辆数据
        for (ReportCarOut car : exitCars) {
            String vehicleType = getVehicleTypeFromDB(car.getCarLicenseNumber());
            VehicleTypeStats stats = typeMap.computeIfAbsent(vehicleType, k -> {
                VehicleTypeStats s = new VehicleTypeStats();
                s.setName(k);
                s.setEntry(0);
                s.setExit(0);
                return s;
            });
            stats.setExit(stats.getExit() + 1);
        }
        
        return typeMap.values().stream()
            .filter(stats -> stats.getEntry() > 0 || stats.getExit() > 0)
            .sorted((a, b) -> Integer.compare(b.getEntry() + b.getExit(), a.getEntry() + a.getExit()))
            .collect(Collectors.toList());
    }


    /**
     * 获取通道名称（数据库版本）
     */
    private String getChannelNameFromDB(String channelName) {
        if (StringUtils.hasText(channelName)) {
            return channelName;
        }
        return "未知通道";
    }

    /**
     * 获取车辆类型（数据库版本）
     */
    private String getVehicleTypeFromDB(String plateNumber) {
        if (StringUtils.hasText(plateNumber)) {
            // 根据车牌号长度判断：7位=油车，8位=新能源
            return plateNumber.length() == 7 ? "油车" : "新能源车";
        }
        return "未知类型";
    }

    /**
     * 检查车辆时间是否在指定范围内（数据库版本）
     */
    private boolean isCarInTimeRangeForDB(String timeStr, TimeRangeInfo timeInfo) {
        // 如果没有自定义时间范围，返回true
        if (!StringUtils.hasText(timeInfo.getStartTime()) || !StringUtils.hasText(timeInfo.getEndTime())) {
            return true;
        }
        
        if (!StringUtils.hasText(timeStr)) {
            return false;
        }
        
        try {
            LocalDateTime startTime = LocalDateTime.parse(timeInfo.getStartTime(), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime endTime = LocalDateTime.parse(timeInfo.getEndTime(), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime carTime = LocalDateTime.parse(timeStr, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            return !carTime.isBefore(startTime) && !carTime.isAfter(endTime);
            
        } catch (Exception e) {
            log.warn("⚠️ 解析车辆时间失败: {}", timeStr);
            return true; // 解析失败时不过滤
        }
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
        
        int totalEntry = 0;
        int totalExit = 0;
        
        for (HourlyStats stats : hourlyStats) {
            totalEntry += stats.getEntry();
            totalExit += stats.getExit();
        }
        
        summary.put("totalEntry", totalEntry);
        summary.put("totalExit", totalExit);
        summary.put("netFlow", totalEntry - totalExit);
        
        return summary;
    }

    /**
     * 获取模拟统计数据
     */
    private Map<String, Object> getMockStatistics(String timeRange) {
        Map<String, Object> result = new HashMap<>();
        
        // 生成模拟的每小时数据
        List<HourlyStats> hourlyData = new ArrayList<>();
        List<String> hourLabels = getHourLabels(timeRange);
        
        for (String hour : hourLabels) {
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            stats.setEntry((int) (Math.random() * 50) + 10);
            stats.setExit((int) (Math.random() * 45) + 8);
            hourlyData.add(stats);
        }
        
        // 生成模拟的通道数据
        List<ChannelStats> channelStats = Arrays.asList(
            createChannelStats("东门入口", 45, 32),
            createChannelStats("西门入口", 38, 41),
            createChannelStats("南门入口", 52, 28),
            createChannelStats("北门入口", 29, 35),
            createChannelStats("东门出口", 25, 30),
            createChannelStats("西门出口", 33, 28),
            createChannelStats("南门出口", 28, 32),
            createChannelStats("北门出口", 31, 29)
        );
        
        // 生成模拟的车辆类型数据
        List<VehicleTypeStats> vehicleTypeStats = Arrays.asList(
            createVehicleTypeStats("油车", 120, 110),
            createVehicleTypeStats("新能源车", 85, 78),
            createVehicleTypeStats("未知类型", 15, 12)
        );
        
        result.put("hourlyData", hourlyData);
        result.put("channelStats", channelStats);
        result.put("vehicleTypeStats", vehicleTypeStats);
        result.put("summary", calculateSummary(hourlyData));
        result.put("dataSource", "MOCK");
        
        return result;
    }

    /**
     * 获取模拟详细统计数据
     */
    private Map<String, Object> getMockDetailStatistics(String timePoint) {
        Map<String, Object> result = new HashMap<>();
        
        // 生成模拟的通道数据（详细）
        List<ChannelStats> channelStats = Arrays.asList(
            createChannelStats("东门入口", 45, 32),
            createChannelStats("西门入口", 38, 41),
            createChannelStats("南门入口", 52, 28),
            createChannelStats("北门入口", 29, 35),
            createChannelStats("东门出口", 25, 30),
            createChannelStats("西门出口", 33, 28),
            createChannelStats("南门出口", 28, 32),
            createChannelStats("北门出口", 31, 29)
        );
        
        // 生成模拟的车辆类型数据（详细）
        List<VehicleTypeStats> vehicleTypeStats = Arrays.asList(
            createVehicleTypeStats("油车", 120, 110),
            createVehicleTypeStats("新能源车", 85, 78),
            createVehicleTypeStats("未知类型", 15, 12)
        );
        
        result.put("channelStats", channelStats);
        result.put("vehicleTypeStats", vehicleTypeStats);
        result.put("timePoint", timePoint);
        result.put("dataSource", "MOCK");
        
        return result;
    }

    /**
     * 获取模拟通道详情统计数据
     */
    private Map<String, Object> getMockChannelDetailStatistics(String channelType, String timeRange) {
        Map<String, Object> result = new HashMap<>();
        
        // 根据通道类型生成不同的通道数据
        List<ChannelStats> channelStats;
        if ("entry".equals(channelType)) {
            // 进口通道数据
            channelStats = Arrays.asList(
                createChannelStats("3号门入口", 90, 0),
                createChannelStats("5号门入口", 69, 0),
                createChannelStats("8号门入口", 86, 0),
                createChannelStats("银行门入口", 80, 0)
            );
        } else if ("exit".equals(channelType)) {
            // 出口通道数据
            channelStats = Arrays.asList(
                createChannelStats("3号门出口", 0, 85),
                createChannelStats("5号门出口", 0, 65),
                createChannelStats("8号门出口", 0, 82),
                createChannelStats("银行门出口", 0, 75)
            );
        } else {
            // 所有通道数据
            channelStats = Arrays.asList(
                createChannelStats("3号门入口", 90, 85),
                createChannelStats("5号门入口", 69, 65),
                createChannelStats("8号门入口", 86, 82),
                createChannelStats("银行门入口", 80, 75),
                createChannelStats("3号门出口", 0, 85),
                createChannelStats("5号门出口", 0, 65),
                createChannelStats("8号门出口", 0, 82),
                createChannelStats("银行门出口", 0, 75)
            );
        }
        
        // 生成模拟的每小时数据
        List<HourlyStats> hourlyData = new ArrayList<>();
        List<String> hourLabels = getHourLabels(timeRange);
        
        for (String hour : hourLabels) {
            HourlyStats stats = new HourlyStats();
            stats.setHour(hour);
            if ("entry".equals(channelType)) {
                stats.setEntry((int) (Math.random() * 50) + 10);
                stats.setExit(0);
            } else if ("exit".equals(channelType)) {
                stats.setEntry(0);
                stats.setExit((int) (Math.random() * 45) + 8);
            } else {
                stats.setEntry((int) (Math.random() * 50) + 10);
                stats.setExit((int) (Math.random() * 45) + 8);
            }
            hourlyData.add(stats);
        }
        
        // 生成模拟的车辆类型数据
        List<VehicleTypeStats> vehicleTypeStats = Arrays.asList(
            createVehicleTypeStats("油车", 120, 110),
            createVehicleTypeStats("新能源车", 85, 78),
            createVehicleTypeStats("未知类型", 15, 12)
        );
        
        result.put("channelStats", channelStats);
        result.put("hourlyData", hourlyData);
        result.put("vehicleTypeStats", vehicleTypeStats);
        result.put("summary", calculateSummary(hourlyData));
        result.put("timeRange", timeRange);
        result.put("channelType", channelType);
        result.put("dataSource", "MOCK");
        
        return result;
    }

    /**
     * 获取小时标签
     */
    private List<String> getHourLabels(String timeRange) {
        List<String> labels = new ArrayList<>();
        
        switch (timeRange) {
            case "daily":
                for (int i = 0; i < 24; i++) {
                    labels.add(String.format("%02d:00", i));
                }
                break;
            case "weekly":
                labels = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
                break;
            case "monthly":
                LocalDateTime now = LocalDateTime.now();
                int daysInMonth = now.toLocalDate().lengthOfMonth();
                for (int i = 1; i <= daysInMonth; i++) {
                    labels.add(String.format("%d日", i));
                }
                break;
            case "yearly":
                for (int i = 1; i <= 12; i++) {
                    labels.add(String.format("%d月", i));
                }
                break;
            default:
                for (int i = 0; i < 24; i++) {
                    labels.add(String.format("%02d:00", i));
                }
        }
        
        return labels;
    }

    /**
     * 创建通道统计数据
     */
    private ChannelStats createChannelStats(String name, int entry, int exit) {
        ChannelStats stats = new ChannelStats();
        stats.setName(name);
        stats.setEntry(entry);
        stats.setExit(exit);
        stats.setTotal(entry + exit);
        return stats;
    }

    /**
     * 创建车辆类型统计数据
     */
    private VehicleTypeStats createVehicleTypeStats(String name, int entry, int exit) {
        VehicleTypeStats stats = new VehicleTypeStats();
        stats.setName(name);
        stats.setEntry(entry);
        stats.setExit(exit);
        return stats;
    }

    // ==================== 请求参数对象 ====================

    /**
     * 进出口车辆统计数据请求
     */
    @Data
    public static class VehicleFlowStatisticsRequest {
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

    /**
     * 通道详情统计数据请求
     */
    @Data
    public static class ChannelDetailStatisticsRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
        
        @ApiParam(value = "通道类型(entry-进口通道, exit-出口通道, all-所有通道)", required = true)
        private String channelType;
        
        @ApiParam(value = "时间范围(daily-今日, weekly-本周, monthly-本月, yearly-本年度)", required = true)
        private String timeRange;
        
        @ApiParam(value = "开始时间", required = false)
        private String startTime;
        
        @ApiParam(value = "结束时间", required = false)
        private String endTime;
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
        private int entry;
        private int exit;
    }

    /**
     * 通道统计数据
     */
    @Data
    public static class ChannelStats {
        private String name;
        private int entry;
        private int exit;
        private int total; // 总数 = entry + exit
        private List<HourlyStats> hourlyData; // 每小时数据
    }

    /**
     * 车辆类型统计数据
     */
    @Data
    public static class VehicleTypeStats {
        private String name;
        private int entry;
        private int exit;
    }

    /**
     * 获取收费分析数据 - 从payment_record表查询
     * 
     * @param request 请求参数
     * @return 收费分析统计数据
     */
    @PostMapping("/revenue-analysis")
    @ApiOperation(value = "获取收费分析数据", notes = "根据停车时长统计收费情况")
    public ResponseEntity<Result<Map<String, Object>>> getRevenueAnalysis(@RequestBody RevenueAnalysisRequest request) {
        log.info("💰 [收费分析] 开始查询 - 车场: {}, 时间范围: {}", request.getParkName(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [收费分析] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 获取时间范围
            LocalDateTime[] timeRange = getTimeRange(request.getTimeRange());
            LocalDateTime startTime = timeRange[0];
            LocalDateTime endTime = timeRange[1];
            
            // 🔥 从 payment_record 表查询缴费记录
            QueryWrapper<PaymentRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("pay_time", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            queryWrapper.le("pay_time", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            queryWrapper.eq("deleted", 0);
            queryWrapper.orderByDesc("pay_time");
            
            List<PaymentRecord> paymentRecords = paymentRecordMapper.selectList(queryWrapper);
            log.info("📊 [收费分析] 从payment_record表查询到 {} 条缴费记录", paymentRecords.size());
            
            // 🔍 如果没有数据，记录详细信息
            if (paymentRecords.isEmpty()) {
                log.warn("⚠️ [收费分析] payment_record表中没有数据，时间范围: {} ~ {}", 
                    startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            // 按停车时长分组统计收费（使用简短标签）
            Map<String, List<Double>> revenueByDuration = new LinkedHashMap<>();
            revenueByDuration.put("0-0.5h", new ArrayList<>());
            revenueByDuration.put("0.5-1h", new ArrayList<>());
            revenueByDuration.put("1-2h", new ArrayList<>());
            revenueByDuration.put("2-4h", new ArrayList<>());
            revenueByDuration.put("4-6h", new ArrayList<>());
            revenueByDuration.put("6-8h", new ArrayList<>());
            revenueByDuration.put("8-12h", new ArrayList<>());
            revenueByDuration.put(">12h", new ArrayList<>());
            
            // 支付状态统计
            int paidCount = 0;
            int unpaidCount = 0;
            int freeCount = 0;
            double totalRevenue = 0.0;
            int skippedCount = 0; // 跳过的记录数
            
            for (PaymentRecord record : paymentRecords) {
                // 解析收费金额 - 优先使用实际应收金额，其次使用应收金额
                double amount = 0.0;
                try {
                    String amountStr = record.getActualReceivable();
                    if (amountStr == null || amountStr.isEmpty()) {
                        amountStr = record.getAmountReceivable();
                    }
                    
                    if (amountStr != null && !amountStr.isEmpty()) {
                        // 去除货币符号、空格和其他非数字字符（保留数字和小数点）
                        amountStr = amountStr.replaceAll("[^0-9.]", "");
                        if (!amountStr.isEmpty()) {
                            amount = Double.parseDouble(amountStr);
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [收费分析] 解析收费金额失败: actualReceivable={}, amountReceivable={}", 
                        record.getActualReceivable(), record.getAmountReceivable());
                }
                
                // 解析停车时长 - parkingDuration格式：xx小时xx分钟xx秒
                long durationMinutes = 0;
                String durationStr = record.getParkingDuration();
                
                // 如果停车时长为空或无效，尝试根据收费金额估算时长
                if (durationStr == null || durationStr.isEmpty()) {
                    // 根据收费金额粗略估算（假设5元/小时）
                    if (amount > 0) {
                        durationMinutes = (long) (amount / 5.0 * 60);
                        log.debug("🔧 [收费分析] 停车时长为空，根据金额估算: 金额=¥{}, 估算时长={}分钟", amount, durationMinutes);
                    } else {
                        durationMinutes = 15; // 默认15分钟
                        log.debug("🔧 [收费分析] 停车时长为空且金额为0，使用默认15分钟");
                    }
                } else {
                    try {
                        durationMinutes = parseParkingDuration(durationStr);
                    } catch (Exception e) {
                        // 解析失败，根据金额估算
                        if (amount > 0) {
                            durationMinutes = (long) (amount / 5.0 * 60);
                            log.warn("⚠️ [收费分析] 解析停车时长失败 - 原始值: '{}', 根据金额估算={}分钟", durationStr, durationMinutes);
                        } else {
                            durationMinutes = 15;
                            log.warn("⚠️ [收费分析] 解析停车时长失败 - 原始值: '{}', 使用默认15分钟", durationStr);
                        }
                        skippedCount++;
                    }
                }
                
                // 根据停车时长分类（使用简短标签）
                String durationCategory;
                if (durationMinutes <= 30) {
                    durationCategory = "0-0.5h";
                } else if (durationMinutes <= 60) {
                    durationCategory = "0.5-1h";
                } else if (durationMinutes <= 120) {
                    durationCategory = "1-2h";
                } else if (durationMinutes <= 240) {
                    durationCategory = "2-4h";
                } else if (durationMinutes <= 360) {
                    durationCategory = "4-6h";
                } else if (durationMinutes <= 480) {
                    durationCategory = "6-8h";
                } else if (durationMinutes <= 720) {
                    durationCategory = "8-12h";
                } else {
                    durationCategory = ">12h";
                }
                
                revenueByDuration.get(durationCategory).add(amount);
                
                // 统计支付状态
                if (amount > 0) {
                    paidCount++;
                    totalRevenue += amount;
                } else if (amount == 0) {
                    freeCount++;
                } else {
                    unpaidCount++;
                }
            }
            
            int totalProcessed = paymentRecords.size();
            int successCount = totalProcessed - skippedCount;
            log.info("📊 [收费分析] 处理完成 - 总记录: {}, 成功统计: {}, 时长解析失败(已估算): {}", 
                totalProcessed, successCount, skippedCount);
            
            // 计算每个时长段的平均收费和数量
            List<Map<String, Object>> revenueByDurationList = new ArrayList<>();
            int emptyCategories = 0;
            boolean hasAnyData = false;
            
            for (Map.Entry<String, List<Double>> entry : revenueByDuration.entrySet()) {
                List<Double> amounts = entry.getValue();
                
                if (!amounts.isEmpty()) {
                    hasAnyData = true;
                }
            }
            
            // 🎯 如果有任何数据，只返回有数据的时长区间（动态横坐标）
            // 🎯 如果没有任何数据，返回所有区间（值为0）以便前端显示空图表
            for (Map.Entry<String, List<Double>> entry : revenueByDuration.entrySet()) {
                List<Double> amounts = entry.getValue();
                
                if (amounts.isEmpty()) {
                    emptyCategories++;
                    if (hasAnyData) {
                        // 有其他数据时，跳过空区间
                        log.debug("🔍 [收费分析-时长分布] {}: 无数据，不添加到结果集", entry.getKey());
                        continue;
                    } else {
                        // 没有任何数据时，返回所有区间（值为0）
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", entry.getKey());
                        item.put("avgRevenue", "0.00");
                        item.put("count", 0);
                        revenueByDurationList.add(item);
                        log.debug("🔍 [收费分析-时长分布] {}: 无数据，但添加空值以便前端显示", entry.getKey());
                    }
                } else {
                    double avgRevenue = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("avgRevenue", String.format("%.2f", avgRevenue));
                    item.put("count", amounts.size());
                    revenueByDurationList.add(item);
                    
                    // 输出每个时长段的详细统计
                    log.info("📈 [收费分析-时长分布] {}: 数量={}, 平均收费=¥{}", 
                        entry.getKey(), amounts.size(), String.format("%.2f", avgRevenue));
                }
            }
            
            log.info("📊 [收费分析-时长分布] 有数据的区间: {}, 空区间: {}, 总记录数: {}", 
                revenueByDurationList.size() - emptyCategories, emptyCategories, paymentRecords.size());
            
            // 支付状态统计
            List<Map<String, Object>> paymentStats = new ArrayList<>();
            paymentStats.add(createPaymentStat("已付费", paidCount, totalProcessed));
            paymentStats.add(createPaymentStat("未付费", unpaidCount, totalProcessed));
            paymentStats.add(createPaymentStat("免费停车", freeCount, totalProcessed));
            
            // 汇总信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalRevenue", totalRevenue);
            summary.put("avgRevenue", totalProcessed > 0 ? String.format("%.2f", totalRevenue / totalProcessed) : "0.00");
            summary.put("paidVehicles", paidCount);
            summary.put("unpaidVehicles", unpaidCount);
            summary.put("freeVehicles", freeCount);
            summary.put("totalVehicles", totalProcessed);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("paymentStats", paymentStats);
            result.put("revenueByDuration", revenueByDurationList);
            result.put("summary", summary);
            result.put("timeRange", request.getTimeRange());
            result.put("dataSource", "PAYMENT_RECORD");
            
            log.info("✅ [收费分析] 查询成功 - 总收入: ¥{}, 已付费: {}, 免费: {}", 
                     String.format("%.2f", totalRevenue), paidCount, freeCount);
            log.info("📦 [收费分析] 返回数据结构: paymentStats={}, revenueByDuration={}, summary={}", 
                     paymentStats.size(), revenueByDurationList.size(), summary);
            
            return ResponseEntity.ok(Result.success(result));
            
        } catch (Exception e) {
            log.error("❌ [收费分析] 查询失败", e);
            return ResponseEntity.ok(Result.error("查询收费分析数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 解析停车时长字符串为分钟数
     * 格式：xx小时xx分钟xx秒 或 xx分钟xx秒 或 xx秒
     * 
     * @param durationStr 停车时长字符串
     * @return 总分钟数
     */
    private long parseParkingDuration(String durationStr) {
        long totalMinutes = 0;
        
        try {
            // 提取小时数
            if (durationStr.contains("小时")) {
                int hourIndex = durationStr.indexOf("小时");
                String hourPart = durationStr.substring(0, hourIndex).trim();
                // 提取数字部分
                hourPart = hourPart.replaceAll("[^0-9]", "");
                if (!hourPart.isEmpty()) {
                    totalMinutes += Long.parseLong(hourPart) * 60;
                }
            }
            
            // 提取分钟数
            if (durationStr.contains("分钟")) {
                int startIndex = durationStr.contains("小时") ? durationStr.indexOf("小时") + 2 : 0;
                int endIndex = durationStr.indexOf("分钟");
                if (endIndex > startIndex) {
                    String minutePart = durationStr.substring(startIndex, endIndex).trim();
                    minutePart = minutePart.replaceAll("[^0-9]", "");
                    if (!minutePart.isEmpty()) {
                        totalMinutes += Long.parseLong(minutePart);
                    }
                }
            }
            
            // 提取秒数（如果秒数>=30，则向上取整加1分钟）
            if (durationStr.contains("秒")) {
                int startIndex = 0;
                if (durationStr.contains("分钟")) {
                    startIndex = durationStr.indexOf("分钟") + 2;
                } else if (durationStr.contains("小时")) {
                    startIndex = durationStr.indexOf("小时") + 2;
                }
                int endIndex = durationStr.indexOf("秒");
                if (endIndex > startIndex) {
                    String secondPart = durationStr.substring(startIndex, endIndex).trim();
                    secondPart = secondPart.replaceAll("[^0-9]", "");
                    if (!secondPart.isEmpty()) {
                        long seconds = Long.parseLong(secondPart);
                        if (seconds >= 30) {
                            totalMinutes += 1; // 秒数>=30则向上取整
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ [解析停车时长] 失败 - 原始字符串: '{}'", durationStr, e);
            throw e;
        }
        
        return totalMinutes;
    }

    /**
     * 获取车辆热力图数据
     * 
     * @param request 请求参数
     * @return 车辆热力图数据
     */
    @PostMapping("/heatmap-data")
    @ApiOperation(value = "获取车辆热力图数据", notes = "按小时和停车时长分布统计车辆数据")
    public ResponseEntity<Result<Map<String, Object>>> getHeatmapData(@RequestBody HeatmapDataRequest request) {
        log.info("🔥 [车辆热力图] 开始查询 - 车场: {}, 时间范围: {}", request.getParkName(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [车辆热力图] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 获取时间范围
            LocalDateTime[] timeRange = getTimeRange(request.getTimeRange());
            LocalDateTime startTime = timeRange[0];
            LocalDateTime endTime = timeRange[1];
            
            // 查询离场记录
            QueryWrapper<ReportCarOut> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("leave_time", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            queryWrapper.le("leave_time", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            List<ReportCarOut> carOutRecords = reportCarOutMapper.selectList(queryWrapper);
            log.info("📊 [车辆热力图] 查询到 {} 条离场记录", carOutRecords.size());
            
            // 初始化热力图数据矩阵 [小时][时长段]
            int[][] heatmapMatrix = new int[24][9]; // 24小时 x 9个时长段
            
            int filteredCount = 0;  // 记录过滤掉的超24小时记录数
            
            for (ReportCarOut record : carOutRecords) {
                try {
                    // 解析离场时间的小时
                    String leaveTimeStr = record.getLeaveTime();
                    if (leaveTimeStr == null || leaveTimeStr.isEmpty()) {
                        continue;
                    }
                    
                    LocalDateTime leaveTime = LocalDateTime.parse(leaveTimeStr, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    int hour = leaveTime.getHour();
                    
                    // 解析停车时长（秒）
                    long durationSeconds = 0;
                    String stoppingTimeStr = record.getStoppingTime();
                    if (stoppingTimeStr != null && !stoppingTimeStr.isEmpty()) {
                        // 清理可能的非数字字符
                        stoppingTimeStr = stoppingTimeStr.trim().replaceAll("[^0-9]", "");
                        if (!stoppingTimeStr.isEmpty()) {
                            durationSeconds = Long.parseLong(stoppingTimeStr);
                        }
                    }
                    long durationMinutes = durationSeconds / 60;
                    
                    // 🔥 过滤掉超过24小时的记录
                    if (durationMinutes > 1440) {
                        filteredCount++;
                        continue;
                    }
                    
                    // 确定时长段索引（新增 12-18h 和 18-24h 区间）
                    int durationIndex;
                    if (durationMinutes <= 15) {
                        durationIndex = 0; // 0-15min
                    } else if (durationMinutes <= 30) {
                        durationIndex = 1; // 15-30min
                    } else if (durationMinutes <= 60) {
                        durationIndex = 2; // 30min-1h
                    } else if (durationMinutes <= 120) {
                        durationIndex = 3; // 1-2h
                    } else if (durationMinutes <= 240) {
                        durationIndex = 4; // 2-4h
                    } else if (durationMinutes <= 480) {
                        durationIndex = 5; // 4-8h
                    } else if (durationMinutes <= 720) {
                        durationIndex = 6; // 8-12h
                    } else if (durationMinutes <= 1080) {
                        durationIndex = 7; // 12-18h
                    } else {
                        durationIndex = 8; // 18-24h
                    }
                    
                    // 累加计数
                    heatmapMatrix[hour][durationIndex]++;
                    
                } catch (Exception e) {
                    log.warn("⚠️ [车辆热力图] 解析记录失败: {}", e.getMessage());
                }
            }
            
            // 🎯 找出实际有数据的小时范围
            int minHour = 23, maxHour = 0;
            boolean hasData = false;
            for (int h = 0; h < 24; h++) {
                for (int d = 0; d < 9; d++) {
                    if (heatmapMatrix[h][d] > 0) {
                        hasData = true;
                        minHour = Math.min(minHour, h);
                        maxHour = Math.max(maxHour, h);
                    }
                }
            }
            
            // 如果没有数据，返回空数组
            if (!hasData) {
                minHour = 0;
                maxHour = 23;
                log.warn("⚠️ [车辆热力图] 没有有效数据，将返回空热力图");
            }
            
            // 转换为前端需要的格式 [[hourIndex, durationIndex, value], ...]
            // 🔥 只返回有数据的小时范围（动态横坐标）
            // hourIndex 是相对于 minHour 的索引（0, 1, 2, ...），用于对应 hourLabels 数组
            List<int[]> vehicleHeatmapData = new ArrayList<>();
            int hourIndex = 0;
            for (int h = minHour; h <= maxHour; h++) {
                for (int d = 0; d < 9; d++) {
                    vehicleHeatmapData.add(new int[]{hourIndex, d, heatmapMatrix[h][d]});
                }
                hourIndex++;
            }
            
            log.info("📊 [车辆热力图] 实际数据时间范围: {}:00 ~ {}:00", minHour, maxHour);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("vehicleHeatmapData", vehicleHeatmapData);
            result.put("timeRange", request.getTimeRange());
            result.put("dataSource", "DATABASE");
            result.put("totalRecords", carOutRecords.size() - filteredCount);
            result.put("filteredRecords", filteredCount);  // 添加过滤计数
            result.put("minHour", minHour);  // 最小小时
            result.put("maxHour", maxHour);  // 最大小时
            result.put("hourLabels", generateHourLabels(minHour, maxHour));  // 动态生成小时标签
            
            // 添加图例说明（新增区间）
            List<String> durationLabels = Arrays.asList(
                "0-15min", "15-30min", "30min-1h", "1-2h", "2-4h", 
                "4-8h", "8-12h", "12-18h", "18-24h"
            );
            result.put("durationLabels", durationLabels);
            
            log.info("✅ [车辆热力图] 查询成功 - 总记录: {}, 有效记录: {}, 过滤(>24h): {}", 
                     carOutRecords.size(), carOutRecords.size() - filteredCount, filteredCount);
            
            return ResponseEntity.ok(Result.success(result));
            
        } catch (Exception e) {
            log.error("❌ [车辆热力图] 查询失败", e);
            return ResponseEntity.ok(Result.error("查询车辆热力图数据失败: " + e.getMessage()));
        }
    }

    /**
     * 创建支付状态统计项
     */
    private Map<String, Object> createPaymentStat(String name, int value, int total) {
        Map<String, Object> stat = new HashMap<>();
        stat.put("name", name);
        stat.put("value", value);
        stat.put("rate", total > 0 ? String.format("%.1f", (value * 100.0 / total)) : "0.0");
        return stat;
    }

    /**
     * 生成小时标签数组
     * @param minHour 最小小时
     * @param maxHour 最大小时
     * @return 小时标签数组，如 ["0:00", "1:00", ..., "9:00"]
     */
    private List<String> generateHourLabels(int minHour, int maxHour) {
        List<String> labels = new ArrayList<>();
        for (int h = minHour; h <= maxHour; h++) {
            labels.add(h + ":00");
        }
        return labels;
    }

    /**
     * 根据时间范围字符串获取开始和结束时间
     * @param timeRange 时间范围 (today, week, month, year)
     * @return [startTime, endTime]
     */
    private LocalDateTime[] getTimeRange(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end;
        
        switch (timeRange) {
            case "today":
                // 今天 00:00:00 到现在
                start = now.toLocalDate().atStartOfDay();
                end = now;
                break;
            case "week":
                // 本周一 00:00:00 到现在
                start = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
                end = now;
                break;
            case "month":
                // 本月1日 00:00:00 到现在
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now;
                break;
            case "year":
                // 今年1月1日 00:00:00 到现在
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now;
                break;
            default:
                // 默认今天
                start = now.toLocalDate().atStartOfDay();
                end = now;
        }
        
        return new LocalDateTime[]{start, end};
    }

    /**
     * 收费分析请求参数
     */
    @Data
    public static class RevenueAnalysisRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
        
        @ApiParam(value = "时间范围(today-今日, week-本周, month-本月, year-本年度)", required = true)
        private String timeRange;
    }

    /**
     * 热力图数据请求参数
     */
    @Data
    public static class HeatmapDataRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
        
        @ApiParam(value = "时间范围(today-今日, week-本周, month-本月, year-本年度)", required = true)
        private String timeRange;
    }
}
