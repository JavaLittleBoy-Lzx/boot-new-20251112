package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.NightStudentAlertConfig;
import com.parkingmanage.entity.NightStudentAlertRecord;
import com.parkingmanage.service.NightStudentAlertService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夜间学生出校提醒控制器
 *
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/night-alert")
@Api(tags = "夜间学生出校提醒管理")
public class NightStudentAlertController {

    @Resource
    private NightStudentAlertService nightStudentAlertService;

    // ==================== 配置相关 ====================

    @GetMapping("/config")
    @ApiOperation(value = "获取夜间提醒配置", notes = "获取夜间学生出校提醒的配置信息")
    public ResponseEntity<Result> getConfig() {
        try {
            NightStudentAlertConfig config = nightStudentAlertService.getConfig();
            return ResponseEntity.ok(Result.success(config));
        } catch (Exception e) {
            log.error("❌ [夜间提醒配置] 获取失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取配置失败: " + e.getMessage()));
        }
    }

    @PutMapping("/config")
    @ApiOperation(value = "更新夜间提醒配置", notes = "更新夜间学生出校提醒的配置信息")
    public ResponseEntity<Result> updateConfig(@RequestBody NightStudentAlertConfig config) {
        try {
            boolean success = nightStudentAlertService.updateConfig(config);
            if (success) {
                log.info("✅ [夜间提醒配置] 更新成功 - enabled: {}, start: {}, end: {}",
                        config.getEnabled(), config.getNightStartTime(), config.getNightEndTime());
                return ResponseEntity.ok(Result.success("配置更新成功"));
            } else {
                return ResponseEntity.ok(Result.error("配置更新失败"));
            }
        } catch (Exception e) {
            log.error("❌ [夜间提醒配置] 更新失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("更新配置失败: " + e.getMessage()));
        }
    }

    // ==================== 提醒记录相关 ====================

    @GetMapping("/unread-count")
    @ApiOperation(value = "获取未读数量", notes = "获取夜间学生出校提醒的未读记录数量")
    public ResponseEntity<Result> getUnreadCount() {
        try {
            int count = nightStudentAlertService.getUnreadCount();
            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", count);
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 获取未读数量失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取未读数量失败: " + e.getMessage()));
        }
    }

    @GetMapping("/records")
    @ApiOperation(value = "查询提醒记录", notes = "分页查询夜间学生出校提醒记录，支持按通道、性别、学院筛选")
    public ResponseEntity<Result> getRecords(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "通道名称") @RequestParam(required = false) String channelName,
            @ApiParam(value = "性别") @RequestParam(required = false) String gender,
            @ApiParam(value = "学院") @RequestParam(required = false) String college,
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        try {
            IPage<NightStudentAlertRecord> records = nightStudentAlertService.getRecords(
                    pageNum, pageSize, channelName, gender, college, startTime, endTime);

            Map<String, Object> data = new HashMap<>();
            data.put("records", records.getRecords());
            data.put("total", records.getTotal());
            data.put("pageNum", records.getCurrent());
            data.put("pageSize", records.getSize());
            data.put("pages", records.getPages());
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 查询记录失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询记录失败: " + e.getMessage()));
        }
    }

    @PutMapping("/read/{id}")
    @ApiOperation(value = "标记单条已读", notes = "将指定ID的夜间学生出校提醒标记为已读")
    public ResponseEntity<Result> markAsRead(
            @ApiParam(value = "记录ID", required = true) @PathVariable int id) {
        try {
            boolean success = nightStudentAlertService.markAsRead(id);
            if (success) {
                log.info("✅ [夜间提醒] 标记已读成功 - id: {}", id);
                return ResponseEntity.ok(Result.success("标记已读成功"));
            } else {
                return ResponseEntity.ok(Result.error("标记已读失败"));
            }
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 标记已读失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("标记已读失败: " + e.getMessage()));
        }
    }

    @PutMapping("/read-all")
    @ApiOperation(value = "标记全部已读", notes = "将所有未读的夜间学生出校提醒标记为已读")
    public ResponseEntity<Result> markAllAsRead() {
        try {
            int count = nightStudentAlertService.markAllAsRead();
            log.info("✅ [夜间提醒] 标记全部已读成功 - 数量: {}", count);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 标记全部已读失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("标记全部已读失败: " + e.getMessage()));
        }
    }

    // ==================== 统计相关 ====================

    @GetMapping("/statistics")
    @ApiOperation(value = "统计报表", notes = "获取夜间学生出校提醒的统计数据，按出口、性别、学院统计")
    public ResponseEntity<Result> getStatistics(
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        try {
            Map<String, Object> statistics = nightStudentAlertService.getStatistics(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 获取统计失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取统计失败: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/channel")
    @ApiOperation(value = "按通道统计", notes = "按出口通道统计夜间学生出校数量")
    public ResponseEntity<Result> getStatisticsByChannel(
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        try {
            Map<String, Object> statistics = nightStudentAlertService.getStatisticsByChannel(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 按通道统计失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("按通道统计失败: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/gender")
    @ApiOperation(value = "按性别统计", notes = "按性别统计夜间学生出校数量")
    public ResponseEntity<Result> getStatisticsByGender(
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        try {
            Map<String, Object> statistics = nightStudentAlertService.getStatisticsByGender(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 按性别统计失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("按性别统计失败: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/college")
    @ApiOperation(value = "按学院统计", notes = "按学院统计夜间学生出校数量")
    public ResponseEntity<Result> getStatisticsByCollege(
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        try {
            Map<String, Object> statistics = nightStudentAlertService.getStatisticsByCollege(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 按学院统计失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("按学院统计失败: " + e.getMessage()));
        }
    }

    @GetMapping("/channels")
    @ApiOperation(value = "获取通道列表", notes = "获取所有可用的出口通道列表")
    public ResponseEntity<Result> getChannelList() {
        try {
            List<String> channels = nightStudentAlertService.getAllChannelNames();
            Map<String, Object> data = new HashMap<>();
            data.put("channels", channels);
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 获取通道列表失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取通道列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/colleges")
    @ApiOperation(value = "获取学院列表", notes = "获取所有已记录的学院列表")
    public ResponseEntity<Result> getCollegeList() {
        try {
            List<String> colleges = nightStudentAlertService.getAllColleges();
            Map<String, Object> data = new HashMap<>();
            data.put("colleges", colleges);
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            log.error("❌ [夜间提醒] 获取学院列表失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取学院列表失败: " + e.getMessage()));
        }
    }

    // ==================== 测试相关 ====================

    @PostMapping("/test/create")
    @ApiOperation(value = "测试创建提醒记录", notes = "用于测试WebSocket推送，不经过海康查询")
    public ResponseEntity<Result> testCreateAlert(@RequestBody NightStudentAlertRecord record) {
        try {
            log.info("🧪 [夜间提醒测试] 收到测试请求 - 姓名: {}, 学院: {}, 通道: {}",
                    record.getPersonName(), record.getCollege(), record.getChannelName());
            System.out.println("record = " + record.getEventTime());
            // 设置默认时间（如果未指定）
            if (record.getEventTime() == null || record.getEventTime().isEmpty()) {
                record.setEventTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            }

            boolean success = nightStudentAlertService.createAlertAndPush(record);
            if (success) {
                log.info("✅ [夜间提醒测试] 创建并推送成功");
                return ResponseEntity.ok(Result.success("测试记录创建成功"));
            } else {
                return ResponseEntity.ok(Result.error("测试记录创建失败"));
            }
        } catch (Exception e) {
            log.error("❌ [夜间提醒测试] 创建失败 - {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("测试记录创建失败: " + e.getMessage()));
        }
    }
}
