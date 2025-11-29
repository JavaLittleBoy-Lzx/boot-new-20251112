package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 海康威视人员信息查询服务
 * 对接海康威视开放平台的人员查询接口
 * 
 * @author System
 */
@Slf4j
@Service
public class HikvisionPersonService {

    @Value("${hikvision.api.base-url}")
    private String baseUrl;

    @Value("${hikvision.api.app-key}")
    private String appKey;

    @Value("${hikvision.api.app-secret}")
    private String appSecret;

    @Value("${hikvision.api.timeout}")
    private int timeout;
    
    /**
     * 创建并配置ArtemisConfig对象
     * 根据文档2.1.2，使用实例方法设置host、appKey、appSecret
     * 
     * @return 配置好的ArtemisConfig对象
     */
    private ArtemisConfig createArtemisConfig() {
        // 从baseUrl中提取host（格式：https://10.100.111.5:443/artemis）
        // 根据文档，host格式为 IP:Port，例如：10.0.0.1:443
        String host = baseUrl;
        if (host.startsWith("https://")) {
            host = host.substring(8); // 去掉 "https://"
        } else if (host.startsWith("http://")) {
            host = host.substring(7); // 去掉 "http://"
        }
        // 去掉路径部分，只保留host:port
        int pathIndex = host.indexOf("/");
        if (pathIndex > 0) {
            host = host.substring(0, pathIndex);
        }
        
        // 创建ArtemisConfig对象并使用实例方法设置参数
        // 根据文档2.1.2示例代码：
        // ArtemisConfig artemisConfig = new ArtemisConfig();
        // artemisConfig.setHost("10.0.0.1:443"); // 平台(nginx) IP和端口
        // artemisConfig.setAppKey("11111111"); // 合作方 key
        // artemisConfig.setAppSecret("AAAAAAAAAAAAA"); // 合作方 Secret
        ArtemisConfig artemisConfig = new ArtemisConfig();
        artemisConfig.setHost(host); // 平台(nginx) IP和端口
        artemisConfig.setAppKey(appKey); // 合作方 key
        artemisConfig.setAppSecret(appSecret); // 合作方 Secret
        
        return artemisConfig;
    }

