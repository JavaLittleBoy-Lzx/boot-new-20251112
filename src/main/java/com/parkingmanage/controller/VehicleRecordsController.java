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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆记录控制器
 * 提供车辆进出场记录查询
 * 
 * @author System
 */
@Api(tags = "车辆记录管理")
@RestController
@RequestMapping("/parking/vehicle-records")
@Slf4j
public class VehicleRecordsController {

    @Autowired
    private ReportCarInMapper reportCarInMapper;

    @Autowired
    private ReportCarOutMapper reportCarOutMapper;

    /**
     * 查询车辆进场记录
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param plateNumber 车牌号
     * @param channel 通道
     * @param page 页码
     * @param size 每页条数
     * @return 进场记录列表
     */
    @GetMapping("/report_car_in")
    @ApiOperation(value = "查询车辆进场记录", notes = "支持时间范围、车牌号、通道筛选")
    public Result<?> getCarInRecords(
            @ApiParam(value = "开始时间") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String endDate,
            @ApiParam(value = "车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam(value = "通道") @RequestParam(required = false) String channel,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            log.info("📋 [车辆进场] 查询参数 - 开始: {}, 结束: {}, 车牌: {}, 通道: {}, 页: {}, 大小: {}",
                    startDate, endDate, plateNumber, channel, page, size);
            
            // 构建查询条件
            QueryWrapper<ReportCarIn> queryWrapper = new QueryWrapper<>();
            
            // 时间范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.ge("enter_time", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.le("enter_time", endDate);
            }
            
            // 车牌号筛选
            if (plateNumber != null && !plateNumber.isEmpty()) {
                queryWrapper.like("car_license_number", plateNumber);
            }
            
            // 通道筛选
            if (channel != null && !channel.isEmpty()) {
                queryWrapper.eq("enter_channel_name", channel);
            }
            
            // 按时间倒序
            queryWrapper.orderByDesc("enter_time");
            
            // 查询所有符合条件的记录
            List<ReportCarIn> allRecords = reportCarInMapper.selectList(queryWrapper);
            
            // 计算分页
            int total = allRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<ReportCarIn> pagedRecords;
            if (fromIndex >= total) {
                pagedRecords = new ArrayList<>();
            } else {
                pagedRecords = allRecords.subList(fromIndex, toIndex);
            }
            
