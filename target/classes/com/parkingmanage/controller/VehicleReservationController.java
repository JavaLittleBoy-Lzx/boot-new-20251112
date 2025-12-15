package com.parkingmanage.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.entity.PaymentRecord;
import com.parkingmanage.entity.VisitorReservationSync;
import com.parkingmanage.service.ReportCarInService;
import com.parkingmanage.service.ReportCarOutService;
import com.parkingmanage.service.VehicleReservationService;
import com.parkingmanage.service.PaymentRecordService;
import com.parkingmanage.websocket.VehicleWebSocketHandler;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.Controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 李子雄
 */
@RestController
@RequestMapping(value = "/parking/nefuData", method = { RequestMethod.GET, RequestMethod.POST })
public class VehicleReservationController {

    @Resource
    private VehicleReservationService vehicleReservationService;

    @Autowired
    public AIKEConfig aikeConfig;

    @Resource
    private ReportCarInService reportCarInService;

    @Resource
    private ReportCarOutService reportCarOutService;

    @Resource
    private VehicleWebSocketHandler vehicleWebSocketHandler;

    @Resource
    private PaymentRecordService paymentRecordService;

    @Autowired
    private com.parkingmanage.service.AcmsVipService acmsVipService;

    @Resource
    private com.parkingmanage.mapper.VisitorReservationSyncMapper visitorReservationSyncMapper;

    @Autowired
    private com.parkingmanage.service.VisitorReservationSyncService visitorReservationSyncService;

    @Setter
    @Getter
    private String enterPreVipType = "";

    private Logger logger = LoggerFactory.getLogger(Controller.class);
    // 车牌正则：第一个是汉字，第二个是字母
    private static final String LICENSE_REGEX = "^[\\u4e00-\\u9fff][A-Za-z][·\\-]?[A-Za-z0-9]{5,6}$";

    private static final Pattern pattern = Pattern.compile(LICENSE_REGEX);

    public static boolean isValidLicensePlate(String plate) {
        if (plate == null || plate.isEmpty()) {
            return false;
        }
        return pattern.matcher(plate).matches();
    }

