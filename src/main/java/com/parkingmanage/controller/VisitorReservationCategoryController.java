package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.mapper.VisitorReservationSyncMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访客预约分类统计控制器
 * 统计vip_type_name字段的访客人数分布
 * 按创建时间(create_time)进行统计，而非预约开始时间(start_time)
 * 
 * @author System
 */
@Api(tags = "访客预约分类统计")
@RestController
@RequestMapping("/parking/visitor")
@Slf4j
public class VisitorReservationCategoryController {

    @Autowired
    private VisitorReservationSyncMapper visitorReservationSyncMapper;

    /**
     * 获取访客预约分类统计数据
     * 
     * @param timeRange 时间范围：today-今日, week-本周, month-本月, year-本年度
     * @return 分类统计数据
     */
    @GetMapping("/reservation-category")
    @ApiOperation(value = "获取访客预约分类统计", notes = "根据时间范围统计各类访客预约人数")
    public Result<?> getReservationCategory(
            @ApiParam(value = "时间范围", required = true) 
            @RequestParam(defaultValue = "today") String timeRange) {
        
        try {
            log.info("📊 [访客预约分类] 开始统计，时间范围: {}", timeRange);

            // 根据时间范围计算开始和结束时间
            Date[] timeRangeDates = getTimeRangeDates(timeRange);
            Date startDate = timeRangeDates[0];
            Date endDate = timeRangeDates[1];
            
            log.info("📅 [访客预约分类] 查询范围: {} 到 {}", startDate, endDate);
            
            // 查询指定时间范围内的所有预约记录（按创建时间统计）
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("create_time", startDate)
                       .le("create_time", endDate)
                       .eq("deleted", 0);
            
            List<VisitorReservationSync> reservations = visitorReservationSyncMapper.selectList(queryWrapper);
            
            log.info("✅ [访客预约分类] 查询到 {} 条{}预约", reservations.size(), getTimeRangeLabel(timeRange));
            
            // 按vip_type_name分组统计
            Map<String, Integer> categoryCount = new HashMap<>();
            
            for (VisitorReservationSync reservation : reservations) {
                String vipTypeName = reservation.getVipTypeName();
                
                // 如果vip_type_name为空，使用"未分类"
                if (!StringUtils.hasText(vipTypeName)) {
                    vipTypeName = "未分类";
                }
                
                categoryCount.merge(vipTypeName, 1, Integer::sum);
            }
            
            // 转换为列表并按数量降序排列
            List<Map<String, Object>> categories = categoryCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> category = new HashMap<>();
                    category.put("name", entry.getKey());
                    category.put("value", entry.getValue());
                    return category;
                })
                .sorted((a, b) -> Integer.compare((Integer)b.get("value"), (Integer)a.get("value")))
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("categories", categories);
            result.put("timeRange", timeRange);
            result.put("totalCount", reservations.size());
            result.put("categoryCount", categories.size());
            result.put("dataSource", "DATABASE");
            
            log.info("📊 [访客预约分类] 统计完成，共 {} 个分类", categories.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [访客预约分类] 统计失败", e);
            return Result.error("获取分类数据失败: " + e.getMessage());
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
                startDateTime = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
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
