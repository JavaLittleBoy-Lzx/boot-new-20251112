# 智谱AI API集成指南 - 第2部分

> **版本**：V1.0 | **更新**：2025-11-17

---

## 六、Embeddings文本向量化

### 6.1 应用场景

- **语义搜索**：搜索相似的访客记录、违规事件
- **智能推荐**：推荐相关的历史案例
- **内容去重**：检测重复的咨询问题

### 6.2 实现代码

**在ZhipuAiService中添加**：

```java
/**
 * 文本向量化 (Embeddings)
 */
public float[] getEmbeddings(String text) {
    try {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "embedding-2");
        requestBody.put("input", text);
        
        String response = sendPostRequest("/embeddings", requestBody.toJSONString());
        
        JSONObject result = JSON.parseObject(response);
        JSONArray data = result.getJSONArray("data");
        if (data != null && !data.isEmpty()) {
            JSONObject embedding = data.getJSONObject(0);
            JSONArray vector = embedding.getJSONArray("embedding");
            
            float[] embeddings = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                embeddings[i] = vector.getFloatValue(i);
            }
            return embeddings;
        }
        
        return null;
    } catch (Exception e) {
        logger.error("获取Embeddings失败: {}", e.getMessage(), e);
        return null;
    }
}

/**
 * 计算余弦相似度
 */
public double cosineSimilarity(float[] vec1, float[] vec2) {
    if (vec1.length != vec2.length) {
        throw new IllegalArgumentException("向量维度不匹配");
    }
    
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;
    
    for (int i = 0; i < vec1.length; i++) {
        dotProduct += vec1[i] * vec2[i];
        norm1 += vec1[i] * vec1[i];
        norm2 += vec2[i] * vec2[i];
    }
    
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}
```

### 6.3 语义搜索示例

```java
@ApiOperation("语义搜索访客记录")
@GetMapping("/semantic-search")
public ResponseEntity<Result> semanticSearch(@RequestParam String query) {
    
    // 1. 获取查询文本的向量
    float[] queryVector = zhipuAiService.getEmbeddings(query);
    
    // 2. 从数据库获取所有记录（实际应该分批处理）
    List<VisitorReservationSync> allRecords = visitorService.getAllRecords();
    
    // 3. 计算相似度并排序
    List<SearchResult> results = new ArrayList<>();
    for (VisitorReservationSync record : allRecords) {
        String recordText = record.getVisitorName() + " " + 
                           record.getVisitPurpose() + " " + 
                           record.getCarNumber();
        
        float[] recordVector = zhipuAiService.getEmbeddings(recordText);
        double similarity = zhipuAiService.cosineSimilarity(queryVector, recordVector);
        
        results.add(new SearchResult(record, similarity));
    }
    
    // 4. 按相似度排序，取Top 10
    results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
    List<SearchResult> topResults = results.stream()
            .limit(10)
            .collect(Collectors.toList());
    
    return ResponseEntity.ok(Result.success(topResults));
}
```

---

## 七、函数调用 (Function Call)

### 7.1 应用场景

用户可以用自然语言查询数据，AI自动调用相应的系统函数。

**示例**：
- "查询昨天的车流量" → 自动调用 `getVehicleFlow(date)`
- "帮我查一下京A12345的进场记录" → 自动调用 `getCarInRecords(plateNumber)`

### 7.2 定义函数

```java
/**
 * 定义可调用的函数
 */
public JSONArray defineFunctions() {
    JSONArray functions = new JSONArray();
    
    // 函数1：查询车流量
    JSONObject func1 = new JSONObject();
    func1.put("name", "get_vehicle_flow");
    func1.put("description", "查询指定日期的车辆进出场流量统计");
    
    JSONObject params1 = new JSONObject();
    params1.put("type", "object");
    
    JSONObject properties1 = new JSONObject();
    JSONObject dateParam = new JSONObject();
    dateParam.put("type", "string");
    dateParam.put("description", "日期，格式：YYYY-MM-DD");
    properties1.put("date", dateParam);
    
    params1.put("properties", properties1);
    params1.put("required", new JSONArray().fluentAdd("date"));
    
    func1.put("parameters", params1);
    functions.add(func1);
    
    // 函数2：查询车辆记录
    JSONObject func2 = new JSONObject();
    func2.put("name", "get_car_records");
    func2.put("description", "根据车牌号查询车辆进出场记录");
    
    JSONObject params2 = new JSONObject();
    params2.put("type", "object");
    
    JSONObject properties2 = new JSONObject();
    JSONObject plateParam = new JSONObject();
    plateParam.put("type", "string");
    plateParam.put("description", "车牌号");
    properties2.put("plate_number", plateParam);
    
    params2.put("properties", properties2);
    params2.put("required", new JSONArray().fluentAdd("plate_number"));
    
    func2.put("parameters", params2);
    functions.add(func2);
    
    return functions;
}
```

