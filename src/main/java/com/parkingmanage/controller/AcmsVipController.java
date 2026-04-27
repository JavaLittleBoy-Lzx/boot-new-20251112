package com.parkingmanage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.AcmsEventRecord;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.service.AcmsEventRecordService;
import com.parkingmanage.service.AcmsVipService;
import com.parkingmanage.service.AcmsVipService.VipOwnerInfo;
import com.parkingmanage.service.AcmsVipService.VipTicketInfo;
import com.parkingmanage.service.HikvisionPersonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACMS VIP车主信息接口控制器
 * 对接ACMS外部系统的VIP相关接口
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/acms/vip")
@Api(tags = "ACMS VIP车主信息管理")
public class AcmsVipController {

    @Resource
    private AcmsVipService acmsVipService;

    @Resource
    private HikvisionPersonService hikvisionPersonService;

    @Resource
    private AcmsEventRecordService acmsEventRecordService;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    // 访客预约查询接口地址
    @Value("${visitor.reservation.api.url:http://202.118.219.92:8675}")
    private String visitorReservationApiUrl;

    // 🔔 新增：WebSocket处理器
    @Resource
    private com.parkingmanage.websocket.VehicleWebSocketHandler vehicleWebSocketHandler;

    @Resource
    private com.parkingmanage.service.VisitorReservationSyncService visitorReservationSyncService;

    @Resource
    private com.parkingmanage.mapper.VisitorReservationSyncMapper visitorReservationSyncMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.parkingmanage.service.FocusAlertService focusAlertService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.parkingmanage.service.NightStudentAlertService nightStudentAlertService;

