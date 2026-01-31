package com.parkingmanage.service;

import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 海康威视图片代理服务
 * 用于转发海康图片服务器的图片请求，解决线上服务器无法直接访问内网海康图片服务器的问题
 *
 * @author System
 */
@Slf4j
@Service
public class HikvisionImageProxyService {

    @Resource
    private RestTemplate hikvisionRestTemplate;

    @Value("${hikvision.traffic.base-url}")
    private String baseUrl;

    @Value("${hikvision.traffic.app-key}")
    private String appKey;

    @Value("${hikvision.traffic.app-secret}")
    private String appSecret;

    @Value("${hikvision.traffic.image-timeout:10000}")
    private int imageTimeout;

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
     * 代理获取图片
     * 从海康图片服务器获取图片并返回字节数组
     *
     * @param imageUrl 海康图片URL（如：http://10.100.110.82/pic?=xxx）
     * @return 图片字节数组
     */
    public byte[] proxyImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.warn("⚠️ [图片代理] 图片URL为空");
            return null;
        }

        log.info("🖼️ [图片代理] 开始代理图片 - URL: {}", imageUrl);

        try {
            // 方式1: 尝试使用 Artemis SDK 获取图片（通过 Artemis 网关）
            byte[] imageBytes = proxyImageWithArtemis(imageUrl);
            if (imageBytes != null && imageBytes.length > 0) {
                log.info("✅ [图片代理] Artemis方式代理成功 - 图片大小: {} KB", imageBytes.length / 1024);
                return imageBytes;
            }

            // 方式2: 尝试使用 HTTP 直连方式
            imageBytes = proxyImageWithHttp(imageUrl);
            if (imageBytes != null && imageBytes.length > 0) {
                log.info("✅ [图片代理] HTTP方式代理成功 - 图片大小: {} KB", imageBytes.length / 1024);
                return imageBytes;
            }

            log.warn("⚠️ [图片代理] 所有方式都失败");
            return null;

        } catch (Exception e) {
            log.error("❌ [图片代理] 代理失败 - URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 使用海康Artemis SDK获取图片（带认证）
     * 通过 Artemis 网关访问图片服务
     *
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    private byte[] proxyImageWithArtemis(String imageUrl) {
        try {
            // 创建并配置ArtemisConfig对象
            ArtemisConfig artemisConfig = createArtemisConfig();

            // 提取图片路径（如：/pic?=xxx）
            String picPath = extractPicPath(imageUrl);
            if (picPath == null) {
                log.warn("⚠️ [图片代理-Artemis] 无法提取图片路径");
                return null;
            }

            // 构建请求路径 - 通过 Artemis 网关访问
            // 图片路径可能是 /artemis/pic?=xxx 或直接 /pic?=xxx
            final String ARTEMIS_PATH = "/artemis";
            String fullPicPath = ARTEMIS_PATH + picPath;

            Map<String, String> path = new HashMap<String, String>(2) {
                {
                    if (baseUrl.startsWith("https://")) {
                        put("https://", fullPicPath);
                    } else {
                        put("http://", fullPicPath);
                    }
                }
            };

            log.info("📤 [图片代理-Artemis] 请求路径: {}", fullPicPath);

            // 使用Artemis SDK发送GET请求获取图片
            String responseStr = ArtemisHttpUtil.doGetArtemis(
                artemisConfig,
                path,
                null,
                null,
                null
            );

            // Artemis返回的是String，需要转换为byte[]
            if (responseStr != null && !responseStr.isEmpty()) {
                // 检查是否是错误响应（JSON格式的错误）
                if (responseStr.trim().startsWith("{") || responseStr.trim().startsWith("[")) {
                    log.warn("⚠️ [图片代理-Artemis] 返回的是错误响应: {}", responseStr);
                    return null;
                }

                // 尝试 ISO-8859-1 编码（单字节编码，应该能还原原始字节）
                byte[] imageBytes = responseStr.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

                // 检查是否是有效的图片（JPEG以FF D8开头）
                if (imageBytes.length > 2 && imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8) {
                    log.info("✅ [图片代理-Artemis] 图片获取成功 - 大小: {} KB", imageBytes.length / 1024);
                    return imageBytes;
                } else {
                    log.warn("⚠️ [图片代理-Artemis] 不是有效的JPEG图片 - 头: 0x{} 0x{}",
                        String.format("%02X", imageBytes[0]),
                        String.format("%02X", imageBytes[1]));
                    return null;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ [图片代理-Artemis] 获取失败", e);
            return null;
        }
    }

    /**
     * 使用HTTP直接请求获取图片（带海康签名认证）
     * 使用原生 HttpsURLConnection 绕过 RestTemplate
     *
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    private byte[] proxyImageWithHttp(String imageUrl) {
        java.io.InputStream inputStream = null;
        try {
            // 提取图片路径
            String picPath = extractPicPath(imageUrl);
            if (picPath == null) {
                log.warn("⚠️ [图片代理-HTTP] 无法提取图片路径");
                return null;
            }

            // 构建完整的请求URL
            String fullUrl = baseUrl + picPath;
            log.info("📤 [图片代理-HTTP] 请求URL: {}", fullUrl);

            // 使用原生 URL 连接
            java.net.URL url = new java.net.URL(fullUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            // 设置请求头
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/jpeg,image/png,*/*");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // 如果是 HTTPS，配置 SSL
            if (conn instanceof HttpsURLConnection) {
                configureTrustAll((HttpsURLConnection) conn);
            }

            conn.connect();

            int responseCode = conn.getResponseCode();
            log.info("📨 [图片代理-HTTP] 响应状态: {}", responseCode);

            if (responseCode == 200) {
                inputStream = conn.getInputStream();
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = outputStream.toByteArray();
                log.info("✅ [图片代理-HTTP] 获取成功 - 大小: {} KB, 头: 0x{} 0x{}",
                    imageBytes.length / 1024,
                    imageBytes.length > 0 ? String.format("%02X", imageBytes[0]) : "N/A",
                    imageBytes.length > 1 ? String.format("%02X", imageBytes[1]) : "N/A");
                return imageBytes;
            } else {
                // 读取错误响应
                java.io.InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    byte[] errorBytes = readAllBytes(errorStream);
                    String errorResponse = new String(errorBytes, java.nio.charset.StandardCharsets.UTF_8);
                    log.warn("⚠️ [图片代理-HTTP] 错误响应: {}", errorResponse);
                }
                log.warn("⚠️ [图片代理-HTTP] 响应状态: {}", responseCode);
                return null;
            }

        } catch (Exception e) {
            log.error("❌ [图片代理-HTTP] 获取失败", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 配置信任所有证书（用于 HTTPS）
     */
    private void configureTrustAll(HttpsURLConnection conn) {
        try {
            // 创建信任所有证书的 TrustManager
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };

            // 安装信任管理器
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            log.error("❌ [SSL配置] 配置失败", e);
        }
    }

    /**
     * 读取 InputStream 的所有字节
     */
    private byte[] readAllBytes(java.io.InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * 计算HMAC-SHA256签名
     *
     * @param text 待签名的文本
     * @param secret 密钥
     * @return 十六进制签名字符串
     */
    private String calculateHmacSHA256(String text, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串（大写）
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名计算失败", e);
        }
    }

    /**
     * 从完整URL中提取图片路径
     * 例如：http://10.100.110.82/pic?=xxx -> /pic?=xxx
     *
     * @param imageUrl 完整图片URL
     * @return 图片路径
     */
    private String extractPicPath(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        // 如果已经是路径格式，直接返回
        if (imageUrl.startsWith("/pic?=")) {
            return imageUrl;
        }

        // 从完整URL中提取路径
        try {
            int picIndex = imageUrl.indexOf("/pic?=");
            if (picIndex >= 0) {
                return imageUrl.substring(picIndex);
            }
        } catch (Exception e) {
            log.error("❌ [图片路径提取] 提取失败", e);
        }

        return null;
    }

    /**
     * 将图片URL转换为代理URL
     * 将海康图片服务器的URL转换为本地代理URL
     *
     * @param originalUrl 原始图片URL
     * @param baseUrl 本地服务器基础URL（如：http://xxx:8675）
     * @return 代理URL
     */
    public String convertToProxyUrl(String originalUrl, String baseUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return null;
        }

        // 如果已经是代理URL，直接返回
        if (originalUrl.contains("/parking/hikvision/traffic/image-proxy")) {
            return originalUrl;
        }

        // 转换为代理URL
        try {
            String encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8");
            return baseUrl + "/parking/hikvision/traffic/image-proxy?url=" + encodedUrl;
        } catch (Exception e) {
            log.error("❌ [图片代理] URL编码失败", e);
            return originalUrl;
        }
    }

    /**
     * 判断URL是否为海康图片地址
     *
     * @param url 图片URL
     * @return 是否为海康图片地址
     */
    public boolean isHikvisionImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.contains("10.100.110.82") ||
               url.contains("/pic?") ||
               url.contains("hikvision") ||
               url.contains("artemis"); // 海康的图片服务路径
    }
}
