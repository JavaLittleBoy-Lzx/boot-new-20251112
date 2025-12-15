package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.AcmsEventRecord;
import com.parkingmanage.mapper.AcmsEventRecordMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 人脸监控数据控制器
 * 提供人脸热力图和人脸监控实时数据
 * 
 * @author System
 */
@Api(tags = "人脸监控数据")
@RestController
@RequestMapping("/parking/face-monitor")
@Slf4j
public class FaceMonitorController {

    @Autowired
    private AcmsEventRecordMapper acmsEventRecordMapper;

    /**
     * 获取人脸热力图数据 - 按位置区域统计
     * 
     * @param timeRange 时间范围：today-今日, week-本周, month-本月, year-本年度
     * @return 热力图数据（24小时 × 位置区域）
     */
    @GetMapping("/heatmap")
    @ApiOperation(value = "获取人脸热力图数据", notes = "按位置区域统计24小时人流量")
    public Result<?> getHeatmapData(
            @ApiParam(value = "时间范围", required = false) 
            @RequestParam(defaultValue = "today") String timeRange) {
        
        try {
            log.info("📊 [人脸热力图] 开始查询，时间范围: {}", timeRange);

            // 获取时间范围
            Date[] dates = getTimeRangeDates(timeRange);
            Date startDate = dates[0];
            Date endDate = dates[1];
            
            // 查询人脸识别记录
            QueryWrapper<AcmsEventRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("event_time", startDate)
                       .le("event_time", endDate)
                       .isNotNull("person_name")
                       .eq("deleted", 0)
                       .orderByAsc("event_time");
            
            List<AcmsEventRecord> records = acmsEventRecordMapper.selectList(queryWrapper);
            
            log.info("✅ [人脸热力图] 查询到 {} 条记录", records.size());
            
            // 提取所有唯一的通道名称作为位置区域
            Set<String> locationSet = records.stream()
                .map(AcmsEventRecord::getChannelName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            List<String> locations = new ArrayList<>(locationSet);
            Collections.sort(locations); // 排序保持一致性
            
            // 如果没有数据，使用默认位置
            if (locations.isEmpty()) {
                locations = Arrays.asList(
                    "1号门", "2号门", "3号门", "4号门", "5号门", 
                    "林科门", "兴安门", "银行门", "体育馆门", "其他"
                );
            }
            
            // 初始化热力图数据结构：hour × location
            Map<String, Integer> heatmapData = new HashMap<>();
            
            // 统计每个小时+位置的人数（区分进出）
            Map<String, Integer> entryHeatmap = new HashMap<>();
            Map<String, Integer> exitHeatmap = new HashMap<>();
            
            for (AcmsEventRecord record : records) {
                Date eventTime = record.getEventTime();
                if (eventTime == null) continue;
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(eventTime);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                
                String location = record.getChannelName();
                if (location == null) location = "其他";
                
                // 如果位置不在列表中，归类为"其他"
                int locationIndex = locations.indexOf(location);
                if (locationIndex < 0) {
                    if (!locations.contains("其他")) {
                        locations.add("其他");
                    }
                    locationIndex = locations.indexOf("其他");
                }
                
                String key = hour + "," + locationIndex;
                
                // 根据方向统计
                String direction = record.getDirection();
                if ("进".equals(direction)) {
                    entryHeatmap.merge(key, 1, Integer::sum);
                } else if ("出".equals(direction)) {
                    exitHeatmap.merge(key, 1, Integer::sum);
                }
                
                // 总计
                heatmapData.merge(key, 1, Integer::sum);
            }
            
            // 转换为echarts需要的格式：[[hour, locationIndex, value], ...]
            List<int[]> heatmapArray = new ArrayList<>();
            List<int[]> entryHeatmapArray = new ArrayList<>();
            List<int[]> exitHeatmapArray = new ArrayList<>();
            
            for (int hour = 0; hour < 24; hour++) {
                for (int loc = 0; loc < locations.size(); loc++) {
                    String key = hour + "," + loc;
                    int count = heatmapData.getOrDefault(key, 0);
                    int entryCount = entryHeatmap.getOrDefault(key, 0);
                    int exitCount = exitHeatmap.getOrDefault(key, 0);
                    
                    heatmapArray.add(new int[]{hour, loc, count});
                    entryHeatmapArray.add(new int[]{hour, loc, entryCount});
                    exitHeatmapArray.add(new int[]{hour, loc, exitCount});
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("heatmapData", heatmapArray);
            result.put("entryHeatmapData", entryHeatmapArray);
            result.put("exitHeatmapData", exitHeatmapArray);
            result.put("locations", locations);
            result.put("totalRecords", records.size());
            result.put("timeRange", timeRange);
            
            log.info("📊 [人脸热力图] 生成完成，位置数: {}, 总数据点: {}, 进场: {}, 出场: {}", 
                    locations.size(), heatmapArray.size(), 
                    entryHeatmapArray.stream().mapToInt(arr -> arr[2]).sum(),
                    exitHeatmapArray.stream().mapToInt(arr -> arr[2]).sum());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [人脸热力图] 查询失败", e);
            return Result.error("获取热力图数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取人脸监控列表数据（支持筛选和分页）
     * 用于详情页面的数据查询
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param direction 方向（进/出）
     * @param personName 姓名
     * @param personType 人员类型
     * @param channel 通道
     * @param phoneNo 手机号
     * @param idNumber 身份证号
     * @param organization 组织机构/学院
     * @param isReserved 是否预约（true-预约访客，false-未预约访客，null-全部）
     * @param page 页码
     * @param size 每页条数
     * @return 人脸监控数据列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取人脸监控列表", notes = "支持时间范围、方向、姓名、人员类型、通道、手机号、身份证号、学院、是否预约筛选")
    public Result<?> getList(
            @ApiParam(value = "开始时间") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String endDate,
            @ApiParam(value = "方向") @RequestParam(required = false) String direction,
            @ApiParam(value = "姓名") @RequestParam(required = false) String personName,
            @ApiParam(value = "人员类型") @RequestParam(required = false) String personType,
            @ApiParam(value = "通道") @RequestParam(required = false) String channel,
            @ApiParam(value = "手机号") @RequestParam(required = false) String phoneNo,
            @ApiParam(value = "身份证号") @RequestParam(required = false) String idNumber,
            @ApiParam(value = "组织机构/学院") @RequestParam(required = false) String organization,
            @ApiParam(value = "是否预约") @RequestParam(required = false) Boolean isReserved,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页条数") @RequestParam(defaultValue = "50000") Integer size) {
        
        try {
            log.info("📋 [人脸列表] 查询参数 - 开始时间: {}, 结束时间: {}, 方向: {}, 姓名: {}, 人员类型: {}, 通道: {}, 手机号: {}, 身份证号: {}, 学院: {}, 是否预约: {}, 页码: {}, 每页: {}",
                    startDate, endDate, direction, personName, personType, channel, phoneNo, idNumber, organization, isReserved, page, size);
            
            // 构建查询条件
            QueryWrapper<AcmsEventRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("person_name")
                       .eq("deleted", 0);
            
            // 时间范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.ge("event_time", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.le("event_time", endDate);
            }
            
            // 方向筛选
            if (direction != null && !direction.isEmpty()) {
                queryWrapper.eq("direction", direction);
            }
            
            // 姓名筛选
            if (personName != null && !personName.isEmpty()) {
                queryWrapper.like("person_name", personName);
            }
            
            // 通道筛选
            if (channel != null && !channel.isEmpty()) {
                queryWrapper.eq("channel_name", channel);
            }
            
            // 手机号筛选
            if (phoneNo != null && !phoneNo.isEmpty()) {
                queryWrapper.like("phone_no", phoneNo);
            }
            
            // 身份证号筛选
            if (idNumber != null && !idNumber.isEmpty()) {
                queryWrapper.like("id_number", idNumber);
            }
            
            // 组织机构/学院筛选 - 不在这里过滤，在后面代码中进行更精确的匹配
            // if (organization != null && !organization.isEmpty()) {
            //     queryWrapper.like("organization", organization);
            // }
            
            // 按时间倒序
            queryWrapper.orderByDesc("event_time");
            
            // 查询所有符合条件的记录
            List<AcmsEventRecord> allRecords = acmsEventRecordMapper.selectList(queryWrapper);
            
            // 根据人员类型筛选（需要在代码中过滤）
            if (personType != null && !personType.isEmpty()) {
                allRecords = allRecords.stream()
                    .filter(record -> personType.equals(determinePersonType(record)))
                    .collect(Collectors.toList());
            }
            
            // 根据是否预约筛选（需要在代码中过滤）
            if (isReserved != null) {
                allRecords = allRecords.stream()
                    .filter(record -> {
                        boolean hasReservation = record.getReservationTimeRange() != null && !record.getReservationTimeRange().isEmpty();
                        return isReserved ? hasReservation : !hasReservation;
                    })
                    .collect(Collectors.toList());
            }
            
            // 根据组织机构/学院进行更精确的过滤
            // 如果在数据库查询中已经使用了LIKE，这里再进行一次精确匹配
            // 提取学院名称（默认组织/学院名称/...格式）
            if (organization != null && !organization.isEmpty()) {
                final String searchKeyword = organization.toLowerCase();
                allRecords = allRecords.stream()
                    .filter(record -> {
                        String org = record.getOrganization();
                        if (org == null || org.isEmpty()) {
                            return false;
                        }
                        
                        // 提取学院名称部分（去除"默认组织/"前缀后的第一段）
                        String collegeName = org;
                        if (collegeName.startsWith("默认组织/")) {
                            collegeName = collegeName.substring("默认组织/".length());
                        }
                        
                        // 取第一个"/"之前的部分作为学院名称
                        int slashIndex = collegeName.indexOf("/");
                        if (slashIndex > 0) {
                            collegeName = collegeName.substring(0, slashIndex);
                        }
                        
                        // 判断学院名称是否包含搜索关键词
                        return collegeName.toLowerCase().contains(searchKeyword);
                    })
                    .collect(Collectors.toList());
            }
            
            // 计算分页
            int total = allRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<AcmsEventRecord> pagedRecords;
            if (fromIndex >= total) {
                pagedRecords = new ArrayList<>();
            } else {
                pagedRecords = allRecords.subList(fromIndex, toIndex);
            }
            
            // 转换数据格式
            List<Map<String, Object>> dataList = pagedRecords.stream().map(record -> {
                Map<String, Object> data = new HashMap<>();
                
                // 基本信息
                data.put("id", record.getId());
                data.put("personName", record.getPersonName());
                data.put("channelName", record.getChannelName());
                data.put("direction", record.getDirection());
                data.put("eventTime", record.getEventTime());
                data.put("photoUrl", record.getPhotoUrl());
                
                // 部门信息
                String department = record.getOrganization();
                if (department == null || department.isEmpty()) {
                    department = "未知部门";
                } else {
                    if (department.startsWith("默认组织/")) {
                        department = department.substring("默认组织/".length());
                    }
                    if (department.isEmpty() || "默认组织".equals(department)) {
                        department = "未知部门";
                    }
                }
                data.put("department", department);
                
                // 人员类型
                String pType = determinePersonType(record);
                data.put("personType", pType);
                
                // 识别方式
                String recognitionMethod = determineRecognitionMethod(record);
                data.put("recognitionMethod", recognitionMethod);
                
                // 其他信息
                data.put("jobNo", record.getJobNo());
                data.put("phoneNo", maskPhoneNumber(record.getPhoneNo()));
                data.put("idNumber", maskIdCard(record.getIdCard()));
                data.put("vipTypeName", record.getVipTypeName());
                
                // 预约相关信息
                data.put("reservationTimeRange", record.getReservationTimeRange());
                data.put("reservationFormName", record.getVipTypeName());  // 使用vip_type_name作为表单名称
                data.put("reservationCarPlate", record.getPlateNumber());  // 使用plate_number作为车牌号码
                
                // 判断是否为预约访客
                boolean isReservedVisitor = record.getReservationTimeRange() != null && !record.getReservationTimeRange().isEmpty();
                data.put("isReservedVisitor", isReservedVisitor);
                
                // 如果是人证比对且没有预约信息，则为纯访客
                boolean isPureVisitor = record.getEventType() != null && record.getEventType() == 197162 
                    && (record.getReservationTimeRange() == null || record.getReservationTimeRange().isEmpty());
                data.put("isPureVisitor", isPureVisitor);
                
                return data;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", dataList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            
            log.info("✅ [人脸列表] 查询成功，总数: {}, 当前页: {}, 返回: {} 条", total, page, dataList.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [人脸列表] 查询失败", e);
            return Result.error("查询人脸列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取人脸监控实时数据
     * 包含部门、人员类型（预约访客/未预约访客/教职工）、识别方式等
     * 
     * @param limit 返回记录数量，默认50条
     * @return 人脸监控数据列表
     */
    @GetMapping("/realtime")
    @ApiOperation(value = "获取人脸监控实时数据", notes = "获取最新的人脸识别记录，包含部门、人员类型、识别方式")
    public Result<?> getRealtimeData(
            @ApiParam(value = "返回记录数量", required = false) 
            @RequestParam(defaultValue = "50") Integer limit) {
        
        try {
            log.info("📡 [人脸监控] 获取实时数据，限制: {} 条", limit);
            
            // 查询最新的人脸识别记录（排除"默认组织"）
            QueryWrapper<AcmsEventRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("person_name")
                       .and(wrapper -> wrapper
                           .isNull("organization")  // organization为null，或者
                           .or()
                           .ne("organization", "默认组织"))  // organization不是"默认组织"
                       .eq("deleted", 0)
                       .orderByDesc("event_time")
                       .last("LIMIT " + limit);
            
            List<AcmsEventRecord> records = acmsEventRecordMapper.selectList(queryWrapper);
            
            // 转换数据格式
            List<Map<String, Object>> dataList = records.stream().map(record -> {
                Map<String, Object> data = new HashMap<>();
                
                // 基本信息
                data.put("id", record.getId());
                data.put("personName", record.getPersonName());
                data.put("channelName", record.getChannelName());
                data.put("direction", record.getDirection());
                data.put("eventTime", record.getEventTime());
                data.put("photoUrl", record.getPhotoUrl());
                
                // 部门信息（只去除"默认组织/"前缀，保留完整路径）
                String department = record.getOrganization();
                if (department == null || department.isEmpty()) {
                    department = "未知部门";
                } else {
                    // 如果包含"默认组织/"，去除这个前缀
                    if (department.startsWith("默认组织/")) {
                        department = department.substring("默认组织/".length());
                    }
                    // 如果结果为空或就是"默认组织"，设为未知部门
                    if (department.isEmpty() || "默认组织".equals(department)) {
                        department = "未知部门";
                    }
                }
                data.put("department", department);
                
                // 人员类型判断
                String personType = determinePersonType(record);
                data.put("personType", personType);
                
                // 识别方式
                String recognitionMethod = determineRecognitionMethod(record);
                data.put("recognitionMethod", recognitionMethod);
                
                // 识别类型文字
                data.put("recognitionType", record.getRecognitionType());
                
                // 其他信息
                data.put("jobNo", record.getJobNo());
                data.put("phoneNo", record.getPhoneNo());
                data.put("vipTypeName", record.getVipTypeName());
                data.put("reservationTimeRange", record.getReservationTimeRange());
                
                return data;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", dataList);
            result.put("total", dataList.size());
            
            log.info("✅ [人脸监控] 返回 {} 条实时数据", dataList.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [人脸监控] 获取实时数据失败", e);
            return Result.error("获取实时数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 判断人员类型
     * 
     * @param record 事件记录
     * @return 人员类型：预约访客/未预约访客/教职工/学生
     */
    private String determinePersonType(AcmsEventRecord record) {
        // 如果是未预约访客（刷身份证且is_unreserved_visitor=1）
        if (record.getIsUnreservedVisitor() != null && record.getIsUnreservedVisitor() == 1) {
            return "未预约访客";
        }
        
        // 如果有预约时间段信息，说明是预约访客
        if (record.getReservationTimeRange() != null && !record.getReservationTimeRange().isEmpty()) {
            return "预约访客";
        }
        
        // 根据organization判断是否为校内人员
        String org = record.getOrganization();
        if (org != null) {
            if (org.contains("学院") || org.contains("部门") || org.contains("处") || org.contains("中心")) {
                // 进一步判断是教职工还是学生
                if (org.contains("学生") || (record.getJobNo() != null && record.getJobNo().length() > 8)) {
                    return "学生";
                } else {
                    return "教职工";
                }
            }
        }
        
        // 默认为访客
        return "访客";
    }
    
    /**
     * 判断识别方式
     * 
     * @param record 事件记录
     * @return 识别方式：人脸识别/刷卡/刷身份证
     */
    private String determineRecognitionMethod(AcmsEventRecord record) {
        Integer eventType = record.getEventType();
        
        if (eventType == null) {
            return "未知";
        }
        
        // 197162: 人证比对（刷身份证）
        // 196893: 人脸识别
        // 198914: 刷校园卡
        if (eventType == 197162) {
            return "刷身份证";
        } else if (eventType == 196893) {
            return "人脸识别";
        } else if (eventType == 198914) {
            return "刷卡";
        } else {
            return "其他方式";
        }
    }
    
    /**
     * 手机号脱敏
     * 保留前3位和后4位，中间用****替代
     * 例如：138****5678
     * 
     * @param phoneNo 手机号
     * @return 脱敏后的手机号
     */
    private String maskPhoneNumber(String phoneNo) {
        if (phoneNo == null || phoneNo.isEmpty()) {
            return "";
        }
        
        // 如果长度小于7位，无法脱敏，直接返回
        if (phoneNo.length() < 7) {
            return phoneNo;
        }
        
        // 标准11位手机号：保留前3位和后4位
        if (phoneNo.length() == 11) {
            return phoneNo.substring(0, 3) + "****" + phoneNo.substring(7);
        }
        
        // 其他长度：保留前3位和后4位
        return phoneNo.substring(0, 3) + "****" + phoneNo.substring(phoneNo.length() - 4);
    }
    
    /**
     * 身份证号脱敏
     * 保留前6位和后4位，中间用********替代
     * 例如：110101********1234
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return "";
        }
        
        // 如果长度小于10位，无法脱敏，直接返回
        if (idCard.length() < 10) {
            return idCard;
        }
        
        // 标准18位身份证号：保留前6位和后4位
        if (idCard.length() == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }
        
        // 15位旧身份证号：保留前6位和后3位
        if (idCard.length() == 15) {
            return idCard.substring(0, 6) + "******" + idCard.substring(12);
        }
        
        // 其他长度：保留前6位和后4位
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * 根据时间范围计算开始和结束时间
     * 
     * @param timeRange 时间范围
     * @return Date数组，[0]是开始时间，[1]是结束时间
     */
    private Date[] getTimeRangeDates(String timeRange) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        LocalDate today = LocalDate.now();
        
        switch (timeRange.toLowerCase()) {
            case "week":
                // 本周：从本周一到今天
                startDateTime = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
                endDateTime = LocalDateTime.now();
                break;
                
            case "month":
                // 本月：从本月1日到今天
                startDateTime = today.withDayOfMonth(1).atStartOfDay();
                endDateTime = LocalDateTime.now();
                break;
                
            case "year":
                // 本年度：从1月1日到今天
                startDateTime = today.withDayOfYear(1).atStartOfDay();
                endDateTime = LocalDateTime.now();
                break;
                
            case "today":
            default:
                // 今日：从今天00:00到现在
                startDateTime = today.atStartOfDay();
                endDateTime = LocalDateTime.now();
                break;
        }
        
        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
        
        return new Date[]{startDate, endDate};
    }
}