    @ApiOperation("获取具体车牌的在场车辆")
    @GetMapping("/getParkOnSiteCarByCarNo")
    public ResponseEntity getParkOnSiteCarByCarNo(String parkCodeList, String enterTimeFrom, String enterTimeTo,
            String carNo, String pageNum, String pageSize) throws ParseException {
        HashMap<String, Object> params = new HashMap<>();
        String formatEnterTimeFrom = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        params.put("parkCodeList", Arrays.asList(parkCodeList));
        params.put("enterTimeFrom", enterTimeFrom);
        params.put("enterTimeTo", enterTimeTo);
        params.put("carNo", carNo);
        params.put("pageNum", Integer.valueOf(pageNum));
        params.put("pageSize", Integer.valueOf(pageSize));
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET,
                "getParkOnSiteCar", params);
        return ResponseEntity.ok(data);
    }

    @ApiOperation("转换时间格式")
    public static String convertDateFormat(String input) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = null;
        try {
            date = inputFormat.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputFormat.format(date);
    }

    /**
     * 将各种时间格式转换为标准格式 YYYY-MM-DD HH:MM:SS
     * 
     * @param timeString 原始时间字符串
     * @return 标准格式的时间字符串
     */
    private String formatTimeToStandard(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        try {
            // 尝试解析各种可能的时间格式
            SimpleDateFormat[] inputFormats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                    new SimpleDateFormat("yyyyMMddHHmmss"),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            };

            for (SimpleDateFormat format : inputFormats) {
                try {
                    Date date = format.parse(timeString);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return outputFormat.format(date);
                } catch (ParseException e) {
                    // 继续尝试下一个格式
                }
            }

            // 如果所有格式都失败，返回当前时间
            logger.warn("无法解析时间格式: {}, 使用当前时间", timeString);
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        } catch (Exception e) {
            logger.error("时间格式转换失败: {}", e.getMessage());
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    @ApiOperation("查询单条停车记录详情")
    @GetMapping("/getParkDetail")
    public ResponseEntity getParkDetail(String parkCode, String carCode) {
        HashMap<String, Object> params = new HashMap<>();
        // enterTime格式必须是yyyy-MM-dd HH:mm:ss
        params.put("parkCode", parkCode);
        params.put("carCode", carCode);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET,
                "getParkDetail", params);
        // logger.info("调用aike接口后的查询单条停车记录详情" + data);
        return ResponseEntity.ok(data);
    }

    /**
     * 获取最新的进出场记录（合并版本）
     * 用于前端定时查询，替代WebSocket推送
     * 
     * @param limit    查询数量限制，默认50条
     * @param lastTime 上次查询的最后时间，用于增量查询（可选）
     * @return 最新的进出场记录列表
     */
    @ApiOperation("获取最新的进出场记录")
    @GetMapping("/getLatestVehicleRecords")
    public ResponseEntity<Result> getLatestVehicleRecords(
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String lastTime) {

        try {
            logger.info("🔍 查询最新进出场记录: limit={}, lastTime={}", limit, lastTime);

            // 获取当前日期的开始时间
            String todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 处理 lastTime 参数
            String queryTime;
            if (lastTime != null && !lastTime.isEmpty()) {
                try {
                    // 尝试将 lastTime 解析为毫秒时间戳（前端传来的格式）
                    long timestamp = Long.parseLong(lastTime);
                    // 转换为 LocalDateTime
                    LocalDateTime dateTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp),
                            java.time.ZoneId.systemDefault());
                    // 格式化为 MySQL DATETIME 格式
                    queryTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    logger.info("✅ 时间戳转换: {} -> {}", lastTime, queryTime);
                } catch (NumberFormatException e) {
                    // 如果不是时间戳，假设已经是标准格式，直接使用
                    queryTime = lastTime;
                    logger.info("ℹ️ 使用原始时间格式: {}", queryTime);
                }
            } else {
                // 如果没有指定lastTime，使用当天开始时间
                queryTime = todayStart;
            }

            // 查询进场记录
            QueryWrapper<ReportCarIn> carInQuery = new QueryWrapper<>();
            carInQuery.ge("create_time", queryTime)
                    .eq("deleted", 0)
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit);
            List<ReportCarIn> carInList = reportCarInService.list(carInQuery);

            // 查询离场记录
            QueryWrapper<ReportCarOut> carOutQuery = new QueryWrapper<>();
            carOutQuery.ge("create_time", queryTime)
                    .eq("deleted", 0)
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit);
            List<ReportCarOut> carOutList = reportCarOutService.list(carOutQuery);

            // 构造返回数据
            JSONArray resultArray = new JSONArray();

            // 处理进场记录
            for (ReportCarIn carIn : carInList) {
                JSONObject record = new JSONObject();
                record.put("id", carIn.getId());
                record.put("plateNumber", carIn.getCarLicenseNumber());
                record.put("channel", carIn.getEnterChannelName());
                record.put("vipName", carIn.getEnterCustomVipName());
                record.put("time", carIn.getEnterTime());
                record.put("eventType", "in");
                record.put("status", "进场");
                record.put("imageUrl", carIn.getEnterCarFullPicture());
                record.put("vipType", carIn.getEnterVipType());
                record.put("carType", carIn.getEnterCarType());
                record.put("carColor", carIn.getEnterCarLicenseColor());
                record.put("enterType", carIn.getEnterType());
                record.put("createTime", carIn.getCreateTime());
                resultArray.add(record);
            }

            // 处理离场记录
            for (ReportCarOut carOut : carOutList) {
                JSONObject record = new JSONObject();
                record.put("id", carOut.getId());
                record.put("plateNumber", carOut.getCarLicenseNumber());
                record.put("channel", carOut.getLeaveChannelName());
                record.put("vipName", carOut.getLeaveCustomVipName());
                record.put("time", carOut.getLeaveTime());
                record.put("eventType", "out");
                record.put("status", "离场");
                record.put("imageUrl", carOut.getLeaveCarFullPicture());
                record.put("vipType", carOut.getLeaveVipType());
                record.put("carType", carOut.getLeaveCarType());
                record.put("carColor", carOut.getLeaveCarLicenseColor());
                record.put("leaveType", carOut.getLeaveType());
                record.put("amountReceivable", carOut.getAmountReceivable());
                record.put("stoppingTime", carOut.getStoppingTime());
                record.put("createTime", carOut.getCreateTime());
                resultArray.add(record);
            }

            // 按创建时间倒序排序
            resultArray.sort((o1, o2) -> {
                JSONObject j1 = (JSONObject) o1;
                JSONObject j2 = (JSONObject) o2;
                Date d1 = j1.getDate("createTime");
                Date d2 = j2.getDate("createTime");
                return d2.compareTo(d1);
            });

            // 限制返回数量
            if (resultArray.size() > limit) {
                resultArray = new JSONArray(resultArray.subList(0, limit));
            }

            logger.info("✅ 查询到 {} 条进出场记录", resultArray.size());

            // 返回结果
            HashMap<String, Object> resultData = new HashMap<>();
            resultData.put("records", resultArray);
            resultData.put("total", resultArray.size());
            resultData.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return ResponseEntity.ok(Result.success(resultData));

        } catch (Exception e) {
            logger.error("❌ 查询最新进出场记录失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最新的进场记录
     * 
     * @param limit    查询数量限制，默认50条
     * @param lastTime 上次查询的最后时间，用于增量查询（可选）
     * @return 最新的进场记录列表
     */
    @ApiOperation("获取最新的进场记录")
    @GetMapping("/getLatestCarInRecords")
    public ResponseEntity<Result> getLatestCarInRecords(
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String lastTime) {

        try {
            logger.info("🔍 查询最新进场记录: limit={}, lastTime={}", limit, lastTime);

            // 获取当前日期的开始时间
            String todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 如果没有指定lastTime，使用当天开始时间
            String queryTime = (lastTime != null && !lastTime.isEmpty()) ? lastTime : todayStart;

            // 查询进场记录
            QueryWrapper<ReportCarIn> query = new QueryWrapper<>();
            query.ge("create_time", queryTime)
                    .eq("deleted", 0)
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit);

            List<ReportCarIn> carInList = reportCarInService.list(query);

            // 构造返回数据
            JSONArray resultArray = new JSONArray();
            for (ReportCarIn carIn : carInList) {
                JSONObject record = new JSONObject();
                record.put("id", carIn.getId());
                record.put("plateNumber", carIn.getCarLicenseNumber());
                record.put("channel", carIn.getEnterChannelName());
                record.put("vipName", carIn.getEnterCustomVipName());
                record.put("time", carIn.getEnterTime());
                record.put("eventType", "in");
                record.put("status", "进场");
                record.put("imageUrl", carIn.getEnterCarFullPicture());
                record.put("vipType", carIn.getEnterVipType());
                record.put("carType", carIn.getEnterCarType());
                record.put("carColor", carIn.getEnterCarLicenseColor());
                record.put("enterType", carIn.getEnterType());
                record.put("createTime", carIn.getCreateTime());
                resultArray.add(record);
            }

            logger.info("✅ 查询到 {} 条进场记录", resultArray.size());

            // 返回结果
            HashMap<String, Object> resultData = new HashMap<>();
            resultData.put("records", resultArray);
            resultData.put("total", resultArray.size());
            resultData.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return ResponseEntity.ok(Result.success(resultData));

        } catch (Exception e) {
            logger.error("❌ 查询最新进场记录失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最新的离场记录
     * 
     * @param limit    查询数量限制，默认50条
     * @param lastTime 上次查询的最后时间，用于增量查询（可选）
     * @return 最新的离场记录列表
     */
    @ApiOperation("获取最新的离场记录")
    @GetMapping("/getLatestCarOutRecords")
    public ResponseEntity<Result> getLatestCarOutRecords(
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String lastTime) {

        try {
            logger.info("🔍 查询最新离场记录: limit={}, lastTime={}", limit, lastTime);

            // 获取当前日期的开始时间
            String todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 如果没有指定lastTime，使用当天开始时间
            String queryTime = (lastTime != null && !lastTime.isEmpty()) ? lastTime : todayStart;

            // 查询离场记录
            QueryWrapper<ReportCarOut> query = new QueryWrapper<>();
            query.ge("create_time", queryTime)
                    .eq("deleted", 0)
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit);

            List<ReportCarOut> carOutList = reportCarOutService.list(query);

            // 构造返回数据
            JSONArray resultArray = new JSONArray();
            for (ReportCarOut carOut : carOutList) {
                JSONObject record = new JSONObject();
                record.put("id", carOut.getId());
                record.put("plateNumber", carOut.getCarLicenseNumber());
                record.put("channel", carOut.getLeaveChannelName());
                record.put("vipName", carOut.getLeaveCustomVipName());
                record.put("time", carOut.getLeaveTime());
                record.put("eventType", "out");
                record.put("status", "离场");
                record.put("imageUrl", carOut.getLeaveCarFullPicture());
                record.put("vipType", carOut.getLeaveVipType());
                record.put("carType", carOut.getLeaveCarType());
                record.put("carColor", carOut.getLeaveCarLicenseColor());
                record.put("leaveType", carOut.getLeaveType());
                record.put("amountReceivable", carOut.getAmountReceivable());
                record.put("stoppingTime", carOut.getStoppingTime());
                record.put("enterTime", carOut.getEnterTime());
                record.put("createTime", carOut.getCreateTime());
                resultArray.add(record);
            }

            logger.info("✅ 查询到 {} 条离场记录", resultArray.size());

            // 返回结果
            HashMap<String, Object> resultData = new HashMap<>();
            resultData.put("records", resultArray);
            resultData.put("total", resultArray.size());
            resultData.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return ResponseEntity.ok(Result.success(resultData));

        } catch (Exception e) {
            logger.error("❌ 查询最新离场记录失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * AKE进场上报
     *
     * @param body
     * @return
     */
    @PostMapping(value = "/reportCarIn", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<JSONObject> reportCarIn(@RequestBody String body) {
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(body);
            // 对JSON中的URL编码字符串进行解码
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                if (bizContent != null) {
                    // 解码所有可能包含中文的字段
                    decodeUrlEncodedFields(bizContent);
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 无法解析请求体为JSON，body={}", body);
        }
        // logger.info("进场数据（已解码）= " + data);

        // 通过WebSocket推送进场数据到前端
        if (data != null && data.containsKey("biz_content")) {
            JSONObject bizContent = data.getJSONObject("biz_content");
            if (bizContent != null) {
                // 获取车牌号码
                String plateNumber = bizContent.getString("car_license_number");
                if (plateNumber == null || plateNumber.isEmpty()) {
                    plateNumber = bizContent.getString("enter_car_license_number");
                }

                // 过滤未识别的车牌号码
                if (plateNumber != null && !plateNumber.equals("未识别") && !plateNumber.isEmpty()) {
                    // 写入进场数据到数据库
                    boolean saveResult = false;
                    try {
                        saveResult = saveCarInData(bizContent);
                        if (saveResult) {
                            logger.info("✅ 成功写入进场数据到数据库: 车牌={}", plateNumber);
                        } else {
                            logger.warn("⚠️ 跳过写入进场数据(可能重复): 车牌={}", plateNumber);
                        }
                    } catch (Exception e) {
                        logger.error("❌ 写入进场数据失败: 车牌={}, 错误={}", plateNumber, e.getMessage());
                    }
                    
                    // 🔔 只有保存成功后才执行后续操作(参考eventRcv的处理模式)
                    if (saveResult) {
                        String enterTime = bizContent.getString("enter_time");
                        
                        // 检查是否需要调用接口添加访客
                        checkAndAddVisitorIfNeeded(plateNumber, bizContent.getString("enter_channel_name"), enterTime);
                        
                        // 🔔 更新预约记录并推送提醒
                        if (visitorReservationSyncService != null && enterTime != null) {
                            // 先调用原有的处理方法
                            try {
                                visitorReservationSyncService.handleCarIn(plateNumber, enterTime);
                            } catch (Exception ex) {
                                logger.warn("⚠️ 更新预约记录的车辆进场状态失败: 车牌={}, 错误={}", plateNumber, ex.getMessage());
                            }
                            
                            // 然后查询是否匹配到预约记录并推送提醒
                            try {
                                // 根据车牌号和当前时间查询预约记录
                                java.util.Date currentTime = new java.util.Date(System.currentTimeMillis());
                                
                                // 使用查询包装器根据车牌号查询预约记录
                                QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
                                queryWrapper.eq("car_number", plateNumber)  // 车牌号匹配
                                          .le("start_time", currentTime)      // 预约开始时间 <= 当前时间
                                          .ge("end_time", currentTime)        // 预约结束时间 >= 当前时间
                                          // deleted=0 会由 @TableLogic 自动处理，无需手动添加
                                          .orderByDesc("create_time")          // 按创建时间倒序
                                          .last("LIMIT 1");                    // 只取最新一条
                                
                                VisitorReservationSync reservation = visitorReservationSyncMapper.selectOne(queryWrapper);
                                
                                // 推送WebSocket提醒（如果有匹配的预约）
                                if (reservation != null) {
                                    logger.info("📢 [车辆进场] 准备推送车辆进场提醒 - 车牌={}, 预约人={}", plateNumber, reservation.getVisitorName());
                                    sendVehicleEntryAlert(reservation, bizContent);
                                } else {
                                    logger.debug("ℹ️ 未找到车牌 {} 的有效预约记录", plateNumber);
                                }
                            } catch (Exception queryEx) {
                                logger.warn("⚠️ 推送车辆进场提醒失败: 车牌={}, 错误={}", plateNumber, queryEx.getMessage());
                            }
                        }
                    }
                } else {
                    logger.info("⚠️ 跳过未识别车牌号码的进场数据: {}", plateNumber);
                }
            }
        }

        // 构建响应JSON，使用JSONObject(true)确保字段顺序，按照要求的顺序逐个添加字段
        JSONObject response = new JSONObject(true);
        response.put("command", "REPORT_CAR_IN_LIST");
        response.put("message_id", "vems");
        
        // 获取device_id，从请求数据中获取，如果没有则使用默认值
        String deviceId = "0000000000000000000000000000vems";
        if (data != null && data.containsKey("device_id")) {
            deviceId = data.getString("device_id");
        }
        response.put("device_id", deviceId);
        
        response.put("sign_type", "MD5");
        response.put("sign", "f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        response.put("charset", "UTF-8");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        JSONObject bizContent = new JSONObject(true);
        bizContent.put("code", "0");
        bizContent.put("msg", "ok");
        response.put("biz_content", bizContent);
        
//        logger.info("📤 进场上报响应: {}", response.toJSONString());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * 解码URL编码的字段
     * 
     * @param bizContent 业务内容JSON对象
     */
    private void decodeUrlEncodedFields(JSONObject bizContent) {
        // 需要解码的字段列表 - 扩展更多可能的字段
        String[] fieldsToDecode = {
                "car_license_number",
                "enter_custom_vip_name",
                "enter_car_license_number",
                "enter_channel_name",
                "leave_channel_name",
                "leave_car_license_number",
                "leave_custom_vip_name",
                "last_correct_license_number",
                "last_correct_name",
                "car_license_number_plate",
                "enter_car_license_number_plate",
                "leave_car_license_number_plate",
                "custom_vip_name",
                "enter_custom_vip_name",
                "leave_custom_vip_name",
                "channel_name",
                "enter_channel_name",
                "leave_channel_name",
                "park_name",
                "enter_park_name",
                "leave_park_name",
                "operator_name",
                "enter_operator_name",
                "leave_operator_name",
                "remark",
                "enter_remark",
                "leave_remark",
                "description",
                "enter_description",
                "leave_description"
        };

        // 使用递归方法处理所有字段和嵌套结构
        decodeNestedObjects(bizContent);
    }

    /**
     * 检测字符串是否包含URL编码
     * 
     * @param str 待检测的字符串
     * @return 如果包含URL编码返回true
     */
    private boolean isUrlEncoded(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 检测是否包含百分号编码模式（%XX格式）
        // 支持多种URL编码模式：%XX, %uXXXX, %XX%XX等
        return str.contains("%") && (str.matches(".*%[0-9A-Fa-f]{2}.*") || // 标准URL编码 %XX
                str.matches(".*%u[0-9A-Fa-f]{4}.*") || // Unicode编码 %uXXXX
                str.matches(".*%[0-9A-Fa-f]{2}%[0-9A-Fa-f]{2}.*") // 连续编码
        );
    }

    /**
     * 安全解码URL编码字符串
     * 
     * @param encodedStr 编码的字符串
     * @return 解码后的字符串，如果解码失败返回原字符串
     */
    private String safeUrlDecode(String encodedStr) {
        if (encodedStr == null || encodedStr.isEmpty()) {
            return encodedStr;
        }

        try {
            // 先尝试标准URL解码
            String decoded = URLDecoder.decode(encodedStr, StandardCharsets.UTF_8.toString());

            // 如果解码后仍然包含编码字符，尝试多次解码
            int maxAttempts = 3;
            int attempts = 0;
            while (isUrlEncoded(decoded) && attempts < maxAttempts) {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.toString());
                attempts++;
            }

            return decoded;
        } catch (Exception e) {
            logger.warn("URL解码失败，返回原字符串: {}", e.getMessage());
            return encodedStr;
        }
    }

    /**
     * 保存进场数据到数据库
     * 
     * @param bizContent 业务内容JSON对象
     * @return 保存是否成功，重复数据返回false，保存成功返回true
     */
    private boolean saveCarInData(JSONObject bizContent) {
        try {
            // 检查是否已存在相同的进场记录（基于车牌号码和进场时间）
            String carLicenseNumber = bizContent.getString("car_license_number");
            if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
                carLicenseNumber = bizContent.getString("enter_car_license_number");
            }
            String enterTime = bizContent.getString("enter_time");

            // 检查重复数据
            if (isDuplicateCarIn(carLicenseNumber, enterTime)) {
                logger.info("⚠️ 跳过重复的进场数据: 车牌={}, 时间={}", carLicenseNumber, enterTime);
                return false;
            }

            // 创建ReportCarIn实体
            ReportCarIn reportCarIn = new ReportCarIn();
            reportCarIn.setCarLicenseNumber(carLicenseNumber);
            reportCarIn.setEnterChannelName(bizContent.getString("enter_channel_name"));
            reportCarIn.setEnterTime(enterTime);

            // 转换进出类型数字为文字
            String enterType = convertEnterLeaveTypeToString(bizContent.getString("enter_type"));
            reportCarIn.setEnterType(enterType);

            // 转换VIP类型数字为文字
            String enterVipType = convertVipTypeToString(bizContent.getString("enter_vip_type"));
            reportCarIn.setEnterVipType(enterVipType);

            // 转换车牌颜色数字为文字
            String enterCarLicenseColor = convertCarLicenseColorToString(
                    bizContent.getString("enter_car_license_color"));
            reportCarIn.setEnterCarLicenseColor(enterCarLicenseColor);

            // 转换车辆类型数字为文字
            String enterCarType = convertCarTypeToString(bizContent.getString("enter_car_type"));
            reportCarIn.setEnterCarType(enterCarType);

            reportCarIn.setEnterCustomVipName(bizContent.getString("enter_custom_vip_name"));
            // 设置进场车辆全图（添加URL前缀）
            String enterCarFullPicture = addImageUrlPrefix(bizContent.getString("enter_car_full_picture"));
            reportCarIn.setEnterCarFullPicture(enterCarFullPicture);
            // 保存到数据库
            reportCarInService.save(reportCarIn);
            logger.info("✅ 成功保存进场数据: 车牌={}, 时间={}", carLicenseNumber, enterTime);
            return true;

        } catch (Exception e) {
            logger.error("❌ 保存进场数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查进场数据是否重复
     * 
     * @param carLicenseNumber 车牌号码
     * @param enterTime        进场时间
     * @return 是否重复
     */
    private boolean isDuplicateCarIn(String carLicenseNumber, String enterTime) {
        try {
            // 使用QueryWrapper查询是否存在相同的记录
            QueryWrapper<ReportCarIn> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_license_number", carLicenseNumber)
                    .eq("enter_time", enterTime)
                    .eq("deleted", 0);
            return reportCarInService.count(queryWrapper) > 0;
        } catch (Exception e) {
            logger.error("检查进场数据重复性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 保存离场数据到数据库
     * 
     * @param bizContent 业务内容JSON对象
     */
    private void saveCarOutData(JSONObject bizContent) {
        try {
            // 检查是否已存在相同的离场记录（基于车牌号码和离场时间）
            String carLicenseNumber = bizContent.getString("car_license_number");
            if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
                carLicenseNumber = bizContent.getString("leave_car_license_number");
            }
            String leaveTime = bizContent.getString("leave_time");

            // 检查重复数据
            if (isDuplicateCarOut(carLicenseNumber, leaveTime)) {
                logger.info("⚠️ 跳过重复的离场数据: 车牌={}, 时间={}", carLicenseNumber, leaveTime);
                return;
            }

            // 创建ReportCarOut实体
            ReportCarOut reportCarOut = new ReportCarOut();
            reportCarOut.setCarLicenseNumber(carLicenseNumber);
            reportCarOut.setEnterChannelName(bizContent.getString("enter_channel_name"));
            reportCarOut.setLeaveChannelName(bizContent.getString("leave_channel_name"));
            reportCarOut.setEnterTime(bizContent.getString("enter_time"));
            reportCarOut.setLeaveTime(leaveTime);

            // 转换进出类型数字为文字
            String enterType = convertEnterLeaveTypeToString(bizContent.getString("enter_type"));
            String leaveType = convertEnterLeaveTypeToString(bizContent.getString("leave_type"));
            reportCarOut.setEnterType(enterType);
            reportCarOut.setLeaveType(leaveType);

            // 转换VIP类型数字为文字
            String enterVipType = convertVipTypeToString(bizContent.getString("enter_vip_type"));
            String leaveVipType = convertVipTypeToString(bizContent.getString("leave_vip_type"));
            reportCarOut.setEnterVipType(enterVipType);
            reportCarOut.setLeaveVipType(leaveVipType);

            // 设置离场VIP名称
            reportCarOut.setLeaveCustomVipName(bizContent.getString("leave_custom_vip_name"));

            // 设置应收金额
            reportCarOut.setAmountReceivable(bizContent.getString("amount_receivable"));

            // 设置图片字段（添加URL前缀）
            String leaveCarLicensePicture = addImageUrlPrefix(bizContent.getString("leave_car_full_picture"));
            String enterCarFullPicture = addImageUrlPrefix(bizContent.getString("enter_car_full_picture"));
            reportCarOut.setLeaveCarFullPicture(leaveCarLicensePicture);
            reportCarOut.setEnterCarFullPicture(enterCarFullPicture);

            // 转换车牌颜色数字为文字
            String enterCarLicenseColor = convertCarLicenseColorToString(
                    bizContent.getString("enter_car_license_color"));
            String leaveCarLicenseColor = convertCarLicenseColorToString(
                    bizContent.getString("leave_car_license_color"));
            reportCarOut.setEnterCarLicenseColor(enterCarLicenseColor);
            reportCarOut.setLeaveCarLicenseColor(leaveCarLicenseColor);

            // 转换车辆类型数字为文字
            String enterCarType = convertCarTypeToString(bizContent.getString("enter_car_type"));
            String leaveCarType = convertCarTypeToString(bizContent.getString("leave_car_type"));
            reportCarOut.setEnterCarType(enterCarType);
            reportCarOut.setLeaveCarType(leaveCarType);

            // 转换记录类型数字为文字
            String recordType = convertRecordTypeToString(bizContent.getString("record_type"));
            reportCarOut.setRecordType(recordType);
            reportCarOut.setRemark(bizContent.getString("remark"));

            // 处理停车时长：从bizContent中获取stopping_time（秒），转换为格式化字符串
            String stoppingTimeStr = bizContent.getString("stopping_time");
            if (stoppingTimeStr != null && !stoppingTimeStr.isEmpty()) {
                try {
                    int stoppingTimeSeconds = Integer.parseInt(stoppingTimeStr);
                    String formattedDuration = formatParkingDuration(stoppingTimeSeconds);
                    reportCarOut.setStoppingTime(formattedDuration);
                    logger.info("🕒 离场数据停车时长格式化: {}秒 -> {}", stoppingTimeSeconds, formattedDuration);
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ 停车时长格式错误，无法转换为数字: {}", stoppingTimeStr);
                    reportCarOut.setStoppingTime("0秒");
                }
            } else {
                reportCarOut.setStoppingTime("0秒");
            }

            // 保存到数据库
            reportCarOutService.save(reportCarOut);
            logger.info("✅ 成功保存离场数据: 车牌={}, 进场类型={}, 离场类型={}, 进场VIP={}, 离场VIP={}, 时间={}",
                    carLicenseNumber, enterType, leaveType, enterVipType, leaveVipType, leaveTime);

            // 处理缴费记录
            processPaymentRecords(bizContent);

        } catch (Exception e) {
            logger.error("❌ 保存离场数据失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 检查离场数据是否重复
     * 
     * @param carLicenseNumber 车牌号码
     * @param leaveTime        离场时间
     * @return 是否重复
     */
    private boolean isDuplicateCarOut(String carLicenseNumber, String leaveTime) {
        try {
            // 使用QueryWrapper查询是否存在相同的记录
            QueryWrapper<ReportCarOut> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_license_number", carLicenseNumber)
                    .eq("leave_time", leaveTime)
                    .eq("deleted", 0);
            return reportCarOutService.count(queryWrapper) > 0;
        } catch (Exception e) {
            logger.error("检查离场数据重复性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将秒数转换为小时分钟秒格式
     * 
     * @param seconds 秒数
     * @return 格式化的时间字符串
     */
    private String formatParkingDuration(int seconds) {
        if (seconds <= 0) {
            return "0秒";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append("小时");
        }

        if (minutes > 0) {
            result.append(minutes).append("分钟");
        }

        if (remainingSeconds > 0) {
            result.append(remainingSeconds).append("秒");
        }

        // 如果所有单位都为0，返回0秒
        if (result.length() == 0) {
            return "0秒";
        }

        return result.toString();
    }

    /**
     * 处理缴费记录
     * 
     * @param bizContent 业务内容JSON对象
     */
    private void processPaymentRecords(JSONObject bizContent) {
        try {
            // 获取payment_record_list
            JSONArray paymentRecordList = bizContent.getJSONArray("payment_record_list");
            if (paymentRecordList != null && paymentRecordList.size() > 0) {
                // 获取车牌号码，优先从car_license_number获取，其次从leave_car_license_number获取
                String carLicenseNumber = bizContent.getString("car_license_number");
                if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
                    carLicenseNumber = bizContent.getString("leave_car_license_number");
                }

                logger.info("🔍 处理缴费记录，车牌号码: {}", carLicenseNumber);

                for (int i = 0; i < paymentRecordList.size(); i++) {
                    JSONObject paymentRecord = paymentRecordList.getJSONObject(i);
                    String actualReceivable = paymentRecord.getString("actual_receivable");

                    // 只处理actual_receivable不为0.00或0.0的缴费记录
                    if (actualReceivable != null && !"0.00".equals(actualReceivable)
                            && !"0.0".equals(actualReceivable)) {
                        // 检查是否已存在相同的缴费记录
                        String payTime = paymentRecord.getString("pay_time");
                        if (!paymentRecordService.existsByCarLicenseNumberAndPayTimeAndActualReceivable(
                                carLicenseNumber, payTime, actualReceivable)) {
                            // 创建缴费记录
                            PaymentRecord paymentRecordEntity = new PaymentRecord();

                            // 转换支付方式数字为文字
                            String paymentMode = convertPaymentModeToString(paymentRecord.getString("payment_mode"));
                            paymentRecordEntity.setPaymentMode(paymentMode);
                            paymentRecordEntity.setPaymentModeRemark(paymentRecord.getString("payment_mode_remark"));

                            // 转换支付来源数字为文字
                            String payOrigin = convertPayOriginToString(paymentRecord.getString("pay_origin"));
                            paymentRecordEntity.setPayOrigin(payOrigin);
                            paymentRecordEntity.setPayOriginRemark(paymentRecord.getString("pay_origin_remark"));

                            paymentRecordEntity.setPayTime(payTime);

                            // 转换支付状态数字为文字
                            String payStatus = convertPayStatusToString(paymentRecord.getString("pay_status"));
                            paymentRecordEntity.setPayStatus(payStatus);

                            paymentRecordEntity.setActualReceivable(actualReceivable);
                            paymentRecordEntity.setAmountReceivable(paymentRecord.getString("amount_receivable"));

                            // 设置车牌号码
                            paymentRecordEntity.setCarPlateNumber(carLicenseNumber);

                            // 处理停车时长：从bizContent中获取stopping_time（秒），转换为格式化字符串
                            String stoppingTimeStr = bizContent.getString("stopping_time");
                            if (stoppingTimeStr != null && !stoppingTimeStr.isEmpty()) {
                                try {
                                    int stoppingTimeSeconds = Integer.parseInt(stoppingTimeStr);
                                    String formattedDuration = formatParkingDuration(stoppingTimeSeconds);
                                    paymentRecordEntity.setParkingDuration(formattedDuration);
                                    logger.info("🕒 停车时长格式化: {}秒 -> {}", stoppingTimeSeconds, formattedDuration);
                                } catch (NumberFormatException e) {
                                    logger.warn("⚠️ 停车时长格式错误，无法转换为数字: {}", stoppingTimeStr);
                                    paymentRecordEntity.setParkingDuration("0秒");
                                }
                            } else {
                                paymentRecordEntity.setParkingDuration("0秒");
                            }

                            // 保存缴费记录
                            paymentRecordService.save(paymentRecordEntity);
                            logger.info("✅ 成功保存缴费记录: 车牌={}, 支付方式={}, 支付来源={}, 支付状态={}, 金额={}, 时间={}",
                                    carLicenseNumber, paymentMode, payOrigin, payStatus, actualReceivable, payTime);
                        } else {
                            logger.info("⚠️ 跳过重复的缴费记录: 车牌={}, 金额={}, 时间={}",
                                    carLicenseNumber, actualReceivable, payTime);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ 处理缴费记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 转换支付方式数字为文字
     * 
     * @param paymentMode 支付方式数字字符串
     * @return 支付方式文字描述
     */
    private String convertPaymentModeToString(String paymentMode) {
        if (paymentMode == null || paymentMode.isEmpty()) {
            return "未知";
        }

        try {
            int mode = Integer.parseInt(paymentMode);
            switch (mode) {
                case 0:
                    return "未知";
                case 1:
                    return "现金";
                case 2:
                    return "自发卡";
                case 3:
                    return "次数";
                case 4:
                    return "微信支付";
                case 5:
                    return "支付宝支付";
                case 6:
                    return "银联闪付";
                case 7:
                    return "交通卡";
                case 8:
                    return "免费";
                case 9:
                    return "账户余额";
                case 10:
                    return "银联接触卡";
                case 11:
                    return "银联网络支付";
                case 12:
                    return "第三方余额";
                default:
                    return "未知";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 支付方式格式错误，无法转换为数字: {}", paymentMode);
            return paymentMode; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换支付来源数字为文字
     * 
     * @param payOrigin 支付来源数字字符串
     * @return 支付来源文字描述
     */
    private String convertPayOriginToString(String payOrigin) {
        if (payOrigin == null || payOrigin.isEmpty()) {
            return "未定义";
        }

        try {
            int origin = Integer.parseInt(payOrigin);
            switch (origin) {
                case 0:
                    return "未定义";
                case 1:
                    return "出入口";
                case 2:
                    return "中央缴费";
                case 3:
                    return "自助缴费机";
                case 4:
                    return "平板";
                case 5:
                    return "移动POS机";
                case 6:
                    return "移动APP";
                case 7:
                    return "微信服务号";
                case 8:
                    return "支付宝服务窗";
                case 9:
                    return "线上支付";
                default:
                    return "未定义";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 支付来源格式错误，无法转换为数字: {}", payOrigin);
            return payOrigin; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换支付状态数字为文字
     * 
     * @param payStatus 支付状态数字字符串
     * @return 支付状态文字描述
     */
    private String convertPayStatusToString(String payStatus) {
        if (payStatus == null || payStatus.isEmpty()) {
            return "未缴费";
        }

        try {
            int status = Integer.parseInt(payStatus);
            switch (status) {
                case 0:
                    return "未缴费";
                case 1:
                    return "缴费成功";
                case 2:
                    return "缴费超时";
                default:
                    return "未缴费";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 支付状态格式错误，无法转换为数字: {}", payStatus);
            return payStatus; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换VIP类型数字为文字
     * 
     * @param vipType VIP类型数字字符串
     * @return VIP类型文字描述
     */
    private String convertVipTypeToString(String vipType) {
        if (vipType == null || vipType.isEmpty()) {
            return "未定义";
        }

        try {
            int type = Integer.parseInt(vipType);
            switch (type) {
                case 0:
                    return "未定义";
                case 1:
                    return "临时车";
                case 2:
                    return "本地VIP";
                case 3:
                    return "第三方VIP";
                case 4:
                    return "黑名单";
                case 5:
                    return "访客";
                case 6:
                    return "预定车辆";
                case 7:
                    return "共享车位车辆";
                default:
                    return "未定义";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ VIP类型格式错误，无法转换为数字: {}", vipType);
            return vipType; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换车牌颜色数字为文字
     * 
     * @param carLicenseColor 车牌颜色数字字符串
     * @return 车牌颜色文字描述
     */
    private String convertCarLicenseColorToString(String carLicenseColor) {
        if (carLicenseColor == null || carLicenseColor.isEmpty()) {
            return "其他";
        }

        try {
            int color = Integer.parseInt(carLicenseColor);
            switch (color) {
                case 0:
                    return "其他";
                case 1:
                    return "蓝色";
                case 2:
                    return "黄色";
                case 3:
                    return "白色";
                case 4:
                    return "黑色";
                case 5:
                    return "绿色";
                default:
                    return "其他";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 车牌颜色格式错误，无法转换为数字: {}", carLicenseColor);
            return carLicenseColor; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换进出类型数字为文字
     * 
     * @param enterLeaveType 进出类型数字字符串
     * @return 进出类型文字描述
     */
    private String convertEnterLeaveTypeToString(String enterLeaveType) {
        if (enterLeaveType == null || enterLeaveType.isEmpty()) {
            return "未确认";
        }

        try {
            int type = Integer.parseInt(enterLeaveType);
            switch (type) {
                case 0:
                    return "未确认";
                case 1:
                    return "自动放行";
                case 2:
                    return "确认放行";
                case 3:
                    return "异常放行";
                default:
                    return "未确认";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 进出类型格式错误，无法转换为数字: {}", enterLeaveType);
            return enterLeaveType; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换车辆类型数字为文字
     * 
     * @param carType 车辆类型数字字符串
     * @return 车辆类型文字描述
     */
    private String convertCarTypeToString(String carType) {
        if (carType == null || carType.isEmpty()) {
            return "未定义";
        }

        try {
            int type = Integer.parseInt(carType);
            switch (type) {
                case 0:
                    return "未定义";
                case 1:
                    return "小型车";
                case 2:
                    return "大型车";
                case 3:
                    return "摩托车";
                case 4:
                    return "电动车";
                case 5:
                    return "货车";
                case 6:
                    return "客车";
                case 7:
                    return "特种车辆";
                default:
                    return "未定义";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 车辆类型格式错误，无法转换为数字: {}", carType);
            return carType; // 如果已经是文字，直接返回
        }
    }

    /**
     * 转换记录类型数字为文字
     * 
     * @param recordType 记录类型数字字符串
     * @return 记录类型文字描述
     */
    private String convertRecordTypeToString(String recordType) {
        if (recordType == null || recordType.isEmpty()) {
            return "正常记录";
        }

        try {
            int type = Integer.parseInt(recordType);
            switch (type) {
                case 0:
                    return "未定义";
                case 1:
                    return "有牌车";
                case 2:
                    return "无牌车";
                case 3:
                    return "遮挡车";
                case 4:
                    return "非汽车";
                case 5:
                    return "误触发";
                default:
                    return "正常记录";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 记录类型格式错误，无法转换为数字: {}", recordType);
            return recordType; // 如果已经是文字，直接返回
        }
    }

    /**
     * 为图片URL添加前缀
     * 
     * @param imageUrl 原始图片URL
     * @return 带前缀的完整URL
     */
    private String addImageUrlPrefix(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        // 如果URL已经包含前缀，直接返回
        if (imageUrl.startsWith("http://10.100.111.2:8092")) {
            return imageUrl;
        }

        // 添加前缀
        return "http://10.100.111.2:8092" + imageUrl;
    }

    /**
     * 递归解码嵌套对象中的URL编码字段
     * 
     * @param obj 待处理的对象（可能是JSONObject、JSONArray或其他类型）
     */
    private void decodeNestedObjects(Object obj) {
        if (obj == null) {
            return;
        }

        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            // 递归处理JSONObject中的所有字段
            for (String key : jsonObj.keySet()) {
                Object value = jsonObj.get(key);
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (isUrlEncoded(stringValue)) {
                        String decodedValue = safeUrlDecode(stringValue);
                        jsonObj.put(key, decodedValue);
                        // logger.info("嵌套字段 {} 解码: {} -> {}", key, stringValue, decodedValue);
                    }
                } else if (value instanceof JSONObject || value instanceof JSONArray) {
                    // 递归处理嵌套的JSON对象和数组
                    decodeNestedObjects(value);
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            // 递归处理数组中的每个元素
            for (int i = 0; i < jsonArray.size(); i++) {
                Object item = jsonArray.get(i);
                if (item instanceof JSONObject || item instanceof JSONArray) {
                    decodeNestedObjects(item);
                }
            }
        }
    }

    /**
     * AKE离场上报
     *
     * @param body
     * @return
     */
    @PostMapping(value = "/reportCarOut", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<JSONObject> reportCarOut(@RequestBody String body) {
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(body);
            // 对JSON中的URL编码字符串进行解码
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                if (bizContent != null) {
                    // 解码所有可能包含中文的字段
                    decodeUrlEncodedFields(bizContent);
                    // 检查并打印actual_receivable不为0.00的数据
                    String actualReceivable = bizContent.getString("actual_receivable");
                    if ((actualReceivable != null && !"0.00".equals(actualReceivable))
                            || actualReceivable != null && !"0.0".equals(actualReceivable)) {
                        logger.info("💰 在reportCarOut中检测到有实际收费的数据: 车牌={}, 实际收费={}, 记录号={}",
                                bizContent.getString("car_license_number"),
                                actualReceivable,
                                bizContent.getString("record_number"));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 无法解析请求体为JSON，body={}", body);
        }
        // System.out.println("离场数据（已解码）= " + data);

        // 通过WebSocket推送离场数据到前端
        if (data != null && data.containsKey("biz_content")) {
            JSONObject bizContent = data.getJSONObject("biz_content");
            if (bizContent != null) {
                // 获取车牌号码
                String plateNumber = bizContent.getString("car_license_number");
                if (plateNumber == null || plateNumber.isEmpty()) {
                    plateNumber = bizContent.getString("leave_car_license_number");
                }

                // 过滤未识别的车牌号码
                if (plateNumber != null && !plateNumber.equals("未识别") && !plateNumber.isEmpty()) {
                    // 写入离场数据到数据库
                    try {
                        saveCarOutData(bizContent);
                        logger.info("✅ 成功写入离场数据到数据库: 车牌={}", plateNumber);
                        // 更新预约记录的车辆进出场状态
                        if (visitorReservationSyncService != null) {
                            String enterTime = bizContent.getString("enter_time");
                            String leaveTime = bizContent.getString("leave_time");
                            if (leaveTime != null) {
                                visitorReservationSyncService.handleCarOut(plateNumber, enterTime, leaveTime);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("❌ 写入离场数据失败: 车牌={}, 错误={}", plateNumber, e.getMessage());
                    }
                } else {
                    logger.info("⚠️ 跳过未识别车牌号码的离场数据: {}", plateNumber);
                }
            }
        }

        // 构建响应JSON，使用JSONObject(true)确保字段顺序，按照要求的顺序逐个添加字段
        JSONObject response = new JSONObject(true);
        response.put("command", "REPORT_CAR_OUT_LIST");
        response.put("message_id", "vems");
        
        // 获取device_id，从请求数据中获取，如果没有则使用默认值
        String deviceId = "0000000000000000000000000000vems";
        if (data != null && data.containsKey("device_id")) {
            deviceId = data.getString("device_id");
        }
        response.put("device_id", deviceId);
        
        response.put("sign_type", "MD5");
        response.put("sign", "f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        response.put("charset", "UTF-8");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        JSONObject bizContent = new JSONObject(true);
        bizContent.put("code", "0");
        bizContent.put("msg", "ok");
        response.put("biz_content", bizContent);
        
//        logger.info("📤 离场上报响应: {}", response.toJSONString());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * AKE VIP票上报接口 - 开通/续期/退费VIP票上报
     * VEMS系统主动向外部系统上报开通/续期/退费VIP票
     * 
     * @param body 请求体
     * @return 响应结果
     */
    @ApiOperation("VIP票上报 - 开通/续期/退费")
    @PostMapping(value = "/syncVipTicketOperateRecord", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<JSONObject> syncVipTicketOperateRecord(@RequestBody String body) {
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(body);
            logger.info("📋 收到VIP票上报数据，command={}", data.getString("command"));

            // 对JSON中的URL编码字符串进行解码
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                if (bizContent != null) {
                    // 解码所有可能包含中文的字段
                    decodeUrlEncodedFields(bizContent);

                    // 记录关键信息
                    String carLicenseNumber = bizContent.getString("car_license_number");
                    String customVipName = bizContent.getString("custom_vip_name");
                    String customerName = bizContent.getString("customer_name");
                    String operateTime = bizContent.getString("operate_time");
                    String totalDiscountPrice = bizContent.getString("total_discount_price");

                    logger.info("🎫 VIP票信息: 车牌={}, VIP类型={}, 客户={}, 金额={}, 操作时间={}",
                            carLicenseNumber, customVipName, customerName, totalDiscountPrice, operateTime);

                    // 处理操作记录
                    String ticketRecordType = "0"; // 默认为开通
                    if (bizContent.containsKey("operate_record")) {
                        JSONObject operateRecord = bizContent.getJSONObject("operate_record");
                        ticketRecordType = operateRecord.getString("ticket_record_type");
                        String ticketRecordTypeText = convertTicketRecordTypeToString(ticketRecordType);
                        logger.info("📝 操作类型: {}", ticketRecordTypeText);
                    }

                    // 🚀 新增：通过开通VIP月票接口写入数据
                    if ("0".equals(ticketRecordType)) { // 仅处理开通操作
                        // 🔍 第一步：检查 custom_vip_name 是否为 "二道岗可通行车辆"
                        if ("二道岗可通行车辆".equals(customVipName)) {
                            logger.info("⏭️ [跳过开通] VIP类型为'二道岗可通行车辆'，跳过开通VIP接口调用");
                            return buildSuccessResponse(data);
                        } else {
                            // 🔍 第二步：如果不是"二道岗可通行车辆"，先查询是否已存在已生效的"二道岗可通行车辆"VIP票
                            List<com.parkingmanage.service.AcmsVipService.VipTicketDetailInfo> existingTickets = null;
                            try {
                                logger.info("🔍 [查询VIP票] 开始查询车辆是否已有'二道岗可通行车辆'VIP票，车牌: {}", carLicenseNumber);

                                // 处理多个车牌号的情况（用逗号分隔）
                                List<String> carLicenseList = parseCarLicenseNumbers(carLicenseNumber);
                                String queryCarLicense = carLicenseList.get(0); // 取第一个车牌号进行查询

                                existingTickets = acmsVipService.getVipTicketList(
                                        queryCarLicense, // 车牌号（取第一个）
                                        customerName, // 车主姓名
                                        "二道岗可通行车辆" // VIP类型名称
                                );
                                System.out.println("existingTickets = " + existingTickets);
                                if (existingTickets != null && !existingTickets.isEmpty()) {
                                    // 检查是否存在 ticket_status 为 "生效中" 且 vip_type_name 为 "二道岗可通行车辆" 的VIP票
                                    // 并且车牌号在待查询的车牌号列表中
                                    boolean hasValidTicket = false;
                                    for (com.parkingmanage.service.AcmsVipService.VipTicketDetailInfo ticket : existingTickets) {
                                        if ("生效中".equals(ticket.getTicketStatus())
                                                && "二道岗可通行车辆".equals(ticket.getVipTypeName())) {
                                            // 检查返回结果中的车牌号是否在待查询的车牌号列表中
                                            // 注意：返回结果中的 carNo 也可能包含多个车牌号（用逗号分隔）
                                            String ticketCarNo = ticket.getCarNo();
                                            if (ticketCarNo != null && !ticketCarNo.trim().isEmpty()) {
                                                // 解析返回结果中的车牌号（可能也是多个）
                                                List<String> ticketCarList = parseCarLicenseNumbers(ticketCarNo);
                                                // 检查两个车牌号列表是否有交集
                                                boolean hasMatch = false;
                                                for (String ticketCar : ticketCarList) {
                                                    if (carLicenseList.contains(ticketCar)) {
                                                        hasMatch = true;
                                                        logger.info("✅ [已存在VIP票] 车辆 {} 已存在已生效的'二道岗可通行车辆'VIP票，票号: {}, 跳过开通",
                                                                ticketCar, ticket.getTicketNo());
                                                        break;
                                                    }
                                                }
                                                if (hasMatch) {
                                                    hasValidTicket = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (hasValidTicket) {
                                        logger.info("⏭️ [跳过开通] 车辆已存在已生效的'二道岗可通行车辆'VIP票，跳过开通VIP接口调用");
                                        return buildSuccessResponse(data);
                                    }
                                }

                                // 如果没有找到有效的VIP票，继续开通
                                logger.info("ℹ️ [未找到VIP票] 车辆 {} 未找到'二道岗可通行车辆'VIP票，将继续开通", carLicenseNumber);
                                try {
                                    boolean success = processVipTicketData(bizContent);
                                    if (success) {
                                        // 记录已处理的去重标识
                                        logger.info("✅ VIP票数据已成功通过ACMS接口写入！");
                                    } else {
                                        logger.warn("⚠️ VIP票数据写入ACMS失败");
                                    }
                                } catch (Exception e) {
                                    logger.error("❌ 调用ACMS开通VIP月票接口失败: {}", e.getMessage(), e);
                                }
                            } catch (Exception e) {
                                logger.error("❌ [查询VIP票失败] 查询车辆VIP票时发生错误: {}", e.getMessage(), e);
                                // 查询失败不影响后续流程，继续执行开通操作
                                try {
                                    boolean success = processVipTicketData(bizContent);
                                    if (success) {
                                        logger.info("✅ VIP票数据已成功通过ACMS接口写入！");
                                    } else {
                                        logger.warn("⚠️ VIP票数据写入ACMS失败");
                                    }
                                } catch (Exception ex) {
                                    logger.error("❌ 调用ACMS开通VIP月票接口失败: {}", ex.getMessage(), ex);
                                }
                            }
                        }

                    } else {
                        logger.info("ℹ️ 操作类型为 {} ，跳过ACMS接口调用（仅处理开通操作）",
                                convertTicketRecordTypeToString(ticketRecordType));
                    }

                    logger.info("✅ VIP票上报数据处理成功");
                }
            }
        } catch (Exception e) {
            logger.error("❌ VIP票上报数据处理失败: {}", e.getMessage(), e);
        }

        return buildSuccessResponse(data);
    }

    /**
     * 构建成功响应
     * 
     * @param data 原始请求数据
     * @return 响应对象
     */
    private ResponseEntity<JSONObject> buildSuccessResponse(JSONObject data) {
        // 构建响应JSON，使用JSONObject(true)确保字段顺序，按照要求的顺序逐个添加字段
        JSONObject response = new JSONObject(true);
        response.put("command", "REPORT_VIP_TICKET_RETURN");
        response.put("message_id", "vems");
        
        // 获取device_id，从请求数据中获取，如果没有则使用默认值
        String deviceId = "0000000000000000000000000000vems";
        if (data != null && data.containsKey("device_id")) {
            deviceId = data.getString("device_id");
        }
        response.put("device_id", deviceId);
        
        response.put("sign_type", "MD5");
        response.put("sign", "00000000");
        response.put("charset", "UTF-8");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        JSONObject bizContent = new JSONObject(true);
        bizContent.put("code", "0");
        bizContent.put("msg", "ok");
        response.put("biz_content", bizContent);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * 处理VIP票数据，通过ACMS开通VIP月票接口写入
     * 
     * @param bizContent VIP票业务内容
     * @return 是否成功
     */
    private boolean processVipTicketData(JSONObject bizContent) {
        try {
            // 获取原始ticket_no，用于去重和请求
            String originalTicketNo = null;
            if (bizContent.containsKey("operate_record")) {
                JSONObject operateRecord = bizContent.getJSONObject("operate_record");
                if (operateRecord != null) {
                    originalTicketNo = operateRecord.getString("ticket_no");
                }
            }
            // 如果operate_record中没有，从bizContent中直接获取
            if (originalTicketNo == null || originalTicketNo.isEmpty()) {
                originalTicketNo = bizContent.getString("ticket_no");
            }

            // 构建开通VIP票请求
            com.parkingmanage.service.AcmsVipService.OpenVipTicketRequest request = new com.parkingmanage.service.AcmsVipService.OpenVipTicketRequest();

            // 基本信息
            String carLicenseNumber = bizContent.getString("car_license_number");
            String customVipName = bizContent.getString("custom_vip_name");
            String customerName = bizContent.getString("customer_name");
            String operateTime = bizContent.getString("operate_time");
            String totalDiscountPrice = bizContent.getString("total_discount_price");
            String totalOriginalPrice = bizContent.getString("total_original_price");
            String customerTelphone = bizContent.getString("customer_telphone");
            String customerCompany = bizContent.getString("customer_company");
            String customerDepartment = bizContent.getString("customer_department");

            // 校验必填字段
            if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
                logger.warn("⚠️ 车牌号为空，跳过处理");
                return false;
            }

            // VIP类型固定为"二道岗可通行车辆"，不需要校验

            // 设置请求参数（VIP类型固定为：二道岗可通行车辆）
            request.setVipTypeName("二道岗可通行车辆");
            // 使用原始ticket_no，如果为空则生成一个（但这种情况不应该发生，因为已经在接口层做了去重）
            request.setTicketNo(originalTicketNo != null && !originalTicketNo.isEmpty()
                    ? originalTicketNo
                    : "SYNC_" + System.currentTimeMillis());
            request.setCarOwner(customerName != null ? customerName : "未知");
            request.setTelphone(customerTelphone != null ? customerTelphone : "");
            request.setCompany(customerCompany != null ? customerCompany : "");
            request.setDepartment(customerDepartment != null ? customerDepartment : "");
            request.setSex("0"); // 默认男
            request.setOperator("FKJK"); // 操作员改为FKJK
            request.setOperateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 价格信息
            request.setOriginalPrice(totalOriginalPrice != null ? totalOriginalPrice : "0.00");
            request.setDiscountPrice(totalDiscountPrice != null ? totalDiscountPrice : "0.00");
            request.setOpenValue("1");

            // 车辆列表：处理多个车牌号的情况（用逗号分隔）
            java.util.List<String> carList = parseCarLicenseNumbers(carLicenseNumber);
            request.setCarList(carList);
            request.setOpenCarCount(String.valueOf(carList.size()));

            // 时间段列表：从VIP票数据中获取开始和结束时间
            java.util.List<com.parkingmanage.service.AcmsVipService.TimePeriod> timePeriodList = new java.util.ArrayList<>();

            // 优先从vip_time_arr数组中获取时间段（格式：数组，每个元素包含start_time和end_time）
            if (bizContent.containsKey("vip_time_arr")) {
                Object vipTimeArrObj = bizContent.get("vip_time_arr");
                if (vipTimeArrObj instanceof JSONArray) {
                    JSONArray vipTimeArr = (JSONArray) vipTimeArrObj;
                    if (vipTimeArr != null && vipTimeArr.size() > 0) {
                        // 遍历数组，解析每个时间段
                        for (int i = 0; i < vipTimeArr.size(); i++) {
                            Object itemObj = vipTimeArr.get(i);
                            if (itemObj instanceof JSONObject) {
                                JSONObject timeItem = (JSONObject) itemObj;
                                String periodStartTime = timeItem.getString("start_time");
                                String periodEndTime = timeItem.getString("end_time");

                                if (periodStartTime != null && !periodStartTime.isEmpty()
                                        && periodEndTime != null && !periodEndTime.isEmpty()) {
                                    com.parkingmanage.service.AcmsVipService.TimePeriod period = new com.parkingmanage.service.AcmsVipService.TimePeriod();
                                    period.setStart_time(periodStartTime);
                                    period.setEnd_time(periodEndTime);
                                    timePeriodList.add(period);
                                    logger.info("📅 [时间段{}] 开始时间={}, 结束时间={}",
                                            i + 1, periodStartTime, periodEndTime);
                                }
                            }
                        }
                    }
                }
            }

            // 如果vip_time_arr中没有数据，尝试从operate_record中获取
            if (timePeriodList.isEmpty() && bizContent.containsKey("operate_record")) {
                JSONObject operateRecord = bizContent.getJSONObject("operate_record");
                if (operateRecord != null) {
                    String startTime = operateRecord.getString("ticket_start_time");
                    if (startTime == null || startTime.isEmpty()) {
                        startTime = operateRecord.getString("start_time");
                    }
                    String endTime = operateRecord.getString("ticket_end_time");
                    if (endTime == null || endTime.isEmpty()) {
                        endTime = operateRecord.getString("end_time");
                    }

                    if (startTime != null && !startTime.isEmpty()
                            && endTime != null && !endTime.isEmpty()) {
                        com.parkingmanage.service.AcmsVipService.TimePeriod period = new com.parkingmanage.service.AcmsVipService.TimePeriod();
                        period.setStart_time(startTime);
                        period.setEnd_time(endTime);
                        timePeriodList.add(period);
                        logger.info("📅 [从operate_record获取] 开始时间={}, 结束时间={}", startTime, endTime);
                    }
                }
            }

            // 如果仍然没有获取到时间段，尝试从bizContent中直接获取单个时间字段
            if (timePeriodList.isEmpty()) {
                String startTime = bizContent.getString("ticket_start_time");
                if (startTime == null || startTime.isEmpty()) {
                    startTime = bizContent.getString("start_time");
                }
                String endTime = bizContent.getString("ticket_end_time");
                if (endTime == null || endTime.isEmpty()) {
                    endTime = bizContent.getString("end_time");
                }

                if (startTime != null && !startTime.isEmpty()
                        && endTime != null && !endTime.isEmpty()) {
                    com.parkingmanage.service.AcmsVipService.TimePeriod period = new com.parkingmanage.service.AcmsVipService.TimePeriod();
                    period.setStart_time(startTime);
                    period.setEnd_time(endTime);
                    timePeriodList.add(period);
                    logger.info("📅 [从bizContent获取] 开始时间={}, 结束时间={}", startTime, endTime);
                }
            }

            // 如果仍然没有获取到时间段，使用操作时间作为默认值
            if (timePeriodList.isEmpty()) {
                String defaultTime = operateTime != null ? operateTime
                        : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                com.parkingmanage.service.AcmsVipService.TimePeriod period = new com.parkingmanage.service.AcmsVipService.TimePeriod();
                period.setStart_time(defaultTime);
                period.setEnd_time(defaultTime);
                timePeriodList.add(period);
                logger.warn("⚠️ 未找到时间段数据，使用操作时间作为默认值: {}", defaultTime);
            }

            logger.info("ℹ️ 共设置 {} 个时间段", timePeriodList.size());
            request.setTimePeriodList(timePeriodList);

            // 调用ACMS接口（VIP类型固定为：二道岗可通行车辆）
            logger.info("📤 准备调用ACMS开通VIP票接口: 车牌={}, VIP类型=二道岗可通行车辆, 客户={}",
                    carLicenseNumber, customerName);

            boolean success = acmsVipService.openVipTicketToVems(request);
            // boolean success = true;
            if (success) {
                logger.info("✅ ACMS开通VIP票成功: 车牌={}, VIP类型=二道岗可通行车辆", carLicenseNumber);
            } else {
                logger.warn("⚠️ ACMS开通VIP票失败: 车牌={}", carLicenseNumber);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ 处理VIP票数据异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析车牌号字符串，支持多个车牌号用逗号分隔
     * 
     * @param carLicenseNumber 车牌号字符串，可能包含多个车牌号（用逗号分隔），例如："黑C6195L,黑A8SC60"
     * @return 车牌号列表
     */
    private java.util.List<String> parseCarLicenseNumbers(String carLicenseNumber) {
        java.util.List<String> carList = new java.util.ArrayList<>();
        if (carLicenseNumber == null || carLicenseNumber.trim().isEmpty()) {
            return carList;
        }
        
        // 按逗号分割车牌号
        String[] carNumbers = carLicenseNumber.split(",");
        for (String carNo : carNumbers) {
            String trimmedCarNo = carNo.trim();
            if (!trimmedCarNo.isEmpty()) {
                carList.add(trimmedCarNo);
            }
        }
        
        // 如果没有分割出任何车牌号，则使用原始字符串
        if (carList.isEmpty()) {
            carList.add(carLicenseNumber.trim());
        }
        
        return carList;
    }

    /**
     * 转换票据记录类型数字为文字
     * 
     * @param ticketRecordType 票据记录类型数字字符串
     * @return 票据记录类型文字描述
     */
    private String convertTicketRecordTypeToString(String ticketRecordType) {
        if (ticketRecordType == null || ticketRecordType.isEmpty()) {
            return "未知";
        }

        try {
            int type = Integer.parseInt(ticketRecordType);
            switch (type) {
                case 0:
                    return "开通";
                case 1:
                    return "续期";
                case 2:
                    return "退费";
                case 3:
                    return "暂停";
                case 4:
                    return "恢复";
                default:
                    return "未知(" + type + ")";
            }
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 票据记录类型格式错误，无法转换为数字: {}", ticketRecordType);
            return ticketRecordType;
        }
    }

    /**
     * 检查并添加访客（如果需要）
     * 在reportCarIn接口中，如果检测到车牌号码在访客暂存表中存在且未调用接口，
     * 并且通道不是指定的四个通道，则调用接口添加访客
     * 
     * @param carLicenseNumber 车牌号码
     * @param enterChannelName 进场通道名称
     * @param enterTime 进场时间
     */
    private void checkAndAddVisitorIfNeeded(String carLicenseNumber, String enterChannelName, String enterTime) {
        try {
            // 需要排除的通道名称列表
            List<String> excludedChannels = Arrays.asList(
                    "体育馆桥旁入口",
                    "校区桥旁入口",
                    "体育馆校内入口1",
                    "体育馆校内入口2");

            // 需要排除的VIP类型列表
            List<String> excludedVipTypes = Arrays.asList(
                    "体育馆自助访客",
                    "体育馆访客车辆");
            logger.info("ℹ️ [检查添加访客] 车牌: {}, 通道: {}, 进场时间: {}", carLicenseNumber, enterChannelName, enterTime);
            // 检查通道名称，如果在排除列表中，则跳过
            if (enterChannelName != null && excludedChannels.contains(enterChannelName)) {
                logger.info("⏭️ [跳过添加访客] 车牌: {}, 通道: {} 在排除列表中", carLicenseNumber, enterChannelName);
                return;
            }

            // 校验进场时间
            if (enterTime == null || enterTime.isEmpty()) {
                logger.warn("⚠️ [时间缺失] 车牌: {}, 进场时间为空，跳过添加访客", carLicenseNumber);
                return;
            }

            // 解析进场时间
            Date enterTimeDate = null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                enterTimeDate = sdf.parse(enterTime);
            } catch (ParseException e) {
                logger.error("❌ [时间解析失败] 车牌: {}, 进场时间: {}, 错误: {}", carLicenseNumber, enterTime, e.getMessage());
                return;
            }

            // 查询访客暂存表，查找相同车牌且进场时间在预约记录时间范围内的记录
            // 条件：进场时间 >= gateway_transit_begin_time 且 进场时间 <= gateway_transit_end_time
            // 查询已处理过的记录：apply_state_name = "已来访" 且 apply_from_name = "FKJK添加"
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_number", carLicenseNumber)
                    .le("gateway_transit_begin_time", enterTimeDate)
                    .ge("gateway_transit_end_time", enterTimeDate)
                    .eq("deleted", 0)
                    .orderByDesc("external_create_time"); // 按外部创建时间倒序

            List<VisitorReservationSync> visitorSyncList = visitorReservationSyncMapper.selectList(queryWrapper);
            logger.info("ℹ️ [访客检查] 车牌: {} 在暂存表中找到 {} 条记录", carLicenseNumber, visitorSyncList.size());
            if (visitorSyncList == null || visitorSyncList.isEmpty()) {
                logger.debug("ℹ️ [访客检查] 车牌: {} 在暂存表中未找到时间范围内的预约记录", carLicenseNumber);
                return;
            }

            // 筛选掉需要排除的VIP类型
            visitorSyncList = visitorSyncList.stream()
                    .filter(record -> {
                        String vipTypeName = record.getVipTypeName();
                        if (vipTypeName == null || vipTypeName.isEmpty()) {
                            return true; // VIP类型为空，保留记录
                        }
                        // 检查VIP类型是否包含排除列表中的任何一个关键词
                        boolean shouldExclude = excludedVipTypes.stream()
                                .anyMatch(vipTypeName::contains);
                        if (shouldExclude) {
                            logger.info("⏭️ [排除VIP类型] 车牌: {}, VIP类型: {} 在排除列表中", carLicenseNumber, vipTypeName);
                        }
                        return !shouldExclude; // 不在排除列表中的保留
                    })
                    .collect(java.util.stream.Collectors.toList());
            visitorSyncList.forEach(record -> {
                logger.info("🔍 [访客检查] 车牌: {}, 预约ID: {}, 预约时间: {}", carLicenseNumber, record.getId(), record.getGatewayTransitBeginTime());
            });
            // 再次检查筛选后是否还有记录
            if (visitorSyncList.isEmpty()) {
                logger.debug("ℹ️ [访客检查] 车牌: {} 筛选排除VIP类型后无可用预约记录", carLicenseNumber);
                return;
            }

            logger.info("🔍 [发现预约记录] 车牌: {}, 筛选后找到 {} 条时间范围内的预约记录", carLicenseNumber, visitorSyncList.size());

            // 根据记录数量和处理逻辑
            VisitorReservationSync targetVisitorSync = null;

            if (visitorSyncList.size() == 1) {
                // 单条记录的情况
                VisitorReservationSync visitorSync = visitorSyncList.get(0);
                
                // 判断是否为FKJK添加
                boolean isFkjkAdded = "FKJK添加".equals(visitorSync.getApplyFromName());
                
                if (isFkjkAdded) {
                    logger.info("⏭️ [跳过添加访客] 车牌: {}, 预约ID: {}, 该记录为FKJK添加，无需重复添加",
                            carLicenseNumber, visitorSync.getReservationId());
                    return;
                } else {
                    // 不是FKJK添加，使用这条记录
                    targetVisitorSync = visitorSync;
                    logger.info("✅ [选择预约记录] 车牌: {}, 预约ID: {}, 使用单条非FKJK添加的记录",
                            carLicenseNumber, visitorSync.getReservationId());
                }
            } else {
                // 多条记录的情况
                // 统计FKJK添加和非FKJK添加的数量
                List<VisitorReservationSync> nonFkjkList = new java.util.ArrayList<>();
                int fkjkCount = 0;
                
                for (VisitorReservationSync visitorSync : visitorSyncList) {
                    if ("FKJK添加".equals(visitorSync.getApplyFromName())) {
                        fkjkCount++;
                    } else {
                        nonFkjkList.add(visitorSync);
                    }
                }

                if (fkjkCount == visitorSyncList.size()) {
                    // 全部为FKJK添加
                    logger.info("⏭️ [跳过添加访客] 车牌: {}, 所有 {} 条记录均为FKJK添加，无需重复添加",
                            carLicenseNumber, visitorSyncList.size());
                    return;
                } else if (nonFkjkList.size() == 1) {
                    // 只有一条非FKJK添加的记录
                    targetVisitorSync = nonFkjkList.get(0);
                    logger.info("✅ [选择预约记录] 车牌: {}, 预约ID: {}, 使用唯一一条非FKJK添加的记录",
                            carLicenseNumber, targetVisitorSync.getReservationId());
                } else {
                    // 有多条非FKJK添加的记录，选择external_create_time最晚的一条
                    targetVisitorSync = nonFkjkList.stream()
                            .max((v1, v2) -> {
                                Date t1 = v1.getExternalCreateTime();
                                Date t2 = v2.getExternalCreateTime();
                                if (t1 == null && t2 == null) return 0;
                                if (t1 == null) return -1;
                                if (t2 == null) return 1;
                                return t1.compareTo(t2);
                            })
                            .orElse(null);
                    
                    if (targetVisitorSync != null) {
                        logger.info("✅ [选择预约记录] 车牌: {}, 预约ID: {}, 使用external_create_time最晚的非FKJK添加记录",
                                carLicenseNumber, targetVisitorSync.getReservationId());
                    }
                }
            }

            // 如果没有找到目标记录，直接返回
            if (targetVisitorSync == null) {
                logger.warn("⚠️ [未找到目标记录] 车牌: {}, 无法确定要使用的预约记录", carLicenseNumber);
                return;
            }

            logger.info("🔍 [发现待添加访客] 车牌: {}, 预约ID: {}, 访客: {}, 通道: {}",
                    carLicenseNumber, targetVisitorSync.getReservationId(), targetVisitorSync.getVisitorName(), enterChannelName);

            // 构建添加访客请求
            com.parkingmanage.service.AcmsVipService.AddVisitorCarRequest request = new com.parkingmanage.service.AcmsVipService.AddVisitorCarRequest();

            // 基本信息
            request.setCarCode(targetVisitorSync.getCarNumber());
            request.setOwner(targetVisitorSync.getVisitorName());
            request.setVisitName("访客二道岗通行"); // 固定使用"访客二道岗通行"
            request.setPhonenum(targetVisitorSync.getVisitorPhone() != null ? targetVisitorSync.getVisitorPhone() : "");
            request.setReason(""); // 来访原因设为空
            request.setOperator("FKJK"); // 固定操作员
            request.setOperateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 设置访客时间：使用网关通行时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startTime = null;
            String endTime = null;

            if (targetVisitorSync.getGatewayTransitBeginTime() != null) {
                startTime = sdf.format(targetVisitorSync.getGatewayTransitBeginTime());
            } else if (targetVisitorSync.getStartTime() != null) {
                startTime = sdf.format(targetVisitorSync.getStartTime());
            }

            if (targetVisitorSync.getGatewayTransitEndTime() != null) {
                endTime = sdf.format(targetVisitorSync.getGatewayTransitEndTime());
            } else if (targetVisitorSync.getEndTime() != null) {
                endTime = sdf.format(targetVisitorSync.getEndTime());
            }

            // 校验时间字段
            if (startTime == null || endTime == null) {
                logger.warn("⚠️ [时间缺失] 预约ID: {}, 开始时间: {}, 结束时间: {}, 跳过添加访客",
                        targetVisitorSync.getReservationId(), startTime, endTime);
                return;
            }

            // 创建访客时间对象
            com.parkingmanage.service.AcmsVipService.VisitTime visitTime = new com.parkingmanage.service.AcmsVipService.VisitTime();
            visitTime.setStart_time(startTime);
            visitTime.setEnd_time(endTime);
            request.setVisitTime(visitTime);

            // 调用添加访客接口
            logger.info("📤 [调用添加访客接口] 车牌: {}, 访客: {}, 通道: {}, 时间: {} 至 {}",
                    carLicenseNumber, targetVisitorSync.getVisitorName(), enterChannelName, startTime, endTime);

            boolean success = acmsVipService.addVisitorCarToAcms(request);

            if (success) {
                // 更新标记：设置apiCalled=1，apiCallTime=当前时间
                targetVisitorSync.setApiCalled(1);
                targetVisitorSync.setApiCallTime(new Date());
                // 更新申请状态和发起渠道
                targetVisitorSync.setApplyStateName("已来访");
                targetVisitorSync.setApplyFromName("FKJK添加");
                visitorReservationSyncMapper.updateById(targetVisitorSync);

                logger.info("✅ [访客添加成功] 车牌: {}, 预约ID: {}, 访客: {}, 通道: {}, 已更新标记和状态",
                        carLicenseNumber, targetVisitorSync.getReservationId(), targetVisitorSync.getVisitorName(),
                        enterChannelName);
            } else {
                logger.warn("⚠️ [访客添加失败] 车牌: {}, 预约ID: {}, 通道: {}, 未更新标记（等待下次进场时重试）",
                        carLicenseNumber, targetVisitorSync.getReservationId(), enterChannelName);
            }

        } catch (Exception e) {
            logger.error("❌ [检查并添加访客异常] 车牌: {}, 通道: {}, 错误: {}",
                    carLicenseNumber, enterChannelName, e.getMessage(), e);
        }
    }

    /**
     * 根据车牌号查询当前有效的访客预约信息
     * 
     * @param carNumber 车牌号
     * @return 访客预约信息（只包含非空字段）
     */
    @ApiOperation("根据车牌号查询当前有效的访客预约信息")
    @GetMapping("/getVisitorReservationByCarNumber")
    public ResponseEntity<Result> getVisitorReservationByCarNumber(@RequestParam String carNumber) {
        try {
            logger.info("🔍 [访客预约查询] 查询车牌: {}", carNumber);

            // 1. 参数校验
            if (carNumber == null || carNumber.trim().isEmpty()) {
                logger.warn("⚠️ [访客预约查询] 车牌号为空");
                return ResponseEntity.ok(Result.error("车牌号不能为空"));
            }

            // 2. 查询该车牌号的所有预约记录
            QueryWrapper<VisitorReservationSync> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("car_number", carNumber)
                    .eq("deleted", 0)
                    .orderByDesc("create_time");
            
            List<VisitorReservationSync> reservations = visitorReservationSyncMapper.selectList(queryWrapper);
            
            if (reservations == null || reservations.isEmpty()) {
                logger.info("ℹ️ [访客预约查询] 车牌: {}, 未找到预约记录", carNumber);
                return ResponseEntity.ok(Result.success(new HashMap<>()));
            }

            // 3. 获取当前时间
            Date currentTime = new Date();
            
            // 4. 筛选当前时间在 gateway_transit_begin_time 和 gateway_transit_end_time 之间的记录
            List<JSONObject> validReservations = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (VisitorReservationSync reservation : reservations) {
                Date beginTime = reservation.getGatewayTransitBeginTime();
                Date endTime = reservation.getGatewayTransitEndTime();
                
                // 判断当前时间是否在有效期内
                if (beginTime != null && endTime != null) {
                    if (currentTime.compareTo(beginTime) >= 0 && currentTime.compareTo(endTime) <= 0) {
                        // 构建返回数据（只包含非空字段）
                        JSONObject data = new JSONObject();
                        
                        // 添加非空字段
                        if (reservation.getVisitorName() != null && !reservation.getVisitorName().isEmpty()) {
                            data.put("visitorName", reservation.getVisitorName());
                        }
                        if (reservation.getVisitorPhone() != null && !reservation.getVisitorPhone().isEmpty()) {
                            data.put("visitorPhone", reservation.getVisitorPhone());
                        }
                        if (reservation.getPassName() != null && !reservation.getPassName().isEmpty()) {
                            data.put("passName", reservation.getPassName());
                        }
                        if (reservation.getPassDep() != null && !reservation.getPassDep().isEmpty()) {
                            data.put("passDep", reservation.getPassDep());
                        }
                        if (reservation.getVipTypeName() != null && !reservation.getVipTypeName().isEmpty()) {
                            data.put("vipTypeName", reservation.getVipTypeName());
                        }
                        if (beginTime != null) {
                            data.put("gatewayTransitBeginTime", sdf.format(beginTime));
                        }
                        if (endTime != null) {
                            data.put("gatewayTransitEndTime", sdf.format(endTime));
                        }
                        if (reservation.getCarVisitStatus() != null && !reservation.getCarVisitStatus().isEmpty()) {
                            data.put("carVisitStatus", reservation.getCarVisitStatus());
                        }
                        if (reservation.getCarVisitTimes() != null && !reservation.getCarVisitTimes().isEmpty()) {
                            data.put("carVisitTimes", reservation.getCarVisitTimes());
                        }
                        
                        // 添加预约ID用于追踪
                        if (reservation.getReservationId() != null && !reservation.getReservationId().isEmpty()) {
                            data.put("reservationId", reservation.getReservationId());
                        }
                        
                        validReservations.add(data);
                        
                        logger.info("✅ [访客预约查询] 车牌: {}, 找到有效预约: 访客={}, 被访人={}, 有效期={} 至 {}", 
                            carNumber, reservation.getVisitorName(), reservation.getPassName(),
                            sdf.format(beginTime), sdf.format(endTime));
                    }
                }
            }
            
            // 5. 返回结果
            if (validReservations.isEmpty()) {
                logger.info("ℹ️ [访客预约查询] 车牌: {}, 找到{}条预约记录，但当前时间不在有效期内", 
                    carNumber, reservations.size());
                
                HashMap<String, Object> resultData = new HashMap<>();
                resultData.put("message", "找到预约记录，但当前时间不在有效期内");
                resultData.put("totalCount", reservations.size());
                resultData.put("validCount", 0);
                return ResponseEntity.ok(Result.success(resultData));
            } else {
                logger.info("✅ [访客预约查询] 车牌: {}, 共{}条预约记录，其中{}条当前有效", 
                    carNumber, reservations.size(), validReservations.size());
                
                HashMap<String, Object> resultData = new HashMap<>();
                resultData.put("totalCount", reservations.size());
                resultData.put("validCount", validReservations.size());
                resultData.put("reservations", validReservations);
                return ResponseEntity.ok(Result.success(resultData));
            }

        } catch (Exception e) {
            logger.error("❌ [访客预约查询] 车牌: {}, 查询失败: {}", carNumber, e.getMessage(), e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 🔔 推送车辆进场提醒到前端
     * @param reservation 预约记录
     * @param bizContent 进场数据
     */
    private void sendVehicleEntryAlert(VisitorReservationSync reservation, JSONObject bizContent) {
        try {
            JSONObject alert = new JSONObject(true);
            
            // 提醒类型
            alert.put("alertType", "reservation_entry");
            alert.put("type", "vehicle");
            alert.put("timestamp", System.currentTimeMillis());
            
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
            alert.put("time", bizContent.getString("enter_time"));
            alert.put("channel", bizContent.getString("enter_channel_name"));
            
            // 车牌颜色信息（用于前端显示不同颜色的车牌样式）
            alert.put("enter_car_license_color", bizContent.getString("enter_car_license_color"));
            alert.put("enter_car_type", bizContent.getString("enter_car_type"));
            
            // 进场照片（添加URL前缀）
            String imageUrl = bizContent.getString("enter_car_full_picture");
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 如果不是http开头，添加前缀（imageUrl本身已包含/vems路径）
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "http://10.100.111.2:8092" + 
                              (imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl);
                }
                alert.put("imageUrl", imageUrl);
                logger.info("✅ [图片URL处理] 完整URL: {}", imageUrl);
            } else {
                logger.warn("⚠️ [图片URL处理] enter_car_full_picture 字段为空，无法添加图片");
            }
            
            // 通过WebSocket推送到所有连接的客户端
            vehicleWebSocketHandler.broadcastMessage(alert);
            
            logger.info("🔔 [车辆进场提醒] 已推送 - 车牌: {}, 预约人: {}, 进场时间: {}, 包含图片: {}", 
                       reservation.getCarNumber(), 
                       reservation.getVisitorName(),
                       bizContent.getString("enter_time"),
                       (imageUrl != null && !imageUrl.isEmpty() ? "是" : "否"));
            
        } catch (Exception e) {
            logger.error("❌ 推送车辆进场提醒失败", e);
        }
    }
}