    /**
     * 获取车主信息
     * 对应ACMS接口：GET_CUSTOMER (4.6)
     * 
     * @param request 请求参数
     * @return 车主信息
     */
    @PostMapping("/owner-info")
    @ApiOperation(value = "获取车主信息", notes = "根据车牌号查询ACMS系统中的车主信息")
    public ResponseEntity<Result> getOwnerInfo(@RequestBody OwnerInfoRequest request) {
        log.info("🔍 [ACMS-车主信息] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-车主信息] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-车主信息] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            if (ownerInfo == null) {
                log.info("📭 [ACMS-车主信息] 未找到车主信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的车主信息"));
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("ownerName", ownerInfo.getOwnerName());
            data.put("ownerPhone", ownerInfo.getOwnerPhone());
            data.put("ownerAddress", ownerInfo.getOwnerAddress());
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            log.info("✅ [ACMS-车主信息] 查询成功 - 车主: {}, 电话: {}", 
                ownerInfo.getOwnerName(), 
                ownerInfo.getOwnerPhone());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-车主信息] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("查询车主信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取VIP车辆信息
     * 对应ACMS接口：GET_VIP_CAR (4.13)
     * 
     * @param request 请求参数
     * @return VIP车辆信息
     */
    @PostMapping("/vip-ticket-info")
    @ApiOperation(value = "获取VIP车辆信息", notes = "根据车牌号查询ACMS系统中的VIP票信息")
    public ResponseEntity<Result> getVipTicketInfo(@RequestBody VipTicketRequest request) {
        log.info("🎫 [ACMS-VIP票] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-VIP票] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-VIP票] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(),
                request.getParkName()
            );
            
            if (ticketInfo == null) {
                log.info("📭 [ACMS-VIP票] 未找到VIP票信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的VIP票信息"));
            }

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("vipTypeName", ticketInfo.getVipTypeName());
            data.put("ownerName", ticketInfo.getOwnerName());
            data.put("ownerPhone", ticketInfo.getOwnerPhone());
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            log.info("✅ [ACMS-VIP票] 查询成功 - VIP类型: {}, 车主: {}", 
                ticketInfo.getVipTypeName(), 
                ticketInfo.getOwnerName());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-VIP票] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("查询VIP票信息失败: " + e.getMessage()));
        }
    }

    /**
     * 综合查询车主和VIP票信息
     * 
     * @param request 请求参数
     * @return 车主信息和VIP票信息
     */
    @PostMapping("/comprehensive-info")
    @ApiOperation(value = "综合查询车主和VIP票信息", notes = "同时查询车主信息和VIP票信息")
    public ResponseEntity<Result> getComprehensiveInfo(@RequestBody ComprehensiveRequest request) {
        log.info("🔍 [ACMS-综合查询] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-综合查询] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-综合查询] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 并行查询车主信息和VIP票信息
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            // 车主信息
            if (ownerInfo != null) {
                Map<String, Object> ownerData = new HashMap<>();
                ownerData.put("ownerName", ownerInfo.getOwnerName());
                ownerData.put("ownerPhone", ownerInfo.getOwnerPhone());
                ownerData.put("ownerAddress", ownerInfo.getOwnerAddress());
                data.put("ownerInfo", ownerData);
                data.put("hasOwnerInfo", true);
            } else {
                data.put("ownerInfo", null);
                data.put("hasOwnerInfo", false);
            }
            
            // VIP票信息
            if (ticketInfo != null) {
                Map<String, Object> ticketData = new HashMap<>();
                ticketData.put("vipTypeName", ticketInfo.getVipTypeName());
                ticketData.put("ownerName", ticketInfo.getOwnerName());
                ticketData.put("ownerPhone", ticketInfo.getOwnerPhone());
                data.put("ticketInfo", ticketData);
                data.put("hasTicketInfo", true);
            } else {
                data.put("ticketInfo", null);
                data.put("hasTicketInfo", false);
            }
            
            log.info("✅ [ACMS-综合查询] 查询完成 - 车主信息: {}, VIP票信息: {}", 
                (ownerInfo != null ? "有" : "无"), 
                (ticketInfo != null ? "有" : "无"));
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-综合查询] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("综合查询失败: " + e.getMessage()));
        }
    }

    /**
     * 融合查询VIP月票和车主详细信息
     * 先查询VIP月票信息（vip_type_name、car_owner、car_owner_phone）
     * 再查询车主详细信息（customer_department作为地址，customer_address作为车主类别）
     * 
     * @param request 请求参数
     * @return 融合后的信息
     */
    @PostMapping("/merged-info")
    @ApiOperation(value = "融合查询VIP月票和车主信息", notes = "先查VIP票，再查车主详情，返回融合数据")
    public ResponseEntity<Result> getMergedVipAndOwnerInfo(@RequestBody MergedInfoRequest request) {
        log.info("🔄 [ACMS-融合查询] 开始查询 - 车牌号: {}, 车场: {}", request.getPlateNumber(), request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [ACMS-融合查询] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-融合查询] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 第一步：查询VIP月票信息
            VipTicketInfo ticketInfo = acmsVipService.getVipTicketInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            System.out.println("ticketInfo = " + ticketInfo);
            
            // 第二步：查询车主详细信息（使用必需参数）
            VipOwnerInfo ownerInfo = acmsVipService.getOwnerInfo(
                request.getPlateNumber(), 
                request.getParkName()
            );
            System.out.println("ownerInfo = " + ownerInfo);
            
            // 第三步：如果ACMS中没有VIP月票信息，则查询访客预约信息作为补充
            List<VisitorReservationSync> visitorReservations = null;
            if (ticketInfo == null) {
                log.info("📋 [ACMS-融合查询] ACMS中无月票信息，尝试查询访客预约记录 - 车牌号: {}", request.getPlateNumber());
                visitorReservations = queryVisitorReservationsByHttp(request.getPlateNumber());
                
                if (visitorReservations != null && !visitorReservations.isEmpty()) {
                    log.info("✅ [ACMS-融合查询] 找到访客预约记录 - 车牌号: {}, 数量: {}", 
                        request.getPlateNumber(), visitorReservations.size());
                } else {
                    log.info("📭 [ACMS-融合查询] 未找到访客预约记录 - 车牌号: {}", request.getPlateNumber());
                }
            } else {
                log.info("✅ [ACMS-融合查询] ACMS中已有月票信息，跳过访客预约查询 - 车牌号: {}", request.getPlateNumber());
            }
            
            // 构建融合数据
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", request.getPlateNumber());
            data.put("parkName", request.getParkName());
            
            // VIP月票信息（优先级最高）
            if (ticketInfo != null) {
                data.put("vipTypeName", ticketInfo.getVipTypeName());      // 月票名称
                data.put("ownerName", ticketInfo.getOwnerName());          // 车主姓名（来自VIP票）
                data.put("ownerPhone", ticketInfo.getOwnerPhone());        // 车主手机号（来自VIP票）
                data.put("dataSource", "ACMS_VIP");                        // 数据来源标识
            } else {
                log.warn("📭 [ACMS-融合查询] ACMS中未找到VIP票信息");
            }
            
            // 车主详细信息（根据你的需求映射）
            if (ownerInfo != null) {
                // customer_department 作为地址
                data.put("ownerAddress", ownerInfo.getCustomerDepartment());
                
                // customer_address 作为车主类别
                data.put("ownerCategory", ownerInfo.getCustomerAddress());
                
                // 额外的详细信息
                data.put("customerCompany", ownerInfo.getCustomerCompany());
                data.put("customerRoomNumber", ownerInfo.getCustomerRoomNumber());
                
                // 如果VIP票中没有车主信息，使用车主详情中的
                if (ticketInfo == null) {
                    data.put("ownerName", ownerInfo.getOwnerName());
                    data.put("ownerPhone", ownerInfo.getOwnerPhone());
                }
            } else {
                log.warn("📭 [ACMS-融合查询] 未找到车主详细信息");
            }
            
            // 访客预约信息（只有ACMS中无月票信息时才作为补充）
            if (visitorReservations != null && !visitorReservations.isEmpty()) {
                // 添加访客预约列表
                data.put("visitorReservations", visitorReservations);
                data.put("visitorReservationCount", visitorReservations.size());
                data.put("hasVisitorReservation", true);
                
                // 因为ticketInfo为null才会查询访客预约，所以直接使用第一条预约记录
                VisitorReservationSync firstReservation = visitorReservations.get(0);
                data.put("ownerName", firstReservation.getVisitorName());
                data.put("ownerPhone", firstReservation.getVisitorPhone());
                data.put("vipTypeName", firstReservation.getVipTypeName());
                data.put("ownerCategory", "访客");
                data.put("dataSource", "VISITOR_RESERVATION");  // 数据来源标识
                log.info("📝 [ACMS-融合查询] 使用访客预约信息（ACMS无月票数据） - 姓名: {}, 电话: {}", 
                    firstReservation.getVisitorName(), firstReservation.getVisitorPhone());
            } else if (ticketInfo == null) {
                // ACMS无月票且没有访客预约
                data.put("visitorReservations", new ArrayList<>());
                data.put("visitorReservationCount", 0);
                data.put("hasVisitorReservation", false);
            }
            
            // 判断是否至少有一个数据源
            if (ticketInfo == null && ownerInfo == null && 
                (visitorReservations == null || visitorReservations.isEmpty())) {
                log.info("📭 [ACMS-融合查询] 未找到任何信息 - 车牌号: {}", request.getPlateNumber());
                return ResponseEntity.ok(Result.error("未找到该车牌的任何信息"));
            }
            
            log.info("✅ [ACMS-融合查询] 查询成功 - 数据来源: {}, 月票: {}, 车主: {}, 地址: {}, 类别: {}", 
                data.get("dataSource"),
                data.get("vipTypeName"),
                data.get("ownerName"),
                data.get("ownerAddress"),
                data.get("ownerCategory"));
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-融合查询] 查询失败 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("融合查询失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式查询车主信息（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param parkName 车场名称
     * @return 车主信息
     */
    @GetMapping("/owner-info")
    @ApiOperation(value = "GET方式获取车主信息", notes = "根据车牌号查询车主信息（GET方式）")
    public ResponseEntity<Result> getOwnerInfoByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        OwnerInfoRequest request = new OwnerInfoRequest();
        request.setPlateNumber(plateNumber);
        request.setParkName(parkName);
        return getOwnerInfo(request);
    }

    /**
     * GET方式查询VIP票信息（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param parkName 车场名称
     * @return VIP票信息
     */
    @GetMapping("/vip-ticket-info")
    @ApiOperation(value = "GET方式获取VIP票信息", notes = "根据车牌号查询VIP票信息（GET方式）")
    public ResponseEntity<Result> getVipTicketInfoByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        VipTicketRequest request = new VipTicketRequest();
        request.setPlateNumber(plateNumber);
        request.setParkName(parkName);
        return getVipTicketInfo(request);
    }

    /**
     * 获取黑名单类型列表
     * 对应ACMS接口：GET_CAR_VIP_TYPE (4.25)
     * 
     * @param request 请求参数
     * @return 黑名单类型列表
     */
    @PostMapping("/blacklist-types")
    @ApiOperation(value = "获取黑名单类型列表", notes = "从ACMS系统获取所有黑名单类型，用于下拉选择")
    public ResponseEntity<Result> getBlacklistTypes(@RequestBody BlacklistTypesRequest request) {
        log.info("📋 [ACMS-黑名单类型] 开始查询 - 车场: {}", request.getParkName());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getParkName())) {
                log.warn("⚠️ [ACMS-黑名单类型] 车场名称不能为空");
                return ResponseEntity.ok(Result.error("车场名称不能为空"));
            }
            
            // 调用服务层
            List<AcmsVipService.BlacklistTypeInfo> blacklistTypes = acmsVipService.getBlacklistTypes(request.getParkName());
            
            if (blacklistTypes == null || blacklistTypes.isEmpty()) {
                log.info("📭 [ACMS-黑名单类型] 未找到黑名单类型 - 车场: {}", request.getParkName());
                
                // 返回默认黑名单类型（兜底方案）
                Map<String, Object> data = new HashMap<>();
                data.put("blacklistTypes", getDefaultBlacklistTypes());
                data.put("isDefault", true);
                data.put("message", "ACMS系统未配置黑名单类型，已返回默认选项");
                
                return ResponseEntity.ok(Result.success(data));
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("blacklistTypes", blacklistTypes);
            data.put("isDefault", false);
            data.put("count", blacklistTypes.size());
            
            log.info("✅ [ACMS-黑名单类型] 查询成功 - 共 {} 种类型", blacklistTypes.size());
            
            return ResponseEntity.ok(Result.success(data));
            
        } catch (Exception e) {
            log.error("❌ [ACMS-黑名单类型] 查询失败 - 车场: {}", request.getParkName(), e);
            
            // 发生异常时返回默认类型
            Map<String, Object> data = new HashMap<>();
            data.put("blacklistTypes", getDefaultBlacklistTypes());
            data.put("isDefault", true);
            data.put("message", "查询失败，已返回默认选项");
            
            return ResponseEntity.ok(Result.success(data));
        }
    }

    /**
     * GET方式获取黑名单类型列表（简化接口）
     * 
     * @param parkName 车场名称
     * @return 黑名单类型列表
     */
    @GetMapping("/blacklist-types")
    @ApiOperation(value = "GET方式获取黑名单类型列表", notes = "从ACMS系统获取所有黑名单类型（GET方式）")
    public ResponseEntity<Result> getBlacklistTypesByGet(
            @ApiParam(value = "车场名称", required = true) @RequestParam String parkName) {
        
        BlacklistTypesRequest request = new BlacklistTypesRequest();
        request.setParkName(parkName);
        
        return getBlacklistTypes(request);
    }

    /**
     * 接收ACMS推送的数据
     * 用于接收ACMS系统主动推送的事件数据
     * 支持三种事件类型：
     * - 197162: 人证比对通过（刷身份证进/出）- 不需要调用查询人员信息接口
     * - 196893: 人脸识别 - 需要调用查询人员信息接口
     * - 198914: 刷校园卡 - 需要调用查询人员信息接口
     * 
     * 数据格式：
     * {
     *   "method": "OnEventNotify",
     *   "params": {
     *     "ability": "event_acs",
     *     "events": [
     *       {
     *         "data": { ... },
     *         "eventId": "...",
     *         "eventType": 197162,
     *         "happenTime": "...",
     *         "srcName": "...",
     *         ...
     *       }
     *     ]
     *   }
     * }
     * 
     * @param data 推送的JSON数据
     * @return 处理结果
     */
    @PostMapping("/eventRcv")
    @ApiOperation(value = "接收ACMS推送数据", notes = "接收ACMS系统主动推送的事件数据，根据eventType进行不同处理，支持批量事件处理")
    public ResponseEntity<Result> eventRcv(@RequestBody String data) {
        try {
            // 记录接收时间
            java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = simpleDateFormat.format(new java.util.Date());
            
            // 解析JSON数据
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSON.parseObject(data);
            String prettyData = com.alibaba.fastjson.JSON.toJSONString(jsonObject, 
                    com.alibaba.fastjson.serializer.SerializerFeature.PrettyFormat,
                    com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue);
            
            // 记录日志
//            log.info("📨 [ACMS-数据推送] {} 收到推送数据: {}", currentTime, data);
            log.debug("📨 [ACMS-数据推送] 格式化数据:\n{}", prettyData);
            
            // 检查是否是新的数据格式（包含method和params）
            com.alibaba.fastjson.JSONArray eventsArray = null;
            if (jsonObject.containsKey("method") && jsonObject.containsKey("params")) {
                // 新格式：从params.events中获取事件数组
                com.alibaba.fastjson.JSONObject params = jsonObject.getJSONObject("params");
                if (params != null) {
                    eventsArray = params.getJSONArray("events");
                }
                log.info("📋 [ACMS-数据推送] 检测到新数据格式，events数组大小: {}", 
                    eventsArray != null ? eventsArray.size() : 0);
            } else {
                // 旧格式：直接是单个事件对象，转换为数组
                eventsArray = new com.alibaba.fastjson.JSONArray();
                eventsArray.add(jsonObject);
                log.info("📋 [ACMS-数据推送] 检测到旧数据格式，转换为单个事件数组");
            }
            
            if (eventsArray == null || eventsArray.isEmpty()) {
                log.warn("⚠️ [ACMS-数据推送] 事件数组为空，跳过处理");
                return ResponseEntity.ok(Result.error("事件数组不能为空"));
            }
            
            // 处理结果统计
            int totalCount = eventsArray.size();
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            List<Map<String, Object>> processResults = new java.util.ArrayList<>();
            
            // 遍历处理每个事件
            for (int i = 0; i < eventsArray.size(); i++) {
                com.alibaba.fastjson.JSONObject eventObj = eventsArray.getJSONObject(i);
                if (eventObj == null) {
                    log.warn("⚠️ [ACMS-数据推送] 第 {} 个事件对象为空，跳过", i + 1);
                    skipCount++;
                    continue;
                }
                
                try {
                    // 从事件对象中提取eventType和eventId
                    Integer eventType = eventObj.getInteger("eventType");
                    String eventId = eventObj.getString("eventId");
                    
                    if (eventType == null) {
                        log.warn("⚠️ [ACMS-数据推送] 第 {} 个事件类型为空，跳过 - eventId: {}", i + 1, eventId);
                        skipCount++;
                        continue;
                    }
                    
                    // 只处理指定的事件类型
                    if (eventType != 197162 && eventType != 196893 && eventType != 198914) {
                        log.info("ℹ️ [ACMS-数据推送] 第 {} 个事件类型 {} 不在处理范围内，跳过 - eventId: {}", 
                            i + 1, eventType, eventId);
                        skipCount++;
                        continue;
                    }
                    
                    // 获取事件数据（从data字段中获取）
                    com.alibaba.fastjson.JSONObject eventData = eventObj.getJSONObject("data");
                    if (eventData == null) {
                        log.warn("⚠️ [ACMS-数据推送] 第 {} 个事件的data字段为空，跳过 - eventId: {}", i + 1, eventId);
                        skipCount++;
                        continue;
                    }
                    
                    // 创建事件记录对象
                    AcmsEventRecord eventRecord = new AcmsEventRecord();
                    eventRecord.setEventId(eventId);
                    eventRecord.setEventType(eventType);
//                    eventRecord.setRawData(data);
                    
                    // 根据eventType设置识别类型
                    String recognitionType = "";
                    if (eventType == 197162) {
                        recognitionType = "人证比对";
                    } else if (eventType == 196893) {
                        recognitionType = "人脸识别";
                    } else if (eventType == 198914) {
                        recognitionType = "刷校园卡";
                    }
                    eventRecord.setRecognitionType(recognitionType);
                    
                    // 从eventData中提取基本信息
                    String personId = eventData.getString("ExtEventPersonNo");
                    eventRecord.setPersonId(personId);
                    
                    // 从事件对象中提取通道名称和方向
                    String srcName = eventObj.getString("srcName");
                    eventRecord.setChannelName(srcName);
                    
                    // 根据通道名称判断进出方向
                    if (StringUtils.hasText(srcName)) {
                        // 图书馆1、2、3、4都是进
                        if (srcName.contains("图书馆1") || srcName.contains("图书馆2") 
                                || srcName.contains("图书馆3") || srcName.contains("图书馆4")) {
                            eventRecord.setDirection("进");
                        } else if (srcName.contains("出口")) {
                            eventRecord.setDirection("出");
                        } else if (srcName.contains("入口")) {
                            eventRecord.setDirection("进");
                        }
                    }
                    
                    // 解析事件时间（优先使用happenTime，其次使用ExtEventszDateTime）
                    String happenTimeStr = eventObj.getString("happenTime");
                    java.util.Date eventTime = null;
                    if (StringUtils.hasText(happenTimeStr)) {
                        try {
                            // 解析ISO 8601格式：2025-11-07T19:08:36.000+08:00
                            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(happenTimeStr);
                            eventTime = java.util.Date.from(zonedDateTime.toInstant());
                            eventRecord.setEventTime(eventTime);
                            log.info("📅 [ACMS-数据推送] 解析happenTime: {}", happenTimeStr);
                        } catch (Exception e) {
                            log.warn("⚠️ [ACMS-数据推送] happenTime解析失败: {}", happenTimeStr);
                        }
                    }
                    
                    // 根据eventType进行不同处理
                    if (eventType == 197162) {
                        // 人证比对：不需要调用查询人员信息接口，直接使用推送数据
                        log.info("🔍 [ACMS-数据推送] 人证比对事件，直接使用推送数据 - eventId: {}", eventId);
                        
                        // 从eventData中提取身份证信息
                        com.alibaba.fastjson.JSONObject idCardInfo = eventData.getJSONObject("ExtEventIdentityCardInfo");
                        if (idCardInfo != null) {
                            eventRecord.setPersonName(idCardInfo.getString("Name"));
                            eventRecord.setIdCard(idCardInfo.getString("IdNum"));
                            eventRecord.setGender(idCardInfo.getInteger("Sex") != null ? 
                                (idCardInfo.getInteger("Sex") == 1 ? "男" : "女") : null);
                            
                            // 设置组织为身份证的住址Address
                            String address = idCardInfo.getString("Address");
                            if (StringUtils.hasText(address)) {
                                eventRecord.setOrganization(address);
                            }
                            
                            // 获取身份证照片URL并添加前缀
                            String idCardPictureUrl = idCardInfo.getString("ExtEventIDCardPictureURL");
                            if (!StringUtils.hasText(idCardPictureUrl)) {
                                idCardPictureUrl = eventData.getString("ExtEventIDCardPictureURL");
                            }
                            if (StringUtils.hasText(idCardPictureUrl)) {
                                // 添加前缀，如果URL以/开头，直接拼接；否则添加/
                                String fullUrl;
                                if (idCardPictureUrl.startsWith("/")) {
                                    fullUrl = "https://10.120.11.4" + idCardPictureUrl;
                                } else {
                                    fullUrl = "https://10.120.11.4/" + idCardPictureUrl;
                                }
                                eventRecord.setPhotoUrl(fullUrl);
                                log.info("📷 [ACMS-数据推送] 身份证照片URL: {}", fullUrl);
                            }
                        }
                        
                        // 从eventData中提取其他信息
                        String extEventCardNo = eventData.getString("ExtEventCardNo");
                        if (StringUtils.hasText(extEventCardNo) && !StringUtils.hasText(eventRecord.getIdCard())) {
                            eventRecord.setIdCard(extEventCardNo);
                        }
                        
                        // 解析ExtEventszDateTime字段作为事件时间（如果happenTime解析失败）
                        if (eventTime == null) {
                            String extEventDateTimeStr = eventData.getString("ExtEventszDateTime");
                            if (StringUtils.hasText(extEventDateTimeStr)) {
                                try {
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    eventTime = sdf.parse(extEventDateTimeStr);
                                    eventRecord.setEventTime(eventTime);
                                    log.info("📅 [ACMS-数据推送] 解析ExtEventszDateTime: {}", extEventDateTimeStr);
                                } catch (Exception e) {
                                    log.warn("⚠️ [ACMS-数据推送] ExtEventszDateTime解析失败: {}", extEventDateTimeStr);
                                }
                            }
                        }
                        
                        // 根据姓名和时间范围查询访客预约记录
                        if (StringUtils.hasText(eventRecord.getPersonName()) && eventTime != null) {
                            try {
                                VisitorReservationSync reservation = 
                                    visitorReservationSyncMapper.selectByVisitorNameAndTimeRange(
                                        eventRecord.getPersonName(), eventTime);
                                
                                if (reservation != null) {
                                    // 优先使用预约记录中的手机号
                                    if (StringUtils.hasText(reservation.getVisitorPhone())) {
                                        eventRecord.setPhoneNo(reservation.getVisitorPhone());
                                        log.info("📱 [ACMS-数据推送] 从预约记录获取手机号: {} - 姓名: {}", 
                                            reservation.getVisitorPhone(), eventRecord.getPersonName());
                                    }
                                    
                                    // 优先使用预约记录中的身份证号
                                    if (StringUtils.hasText(reservation.getVisitorIdCard())) {
                                        eventRecord.setIdCard(reservation.getVisitorIdCard());
                                        log.info("🆔 [ACMS-数据推送] 从预约记录获取身份证号: {} - 姓名: {}", 
                                            reservation.getVisitorIdCard(), eventRecord.getPersonName());
                                    }
                                    
                                    // 优先使用预约记录中的所属部门
                                    if (StringUtils.hasText(reservation.getPassDep())) {
                                        eventRecord.setOrganization(reservation.getPassDep());
                                        log.info("🏢 [ACMS-数据推送] 从预约记录获取所属部门: {} - 姓名: {}", 
                                            reservation.getPassDep(), eventRecord.getPersonName());
                                    }
                                    
                                    // 优先使用预约记录中的车牌号
                                    if (StringUtils.hasText(reservation.getCarNumber())) {
                                        eventRecord.setPlateNumber(reservation.getCarNumber());
                                        log.info("🚗 [ACMS-数据推送] 从预约记录获取车牌号: {} - 姓名: {}", 
                                            reservation.getCarNumber(), eventRecord.getPersonName());
                                    }
                                    
                                    // 设置VIP类型名称
                                    if (StringUtils.hasText(reservation.getVipTypeName())) {
                                        eventRecord.setVipTypeName(reservation.getVipTypeName());
                                        log.info("✅ [ACMS-数据推送] 从预约记录获取VIP类型: {} - 姓名: {}", 
                                            reservation.getVipTypeName(), eventRecord.getPersonName());
                                    }
                                    
                                    // 设置预约时间段（使用网关通行时间）
                                    if (reservation.getGatewayTransitBeginTime() != null && reservation.getGatewayTransitEndTime() != null) {
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        String timeRange = sdf.format(reservation.getGatewayTransitBeginTime()) + "-" + sdf.format(reservation.getGatewayTransitEndTime());
                                        eventRecord.setReservationTimeRange(timeRange);
                                        log.info("📅 [ACMS-数据推送] 从预约记录获取预约时间段(网关通行时间): {} - 姓名: {}", 
                                            timeRange, eventRecord.getPersonName());
                                    }
                                    
                                    // 🔔 注释掉提前推送，改为在保存成功后推送
                                    // sendPersonEntryAlert(reservation, eventRecord, eventObj);
                                } else {
                                    log.info("ℹ️ [ACMS-数据推送] 未找到匹配的预约记录 - 姓名: {}, 时间: {}", 
                                        eventRecord.getPersonName(), eventTime);
                                }
                            } catch (Exception e) {
                                log.warn("⚠️ [ACMS-数据推送] 查询访客预约记录失败: {}", e.getMessage());
                            }
                        } else {
                            if (!StringUtils.hasText(eventRecord.getPersonName())) {
                                log.warn("⚠️ [ACMS-数据推送] 姓名为空，无法查询预约记录");
                            }
                            if (eventTime == null) {
                                log.warn("⚠️ [ACMS-数据推送] 事件时间为空，无法查询预约记录");
                            }
                        }
                        
                    } else {
                        // 人脸识别(196893)或刷校园卡(198914)：需要调用查询人员信息接口
                        log.info("🔍 [ACMS-数据推送] {}事件，需要查询人员信息 - personId: {}, eventId: {}", 
                            recognitionType, personId, eventId);
                        
                        // 检查通道名称，如果包含"公寓"或"图书馆"，则跳过人员信息查询
                        if (StringUtils.hasText(srcName) && (srcName.contains("公寓") || srcName.contains("图书馆"))) {
                            log.info("⏭️ [ACMS-数据推送] 通道名称包含公寓/图书馆，跳过人员信息查询 - 通道: {}, personId: {}", 
                                srcName, personId);
                        } else if (StringUtils.hasText(personId)) {
                            // 调用海康威视接口查询人员信息
                            HikvisionPersonService.PersonListResponse personResponse = 
                                hikvisionPersonService.queryPersonList(personId);
                            // 响应结果打印
                            log.info("[ACMS-数据推送] 查询人员信息响应结果: {}", personResponse);
                            if (personResponse != null && "0".equals(personResponse.getCode()) 
                                    && personResponse.getData() != null 
                                    && personResponse.getData().getList() != null 
                                    && !personResponse.getData().getList().isEmpty()) {
                                // 获取信息最全的一条人员信息
                                HikvisionPersonService.PersonInfo personInfo = 
                                    hikvisionPersonService.getMostCompletePerson(personResponse);
                                
                                if (personInfo != null) {
                                    // 填充人员信息（先填充查询接口的结果）
                                    eventRecord.setPersonName(personInfo.getPersonName());
                                    eventRecord.setJobNo(personInfo.getJobNo());
                                    eventRecord.setPhoneNo(personInfo.getPhoneNo());
                                    // 将性别数字转换为文字：1-男，2-女，0-未知
                                    if (personInfo.getGender() != null) {
                                        if (personInfo.getGender() == 1) {
                                            eventRecord.setGender("男");
                                        } else if (personInfo.getGender() == 2) {
                                            eventRecord.setGender("女");
                                        } else {
                                            eventRecord.setGender("未知");
                                        }
                                    } else {
                                        eventRecord.setGender(null);
                                    }
                                    eventRecord.setIdCard(personInfo.getCertificateNo());
                                    eventRecord.setOrganization(personInfo.getOrgPathName());
                                    
                                    log.info("✅ [ACMS-数据推送] 成功查询人员信息 - 姓名: {}, 工号: {}", 
                                        personInfo.getPersonName(), personInfo.getJobNo());
                                    
                                    // 查询预约记录，优先使用预约记录中的手机号、身份证号、所属部门、车牌号码
                                    if (StringUtils.hasText(eventRecord.getPersonName()) && eventTime != null) {
                                        try {
                                            VisitorReservationSync reservation = 
                                                visitorReservationSyncMapper.selectByVisitorNameAndTimeRange(
                                                    eventRecord.getPersonName(), eventTime);
                                            
                                            if (reservation != null) {
                                                // 优先使用预约记录中的手机号
                                                if (StringUtils.hasText(reservation.getVisitorPhone())) {
                                                    eventRecord.setPhoneNo(reservation.getVisitorPhone());
                                                    log.info("📱 [ACMS-数据推送] 从预约记录获取手机号: {} - 姓名: {}", 
                                                        reservation.getVisitorPhone(), eventRecord.getPersonName());
                                                }
                                                
                                                // 优先使用预约记录中的身份证号
                                                if (StringUtils.hasText(reservation.getVisitorIdCard())) {
                                                    eventRecord.setIdCard(reservation.getVisitorIdCard());
                                                    log.info("🆔 [ACMS-数据推送] 从预约记录获取身份证号: {} - 姓名: {}", 
                                                        reservation.getVisitorIdCard(), eventRecord.getPersonName());
                                                }
                                                
                                                // 优先使用预约记录中的所属部门
                                                if (StringUtils.hasText(reservation.getPassDep())) {
                                                    eventRecord.setOrganization(reservation.getPassDep());
                                                    log.info("🏢 [ACMS-数据推送] 从预约记录获取所属部门: {} - 姓名: {}", 
                                                        reservation.getPassDep(), eventRecord.getPersonName());
                                                }
                                                
                                                // 优先使用预约记录中的车牌号
                                                if (StringUtils.hasText(reservation.getCarNumber())) {
                                                    eventRecord.setPlateNumber(reservation.getCarNumber());
                                                    log.info("🚗 [ACMS-数据推送] 从预约记录获取车牌号: {} - 姓名: {}", 
                                                        reservation.getCarNumber(), eventRecord.getPersonName());
                                                }
                                                
                                                // 设置VIP类型名称
                                                if (StringUtils.hasText(reservation.getVipTypeName())) {
                                                    eventRecord.setVipTypeName(reservation.getVipTypeName());
                                                    log.info("✅ [ACMS-数据推送] 从预约记录获取VIP类型: {} - 姓名: {}", 
                                                        reservation.getVipTypeName(), eventRecord.getPersonName());
                                                }
                                                
                                                // 设置预约时间段（使用网关通行时间）
                                                if (reservation.getGatewayTransitBeginTime() != null && reservation.getGatewayTransitEndTime() != null) {
                                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                    String timeRange = sdf.format(reservation.getGatewayTransitBeginTime()) + "-" + sdf.format(reservation.getGatewayTransitEndTime());
                                                    eventRecord.setReservationTimeRange(timeRange);
                                                    log.info("📅 [ACMS-数据推送] 从预约记录获取预约时间段(网关通行时间): {} - 姓名: {}", 
                                                        timeRange, eventRecord.getPersonName());
                                                }
                                                
                                                // 🔔 注释掉提前推送，改为在保存成功后推送（人脸识别/刷校园卡场景）
                                                // sendPersonEntryAlert(reservation, eventRecord, eventObj);
                                            } else {
                                                log.info("ℹ️ [ACMS-数据推送] 未找到匹配的预约记录 - 姓名: {}, 时间: {}", 
                                                    eventRecord.getPersonName(), eventTime);
                                            }
                                        } catch (Exception e) {
                                            log.warn("⚠️ [ACMS-数据推送] 查询访客预约记录失败: {}", e.getMessage());
                                        }
                                    } else {
                                        if (!StringUtils.hasText(eventRecord.getPersonName())) {
                                            log.warn("⚠️ [ACMS-数据推送] 姓名为空，无法查询预约记录");
                                        }
                                        if (eventTime == null) {
                                            log.warn("⚠️ [ACMS-数据推送] 事件时间为空，无法查询预约记录");
                                        }
                                    }
                                } else {
                                    log.warn("⚠️ [ACMS-数据推送] 未能获取到有效的人员信息");
                                }
                            } else {
                                log.warn("⚠️ [ACMS-数据推送] 查询人员信息失败 - code: {}, msg: {}", 
                                    personResponse != null ? personResponse.getCode() : "null",
                                    personResponse != null ? personResponse.getMsg() : "unknown error");
                            }
                        } else {
                            log.warn("⚠️ [ACMS-数据推送] 人员ID为空，无法查询人员信息");
                        }
                        
                        // 解析ExtEventszDateTime字段作为事件时间（如果happenTime解析失败）
                        if (eventTime == null) {
                            String extEventDateTimeStr = eventData.getString("ExtEventszDateTime");
                            if (StringUtils.hasText(extEventDateTimeStr)) {
                                try {
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    eventTime = sdf.parse(extEventDateTimeStr);
                                    eventRecord.setEventTime(eventTime);
                                    log.info("📅 [ACMS-数据推送] 解析ExtEventszDateTime: {}", extEventDateTimeStr);
                                } catch (Exception e) {
                                    log.warn("⚠️ [ACMS-数据推送] ExtEventszDateTime解析失败: {}", extEventDateTimeStr);
                                }
                            }
                        }
                        
                        // 获取人脸照片URL并加上前缀
                        String pictureUrl = eventData.getString("ExtEventPictureURL");
                        if (StringUtils.hasText(pictureUrl)) {
                            // 添加前缀，如果URL以/开头，直接拼接；否则添加/
                            String fullUrl;
                            if (pictureUrl.startsWith("/")) {
                                fullUrl = "https://10.120.11.4" + pictureUrl;
                            } else {
                                fullUrl = "https://10.120.11.4/" + pictureUrl;
                            }
                            eventRecord.setPhotoUrl(fullUrl);
                            log.info("📷 [ACMS-数据推送] 人脸照片URL: {}", fullUrl);
                        }
                    }
                    
                    // 筛选条件：过滤不需要存储的数据
                    boolean shouldSave = true;
                    String skipReason = "";
                    
                    // 条件1：过滤通道名称包含“公寓”或“图书馆”的数据
                    if (StringUtils.hasText(eventRecord.getChannelName())) {
                        String channelName = eventRecord.getChannelName();
                        if (channelName.contains("公寓") || channelName.contains("图书馆")) {
                            shouldSave = false;
                            skipReason = "通道名称包含公寓/图书馆";
                            log.info("⛔ [ACMS-数据筛选] 跳过保存 - 原因: {}, 通道: {}, 姓名: {}, eventId: {}",
                                skipReason, channelName, eventRecord.getPersonName(), eventId);
                        }
                    }
                    
                    // 条件2：过滤同一人1分钟内的重复事件
                    if (shouldSave && eventRecord.getPersonName() != null && eventTime != null) {
                        try {
                            // 计算1分钟前的时间
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(eventTime);
                            cal.add(java.util.Calendar.MINUTE, -1);
                            java.util.Date oneMinuteAgo = cal.getTime();
                            
                            // 查询数据库：检查是否在1分钟内有同一人的记录
                            boolean hasDuplicateInOneMinute = acmsEventRecordService.checkDuplicateInTimeRange(
                                eventRecord.getPersonName(), oneMinuteAgo, eventTime);
                            
                            if (hasDuplicateInOneMinute) {
                                shouldSave = false;
                                skipReason = "同一人1分钟内有重复记录";
                                log.info("⛔ [ACMS-数据筛选] 跳过保存 - 原因: {}, 姓名: {}, 时间: {}, eventId: {}",
                                    skipReason, eventRecord.getPersonName(), 
                                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eventTime), eventId);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ [ACMS-数据筛选] 检查重复记录失败: {}", e.getMessage());
                            // 如果检查失败，仍然允许保存
                        }
                    }
                    
                    // 保存事件记录（只有通过筛选的才保存）
                    boolean saveResult = false;
                    if (shouldSave) {
                        saveResult = acmsEventRecordService.saveEventRecord(eventRecord);
                    } else {
                        // 记录被筛选掉，计入跳过数
                        skipCount++;
                    }
                    
                    // 更新预约记录的人员进出场状态（5.1, 5.2）
                    if (saveResult && eventRecord.getPersonName() != null && eventTime != null) {
                        try {
                            String direction = eventRecord.getDirection();
                            String plateNumber = eventRecord.getPlateNumber();
                            if (visitorReservationSyncService != null && direction != null) {
                                visitorReservationSyncService.handlePersonVisit(
                                    eventRecord.getPersonName(), eventTime, direction, plateNumber);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ [ACMS-数据推送] 更新预约记录的人员进出场状态失败: {}", e.getMessage());
                        }
                    }
                    
                    // 记录处理结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("eventId", eventId);
                    result.put("eventType", eventType);
                    result.put("recognitionType", recognitionType);
                    result.put("personName", eventRecord.getPersonName());
                    result.put("saveResult", saveResult);
                    processResults.add(result);
                    
                    if (saveResult) {
                        successCount++;
                        log.info("✅ [ACMS-数据推送] 事件记录保存成功 - eventId: {}, eventType: {}, personName: {}", 
                            eventId, eventType, eventRecord.getPersonName());
                        
                        // 注释：普通广播推送已移除，只保留关注提醒推送
                        
                        // 🔔 【新增】检查关注列表并发送关注提醒
                        if (focusAlertService != null && eventRecord.getIdCard() != null && eventTime != null) {
                            try {
                                // 查询预约信息（如果有）- 放宽时间范围，允许提前或延迟到达
                                VisitorReservationSync reservation = null;
                                if (eventRecord.getPersonName() != null) {
                                    // 先尝试精确匹配（在预约时间范围内）
                                    reservation = visitorReservationSyncMapper.selectByVisitorNameAndTimeRange(
                                        eventRecord.getPersonName(), eventTime);
                                    
                                    // 如果没找到，尝试根据身份证号查找最近的预约记录
                                    if (reservation == null && eventRecord.getIdCard() != null) {
                                        log.info("🔍 [关注追踪] 人员 {} 未在有效期内，尝试按身份证查找最近预约", eventRecord.getPersonName());
                                        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VisitorReservationSync> loosQuery = 
                                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                                        loosQuery.eq("visitor_id_card", eventRecord.getIdCard())
                                                .orderByDesc("create_time")
                                                .last("LIMIT 1");
                                        reservation = visitorReservationSyncMapper.selectOne(loosQuery);
                                        
                                        if (reservation != null) {
                                            log.info("✅ [关注追踪] 找到人员 {} 的历史预约记录", eventRecord.getPersonName());
                                        }
                                    } else if (reservation != null) {
                                        log.info("✅ [关注追踪] 人员 {} 在预约有效期内", eventRecord.getPersonName());
                                    }
                                }
                                
                                // 确定事件类型
                                String eventTypeStr = "进".equals(eventRecord.getDirection()) ? "entry" : "exit";
                                
                                // 确保照片URL有前缀
                                String photoUrl = eventRecord.getPhotoUrl();
                                if (photoUrl != null && !photoUrl.isEmpty() && !photoUrl.startsWith("http")) {
                                    photoUrl = "https://10.120.11.4" + (photoUrl.startsWith("/") ? photoUrl : "/" + photoUrl);
                                    log.info("📷 [关注追踪] 为人员照片添加前缀: {}", photoUrl);
                                }
                                
                                // 调用关注提醒服务
                                focusAlertService.handlePersonAlert(
                                    eventRecord.getIdCard(),
                                    eventRecord.getPersonName(),
                                    eventTypeStr,
                                    eventTime,
                                    eventRecord.getChannelName(),
                                    eventRecord.getOrganization(),
                                    eventRecord.getPhoneNo(),
                                    photoUrl,
                                    reservation
                                );
                            } catch (Exception focusEx) {
                                log.warn("⚠️ [ACMS-数据推送] 处理人员进出场关注提醒失败: 身份证={}, 错误={}",
                                    eventRecord.getIdCard(), focusEx.getMessage());
                            }
                        }

                        // 🌙 【新增】检查夜间学生出校提醒
                        if (nightStudentAlertService != null) {
                            try {
                                // 检查是否应该触发夜间学生出校提醒
                                if (nightStudentAlertService.shouldTriggerAlert(
                                        eventRecord.getOrganization(),
                                        eventRecord.getJobNo(),
                                        eventRecord.getDirection(),
                                        eventTime,
                                        eventRecord.getChannelName())) {

                                    // 创建夜间学生出校提醒记录
                                    com.parkingmanage.entity.NightStudentAlertRecord nightAlert = new com.parkingmanage.entity.NightStudentAlertRecord();
                                    nightAlert.setPersonName(eventRecord.getPersonName());
                                    nightAlert.setIdCard(eventRecord.getIdCard());
                                    nightAlert.setJobNo(eventRecord.getJobNo());
                                    nightAlert.setGender(eventRecord.getGender());
                                    // 从组织机构中提取学院名称
                                    nightAlert.setCollege(extractCollegeFromOrganization(eventRecord.getOrganization()));
                                    nightAlert.setChannelName(eventRecord.getChannelName());
                                    nightAlert.setEventTime(happenTimeStr);  // 直接用原始字符串，不转换
                                    nightAlert.setPhotoUrl(eventRecord.getPhotoUrl());

                                    // 保存记录并推送WebSocket
                                    nightStudentAlertService.createAlertAndPush(nightAlert);
                                    log.info("🌙 [夜间学生出校提醒] 触发提醒 - 姓名: {}, 学院: {}, 通道: {}, 时间: {}",
                                            eventRecord.getPersonName(), nightAlert.getCollege(),
                                            eventRecord.getChannelName(), eventTime);
                                }
                            } catch (Exception nightEx) {
                                log.warn("⚠️ [ACMS-数据推送] 处理夜间学生出校提醒失败: 姓名={}, 错误={}",
                                        eventRecord.getPersonName(), nightEx.getMessage());
                            }
                        }
                    } else {
                        failCount++;
                        log.warn("⚠️ [ACMS-数据推送] 事件记录保存失败 - eventId: {}", eventId);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ [ACMS-数据推送] 处理第 {} 个事件失败 - eventId: {}", 
                        i + 1, eventObj.getString("eventId"), e);
                    
                    // 记录失败结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("eventId", eventObj.getString("eventId"));
                    result.put("eventType", eventObj.getInteger("eventType"));
                    result.put("saveResult", false);
                    result.put("error", e.getMessage());
                    processResults.add(result);
                }
            }
            
            // 构建返回数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("receiveTime", currentTime);
            responseData.put("totalCount", totalCount);
            responseData.put("successCount", successCount);
            responseData.put("failCount", failCount);
            responseData.put("skipCount", skipCount);
            responseData.put("processResults", processResults);
            
            if (successCount > 0) {
                log.info("✅ [ACMS-数据推送] 批量处理完成 - 总数: {}, 成功: {}, 失败: {}, 跳过: {}", 
                    totalCount, successCount, failCount, skipCount);
                return ResponseEntity.ok(Result.success(responseData));
            } else if (skipCount == totalCount) {
                log.info("ℹ️ [ACMS-数据推送] 所有事件都被跳过 - 总数: {}", totalCount);
                return ResponseEntity.ok(Result.success(responseData));
            } else {
                log.warn("⚠️ [ACMS-数据推送] 所有事件处理失败 - 总数: {}, 失败: {}", totalCount, failCount);
                Result<Map<String, Object>> errorResult = Result.error("所有事件处理失败");
                errorResult.setData(responseData);
                return ResponseEntity.ok(errorResult);
            }
            
        } catch (Exception e) {
            log.error("❌ [ACMS-数据推送] 接收数据失败", e);
            return ResponseEntity.ok(Result.error("数据接收失败: " + e.getMessage()));
        }
    }

    /**
     * 查询海康威视人员信息
     * 根据人员ID查询人员详细信息
     * 
     * @param request 查询请求
     * @return 人员信息
     */
    @PostMapping("/hikvision/person-info")
    @ApiOperation(value = "查询海康威视人员信息", notes = "根据人员ID查询海康威视系统中的人员详细信息")
    public ResponseEntity<Result> queryHikvisionPersonInfo(@RequestBody QueryPersonRequest request) {
        log.info("🔍 [海康-人员查询] 开始查询 - personIds: {}", request.getPersonIds());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPersonIds())) {
                log.warn("⚠️ [海康-人员查询] 人员ID不能为空");
                return ResponseEntity.ok(Result.error("人员ID不能为空"));
            }
            
            // 调用海康威视服务
            HikvisionPersonService.PersonListResponse response = 
                hikvisionPersonService.queryPersonList(request.getPersonIds());
            
            if (response != null && "0".equals(response.getCode())) {
                int count = 0;
                if (response.getData() != null && response.getData().getList() != null) {
                    count = response.getData().getList().size();
                }
                log.info("✅ [海康-人员查询] 查询成功 - 获取 {} 条人员信息", count);
                
                Map<String, Object> data = new HashMap<>();
                if (response.getData() != null) {
                    data.put("total", response.getData().getTotal());
                    data.put("pageNo", response.getData().getPageNo());
                    data.put("pageSize", response.getData().getPageSize());
                    data.put("personList", response.getData().getList());
                } else {
                    data.put("total", 0);
                    data.put("pageNo", 1);
                    data.put("pageSize", 1000);
                    data.put("personList", new ArrayList<>());
                }
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                log.warn("⚠️ [海康-人员查询] 查询失败 - code: {}, msg: {}", 
                    response != null ? response.getCode() : "null",
                    response != null ? response.getMsg() : "unknown error");
                return ResponseEntity.ok(Result.error("查询失败: " + 
                    (response != null ? response.getMsg() : "unknown error")));
            }
            
        } catch (Exception e) {
            log.error("❌ [海康-人员查询] 查询异常", e);
            return ResponseEntity.ok(Result.error("查询异常: " + e.getMessage()));
        }
    }

    /**
     * 获取默认黑名单类型（兜底方案）
     * 当ACMS系统未配置或查询失败时返回
     */
    private List<Map<String, String>> getDefaultBlacklistTypes() {
        List<Map<String, String>> defaultTypes = new java.util.ArrayList<>();
        
        Map<String, String> type1 = new HashMap<>();
        type1.put("code", "default_violation");
        type1.put("name", "违规黑名单");
        type1.put("vipGroupType", "1");
        type1.put("vipType", "2");
        type1.put("description", "因违规停车被加入黑名单");
        defaultTypes.add(type1);
        
        Map<String, String> type2 = new HashMap<>();
        type2.put("code", "default_security");
        type2.put("name", "安全黑名单");
        type2.put("vipGroupType", "1");
        type2.put("vipType", "2");
        type2.put("description", "因安全原因被加入黑名单");
        defaultTypes.add(type2);
        
        Map<String, String> type3 = new HashMap<>();
        type3.put("code", "default_malicious");
        type3.put("name", "恶意黑名单");
        type3.put("vipGroupType", "1");
        type3.put("vipType", "2");
        type3.put("description", "因恶意行为被加入黑名单");
        defaultTypes.add(type3);
        
        return defaultTypes;
    }

    /**
     * 通过HTTP请求查询访客预约信息
     * 
     * @param carNumber 车牌号
     * @return 访客预约列表
     */
    private List<VisitorReservationSync> queryVisitorReservationsByHttp(String carNumber) {
        try {
            // URL编码车牌号
            String encodedCarNumber = URLEncoder.encode(carNumber, StandardCharsets.UTF_8.toString());
            
            // 构建请求URL
            String url = visitorReservationApiUrl + "/parking/visitor-reservation-sync/query-valid-by-car-number?carNumber=" + encodedCarNumber;
            
            log.info("🌐 [访客预约HTTP查询] 发送请求 - URL: {}", url);
            
            // 发送GET请求
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ [访客预约HTTP查询] 返回结果为空");
                return new ArrayList<>();
            }
            
            log.info("📥 [访客预约HTTP查询] 收到响应: {}", response);
            
            // 解析JSON响应
            Map<String, Object> resultMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            // 检查返回码
            String code = String.valueOf(resultMap.get("code"));
            if (!"0".equals(code)) {
                log.warn("⚠️ [访客预约HTTP查询] 接口返回错误码: {}, 消息: {}", code, resultMap.get("msg"));
                return new ArrayList<>();
            }
            
            // 提取data字段
            Object dataObj = resultMap.get("data");
            if (dataObj == null) {
                log.info("📭 [访客预约HTTP查询] data为空，无访客预约记录");
                return new ArrayList<>();
            }
            
            // 将data转换为List<VisitorReservationSync>
            List<VisitorReservationSync> reservations = objectMapper.convertValue(
                dataObj, 
                new TypeReference<List<VisitorReservationSync>>() {}
            );
            
            log.info("✅ [访客预约HTTP查询] 成功获取 {} 条访客预约记录", reservations != null ? reservations.size() : 0);
            
            return reservations != null ? reservations : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("❌ [访客预约HTTP查询] 查询失败 - 车牌号: {}, 错误: {}", carNumber, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ==================== 请求参数对象 ====================

    /**
     * 车主信息查询请求
     */
    @Data
    public static class OwnerInfoRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * VIP票查询请求
     */
    @Data
    public static class VipTicketRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 综合查询请求
     */
    @Data
    public static class ComprehensiveRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 融合查询请求
     */
    @Data
    public static class MergedInfoRequest {
        @ApiParam(value = "车牌号", required = true)
        private String plateNumber;
        
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 黑名单类型查询请求
     */
    @Data
    public static class BlacklistTypesRequest {
        @ApiParam(value = "车场名称", required = true)
        private String parkName;
    }

    /**
     * 海康威视人员查询请求
     */
    @Data
    public static class QueryPersonRequest {
        @ApiParam(value = "人员ID（多个ID用逗号分隔）", required = true, example = "32fb3b91-f823-42b6-8fca-137bff553857")
        private String personIds;
    }

    /**
     * 开通VIP月票车辆
     * 对应VEMS接口：OPEN_VIP_TICKET (4.2)
     * 
     * @param request 开通VIP票请求
     * @return 开通结果
     */
    @PostMapping("/open-vip-ticket")
    @ApiOperation(value = "开通VIP月票", notes = "向VEMS系统发送开通VIP票请求")
    public ResponseEntity<Result> openVipTicket(@RequestBody OpenVipTicketRequest request) {
        log.info("🎫 [开通VIP票] 开始处理 - 车主: {}, VIP类型: {}, 车辆数: {}", 
                request.getCarOwner(), request.getVipTypeName(), 
                request.getCarList() != null ? request.getCarList().size() : 0);
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getVipTypeName())) {
                log.warn("⚠️ [开通VIP票] VIP类型名称不能为空");
                return ResponseEntity.ok(Result.error("VIP类型名称不能为空"));
            }
            
            if (!StringUtils.hasText(request.getCarOwner())) {
                log.warn("⚠️ [开通VIP票] 车主姓名不能为空");
                return ResponseEntity.ok(Result.error("车主姓名不能为空"));
            }
            
            if (request.getCarList() == null || request.getCarList().isEmpty()) {
                log.warn("⚠️ [开通VIP票] 车辆列表不能为空");
                return ResponseEntity.ok(Result.error("车辆列表不能为空"));
            }
            
            if (request.getTimePeriodList() == null || request.getTimePeriodList().isEmpty()) {
                log.warn("⚠️ [开通VIP票] 时间段列表不能为空");
                return ResponseEntity.ok(Result.error("时间段列表不能为空"));
            }
            
            // 构建Service层请求
            AcmsVipService.OpenVipTicketRequest serviceRequest = new AcmsVipService.OpenVipTicketRequest();
            serviceRequest.setVipTypeName(request.getVipTypeName());
            serviceRequest.setTicketNo(request.getTicketNo() != null ? request.getTicketNo() : generateTicketNo());
            serviceRequest.setCarOwner(request.getCarOwner());
            serviceRequest.setTelphone(request.getTelphone());
            serviceRequest.setCompany(request.getCompany());
            serviceRequest.setDepartment(request.getDepartment());
            serviceRequest.setSex(request.getSex() != null ? request.getSex() : "0");
            serviceRequest.setOperator(request.getOperator() != null ? request.getOperator() : "系统管理员");
            serviceRequest.setOperateTime(request.getOperateTime() != null ? request.getOperateTime() : getCurrentTime());
            serviceRequest.setOriginalPrice(request.getOriginalPrice());
            serviceRequest.setDiscountPrice(request.getDiscountPrice());
            serviceRequest.setOpenValue(request.getOpenValue() != null ? request.getOpenValue() : "1");
            serviceRequest.setOpenCarCount(String.valueOf(request.getCarList().size()));
            serviceRequest.setCarList(request.getCarList());
            serviceRequest.setTimePeriodList(request.getTimePeriodList());
            
            // 调用服务层
            boolean success = acmsVipService.openVipTicketToVems(serviceRequest);
            
            if (success) {
                log.info("✅ [开通VIP票] 开通成功 - 车主: {}, VIP类型: {}", 
                        request.getCarOwner(), request.getVipTypeName());
                
                Map<String, Object> data = new HashMap<>();
                data.put("ticketNo", serviceRequest.getTicketNo());
                data.put("carOwner", request.getCarOwner());
                data.put("vipTypeName", request.getVipTypeName());
                data.put("carCount", request.getCarList().size());
                data.put("message", "VIP票开通成功");
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                log.warn("⚠️ [开通VIP票] 开通失败 - 车主: {}", request.getCarOwner());
                return ResponseEntity.ok(Result.error("VIP票开通失败，请检查VEMS系统"));
            }
            
        } catch (Exception e) {
            log.error("❌ [开通VIP票] 开通异常 - 车主: {}", request.getCarOwner(), e);
            return ResponseEntity.ok(Result.error("VIP票开通失败: " + e.getMessage()));
        }
    }

    /**
     * 生成票号
     */
    private String generateTicketNo() {
        return "TICKET_" + System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 开通VIP票请求参数
     */
    @Data
    public static class OpenVipTicketRequest {
        @ApiParam(value = "VIP类型名称", required = true, example = "月卡VIP")
        private String vipTypeName;
        
        @ApiParam(value = "票号（可选，不填则自动生成）", example = "TICKET_20250110_001")
        private String ticketNo;
        
        @ApiParam(value = "车主姓名", required = true, example = "张三")
        private String carOwner;
        
        @ApiParam(value = "联系电话", required = true, example = "13800138000")
        private String telphone;
        
        @ApiParam(value = "公司", example = "测试公司")
        private String company;
        
        @ApiParam(value = "部门", example = "技术部")
        private String department;
        
        @ApiParam(value = "性别：0-男，1-女", example = "0")
        private String sex;
        
        @ApiParam(value = "操作人（可选，默认为系统管理员）", example = "系统管理员")
        private String operator;
        
        @ApiParam(value = "操作时间（可选，默认为当前时间）", example = "2025-01-10 12:00:00")
        private String operateTime;
        
        @ApiParam(value = "原价", required = true, example = "300.00")
        private String originalPrice;
        
        @ApiParam(value = "优惠价", required = true, example = "300.00")
        private String discountPrice;
        
        @ApiParam(value = "开通值（可选，默认为1）", example = "1")
        private String openValue;
        
        @ApiParam(value = "车辆列表（车牌号）", required = true)
        private List<String> carList;
        
        @ApiParam(value = "时间段列表", required = true)
        private List<AcmsVipService.TimePeriod> timePeriodList;
    }

    /**
     * 根据车牌号批量退费VIP票
     * 查询该车牌号下vip_type_name为"二道岗可通行车辆"且ticket_status为"1"(已生效)的VIP票，并进行批量退费
     * 对应ACMS接口：REFUND_VIP_TICKET
     * 
     * @param request 退费请求
     * @return 退费结果
     */
    @PostMapping("/refund-vip-ticket")
    @ApiOperation(value = "批量退费VIP票", notes = "根据车牌号查询并退费符合条件的VIP票（vip_type_name=二道岗可通行车辆, ticket_status=1）")
    public ResponseEntity<Result> refundVipTicket(@RequestBody RefundVipTicketRequest request) {
        log.info("💰 [退费VIP票] 开始处理 - 车牌号: {}, 操作人: {}", request.getPlateNumber(), request.getOperator());
        
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getPlateNumber())) {
                log.warn("⚠️ [退费VIP票] 车牌号不能为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }
            
            // 设置默认操作人（如果未提供）
            String operator = request.getOperator();
            if (!StringUtils.hasText(operator)) {
                operator = "系统管理员";
                log.info("ℹ️ [退费VIP票] 未提供操作人，使用默认值: {}", operator);
            }
            
            // 设置默认操作时间（如果未提供）
            String operateTime = request.getOperateTime();
            if (!StringUtils.hasText(operateTime)) {
                operateTime = getCurrentTime();
                log.info("ℹ️ [退费VIP票] 未提供操作时间，使用当前时间: {}", operateTime);
            }
            
            // 调用服务层
            AcmsVipService.RefundVipTicketResult result = acmsVipService.refundVipTicketByPlateNumber(
                    request.getPlateNumber(),
                    operator,
                    operateTime,
                    request.getReason()
            );
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", result.getPlateNumber());
            data.put("successCount", result.getSuccessCount());
            data.put("failedCount", result.getFailedCount());
            data.put("successSeqs", result.getSuccessSeqs());
            data.put("failedSeqs", result.getFailedSeqs());
            data.put("message", result.getMessage());
            
            // 根据退费结果返回不同的状态
            if (result.getSuccessCount() > 0) {
                // 有成功的记录（全部成功或部分成功）
                if (result.getFailedCount() == 0) {
                    // 全部成功
                    log.info("✅ [退费VIP票] 退费成功 - 车牌号: {}, 成功: {} 条", 
                            request.getPlateNumber(), result.getSuccessCount());
                } else {
                    // 部分成功
                    log.warn("⚠️ [退费VIP票] 部分退费成功 - 车牌号: {}, 成功: {} 条, 失败: {} 条", 
                            request.getPlateNumber(), result.getSuccessCount(), result.getFailedCount());
                }
                // 有成功记录时返回success，data中包含详细信息
                return ResponseEntity.ok(Result.success(data));
            } else {
                // 全部失败或未找到符合条件的VIP票
                if (result.getFailedCount() > 0) {
                    // 全部失败
                    log.warn("⚠️ [退费VIP票] 退费失败 - 车牌号: {}, 失败: {} 条", 
                            request.getPlateNumber(), result.getFailedCount());
                } else {
                    // 未找到符合条件的VIP票
                    log.warn("⚠️ [退费VIP票] 未找到符合条件的VIP票 - 车牌号: {}", request.getPlateNumber());
                }
                // 创建错误结果，但包含data信息
                Result<Map<String, Object>> errorResult = Result.error(result.getMessage());
                errorResult.setData(data);
                return ResponseEntity.ok(errorResult);
            }
            
        } catch (Exception e) {
            log.error("❌ [退费VIP票] 退费异常 - 车牌号: {}", request.getPlateNumber(), e);
            return ResponseEntity.ok(Result.error("退费失败: " + e.getMessage()));
        }
    }

    /**
     * GET方式批量退费VIP票（简化接口）
     * 
     * @param plateNumber 车牌号
     * @param operator 操作人（可选）
     * @param operateTime 操作时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param reason 退费原因（可选）
     * @return 退费结果
     */
    @GetMapping("/refund-vip-ticket")
    @ApiOperation(value = "GET方式批量退费VIP票", notes = "根据车牌号查询并退费符合条件的VIP票（GET方式）")
    public ResponseEntity<Result> refundVipTicketByGet(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "操作人（可选，默认为系统管理员）") @RequestParam(required = false) String operator,
            @ApiParam(value = "操作时间（可选，默认为当前时间，格式：yyyy-MM-dd HH:mm:ss）") @RequestParam(required = false) String operateTime,
            @ApiParam(value = "退费原因（可选）") @RequestParam(required = false) String reason) {
        
        RefundVipTicketRequest request = new RefundVipTicketRequest();
        request.setPlateNumber(plateNumber);
        request.setOperator(operator);
        request.setOperateTime(operateTime);
        request.setReason(reason);
        
        return refundVipTicket(request);
    }

    /**
     * 退费VIP票请求参数
     */
    @Data
    public static class RefundVipTicketRequest {
        @ApiParam(value = "车牌号", required = true, example = "京A12345")
        private String plateNumber;
        
        @ApiParam(value = "操作人（可选，默认为系统管理员）", example = "系统管理员")
        private String operator;
        
        @ApiParam(value = "操作时间（可选，默认为当前时间，格式：yyyy-MM-dd HH:mm:ss）", example = "2025-01-10 12:00:00")
        private String operateTime;
        
        @ApiParam(value = "退费原因（可选）", example = "用户申请退费")
        private String reason;
    }

    /**
     * 接收ACMS推送的违规车辆数据
     * 用于接收园区卡口违规事件数据（超速、违停、逆行等）
     * 
     * 支持的事件类型参考海康威视文档：
     * - eventType: 车辆违规事件类型代码
     * 
     * 数据格式：
     * {
     *   "method": "OnEventNotify",
     *   "params": {
     *     "ability": "event_vehicle_violation",
     *     "events": [
     *       {
     *         "data": {
     *           "plateNo": "京A12345",
     *           "plateColor": 1,
     *           "violationType": "speeding",
     *           "speed": 80,
     *           "speedLimit": 40,
     *           "vehiclePictureUrl": "/pic/...",
     *           ...
     *         },
     *         "eventId": "...",
     *         "eventType": 300001,
     *         "happenTime": "2025-11-25T16:30:00.000+08:00",
     *         "srcName": "东门卡口",
     *         ...
     *       }
     *     ]
     *   }
     * }
     * 
     * @param data 推送的JSON数据
     * @return 处理结果
     */
    @PostMapping("/violationEventRcv")
    @ApiOperation(value = "接收违规车辆推送数据", notes = "接收ACMS/海康系统主动推送的违规车辆事件数据，支持批量事件处理")
    public ResponseEntity<Result> violationEventRcv(@RequestBody String data) {
        try {
            // 记录接收时间
            java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = simpleDateFormat.format(new java.util.Date());
            
            // 解析JSON数据
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSON.parseObject(data);
            
            log.info("📨 [违规车辆推送] {} 收到推送数据", currentTime);
            log.debug("📨 [违规车辆推送] 数据内容: {}", data);
            
            // 检查数据格式（支持新旧两种格式）
            com.alibaba.fastjson.JSONArray eventsArray = null;
            if (jsonObject.containsKey("method") && jsonObject.containsKey("params")) {
                // 新格式：从params.events中获取事件数组
                com.alibaba.fastjson.JSONObject params = jsonObject.getJSONObject("params");
                if (params != null) {
                    eventsArray = params.getJSONArray("events");
                }
                log.info("📋 [违规车辆推送] 检测到新数据格式，events数组大小: {}", 
                    eventsArray != null ? eventsArray.size() : 0);
            } else {
                // 旧格式：直接是单个事件对象，转换为数组
                eventsArray = new com.alibaba.fastjson.JSONArray();
                eventsArray.add(jsonObject);
                log.info("📋 [违规车辆推送] 检测到旧数据格式，转换为单个事件数组");
            }
            
            if (eventsArray == null || eventsArray.isEmpty()) {
                log.warn("⚠️ [违规车辆推送] 事件数组为空，跳过处理");
                return ResponseEntity.ok(Result.error("事件数组不能为空"));
            }
            
            // 处理结果统计
            int totalCount = eventsArray.size();
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            List<Map<String, Object>> processResults = new java.util.ArrayList<>();
            
            // 遍历处理每个事件
            for (int i = 0; i < eventsArray.size(); i++) {
                com.alibaba.fastjson.JSONObject eventObj = eventsArray.getJSONObject(i);
                if (eventObj == null) {
                    log.warn("⚠️ [违规车辆推送] 第 {} 个事件对象为空，跳过", i + 1);
                    skipCount++;
                    continue;
                }
                
                try {
                    // 从事件对象中提取基本信息
                    Integer eventType = eventObj.getInteger("eventType");
                    String eventId = eventObj.getString("eventId");
                    String srcName = eventObj.getString("srcName");
                    String happenTimeStr = eventObj.getString("happenTime");
                    
                    if (eventType == null) {
                        log.warn("⚠️ [违规车辆推送] 第 {} 个事件类型为空，跳过 - eventId: {}", i + 1, eventId);
                        skipCount++;
                        continue;
                    }
                    
                    // 解析事件时间
                    java.util.Date happenTime = null;
                    if (StringUtils.hasText(happenTimeStr)) {
                        try {
                            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(happenTimeStr);
                            happenTime = java.util.Date.from(zonedDateTime.toInstant());
                        } catch (Exception e) {
                            log.warn("⚠️ [违规车辆推送] happenTime解析失败: {}", happenTimeStr);
                        }
                    }
                    
                    // 获取事件数据
                    com.alibaba.fastjson.JSONObject eventData = eventObj.getJSONObject("data");
                    if (eventData == null) {
                        log.warn("⚠️ [违规车辆推送] 第 {} 个事件的data字段为空，跳过 - eventId: {}", i + 1, eventId);
                        skipCount++;
                        continue;
                    }
                    
                    // 提取车辆信息
                    String plateNo = eventData.getString("plateNo");
                    Integer plateColor = eventData.getInteger("plateColor");
                    String violationType = eventData.getString("violationType");
                    String violationTypeName = eventData.getString("violationTypeName");
                    Integer speed = eventData.getInteger("speed");
                    Integer speedLimit = eventData.getInteger("speedLimit");
                    Integer laneNo = eventData.getInteger("laneNo");
                    Integer vehicleType = eventData.getInteger("vehicleType");
                    String vehicleColor = eventData.getString("vehicleColor");
                    Integer direction = eventData.getInteger("direction");
                    
                    // 提取图片URL
                    String vehiclePictureUrl = eventData.getString("vehiclePictureUrl");
                    String platePictureUrl = eventData.getString("platePictureUrl");
                    String panoramaPictureUrl = eventData.getString("panoramaPictureUrl");
                    
                    // 添加图片URL前缀（如果需要）
                    if (StringUtils.hasText(vehiclePictureUrl) && !vehiclePictureUrl.startsWith("http")) {
                        vehiclePictureUrl = "https://10.120.11.4" + (vehiclePictureUrl.startsWith("/") ? "" : "/") + vehiclePictureUrl;
                    }
                    if (StringUtils.hasText(platePictureUrl) && !platePictureUrl.startsWith("http")) {
                        platePictureUrl = "https://10.120.11.4" + (platePictureUrl.startsWith("/") ? "" : "/") + platePictureUrl;
                    }
                    if (StringUtils.hasText(panoramaPictureUrl) && !panoramaPictureUrl.startsWith("http")) {
                        panoramaPictureUrl = "https://10.120.11.4" + (panoramaPictureUrl.startsWith("/") ? "" : "/") + panoramaPictureUrl;
                    }
                    
                    // 构建违规记录对象（这里简化处理，实际应该保存到数据库）
                    Map<String, Object> violationRecord = new HashMap<>();
                    violationRecord.put("eventId", eventId);
                    violationRecord.put("eventType", eventType);
                    violationRecord.put("plateNo", plateNo);
                    violationRecord.put("plateColor", plateColor);
                    violationRecord.put("violationType", violationType);
                    violationRecord.put("violationTypeName", violationTypeName);
                    violationRecord.put("location", srcName);
                    violationRecord.put("speed", speed);
                    violationRecord.put("speedLimit", speedLimit);
                    violationRecord.put("laneNo", laneNo);
                    violationRecord.put("vehicleType", vehicleType);
                    violationRecord.put("vehicleColor", vehicleColor);
                    violationRecord.put("direction", direction);
                    violationRecord.put("directionName", direction != null ? (direction == 0 ? "进" : "出") : null);
                    violationRecord.put("vehiclePictureUrl", vehiclePictureUrl);
                    violationRecord.put("platePictureUrl", platePictureUrl);
                    violationRecord.put("panoramaPictureUrl", panoramaPictureUrl);
                    violationRecord.put("happenTime", happenTime);
                    violationRecord.put("receiveTime", currentTime);
                    
                    log.info("✅ [违规车辆推送] 接收成功 - eventId: {}, 车牌: {}, 违规类型: {}, 地点: {}", 
                        eventId, plateNo, violationTypeName, srcName);
                    
                    // TODO: 这里应该调用Service保存到violation_vehicle_record表
                    // violationVehicleService.saveViolationRecord(violationRecord);
                    
                    successCount++;
                    
                    // 记录处理结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("eventId", eventId);
                    result.put("eventType", eventType);
                    result.put("plateNo", plateNo);
                    result.put("violationType", violationType);
                    result.put("saveResult", true);
                    processResults.add(result);
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ [违规车辆推送] 处理第 {} 个事件失败 - eventId: {}", 
                        i + 1, eventObj.getString("eventId"), e);
                    
                    // 记录失败结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("eventId", eventObj.getString("eventId"));
                    result.put("eventType", eventObj.getInteger("eventType"));
                    result.put("saveResult", false);
                    result.put("error", e.getMessage());
                    processResults.add(result);
                }
            }
            
            // 构建返回数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("receiveTime", currentTime);
            responseData.put("totalCount", totalCount);
            responseData.put("successCount", successCount);
            responseData.put("failCount", failCount);
            responseData.put("skipCount", skipCount);
            responseData.put("processResults", processResults);
            
            if (successCount > 0) {
                log.info("✅ [违规车辆推送] 批量处理完成 - 总数: {}, 成功: {}, 失败: {}, 跳过: {}", 
                    totalCount, successCount, failCount, skipCount);
                return ResponseEntity.ok(Result.success(responseData));
            } else if (skipCount == totalCount) {
                log.info("ℹ️ [违规车辆推送] 所有事件都被跳过 - 总数: {}", totalCount);
                return ResponseEntity.ok(Result.success(responseData));
            } else {
                log.warn("⚠️ [违规车辆推送] 所有事件处理失败 - 总数: {}, 失败: {}", totalCount, failCount);
                Result<Map<String, Object>> errorResult = Result.error("所有事件处理失败");
                errorResult.setData(responseData);
                return ResponseEntity.ok(errorResult);
            }
            
        } catch (Exception e) {
            log.error("❌ [违规车辆推送] 接收数据失败", e);
            return ResponseEntity.ok(Result.error("数据接收失败: " + e.getMessage()));
        }
    }

    /**
     * 🔔 推送人员进场提醒到前端
     * @param reservation 预约记录
     * @param eventRecord 事件记录
     * @param eventObj 原始事件JSON对象
     */
    private void sendPersonEntryAlert(VisitorReservationSync reservation, 
                                       AcmsEventRecord eventRecord,
                                       com.alibaba.fastjson.JSONObject eventObj) {
        try {
            com.alibaba.fastjson.JSONObject alert = new com.alibaba.fastjson.JSONObject(true);
            
            // 提醒类型
            alert.put("alertType", "reservation_entry");
            alert.put("type", "person");
            alert.put("timestamp", System.currentTimeMillis());
            
            // 生成唯一的 event_id（使用 UUID）
            String eventId = java.util.UUID.randomUUID().toString();
            alert.put("event_id", eventId);
            
            // 预约信息
            alert.put("id", reservation.getId());
            alert.put("visitorName", reservation.getVisitorName());
            alert.put("visitorPhone", reservation.getVisitorPhone());
            alert.put("plateNumber", reservation.getCarNumber());
            alert.put("purpose", reservation.getVipTypeName());
            alert.put("reservationStartTime", reservation.getGatewayTransitBeginTime());
            alert.put("reservationEndTime", reservation.getGatewayTransitEndTime());
            alert.put("visitedPerson", reservation.getPassName());
            alert.put("visitedDepartment", reservation.getPassDep());


            // 进场信息
            alert.put("time", eventRecord.getEventTime());
            alert.put("channel", eventRecord.getChannelName());
            alert.put("direction", eventRecord.getDirection());
            alert.put("recognitionType", eventRecord.getRecognitionType());
            
            // 进场照片（人脸照片或身份证照片）
            String imageUrl = eventRecord.getPhotoUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://10.120.11.4" + 
                              (imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl);
                }
                alert.put("imageUrl", imageUrl);
            }
            
            // 通过WebSocket推送
            vehicleWebSocketHandler.broadcastMessage(alert);
            
            log.info("🔔 [人员进场提醒] 已推送 - event_id: {}, 姓名: {}, 通道: {}, 方向: {}", 
                    eventId,
                    eventRecord.getPersonName(), 
                    eventRecord.getChannelName(),
                    eventRecord.getDirection());
            
        } catch (Exception e) {
            log.error("❌ 推送人员进场提醒失败", e);
        }
    }

    /**
     * 从组织机构字符串中提取学院名称
     * 格式示例：默认组织/计算机与控制工程学院/学生/硕士
     * 返回：计算机与控制工程学院
     *
     * @param organization 组织机构字符串
     * @return 学院名称，如果无法提取则返回原字符串
     */
    private String extractCollegeFromOrganization(String organization) {
        if (organization == null || organization.isEmpty()) {
            return organization;
        }
        String collegeName = organization;
        // 去除"默认组织/"前缀
        if (collegeName.startsWith("默认组织/")) {
            collegeName = collegeName.substring("默认组织/".length());
        }
        // 取第一个"/"之前的部分作为学院名称
        int slashIndex = collegeName.indexOf("/");
        if (slashIndex > 0) {
            collegeName = collegeName.substring(0, slashIndex);
        }
        return collegeName;
    }
} 