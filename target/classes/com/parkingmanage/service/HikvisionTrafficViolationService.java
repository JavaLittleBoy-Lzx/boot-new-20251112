package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 海康威视园区卡口违章事件查询服务
 * 对接海康威视开放平台的违章事件查询接口
 * 
 * @author System
 */
@Slf4j
@Service
public class HikvisionTrafficViolationService {

    @Resource
    private HikvisionImageProxyService hikvisionImageProxyService;

    @Value("${hikvision.traffic.base-url}")
    private String baseUrl;

    @Value("${hikvision.traffic.app-key}")
    private String appKey;

    @Value("${hikvision.traffic.app-secret}")
    private String appSecret;

    @Value("${hikvision.traffic.timeout}")
    private int timeout;

    @Value("${hikvision.traffic.local-base-url:http://localhost:8675}")
    private String localBaseUrl;
    
    /**
     * 创建并配置ArtemisConfig对象
     * 
     * @return 配置好的ArtemisConfig对象
     */
    private ArtemisConfig createArtemisConfig() {
        // 从baseUrl中提取host（格式：https://10.100.110.82:443）
        String host = baseUrl;
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }
        // 去掉路径部分，只保留host:port
        int pathIndex = host.indexOf("/");
        if (pathIndex > 0) {
            host = host.substring(0, pathIndex);
        }
        
        ArtemisConfig artemisConfig = new ArtemisConfig();
        artemisConfig.setHost(host);
        artemisConfig.setAppKey(appKey);
        artemisConfig.setAppSecret(appSecret);
        
