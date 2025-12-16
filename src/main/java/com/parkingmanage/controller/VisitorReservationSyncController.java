package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.mapper.VisitorReservationSyncMapper;
import com.parkingmanage.service.VisitorReservationSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 访客预约同步数据查询控制器
 * 提供visitor_reservation_sync表的标准查询接口
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/visitor-reservation-sync")
@Api(tags = "访客预约同步数据查询")
public class VisitorReservationSyncController {

    @Autowired
    private VisitorReservationSyncService visitorReservationSyncService;
    
    @Autowired
    private VisitorReservationSyncMapper visitorReservationSyncMapper;

    /**
     * 根据车牌号查询预约记录
     * 
     * @param carNumber 车牌号（必填）
     * @param pageNum 页码，默认1
     * @param pageSize 每页数量，默认10
     * @return 查询结果
     */
    @GetMapping("/query-by-car-number")
    @ApiOperation(value = "根据车牌号查询预约记录", notes = "根据车牌号查询访客预约记录，支持分页")
    public ResponseEntity<Result> queryByCarNumber(
            @ApiParam(value = "车牌号", required = true) @RequestParam String carNumber,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        log.info("🔍 [查询预约记录] 车牌号: {}, 页码: {}, 每页: {}", carNumber, pageNum, pageSize);
        
        try {
            // 参数校验
            if (!StringUtils.hasText(carNumber)) {
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            // 构建分页对象
            Page<VisitorReservationSync> page = new Page<>(pageNum, pageSize);
            
            // 构建查询条件
            LambdaQueryWrapper<VisitorReservationSync> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VisitorReservationSync::getCarNumber, carNumber)
                       .orderByDesc(VisitorReservationSync::getCreateTime);
            
            // 执行查询 - 使用 Mapper 的 selectPage 方法
            IPage<VisitorReservationSync> resultPage = visitorReservationSyncMapper.selectPage(page, queryWrapper);
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("records", resultPage.getRecords());
            data.put("total", resultPage.getTotal());
            data.put("pageNum", resultPage.getCurrent());
            data.put("pageSize", resultPage.getSize());
            data.put("pages", resultPage.getPages());
            
            log.info("✅ [查询成功] 车牌号: {}, 找到 {} 条记录", carNumber, resultPage.getTotal());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 车牌号: {}, 错误: {}", carNumber, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据车牌号查询最新的一条预约记录
     * 
     * @param carNumber 车牌号
     * @return 最新的预约记录
     */
    @GetMapping("/query-latest-by-car-number")
    @ApiOperation(value = "根据车牌号查询最新预约", notes = "根据车牌号查询最新的一条预约记录")
    public ResponseEntity<Result> queryLatestByCarNumber(
            @ApiParam(value = "车牌号", required = true) @RequestParam String carNumber) {
        
        log.info("🔍 [查询最新预约] 车牌号: {}", carNumber);
        
        try {
            if (!StringUtils.hasText(carNumber)) {
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            // 构建查询条件：查询最新的一条记录
            LambdaQueryWrapper<VisitorReservationSync> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VisitorReservationSync::getCarNumber, carNumber)
                       .orderByDesc(VisitorReservationSync::getCreateTime)
                       .last("LIMIT 1");
            
            VisitorReservationSync reservation = visitorReservationSyncMapper.selectOne(queryWrapper);
            
            if (reservation == null) {
                log.info("📭 [未找到记录] 车牌号: {}", carNumber);
                return ResponseEntity.ok(Result.error("未找到该车牌号的预约记录"));
            }
            
            log.info("✅ [查询成功] 车牌号: {}, 预约ID: {}, 访客: {}", 
                    carNumber, reservation.getReservationId(), reservation.getVisitorName());
            
            return ResponseEntity.ok(Result.success(reservation));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 车牌号: {}, 错误: {}", carNumber, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据车牌号查询当前有效的预约记录
     * 查询条件：车牌号匹配 且 当前时间在预约时间范围内
     * 
     * @param carNumber 车牌号
     * @return 当前有效的预约记录
     */
    @GetMapping("/query-valid-by-car-number")
    @ApiOperation(value = "根据车牌号查询有效预约", notes = "查询车牌号当前时间段内有效的预约记录")
    public ResponseEntity<Result> queryValidByCarNumber(
            @ApiParam(value = "车牌号", required = true) @RequestParam String carNumber) {
        
        log.info("🔍 [查询有效预约] 车牌号: {}", carNumber);
        
        try {
            if (!StringUtils.hasText(carNumber)) {
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            // 获取当前时间
            Date currentTime = new Date();
            
            // 构建查询条件：车牌号匹配 且 当前时间在预约时间范围内
            LambdaQueryWrapper<VisitorReservationSync> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VisitorReservationSync::getCarNumber, carNumber)
                       .le(VisitorReservationSync::getStartTime, currentTime)  // 开始时间 <= 当前时间
                       .ge(VisitorReservationSync::getEndTime, currentTime)    // 结束时间 >= 当前时间
                       .orderByDesc(VisitorReservationSync::getCreateTime);
            
            List<VisitorReservationSync> reservations = visitorReservationSyncMapper.selectList(queryWrapper);
            
            if (reservations == null || reservations.isEmpty()) {
                log.info("📭 [未找到有效预约] 车牌号: {}", carNumber);
                return ResponseEntity.ok(Result.error("未找到该车牌号的有效预约记录"));
            }
            
            log.info("✅ [查询成功] 车牌号: {}, 找到 {} 条有效预约", carNumber, reservations.size());
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", reservations);
            data.put("total", reservations.size());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 车牌号: {}, 错误: {}", carNumber, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据预约ID查询详情
     * 
     * @param reservationId 预约ID
     * @return 预约详情
     */
    @GetMapping("/query-by-id")
    @ApiOperation(value = "根据预约ID查询详情", notes = "根据外部系统的预约ID查询预约详情")
    public ResponseEntity<Result> queryById(
            @ApiParam(value = "预约ID", required = true) @RequestParam String reservationId) {
        
        log.info("🔍 [查询预约详情] 预约ID: {}", reservationId);
        
        try {
            if (!StringUtils.hasText(reservationId)) {
                return ResponseEntity.ok(Result.error("预约ID不能为空"));
            }
            
            LambdaQueryWrapper<VisitorReservationSync> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VisitorReservationSync::getReservationId, reservationId);
            
            VisitorReservationSync reservation = visitorReservationSyncMapper.selectOne(queryWrapper);
            
            if (reservation == null) {
                log.info("📭 [未找到记录] 预约ID: {}", reservationId);
                return ResponseEntity.ok(Result.error("未找到该预约记录"));
            }
            
            log.info("✅ [查询成功] 预约ID: {}, 访客: {}, 车牌: {}", 
                    reservationId, reservation.getVisitorName(), reservation.getCarNumber());
            
            return ResponseEntity.ok(Result.success(reservation));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 预约ID: {}, 错误: {}", reservationId, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询所有预约记录
     * 
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @return 分页结果
     */
    @GetMapping("/query-all")
    @ApiOperation(value = "分页查询所有预约", notes = "分页查询所有预约记录，可指定时间范围")
    public ResponseEntity<Result> queryAll(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @ApiParam(value = "开始时间") @RequestParam(required = false) String startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String endTime) {
        
        log.info("🔍 [查询所有预约] 页码: {}, 每页: {}, 开始时间: {}, 结束时间: {}", 
                pageNum, pageSize, startTime, endTime);
        
        try {
            Page<VisitorReservationSync> page = new Page<>(pageNum, pageSize);
            
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            
            // 如果指定了时间范围
            if (StringUtils.hasText(startTime)) {
                queryWrapper.ge("create_time", startTime);
            }
            if (StringUtils.hasText(endTime)) {
                queryWrapper.le("create_time", endTime);
            }
            
            queryWrapper.orderByDesc("create_time");
            
            IPage<VisitorReservationSync> resultPage = visitorReservationSyncMapper.selectPage(page, queryWrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", resultPage.getRecords());
            data.put("total", resultPage.getTotal());
            data.put("pageNum", resultPage.getCurrent());
            data.put("pageSize", resultPage.getSize());
            data.put("pages", resultPage.getPages());
            
            log.info("✅ [查询成功] 共 {} 条记录", resultPage.getTotal());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 错误: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据访客姓名查询预约记录
     * 
     * @param visitorName 访客姓名
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 查询结果
     */
    @GetMapping("/query-by-visitor-name")
    @ApiOperation(value = "根据访客姓名查询", notes = "根据访客姓名查询预约记录")
    public ResponseEntity<Result> queryByVisitorName(
            @ApiParam(value = "访客姓名", required = true) @RequestParam String visitorName,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        log.info("🔍 [查询预约记录] 访客姓名: {}", visitorName);
        
        try {
            if (!StringUtils.hasText(visitorName)) {
                return ResponseEntity.ok(Result.error("访客姓名不能为空"));
            }
            
            Page<VisitorReservationSync> page = new Page<>(pageNum, pageSize);
            
            LambdaQueryWrapper<VisitorReservationSync> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VisitorReservationSync::getVisitorName, visitorName)
                       .orderByDesc(VisitorReservationSync::getCreateTime);
            
            IPage<VisitorReservationSync> resultPage = visitorReservationSyncMapper.selectPage(page, queryWrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", resultPage.getRecords());
            data.put("total", resultPage.getTotal());
            data.put("pageNum", resultPage.getCurrent());
            data.put("pageSize", resultPage.getSize());
            data.put("pages", resultPage.getPages());
            
            log.info("✅ [查询成功] 访客姓名: {}, 找到 {} 条记录", visitorName, resultPage.getTotal());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [查询失败] 访客姓名: {}, 错误: {}", visitorName, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }
}
