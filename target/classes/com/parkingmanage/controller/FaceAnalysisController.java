package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.AcmsEventRecord;
import com.parkingmanage.mapper.AcmsEventRecordMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 人脸数据分析控制器
 * 基于acms_event_record表提供人脸识别数据统计分析
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/analysis/face")
@Api(tags = "人脸数据分析")
public class FaceAnalysisController {

    @Resource
    private AcmsEventRecordMapper acmsEventRecordMapper;

    /**
     * 获取通道人流统计数据
     * 
     * @param request 请求参数
     * @return 统计数据
     */
    @PostMapping("/channel-statistics")
    @ApiOperation(value = "获取通道人流统计数据", notes = "根据时间范围获取各通道的人流统计数据")
    public ResponseEntity<Result<Map<String, Object>>> getChannelStatistics(@RequestBody ChannelStatisticsRequest request) {
        log.info("📊 [通道人流统计] 开始查询 - 时间范围: {}", request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getTimeRange())) {
                log.warn("⚠️ [通道人流统计] 时间范围不能为空");
                return ResponseEntity.ok(Result.error("时间范围不能为空"));
            }
            
            // 获取统计数据
            Map<String, Object> statistics = calculateChannelStatistics(
                request.getTimeRange(),
                request.getStartTime(),
                request.getEndTime()
            );
            
            log.info("✅ [通道人流统计] 查询成功");
            