### 7.3 调用示例

```java
@ApiOperation("智能查询（带函数调用）")
@PostMapping("/smart-query")
public ResponseEntity<Result> smartQuery(@RequestBody QueryRequest request) {
    
    // 1. 构建请求
    JSONObject requestBody = new JSONObject();
    requestBody.put("model", "glm-4");
    
    JSONArray messages = new JSONArray();
    JSONObject userMsg = new JSONObject();
    userMsg.put("role", "user");
    userMsg.put("content", request.getQuery());
    messages.add(userMsg);
    
    requestBody.put("messages", messages);
    requestBody.put("tools", defineFunctions());
    
    // 2. 调用AI
    String response = zhipuAiService.sendPostRequest(
        "/chat/completions", 
        requestBody.toJSONString()
    );
    
    // 3. 解析响应
    JSONObject result = JSON.parseObject(response);
    JSONObject choice = result.getJSONArray("choices").getJSONObject(0);
    JSONObject message = choice.getJSONObject("message");
    
    // 4. 检查是否需要调用函数
    JSONArray toolCalls = message.getJSONArray("tool_calls");
    if (toolCalls != null && !toolCalls.isEmpty()) {
        JSONObject toolCall = toolCalls.getJSONObject(0);
        String functionName = toolCall.getJSONObject("function").getString("name");
        String arguments = toolCall.getJSONObject("function").getString("arguments");
        
        // 5. 执行相应的函数
        Object functionResult = executFunction(functionName, arguments);
        
        // 6. 将函数结果返回给AI，生成最终回答
        // ... (省略完整流程)
        
        return ResponseEntity.ok(Result.success(functionResult));
    }
    
    // 直接返回AI回答
    return ResponseEntity.ok(Result.success(message.getString("content")));
}

/**
 * 执行函数
 */
private Object executFunction(String functionName, String arguments) {
    JSONObject args = JSON.parseObject(arguments);
    
    switch (functionName) {
        case "get_vehicle_flow":
            String date = args.getString("date");
            return vehicleFlowService.getFlowByDate(date);
            
        case "get_car_records":
            String plateNumber = args.getString("plate_number");
            return carRecordService.getRecordsByPlate(plateNumber);
            
        default:
            return "未知函数";
    }
}
```

---

## 八、最佳实践

### 8.1 提示词工程

#### 系统角色定义

```java
String systemPrompt = "你是东北林业大学智慧停车管理系统的AI助手。\n" +
    "你的职责包括：\n" +
    "1. 回答访客关于停车预约、进出场的问题\n" +
    "2. 解释停车规则和收费标准\n" +
    "3. 帮助用户查询车辆记录\n" +
    "\n" +
    "注意事项：\n" +
    "- 语言简洁、专业、礼貌\n" +
    "- 不确定的信息要说明\n" +
    "- 涉及个人隐私的查询需要验证身份\n" +
    "- 回答长度控制在200字以内";
```

#### Few-Shot示例

```java
JSONArray fewShotExamples = new JSONArray();

// 示例1
JSONObject example1User = new JSONObject();
example1User.put("role", "user");
example1User.put("content", "我可以在校园停车吗？");
fewShotExamples.add(example1User);

JSONObject example1Assistant = new JSONObject();
example1Assistant.put("role", "assistant");
example1Assistant.put("content", "访客可以通过预约系统提前预约停车位。" +
    "请提供您的姓名、车牌号和来访时间，系统将为您分配停车位。");
fewShotExamples.add(example1Assistant);

// 添加到消息中
messages.addAll(fewShotExamples);
```

### 8.2 错误处理

```java
public String chatCompletionWithRetry(String userMessage, int maxRetries) {
    int retries = 0;
    
    while (retries < maxRetries) {
        try {
            return chatCompletion(userMessage);
        } catch (Exception e) {
            retries++;
            logger.warn("智谱AI调用失败，重试次数: {}/{}", retries, maxRetries);
            
            if (retries >= maxRetries) {
                logger.error("智谱AI调用失败，已达最大重试次数");
                return "抱歉，系统暂时无法处理您的请求，请稍后再试。";
            }
            
            // 指数退避
            try {
                Thread.sleep(1000 * (long)Math.pow(2, retries));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    return null;
}
```

### 8.3 费用控制

```java
/**
 * Token计数（估算）
 */
public int estimateTokens(String text) {
    // 中文：约1.5字符=1 token
    // 英文：约4字符=1 token
    int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
    int otherChars = text.length() - chineseChars;
    
    return (int)(chineseChars / 1.5 + otherChars / 4.0);
}

/**
 * 限制请求频率
 */
@Component
public class RateLimiter {
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String userId) {
        int count = requestCounts.getOrDefault(userId, 0);
        if (count >= 100) {  // 每小时100次
            return false;
        }
        requestCounts.put(userId, count + 1);
        return true;
    }
}
```

