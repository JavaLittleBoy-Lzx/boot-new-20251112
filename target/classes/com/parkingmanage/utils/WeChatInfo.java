package com.parkingmanage.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录信息DTO
 * 用于封装微信jscode2session接口返回的数据
 * 
 * @author parking-system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeChatInfo {
    
    /**
     * 用户唯一标识
     */
    private String openid;
    
    /**
     * 会话密钥
     */
    private String sessionKey;
    
    /**
     * 用户在开放平台的唯一标识符（可选）
     * 只有在满足一定条件的小程序中才会返回
     */
    private String unionid;
    
    /**
     * 错误码（有错误时返回）
     */
    private Integer errcode;
    
    /**
     * 错误信息（有错误时返回）
     */
    private String errmsg;
    
    /**
     * 检查是否成功获取信息
     * @return true-成功，false-失败
     */
    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }
    
    /**
     * 检查是否有有效的登录信息
     * @return true-有效，false-无效
     */
    public boolean hasValidInfo() {
        return isSuccess() && openid != null && sessionKey != null;
    }
    
    /**
     * 获取错误描述
     * @return 错误描述
     */
    public String getErrorDescription() {
        if (isSuccess()) {
            return null;
        }
        
        // 常见错误码说明
        switch (errcode) {
            case 40029:
                return "code无效，请重新获取";
            case 45011:
                return "API调用太频繁，请稍后再试";
            case 40013:
                return "AppID无效";
            case 40125:
                return "AppSecret无效";
            default:
                return errmsg != null ? errmsg : "未知错误：" + errcode;
        }
    }
    
    @Override
    public String toString() {
        if (!isSuccess()) {
            return String.format("WeChatInfo{errcode=%d, errmsg='%s'}", errcode, errmsg);
        }
        
        return String.format("WeChatInfo{openid='%s', sessionKey='%s', unionid='%s'}", 
            openid != null ? openid.substring(0, Math.min(8, openid.length())) + "..." : "null",
            sessionKey != null ? "***" : "null",
            unionid != null ? unionid.substring(0, Math.min(8, unionid.length())) + "..." : "null");
    }
} 