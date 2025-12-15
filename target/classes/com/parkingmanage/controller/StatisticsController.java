package com.parkingmanage.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.AcmsEventRecord;
import com.parkingmanage.mapper.AcmsEventRecordMapper;
import com.parkingmanage.mapper.ReportCarInMapper;
import com.parkingmanage.mapper.ReportCarOutMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统计数据控制器
 * 提供车辆、人脸的进出场统计数据
 * 
 * @author System
 */
@Api(tags = "统计数据")
@RestController
@RequestMapping("/parking/statistics")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class StatisticsController {

    @Autowired
    private ReportCarInMapper reportCarInMapper;

    @Autowired
    private ReportCarOutMapper reportCarOutMapper;

    @Autowired
    private AcmsEventRecordMapper acmsEventRecordMapper;

    @Autowired
    private RestTemplate restTemplate;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 统计车辆进场数（report_car_in表）
     */
    @GetMapping("/vehicle-entry")
    @ApiOperation(value = "统计车辆进场数", notes = "统计指定时间范围内的车辆进场数量")
    public Result<Integer> getVehicleEntry(
            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00")
            @RequestParam String startDate,
            @ApiParam(value = "结束时间", required = true, example = "2024-01-01 23:59:59")
            @RequestParam String endDate) {
        
        try {
            log.info("📊 [车辆进场统计] 开始查询: {} - {}", startDate, endDate);
            
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
            
            // 统计创建时间在时间范围内的记录数
            QueryWrapper<com.parkingmanage.entity.ReportCarIn> wrapper = new QueryWrapper<>();
            wrapper.between("create_time", start, end);
            
            Integer count = reportCarInMapper.selectCount(wrapper);
            
            log.info("✅ [车辆进场统计] 查询完成: {} 辆", count);
            return Result.success(count);
            
        } catch (Exception e) {
            log.error("❌ [车辆进场统计] 查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 统计车辆出场数（report_car_out表）
     */
    @GetMapping("/vehicle-exit")
    @ApiOperation(value = "统计车辆出场数", notes = "统计指定时间范围内的车辆出场数量")
    public Result<Integer> getVehicleExit(
            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00")
            @RequestParam String startDate,
            @ApiParam(value = "结束时间", required = true, example = "2024-01-01 23:59:59")
            @RequestParam String endDate) {
        
        try {
            log.info("📊 [车辆出场统计] 开始查询: {} - {}", startDate, endDate);
            
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
            
            // 统计创建时间在时间范围内的记录数
            QueryWrapper<com.parkingmanage.entity.ReportCarOut> wrapper = new QueryWrapper<>();
            wrapper.between("create_time", start, end);
            
            Integer count = reportCarOutMapper.selectCount(wrapper);
            
            log.info("✅ [车辆出场统计] 查询完成: {} 辆", count);
            return Result.success(count);
            
        } catch (Exception e) {
            log.error("❌ [车辆出场统计] 查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 统计人脸进场数（acms_event_record表，direction='进'）
     */
    @GetMapping("/face-entry")
    @ApiOperation(value = "统计人脸进场数", notes = "统计指定时间范围内的人脸进场数量")
    public Result<Integer> getFaceEntry(
            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00")
            @RequestParam String startDate,
            @ApiParam(value = "结束时间", required = true, example = "2024-01-01 23:59:59")
            @RequestParam String endDate) {
        
        try {
            log.info("📊 [人脸进场统计] 开始查询: {} - {}", startDate, endDate);
            
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
            
            // 统计event_time在时间范围内且direction='进'的记录数
            QueryWrapper<AcmsEventRecord> wrapper = new QueryWrapper<>();
            wrapper.between("event_time", start, end)
                   .eq("direction", "进");
            
            Integer count = acmsEventRecordMapper.selectCount(wrapper);
            
            log.info("✅ [人脸进场统计] 查询完成: {} 人次", count);
            return Result.success(count);
            
        } catch (Exception e) {
            log.error("❌ [人脸进场统计] 查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 统计人脸出场数（acms_event_record表，direction='出'）
     */
    @GetMapping("/face-exit")
    @ApiOperation(value = "统计人脸出场数", notes = "统计指定时间范围内的人脸出场数量")
    public Result<Integer> getFaceExit(
            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00")
            @RequestParam String startDate,
            @ApiParam(value = "结束时间", required = true, example = "2024-01-01 23:59:59")
            @RequestParam String endDate) {
        
        try {
            log.info("📊 [人脸出场统计] 开始查询: {} - {}", startDate, endDate);
            
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_TIME_FORMATTER);
            
            // 统计event_time在时间范围内且direction='出'的记录数
            QueryWrapper<AcmsEventRecord> wrapper = new QueryWrapper<>();
            wrapper.between("event_time", start, end)
                   .eq("direction", "出");
            
            Integer count = acmsEventRecordMapper.selectCount(wrapper);
            
            log.info("✅ [人脸出场统计] 查询完成: {} 人次", count);
            return Result.success(count);
            
        } catch (Exception e) {
            log.error("❌ [人脸出场统计] 查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
