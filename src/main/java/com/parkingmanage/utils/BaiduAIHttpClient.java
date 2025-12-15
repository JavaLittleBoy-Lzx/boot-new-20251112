package com.parkingmanage.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 百度AI HTTP客户端工具类
 * 参考百度官方文档示例
 * @author AI Assistant
 * @date 2025-06-28
 */
@Slf4j
@Component
public class BaiduAIHttpClient {

    /**
     * 获取百度AI访问令牌
     * 参考百度官方示例：https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu
     * 
     * @param clientId API Key
     * @param clientSecret Secret Key
     * @return access_token 或 null
     */
    public static String getAccessToken(String clientId, String clientSecret) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        
        try {
            // 构建请求URL - 参考百度官方文档
            String requestUrl = String.format(
                "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId, clientSecret
            );
            
            log.debug("获取百度AI访问令牌，URL: {}", requestUrl.replaceAll("(client_secret=)[^&]*", "$1***"));
            
            // 创建连接
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性 - 参考百度官方示例
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "BaiduAI-Client/1.0");
            
            // 发送空的请求体（参数已在URL中）
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write("".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            log.debug("百度Token API响应码: {}", responseCode);
            
            // 读取响应内容
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            String responseBody = response.toString();
            log.debug("百度Token API响应: {}", responseBody);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return responseBody;
            } else {
                log.error("获取访问令牌失败，响应码: {}, 响应内容: {}", responseCode, responseBody);
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取百度AI访问令牌异常", e);
            return null;
        } finally {
            // 关闭资源
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 发送车牌识别请求
     * 参考百度官方车牌识别示例
     * 
     * @param accessToken 访问令牌
     * @param base64Image base64编码的图片
     * @param multiDetect 是否检测多个车牌
     * @return API响应结果
     */
    public static String sendLicensePlateRequest(String accessToken, String base64Image, boolean multiDetect) {
        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        
        try {
            // 构建请求URL
            String requestUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate?access_token=" + accessToken;
            
            log.debug("发送车牌识别请求，图片大小: {}", base64Image.length());
            
            // 创建连接
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "BaiduAI-Client/1.0");
            
            // 构建请求参数 - 参考百度官方示例，对base64进行URL编码
            StringBuilder paramBuilder = new StringBuilder();
            String encodedImage = URLEncoder.encode(base64Image, "UTF-8");
            paramBuilder.append("image=").append(encodedImage);
            paramBuilder.append("&multi_detect=").append(multiDetect);
            
            String paramString = paramBuilder.toString();
            log.debug("请求参数长度: {} (编码后)", paramString.length());
            
            // 发送请求参数
            outputStream = connection.getOutputStream();
            outputStream.write(paramString.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            log.debug("百度车牌识别API响应码: {}", responseCode);
            
            // 读取响应内容
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            String responseBody = response.toString();
            log.debug("百度车牌识别API响应: {}", responseBody);
            
            return responseBody;
            
        } catch (Exception e) {
            log.error("发送车牌识别请求异常", e);
            return null;
        } finally {
            // 关闭资源
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
} 