### 8.4 缓存策略

```java
@Service
public class CachedZhipuAiService {
    
    @Resource
    private ZhipuAiService zhipuAiService;
    
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    public String chatCompletionCached(String userMessage) {
        // 1. 检查缓存
        String cacheKey = DigestUtils.md5Hex(userMessage);
        if (cache.containsKey(cacheKey)) {
            logger.info("命中缓存: {}", userMessage);
            return cache.get(cacheKey);
        }
        
        // 2. 调用API
        String response = zhipuAiService.chatCompletion(userMessage);
        
        // 3. 存入缓存
        if (response != null) {
            cache.put(cacheKey, response);
        }
        
        return response;
    }
}
```

---

## 九、安全建议

### 9.1 API密钥安全

```yaml
# ❌ 不要硬编码在代码中
# ✅ 使用环境变量或配置中心
zhipu:
  api:
    key: ${ZHIPU_API_KEY:default_key}
```

### 9.2 输入验证

```java
public String sanitizeInput(String input) {
    // 1. 长度限制
    if (input.length() > 2000) {
        input = input.substring(0, 2000);
    }
    
    // 2. 敏感词过滤
    String[] sensitiveWords = {"密码", "身份证", "银行卡"};
    for (String word : sensitiveWords) {
        input = input.replace(word, "***");
    }
    
    // 3. HTML标签移除
    input = input.replaceAll("<[^>]*>", "");
    
    return input;
}
```

### 9.3 内容审核

```java
public boolean isContentSafe(String content) {
    // 调用智谱AI的内容审核API
    // 或使用第三方审核服务
    return true;
}
```

---

## 十、完整示例

### 10.1 智能访客咨询完整流程

```java
@RestController
@RequestMapping("/api/smart-visitor")
public class SmartVisitorController {
    
    @Resource
    private ZhipuAiService zhipuAiService;
    
    @Resource
    private VisitorReservationService reservationService;
    
    @PostMapping("/consult")
    public ResponseEntity<Result> consult(@RequestBody ConsultRequest request) {
        
        // 1. 输入验证
        String question = sanitizeInput(request.getQuestion());
        
        // 2. 构建上下文
        String context = buildContext(request.getUserId());
        
        // 3. 构建提示词
        String prompt = String.format(
            "用户背景：%s\n用户问题：%s\n请给出专业回答。",
            context, question
        );
        
        // 4. 调用AI
        String answer = zhipuAiService.chatCompletionWithRetry(prompt, 3);
        
        // 5. 记录日志
        logConsultation(request.getUserId(), question, answer);
        
        // 6. 返回结果
        return ResponseEntity.ok(Result.success(answer));
    }
    
    private String buildContext(String userId) {
        // 获取用户的预约记录
        List<VisitorReservationSync> records = 
            reservationService.getByUserId(userId);
        
        if (records.isEmpty()) {
            return "新访客，无历史记录";
        }
        
        return String.format("该访客有%d次预约记录，最近一次是%s",
            records.size(),
            records.get(0).getCreateTime()
        );
    }
}
```

---

## 十一、监控与日志

### 11.1 请求日志

```java
@Aspect
@Component
public class ZhipuAiLogAspect {
    
    @Around("execution(* com.parkingmanage.service.ZhipuAiService.chat*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("智谱AI调用成功，耗时: {}ms", duration);
            
            return result;
        } catch (Exception e) {
            logger.error("智谱AI调用失败: {}", e.getMessage());
            throw e;
        }
    }
}
```

### 11.2 性能监控

```java
@Component
public class ZhipuAiMetrics {
    
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong successCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);
    
    public void recordSuccess() {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
    }
    
    public void recordFailure() {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
    }
    
    @Scheduled(cron = "0 0 * * * *")  // 每小时统计
    public void reportMetrics() {
        logger.info("智谱AI统计 - 总调用: {}, 成功: {}, 失败: {}",
            totalCalls.get(), successCalls.get(), failedCalls.get());
    }
}
```

---

## 十二、常见问题

### Q1: API调用超时怎么办？

**A**: 
1. 增加timeout配置
2. 使用异步调用
3. 实现重试机制

### Q2: 如何减少费用？

**A**:
1. 实现缓存机制
2. 使用更经济的模型（glm-3-turbo）
3. 限制每日调用次数
4. 优化提示词长度

### Q3: 如何提高回答质量？

**A**:
1. 优化系统提示词
2. 提供Few-Shot示例
3. 添加上下文信息
4. 使用Function Call精确查询

---

**相关文档**：
- [官方API文档](https://docs.bigmodel.cn/api-reference)
- [01-项目概述与架构](./01-项目概述与架构.md)
- [02-核心功能模块详解](./02-核心功能模块详解.md)