        return artemisConfig;
    }

    /**
     * 查询违章事件
     * 对应海康接口：/api/mpc/v2/illegal/events/search
     * 
     * @param request 查询请求参数
     * @return 违章事件列表
     */
    public ViolationEventResponse queryViolationEvents(ViolationEventRequest request) {
        log.info("🚗 [海康-违章查询] 开始查询违章事件 - 车牌号: {}, 违章类型: {}, 时间范围: {} ~ {}", 
            request.getPlateNo(), 
            request.getIllegalType(),
            request.getBeginTime(),
            request.getEndTime());
        
        try {
            // 创建并配置ArtemisConfig对象
            ArtemisConfig artemisConfig = createArtemisConfig();
            
            // 设置OpenAPI接口的上下文
            final String ARTEMIS_PATH = "/artemis";
            
            // 设置接口的URI地址
            final String violationApi = ARTEMIS_PATH + "/api/mpc/v2/illegal/events/search";
            
            // 构建请求路径path参数
            Map<String, String> path = new HashMap<String, String>(2) {
                {
                    if (baseUrl.startsWith("https://")) {
                        put("https://", violationApi);
                    } else {
                        put("http://", violationApi);
                    }
                }
            };
            
            // 构建请求体（JSON格式的字符串）
            JSONObject requestBody = new JSONObject();
            requestBody.put("pageSize", request.getPageSize());
            requestBody.put("pageNo", request.getPageNo());
            
            // 可选参数
            if (request.getPlateNo() != null && !request.getPlateNo().trim().isEmpty()) {
                requestBody.put("plateNo", request.getPlateNo());
            }
            if (request.getSpeedType() != null && !request.getSpeedType().trim().isEmpty()) {
                requestBody.put("speedType", request.getSpeedType());
            }
            if (request.getIllegalType() != null && !request.getIllegalType().trim().isEmpty()) {
                requestBody.put("illegalType", request.getIllegalType());
            }
            if (request.getMonitoringId() != null && !request.getMonitoringId().trim().isEmpty()) {
                requestBody.put("monitoringId", request.getMonitoringId());
            }
            if (request.getBeginTime() != null && !request.getBeginTime().trim().isEmpty()) {
                requestBody.put("beginTime", request.getBeginTime());
            }
            if (request.getEndTime() != null && !request.getEndTime().trim().isEmpty()) {
                requestBody.put("endTime", request.getEndTime());
            }
            if (request.getCreateBeginTime() != null && !request.getCreateBeginTime().trim().isEmpty()) {
                requestBody.put("createBeginTime", request.getCreateBeginTime());
            }
            if (request.getCreateEndTime() != null && !request.getCreateEndTime().trim().isEmpty()) {
                requestBody.put("createEndTime", request.getCreateEndTime());
            }
            if (request.getAlarmReason() != null && !request.getAlarmReason().trim().isEmpty()) {
                requestBody.put("alarmReason", request.getAlarmReason());
            }
            if (request.getEventId() != null && !request.getEventId().trim().isEmpty()) {
                requestBody.put("eventId", request.getEventId());
            }
            
            String body = requestBody.toJSONString();
            
            log.info("📤 [海康-违章查询] 请求参数: {}", body);
            
            // 设置contentType
            String contentType = "application/json";
            
            // 使用Artemis SDK发送POST请求
            String responseStr = ArtemisHttpUtil.doPostStringArtemis(
                artemisConfig,
                path,
                body,
                null,
                null,
                contentType
            );
            
            log.info("📨 [海康-违章查询] 响应内容: {}", responseStr);
            
            // 解析响应
            ViolationEventResponse response = JSON.parseObject(responseStr, ViolationEventResponse.class);

            if (response != null && "0".equals(response.getCode())) {
                // 转换图片URL为代理URL
                convertImageUrlsToProxy(response);
                int count = 0;
                if (response.getData() != null && response.getData().getList() != null) {
                    count = response.getData().getList().size();
                }
                log.info("✅ [海康-违章查询] 查询成功 - 获取 {} 条违章记录，总数: {}", 
                    count, 
                    response.getData() != null ? response.getData().getTotal() : 0);
                return response;
            } else {
                log.warn("⚠️ [海康-违章查询] 查询失败 - code: {}, msg: {}", 
                    response != null ? response.getCode() : "null", 
                    response != null ? response.getMsg() : "unknown error");
                return response;
            }
            
        } catch (Exception e) {
            log.error("❌ [海康-违章查询] 查询异常", e);
            ViolationEventResponse errorResponse = new ViolationEventResponse();
            errorResponse.setCode("1");
            errorResponse.setMsg("查询失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 违章事件查询请求
     */
    @Data
    public static class ViolationEventRequest {
        private Integer pageSize;           // 每页记录数,范围 ( 0 , 1000 ]，必填
        private Integer pageNo;             // 目标页码,范围 ( 0 , ~ )，必填
        private String plateNo;             // 车牌号，支持模糊查询
        private String speedType;           // 测速类型，默认-1（全部类型），1（点位测速），2（区间测速）
        private String illegalType;         // 违章类型，默认-1（全部类型），1（超速），2（逆行），3（黑名单），5（违停）
        private String monitoringId;        // 卡口点id，不传代表查询全部
        private String beginTime;           // 过车开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+当前时区）
        private String endTime;             // 过车结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+当前时区）
        private String createBeginTime;     // 入库开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+当前时区）
        private String createEndTime;       // 入库结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+当前时区）
        private String alarmReason;         // 布防原因，默认-1（全部原因），1（被盗车），2（被抢车），3（嫌疑车），4（交通违法车），5（紧急查控车）
        private String eventId;             // 事件的唯一编号
    }

    /**
     * 违章事件响应
     */
    @Data
    public static class ViolationEventResponse {
        private String code;                // 返回码，0 – 成功，其他- 失败
        private String msg;                 // 返回提示信息
        private ViolationEventData data;    // 返回数据
    }

    /**
     * 违章事件数据
     */
    @Data
    public static class ViolationEventData {
        private Integer total;              // 总记录数
        private Integer pageSize;           // 分页大小
        private Integer pageNo;             // 页码
        private List<ViolationEvent> list;  // 记录数据集合
    }

    /**
     * 违章事件详情
     */
    @Data
    public static class ViolationEvent {
        private String crossTime;           // 过车时间（格式：yyyy-MM-ddTHH:mm:ss.sss+当前时区）
        private String eventId;             // 事件的唯一标识
        private String plateNo;             // 车牌号
        private Integer illegalType;        // 违章类型，1（超速），2（逆行），3（黑名单），5（违停）
        private Integer speedType;          // 测速类型，1（点位测速），2（区间测速）
        private String monitoringId;        // 事件源(卡口点的编号)
        private String monitoringName;      // 事件源(卡口点的名称)
        private String platePicUri;         // 车牌图片uri
        private String carPicUri;           // 车辆图片uri
        private String aswSyscode;          // 图片服务的唯一标识
        private Integer reason;             // 布防原因(只有黑名单事件才会有值返回)，1（被盗车），2（被抢车），3（嫌疑车），4（交通违法车），5（紧急查控车）
        private String uuid;                // 事件唯一标识
        private String pointIntervalName;   // 区间/点位测速名称
        private Integer speed;              // 车速(单位km/h)，大于0
    }

    /**
     * 将违章事件中的图片URL转换为代理URL
     * 这样线上服务器就可以通过本地服务器访问海康图片
     *
     * @param response 违章事件响应
     */
    private void convertImageUrlsToProxy(ViolationEventResponse response) {
        if (response == null || response.getData() == null || response.getData().getList() == null) {
            return;
        }

        try {
            // 获取本地服务器的基础URL（从配置中获取或使用默认值）
            String localBaseUrl = getLocalServerBaseUrl();

            for (ViolationEvent event : response.getData().getList()) {
                // 只转换车辆图片URL（carPicUri），车牌图片（platePicUri）保持不变
                if (event.getCarPicUri() != null && isHikvisionImageUrl(event.getCarPicUri())) {
                    String originalUrl = event.getCarPicUri();

                    // 如果是相对路径，补全成完整URL
                    String fullUrl = originalUrl;
                    if (originalUrl.startsWith("/pic?=")) {
                        fullUrl = "http://10.100.110.82" + originalUrl;
                    }

                    String proxyUrl = hikvisionImageProxyService.convertToProxyUrl(fullUrl, localBaseUrl);
                    event.setCarPicUri(proxyUrl);
                    log.debug("🔄 [图片URL转换] carPicUri: {} -> {}", originalUrl, proxyUrl);
                }
            }

            log.info("✅ [图片URL转换] 已转换 {} 条违章记录的图片URL", response.getData().getList().size());

        } catch (Exception e) {
            log.error("❌ [图片URL转换] 转换失败", e);
        }
    }

    /**
     * 获取本地服务器的基础URL
     * 从配置文件读取
     *
     * @return 本地服务器基础URL
     */
    private String getLocalServerBaseUrl() {
        return localBaseUrl;
    }

    /**
     * 判断URL是否为海康图片地址
     *
     * @param url 图片URL
     * @return 是否为海康图片地址
     */
    private boolean isHikvisionImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        // 海康图片URL特征：
        // 1. 以 /pic?= 开头
        // 2. 以 /pic? 开头（没有等号的情况）
        // 3. 包含 10.100.110.82
        // 4. 已经是代理URL（说明需要继续代理）
        return url.startsWith("/pic?=") || url.startsWith("/pic?") || url.contains("10.100.110.82");
    }
}
