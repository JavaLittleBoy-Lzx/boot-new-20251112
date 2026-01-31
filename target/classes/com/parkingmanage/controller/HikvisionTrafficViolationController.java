package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.HikvisionImageProxyService;
import com.parkingmanage.service.HikvisionTrafficViolationService;
import com.parkingmanage.service.HikvisionTrafficViolationService.ViolationEventRequest;
import com.parkingmanage.service.HikvisionTrafficViolationService.ViolationEventResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 海康威视园区卡口违章事件查询控制器
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/hikvision/traffic")
@Api(tags = "海康威视园区卡口违章查询")
public class HikvisionTrafficViolationController {

    @Resource
    private HikvisionTrafficViolationService hikvisionTrafficViolationService;

    @Resource
    private HikvisionImageProxyService hikvisionImageProxyService;

    /**
     * 查询违章事件（简化版）
     * 只需要传必要参数：pageSize、pageNo、beginTime、endTime
     * 其他参数都是可选的
     * 
     * @param request 查询请求
     * @return 违章事件列表
     */
    @PostMapping("/violations/search")
    @ApiOperation(value = "查询违章事件", notes = "只需传必要参数：pageSize、pageNo、beginTime、endTime，其他参数可选")
    public ResponseEntity<Result> searchViolations(@RequestBody ViolationSearchRequest request) {
        log.info("🔍 [违章查询] 开始查询 - 车牌号: {}, 违章类型: {}, 页码: {}/{}, 时间: {} ~ {}", 
            request.getPlateNo(), 
            request.getIllegalType(),
            request.getPageNo(),
            request.getPageSize(),
            request.getBeginTime(),
            request.getEndTime());
        
        try {
            // 设置默认值
            if (request.getPageSize() == null) {
                request.setPageSize(20); // 默认每页20条
            }
            if (request.getPageNo() == null) {
                request.setPageNo(1); // 默认第1页
            }
            
            // 参数校验
            if (request.getPageSize() <= 0 || request.getPageSize() > 1000) {
                log.warn("⚠️ [违章查询] 分页大小无效: {}", request.getPageSize());
                return ResponseEntity.ok(Result.error("分页大小必须在 (0, 1000] 范围内"));
            }
            
            if (request.getPageNo() <= 0) {
                log.warn("⚠️ [违章查询] 页码无效: {}", request.getPageNo());
                return ResponseEntity.ok(Result.error("页码必须大于0"));
            }
            
            // 构建服务层请求对象
            ViolationEventRequest serviceRequest = new ViolationEventRequest();
            serviceRequest.setPageSize(request.getPageSize());
            serviceRequest.setPageNo(request.getPageNo());
            serviceRequest.setPlateNo(request.getPlateNo());
            serviceRequest.setSpeedType(request.getSpeedType());
            serviceRequest.setIllegalType(request.getIllegalType());
            serviceRequest.setMonitoringId(request.getMonitoringId());
            serviceRequest.setBeginTime(request.getBeginTime());
            serviceRequest.setEndTime(request.getEndTime());
            serviceRequest.setCreateBeginTime(request.getCreateBeginTime());
            serviceRequest.setCreateEndTime(request.getCreateEndTime());
            serviceRequest.setAlarmReason(request.getAlarmReason());
            serviceRequest.setEventId(request.getEventId());
            
            // 调用服务层
            ViolationEventResponse response = hikvisionTrafficViolationService.queryViolationEvents(serviceRequest);
            
            if (response != null && "0".equals(response.getCode())) {
                // 构建返回数据
                Map<String, Object> data = new HashMap<>();
                data.put("total", response.getData() != null ? response.getData().getTotal() : 0);
                data.put("pageSize", response.getData() != null ? response.getData().getPageSize() : request.getPageSize());
                data.put("pageNo", response.getData() != null ? response.getData().getPageNo() : request.getPageNo());
                data.put("list", response.getData() != null ? response.getData().getList() : null);
                
                log.info("✅ [违章查询] 查询成功 - 总数: {}, 当前页: {} 条", 
                    data.get("total"),
                    response.getData() != null && response.getData().getList() != null ? 
                        response.getData().getList().size() : 0);
                
                return ResponseEntity.ok(Result.success(data));
            } else {
                log.warn("⚠️ [违章查询] 查询失败 - code: {}, msg: {}", 
                    response != null ? response.getCode() : "null",
                    response != null ? response.getMsg() : "unknown");
                return ResponseEntity.ok(Result.error(
                    response != null ? response.getMsg() : "查询失败"));
            }
            
        } catch (Exception e) {
            log.error("❌ [违章查询] 查询异常", e);
            return ResponseEntity.ok(Result.error("查询违章事件失败: " + e.getMessage()));
        }
    }

