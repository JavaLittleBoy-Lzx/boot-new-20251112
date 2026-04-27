package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 海康威视通道/门口信息查询服务
 * 对接海康威视开放平台的门禁设备查询接口
 *
 * @author System
 */
@Slf4j
@Service
public class HikvisionChannelService {

    @Value("${hikvision.api.base-url}")
    private String baseUrl;

    @Value("${hikversion.api.app-key:}")
    private String appKey;

    @Value("${hikvision.api.app-secret}")
    private String appSecret;

    @Value("${hikvision.api.timeout}")
    private int timeout;

    /**
     * 创建并配置ArtemisConfig对象
     */
    private ArtemisConfig createArtemisConfig() {
        String host = baseUrl;
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }
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
     * 查询门禁设备列表（通道/门口）
     * 对应海康接口：/api/resource/v2/door/list
     *
     * @return 通道名称列表
     */
    public List<String> queryDoorList() {
        log.info("🔍 [海康-通道查询] 开始查询门禁设备列表");

        try {
            ArtemisConfig artemisConfig = createArtemisConfig();
            final String ARTEMIS_PATH = "/artemis";
            final String doorListApi = ARTEMIS_PATH + "/api/resource/v2/door/list";

            Map<String, String> path = new HashMap<String, String>(2) {
                {
                    if (baseUrl.startsWith("https://")) {
                        put("https://", doorListApi);
                    } else {
                        put("http://", doorListApi);
                    }
                }
            };

            // 构建请求体 - 分页查询
            JSONObject requestBody = new JSONObject();
            requestBody.put("pageNo", 1);
            requestBody.put("pageSize", 1000);
            String body = requestBody.toJSONString();

            String contentType = "application/json";

            String responseStr = ArtemisHttpUtil.doPostStringArtemis(
                artemisConfig,
                path,
                body,
                null,
                null,
                contentType
            );

            log.info("📨 [海康-通道查询] 响应内容: {}", responseStr);

            return parseDoorListResponse(responseStr);

        } catch (Exception e) {
            log.error("❌ [海康-通道查询] 查询异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析门禁设备列表响应
     *
     * @param responseStr 响应字符串
     * @return 通道名称列表
     */
    private List<String> parseDoorListResponse(String responseStr) {
        List<String> channelList = new ArrayList<>();

        try {
            JSONObject response = JSON.parseObject(responseStr);

            if (response == null) {
                log.warn("⚠️ [海康-通道查询] 响应为空");
                return channelList;
            }

            String code = response.getString("code");
            if (!"0".equals(code)) {
                String msg = response.getString("msg");
                log.warn("⚠️ [海康-通道查询] 查询失败 - code: {}, msg: {}", code, msg);
                return channelList;
            }

            JSONObject data = response.getJSONObject("data");
            if (data == null) {
                log.warn("⚠️ [海康-通道查询] data为空");
                return channelList;
            }

            JSONArray list = data.getJSONArray("list");
            if (list == null || list.isEmpty()) {
                log.info("📋 [海康-通道查询] 未查询到通道数据");
                return channelList;
            }

            for (int i = 0; i < list.size(); i++) {
                JSONObject door = list.getJSONObject(i);
                String doorName = door.getString("doorName");
                if (doorName != null && !doorName.trim().isEmpty()) {
                    channelList.add(doorName.trim());
                }
            }

            log.info("✅ [海康-通道查询] 查询成功 - 获取 {} 个通道", channelList.size());
            return channelList;

        } catch (Exception e) {
            log.error("❌ [海康-通道查询] 解析响应失败", e);
            return channelList;
        }
    }

    /**
     * 获取通道名称列表（带缓存逻辑，后续可优化）
     *
     * @return 通道名称列表
     */
    public List<String> getChannelList() {
        return queryDoorList();
    }

    /**
     * 通道信息响应
     */
    @Data
    public static class DoorListResponse {
        private String code;
        private String msg;
        private DoorListData data;
    }

    /**
     * 通道列表数据
     */
    @Data
    public static class DoorListData {
        private Integer total;
        private Integer pageNo;
        private Integer pageSize;
        private List<DoorInfo> list;
    }

    /**
     * 门禁设备信息
     */
    @Data
    public static class DoorInfo {
        private String doorIndexCode;      // 门禁设备唯一标识
        private String doorName;           // 门禁名称
        private String doorType;           // 门禁类型
        private String regionIndexCode;     // 区域唯一标识
        private String regionName;          // 区域名称
        private String acsIndexCode;        // 门禁设备唯一标识
    }
}