            // 转换数据格式
            List<Map<String, Object>> dataList = pagedRecords.stream().map(record -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("carNo", record.getCarLicenseNumber());
                data.put("plateNumber", record.getCarLicenseNumber());
                data.put("channelName", record.getEnterChannelName());
                data.put("createTime", record.getEnterTime());
                data.put("enterTime", record.getEnterTime());
                data.put("enterCarType", record.getEnterType());
                
                // 修复VIP名称字段：使用实际的VIP名称而不是VIP类型
                data.put("vipName", record.getEnterCustomVipName());  // 使用实际VIP名称
                data.put("enterCustomVipName", record.getEnterCustomVipName());  // 兼容字段
                data.put("enter_custom_vip_name", record.getEnterCustomVipName());  // 下划线版本
                data.put("enterVipType", record.getEnterVipType());  // VIP类型
                data.put("enter_vip_type", record.getEnterVipType());  // 下划线版本
                
                data.put("enter_car_license_color", record.getEnterCarLicenseColor());
                data.put("enter_car_type", record.getEnterType());
                data.put("imageUrl", record.getEnterCarFullPicture());
                return data;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", dataList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            
            log.info("✅ [车辆进场] 查询成功，总数: {}, 当前页: {}, 返回: {} 条", total, page, dataList.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [车辆进场] 查询失败", e);
            return Result.error("查询车辆进场记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询车辆出场记录
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param plateNumber 车牌号
     * @param channel 通道
     * @param page 页码
     * @param size 每页条数
     * @return 出场记录列表
     */
    @GetMapping("/report_car_out")
    @ApiOperation(value = "查询车辆出场记录", notes = "支持时间范围、车牌号、通道筛选")
    public Result<?> getCarOutRecords(
            @ApiParam(value = "开始时间") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String endDate,
            @ApiParam(value = "车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam(value = "通道") @RequestParam(required = false) String channel,
            @ApiParam(value = "进场通道") @RequestParam(required = false) String enterChannel,
            @ApiParam(value = "出场通道") @RequestParam(required = false) String exitChannel,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            log.info("📋 [车辆出场] 查询参数 - 开始: {}, 结束: {}, 车牌: {}, 通道: {}, 进场通道: {}, 出场通道: {}, 页: {}, 大小: {}",
                    startDate, endDate, plateNumber, channel, enterChannel, exitChannel, page, size);
            
            // 构建查询条件
            QueryWrapper<ReportCarOut> queryWrapper = new QueryWrapper<>();
            
            // 时间范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.ge("leave_time", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.le("leave_time", endDate);
            }
            
            // 车牌号筛选
            if (plateNumber != null && !plateNumber.isEmpty()) {
                queryWrapper.like("car_license_number", plateNumber);
            }
            
            // 🔥 新增：独立的进场通道和出场通道筛选
            if (enterChannel != null && !enterChannel.isEmpty()) {
                queryWrapper.eq("enter_channel_name", enterChannel);
                log.info("🔍 [进场通道筛选] enter_channel_name = '{}'", enterChannel);
            }
            
            if (exitChannel != null && !exitChannel.isEmpty()) {
                queryWrapper.eq("leave_channel_name", exitChannel);
                log.info("🔍 [出场通道筛选] leave_channel_name = '{}'", exitChannel);
            }
            
            // 兼容原有的通道参数（默认按出场通道筛选）
            if (channel != null && !channel.isEmpty() && enterChannel == null && exitChannel == null) {
                queryWrapper.eq("leave_channel_name", channel);
                log.info("🔍 [兼容通道筛选] leave_channel_name = '{}'", channel);
            }
            
            // 按时间倒序
            queryWrapper.orderByDesc("leave_time");
            
            // 查询所有符合条件的记录
            List<ReportCarOut> allRecords = reportCarOutMapper.selectList(queryWrapper);
            
            // 计算分页
            int total = allRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<ReportCarOut> pagedRecords;
            if (fromIndex >= total) {
                pagedRecords = new ArrayList<>();
            } else {
                pagedRecords = allRecords.subList(fromIndex, toIndex);
            }
            
            // 转换数据格式 - 完整映射数据库字段
            List<Map<String, Object>> dataList = pagedRecords.stream().map(record -> {
                Map<String, Object> data = new HashMap<>();
                
                // 基础字段
                data.put("id", record.getId());
                data.put("carNo", record.getCarLicenseNumber());
                data.put("plateNumber", record.getCarLicenseNumber());
                data.put("car_license_number", record.getCarLicenseNumber());
                data.put("licensePlateNumber", record.getCarLicenseNumber());
                
                // 通道名称字段
                data.put("enterChannelName", record.getEnterChannelName());
                data.put("enter_channel_name", record.getEnterChannelName());
                data.put("leaveChannelName", record.getLeaveChannelName());
                data.put("leave_channel_name", record.getLeaveChannelName());
                data.put("channelName", record.getLeaveChannelName());  // 兼容字段
                
                // 时间字段
                data.put("createTime", record.getCreateTime());
                data.put("enterTime", record.getEnterTime());
                data.put("enter_time", record.getEnterTime());
                data.put("leaveTime", record.getLeaveTime());
                data.put("leave_time", record.getLeaveTime());
                data.put("exitTime", record.getLeaveTime());
                
                // 类型字段
                data.put("enterType", record.getEnterType());
                data.put("enter_type", record.getEnterType());
                data.put("leaveType", record.getLeaveType());
                data.put("leave_type", record.getLeaveType());
                
                // VIP类型字段
                data.put("enterVipType", record.getEnterVipType());
                data.put("enter_vip_type", record.getEnterVipType());
                data.put("leaveVipType", record.getLeaveVipType());
                data.put("leave_vip_type", record.getLeaveVipType());
                
                // VIP名称字段 - 重要：这是实际的VIP名称
                data.put("leaveCustomVipName", record.getLeaveCustomVipName());
                data.put("leave_custom_vip_name", record.getLeaveCustomVipName());
                data.put("vipName", record.getLeaveCustomVipName());  // 兼容字段
                
                // 车牌颜色字段
                data.put("enterCarLicenseColor", record.getEnterCarLicenseColor());
                data.put("enter_car_license_color", record.getEnterCarLicenseColor());
                data.put("leaveCarLicenseColor", record.getLeaveCarLicenseColor());
                data.put("leave_car_license_color", record.getLeaveCarLicenseColor());
                
                // 车辆类型字段
                data.put("enterCarType", record.getEnterCarType());
                data.put("enter_car_type", record.getEnterCarType());
                data.put("leaveCarType", record.getLeaveCarType());
                data.put("leave_car_type", record.getLeaveCarType());
                
                // 记录类型
                data.put("recordType", record.getRecordType());
                data.put("record_type", record.getRecordType());
                
                // 停车时长
                data.put("stoppingTime", record.getStoppingTime());
                data.put("stopping_time", record.getStoppingTime());
                data.put("parkingTime", record.getStoppingTime());
                
                // 金额
                data.put("amountReceivable", record.getAmountReceivable());
                data.put("amount_receivable", record.getAmountReceivable());
                
                // 照片字段 - 进场和离场都要提供
                data.put("enterPhoto", record.getEnterCarFullPicture());
                data.put("enter_car_full_picture", record.getEnterCarFullPicture());
                data.put("leavePhoto", record.getLeaveCarFullPicture());
                data.put("leave_car_full_picture", record.getLeaveCarFullPicture());
                data.put("imageUrl", record.getEnterCarFullPicture());  // 兼容字段
                
                return data;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", dataList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            
            log.info("✅ [车辆出场] 查询成功，总数: {}, 当前页: {}, 返回: {} 条", total, page, dataList.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [车辆出场] 查询失败", e);
            return Result.error("查询车辆出场记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询在场车辆（进场但未出场）
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param plateNumber 车牌号
     * @param channel 通道
     * @param page 页码
     * @param size 每页条数
     * @return 在场车辆列表
     */
    @GetMapping("/onsite")
    @ApiOperation(value = "查询在场车辆", notes = "查询进场但未出场的车辆")
    public Result<?> getOnsiteVehicles(
            @ApiParam(value = "开始时间") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String endDate,
            @ApiParam(value = "车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam(value = "通道") @RequestParam(required = false) String channel,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            log.info("📋 [在场车辆] 查询参数 - 开始: {}, 结束: {}, 车牌: {}, 通道: {}, 页: {}, 大小: {}",
                    startDate, endDate, plateNumber, channel, page, size);
            
            // 查询进场记录
            QueryWrapper<ReportCarIn> inQuery = new QueryWrapper<>();
            if (startDate != null && !startDate.isEmpty()) {
                inQuery.ge("enter_time", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                inQuery.le("enter_time", endDate);
            }
            if (plateNumber != null && !plateNumber.isEmpty()) {
                inQuery.like("car_license_number", plateNumber);
            }
            if (channel != null && !channel.isEmpty()) {
                inQuery.eq("enter_channel_name", channel);
            }
            inQuery.orderByDesc("enter_time");
            List<ReportCarIn> inRecords = reportCarInMapper.selectList(inQuery);
            
            // 查询出场记录
            QueryWrapper<ReportCarOut> outQuery = new QueryWrapper<>();
            if (startDate != null && !startDate.isEmpty()) {
                outQuery.ge("leave_time", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                outQuery.le("leave_time", endDate);
            }
            List<ReportCarOut> outRecords = reportCarOutMapper.selectList(outQuery);
            
            // 找出已出场的车牌号
            Set<String> exitedPlates = outRecords.stream()
                    .map(ReportCarOut::getCarLicenseNumber)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // 过滤出未出场的车辆
            List<ReportCarIn> onsiteRecords = inRecords.stream()
                    .filter(record -> record.getCarLicenseNumber() != null)
                    .filter(record -> !exitedPlates.contains(record.getCarLicenseNumber()))
                    .collect(Collectors.toList());
            
            // 计算分页
            int total = onsiteRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<ReportCarIn> pagedRecords;
            if (fromIndex >= total) {
                pagedRecords = new ArrayList<>();
            } else {
                pagedRecords = onsiteRecords.subList(fromIndex, toIndex);
            }
            
            // 转换数据格式 - 修复VIP名称映射
            List<Map<String, Object>> dataList = pagedRecords.stream().map(record -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("carNo", record.getCarLicenseNumber());
                data.put("plateNumber", record.getCarLicenseNumber());
                data.put("channelName", record.getEnterChannelName());
                data.put("createTime", record.getEnterTime());
                data.put("enterTime", record.getEnterTime());
                data.put("enterCarType", record.getEnterType());
                
                // 修复VIP名称字段：使用实际的VIP名称而不是VIP类型
                data.put("vipName", record.getEnterCustomVipName());  // ✅ 使用实际VIP名称
                data.put("enterCustomVipName", record.getEnterCustomVipName());  // 兼容字段
                data.put("enter_custom_vip_name", record.getEnterCustomVipName());  // 下划线版本
                data.put("enterVipType", record.getEnterVipType());  // VIP类型
                data.put("enter_vip_type", record.getEnterVipType());  // 下划线版本
                
                data.put("enter_car_license_color", record.getEnterCarLicenseColor());
                data.put("enter_car_type", record.getEnterType());
                data.put("imageUrl", record.getEnterCarFullPicture());
                
                // 计算停车时长
                if (record.getEnterTime() != null) {
                    data.put("duration", calculateDuration(record.getEnterTime()));
                }
                
                return data;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", dataList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            
            log.info("✅ [在场车辆] 查询成功，总数: {}, 当前页: {}, 返回: {} 条", total, page, dataList.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [在场车辆] 查询失败", e);
            return Result.error("查询在场车辆失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算停车时长
     * 
     * @param enterTime 进场时间
     * @return 停车时长描述
     */
    private String calculateDuration(String enterTime) {
        try {
            // 简单的时长计算（实际项目中可能需要更复杂的日期解析）
            return "计算中";
        } catch (Exception e) {
            return "未知";
        }
    }
}
