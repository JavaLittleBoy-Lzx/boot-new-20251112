package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.entity.VisitorReservationSync;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 访客VIP自动开通服务
 * 定时从外部接口获取预约记录，自动开通VIP月票
 * 
 * @author System
 */
@Slf4j
@Service
public class VisitorVipAutoService {

    @Autowired
    private AcmsVipService acmsVipService;
    
    @Autowired(required = false)
    private VisitorReservationSyncService syncService;

    @Value("${visitor.api.url:}")
    private String visitorApiUrl;

    @Value("${visitor.api.page-size:1000}")
    private int pageSize;
    
    @Value("${visitor.api.app-key:}")
    private String appKey;

    @Value("${visitor.auto.enabled:false}")
    private boolean autoEnabled;
    
    // 智能调度配置
    @Value("${visitor.auto.schedule.mode:static}")
    private String scheduleMode;
    
    @Value("${visitor.auto.schedule.fixed-interval:2000}")
    private long fixedInterval;
    
    @Value("${visitor.auto.schedule.min-interval:2000}")
    private long minInterval;
    
    @Value("${visitor.auto.schedule.max-interval:30000}")
    private long maxInterval;
    
    @Value("${visitor.auto.schedule.idle-interval:10000}")
    private long idleInterval;
    
    @Value("${visitor.auto.schedule.no-change-threshold:5}")
    private int noChangeThreshold;
    
    @Value("${visitor.auto.schedule.time-based.enabled:true}")
    private boolean timeBasedEnabled;
    