    /**
     * 根据车牌号查询违章事件（简化接口）
     * 
     * @param plateNo 车牌号
     * @param pageNo 页码，默认1
     * @param pageSize 每页大小，默认20
     * @return 违章事件列表
     */
    @GetMapping("/violations/by-plate")
    @ApiOperation(value = "根据车牌号查询违章", notes = "根据车牌号查询违章事件（GET方式）")
    public ResponseEntity<Result> searchByPlate(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNo,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize) {
        ViolationSearchRequest request = new ViolationSearchRequest();
        request.setPlateNo(plateNo);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return searchViolations(request);
    }

    /**
     * 查询违章事件（极简版 - 无需任何参数）
     * 默认查询最近24小时的违章记录，第1页，每页20条
     * 
     * @return 违章事件列表
     */
    @GetMapping("/violations/search-simple")
    @ApiOperation(value = "查询违章事件（极简版）", notes = "无需任何参数，默认查询最近24小时的违章记录")
    public ResponseEntity<Result> searchViolationsSimple() {
        log.info("🔍 [违章查询-极简] 开始查询最近24小时违章记录");
        
        // 计算时间范围（最近24小时）
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime beginTime = endTime.minusHours(24);
        
        // 格式化时间（格式：yyyy-MM-ddTHH:mm:ss.SSS+08:00）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String beginTimeStr = beginTime.format(formatter) + "+08:00";
        String endTimeStr = endTime.format(formatter) + "+08:00";
        
        ViolationSearchRequest request = new ViolationSearchRequest();
        request.setPageNo(1);
        request.setPageSize(20);
        request.setBeginTime(beginTimeStr);
        request.setEndTime(endTimeStr);
        
        return searchViolations(request);
    }

    /**
     * 查询违章事件（最近N小时）
     * 
     * @param hours 查询最近几小时，默认24小时
     * @param pageNo 页码，默认1
     * @param pageSize 每页大小，默认20
     * @return 违章事件列表
     */
    @GetMapping("/violations/recent-hours")
    @ApiOperation(value = "查询最近N小时违章事件", notes = "查询最近N小时的违章事件")
    public ResponseEntity<Result> searchRecentHours(
            @ApiParam(value = "查询最近几小时", defaultValue = "24") @RequestParam(defaultValue = "24") Integer hours,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime beginTime = endTime.minusHours(hours);
        
        // 格式化时间（格式：yyyy-MM-ddTHH:mm:ss.SSS+08:00）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String beginTimeStr = beginTime.format(formatter) + "+08:00";
        String endTimeStr = endTime.format(formatter) + "+08:00";
        
        ViolationSearchRequest request = new ViolationSearchRequest();
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        request.setBeginTime(beginTimeStr);
        request.setEndTime(endTimeStr);
        
        return searchViolations(request);
    }

