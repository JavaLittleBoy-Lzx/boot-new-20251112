package com.parkingmanage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * 海康威视图片代理控制器
 * 用于解决HTTPS页面加载HTTP图片的混合内容问题
 */
@RestController
@RequestMapping("/api/hikvision")
@CrossOrigin(origins = "*")
public class HikvisionProxyController {

    private static final Logger logger = LoggerFactory.getLogger(HikvisionProxyController.class);
    
    private final RestTemplate restTemplate;
    
    public HikvisionProxyController() {
        this.restTemplate = createRestTemplate();
    }
    
    /**
     * 创建支持不安全HTTPS的RestTemplate
     * 海康设备可能使用自签名证书，需要禁用SSL验证
     */
    private RestTemplate createRestTemplate() {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            // 安装信任所有证书的SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 创建自定义的HttpsURLConnection工厂
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            RestTemplate template = new RestTemplate();
            
            // 设置超时
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
            template.setRequestFactory(factory);
            
            // 添加User-Agent
            template.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                return execution.execute(request, body);
            });
            
            logger.info("✅ [海康代理] RestTemplate初始化成功（已禁用SSL证书验证）");
            return template;
            
        } catch (Exception e) {
            logger.error("❌ [海康代理] RestTemplate初始化失败: {}", e.getMessage());
            return new RestTemplate();
        }
    }

    /**
     * 代理海康设备的图片请求
     * 
     * @param url 海康设备的图片URL (支持 http:// 和 https:// 协议，必须是内网IP)
     * @return 图片二进制数据
     */
    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            logger.info("📷 [海康代理] 请求图片: {}", url);
            
            // 安全验证：只允许代理内网海康设备的图片
            if (!isValidHikvisionUrl(url)) {
                logger.warn("⚠️ [海康代理] 非法URL，拒绝代理: {}", url);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("只允许代理内网海康设备图片".getBytes());
            }
            
            // 下载图片
            long startTime = System.currentTimeMillis();
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] imageData = response.getBody();
                logger.info("✅ [海康代理] 图片获取成功，大小: {} KB, 耗时: {} ms", 
                    imageData.length / 1024, duration);
                
                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);
                headers.setCacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic());
                headers.setContentLength(imageData.length);
                
                return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
            } else {
                logger.error("❌ [海康代理] 图片获取失败，状态码: {}", response.getStatusCode());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
        } catch (Exception e) {
            logger.error("❌ [海康代理] 代理图片失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("图片加载失败: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * 批量代理多个图片URL（返回JSON格式的Base64数据）
     * 适用于需要一次性加载多张图片的场景
     * 
     * @param urls 图片URL列表，以逗号分隔
     * @return JSON格式的图片数据
     */
    @GetMapping("/images/batch")
    public ResponseEntity<String> proxyImagesBatch(@RequestParam String urls) {
        try {
            String[] urlArray = urls.split(",");
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < urlArray.length; i++) {
                String url = urlArray[i].trim();
                
                if (!isValidHikvisionUrl(url)) {
                    continue;
                }
                
                try {
                    ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        String base64 = java.util.Base64.getEncoder().encodeToString(response.getBody());
                        if (i > 0) jsonBuilder.append(",");
                        jsonBuilder.append("{\"url\":\"").append(url).append("\",")
                                   .append("\"data\":\"data:image/jpeg;base64,").append(base64).append("\"}");
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [批量代理] 图片获取失败: {}", url);
                }
            }
            
            jsonBuilder.append("]");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            return new ResponseEntity<>(jsonBuilder.toString(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("❌ [批量代理] 失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("[]");
        }
    }
    
    /**
     * 验证URL是否为合法的海康设备地址
     * 
     * @param url 待验证的URL
     * @return 是否合法
     */
    private boolean isValidHikvisionUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // 只允许内网IP段的海康设备（支持HTTP和HTTPS）
        // 海康设备的HTTPS证书可能无效，也需要通过代理处理
        return url.contains("://10.100.111.") || 
               url.contains("://192.168.") ||
               url.contains("://172.16.") ||
               url.contains("://172.17.") ||
               url.contains("://172.18.");
    }
}