    // 断路器配置
    @Value("${visitor.auto.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;
    
    @Value("${visitor.auto.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;
    
    @Value("${visitor.auto.circuit-breaker.timeout-seconds:60}")
    private int circuitBreakerTimeout;

    // 需要排除的VIP类型
    private static final List<String> EXCLUDED_VIP_TYPES = Arrays.asList(
        "体育馆自助访客",
        "体育馆访客车辆"
    );
    
    // 首次执行标志
    private volatile boolean isFirstExecution = true;
    
    // 执行状态监控
    private volatile boolean isRunning = false;  // 是否正在执行
    private volatile String lastExecuteTime = null;  // 最后执行时间
    private volatile String lastSuccessTime = null;  // 最后成功时间
    private volatile int lastTotalProcessed = 0;  // 最后处理总数
    private volatile int lastTotalSuccess = 0;  // 最后成功数
    private volatile int lastTotalFailed = 0;  // 最后失败数
    private volatile int lastTotalSkipped = 0;  // 最后跳过数
    private volatile long executionCount = 0;  // 总执行次数
    
    // 动态调度相关
    private volatile long currentInterval = 2000;  // 当前间隔
    private volatile int noChangeCount = 0;  // 连续无变化次数
    private volatile long lastExecuteTimeMillis = 0;  // 最后执行时间戳
    
    // 断路器相关
    private volatile int consecutiveFailures = 0;  // 连续失败次数
    private volatile boolean circuitOpen = false;  // 断路器是否开启
    private volatile long circuitOpenTime = 0;  // 断路器开启时间

    /**
     * 服务启动初始化
     */
    @PostConstruct
    public void init() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       访客VIP自动开通服务 - 启动配置（简化模式）                ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ 🔧 配置信息:                                                   ║");
        log.info("║   - 自动开通: {}                                          ║", autoEnabled ? "✅ 已启用" : "⏸️ 未启用");
        log.info("║   - 存储模式: 💾 数据库存储                                   ║");
        log.info("║   - 执行间隔: 📌 固定10秒                                      ║");
        log.info("║   - 查询范围: ⏱️  首次1天 / 常规10秒                           ║");
        log.info("║   - 外部接口: {}                                   ║", visitorApiUrl);
        log.info("║   - 分页大小: {} 条/页                                         ║", pageSize);
        log.info("║   - 处理模式: ✅ 仅新增（检查reservation_id是否存在）          ║");
        log.info("║   - 断路器: {}                                            ║", 
            circuitBreakerEnabled ? "✅ 已启用" : "⏸️ 未启用");
        log.info("║                                                                ║");
        log.info("║ 📋 功能说明:                                                   ║");
        log.info("║   1. 首次执行：查询最近1天数据                                 ║");
        log.info("║   2. 后续执行：每10秒查询前10秒到当前时间的数据                ║");
        log.info("║   3. 检查预约记录ID是否已存在于数据库                         ║");
        log.info("║   4. 不存在则添加到数据库，已存在则跳过                       ║");
        log.info("║   5. 不进行修改和删除操作                                    ║");
        log.info("║                                                                ║");
        if (autoEnabled) {
            log.info("║ 🚀 状态: 定时任务已启动，正在监听...                          ║");
            log.info("║ 💡 提示: 首次执行将查询最近1天数据，后续每10秒查询前10秒数据 ║");
        } else {
            log.info("║ ⏸️  状态: 自动开通未启用                                      ║");
            log.info("║ 💡 提示: 若要启用，请设置 visitor.auto.enabled=true          ║");
        }
        log.info("╚════════════════════════════════════════════════════════════════╝");
    }

    /**
     * 定时调度器：每10秒执行一次
     */
    @Scheduled(fixedRate = 10000)  // 固定每10秒执行
    public void smartScheduler() {
        if (!autoEnabled) {
            return;
        }
        // 执行任务
        autoProcessVisitorVip();
    }

    /**
     * 定时任务：从外部接口获取预约记录并添加到数据库
     * 每10秒执行一次，查询前10秒到当前时间的数据
     */
    public void autoProcessVisitorVip() {
        if (!autoEnabled) {
            log.debug("⏸️ [访客VIP自动开通] 功能未启用");
            return;
        }

        // 检查是否已在执行（防止并发）
        if (isRunning) {
            log.warn("⚠️ [访客VIP自动开通] 上次任务尚未完成，跳过本次执行");
            return;
        }

        // 断路器检查
        if (circuitBreakerEnabled && circuitOpen) {
            if (shouldRetryCircuit()) {
                log.info("🔄 [断路器] 尝试恢复连接...");
                circuitOpen = false;
                consecutiveFailures = 0;
            } else {
                log.debug("⚠️ [断路器] 熔断中，跳过本次执行");
                lastExecuteTimeMillis = System.currentTimeMillis();
                return;
            }
        }

        // 标记开始执行
        isRunning = true;
        executionCount++;
        lastExecuteTime = getCurrentTime();
        lastExecuteTimeMillis = System.currentTimeMillis();

        // 判断是否为首次执行
        boolean isFirst = isFirstExecution;
        if (isFirst) {
            log.info("🔄 [访客VIP自动开通] 开始执行定时任务 [首次执行] [执行间隔:10秒] [查询范围:前1天~当前时间]");
        } else {
            log.info("🔄 [访客VIP自动开通] 开始执行定时任务 [第{}次执行] [执行间隔:10秒] [查询范围:前10秒~当前时间]",
                executionCount);
        }

        try {
            // 获取当前时间作为查询结束时间
            LocalDateTime now = LocalDateTime.now();
            String endTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String startTime;
            
            // 首次执行：查询前1天数据；后续执行：查询前10秒数据
            if (isFirst) {
                startTime = now.minusDays(2)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                log.info("📅 [首次执行] 查询时间范围: {} ~ {}", startTime, endTime);
            } else {
                startTime = now.minusSeconds(10)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                log.info("📅 [常规查询] 查询时间范围: {} ~ {}", startTime, endTime);
            }

            // 收集所有数据
            List<VisitorReservation> allReservations = new ArrayList<>();
            
            // 自动分页：循环调用接口，直到返回的数据长度 < pageSize
            int currentPage = 1;
            boolean hasMoreData = true;
            String executionType = isFirst ? "首次执行" : "常规执行";
            
            log.info("🔄 [{}] 开始自动分页查询，时间范围: {} ~ {}", executionType, startTime, endTime);
            
            while (hasMoreData) {
                List<VisitorReservation> pageData = fetchVisitorReservations(startTime, endTime, currentPage, pageSize);
                
                if (pageData == null) {
                    log.warn("⚠️ [{}-第{}页] fetchVisitorReservations 返回 null，停止分页", executionType, currentPage);
                    handleFailure();
                    break;
                }
                
                int pageDataSize = pageData.size();
                log.info("📄 [{}-第{}页] 获取到 {} 条预约记录", executionType, currentPage, pageDataSize);
                
                // 将当前页数据添加到总集合中
                if (!pageData.isEmpty()) {
                    allReservations.addAll(pageData);
                    if (currentPage == 1 && !pageData.isEmpty()) {
                        log.info("📄 [{}-第{}页] 第一条记录ID: {}", executionType, currentPage, pageData.get(0).getReservationId());
                    }
                }
                
                // 判断是否需要继续分页
                // 如果当前页数据量 < pageSize，说明已经是最后一页
                if (pageDataSize < pageSize) {
                    hasMoreData = false;
                    log.info("✅ [{}] 第{}页数据量 ({}) < pageSize ({}), 已到达最后一页，停止分页", 
                            executionType, currentPage, pageDataSize, pageSize);
                } else if (pageDataSize == pageSize) {
                    // 如果当前页数据量 == pageSize，可能还有下一页，继续查询
                    currentPage++;
                    log.info("🔄 [{}] 第{}页数据量 ({}) == pageSize ({}), 继续查询第{}页", 
                            executionType, currentPage - 1, pageDataSize, pageSize, currentPage);
                } else {
                    // 理论上不应该出现 > pageSize 的情况，但为了安全起见也处理
                    log.warn("⚠️ [{}] 第{}页数据量 ({}) > pageSize ({}), 异常情况，停止分页", 
                            executionType, currentPage, pageDataSize, pageSize);
                    hasMoreData = false;
                }
                
                // 安全限制：防止无限循环，最多查询100页
                if (currentPage > 100) {
                    log.warn("⚠️ [{}] 已查询{}页，达到安全限制，停止分页", executionType, currentPage);
                    hasMoreData = false;
                }
            }
            
            log.info("📊 [{}] 分页查询完成，共查询 {} 页，总计 {} 条记录", 
                    executionType, currentPage, allReservations.size());

            // 如果没有查询到数据
            if (allReservations.isEmpty()) {
                log.info("📭 [查询结果] 没有数据");
                lastTotalProcessed = 0;
                lastTotalSuccess = 0;
                lastTotalFailed = 0;
                lastTotalSkipped = 0;
                lastSuccessTime = getCurrentTime();
                
                // 成功执行（虽然没有数据），重置断路器
                if (circuitBreakerEnabled) {
                    consecutiveFailures = 0;
                }
                
                // 标记首次执行完成
                if (isFirst) {
                    isFirstExecution = false;
                    log.info("ℹ️ [首次执行完成] 后续将每10秒查询前10秒数据");
                }
                
                log.info("✅ [定时任务完成] 无数据，累计执行: {}次", executionCount);
                return;
            }

            log.info("📄 [查询结果] 共获取到 {} 条预约记录", allReservations.size());

            // 处理数据：只检查是否存在，不存在则添加
            int totalProcessed = 0;
            int totalAdded = 0;
            int totalSkipped = 0;
            log.info("📊 [数据统计] 查询数据总和: {} 条", allReservations.size());
            log.info("📊 [服务检查] syncService: {}", syncService != null ? "已注入" : "未注入");
            
            // 验证数据有效性
            int validCount = 0;
            int invalidCount = 0;
            for (VisitorReservation r : allReservations) {
                if (r != null && r.getReservationId() != null && !r.getReservationId().isEmpty()) {
                    validCount++;
                } else {
                    invalidCount++;
                }
            }
            log.info("📊 [数据验证] 有效记录: {} 条, 无效记录: {} 条", validCount, invalidCount);
            
            if (syncService != null) {
                log.info("🔄 [开始处理] 开始将 {} 条记录添加到数据库", allReservations.size());
                // 处理每条记录：检查是否存在，不存在则添加
                for (VisitorReservation reservation : allReservations) {
                    totalProcessed++;
                    
                    try {
                        // 直接使用 Service 方法：内部会检查是否存在，不存在则插入
                        boolean added = syncService.insertIfNotExists(reservation);
                        
                        if (added) {
                            totalAdded++;
                            log.info("✅ [新增] 预约ID: {}, 访客: {}, 车牌: {} - 已添加到数据库",
                                reservation.getReservationId(), 
                                reservation.getVisitorName(), 
                                reservation.getCarNumber());
                        } else {
                            // 已存在或插入失败，跳过
                            totalSkipped++;
                            // 改为info级别，方便查看哪些记录被跳过
                            if (totalSkipped <= 10) {
                                // 前10条跳过的记录输出详细信息
                                log.info("⏭️ [跳过] 预约ID: {}, 访客: {}, 车牌: {} - 已存在或插入失败", 
                                    reservation.getReservationId(), 
                                    reservation.getVisitorName(),
                                    reservation.getCarNumber());
                            } else if (totalSkipped == 11) {
                                // 第11条开始，每100条输出一次统计
                                log.info("⏭️ [跳过统计] 已有 {} 条记录被跳过（已存在或插入失败），后续将每100条输出一次统计", totalSkipped);
                            } else if (totalSkipped % 100 == 0) {
                                log.info("⏭️ [跳过统计] 已跳过 {} 条记录", totalSkipped);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ [添加异常] 预约ID: {}, 错误: {}", 
                            reservation.getReservationId(), e.getMessage(), e);
                        totalSkipped++;
                    }
                }
            } else {
                log.error("❌ [错误] VisitorReservationSyncService 未注入，无法执行数据添加");
            }
            
            // 标记首次执行完成
            if (isFirst) {
                isFirstExecution = false;
                log.info("ℹ️ [首次执行完成] 后续将每10秒查询前10秒数据");
            }
            
            // 更新执行统计
            lastTotalProcessed = totalProcessed;
            lastTotalSuccess = totalAdded;
            lastTotalFailed = 0;
            lastTotalSkipped = totalSkipped;
            lastSuccessTime = getCurrentTime();

            // 成功执行，重置断路器
            if (circuitBreakerEnabled) {
                consecutiveFailures = 0;
            }

            // 输出统计信息
            log.info("✅ [定时任务完成] 总计: {}, 新增: {}, 跳过: {}, 累计执行: {}次",
                totalProcessed, totalAdded, totalSkipped, executionCount);

        } catch (Exception e) {
            log.error("❌ [定时任务异常] 执行失败: {}", e.getMessage(), e);
            handleFailure();
        } finally {
            // 标记执行结束
            isRunning = false;
        }
    }

    /**
     * 获取执行状态信息
     *
     * @return 执行状态详情
     */
    public Map<String, Object> getExecutionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", isRunning);
        status.put("autoEnabled", autoEnabled);
        status.put("lastExecuteTime", lastExecuteTime);
        status.put("lastSuccessTime", lastSuccessTime);
        status.put("executionCount", executionCount);
        status.put("lastTotalProcessed", lastTotalProcessed);
        status.put("lastTotalSuccess", lastTotalSuccess);
        status.put("lastTotalFailed", lastTotalFailed);
        status.put("lastTotalSkipped", lastTotalSkipped);

        // 动态调度信息
        status.put("scheduleMode", scheduleMode);
        status.put("currentInterval", currentInterval / 1000 + "秒");
        status.put("noChangeCount", noChangeCount);

        // 断路器信息
        status.put("circuitOpen", circuitOpen);
        status.put("consecutiveFailures", consecutiveFailures);
        
        // 存储模式信息
        status.put("storageMode", "hybrid");
        status.put("storageModeDescription", "数据库+Hash混合存储");

        // 计算成功率
        if (lastTotalProcessed > 0) {
            double successRate = (double) lastTotalSuccess / lastTotalProcessed * 100;
            status.put("successRate", String.format("%.2f%%", successRate));
        } else {
            status.put("successRate", "N/A");
        }

        // 计算上次执行距今时间
        if (lastExecuteTime != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime lastExec = LocalDateTime.parse(lastExecuteTime, formatter);
                LocalDateTime now = LocalDateTime.now();
                long secondsAgo = java.time.Duration.between(lastExec, now).getSeconds();
                status.put("lastExecuteSecondsAgo", secondsAgo);
                status.put("lastExecuteReadable", formatDuration(secondsAgo));
            } catch (Exception e) {
                status.put("lastExecuteSecondsAgo", -1);
                status.put("lastExecuteReadable", "未知");
            }
        } else {
            status.put("lastExecuteSecondsAgo", -1);
            status.put("lastExecuteReadable", "从未执行");
        }

        return status;
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒前";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else {
            return (seconds / 86400) + "天前";
        }
    }


    /**
     * 从外部接口获取预约记录
     * 接口：/api-applypass/api-applypass/visit-terminal/outsideApplyList
     *
     * @param startTime 开始时间（接口参数：beginTime，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（接口参数：endTime，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageNum 当前页码（接口参数：pageIndex）
     * @param pageSize 每页条数（接口参数：pageSize）
     * @return 预约记录列表
     */
    private List<VisitorReservation> fetchVisitorReservations(
            String startTime, String endTime, int pageNum, int pageSize) {

        try {
            // 构建请求URL
           String url = visitorApiUrl + "/api-applypass/api-applypass/visit-terminal/outsideApplyList";
            // String url = visitorApiUrl + "/parking/nefuData/page";
            // 构建请求参数
            Map<String, Object> params = new HashMap<>();
            // 根据接口文档，使用 beginTime 和 endTime，pageIndex 和 pageSize
            params.put("beginTime", startTime);     // 开始时间（申请开始时间）
            params.put("endTime", endTime);         // 结束时间（申请结束时间）
            params.put("pageIndex", pageNum);       // 当前页码（注意：接口参数名是 pageIndex，不是 pageNum）
            params.put("pageSize", pageSize);       // 每页条数
            
            // 添加 appKey
            if (StringUtils.hasText(appKey)) {
                params.put("appKey", appKey);
            }
            
            // 生成 sign 签名
            String sign = generateSign(params);
            params.put("sign", sign);
            
            log.info("🔐 [签名信息] appKey: {}, sign: {}", appKey, sign);

            // 转换为JSON字符串
            String jsonParams = JSON.toJSONString(params);

            log.info("📤 [外部接口请求] URL: {}, 时间区间: {} ~ {}, 页码: {}, 每页: {}条",
                    url, startTime, endTime, pageNum, pageSize);
            log.info("📤 [请求参数JSON] {}", jsonParams);

            // 调用外部接口（POST JSON方式）
            String response = HttpClientUtil.doPostJson(url, jsonParams);
            
            // 添加响应日志
            if (response != null) {
                log.info("📥 [外部接口响应] 响应长度: {} 字符", response.length());
                // 只打印前500个字符，避免日志过长
                String responsePreview = response.length() > 500 ? response.substring(0, 500) + "..." : response;
                log.debug("📥 [外部接口响应预览] {}", responsePreview);
            } else {
                log.warn("⚠️ [外部接口响应] 响应为null");
            }
            
            // 解析响应
            List<VisitorReservation> result = parseVisitorReservations(response);
            if (result != null) {
                log.info("✅ [数据解析完成] 解析到 {} 条记录", result.size());
            } else {
                log.warn("⚠️ [数据解析完成] 解析结果为null");
            }
            return result;

        } catch (Exception e) {
            log.error("❌ [外部接口调用失败] {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析预约记录响应
     */
    private List<VisitorReservation> parseVisitorReservations(String response) {
        log.info("🔍 [开始解析] parseVisitorReservations 方法被调用");
        
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ [解析失败] 响应内容为空或null");
            return null;
        }

        try {
            log.info("🔍 [解析步骤1] 开始解析JSON响应，响应长度: {}", response.length());
            JSONObject jsonResponse = JSON.parseObject(response);
            log.info("🔍 [解析步骤2] JSON解析成功");

            // 根据实际响应格式调整解析逻辑
            String code = jsonResponse.getString("code");
            log.info("🔍 [解析步骤3] 获取到code字段: {}", code);
            
            // 外部接口成功状态码：0, 200, 600
            if (!"0".equals(code) && !"200".equals(code) && !"600".equals(code)) {
                log.warn("⚠️ [外部接口] 返回错误 - Code: {}, Message: {}", code, jsonResponse.getString("msg"));
                return null;
            } else {
                log.info("✅ [外部接口] 响应成功 - Code: {}", code);
            }

            // 获取数据列表（根据实际响应结构调整）
            Object dataObj = jsonResponse.get("data");
            log.info("🔍 [解析步骤4] 获取data字段，类型: {}", dataObj != null ? dataObj.getClass().getSimpleName() : "null");
            
            if (dataObj == null) {
                log.warn("⚠️ [数据解析] data字段为null");
                return null;
            }

            // 1. 如果data直接是数组（最常见的情况）
            if (jsonResponse.get("data") instanceof com.alibaba.fastjson.JSONArray) {
                com.alibaba.fastjson.JSONArray dataArray = jsonResponse.getJSONArray("data");
                log.info("🔍 [解析步骤5] data是数组类型，数组大小: {}", dataArray.size());
                
                List<VisitorReservation> result = new ArrayList<>();
                
                // 打印第一条记录的完整JSON，用于调试字段名
                if (dataArray.size() > 0) {
                    JSONObject firstItem = dataArray.getJSONObject(0);
                    log.info("📋 [数据样例] 第一条记录的完整JSON: {}", JSON.toJSONString(firstItem));
                    log.info("📋 [字段检查] 关键字段存在情况:");
                    log.info("   - id: {}", firstItem.containsKey("id") ? firstItem.getString("id") : "不存在");
                    log.info("   - visitorPlateNumber: {}", firstItem.containsKey("visitorPlateNumber") ? firstItem.getString("visitorPlateNumber") : "不存在");
                    log.info("   - visitorUserName: {}", firstItem.containsKey("visitorUserName") ? firstItem.getString("visitorUserName") : "不存在");
                }
                
                // 使用processRecordsArray方法处理，统一处理逻辑
                log.info("🔍 [解析步骤6] 开始处理数组数据");
                result = processRecordsArray(dataArray);
                
                log.info("✅ [数据解析] data直接是数组，解析到 {} 条记录", result.size());
                return result;
            }

            // 2. 处理嵌套的data结构（对象格式）
            if (dataObj instanceof JSONObject) {
                log.info("🔍 [解析步骤5] data是对象类型，检查嵌套结构");
                JSONObject dataJson = (JSONObject) dataObj;

                // 检查是否有嵌套的data字段（双层嵌套）
                if (dataJson.containsKey("data")) {
                    log.info("🔍 [解析步骤5.1] 发现嵌套data字段");
                    Object innerDataObj = dataJson.get("data");
                    if (innerDataObj instanceof JSONObject) {
                        JSONObject innerDataJson = (JSONObject) innerDataObj;
                        // 从内层data中获取records
                        if (innerDataJson.containsKey("records")) {
                            com.alibaba.fastjson.JSONArray recordsArray = innerDataJson.getJSONArray("records");
                            log.info("🔍 [解析步骤5.2] 从data.data.records获取数组，大小: {}", recordsArray.size());
                            List<VisitorReservation> result = processRecordsArray(recordsArray);
                            log.info("✅ [数据解析] 双层嵌套data.data.records，解析到 {} 条记录", result.size());
                            return result;
                        }
                    }
                }

                // 处理单层嵌套（直接有records）
                if (dataJson.containsKey("records")) {
                    com.alibaba.fastjson.JSONArray recordsArray = dataJson.getJSONArray("records");
                    log.info("🔍 [解析步骤5.3] 从data.records获取数组，大小: {}", recordsArray.size());
                    List<VisitorReservation> result = processRecordsArray(recordsArray);
                    log.info("✅ [数据解析] data.records，解析到 {} 条记录", result.size());
                    return result;
                }
                
                // 检查是否有list字段
                if (dataJson.containsKey("list")) {
                    com.alibaba.fastjson.JSONArray listArray = dataJson.getJSONArray("list");
                    log.info("🔍 [解析步骤5.4] 从data.list获取数组，大小: {}", listArray.size());
                    List<VisitorReservation> result = processRecordsArray(listArray);
                    log.info("✅ [数据解析] data.list，解析到 {} 条记录", result.size());
                    return result;
                }
            }

            log.warn("⚠️ [数据解析] 无法识别的数据结构，data类型: {}, 内容片段: {}",
                    dataObj.getClass().getSimpleName(),
                    JSON.toJSONString(dataObj).substring(0, Math.min(200, JSON.toJSONString(dataObj).length())));
            return null;

        } catch (Exception e) {
            log.error("❌ 解析预约记录响应失败", e);
            return null;
        }
    }

    /**
     * 处理记录数组，支持多种字段名格式
     * 
     * @param recordsArray JSON数组
     * @return VisitorReservation列表
     */
    private List<VisitorReservation> processRecordsArray(com.alibaba.fastjson.JSONArray recordsArray) {
        log.info("🔄 [处理数组] 开始处理记录数组，数组大小: {}", recordsArray.size());
        List<VisitorReservation> result = new ArrayList<>();
        
        if (recordsArray.size() == 0) {
            log.warn("⚠️ [处理数组] 数组为空");
            return result;
        }
        
        // 打印第一条记录的完整JSON，用于调试字段名
        if (recordsArray.size() > 0) {
            JSONObject firstItem = recordsArray.getJSONObject(0);
            log.info("📋 [处理数组] 第一条记录的完整JSON: {}", JSON.toJSONString(firstItem));
            log.info("📋 [处理数组] 关键字段检查:");
            log.info("   - id: {}", firstItem.containsKey("id") ? firstItem.getString("id") : "不存在");
            log.info("   - visitorPlateNumber: {}", firstItem.containsKey("visitorPlateNumber") ? firstItem.getString("visitorPlateNumber") : "不存在");
            log.info("   - visitorUserName: {}", firstItem.containsKey("visitorUserName") ? firstItem.getString("visitorUserName") : "不存在");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < recordsArray.size(); i++) {
            try {
                JSONObject item = recordsArray.getJSONObject(i);
                VisitorReservation reservation = item.toJavaObject(VisitorReservation.class);
                
                if (reservation == null) {
                    log.warn("⚠️ [处理数组] 第{}条记录转换失败，reservation为null", i + 1);
                    failCount++;
                    continue;
                }
                
                // 检查关键字段 - reservationId 是必需的
                if (reservation.getReservationId() == null || reservation.getReservationId().isEmpty()) {
                    // 尝试从原始JSON中获取id字段（可能是数字类型）
                    Object idObj = item.get("id");
                    if (idObj != null) {
                        reservation.setReservationId(String.valueOf(idObj));
                        log.info("🔧 [处理数组] 第{}条记录，从id字段转换reservationId: {}", i + 1, reservation.getReservationId());
                    } else {
                        log.warn("⚠️ [处理数组] 第{}条记录缺少reservationId，跳过此条记录", i + 1);
                        failCount++;
                        continue;
                    }
                }
                
                // 验证其他关键字段
                if ((reservation.getVisitorName() == null || reservation.getVisitorName().isEmpty()) &&
                    (reservation.getCarNumber() == null || reservation.getCarNumber().isEmpty())) {
                    log.warn("⚠️ [处理数组] 第{}条记录缺少访客姓名和车牌号，可能数据不完整", i + 1);
                }
            
            // 尝试多种字段名格式来获取网关通行时间
            if (reservation.getGatewayTransitBeginTime() == null || reservation.getGatewayTransitBeginTime().isEmpty()) {
                // 尝试下划线格式
                String beginTime = item.getString("gateway_transit_begin_time");
                if (beginTime != null && !beginTime.isEmpty()) {
                    reservation.setGatewayTransitBeginTime(beginTime);
                    log.debug("✅ [字段映射] 使用下划线格式获取网关通行开始时间: {}", beginTime);
                } else {
                    // 尝试其他可能的字段名
                    beginTime = item.getString("gatewayTransitBegin");
                    if (beginTime != null && !beginTime.isEmpty()) {
                        reservation.setGatewayTransitBeginTime(beginTime);
                        log.debug("✅ [字段映射] 使用gatewayTransitBegin获取网关通行开始时间: {}", beginTime);
                    }
                }
            }
            
            if (reservation.getGatewayTransitEndTime() == null || reservation.getGatewayTransitEndTime().isEmpty()) {
                // 尝试下划线格式
                String endTime = item.getString("gateway_transit_end_time");
                if (endTime != null && !endTime.isEmpty()) {
                    reservation.setGatewayTransitEndTime(endTime);
                    log.debug("✅ [字段映射] 使用下划线格式获取网关通行结束时间: {}", endTime);
                } else {
                    // 尝试其他可能的字段名
                    endTime = item.getString("gatewayTransitEnd");
                    if (endTime != null && !endTime.isEmpty()) {
                        reservation.setGatewayTransitEndTime(endTime);
                        log.debug("✅ [字段映射] 使用gatewayTransitEnd获取网关通行结束时间: {}", endTime);
                    }
                }
            }
            
                // 验证数据完整性后再添加
                if (reservation.getReservationId() != null && !reservation.getReservationId().isEmpty()) {
                    result.add(reservation);
                    successCount++;
                    
                    // 每处理100条记录输出一次进度
                    if (successCount % 100 == 0) {
                        log.info("📊 [处理进度] 已成功处理 {} 条记录", successCount);
                    }
                } else {
                    log.warn("⚠️ [处理数组] 第{}条记录reservationId仍为空，跳过", i + 1);
                    failCount++;
                }
            
            } catch (Exception e) {
                log.error("❌ [处理数组] 第{}条记录处理失败: {}", i + 1, e.getMessage(), e);
                // 输出失败的记录内容，便于调试
                try {
                    if (i < recordsArray.size()) {
                        JSONObject failedItem = recordsArray.getJSONObject(i);
                        log.error("❌ [失败记录内容] {}", JSON.toJSONString(failedItem));
                    }
                } catch (Exception ex) {
                    // 忽略输出失败记录的异常
                }
                failCount++;
            }
        }
        
        log.info("✅ [处理数组] 处理完成 - 成功: {} 条, 失败: {} 条, 总计: {} 条", 
                successCount, failCount, recordsArray.size());
        
        return result;
    }

    /**
     * 判断VIP类型是否需要排除
     * 通过 customVipName 字段判断是否为排除类型
     * 
     * @param vipTypeName VIP类型名称（对应接口字段：customVipName）
     * @return true-需要排除，false-不需要排除
     */
    private boolean shouldExclude(String vipTypeName) {
        if (!StringUtils.hasText(vipTypeName)) {
            return false;
        }

        return EXCLUDED_VIP_TYPES.stream()
                .anyMatch(excludedType -> vipTypeName.contains(excludedType));
    }

    /**
     * 检查车牌号是否有效
     * 
     * @param carNumber 车牌号
     * @return true-有效，false-无效（null或空字符串）
     */
    private boolean isValidCarNumber(String carNumber) {
        return StringUtils.hasText(carNumber) && !carNumber.trim().isEmpty();
    }

    /**
     * 调用ACMS添加访客接口
     * 
     * @param reservation 访客预约记录
     * @return 是否添加成功
     */
    private boolean addVisitorToAcms(VisitorReservation reservation) {
        try {
            log.info("📝 [ACMS-添加访客] 开始调用 - 预约ID: {}, 访客: {}, 车牌: {}",
                    reservation.getReservationId(),
                    reservation.getVisitorName(),
                    reservation.getCarNumber());

            // 构建添加访客请求
            AcmsVipService.AddVisitorCarRequest request = new AcmsVipService.AddVisitorCarRequest();
            
            // 基本信息
            request.setCarCode(reservation.getCarNumber());
            request.setOwner(reservation.getVisitorName());
            request.setVisitName("访客二道岗通行");  // 固定使用"访客二道岗通行"
            request.setPhonenum(reservation.getVisitorPhone() != null ? reservation.getVisitorPhone() : "");
            request.setReason("");  // 来访原因设为空
            request.setOperator("FKJK");  // 固定操作员
            request.setOperateTime(getCurrentTime());
            
            // 设置访客时间：优先使用网关通行时间，如果为空则使用预约的开始/结束时间
            String startTime = reservation.getGatewayTransitBeginTime();
            String endTime = reservation.getGatewayTransitEndTime();
            
            // 如果网关通行时间为空，使用预约的开始结束时间
            if (!StringUtils.hasText(startTime)) {
                startTime = reservation.getStartTime();
                log.debug("🔄 [时间字段] 预约ID: {}, 使用 beginTime 作为开始时间: {}", 
                    reservation.getReservationId(), startTime);
            }
            if (!StringUtils.hasText(endTime)) {
                endTime = reservation.getEndTime();
                log.debug("🔄 [时间字段] 预约ID: {}, 使用 endTime 作为结束时间: {}", 
                    reservation.getReservationId(), endTime);
            }

            // 校验时间字段
            if (!StringUtils.hasText(startTime) || !StringUtils.hasText(endTime)) {
                log.warn("⚠️ [时间缺失] 预约ID: {}, 开始时间: {}, 结束时间: {}, 跳过添加访客",
                        reservation.getReservationId(), startTime, endTime);
                return false;
            }

            // 创建访客时间对象
            AcmsVipService.VisitTime visitTime = new AcmsVipService.VisitTime();
            visitTime.setStart_time(startTime);
            visitTime.setEnd_time(endTime);
            request.setVisitTime(visitTime);

            // 记录访客添加信息
            String timeSource = StringUtils.hasText(reservation.getGatewayTransitBeginTime()) 
                ? "网关通行时间" : "预约时间(beginTime/endTime)";
            log.info("📅 [访客添加信息] 预约ID: {}, 访客类型: 访客二道岗通行, 来访时间: {} 至 {} (来源: {})",
                    reservation.getReservationId(), startTime, endTime, timeSource);

            // 调用添加访客服务
            boolean success = acmsVipService.addVisitorCarToAcms(request);

            if (success) {
                log.info("✅ [访客添加成功] 预约ID: {}, 访客: {}, 车牌: {}, 来访时间: {} 至 {} ({})",
                        reservation.getReservationId(), 
                        reservation.getVisitorName(), 
                        reservation.getCarNumber(),
                        startTime,
                        endTime,
                        timeSource);
            } else {
                log.warn("⚠️ [访客添加失败] 预约ID: {}", reservation.getReservationId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [添加访客异常] 预约ID: {}, 错误: {}", 
                    reservation.getReservationId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 为访客添加到ACMS系统（仅调用添加访客接口）
     */
    private boolean openVipTicketForVisitor(VisitorReservation reservation) {
        try {
            log.info("📝 [添加访客] 开始处理 - 预约ID: {}, 访客: {}, 车牌: {}",
                    reservation.getReservationId(),
                    reservation.getVisitorName(),
                    reservation.getCarNumber());

            // 只调用ACMS添加访客接口，不再开通VIP票
            boolean visitorAdded = addVisitorToAcms(reservation);
            
            if (visitorAdded) {
                log.info("✅ [添加访客成功] 预约ID: {}, 访客: {}, 车牌: {}",
                        reservation.getReservationId(), 
                        reservation.getVisitorName(), 
                        reservation.getCarNumber());
            } else {
                log.warn("⚠️ [添加访客失败] 预约ID: {}", reservation.getReservationId());
            }
            
            return visitorAdded;
            
        } catch (Exception e) {
            log.error("❌ [添加访客异常] 预约ID: {}, 错误: {}", 
                    reservation.getReservationId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成票号
     */
    private String generateTicketNo(String reservationId) {
        return "VISITOR_" + reservationId + "_" + System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 动态调整轮询间隔
     * 
     * @param totalAdded 新增数量
     * @param totalModified 修改数量
     */
    private void adjustInterval(int totalAdded, int totalModified) {
        int totalChanges = totalAdded + totalModified;
        
        if (totalChanges == 0) {
            // 无变化，增加无变化计数
            noChangeCount++;
            
            if (noChangeCount >= noChangeThreshold) {
                // 连续无变化，降低频率
                long oldInterval = currentInterval;
                currentInterval = Math.min(currentInterval * 2, maxInterval);
                
                if (oldInterval != currentInterval) {
                    log.info("🐌 [动态调度] 连续{}次无变化，降低频率: {}秒 -> {}秒", 
                        noChangeCount, oldInterval / 1000, currentInterval / 1000);
                }
            }
        } else {
            // 有变化，恢复高频轮询
            if (currentInterval > minInterval) {
                log.info("🚀 [动态调度] 检测到{}条变化，恢复高频轮询: {}秒 -> {}秒", 
                    totalChanges, currentInterval / 1000, minInterval / 1000);
            }
            currentInterval = minInterval;
            noChangeCount = 0;
        }
        
        // 根据时间段调整
        if (timeBasedEnabled) {
            adjustIntervalByTimeOfDay();
        }
    }
    
    /**
     * 根据时间段调整轮询间隔
     */
    private void adjustIntervalByTimeOfDay() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        long oldInterval = currentInterval;
        
        // 夜间（23:00 - 08:00）：降低频率
        if (hour >= 23 || hour < 8) {
            currentInterval = Math.max(currentInterval, maxInterval);
            if (oldInterval != currentInterval) {
                log.info("🌙 [动态调度] 夜间时段，降低频率至{}秒", currentInterval / 1000);
            }
        }
        // 工作时间（08:00 - 18:00）：保持高频
        else if (hour >= 8 && hour < 18) {
            // 如果当前间隔过大，适当缩短
            if (currentInterval > idleInterval) {
                currentInterval = idleInterval;
                if (oldInterval != currentInterval) {
                    log.info("☀️ [动态调度] 工作时段，调整频率至{}秒", currentInterval / 1000);
                }
            }
        }
        // 下班时间（18:00 - 23:00）：中等频率
        else {
            if (currentInterval < idleInterval) {
                currentInterval = idleInterval;
                if (oldInterval != currentInterval) {
                    log.info("🌆 [动态调度] 下班时段，调整频率至{}秒", currentInterval / 1000);
                }
            }
        }
    }
    
    /**
     * 断路器是否应该重试
     */
    private boolean shouldRetryCircuit() {
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - circuitOpenTime) / 1000;
        return elapsedSeconds >= circuitBreakerTimeout;
    }
    
    /**
     * 处理失败（断路器逻辑）
     */
    private void handleFailure() {
        if (!circuitBreakerEnabled) {
            return;
        }
        
        consecutiveFailures++;
        log.error("❌ [外部接口] 调用失败 ({}/{})", consecutiveFailures, failureThreshold);
        
        // 连续失败达到阈值，开启断路器
        if (consecutiveFailures >= failureThreshold && !circuitOpen) {
            circuitOpen = true;
            circuitOpenTime = System.currentTimeMillis();
            log.error("🔴 [断路器] 连续失败{}次，开启断路器，暂停调用{}秒", 
                consecutiveFailures, circuitBreakerTimeout);
        }
    }
    
    /**
     * 生成签名 sign
     * 签名规则：将所有参数（排除 sign 本身）按 key 字母顺序排序，拼接成 key=value&key=value 格式，然后进行 MD5 加密
     * 
     * @param params 请求参数Map
     * @return 签名字符串（MD5，小写）
     */
    private String generateSign(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        try {
            // 创建排序后的参数Map（排除 sign 本身）
            TreeMap<String, Object> sortedParams = new TreeMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                // 排除 sign 本身，避免循环依赖
                if (!"sign".equalsIgnoreCase(key) && entry.getValue() != null) {
                    sortedParams.put(key, entry.getValue());
                }
            }
            
            // 拼接参数字符串：key=value&key=value
            StringBuilder signStr = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                if (signStr.length() > 0) {
                    signStr.append("&");
                }
                signStr.append(entry.getKey()).append("=").append(entry.getValue());
            }
            
            String signString = signStr.toString();
            log.debug("🔐 [签名原始字符串] {}", signString);
            
            // MD5 加密
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(signString.getBytes("UTF-8"));
            
            // 转换为十六进制字符串（小写）
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String sign = hexString.toString();
            log.debug("🔐 [签名结果] {}", sign);
            return sign;
            
        } catch (Exception e) {
            log.error("❌ [签名生成失败] {}", e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * 将VisitorReservationSync转换为VisitorReservation
     */
    private VisitorReservation convertSyncToReservation(VisitorReservationSync entity) {
        VisitorReservation reservation = new VisitorReservation();
        reservation.setReservationId(entity.getReservationId());
        reservation.setVisitorName(entity.getVisitorName());
        reservation.setVisitorPhone(entity.getVisitorPhone());
        reservation.setVisitorIdCard(entity.getVisitorIdCard());
        reservation.setPassDep(entity.getPassDep());
        reservation.setCarNumber(entity.getCarNumber());
        reservation.setVipTypeName(entity.getVipTypeName());
        reservation.setParkName(entity.getParkName());
        reservation.setRemark1(entity.getRemark1());
        reservation.setRemark2(entity.getRemark2());
        reservation.setRemark3(entity.getRemark3());
        
        // 转换时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (entity.getStartTime() != null) {
            reservation.setStartTime(sdf.format(entity.getStartTime()));
        }
        if (entity.getEndTime() != null) {
            reservation.setEndTime(sdf.format(entity.getEndTime()));
        }
        if (entity.getExternalCreateTime() != null) {
            reservation.setCreateTime(sdf.format(entity.getExternalCreateTime()));
        }
        
        // 🔧 修复：添加网关通行时间字段的转换
        if (entity.getGatewayTransitBeginTime() != null) {
            reservation.setGatewayTransitBeginTime(sdf.format(entity.getGatewayTransitBeginTime()));
        }
        if (entity.getGatewayTransitEndTime() != null) {
            reservation.setGatewayTransitEndTime(sdf.format(entity.getGatewayTransitEndTime()));
        }
        
        return reservation;
    }

    /**
     * 访客预约记录（映射真实外部接口数据）
     * 对应接口：/api-applypass/api-applypass/visit-terminal/outsideApplyList
     */
    @Data
    public static class VisitorReservation {
        @JSONField(name = "id")
        private String reservationId;      // 预约记录ID
        
        @JSONField(name = "userId")
        private Long userId;               // 访客档案编号
        
        @JSONField(name = "visitorUserName")
        private String visitorName;        // 访客姓名
        
        @JSONField(name = "visitorPhoneNo")
        private String visitorPhone;       // 访客手机号码
        
        @JSONField(name = "visitorIdCard")
        private String visitorIdCard;      // 访客身份证号码
        
        @JSONField(name = "passDep")
        private String passDep;     // 被访部门门类名称
        
        @JSONField(name = "visitorPlateNumber")
        private String carNumber;          // 随行车辆（车牌号）
        
        @JSONField(name = "customVipName")
        private String vipTypeName;        // 访客VIP类型名称
        
        @JSONField(name = "parkName")
        private String parkName;           // 车场名称
        
        @JSONField(name = "beginTime")
        private String startTime;          // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        
        @JSONField(name = "endTime")
        private String endTime;            // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
        
        @JSONField(name = "gatewayTransitBeginTime")
        private String gatewayTransitBeginTime;  // 网关通行开始时间（格式：yyyy-MM-dd HH:mm:ss）
        
        @JSONField(name = "gatewayTransitEndTime")
        private String gatewayTransitEndTime;    // 网关通行结束时间（格式：yyyy-MM-dd HH:mm:ss）
        
        @JSONField(name = "bz")
        private String remark1;            // 备注信息1
        
        @JSONField(name = "bz2")
        private String remark2;            // 备注信息2
        
        @JSONField(name = "bz3")
        private String remark3;            // 备注信息3
        
        @JSONField(name = "ctDate")
        private String createTime;         // 创建时间（格式：Timestamp）
        
        // 其他额外字段（真实数据中包含的）
        @JSONField(name = "applyFromName")
        private String applyFromName;      // 发起渠道
        
        @JSONField(name = "applyFrom")
        private Integer applyFrom;         // 申请送流ID
        
        @JSONField(name = "formId")
        private Long formId;               // 申请表单id
        
        @JSONField(name = "formName")
        private String formName;           // 表单名称
        
        @JSONField(name = "passName")
        private String passName;           // 被访对象
        
        @JSONField(name = "applyState")
        private Integer applyState;        // 申请状态
        
        @JSONField(name = "applyStateName")
        private String applyStateName;     // 申请状态名称
        
        @JSONField(name = "useStatusId")
        private Integer useStatusId;       // 使用状态ID
        
        @JSONField(name = "phoneNo")
        private String phoneNo;            // 被访人工号
        
        @JSONField(name = "passNo")
        private String passNo;             // 通行证号码
        
        @JSONField(name = "companionsNum")
        private Integer companionsNum;     // 同行人数
        
        @JSONField(name = "codeStr")
        private String codeStr;            // 编码字符串
        
        @JSONField(name = "foreignUserNo")
        private String foreignUserNo;      // 外部用户编号
        
        @JSONField(name = "authState")
        private Integer authState;         // 认证状态
        
        @JSONField(name = "authStateStr")
        private String authStateStr;       // 认证状态字符串
        
        @JSONField(name = "approvalFlowId")
        private String approvalFlowId;     // 申请送流ID
        
        @JSONField(name = "submitId")
        private Long submitId;             // 申请送流ID
        
        @JSONField(name = "taskId")
        private String taskId;             // 任务ID
        
        // 注：已去除 visitorFacePhoto (人脸照片) 字段，占用空间大且不参与变更检测
    }
}

