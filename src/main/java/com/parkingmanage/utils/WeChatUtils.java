package com.parkingmanage.utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序工具类
 * 支持获取真实的openid和session_key
 * 
 * @author MLH
 * @since 2024-01-01
 */
public class WeChatUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatUtils.class);
    
    // 微信小程序配置 - 请替换为你的真实配置
    // 获取方式：登录微信公众平台 -> 小程序 -> 开发 -> 开发管理 -> 开发设置
    private static final String APP_ID = "wx112d8a922018480e";  // 请替换为真实的AppID
    private static final String APP_SECRET = "421d87de7a237263a2dbfb089c4f2d45";  // 请替换为真实的AppSecret
    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    
    // 开发模式开关 - 生产环境请设置为false
    private static final boolean IS_DEV_MODE = true; // 开发调试时设为true
    
    /**
     * 获取完整的微信登录信息（推荐使用）
     * 同时获取openid和session_key
     * 
     * @param code 微信小程序登录凭证
     * @return WeChatInfo 包含openid、session_key等信息
     */
    public static WeChatInfo getWeChatInfo(String code) {
        logger.info("🔐 开始获取微信登录信息，code: [{}]", code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null");
        
        // 首先尝试调用真实的微信API，无论开发模式还是生产模式
        try {
            // 验证参数
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("微信登录凭证code不能为空");
            }
            // 构建请求URL
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_LOGIN_URL, APP_ID, APP_SECRET, code);
            
            // 调用微信API
            RestTemplate restTemplate = new RestTemplate();
            String responseBody = restTemplate.getForObject(url, String.class);
            
            logger.info("📱 微信API响应: {}", responseBody);
            
            // 解析响应
            WeChatInfo weChatInfo = parseWeChatResponse(responseBody);
            
            // 如果获取成功，直接返回
            if (weChatInfo.hasValidInfo()) {
                logger.info("✅ 成功获取真实微信登录信息");
                return weChatInfo;
            } else {
                // API调用成功但返回错误信息
                throw new RuntimeException("微信API返回错误: " + weChatInfo.getErrorDescription());
            }
            
        } catch (Exception e) {
            logger.error("❌ 调用微信登录接口失败: {}", e.getMessage());

            // 生产模式下直接返回错误
            WeChatInfo errorInfo = new WeChatInfo();
            errorInfo.setErrcode(-1);
            errorInfo.setErrmsg("系统异常: " + e.getMessage());
            return errorInfo;
        }
    }
    
    /**
     * 获取微信登录信息的Map格式（向后兼容）
     * 
     * @param code 微信小程序登录凭证
     * @return Map包含sessionKey和openid
     */
    public static Map<String, String> getWeChatInfoMap(String code) {
        WeChatInfo weChatInfo = getWeChatInfo(code);
        Map<String, String> result = new HashMap<>();
        
        if (weChatInfo.hasValidInfo()) {
            result.put("sessionKey", weChatInfo.getSessionKey());
            result.put("openid", weChatInfo.getOpenid());
            if (weChatInfo.getUnionid() != null) {
                result.put("unionid", weChatInfo.getUnionid());
            }
        } else {
            // 兼容原有的异常处理方式
            throw new RuntimeException("获取微信信息失败: " + weChatInfo.getErrorDescription());
        }
        
        return result;
    }
    
    /**
     * 通过code获取session_key（向后兼容）
     * 
     * @param code 微信小程序登录凭证
     * @return session_key
     */
    public static String getSessionKey(String code) {
        WeChatInfo weChatInfo = getWeChatInfo(code);
        
        if (weChatInfo.hasValidInfo()) {
            return weChatInfo.getSessionKey();
        } else {
            throw new RuntimeException("获取session_key失败: " + weChatInfo.getErrorDescription());
        }
    }
    
    /**
     * 通过code获取openid
     * 
     * @param code 微信小程序登录凭证
     * @return openid
     */
    public static String getOpenId(String code) {
        WeChatInfo weChatInfo = getWeChatInfo(code);
        
        if (weChatInfo.hasValidInfo()) {
            return weChatInfo.getOpenid();
        } else {
            throw new RuntimeException("获取openid失败: " + weChatInfo.getErrorDescription());
        }
    }
    
    /**
     * 解析微信API响应
     * 
     * @param responseBody 微信API响应体
     * @return WeChatInfo
     */
    private static WeChatInfo parseWeChatResponse(String responseBody) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            WeChatInfo weChatInfo = new WeChatInfo();
            
            // 检查是否有错误
            if (jsonObject.containsKey("errcode")) {
                Integer errcode = jsonObject.getInteger("errcode");
                String errmsg = jsonObject.getString("errmsg");
                logger.error("🚫 微信API返回错误 - errcode: {}, errmsg: {}", errcode, errmsg);
                weChatInfo.setErrcode(errcode);
                weChatInfo.setErrmsg(errmsg);
                return weChatInfo;
            }
            
            // 成功响应，提取信息
            weChatInfo.setOpenid(jsonObject.getString("openid"));
            weChatInfo.setSessionKey(jsonObject.getString("session_key"));
            weChatInfo.setUnionid(jsonObject.getString("unionid")); // 可能为null
            
            // 验证必要字段
            if (weChatInfo.getOpenid() == null || weChatInfo.getSessionKey() == null) {
                logger.error("❌ 微信API响应格式异常，缺少必要字段: {}", responseBody);
                weChatInfo.setErrcode(-2);
                weChatInfo.setErrmsg("响应格式异常，缺少openid或session_key");
                return weChatInfo;
            }
            
            logger.info("✅ 成功获取微信登录信息 - openid: [{}...], unionid: [{}]", 
                weChatInfo.getOpenid().substring(0, Math.min(8, weChatInfo.getOpenid().length())),
                weChatInfo.getUnionid() != null ? weChatInfo.getUnionid().substring(0, Math.min(8, weChatInfo.getUnionid().length())) + "..." : "null");
            
            return weChatInfo;
            
        } catch (Exception e) {
            logger.error("❌ 解析微信API响应失败: {}", responseBody, e);
            WeChatInfo errorInfo = new WeChatInfo();
            errorInfo.setErrcode(-3);
            errorInfo.setErrmsg("解析响应失败: " + e.getMessage());
            return errorInfo;
        }
    }

    
    /**
     * 解密微信手机号
     * 
     * @param encryptedData 加密数据
     * @param sessionKey 会话密钥
     * @param iv 初始向量
     * @return 手机号
     */
    public static String decryptPhoneNumber(String encryptedData, String sessionKey, String iv) {
        // 首先尝试真实解密
        try {
            // 验证解密参数
            if (encryptedData == null || encryptedData.trim().isEmpty()) {
                throw new IllegalArgumentException("encryptedData不能为空");
            }
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                throw new IllegalArgumentException("sessionKey不能为空");
            }
            if (iv == null || iv.trim().isEmpty()) {
                throw new IllegalArgumentException("iv不能为空");
            }
            logger.info("🔓 开始解密微信手机号");
            byte[] dataByte = Base64.getDecoder().decode(encryptedData);
            byte[] keyByte = Base64.getDecoder().decode(sessionKey);
            byte[] ivByte = Base64.getDecoder().decode(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyByte, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] resultByte = cipher.doFinal(dataByte);
            String result = new String(resultByte, "UTF-8");
            logger.info("✅ 手机号解密成功");
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.containsKey("phoneNumber")) {
                String phoneNumber = jsonObject.getString("phoneNumber");
                logger.info("📱 获取到真实手机号: {}", phoneNumber);
                return phoneNumber;
            } else {
                logger.error("❌ 解密数据中未找到手机号字段，解密结果: {}", result);
                throw new RuntimeException("解密数据中未找到手机号");
            }
            
        } catch (Exception e) {
            logger.error("❌ 解密手机号失败: {}", e.getMessage());
            // 生产模式下解密失败直接抛出异常
            throw new RuntimeException("解密手机号失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解密微信用户信息
     * 
     * @param encryptedData 加密数据
     * @param sessionKey 会话密钥  
     * @param iv 初始向量
     * @return 用户信息JSON字符串
     */
    public static String decryptUserInfo(String encryptedData, String sessionKey, String iv) {
        if (IS_DEV_MODE) {
            // 开发模式：返回模拟用户信息
            JSONObject mockUserInfo = new JSONObject();
            mockUserInfo.put("nickName", "测试用户");
            mockUserInfo.put("avatarUrl", "https://wx.qlogo.cn/mmopen/test.png");
            mockUserInfo.put("gender", 1);
            mockUserInfo.put("city", "深圳");
            mockUserInfo.put("province", "广东");
            mockUserInfo.put("country", "中国");
            
            logger.warn("🧪 开发模式：返回模拟用户信息");
            return mockUserInfo.toJSONString();
        }
        
        // 生产模式：真实解密（复用手机号解密逻辑）
        try {
            logger.info("🔓 开始解密微信用户信息");
            
            byte[] dataByte = Base64.getDecoder().decode(encryptedData);
            byte[] keyByte = Base64.getDecoder().decode(sessionKey);
            byte[] ivByte = Base64.getDecoder().decode(iv);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyByte, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            byte[] resultByte = cipher.doFinal(dataByte);
            String result = new String(resultByte, "UTF-8");
            
            logger.info("✅ 用户信息解密成功");
            return result;
            
        } catch (Exception e) {
            logger.error("❌ 解密用户信息失败", e);
            throw new RuntimeException("解密用户信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证签名
     * 
     * @param signature 签名
     * @param sessionKey 会话密钥
     * @param rawData 原始数据
     * @return 验证结果
     */
    public static boolean validateSignature(String signature, String sessionKey, String rawData) {
        try {
            String expectedSignature = org.apache.commons.codec.digest.DigestUtils.sha1Hex(rawData + sessionKey);
            boolean isValid = signature.equals(expectedSignature);
            
            logger.info("🔐 签名验证结果: {}", isValid ? "通过" : "失败");
            return isValid;
            
        } catch (Exception e) {
            logger.error("❌ 验证签名失败", e);
            return false;
        }
    }
    

    /**
     * 检查是否为有效的手机号
     * 
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasLength(phoneNumber)) {
            logger.warn("⚠️ 手机号为空");
            return false;
        }
        
        // 中国大陆手机号正则表达式
        String phoneRegex = "^1[3-9]\\d{9}$";
        boolean isValid = phoneNumber.matches(phoneRegex);
        
        if (!isValid) {
            logger.warn("⚠️ 手机号格式不正确: {}", phoneNumber);
        }
        
        return isValid;
    }
    
    /**
     * 检查是否为开发模式
     * 
     * @return 是否为开发模式
     */
    public static boolean isDevMode() {
        return IS_DEV_MODE;
    }
    
    /**
     * 获取当前配置的AppID（脱敏显示）
     * 
     * @return 脱敏的AppID
     */
    public static String getMaskedAppId() {
        if (APP_ID.length() > 6) {
            return APP_ID.substring(0, 6) + "***";
        }
        return "***";
    }
} 