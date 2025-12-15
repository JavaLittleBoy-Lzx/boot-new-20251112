# 📄 微信授权回调页面部署说明

## 🎯 **问题描述**

`wechat-callback.html` 文件需要调用 SpringBoot 项目的接口，但静态HTML文件如何访问后端API存在跨域问题。

## 🔧 **解决方案**

### **方案1：使用完整URL + CORS配置（✅ 推荐）**

#### 1. **HTML文件配置**
```javascript
// 在 wechat-callback.html 中设置API基础URL
const API_BASE_URL = 'https://www.xuerparking.cn';

// 使用完整URL调用接口
fetch(`${API_BASE_URL}/parking/wechat/auth`, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ code, state })
});
```

#### 2. **SpringBoot CORS配置**
在 `CorsConfig.java` 中已添加支持：
```java
// 添加了主域名支持
corsConfiguration.addAllowedOrigin("https://www.xuerparking.cn");
```

#### 3. **控制器CORS注解**
已在相关控制器添加：
```java
@CrossOrigin(origins = "*")
```

### **方案2：Nginx反向代理**

#### Nginx配置示例：
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 静态文件
    location / {
        root /var/www/static;
        index index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://www.xuerparking.cn:8024/parking/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### HTML中使用相对路径：
```javascript
// 使用代理后的路径
fetch('/api/wechat/auth', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ code, state })
});
```

### **方案3：放在SpringBoot项目中**

#### 优点：
- 同源，无跨域问题
- 配置简单

#### 缺点：
- 每次更新HTML需要重新部署SpringBoot项目
- 不利于CDN加速

#### 使用方法：
1. 将 `wechat-callback.html` 放在 `src/main/resources/static/` 目录
2. HTML中使用相对路径：
```javascript
fetch('/parking/wechat/auth', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ code, state })
});
```
3. 访问URL：`https://www.xuerparking.cn/wechat-callback.html`

## 🚀 **部署步骤**

### **当前采用方案1（推荐）**

#### 1. **部署HTML文件**
可以将 `wechat-callback.html` 部署到：
- CDN（阿里云OSS、腾讯云COS等）
- 静态文件服务器
- Nginx静态目录

#### 2. **配置微信公众号**
在微信公众平台设置授权回调域名：
```
your-static-domain.com
```

#### 3. **更新API地址**
如果SpringBoot服务器地址变更，只需修改HTML中的：
```javascript
const API_BASE_URL = 'https://your-new-domain';
```

#### 4. **CORS域名更新**
如果HTML部署域名变更，需要在 `CorsConfig.java` 中添加新域名：
```java
corsConfiguration.addAllowedOrigin("https://your-new-static-domain.com");
```

## 🔍 **调试方法**

#### 1. **浏览器控制台**
检查网络请求是否成功，查看CORS错误

#### 2. **调试信息**
HTML页面内置调试功能，点击"调试信息"按钮查看详细信息

#### 3. **常见问题**
- **CORS错误**：检查 `CorsConfig.java` 中是否包含HTML文件的域名
- **网络错误**：检查API_BASE_URL是否正确
- **404错误**：检查接口路径是否正确

## 📋 **检查清单**

#### 部署前检查：
- [ ] HTML文件中API_BASE_URL设置正确
- [ ] SpringBoot项目CORS配置包含HTML域名
- [ ] 微信公众平台回调域名配置正确
- [ ] SSL证书配置正确（如使用HTTPS）

#### 部署后验证：
- [ ] 浏览器直接访问HTML文件正常
- [ ] 微信授权流程能正常跳转
- [ ] API调用无CORS错误
- [ ] 授权成功后能正常返回小程序

## 🎉 **总结**

当前配置已经支持静态HTML文件通过完整URL访问SpringBoot接口，只需要：

1. **部署HTML文件**到任意静态服务器
2. **确保API_BASE_URL**指向正确的SpringBoot服务器
3. **如果更换域名**，记得更新CORS配置

这种方案灵活性最高，维护成本最低！🚀 