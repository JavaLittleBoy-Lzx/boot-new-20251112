package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ACMS VIP车主信息服务
 * 仅用于东北林业大学车场
 * 
 * @author System
 */
@Slf4j
@Service
public class AcmsVipService {

    @Value("${acms.api.url:}")
    private String acmsApiUrl;

    @Value("${acms.api.device_id:}")
    private String deviceId;

    @Value("${acms.api.sign_type:MD5}")
    private String signType;

    @Value("${acms.api.charset:UTF-8}")
    private String charset;

    private static final String DONGBEI_FORESTRY_UNIVERSITY = "东北林业大学";

    /**
     * 获取车主信息
     * 
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return VIP车主信息
     */
    public VipOwnerInfo getOwnerInfo(String plateNumber, String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }
        try {
            // 构建请求参数
            AcmsRequest request = buildOwnerInfoRequest(plateNumber);
            // 🔧 使用 HttpClientUtil 调用ACMS接口（UTF-8编码已内置处理）
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-车主信息] plateNumber={}, url={}", plateNumber, acmsApiUrl + "/cxfService/external/extReq");
            System.out.println("request = " + requestJson);
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            log.info("📥 [ACMS响应-车主信息] plateNumber={}, response={}", plateNumber, response);
            System.out.println("response = " + response);
            // 解析响应
            return parseOwnerInfoResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS获取车主信息失败，车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 获取车辆 VIP 票信息
     * 
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @return VIP票信息
     */
    public VipTicketInfo getVipTicketInfo(String plateNumber, String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }

        try {
            // 构建请求参数
            AcmsRequest request = buildVipTicketRequest(plateNumber);
            
            // 🔧 使用 HttpClientUtil 调用ACMS接口（UTF-8编码已内置处理）
            String requestJson = JSON.toJSONString(request);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            // 解析响应
            return parseVipTicketResponse(response);
            
        } catch (Exception e) {
//            logger.error
            log.error("调用ACMS获取VIP票信息失败，车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 构建车主信息查询请求
     */
    private AcmsRequest buildOwnerInfoRequest(String plateNumber) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_CUSTOMER");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        OwnerInfoBizContent bizContent = new OwnerInfoBizContent();
        bizContent.setCar_code(plateNumber);
        bizContent.setPage_size(1000);
        bizContent.setPage_num(0);
        
        request.setBiz_content(bizContent);
        
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g"); 
        
        return request;
    }

    /**
     * 构建VIP票查询请求
     */
    private AcmsRequest buildVipTicketRequest(String plateNumber) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_VIP_CAR");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        VipTicketBizContent bizContent = new VipTicketBizContent();
        bizContent.setCar_no(plateNumber);
        bizContent.setValid_type("0"); // 查询所有状态
        
        request.setBiz_content(bizContent);
        
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析车主信息响应
     */
    private VipOwnerInfo parseOwnerInfoResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                return null;
            }

            List<JSONObject> customers = bizContent.getJSONArray("customers").toJavaList(JSONObject.class);
            if (customers == null || customers.isEmpty()) {
                return null;
            }

            JSONObject customer = customers.get(0);
            VipOwnerInfo ownerInfo = new VipOwnerInfo();
            ownerInfo.setOwnerName(customer.getString("customer_name"));
            ownerInfo.setOwnerPhone(customer.getString("customer_telphone"));
            
            // 保存原始字段
            ownerInfo.setCustomerDepartment(customer.getString("customer_department"));
            ownerInfo.setCustomerAddress(customer.getString("customer_address"));
            ownerInfo.setCustomerCompany(customer.getString("customer_company"));
            ownerInfo.setCustomerRoomNumber(customer.getString("customer_room_number"));
            
            // 组合单位地址
            String address = buildOwnerAddress(
                customer.getString("customer_company"),
                customer.getString("customer_department"),
                customer.getString("customer_address"),
                customer.getString("customer_room_number")
            );
            ownerInfo.setOwnerAddress(address);
            