            return ResponseEntity.ok(Result.success(statistics));
            
        } catch (Exception e) {
            log.error("❌ [通道人流统计] 查询失败", e);
            return ResponseEntity.ok(Result.error("查询统计数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取通道详细人群属性分布
     * 
     * @param request 请求参数
     * @return 详细统计数据
     */
    @PostMapping("/channel-detail")
    @ApiOperation(value = "获取通道详细人群属性分布", notes = "获取指定通道的详细人群属性分布数据")
    public ResponseEntity<Result<Map<String, Object>>> getChannelDetail(@RequestBody ChannelDetailRequest request) {
        log.info("📈 [通道详细统计] 开始查询 - 通道: {}, 时间范围: {}", request.getChannelName(), request.getTimeRange());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getChannelName())) {
                log.warn("⚠️ [通道详细统计] 通道名称不能为空");
                return ResponseEntity.ok(Result.error("通道名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getTimeRange())) {
                log.warn("⚠️ [通道详细统计] 时间范围不能为空");
                return ResponseEntity.ok(Result.error("时间范围不能为空"));
            }
            
            // 获取详细统计数据
            Map<String, Object> detailStats = calculateChannelDetail(
                request.getChannelName(),
                request.getTimeRange(),
                request.getStartTime(),
                request.getEndTime()
            );
            
            log.info("✅ [通道详细统计] 查询成功 - 通道: {}", request.getChannelName());
            
            return ResponseEntity.ok(Result.success(detailStats));
            
        } catch (Exception e) {
            log.error("❌ [通道详细统计] 查询失败 - 通道: {}", request.getChannelName(), e);
            return ResponseEntity.ok(Result.error("查询详细统计失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式获取通道人流统计数据（简化接口）
     * 
     * @param timeRange 时间范围
     * @return 统计数据
     */
    @GetMapping("/channel-statistics")
    @ApiOperation(value = "GET方式获取通道人流统计数据", notes = "根据时间范围获取统计数据（GET方式）")
    public ResponseEntity<Result<Map<String, Object>>> getChannelStatisticsByGet(
            @ApiParam(value = "时间范围", required = true) @RequestParam String timeRange) {
        
        ChannelStatisticsRequest request = new ChannelStatisticsRequest();
        request.setTimeRange(timeRange);
        
        return getChannelStatistics(request);
    }

    /**
     * GET方式获取通道详细数据（简化接口）
     * 
     * @param channelName 通道名称
     * @param timeRange 时间范围
     * @return 详细统计数据
     */
    @GetMapping("/channel-detail")
    @ApiOperation(value = "GET方式获取通道详细数据", notes = "根据通道名称和时间范围获取详细统计数据（GET方式）")
    public ResponseEntity<Result<Map<String, Object>>> getChannelDetailByGet(
            @ApiParam(value = "通道名称", required = true) @RequestParam String channelName,
            @ApiParam(value = "时间范围", required = true) @RequestParam String timeRange) {
        
        ChannelDetailRequest request = new ChannelDetailRequest();
        request.setChannelName(channelName);
        request.setTimeRange(timeRange);
        
        return getChannelDetail(request);
    }

    /**
     * 计算通道人流统计数据
     */
    private Map<String, Object> calculateChannelStatistics(String timeRange, 
                                                          String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取时间范围
            TimeRangeInfo timeInfo = getTimeRangeInfo(timeRange, startTime, endTime);
            
            // 使用计算出的时间范围
            String actualStartTime = (startTime != null) ? startTime : timeInfo.getStartTime();
            String actualEndTime = (endTime != null) ? endTime : timeInfo.getEndTime();
            
            log.info("📅 [时间范围] 实际使用: startTime={}, endTime={}", actualStartTime, actualEndTime);
            
            // 从数据库获取人脸识别数据
            QueryWrapper<AcmsEventRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("event_time", actualStartTime)
                       .le("event_time", actualEndTime)
                       .eq("deleted", 0)
                       .orderByDesc("event_time");
            List<AcmsEventRecord> eventRecords = acmsEventRecordMapper.selectList(queryWrapper);
            
            if (eventRecords == null || eventRecords.isEmpty()) {
                log.info("📭 [通道人流统计] 数据库数据为空");
                return getMockChannelStatistics();
            }
            
            log.info("✅ [数据库查询] 人脸识别数据: {} 条", eventRecords.size());
            
            // 按通道分组统计
            Map<String, ChannelStats> channelStatsMap = new HashMap<>();
            
            for (AcmsEventRecord record : eventRecords) {
                String channelName = record.getChannelName();
                if (!StringUtils.hasText(channelName)) {
                    channelName = "未知通道";
                }
                
                ChannelStats stats = channelStatsMap.computeIfAbsent(channelName, k -> {
                    ChannelStats s = new ChannelStats();
                    s.setChannelName(k);
                    s.setTotalCount(0);
                    s.setHourlyData(new HashMap<>());
                    s.setPersonTypeData(new HashMap<>());
                    return s;
                });
                
                // 统计总人数
                stats.setTotalCount(stats.getTotalCount() + 1);
                
                // 统计按小时分布
                String hour = extractHour(record.getEventTime());
                stats.getHourlyData().merge(hour, 1, Integer::sum);
                
                // 统计人群属性分布
                String personType = classifyPersonType(record.getOrganization());
                stats.getPersonTypeData().merge(personType, 1, Integer::sum);
            }
            
            // 转换为列表
            List<Map<String, Object>> channelList = channelStatsMap.values().stream()
                .map(stats -> {
                    Map<String, Object> channelData = new HashMap<>();
                    channelData.put("name", stats.getChannelName());
                    channelData.put("totalCount", stats.getTotalCount());
                    
                    // 按小时数据转换为数组
                    List<Integer> hourlyCountData = new ArrayList<>();
                    List<String> hours = Arrays.asList("00", "02", "04", "06", "08", "10", "12", "14", "16", "18", "20", "22");
                    for (String h : hours) {
                        hourlyCountData.add(stats.getHourlyData().getOrDefault(h, 0));
                    }
                    channelData.put("countData", hourlyCountData);
                    channelData.put("hours", hours);
                    
                    // 人群属性分布数据
                    List<Map<String, Object>> typeDistribution = new ArrayList<>();
                    stats.getPersonTypeData().forEach((type, count) -> {
                        Map<String, Object> typeData = new HashMap<>();
                        typeData.put("name", type);
                        typeData.put("value", count);
                        typeData.put("percent", Math.round((count * 100.0) / stats.getTotalCount()));
                        typeDistribution.add(typeData);
                    });
                    // 按数量降序排列
                    typeDistribution.sort((a, b) -> 
                        Integer.compare((Integer)b.get("value"), (Integer)a.get("value")));
                    
                    channelData.put("typeDistribution", typeDistribution);
                    
                    // 计算主要类型
                    if (!typeDistribution.isEmpty()) {
                        Map<String, Object> dominant = typeDistribution.get(0);
                        String dominantType = String.valueOf(dominant.get("name")) + String.valueOf(dominant.get("percent")) + "%";
                        channelData.put("dominantType", dominantType);
                    } else {
                        channelData.put("dominantType", "无数据");
                    }
                    
                    return channelData;
                })
                .sorted((a, b) -> Integer.compare((Integer)b.get("totalCount"), (Integer)a.get("totalCount")))
                .collect(Collectors.toList());
            
            result.put("channels", channelList);
            result.put("timeRange", timeRange);
            result.put("dataSource", "DATABASE");
            
        } catch (Exception e) {
            log.error("❌ [通道人流统计] 计算统计数据失败", e);
            return getMockChannelStatistics();
        }
        
        return result;
    }

    /**
     * 计算通道详细统计数据
     */
    private Map<String, Object> calculateChannelDetail(String channelName, String timeRange, 
                                                      String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取时间范围
            TimeRangeInfo timeInfo = getTimeRangeInfo(timeRange, startTime, endTime);
            
            // 使用计算出的时间范围
            String actualStartTime = (startTime != null) ? startTime : timeInfo.getStartTime();
            String actualEndTime = (endTime != null) ? endTime : timeInfo.getEndTime();
            
            log.info("📅 [通道详细统计] 时间范围: {} ~ {}", actualStartTime, actualEndTime);
            
            // 从数据库获取指定通道的人脸识别数据
            QueryWrapper<AcmsEventRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("channel_name", channelName)
                       .ge("event_time", actualStartTime)
                       .le("event_time", actualEndTime)
                       .eq("deleted", 0)
                       .orderByDesc("event_time");
            List<AcmsEventRecord> eventRecords = acmsEventRecordMapper.selectList(queryWrapper);
            
            if (eventRecords == null || eventRecords.isEmpty()) {
                log.info("📭 [通道详细统计] 数据库数据为空");
                return getMockChannelDetail(channelName);
            }
            
            log.info("✅ [通道详细统计] 数据: {} 条，时间范围: {}", eventRecords.size(), timeRange);
            
            // 根据时间范围进行不同的统计
            List<Integer> countData = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            Map<String, Integer> personTypeData = new HashMap<>();
            
            // 统计人群属性（所有时间范围都需要）
            for (AcmsEventRecord record : eventRecords) {
                String personType = classifyPersonType(record.getOrganization());
                personTypeData.merge(personType, 1, Integer::sum);
            }
            
            switch (timeRange.toLowerCase()) {
                case "today":
                    // 今日：24小时数据（0-23点）
                    Map<String, Integer> hourlyData = new HashMap<>();
                    for (AcmsEventRecord record : eventRecords) {
                        String hour = extractHour(record.getEventTime());
                        hourlyData.merge(hour, 1, Integer::sum);
                    }
                    for (int i = 0; i < 24; i++) {
                        labels.add(String.format("%02d:00", i));
                        countData.add(hourlyData.getOrDefault(String.format("%02d", i), 0));
                    }
                    log.info("📊 [通道详细统计] 生成24小时数据: {} 个时间点", countData.size());
                    break;
                    
                case "week":
                    // 本周：7天数据（周一到周日）
                    Map<Integer, Integer> dailyData = new HashMap<>();
                    for (AcmsEventRecord record : eventRecords) {
                        int dayOfWeek = getDayOfWeek(record.getEventTime());
                        dailyData.merge(dayOfWeek, 1, Integer::sum);
                    }
                    String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    for (int i = 0; i < 7; i++) {
                        labels.add(weekDays[i]);
                        countData.add(dailyData.getOrDefault(i + 1, 0));
                    }
                    log.info("📊 [通道详细统计] 生成7天数据: {} 个数据点", countData.size());
                    break;
                    
                case "month":
                    // 本月：每天的数据（1号到月底）
                    Map<Integer, Integer> monthlyData = new HashMap<>();
                    int daysInMonth = 31;
                    for (AcmsEventRecord record : eventRecords) {
                        int dayOfMonth = getDayOfMonth(record.getEventTime());
                        monthlyData.merge(dayOfMonth, 1, Integer::sum);
                        daysInMonth = Math.max(daysInMonth, dayOfMonth);
                    }
                    for (int i = 1; i <= daysInMonth; i++) {
                        labels.add(i + "号");
                        countData.add(monthlyData.getOrDefault(i, 0));
                    }
                    log.info("📊 [通道详细统计] 生成本月数据: {} 天", countData.size());
                    break;
                    
                case "year":
                    // 本年度：12个月数据（1月到12月）
                    Map<Integer, Integer> yearlyData = new HashMap<>();
                    for (AcmsEventRecord record : eventRecords) {
                        int month = getMonth(record.getEventTime());
                        yearlyData.merge(month, 1, Integer::sum);
                    }
                    String[] months = {"1月", "2月", "3月", "4月", "5月", "6月", 
                                      "7月", "8月", "9月", "10月", "11月", "12月"};
                    for (int i = 0; i < 12; i++) {
                        labels.add(months[i]);
                        countData.add(yearlyData.getOrDefault(i + 1, 0));
                    }
                    log.info("📊 [通道详细统计] 生成12个月数据: {} 个数据点", countData.size());
                    break;
                    
                default:
                    // 默认24小时
                    Map<String, Integer> defaultHourlyData = new HashMap<>();
                    for (AcmsEventRecord record : eventRecords) {
                        String hour = extractHour(record.getEventTime());
                        defaultHourlyData.merge(hour, 1, Integer::sum);
                    }
                    for (int i = 0; i < 24; i++) {
                        labels.add(String.format("%02d:00", i));
                        countData.add(defaultHourlyData.getOrDefault(String.format("%02d", i), 0));
                    }
            }
            
            // 人群属性分布数据
            int totalCount = eventRecords.size();
            List<Map<String, Object>> typeDistribution = personTypeData.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> typeData = new HashMap<>();
                    typeData.put("name", entry.getKey());
                    typeData.put("value", entry.getValue());
                    typeData.put("percent", Math.round((entry.getValue() * 100.0) / totalCount));
                    return typeData;
                })
                .sorted((a, b) -> Integer.compare((Integer)b.get("value"), (Integer)a.get("value")))
                .collect(Collectors.toList());
            
            result.put("channelName", channelName);
            result.put("totalCount", totalCount);
            result.put("hours", labels);  // X轴标签
            result.put("hourlyData", countData);  // Y轴数据，前端期望这个字段名
            result.put("typeDistribution", typeDistribution);
            result.put("dataSource", "DATABASE");
            result.put("timeRange", timeRange);
            
        } catch (Exception e) {
            log.error("❌ [通道详细统计] 计算详细统计数据失败", e);
            return getMockChannelDetail(channelName);
        }
        
        return result;
    }

    /**
     * 根据organization字段分类人员类型
     * 格式示例：默认组织/野生动物与自然保护地学院/学生/硕士/2024林业2
     * 
     * @param organization 组织信息
     * @return 人员类型：学生、教职工、临聘老师、外来人员
     */
    private String classifyPersonType(String organization) {
        if (!StringUtils.hasText(organization)) {
            return "外来人员";
        }
        
        // 按斜杠分割
        String[] parts = organization.split("/");
        
        // 如果没有足够的层级，返回外来人员
        if (parts.length < 3) {
            return "外来人员";
        }
        
        // 第三部分通常是人员类型
        String typeInfo = parts[2].trim();
        
        // 根据关键字判断
        if (typeInfo.contains("学生") || typeInfo.contains("研究生") || typeInfo.contains("本科生") 
            || typeInfo.contains("硕士") || typeInfo.contains("博士")) {
            return "学生";
        } else if (typeInfo.contains("教师") || typeInfo.contains("教授") || typeInfo.contains("讲师")
            || typeInfo.contains("副教授") || typeInfo.contains("教职工") || typeInfo.contains("职工")) {
            return "教职工";
        } else if (typeInfo.contains("临聘") || typeInfo.contains("临时") || typeInfo.contains("兼职")) {
            return "临聘老师";
        } else {
            return "外来人员";
        }
    }

    /**
     * 从日期中提取小时（格式：HH）
     */
    private String extractHour(Date eventTime) {
        if (eventTime == null) {
            return "00";
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventTime);
        return String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
    }
    
    /**
     * 获取星期几（1=周一, 7=周日）
     */
    private int getDayOfWeek(Date eventTime) {
        if (eventTime == null) {
            return 1;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventTime);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        // 转换为周一=1, 周日=7的格式
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }
    
    /**
     * 获取当月的第几天（1-31）
     */
    private int getDayOfMonth(Date eventTime) {
        if (eventTime == null) {
            return 1;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventTime);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 获取月份（1-12）
     */
    private int getMonth(Date eventTime) {
        if (eventTime == null) {
            return 1;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventTime);
        return calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH 从0开始
    }

    /**
     * 获取时间范围信息
     */
    private TimeRangeInfo getTimeRangeInfo(String timeRange, String startTime, String endTime) {
        TimeRangeInfo info = new TimeRangeInfo();
        info.setTimeRange(timeRange);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 如果传入了自定义时间范围，直接使用
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            info.setStartTime(startTime);
            info.setEndTime(endTime);
            return info;
        }
        
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        
        // 根据timeRange计算时间范围
        switch (timeRange.toLowerCase()) {
            case "week":
            case "weekly":
                // 本周：从本周一00:00:00到本周日23:59:59
                startDateTime = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .atTime(LocalTime.MAX);
                break;
                
            case "month":
            case "monthly":
                // 本月：从本月1日00:00:00到本月最后一天23:59:59
                startDateTime = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                break;
                
            case "year":
            case "yearly":
                // 本年度：从本年1月1日00:00:00到本年12月31日23:59:59
                startDateTime = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                endDateTime = today.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
                break;
                
            case "today":
            case "daily":
            default:
                // 今日：从今天00:00:00到今天23:59:59
                startDateTime = today.atStartOfDay();
                endDateTime = today.atTime(LocalTime.MAX);
                break;
        }
        
        info.setStartTime(startDateTime.format(formatter));
        info.setEndTime(endDateTime.format(formatter));
        
        return info;
    }

    /**
     * 获取模拟通道统计数据
     */
    private Map<String, Object> getMockChannelStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        List<String> hours = Arrays.asList("00", "02", "04", "06", "08", "10", "12", "14", "16", "18", "20", "22");
        List<Map<String, Object>> channels = new ArrayList<>();
        
        String[] channelNames = {"1号门入口", "2号门入口", "3号门入口", "1号门出口", "2号门出口"};
        
        for (String name : channelNames) {
            Map<String, Object> channel = new HashMap<>();
            channel.put("name", name);
            channel.put("totalCount", 800 + new Random().nextInt(400));
            channel.put("hours", hours);
            
            List<Integer> countData = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                countData.add(50 + new Random().nextInt(100));
            }
            channel.put("countData", countData);
            
            List<Map<String, Object>> typeDistribution = new ArrayList<>();
            typeDistribution.add(createTypeData("学生", 450));
            typeDistribution.add(createTypeData("教职工", 280));
            typeDistribution.add(createTypeData("外来人员", 180));
            typeDistribution.add(createTypeData("临聘老师", 90));
            
            channel.put("typeDistribution", typeDistribution);
            channel.put("dominantType", "学生45%");
            
            channels.add(channel);
        }
        
        result.put("channels", channels);
        result.put("timeRange", "today");
        result.put("dataSource", "MOCK");
        
        return result;
    }

    /**
     * 获取模拟通道详细数据
     */
    private Map<String, Object> getMockChannelDetail(String channelName) {
        Map<String, Object> result = new HashMap<>();
        
        List<String> hours = new ArrayList<>();
        List<Integer> countData = new ArrayList<>();
        for (int i = 6; i < 18; i++) {
            hours.add(String.format("%02d:00", i));
            countData.add(50 + new Random().nextInt(100));
        }
        
        List<Map<String, Object>> typeDistribution = new ArrayList<>();
        typeDistribution.add(createTypeData("学生", 450));
        typeDistribution.add(createTypeData("教职工", 280));
        typeDistribution.add(createTypeData("外来人员", 180));
        typeDistribution.add(createTypeData("临聘老师", 90));
        
        result.put("channelName", channelName);
        result.put("totalCount", 1000);
        result.put("hours", hours);
        result.put("countData", countData);
        result.put("typeDistribution", typeDistribution);
        result.put("dataSource", "MOCK");
        
        return result;
    }

    /**
     * 创建类型数据对象
     */
    private Map<String, Object> createTypeData(String name, int value) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("value", value);
        data.put("percent", 0); // 百分比在实际统计时计算
        return data;
    }

    // ========== 请求/响应类 ==========

    /**
     * 通道统计请求参数
     */
    @Data
    public static class ChannelStatisticsRequest {
        private String timeRange;
        private String startTime;
        private String endTime;
    }

    /**
     * 通道详细请求参数
     */
    @Data
    public static class ChannelDetailRequest {
        private String channelName;
        private String timeRange;
        private String startTime;
        private String endTime;
    }

    /**
     * 时间范围信息
     */
    @Data
    private static class TimeRangeInfo {
        private String timeRange;
        private String startTime;
        private String endTime;
    }

    /**
     * 通道统计信息
     */
    @Data
    private static class ChannelStats {
        private String channelName;
        private Integer totalCount;
        private Map<String, Integer> hourlyData;
        private Map<String, Integer> personTypeData;
    }
}
