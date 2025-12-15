# 智谱AI API集成指南 - 第1部分

> **版本**：V1.0 | **更新**：2025-11-17 | **API文档**：https://docs.bigmodel.cn/api-reference

---

## 一、智谱AI API简介

### 1.1 API能力概览

智谱AI提供以下核心API能力：

| API类型 | 功能 | 应用场景 |
|--------|------|---------|
| **对话API** | GLM-4/GLM-3-Turbo对话 | 智能客服、访客咨询 |
| **文本生成** | 文本补全、续写 | 自动生成报告、通知 |
| **Embeddings** | 文本向量化 | 语义搜索、相似度匹配 |
| **图像理解** | 图片内容分析 | 车牌图片识别、现场图片分析 |
| **函数调用** | Function Call | 智能调用系统功能 |

### 1.2 在停车系统中的应用场景

#### 🤖 智能客服助手
- 访客预约咨询
- 停车规则解答
- 进出场记录查询

#### 📊 数据分析助手
- 车流趋势分析报告生成
- 异常事件智能总结
- 访客统计报告撰写

#### 🔍 智能搜索
- 语义化搜索访客记录
- 车辆信息智能匹配
- 相似事件检索

#### 📝 自动化文档
- 日报/周报自动生成
- 访客通知自动撰写
- 违规提醒文案生成

---

## 二、API密钥获取

### 2.1 注册账号

1. 访问：https://open.bigmodel.cn/
2. 注册/登录账号
3. 进入控制台

### 2.2 创建API Key

```
控制台 → API Keys → 创建新密钥
```

**密钥示例**：
```
API Key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx
```

### 2.3 配置到项目

在 `application.yml` 中添加：

```yaml
# 智谱AI配置
zhipu:
  api:
    key: your_api_key_here
    base-url: https://open.bigmodel.cn/api/paas/v4
    timeout: 30000
    model: glm-4  # 或 glm-3-turbo
```

---

## 三、依赖配置

### 3.1 Maven依赖

在 `pom.xml` 中添加HTTP客户端依赖（项目已有）：

```xml
<!-- Apache HttpClient (已有) -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.13</version>
</dependency>

<!-- FastJSON (已有) -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.46</version>
</dependency>
```

---

## 四、核心Service实现

### 4.1 创建ZhipuAiService

**文件位置**：`src/main/java/com/parkingmanage/service/ZhipuAiService.java`

```java
package com.parkingmanage.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 智谱AI服务类
 */
@Service
public class ZhipuAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ZhipuAiService.class);
    
    @Value("${zhipu.api.key}")
    private String apiKey;
    
    @Value("${zhipu.api.base-url}")
    private String baseUrl;
    
    @Value("${zhipu.api.model:glm-4}")
    private String model;
    
    /**
     * 对话补全 (Chat Completion)
     */
    public String chatCompletion(String userMessage) {
        return chatCompletion(userMessage, null);
    }
    
    /**
     * 对话补全（带历史消息）
     */
    public String chatCompletion(String userMessage, JSONArray history) {
        try {
            // 1. 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            // 2. 构建消息列表
            JSONArray messages = new JSONArray();
            
            // 添加历史消息
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }
            
            // 添加当前用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            
            // 3. 发送请求
            String response = sendPostRequest("/chat/completions", requestBody.toJSONString());
            
            // 4. 解析响应
            JSONObject result = JSON.parseObject(response);
            JSONArray choices = result.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getString("content");
            }
            
            logger.error("智谱AI响应格式异常: {}", response);
            return null;
            
        } catch (Exception e) {
            logger.error("调用智谱AI失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 流式对话（SSE）
     */
    public void chatCompletionStream(String userMessage, StreamCallback callback) {
        // 流式实现（可选）
    }
    
    /**
     * 发送HTTP POST请求
     */
    private String sendPostRequest(String endpoint, String jsonBody) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            String url = baseUrl + endpoint;
            HttpPost httpPost = new HttpPost(url);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            
            // 设置请求体
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 执行请求
            CloseableHttpResponse response = httpClient.execute(httpPost);
            
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode == 200) {
                    return responseBody;
                } else {
                    logger.error("智谱AI请求失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    throw new RuntimeException("API请求失败: " + statusCode);
                }
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onMessage(String delta);
        void onComplete();
        void onError(Exception e);
    }
}
```

---

## 五、应用场景实现

### 5.1 智能客服助手

**Controller**：`IntelligentAssistantController.java`

```java
package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.service.ZhipuAiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "智能助手")
@RestController
@RequestMapping("/api/assistant")
public class IntelligentAssistantController {
    
    @Resource
    private ZhipuAiService zhipuAiService;
    
    @ApiOperation("智能问答")
    @PostMapping("/chat")
    public ResponseEntity<Result> chat(@RequestBody ChatRequest request) {
        
        // 构建系统提示词
        String systemPrompt = "你是东北林业大学智慧停车系统的AI助手。" +
                "你的职责是帮助访客和用户解答关于停车预约、车辆进出、" +
                "停车规则等问题。请用简洁、专业的语言回答。";
        
        // 构建消息历史
        com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
        
        com.alibaba.fastjson.JSONObject systemMsg = new com.alibaba.fastjson.JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        // 调用智谱AI
        String answer = zhipuAiService.chatCompletion(request.getQuestion(), messages);
        
        return ResponseEntity.ok(Result.success(answer));
    }
    
    @ApiOperation("生成访客通知")
    @PostMapping("/generate-notice")
    public ResponseEntity<Result> generateNotice(@RequestBody NoticeRequest request) {
        
        String prompt = String.format(
                "请为以下访客生成一条预约成功的通知短信：\n" +
                "访客姓名：%s\n" +
                "车牌号：%s\n" +
                "预约时间：%s\n" +
                "要求：礼貌、简洁，不超过70字",
                request.getVisitorName(),
                request.getCarNumber(),
                request.getAppointmentTime()
        );
        
        String notice = zhipuAiService.chatCompletion(prompt);
        
        return ResponseEntity.ok(Result.success(notice));
    }
}

class ChatRequest {
    private String question;
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}

class NoticeRequest {
    private String visitorName;
    private String carNumber;
    private String appointmentTime;
    
    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
    public String getCarNumber() { return carNumber; }
    public void setCarNumber(String carNumber) { this.carNumber = carNumber; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { 
        this.appointmentTime = appointmentTime; 
    }
}
```

### 5.2 数据分析报告生成

```java
@ApiOperation("生成车流分析报告")
@GetMapping("/generate-report")
public ResponseEntity<Result> generateReport(
        @RequestParam String startDate,
        @RequestParam String endDate) {
    
    // 1. 查询数据
    // ... 从数据库查询车流数据
    
    // 2. 构建提示词
    String prompt = String.format(
            "根据以下车流数据，生成一份专业的分析报告：\n" +
            "时间范围：%s 至 %s\n" +
            "总进场车辆：XXX辆\n" +
            "总离场车辆：XXX辆\n" +
            "平均停车时长：XX分钟\n" +
            "高峰时段：XX:00-XX:00\n" +
            "要求：包含数据总结、趋势分析、建议三部分",
            startDate, endDate
    );
    
    // 3. 调用AI生成报告
    String report = zhipuAiService.chatCompletion(prompt);
    
    return ResponseEntity.ok(Result.success(report));
}
```

---

## 继续阅读

**下一部分**：[05-智谱AI集成指南-Part2.md](./05-智谱AI集成指南-Part2.md)
- Embeddings向量化
- 函数调用Function Call
- 完整示例代码
- 最佳实践