            return ownerInfo;
            
        } catch (Exception e) {
            log.error("解析车主信息响应失败", e);
            return null;
        }
    }

    /**
     * 解析VIP票响应
     */
    private VipTicketInfo parseVipTicketResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                return null;
            }

            List<JSONObject> carList = bizContent.getJSONArray("car_list").toJavaList(JSONObject.class);
            if (carList == null || carList.isEmpty()) {
                return null;
            }

            JSONObject car = carList.get(0);
            VipTicketInfo ticketInfo = new VipTicketInfo();
            
            // 获取字段值
            String vipTypeName = car.getString("vip_type_name");
            String ownerName = car.getString("car_owner");
            String ownerPhone = car.getString("car_owner_phone");
            
            // 🔧 打印调试信息，检查编码是否正确
            log.info("📝 [编码调试] VIP类型: {}, 车主: {}, 电话: {}", vipTypeName, ownerName, ownerPhone);
            System.out.println("ticketInfo = VipTicketInfo(vipTypeName=" + vipTypeName + 
                             ", ownerName=" + ownerName + 
                             ", ownerPhone=" + ownerPhone + ")");
            
            ticketInfo.setVipTypeName(vipTypeName);
            ticketInfo.setOwnerName(ownerName);
            ticketInfo.setOwnerPhone(ownerPhone);
            
            return ticketInfo;
            
        } catch (Exception e) {
            log.error("解析VIP票响应失败", e);
            return null;
        }
    }

    /**
     * 组合车主单位地址
     */
    private String buildOwnerAddress(String company, String department, String address, String roomNumber) {
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.hasText(company)) {
            sb.append(company);
        }
        if (StringUtils.hasText(department)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(department);
        }
        if (StringUtils.hasText(address)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(address);
        }
        if (StringUtils.hasText(roomNumber)) {
            if (sb.length() > 0) sb.append("-");
            sb.append(roomNumber);
        }
        
        return sb.toString();
    }

    private String generateMessageId() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * ACMS请求对象
     */
    @Data
    public static class AcmsRequest {
        private String command;
        private String message_id;
        private String device_id;
        private String sign_type;
        private String sign;
        private String charset;
        private String timestamp;
        private Object biz_content;
    }

    /**
     * 车主信息查询业务内容
     */
    @Data
    public static class OwnerInfoBizContent {
        private String customer_id;
        private String name;
        private String telphone;
        private String identity_card_number;
        private String car_code;
        private Integer page_size;
        private Integer page_num;
    }

    /**
     * VIP票查询业务内容
     */
    @Data
    public static class VipTicketBizContent {
        private String valid_type;
        private String car_no;
    }

    /**
     * VIP车主信息
     */
    @Data
    public static class VipOwnerInfo {
        private String ownerName;
        private String ownerPhone;
        private String ownerAddress;
        private String customerDepartment;  // 部门（作为地址）
        private String customerAddress;     // 地址（作为车主类别）
        private String customerCompany;     // 单位
        private String customerRoomNumber;  // 房间号
    }

    /**
     * VIP票信息
     */
    @Data
    public static class VipTicketInfo {
        private String vipTypeName;
        private String ownerName;
        private String ownerPhone;
    }

    /**
     * 获取车辆VIP票列表
     * 对应ACMS接口：GET_VIP_TICKET (4.10)
     * 
     * @param plateNumber 车牌号（可选）
     * @param carOwner 车主姓名（可选）
     * @param vipTypeName VIP类型名称（可选）
     * @return VIP票列表
     */
    public List<VipTicketDetailInfo> getVipTicketList(String plateNumber, String carOwner, String vipTypeName) {
        try {
            // 构建请求参数
            AcmsRequest request = buildGetVipTicketRequest(plateNumber, carOwner, vipTypeName);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-获取车辆VIP票] 车牌={}, 车主={}, VIP类型={}, url={}", 
                    plateNumber, carOwner, vipTypeName, acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
//            log.info("📥 [ACMS响应-获取车辆VIP票] response={}", response);
            
            // 解析响应
            return parseGetVipTicketResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS获取车辆VIP票失败，车牌号: {}, 错误: {}", plateNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建获取车辆VIP票请求
     */
    private AcmsRequest buildGetVipTicketRequest(String plateNumber, String carOwner, String vipTypeName) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_VIP_TICKET");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        GetVipTicketBizContent bizContent = new GetVipTicketBizContent();
        bizContent.setVip_type_name(vipTypeName != null ? vipTypeName : "");
        bizContent.setCar_owner(carOwner != null ? carOwner : "");
        bizContent.setCar_no(plateNumber != null ? plateNumber : "");
        bizContent.setPage_num("1");
        bizContent.setPage_size("1000");
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析获取车辆VIP票响应
     */
    private List<VipTicketDetailInfo> parseGetVipTicketResponse(String response) {
        // 将解析的结果筛选一下ticket_status为"生效中"的
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return null;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            if (!"0".equals(code)) {
                log.warn("⚠️ ACMS返回错误: {}", msg);
                return null;
            }

            // 检查 ticket_list 字段是否存在
            com.alibaba.fastjson.JSONArray ticketListArray = bizContent.getJSONArray("ticket_list");
            if (ticketListArray == null) {
                log.info("📭 响应中未包含ticket_list字段，返回空列表");
                return new java.util.ArrayList<>();
            }
            
            List<JSONObject> ticketList = ticketListArray.toJavaList(JSONObject.class);
            if (ticketList == null || ticketList.isEmpty()) {
                log.info("📭 未找到VIP票数据");
                return new java.util.ArrayList<>();
            }

            List<VipTicketDetailInfo> result = new java.util.ArrayList<>();
            for (JSONObject ticket : ticketList) {
                VipTicketDetailInfo ticketInfo = new VipTicketDetailInfo();
                ticketInfo.setTicketNo(ticket.getString("ticket_no"));
                ticketInfo.setVipTicketSeq(ticket.getString("vip_ticket_seq"));
                ticketInfo.setVipTypeName(ticket.getString("vip_type_name"));
                ticketInfo.setCarOwner(ticket.getString("car_owner"));
                ticketInfo.setCarNo(ticket.getString("car_no"));
                ticketInfo.setTicketStatus(ticket.getString("ticket_status"));
                ticketInfo.setStartTime(ticket.getString("start_time"));
                ticketInfo.setEndTime(ticket.getString("end_time"));
                ticketInfo.setOperateTime(ticket.getString("operate_time"));
                
                result.add(ticketInfo);
            }
            
            log.info("✅ 解析VIP票数据成功，共 {} 条记录", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 解析获取车辆VIP票响应失败", e);
            return null;
        }
    }

    /**
     * 获取车辆VIP票查询业务内容
     */
    @Data
    public static class GetVipTicketBizContent {
        private String vip_type_name;  // VIP类型名称
        private String car_owner;      // 车主姓名
        private String car_no;         // 车牌号
        private String page_num;       // 页码
        private String page_size;      // 每页大小
    }

    /**
     * VIP票详细信息
     */
    @Data
    public static class VipTicketDetailInfo {
        private String ticketNo;       // 票号
        private String vipTicketSeq;   // VIP票序列号
        private String vipTypeName;    // VIP类型名称
        private String carOwner;       // 车主姓名
        private String carNo;          // 车牌号
        private String ticketStatus;   // 票状态（1-已生效，0-未生效等）
        private String startTime;      // 开始时间
        private String endTime;        // 结束时间
        private String operateTime;    // 操作时间
    }

    /**
     * 获取黑名单类型列表
     * 对应ACMS接口：GET_CAR_VIP_TYPE (4.25)
     * 
     * @param parkName 停车场名称
     * @return 黑名单类型列表
     */
    public List<BlacklistTypeInfo> getBlacklistTypes(String parkName) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            return null;
        }

        try {
            // 构建请求参数
            AcmsRequest request = buildBlacklistTypesRequest();
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(request);
            log.info("📤 [ACMS请求-黑名单类型] url={}", acmsApiUrl + "/cxfService/external/extReq");
            System.out.println("request = " + requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-黑名单类型] response={}", response);
            System.out.println("response = " + response);
            
            // 解析响应
            return parseBlacklistTypesResponse(response);
            
        } catch (Exception e) {
            log.error("调用ACMS获取黑名单类型失败", e);
            return null;
        }
    }

    /**
     * 构建黑名单类型查询请求
     */
    private AcmsRequest buildBlacklistTypesRequest() {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_CAR_VIP_TYPE");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        BlacklistTypesBizContent bizContent = new BlacklistTypesBizContent();
        bizContent.setVip_group_type("2"); // 根据实际情况调整，可能需要配置化
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析黑名单类型响应
     */
    private List<BlacklistTypeInfo> parseBlacklistTypesResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                log.warn("⚠️ ACMS返回错误: {}", bizContent != null ? bizContent.getString("msg") : "无响应");
                return null;
            }

            List<JSONObject> customVips = bizContent.getJSONArray("custom_vips").toJavaList(JSONObject.class);
            if (customVips == null || customVips.isEmpty()) {
                log.warn("⚠️ 未找到任何VIP类型");
                return null;
            }

            // 筛选黑名单类型（名称中包含"黑名单"的）
            List<BlacklistTypeInfo> blacklistTypes = new java.util.ArrayList<>();
            for (JSONObject vip : customVips) {
                String vipName = vip.getString("custom_vip_name");
                
                // 筛选条件：名称包含"黑名单"
                if (vipName != null) {
                    BlacklistTypeInfo typeInfo = new BlacklistTypeInfo();
                    typeInfo.setCode(vip.getString("custom_vip_seq"));
                    typeInfo.setName(vipName);
                    typeInfo.setVipGroupType(vip.getString("vip_group_type"));
                    typeInfo.setVipType(vip.getString("vip_type"));
                    typeInfo.setDescription(getBlacklistDescription(vipName));
                    
                    blacklistTypes.add(typeInfo);
                    
                    log.info("✅ 找到黑名单类型: code={}, name={}", typeInfo.getCode(), typeInfo.getName());
                }
            }
            
            log.info("📊 共筛选出 {} 种黑名单类型", blacklistTypes.size());
            return blacklistTypes.isEmpty() ? null : blacklistTypes;
            
        } catch (Exception e) {
            log.error("解析黑名单类型响应失败", e);
            return null;
        }
    }

    /**
     * 根据黑名单类型名称生成描述
     */
    private String getBlacklistDescription(String typeName) {
        if (typeName == null) {
            return "其他原因被加入黑名单";
        }
        
        if (typeName.contains("违规")) {
            return "因违规停车被加入黑名单";
        } else if (typeName.contains("安全")) {
            return "因安全原因被加入黑名单";
        } else if (typeName.contains("恶意")) {
            return "因恶意行为被加入黑名单";
        } else {
            return "其他原因被加入黑名单";
        }
    }

    /**
     * 黑名单类型查询业务内容
     */
    @Data
    public static class BlacklistTypesBizContent {
        private String vip_group_type;
        private String custom_vip_name; // 可选，不填则查询所有
    }

    /**
     * 黑名单类型信息
     */
    @Data
    public static class BlacklistTypeInfo {
        private String code;          // VIP类型编码（custom_vip_seq）
        private String name;          // VIP类型名称（custom_vip_name）
        private String vipGroupType;  // VIP分组类型
        private String vipType;       // VIP类型
        private String description;   // 描述
    }

    /**
     * 添加黑名单到ACMS
     * 对应ACMS接口：ADD_BLACK_LIST_CAR (4.17)
     * 
     * @param request 黑名单添加请求
     * @return 是否添加成功
     */
    public boolean addBlacklistToAcms(AddBlacklistRequest request) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(request.getParkName())) {
            log.info("⏭️ [黑名单同步] 非东北林业大学车场，跳过ACMS同步: {}", request.getParkName());
            return false;
        }

        try {
            // 构建请求参数
            AcmsRequest acmsRequest = buildAddBlacklistRequest(request);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(acmsRequest);
            log.info("📤 [ACMS请求-添加黑名单] carCode={}, url={}", request.getCarCode(), acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-添加黑名单] carCode={}, response={}", request.getCarCode(), response);
            
            // 解析响应
            boolean success = parseAddBlacklistResponse(response);
            
            if (success) {
                log.info("✅ [黑名单同步成功] 车牌: {}, 类型: {}, 原因: {}", 
                        request.getCarCode(), request.getVipTypeName(), request.getReason());
            } else {
                log.warn("⚠️ [黑名单同步失败] 车牌: {}, ACMS返回失败", request.getCarCode());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [黑名单同步异常] 车牌: {}, 错误: {}", request.getCarCode(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建添加黑名单请求
     */
    private AcmsRequest buildAddBlacklistRequest(AddBlacklistRequest request) {
        AcmsRequest acmsRequest = new AcmsRequest();
        acmsRequest.setCommand("ADD_BLACK_LIST_CAR");
        acmsRequest.setMessage_id(generateMessageId());
        acmsRequest.setDevice_id(deviceId);
        acmsRequest.setSign_type(signType);
        acmsRequest.setCharset(charset);
        acmsRequest.setTimestamp(getCurrentTimestamp());
        
        AddBlacklistBizContent bizContent = new AddBlacklistBizContent();
        bizContent.setVip_type_name(request.getVipTypeName());
        bizContent.setCar_code(request.getCarCode());
        bizContent.setCar_owner(request.getCarOwner());
        bizContent.setReason(request.getReason());
        
        // 设置是否永久拉黑
        if ("permanent".equals(request.getDurationType())) {
            bizContent.setIs_permament(1);
            bizContent.setTime_period(null);
        } else if ("temporary".equals(request.getDurationType())) {
            bizContent.setIs_permament(0);
            
            // 设置时间段
            TimePeriod timePeriod = new TimePeriod();
            timePeriod.setStart_time(request.getStartTime());
            timePeriod.setEnd_time(request.getEndTime());
            bizContent.setTime_period(timePeriod);
        }
        
        // 设置备注和操作信息
//        bizContent.setRemark1(request.getRemark1());
        bizContent.setRemark2(request.getRemark2());
        bizContent.setOperator(request.getOperator());
        bizContent.setOperate_time(request.getOperateTime());
        
        acmsRequest.setBiz_content(bizContent);
        acmsRequest.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return acmsRequest;
    }

    /**
     * 解析添加黑名单响应
     */
    private boolean parseAddBlacklistResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return false;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            // code为"0"表示成功
            return "0".equals(code);
            
        } catch (Exception e) {
            log.error("❌ 解析添加黑名单响应失败", e);
            return false;
        }
    }

    /**
     * 添加黑名单请求参数
     */
    @Data
    public static class AddBlacklistRequest {
        private String parkName;        // 停车场名称（用于判断是否同步到ACMS）
        private String vipTypeCode;     // 黑名单类型编码
        private String vipTypeName;     // 黑名单类型名称
        private String carCode;         // 车牌号
        private String carOwner;        // 车主姓名
        private String reason;          // 拉黑原因
        private String durationType;    // 时长类型：permanent/temporary
        private String startTime;       // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String endTime;         // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
        private String remark1;         // 备注1
        private String remark2;         // 备注2
        private String operator;        // 操作人
        private String operateTime;     // 操作时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 添加黑名单业务内容
     */
    @Data
    public static class AddBlacklistBizContent {
        private String vip_type_code;      // 黑名单类型编码
        private String vip_type_name;      // 黑名单类型名称
        private String car_code;           // 车牌号
        private String car_owner;          // 车主
        private String reason;             // 原因
        private Integer is_permament;      // 是否永久：1-永久，0-临时
        private TimePeriod time_period;    // 时间段（临时拉黑时必填）
        private String remark1;            // 备注1
        private String remark2;            // 备注2
        private String operator;           // 操作人
        private String operate_time;       // 操作时间
    }

    /**
     * 时间段
     */
    @Data
    public static class TimePeriod {
        private String start_time;    // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String end_time;      // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 获取在场车辆数据
     * 对应ACMS接口：GET_REALTIME_CAR_IN_LIST (3.5)
     * 
     * @param parkName 停车场名称
     * @param page 页码
     * @param size 每页大小
     * @param carLicenseNumber 车牌号（可选，支持模糊查询）
     * @param carCardNumber 卡号（可选）
     * @param recordType 记录类型（1-在场车辆）
     * @return 在场车辆列表
     */
    public List<RealtimeCarInfo> getRealtimeCarInList(String parkName, String page, String size, 
                                                      String carLicenseNumber, String carCardNumber, String recordType,
                                                      String startTime, String endTime) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            log.info("⏭️ [在场车辆查询] 非东北林业大学车场，跳过ACMS查询: {}", parkName);
            return null;
        }

        try {
            // 固定每页大小为1000，循环调用5次，合并结果
            List<RealtimeCarInfo> allResults = new java.util.ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                AcmsRequest request = buildRealtimeCarInRequest(String.valueOf(i), "1000", carLicenseNumber, carCardNumber, recordType, startTime, endTime);
                String requestJson = JSON.toJSONString(request);
                log.info("📤 [ACMS请求-在场车辆] page={}, size=1000, 时间范围: {} ~ {}, url={}", i, startTime, endTime, acmsApiUrl + "/cxfService/external/extReq");
                log.info("📋 [请求详情] {}", requestJson);
                String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
                log.info("📥 [ACMS响应-在场车辆] page={}, response={}", i, response);
                List<RealtimeCarInfo> pageResult = parseRealtimeCarInResponse(response);
                if (pageResult != null && !pageResult.isEmpty()) {
                    allResults.addAll(pageResult);
                }
            }
            return allResults.isEmpty() ? null : allResults;
            
        } catch (Exception e) {
            log.error("❌ [在场车辆查询异常] 错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取离场车辆数据
     * 对应ACMS接口：GET_REALTIME_CAR_OUT_LIST (3.6)
     * 
     * @param parkName 停车场名称
     * @param page 页码
     * @param size 每页大小
     * @param carLicenseNumber 车牌号（可选，支持模糊查询）
     * @param carCardNumber 卡号（可选）
     * @param recordType 记录类型（2-离场车辆）
     * @return 离场车辆列表
     */
    public List<RealtimeCarInfo> getRealtimeCarOutList(String parkName, String page, String size, 
                                                       String carLicenseNumber, String carCardNumber, String recordType,
                                                       String startTime, String endTime) {
        // 仅处理东北林业大学车场
        if (!DONGBEI_FORESTRY_UNIVERSITY.equals(parkName)) {
            log.info("⏭️ [离场车辆查询] 非东北林业大学车场，跳过ACMS查询: {}", parkName);
            return null;
        }

        try {
            // 固定每页大小为1000，循环调用5次，合并结果
            List<RealtimeCarInfo> allResults = new java.util.ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                AcmsRequest request = buildRealtimeCarOutRequest(String.valueOf(i), "1000", carLicenseNumber, carCardNumber, recordType, startTime, endTime);
                String requestJson = JSON.toJSONString(request);
                log.info("📤 [ACMS请求-离场车辆] page={}, size=1000, 时间范围: {} ~ {}, url={}", i, startTime, endTime, acmsApiUrl + "/cxfService/external/extReq");
                log.info("📋 [请求详情] {}", requestJson);
                String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
                log.info("📥 [ACMS响应-离场车辆] page={}, response={}", i, response);
                List<RealtimeCarInfo> pageResult = parseRealtimeCarOutResponse(response);
                if (pageResult != null && !pageResult.isEmpty()) {
                    allResults.addAll(pageResult);
                }
            }
            return allResults.isEmpty() ? null : allResults;
            
        } catch (Exception e) {
            log.error("❌ [离场车辆查询异常] 错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建在场车辆查询请求
     */
    private AcmsRequest buildRealtimeCarInRequest(String page, String size, String carLicenseNumber, 
                                                  String carCardNumber, String recordType, String startTime, String endTime) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_REALTIME_CAR_IN_LIST");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        RealtimeCarBizContent bizContent = new RealtimeCarBizContent();
        bizContent.setPage(page != null ? page : "1");
        bizContent.setSize(size != null ? size : "1000");
        bizContent.setCar_license_number(carLicenseNumber != null ? carLicenseNumber : "");
        bizContent.setCar_card_number(carCardNumber != null ? carCardNumber : "");
        bizContent.setRecord_type(recordType != null ? recordType : "1");
        bizContent.setStart_time(startTime != null ? startTime : "");
        bizContent.setEnd_time(endTime != null ? endTime : "");
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 构建离场车辆查询请求
     */
    private AcmsRequest buildRealtimeCarOutRequest(String page, String size, String carLicenseNumber, 
                                                   String carCardNumber, String recordType, String startTime, String endTime) {
        AcmsRequest request = new AcmsRequest();
        request.setCommand("GET_CAR_OUT_LIST");
        request.setMessage_id(generateMessageId());
        request.setDevice_id(deviceId);
        request.setSign_type(signType);
        request.setCharset(charset);
        request.setTimestamp(getCurrentTimestamp());
        
        RealtimeCarBizContent bizContent = new RealtimeCarBizContent();
        bizContent.setPage(page != null ? page : "1");
        bizContent.setSize(size != null ? size : "1000");
        bizContent.setCar_license_number(carLicenseNumber != null ? carLicenseNumber : "");
        bizContent.setCar_card_number(carCardNumber != null ? carCardNumber : "");
        bizContent.setRecord_type(recordType != null ? recordType : "2");
        bizContent.setStart_time(startTime != null ? startTime : "");
        bizContent.setEnd_time(endTime != null ? endTime : "");
        
        request.setBiz_content(bizContent);
        request.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return request;
    }

    /**
     * 解析在场车辆响应
     */
    private List<RealtimeCarInfo> parseRealtimeCarInResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return null;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            if (!"0".equals(code)) {
                log.warn("⚠️ ACMS返回错误: {}", msg);
                return null;
            }

            List<JSONObject> carList = bizContent.getJSONArray("car_list").toJavaList(JSONObject.class);
            if (carList == null || carList.isEmpty()) {
                log.info("📭 未找到在场车辆数据");
                return null;
            }

            List<RealtimeCarInfo> result = new java.util.ArrayList<>();
            for (JSONObject car : carList) {
                RealtimeCarInfo carInfo = new RealtimeCarInfo();
                carInfo.setCarLicenseNumber(car.getString("car_license_number"));
                carInfo.setEnterTime(car.getString("enter_time"));
                carInfo.setEnterChannel(car.getString("enter_channel"));
                carInfo.setEnterChannelName(car.getString("enter_channel_name"));
                carInfo.setEnterType(car.getString("enter_type"));
                carInfo.setEnterCarType(car.getString("enter_car_type"));
                carInfo.setEnterCarColor(car.getString("enter_car_color"));
                carInfo.setEnterCarLicenseColor(car.getString("enter_car_license_color"));
                carInfo.setEnterCarLicenseType(car.getString("enter_car_license_type"));
                carInfo.setEnterCarLogo(car.getString("enter_car_logo"));
                carInfo.setEnterCarCardNumber(car.getString("enter_car_card_number"));
                carInfo.setEnterCarLicensePicture(car.getString("enter_car_license_picture"));
                carInfo.setEnterCarFullPicture(car.getString("enter_car_full_picture"));
                carInfo.setEnterRecognitionConfidence(car.getString("enter_recognition_confidence"));
                carInfo.setEnterSpeed(car.getString("enter_speed"));
                carInfo.setParkRecordNumber(car.getString("park_record_number"));
                carInfo.setParkingLotId(car.getString("parking_lot_id"));
                carInfo.setParkingLotSeq(car.getString("parking_lot_seq"));
                carInfo.setRecordType(car.getString("record_type"));
                carInfo.setIsCorrect(car.getString("is_correct"));
                carInfo.setCorrectType(car.getString("correct_type"));
                carInfo.setCorrectConfidence(car.getString("correct_confidence"));
                carInfo.setLastCorrectTime(car.getString("last_correct_time"));
                carInfo.setLastCorrectName(car.getString("last_correct_name"));
                carInfo.setLastCorrectLicenseNumber(car.getString("last_correct_license_number"));
                carInfo.setEnterCustomVipName(car.getString("enter_custom_vip_name"));
                carInfo.setInOperatorTime(car.getString("in_operator_time"));
                carInfo.setInOperatorName(car.getString("in_operator_name"));
                carInfo.setRemark(car.getString("remark"));
                
                result.add(carInfo);
            }
            
            log.info("✅ 解析在场车辆数据成功，共 {} 条记录", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 解析在场车辆响应失败", e);
            return null;
        }
    }

    /**
     * 解析离场车辆响应
     */
    private List<RealtimeCarInfo> parseRealtimeCarOutResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return null;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return null;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            if (!"0".equals(code)) {
                log.warn("⚠️ ACMS返回错误: {}", msg);
                return null;
            }

            List<JSONObject> carList = bizContent.getJSONArray("car_list").toJavaList(JSONObject.class);
            if (carList == null || carList.isEmpty()) {
                log.info("📭 未找到离场车辆数据");
                return null;
            }

            List<RealtimeCarInfo> result = new java.util.ArrayList<>();
            for (JSONObject car : carList) {
                RealtimeCarInfo carInfo = new RealtimeCarInfo();
                carInfo.setCarLicenseNumber(car.getString("car_license_number"));
                carInfo.setEnterTime(car.getString("enter_time"));
                carInfo.setLeaveTime(car.getString("leave_time"));
                carInfo.setEnterChannel(car.getString("enter_channel"));
                carInfo.setEnterChannelName(car.getString("enter_channel_name"));
                carInfo.setLeaveChannel(car.getString("leave_channel"));
                carInfo.setLeaveChannelName(car.getString("leave_channel_name"));
                carInfo.setEnterType(car.getString("enter_type"));
                carInfo.setLeaveType(car.getString("leave_type"));
                carInfo.setEnterCarType(car.getString("enter_car_type"));
                carInfo.setLeaveCarType(car.getString("leave_car_type"));
                carInfo.setEnterCarColor(car.getString("enter_car_color"));
                carInfo.setLeaveCarColor(car.getString("leave_car_color"));
                carInfo.setEnterCarLicenseColor(car.getString("enter_car_license_color"));
                carInfo.setLeaveCarLicenseColor(car.getString("leave_car_license_color"));
                carInfo.setEnterCarLicenseType(car.getString("enter_car_license_type"));
                carInfo.setLeaveCarLicenseType(car.getString("leave_car_license_type"));
                carInfo.setEnterCarLogo(car.getString("enter_car_logo"));
                carInfo.setLeaveCarLogo(car.getString("leave_car_logo"));
                carInfo.setEnterCarCardNumber(car.getString("enter_car_card_number"));
                carInfo.setLeaveCarCardNumber(car.getString("leave_car_card_number"));
                carInfo.setEnterCarLicensePicture(car.getString("enter_car_license_picture"));
                carInfo.setLeaveCarLicensePicture(car.getString("leave_car_license_picture"));
                carInfo.setEnterCarFullPicture(car.getString("enter_car_full_picture"));
                carInfo.setLeaveCarFullPicture(car.getString("leave_car_full_picture"));
                carInfo.setEnterRecognitionConfidence(car.getString("enter_recognition_confidence"));
                carInfo.setLeaveRecognitionConfidence(car.getString("leave_recognition_confidence"));
                carInfo.setEnterSpeed(car.getString("enter_speed"));
                carInfo.setLeaveSpeed(car.getString("leave_speed"));
                carInfo.setParkRecordNumber(car.getString("park_record_number"));
                carInfo.setParkingLotId(car.getString("parking_lot_id"));
                carInfo.setParkingLotSeq(car.getString("parking_lot_seq"));
                carInfo.setRecordType(car.getString("record_type"));
                carInfo.setIsCorrect(car.getString("is_correct"));
                carInfo.setCorrectType(car.getString("correct_type"));
                carInfo.setCorrectConfidence(car.getString("correct_confidence"));
                carInfo.setLastCorrectTime(car.getString("last_correct_time"));
                carInfo.setLastCorrectName(car.getString("last_correct_name"));
                carInfo.setLastCorrectLicenseNumber(car.getString("last_correct_license_number"));
                carInfo.setEnterCustomVipName(car.getString("enter_custom_vip_name"));
                carInfo.setLeaveCustomVipName(car.getString("leave_custom_vip_name"));
                carInfo.setInOperatorTime(car.getString("in_operator_time"));
                carInfo.setInOperatorName(car.getString("in_operator_name"));
                carInfo.setOutOperatorTime(car.getString("out_operator_time"));
                carInfo.setOutOperatorName(car.getString("out_operator_name"));
                carInfo.setRemark(car.getString("remark"));
                
                result.add(carInfo);
            }
            
            log.info("✅ 解析离场车辆数据成功，共 {} 条记录", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 解析离场车辆响应失败", e);
            return null;
        }
    }

    /**
     * 实时车辆查询业务内容
     */
    @Data
    public static class RealtimeCarBizContent {
        private String page;                    // 页码
        private String size;                    // 每页大小
        private String car_license_number;     // 车牌号（支持模糊查询）
        private String car_card_number;         // 卡号
        private String record_type;            // 记录类型（1-在场，2-离场）
        private String start_time;             // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String end_time;                // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 开通VIP票到VEMS系统
     * 对应VEMS接口：OPEN_VIP_TICKET (4.2)
     * 
     * @param request VIP票开通请求
     * @return 是否开通成功
     */
    public boolean openVipTicketToVems(OpenVipTicketRequest request) {
        try {
            // 构建请求参数
            AcmsRequest vemsRequest = buildOpenVipTicketRequest(request);
            
            // 调用VEMS接口
            String requestJson = JSON.toJSONString(vemsRequest);
            log.info("📤 [VEMS请求-开通VIP票] carList={}, url={}", 
                    request.getCarList() != null ? request.getCarList().size() : 0, 
                    acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [VEMS响应-开通VIP票] response={}", response);
            
            // 解析响应
            boolean success = parseOpenVipTicketResponse(response);
            
            if (success) {
                log.info("✅ [VIP票开通成功] 车主: {}, VIP类型: {}, 车辆数: {}", 
                        request.getCarOwner(), request.getVipTypeName(), 
                        request.getCarList() != null ? request.getCarList().size() : 0);
            } else {
                log.warn("⚠️ [VIP票开通失败] 车主: {}, VEMS返回失败", request.getCarOwner());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [VIP票开通异常] 车主: {}, 错误: {}", request.getCarOwner(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建开通VIP票请求
     */
    private AcmsRequest buildOpenVipTicketRequest(OpenVipTicketRequest request) {
        AcmsRequest vemsRequest = new AcmsRequest();
        vemsRequest.setCommand("OPEN_VIP_TICKET");
        vemsRequest.setMessage_id(generateMessageId());
        vemsRequest.setDevice_id(deviceId);
        vemsRequest.setSign_type(signType);
        vemsRequest.setCharset(charset);
        vemsRequest.setTimestamp(getCurrentTimestamp());
        
        OpenVipTicketBizContent bizContent = new OpenVipTicketBizContent();
        bizContent.setVip_type_name(request.getVipTypeName());
//        bizContent.setTicket_no(request.getTicketNo());
        bizContent.setCar_owner(request.getCarOwner());
        bizContent.setTelphone(request.getTelphone());
        bizContent.setCompany(request.getCompany());
        bizContent.setDepartment(request.getDepartment());
        bizContent.setSex(request.getSex());
        bizContent.setOperator("FKJK");
        bizContent.setOperate_time(request.getOperateTime());
        bizContent.setOriginal_price("0.00");
        bizContent.setDiscount_price("0.00");
        bizContent.setOpen_value(request.getOpenValue());
        bizContent.setOpen_car_count(request.getOpenCarCount());
        
        // 设置车辆列表
        if (request.getCarList() != null && !request.getCarList().isEmpty()) {
            List<CarInfo> carList = new java.util.ArrayList<>();
            for (String carNo : request.getCarList()) {
                CarInfo carInfo = new CarInfo();
                carInfo.setCar_no(carNo);
                carList.add(carInfo);
            }
            bizContent.setCar_list(carList);
        }
        
        // 设置时间段列表
        if (request.getTimePeriodList() != null && !request.getTimePeriodList().isEmpty()) {
            bizContent.setTime_period_list(request.getTimePeriodList());
        }
        
        vemsRequest.setBiz_content(bizContent);
        vemsRequest.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return vemsRequest;
    }

    /**
     * 解析开通VIP票响应
     */
    private boolean parseOpenVipTicketResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ VEMS响应为空");
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ VEMS响应缺少biz_content");
                return false;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [VEMS响应解析] code={}, msg={}", code, msg);
            
            // code为"0"表示成功
            return "0".equals(code);
            
        } catch (Exception e) {
            log.error("❌ 解析开通VIP票响应失败", e);
            return false;
        }
    }

    /**
     * 开通VIP票请求参数
     */
    @Data
    public static class OpenVipTicketRequest {
        private String vipTypeName;             // VIP类型名称
        private String ticketNo;                // 票号
        private String carOwner;                // 车主姓名
        private String telphone;                // 电话
        private String company;                 // 公司
        private String department;              // 部门
        private String sex;                     // 性别：0-男，1-女
        private String operator;                // 操作人
        private String operateTime;             // 操作时间（格式：yyyy-MM-dd HH:mm:ss）
        private String originalPrice;           // 原价
        private String discountPrice;           // 优惠价
        private String openValue;               // 开通值
        private String openCarCount;            // 开通车辆数
        private List<String> carList;           // 车辆列表（车牌号列表）
        private List<TimePeriod> timePeriodList;// 时间段列表
    }

    /**
     * 开通VIP票业务内容
     */
    @Data
    public static class OpenVipTicketBizContent {
        private String vip_type_name;           // VIP类型名称
        private String ticket_no;               // 票号
        private String car_owner;               // 车主
        private String telphone;                // 电话
        private String company;                 // 公司
        private String department;              // 部门
        private String sex;                     // 性别：0-男，1-女
        private String operator;                // 操作人
        private String operate_time;            // 操作时间
        private String original_price;          // 原价
        private String discount_price;          // 优惠价
        private String open_value;              // 开通值
        private String open_car_count;          // 开通车辆数
        private List<CarInfo> car_list;         // 车辆列表
        private List<TimePeriod> time_period_list; // 时间段列表
    }

    /**
     * 车辆信息
     */
    @Data
    public static class CarInfo {
        private String car_no;    // 车牌号
    }

    /**
     * 实时车辆信息
     */
    @Data
    public static class RealtimeCarInfo {
        private String carLicenseNumber;           // 车牌号
        private String enterTime;                  // 进场时间
        private String leaveTime;                  // 离场时间
        private String enterChannel;               // 进场通道
        private String enterChannelName;           // 进场通道名称
        private String leaveChannel;               // 离场通道
        private String leaveChannelName;           // 离场通道名称
        private String enterType;                  // 进场类型
        private String leaveType;                  // 离场类型
        private String enterCarType;               // 进场车辆类型
        private String leaveCarType;               // 离场车辆类型
        private String enterCarColor;              // 进场车辆颜色
        private String leaveCarColor;              // 离场车辆颜色
        private String enterCarLicenseColor;       // 进场车牌颜色
        private String leaveCarLicenseColor;       // 离场车牌颜色
        private String enterCarLicenseType;        // 进场车牌类型
        private String leaveCarLicenseType;        // 离场车牌类型
        private String enterCarLogo;               // 进场车标
        private String leaveCarLogo;               // 离场车标
        private String enterCarCardNumber;         // 进场卡号
        private String leaveCarCardNumber;         // 离场卡号
        private String enterCarLicensePicture;     // 进场车牌图片
        private String leaveCarLicensePicture;     // 离场车牌图片
        private String enterCarFullPicture;        // 进场整车图片
        private String leaveCarFullPicture;        // 离场整车图片
        private String enterRecognitionConfidence;  // 进场识别置信度
        private String leaveRecognitionConfidence;  // 离场识别置信度
        private String enterSpeed;                  // 进场速度
        private String leaveSpeed;                  // 离场速度
        private String parkRecordNumber;           // 停车记录号
        private String parkingLotId;               // 停车场ID
        private String parkingLotSeq;              // 停车场序列号
        private String recordType;                 // 记录类型
        private String isCorrect;                  // 是否校正
        private String correctType;                // 校正类型
        private String correctConfidence;          // 校正置信度
        private String lastCorrectTime;            // 最后校正时间
        private String lastCorrectName;            // 最后校正人
        private String lastCorrectLicenseNumber;   // 最后校正车牌号
        private String enterCustomVipName;          // 进场自定义VIP名称
        private String leaveCustomVipName;         // 离场自定义VIP名称
        private String inOperatorTime;             // 进场操作时间
        private String inOperatorName;             // 进场操作人
        private String outOperatorTime;            // 离场操作时间
        private String outOperatorName;            // 离场操作人
        private String remark;                     // 备注
    }

    /**
     * 添加访客到ACMS系统
     * 对应ACMS接口：ADD_VISITOR_CAR (4.16)
     * 
     * @param request 添加访客请求
     * @return 是否添加成功
     */
    public boolean addVisitorCarToAcms(AddVisitorCarRequest request) {
        try {
            // 构建请求参数
            AcmsRequest acmsRequest = buildAddVisitorCarRequest(request);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(acmsRequest);
            log.info("📤 [ACMS请求-添加访客] 车牌={}, 访客={}, url={}", 
                    request.getCarCode(), request.getOwner(), 
                    acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-添加访客] response={}", response);
            
            // 解析响应
            boolean success = parseAddVisitorCarResponse(response);
            
            if (success) {
                log.info("✅ [访客添加成功] 访客: {}, 车牌: {}, 访客类型: {}", 
                        request.getOwner(), request.getCarCode(), request.getVisitName());
            } else {
                log.warn("⚠️ [访客添加失败] 访客: {}, ACMS返回失败", request.getOwner());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [访客添加异常] 访客: {}, 错误: {}", request.getOwner(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建添加访客请求
     */
    private AcmsRequest buildAddVisitorCarRequest(AddVisitorCarRequest request) {
        AcmsRequest acmsRequest = new AcmsRequest();
        acmsRequest.setCommand("ADD_VISITOR_CAR");
        acmsRequest.setMessage_id(generateMessageId());
        acmsRequest.setDevice_id(deviceId);
        acmsRequest.setSign_type(signType);
        acmsRequest.setCharset(charset);
        acmsRequest.setTimestamp(getCurrentTimestamp());
        
        AddVisitorCarBizContent bizContent = new AddVisitorCarBizContent();
        bizContent.setCar_code(request.getCarCode());
        bizContent.setOwner(request.getOwner());
        bizContent.setVisit_name(request.getVisitName());
        bizContent.setPhonenum(request.getPhonenum());
        bizContent.setReason(request.getReason());
        bizContent.setOperator(request.getOperator());
        bizContent.setOperate_time(request.getOperateTime());
        
        // 设置访客时间
        if (request.getVisitTime() != null) {
            bizContent.setVisit_time(request.getVisitTime());
        }
        
        acmsRequest.setBiz_content(bizContent);
        acmsRequest.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return acmsRequest;
    }

    /**
     * 解析添加访客响应
     */
    private boolean parseAddVisitorCarResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return false;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            // code为"0"表示成功
            return "0".equals(code);
            
        } catch (Exception e) {
            log.error("❌ 解析添加访客响应失败", e);
            return false;
        }
    }

    /**
     * 添加访客请求参数
     */
    @Data
    public static class AddVisitorCarRequest {
        private String carCode;         // 车牌号
        private String owner;           // 车主/访客姓名
        private String visitName;       // 访客VIP类型名称
        private String phonenum;        // 电话号码
        private String reason;          // 来访原因
        private String operator;        // 操作员
        private String operateTime;     // 操作时间（格式：yyyy-MM-dd HH:mm:ss）
        private VisitTime visitTime;    // 预计来访时间
    }

    /**
     * 添加访客业务内容
     */
    @Data
    public static class AddVisitorCarBizContent {
        private String car_code;        // 车牌号
        private String owner;           // 车主/访客姓名
        private String visit_name;      // 访客VIP类型名称
        private String phonenum;        // 电话号码
        private String reason;          // 来访原因
        private String operator;        // 操作员
        private String operate_time;    // 操作时间
        private VisitTime visit_time;   // 预计来访时间
    }

    /**
     * 访客时间
     */
    @Data
    public static class VisitTime {
        private String start_time;      // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
        private String end_time;        // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
    }

    /**
     * 解析车牌号字符串，支持多个车牌号用逗号分隔
     * 
     * @param carLicenseNumber 车牌号字符串，可能包含多个车牌号（用逗号分隔），例如："黑C6195L,黑A8SC60"
     * @return 车牌号列表
     */
    private List<String> parseCarLicenseNumbers(String carLicenseNumber) {
        List<String> carList = new java.util.ArrayList<>();
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
     * 根据车牌号批量退费VIP票1
     * 查询该车牌号下vip_type_name为"二道岗可通行车辆"且ticket_status为"1"(已生效)的VIP票，并进行批量退费
     * 对应ACMS接口：REFUND_VIP_TICKET
     * @param plateNumber 车牌号（可能包含多个车牌号，用逗号分隔）
     * @param operator 操作人
     * @param operateTime 操作时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param reason 退费原因（可选）
     * @return 退费结果，包含成功和失败的票序列号列表
     */
    public RefundVipTicketResult refundVipTicketByPlateNumber(String plateNumber, String operator, 
                                                               String operateTime, String reason) {
        RefundVipTicketResult result = new RefundVipTicketResult();
        result.setPlateNumber(plateNumber);
        
        try {
            // 1. 处理多个车牌号的情况（用逗号分隔）
            List<String> carLicenseList = parseCarLicenseNumbers(plateNumber);
            String queryCarLicense = carLicenseList.get(0); // 取第一个车牌号进行查询
            
            // 2. 查询该车牌号的所有VIP票
            log.info("📋 [退费查询] 开始查询车牌号: {} 的VIP票信息（查询车牌: {}）", plateNumber, queryCarLicense);
            List<VipTicketDetailInfo> ticketList = getVipTicketList(queryCarLicense, null, "二道岗可通行车辆");
            
            if (ticketList == null || ticketList.isEmpty()) {
                log.warn("⚠️ [退费查询] 车牌号: {} 未找到任何VIP票", plateNumber);
                result.setMessage("未找到任何VIP票");
                return result;
            }
            
            log.info("📊 [退费查询] 车牌号: {} 共找到 {} 条VIP票记录", plateNumber, ticketList.size());
            
            // 3. 筛选符合条件的VIP票：vip_type_name="二道岗可通行车辆" 且 ticket_status="生效中"
            // 并且车牌号在待查询的车牌号列表中（处理多个车牌号的情况）
            List<VipTicketDetailInfo> eligibleTickets = new java.util.ArrayList<>();
            for (VipTicketDetailInfo ticket : ticketList) {
                if ("二道岗可通行车辆".equals(ticket.getVipTypeName()) && "生效中".equals(ticket.getTicketStatus())) {
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
                                break;
                            }
                        }
                        if (hasMatch) {
                            eligibleTickets.add(ticket);
                            log.info("✅ [退费筛选] 找到符合条件的VIP票: vip_ticket_seq={}, car_no={}, vip_type_name={}, ticket_status={}", 
                                    ticket.getVipTicketSeq(), ticketCarNo, ticket.getVipTypeName(), ticket.getTicketStatus());
                        }
                    }
                }
            }
            
            if (eligibleTickets.isEmpty()) {
                log.warn("⚠️ [退费筛选] 车牌号: {} 未找到符合条件的VIP票（vip_type_name=二道岗可通行车辆, ticket_status=生效中）", plateNumber);
                result.setMessage("未找到符合条件的VIP票");
                return result;
            }
            
            log.info("📊 [退费筛选] 车牌号: {} 共筛选出 {} 条符合条件的VIP票", plateNumber, eligibleTickets.size());
            
            // 4. 批量退费
            List<String> successSeqs = new java.util.ArrayList<>();
            List<String> failedSeqs = new java.util.ArrayList<>();
            
            for (VipTicketDetailInfo ticket : eligibleTickets) {
                String vipTicketSeq = ticket.getVipTicketSeq();
                if (vipTicketSeq == null || vipTicketSeq.isEmpty()) {
                    log.warn("⚠️ [退费] VIP票序列号为空，跳过: ticket_no={}", ticket.getTicketNo());
                    failedSeqs.add(ticket.getTicketNo() != null ? ticket.getTicketNo() : "未知");
                    continue;
                }
                
                log.info("🔄 [退费处理] 开始退费: vip_ticket_seq={}, ticket_no={}", vipTicketSeq, ticket.getTicketNo());
                
                // 构建退费请求
                RefundVipTicketRequest refundRequest = new RefundVipTicketRequest();
                refundRequest.setVipTicketSeq(vipTicketSeq);
                refundRequest.setOperator(operator);
                refundRequest.setOperateTime(operateTime);
                refundRequest.setReason(reason);
                
                // 调用退费接口
                boolean success = refundVipTicket(refundRequest);
                
                if (success) {
                    successSeqs.add(vipTicketSeq);
                    log.info("✅ [退费成功] vip_ticket_seq={}, ticket_no={}", vipTicketSeq, ticket.getTicketNo());
                } else {
                    failedSeqs.add(vipTicketSeq);
                    log.warn("⚠️ [退费失败] vip_ticket_seq={}, ticket_no={}", vipTicketSeq, ticket.getTicketNo());
                }
            }
            
            result.setSuccessCount(successSeqs.size());
            result.setFailedCount(failedSeqs.size());
            result.setSuccessSeqs(successSeqs);
            result.setFailedSeqs(failedSeqs);
            result.setMessage(String.format("退费完成：成功 %d 条，失败 %d 条", successSeqs.size(), failedSeqs.size()));
            
            log.info("✅ [退费完成] 车牌号: {}, 成功: {} 条, 失败: {} 条", plateNumber, successSeqs.size(), failedSeqs.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ [退费异常] 车牌号: {}, 错误: {}", plateNumber, e.getMessage(), e);
            result.setMessage("退费过程中发生异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 退费VIP票
     * 对应ACMS接口：REFUND_VIP_TICKET
     * 
     * @param request 退费请求
     * @return 是否退费成功
     */
    public boolean refundVipTicket(RefundVipTicketRequest request) {
        try {
            // 构建请求参数
            AcmsRequest acmsRequest = buildRefundVipTicketRequest(request);
            
            // 调用ACMS接口
            String requestJson = JSON.toJSONString(acmsRequest);
            log.info("📤 [ACMS请求-退费VIP票] vip_ticket_seq={}, url={}", 
                    request.getVipTicketSeq(), acmsApiUrl + "/cxfService/external/extReq");
            log.info("📋 [请求详情] {}", requestJson);
            
            String response = HttpClientUtil.doPostJson(acmsApiUrl + "/cxfService/external/extReq", requestJson);
            
            log.info("📥 [ACMS响应-退费VIP票] vip_ticket_seq={}, response={}", request.getVipTicketSeq(), response);
            
            // 解析响应
            boolean success = parseRefundVipTicketResponse(response);
            
            if (success) {
                log.info("✅ [退费成功] vip_ticket_seq: {}", request.getVipTicketSeq());
            } else {
                log.warn("⚠️ [退费失败] vip_ticket_seq: {}, ACMS返回失败", request.getVipTicketSeq());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [退费异常] vip_ticket_seq: {}, 错误: {}", request.getVipTicketSeq(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建退费VIP票请求
     */
    private AcmsRequest buildRefundVipTicketRequest(RefundVipTicketRequest request) {
        AcmsRequest acmsRequest = new AcmsRequest();
        acmsRequest.setCommand("REFUND_VIP_TICKET");
        acmsRequest.setMessage_id(generateMessageId());
        acmsRequest.setDevice_id(deviceId);
        acmsRequest.setSign_type(signType);
        acmsRequest.setCharset(charset);
        acmsRequest.setTimestamp(getCurrentTimestamp());
        
        RefundVipTicketBizContent bizContent = new RefundVipTicketBizContent();
        bizContent.setVip_ticket_seq(request.getVipTicketSeq());
        bizContent.setOperator(request.getOperator());
        bizContent.setOperate_time(request.getOperateTime());
        bizContent.setReason(request.getReason());
        
        acmsRequest.setBiz_content(bizContent);
        acmsRequest.setSign("f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        
        return acmsRequest;
    }

    /**
     * 解析退费VIP票响应
     */
    private boolean parseRefundVipTicketResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("⚠️ ACMS响应为空");
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");
            
            if (bizContent == null) {
                log.warn("⚠️ ACMS响应缺少biz_content");
                return false;
            }
            
            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");
            
            log.info("📊 [ACMS响应解析] code={}, msg={}", code, msg);
            
            // code为"0"表示成功
            return "0".equals(code);
            
        } catch (Exception e) {
            log.error("❌ 解析退费VIP票响应失败", e);
            return false;
        }
    }

    /**
     * 退费VIP票请求参数
     */
    @Data
    public static class RefundVipTicketRequest {
        private String vipTicketSeq;   // VIP票序列号
        private String operator;       // 操作人
        private String operateTime;    // 操作时间（格式：yyyy-MM-dd HH:mm:ss）
        private String reason;         // 退费原因（可选）
    }

    /**
     * 退费VIP票业务内容
     */
    @Data
    public static class RefundVipTicketBizContent {
        private String vip_ticket_seq;  // VIP票序列号
        private String operator;        // 操作人
        private String operate_time;    // 操作时间
        private String reason;          // 退费原因
    }

    /**
     * 退费VIP票结果
     */
    @Data
    public static class RefundVipTicketResult {
        private String plateNumber;     // 车牌号
        private int successCount;       // 成功数量
        private int failedCount;        // 失败数量
        private List<String> successSeqs;  // 成功的VIP票序列号列表
        private List<String> failedSeqs;   // 失败的VIP票序列号列表
        private String message;         // 结果消息
    }
} 