package com.parkingmanage.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    //防cors注入攻击的
    // 当前跨域请求最大有效时长。这里默认1天
    private static final long MAX_AGE = 24 * 60 * 60;
    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
//      corsConfiguration.addAllowedOrigin("https://open.yidianting.xin"); // 1 设置访问源地址
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:6954"); // 1 设置访问源地址
        corsConfiguration.addAllowedOrigin("http://www.xuerparking.cn:6954"); // 1 设置访问源地址
        corsConfiguration.addAllowedOrigin("https://www.xuerparking.cn:6954"); // 1 设置访问源地址
        // 🆕 添加微信相关的域名支持
        corsConfiguration.addAllowedOrigin("https://www.xuerparking.cn"); // SpringBoot服务器自身
        // 🆕 如果HTML文件部署在其他CDN或静态服务器上，需要添加对应域名
        // corsConfiguration.addAllowedOrigin("https://your-cdn-domain.com");
//      corsConfiguration.addAllowedOrigin("https://47215w4p56.zicp.fun"); // 1 设置前台访问源地址
//      corsConfiguration.addAllowedOrigin("https://472154x56q.vicp.fun"); // 1 设置后台访问源地址
//      corsConfiguration.addAllowedOrigin("https://40038o456.zicp.fun"); // 1 设置访问源地址,
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.addAllowedHeader("*"); // 2 设置访问源请求头
        corsConfiguration.addAllowedMethod("*"); // 3 设置访问源请求方法
        corsConfiguration.setMaxAge(MAX_AGE);
//        System.out.println("")
        return corsConfiguration;
    }
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig()); // 4 对接口配置跨域设置
        return new CorsFilter(source);
    }
}