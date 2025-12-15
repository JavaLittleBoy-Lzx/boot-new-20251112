# 海康威视Artemis SDK安装说明

## 问题说明
使用Apache HttpClient直接调用海康威视接口时，会遇到SSL证书验证失败的问题：
```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```

## 解决方案
使用海康威视官方提供的Artemis SDK，该SDK已经处理了SSL证书验证问题。

## 安装步骤

### 1. 下载Artemis SDK
从海康威视开放平台下载 `artemis-http-client.jar`：
- 访问：https://open.hikvision.com/
- 在"资源工具" -> "工具下载"中找到Artemis SDK
- 下载 `artemis-http-client.jar` 文件

### 2. 安装到本地Maven仓库
在命令行中执行以下命令（将路径替换为实际jar包路径）：

```bash
mvn install:install-file -Dfile=artemis-http-client.jar -DgroupId=com.hikvision.artemis -DartifactId=artemis-http-client -Dversion=1.1.3 -Dpackaging=jar
```

### 3. 验证安装
执行 `mvn dependency:tree` 检查依赖是否正确加载。

### 4. 配置说明
在 `application.yml` 中已经配置了海康威视的相关参数：
```yaml
hikvision:
  api:
    base-url: https://10.100.111.5:443/artemis
    app-key: 22668058
    app-secret: T09WZsuZyne1guzhZ4Gc
    timeout: 30000
```

## 代码修改说明

已根据海康威视官方文档修改 `HikvisionPersonService` 类：

### 1. ArtemisConfig配置方式（参考文档2.1.2）
根据文档，使用实例方法设置参数：
```java
ArtemisConfig artemisConfig = new ArtemisConfig();
artemisConfig.setHost("10.0.0.1:443"); // 平台(nginx) IP和端口
artemisConfig.setAppKey("11111111"); // 合作方 key
artemisConfig.setAppSecret("AAAAAAAAAAAAA"); // 合作方 Secret
```

### 2. doPostStringArtemis使用方式（参考文档2.2.6）
根据文档2.2.6，无header参数的POST请求方法签名：
```java
public static String doPostStringArtemis(
    ArtemisConfig artemisConfig,  // 请求host、合作方ak/sk封装类
    Map<String, String> path,     // 请求的地址，格式：{"https://": "/artemis/api/xxx"}
    String body,                  // JSON格式的请求参数，需转化为字符串
    Map<String, String> querys,   // 请求url的查询参数（可选，传null）
    String accept,                // 指定客户端能够接收的数据类型（可选，传null）
    String contentType            // 请求实体正文的媒体类型，传"application/json"
);
```

### 3. path参数格式（参考文档2.1.3.1）
path参数是HashMap，Key为协议（http://或https://），Value为接口地址：
```java
Map<String, String> path = new HashMap<String, String>(2) {
    {
        put("https://", "/artemis/api/resource/v2/person/advance/personList");
    }
};
```

### 4. 主要改进
- ✅ 使用 `ArtemisConfig` 实例方法设置host、appKey、appSecret（符合文档2.1.2）
- ✅ 使用 `ArtemisHttpUtil.doPostStringArtemis()` 发送POST请求（符合文档2.2.6）
- ✅ 正确构建path参数（符合文档2.1.3.1）
- ✅ 自动处理SSL证书验证
- ✅ 清理了未使用的导入

## 注意事项
- 确保 `artemis-http-client.jar` 版本与pom.xml中的版本一致
- 如果SDK版本不同，请修改pom.xml中的version
- 确保网络可以访问海康威视平台地址
- 安装SDK后需要重新编译项目：`mvn clean install`