    /**
     * 查询人员信息
     * 对应海康接口：/api/resource/v2/person/advance/personList
     * 根据文档2.2.6 doPostStringArtemis(无 header 参数)使用POST请求
     * 
     * @param personIds 人员ID列表（多个ID用逗号分隔）
     * @return 人员信息列表
     */
    public PersonListResponse queryPersonList(String personIds) {
        log.info("🔍 [海康-人员查询] 开始查询人员信息 - personIds: {}", personIds);
        
        try {
            // 创建并配置ArtemisConfig对象
            ArtemisConfig artemisConfig = createArtemisConfig();
            
            // 设置OpenAPI接口的上下文
            final String ARTEMIS_PATH = "/artemis";
            
            // 设置接口的URI地址
            // 根据文档2.1.3.1，path参数格式：API gateway backend service context + OpenAPI interface request path
            // context为"/artemis"，接口路径为"/api/resource/v2/person/advance/personList"
            final String personListApi = ARTEMIS_PATH + "/api/resource/v2/person/advance/personList";
            
            // 构建请求路径path参数
            // 根据文档2.1.3.1，path是HashMap，Key为协议（http://或https://），Value为接口地址
            Map<String, String> path = new HashMap<String, String>(2) {
                {
                    if (baseUrl.startsWith("https://")) {
                        put("https://", personListApi);
                    } else {
                        put("http://", personListApi);
                    }
                }
            };
            
            // 构建请求体（JSON格式的字符串）
            // 根据文档2.2.6.2，body参数为JSON格式的请求参数，需转化为字符串
            JSONObject requestBody = new JSONObject();
            requestBody.put("personIds", personIds);
            requestBody.put("pageNo", 1);
            requestBody.put("pageSize", 1000);
            String body = requestBody.toJSONString();
            
            // 设置contentType
            // 根据文档2.2.6.2，该方法调用时传application/json
            String contentType = "application/json";
            
            // 使用Artemis SDK发送POST请求
            // 根据文档2.2.6.1方法签名：
            // public static String doPostStringArtemis(ArtemisConfig artemisConfig, Map<String, String> path, 
            //     String body, Map<String, String> querys, String accept, String contentType);
            // 参数说明：
            // - artemisConfig: 请求host、合作方ak/sk封装类
            // - path: 请求的地址
            // - body: JSON格式的请求参数，需转化为字符串
            // - querys: 请求url的查询参数（可选，传null）
            // - accept: 指定客户端能够接收的数据类型（可选，传null表示接收全部类型）
            // - contentType: 请求实体正文的媒体类型，该方法调用时传application/json
            String responseStr = ArtemisHttpUtil.doPostStringArtemis(
                artemisConfig,  // ArtemisConfig对象
                path,           // 请求地址
                body,           // JSON格式的请求体
                null,           // querys查询参数（无）
                null,           // accept（传null表示接收全部类型）
                contentType     // application/json
            );
            
            log.info("📨 [海康-人员查询] 响应内容: {}", responseStr);
            
            // 解析响应
            PersonListResponse response = JSON.parseObject(responseStr, PersonListResponse.class);
            
            if (response != null && "0".equals(response.getCode())) {
                int count = 0;
                if (response.getData() != null && response.getData().getList() != null) {
                    count = response.getData().getList().size();
                }
                log.info("✅ [海康-人员查询] 查询成功 - 获取 {} 条人员信息", count);
                return response;
            } else {
                log.warn("⚠️ [海康-人员查询] 查询失败 - code: {}, msg: {}", 
                    response != null ? response.getCode() : "null", 
                    response != null ? response.getMsg() : "unknown error");
                return response;
            }
            
        } catch (Exception e) {
            log.error("❌ [海康-人员查询] 查询异常", e);
            PersonListResponse errorResponse = new PersonListResponse();
            errorResponse.setCode("1");
            errorResponse.setMsg("查询失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 从人员列表中选取信息最全的一条
     * 通过计算非空字段数量来判断信息的完整度
     * 
     * @param personList 人员信息列表
     * @return 信息最全的人员信息
     */
    private PersonInfo selectMostCompletePerson(List<PersonInfo> personList) {
        if (personList == null || personList.isEmpty()) {
            return null;
        }
        
        if (personList.size() == 1) {
            return personList.get(0);
        }
        
        PersonInfo mostComplete = null;
        int maxScore = -1;
        
        for (PersonInfo person : personList) {
            int score = calculateCompletenessScore(person);
            if (score > maxScore) {
                maxScore = score;
                mostComplete = person;
            }
        }
        
        log.info("📊 [海康-人员查询] 从 {} 条记录中选择信息最全的一条，完整度得分: {}", 
            personList.size(), maxScore);
        
        return mostComplete;
    }
    
    /**
     * 计算人员信息的完整度得分
     * 重要字段（如姓名、证件号、工号等）给予更高权重
     * 
     * @param person 人员信息
     * @return 完整度得分
     */
    private int calculateCompletenessScore(PersonInfo person) {
        if (person == null) {
            return 0;
        }
        
        int score = 0;
        
        // 重要字段（权重 3）
        if (person.getPersonName() != null && !person.getPersonName().trim().isEmpty()) {
            score += 3;
        }
        if (person.getCertificateNo() != null && !person.getCertificateNo().trim().isEmpty()) {
            score += 3;
        }
        if (person.getJobNo() != null && !person.getJobNo().trim().isEmpty()) {
            score += 3;
        }
        if (person.getPersonId() != null && !person.getPersonId().trim().isEmpty()) {
            score += 3;
        }
        
        // 次重要字段（权重 2）
        if (person.getPhoneNo() != null && !person.getPhoneNo().trim().isEmpty()) {
            score += 2;
        }
        if (person.getOrgPathName() != null && !person.getOrgPathName().trim().isEmpty()) {
            score += 2;
        }
        if (person.getOrgName() != null && !person.getOrgName().trim().isEmpty()) {
            score += 2;
        }
        if (person.getGender() != null) {
            score += 2;
        }
        if (person.getPersonPhoto() != null && !person.getPersonPhoto().isEmpty()) {
            score += 2;
        }
        
        // 一般字段（权重 1）
        if (person.getOrgPath() != null && !person.getOrgPath().trim().isEmpty()) {
            score += 1;
        }
        if (person.getOrgIndexCode() != null && !person.getOrgIndexCode().trim().isEmpty()) {
            score += 1;
        }
        if (person.getCertificateType() != null) {
            score += 1;
        }
        if (person.getCardNo() != null && !person.getCardNo().trim().isEmpty()) {
            score += 1;
        }
        if (person.getPlateNo() != null && !person.getPlateNo().trim().isEmpty()) {
            score += 1;
        }
        if (person.getPinyin() != null && !person.getPinyin().trim().isEmpty()) {
            score += 1;
        }
        if (person.getCreateTime() != null && !person.getCreateTime().trim().isEmpty()) {
            score += 1;
        }
        if (person.getUpdateTime() != null && !person.getUpdateTime().trim().isEmpty()) {
            score += 1;
        }
        if (person.getFaceNum() != null && person.getFaceNum() > 0) {
            score += 1;
        }
        if (person.getFingerprintNum() != null && person.getFingerprintNum() > 0) {
            score += 1;
        }
        if (person.getPersonKey() != null && !person.getPersonKey().trim().isEmpty()) {
            score += 1;
        }
        
        return score;
    }

    /**
     * 查询单个人员信息
     * 如果返回多条记录，自动选择信息最全的一条
     * 
     * @param personId 人员ID
     * @return 人员信息
     */
    public PersonInfo queryPersonInfo(String personId) {
        log.info("🔍 [海康-人员查询] 开始查询单个人员 - personId: {}", personId);
        
        PersonListResponse response = queryPersonList(personId);
        
        if (response != null && response.getData() != null 
                && response.getData().getList() != null 
                && !response.getData().getList().isEmpty()) {
            PersonInfo personInfo = selectMostCompletePerson(response.getData().getList());
            if (personInfo != null) {
                log.info("✅ [海康-人员查询] 查询成功 - 人员名称: {}", personInfo.getPersonName());
            }
            return personInfo;
        }
        
        log.warn("⚠️ [海康-人员查询] 未找到人员信息 - personId: {}", personId);
        return null;
    }
    
    /**
     * 从人员列表响应中获取信息最全的一条人员信息
     * 
     * @param response 人员列表响应
     * @return 信息最全的人员信息，如果没有则返回null
     */
    public PersonInfo getMostCompletePerson(PersonListResponse response) {
        if (response == null || response.getData() == null 
                || response.getData().getList() == null 
                || response.getData().getList().isEmpty()) {
            return null;
        }
        
        return selectMostCompletePerson(response.getData().getList());
    }


    /**
     * 人员列表响应
     */
    @Data
    public static class PersonListResponse {
        private String code;
        private String msg;
        private PersonListData data;
    }

    /**
     * 人员列表数据
     */
    @Data
    public static class PersonListData {
        private Integer total;
        private Integer pageNo;
        private Integer pageSize;
        private List<PersonInfo> list;
    }

    /**
     * 人员信息
     */
    @Data
    public static class PersonInfo {
        private String personId;           // 人员ID
        private String personName;         // 人员名称
        private Integer gender;            // 性别：1-男，2-女，0-未知
        private String orgPath;            // 所属组织目录
        private String orgIndexCode;       // 所属组织唯一标识
        private String orgPathName;        // 所属组织名称
        private String orgName;            // 组织名称
        private Integer certificateType;   // 证件类型
        private String certificateNo;      // 证件号码
        private String cardNo;             // 卡号
        private String plateNo;            // 车牌号
        private String orderBy;            // 排序字段
        private String orderType;          // 排序类型
        private String key;                // 资源属性名
        private String operator;           // 操作运算符
        private List<String> values;       // 操作值
        private String createTime;         // 创建时间
        private String updateTime;         // 更新时间
        private String phoneNo;            // 联系电话
        private String jobNo;              // 工号
        private String pinyin;             // 拼音
        private Integer faceNum;           // 人脸数量
        private Integer fingerprintNum;    // 指纹数量
        private String personKey;          // 人员关键字
        private List<PersonPhoto> personPhoto;   // 人员照片列表
        private String personPhotoIndexCode;  // 人员照片ID
        private String picUri;             // 图片URI
        private String serverIndexCode;    // 服务器索引代码
    }

    /**
     * 人员照片信息
     */
    @Data
    public static class PersonPhoto {
        private String serverIndexCode;    // 服务器索引代码
        private String personPhotoIndexCode;  // 人员照片索引代码
        private String picUri;             // 图片URI
    }
}
