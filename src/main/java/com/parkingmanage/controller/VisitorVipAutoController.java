package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.AcmsVipService;
import com.parkingmanage.service.VisitorVipAutoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 访客VIP自动开通控制器
 * 提供状态查询功能（仅支持混合模式自动执行）
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/visitor-vip-auto")
@Api(tags = "访客VIP自动开通管理")
public class VisitorVipAutoController {

    @Autowired
    private VisitorVipAutoService visitorVipAutoService;
    
    @Autowired
    private AcmsVipService acmsVipService;

    /**
     * 获取服务状态（基本信息）
     * 
     * @return 服务状态信息
     */
    @GetMapping("/status")
    @ApiOperation(value = "获取服务状态", notes = "查询访客VIP自动开通服务的基本运行状态")
    public ResponseEntity<Result> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("serviceName", "访客VIP自动开通服务（混合模式）");
            status.put("status", "运行中");
            status.put("storageMode", "hybrid");
            status.put("storageModeDescription", "数据库+内存混合存储，高效变更检测");
            status.put("currentTime", getCurrentTime());
            status.put("description", "智能调度从外部接口获取访客预约记录，使用混合模式自动开通VIP月票");
            status.put("excludedVipTypes", new String[]{"体育馆自助访客", "体育馆访客车辆"});
            status.put("excludedConditions", new String[]{"VIP类型排除", "车牌号为空"});
            
            return ResponseEntity.ok(Result.success(status));
            
        } catch (Exception e) {
            log.error("❌ [获取状态失败] {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取执行状态详情（含实时执行信息）
     * 
     * @return 执行状态详情
     */
    @GetMapping("/execution-status")
    @ApiOperation(value = "获取执行状态详情", notes = "查询定时任务的实时执行状态和统计信息（混合模式）")
    public ResponseEntity<Result> getExecutionStatus() {
        try {
            // 获取执行状态
            Map<String, Object> executionStatus = visitorVipAutoService.getExecutionStatus();
            
            // 添加额外信息
            executionStatus.put("currentTime", getCurrentTime());
            executionStatus.put("storageMode", "hybrid");
            executionStatus.put("executionMode", "自动调度（仅混合模式）");
            
            // 判断任务健康状态
            boolean isRunning = (boolean) executionStatus.get("isRunning");
            boolean autoEnabled = (boolean) executionStatus.get("autoEnabled");
            long secondsAgo = (long) executionStatus.get("lastExecuteSecondsAgo");
            
            String healthStatus;
            if (!autoEnabled) {
                healthStatus = "已禁用";
            } else if (isRunning) {
                healthStatus = "执行中";
            } else if (secondsAgo < 0) {
                healthStatus = "从未执行";
            } else if (secondsAgo <= 5) {
                healthStatus = "正常";
            } else if (secondsAgo <= 30) {
                healthStatus = "延迟";
            } else {
                healthStatus = "异常（超过30秒未执行）";
            }
            
            executionStatus.put("healthStatus", healthStatus);
            
            log.info("📊 [执行状态查询] 混合模式 - 健康状态: {}, 正在执行: {}, 上次执行: {}", 
                    healthStatus, isRunning, executionStatus.get("lastExecuteReadable"));
            
            return ResponseEntity.ok(Result.success(executionStatus));
            
        } catch (Exception e) {
            log.error("❌ [获取执行状态失败] {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("获取执行状态失败: " + e.getMessage()));
        }
    }

    /**
     * 手动测试添加访客到ACMS
     * 
     * @param request 添加访客请求参数
     * @return 添加结果
     */
    @PostMapping("/test-add-visitor")
    @ApiOperation(value = "测试添加访客", notes = "手动调用ACMS添加访客接口（用于测试）")
    public ResponseEntity<Result> testAddVisitor(@RequestBody AddVisitorTestRequest request) {
        try {
            log.info("🧪 [手动测试] 开始添加访客 - 车牌: {}, 访客: {}", request.getCarCode(), request.getOwner());
            
            // 构建ACMS请求
            AcmsVipService.AddVisitorCarRequest acmsRequest = new AcmsVipService.AddVisitorCarRequest();
            acmsRequest.setCarCode(request.getCarCode());
            acmsRequest.setOwner(request.getOwner());
            acmsRequest.setVisitName("访客二道岗通行");
            acmsRequest.setPhonenum(request.getPhonenum() != null ? request.getPhonenum() : "");
            acmsRequest.setReason("");
            acmsRequest.setOperator("外部系统操作员");
            acmsRequest.setOperateTime(getCurrentTime());
            
            // 设置访客时间
            AcmsVipService.VisitTime visitTime = new AcmsVipService.VisitTime();
            visitTime.setStart_time(request.getStartTime());
            visitTime.setEnd_time(request.getEndTime());
            acmsRequest.setVisitTime(visitTime);
            
            // 调用ACMS接口
            boolean success = acmsVipService.addVisitorCarToAcms(acmsRequest);
            
            if (success) {
                log.info("✅ [测试成功] 访客添加成功 - 车牌: {}, 访客: {}", request.getCarCode(), request.getOwner());
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "访客添加成功");
                result.put("carCode", request.getCarCode());
                result.put("owner", request.getOwner());
                result.put("visitName", "访客二道岗通行");
                result.put("startTime", request.getStartTime());
                result.put("endTime", request.getEndTime());
                return ResponseEntity.ok(Result.success(result));
            } else {
                log.warn("⚠️ [测试失败] 访客添加失败 - 车牌: {}", request.getCarCode());
                return ResponseEntity.ok(Result.error("访客添加失败，ACMS返回失败"));
            }
            
        } catch (Exception e) {
            log.error("❌ [测试异常] 添加访客异常: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("添加访客异常: " + e.getMessage()));
        }
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 添加访客测试请求参数
     */
    @Data
    public static class AddVisitorTestRequest {
        @ApiParam(value = "车牌号", required = true, example = "京A12345")
        private String carCode;
        
        @ApiParam(value = "访客姓名", required = true, example = "张三")
        private String owner;
        
        @ApiParam(value = "电话号码", required = false, example = "13800138000")
        private String phonenum;
        
        @ApiParam(value = "来访开始时间", required = true, example = "2025-01-10 12:00:00")
        private String startTime;
        
        @ApiParam(value = "来访结束时间", required = true, example = "2025-01-10 18:00:00")
        private String endTime;
    }
}