    /**
     * 查询违章事件（最近N天）
     * 默认查询最近7天的违章事件
     * 
     * @param days 查询最近几天，默认7天
     * @param pageNo 页码，默认1
     * @param pageSize 每页大小，默认20
     * @return 违章事件列表
     */
    @GetMapping("/violations/recent")
    @ApiOperation(value = "查询最近违章事件", notes = "查询最近N天的违章事件")
    public ResponseEntity<Result> searchRecent(
            @ApiParam(value = "查询最近几天", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime beginTime = endTime.minusDays(days);
        
        // 格式化时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String beginTimeStr = beginTime.format(formatter) + "+08:00";
        String endTimeStr = endTime.format(formatter) + "+08:00";
        
        ViolationSearchRequest request = new ViolationSearchRequest();
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        request.setBeginTime(beginTimeStr);
        request.setEndTime(endTimeStr);
        
        return searchViolations(request);
    }

    /**
     * 根据违章类型查询
     * 
     * @param illegalType 违章类型：1-超速，2-逆行，3-黑名单，5-违停
     * @param pageNo 页码，默认1
     * @param pageSize 每页大小，默认20
     * @return 违章事件列表
     */
    @GetMapping("/violations/by-type")
    @ApiOperation(value = "根据违章类型查询", notes = "根据违章类型查询违章事件")
    public ResponseEntity<Result> searchByType(
            @ApiParam(value = "违章类型：1-超速，2-逆行，3-黑名单，5-违停", required = true) @RequestParam String illegalType,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        ViolationSearchRequest request = new ViolationSearchRequest();
        request.setIllegalType(illegalType);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        
        return searchViolations(request);
    }

    /**
     * 图片代理接口
     * 代理获取海康图片服务器的图片，解决线上服务器无法直接访问内网海康图片服务器的问题
     *
     * @param url 海康图片URL（需要URL编码）
     * @return 图片数据
     */
    @GetMapping("/image-proxy")
    @ApiOperation(value = "图片代理", notes = "代理获取海康图片服务器的图片")
    public void proxyImage(
            @ApiParam(value = "海康图片URL（需要URL编码）", required = true, example = "http%3A%2F%2F10.100.110.82%2Fpic%3F%3Dxxx")
            @RequestParam String url,
            javax.servlet.http.HttpServletResponse response) throws java.io.IOException {

        log.info("🖼️ [图片代理] 收到代理请求 - URL: {}", url);

        String decodedUrl = "";

        try {
            // URL解码
            decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            log.info("🔓 [图片代理] 解码后URL: {}", decodedUrl);

            // 调用服务代理获取图片
            byte[] imageBytes = hikvisionImageProxyService.proxyImage(decodedUrl);

            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("⚠️ [图片代理] 获取图片失败或图片为空");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 设置响应头
            response.setContentType("image/jpeg");
            response.setContentLength(imageBytes.length);
            response.setHeader("Cache-Control", "max-age=3600");

            // 直接写入响应流
            java.io.OutputStream outputStream = response.getOutputStream();
            outputStream.write(imageBytes);
            outputStream.flush();

            log.info("✅ [图片代理] 返回图片成功 - 大小: {} KB", imageBytes.length / 1024);

        } catch (Exception e) {
            log.error("❌ [图片代理] 代理异常 - URL: {}", decodedUrl, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 测试图片代理接口
     * 返回一个测试用的响应，验证图片代理功能是否正常
     *
     * @return 测试结果
     */
    @GetMapping("/image-proxy/test")
    @ApiOperation(value = "测试图片代理", notes = "测试图片代理功能是否正常")
    public ResponseEntity<Result> testImageProxy() {
        log.info("🧪 [图片代理测试] 开始测试");

        try {
            // 构造一个测试URL（使用海康图片服务器的地址格式）
            String testUrl = "http://10.100.110.82/pic?=test";

            // 测试URL转换
            String proxyUrl = hikvisionImageProxyService.convertToProxyUrl(testUrl, "http://localhost:8675");

            java.util.Map<String, Object> result = new HashMap<>();
            result.put("testImageUrl", testUrl);
            result.put("proxyUrl", proxyUrl);
            result.put("isHikvisionImage", hikvisionImageProxyService.isHikvisionImageUrl(testUrl));
            result.put("message", "图片代理服务正常，请使用 proxyUrl 中的地址访问图片");

            log.info("✅ [图片代理测试] 测试完成");
            return ResponseEntity.ok(Result.success(result));

        } catch (Exception e) {
            log.error("❌ [图片代理测试] 测试失败", e);
            return ResponseEntity.ok(Result.error("测试失败: " + e.getMessage()));
        }
    }

    /**
     * 违章查询请求对象（精简版）
     * 只保留最常用的4个参数
     */
    @Data
    public static class ViolationSearchRequest {
        private Integer pageSize;           // 每页记录数,范围 ( 0 , 1000 ]，必填
        private Integer pageNo;             // 目标页码,范围 ( 0 , ~ )，必填
        private String beginTime;           // 过车开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00），必填
        private String endTime;             // 过车结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00），必填
        
        // 以下是可选参数（如果需要更多筛选条件可以传）
        private String plateNo;             // 车牌号，支持模糊查询（可选）
        private String speedType;           // 测速类型，默认-1（全部类型），1（点位测速），2（区间测速）（可选）
        private String illegalType;         // 违章类型，默认-1（全部类型），1（超速），2（逆行），3（黑名单），5（违停）（可选）
        private String monitoringId;        // 卡口点id（可选）
        private String createBeginTime;     // 入库开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00）（可选）
        private String createEndTime;       // 入库结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00）（可选）
        private String alarmReason;         // 布防原因（可选）
        private String eventId;             // 事件的唯一编号（可选）
    }
}
