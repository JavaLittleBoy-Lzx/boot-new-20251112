# 智谱AI API集成指南 - 总览

> **项目**：东北林业大学智慧车行数据可视化平台  
> **API提供商**：智谱AI (BigModel)  
> **文档更新**：2025-11-17

---

## 📚 文档目录

本集成指南分为两个部分，完整介绍如何在您的停车管理系统中集成智谱AI的各种能力。

### 📘 第一部分：基础集成

**文件**：[文档/05-智谱AI集成指南-Part1.md](./文档/05-智谱AI集成指南-Part1.md)

**内容**：
- ✅ 智谱AI API简介与能力概览
- ✅ API密钥获取与配置
- ✅ Maven依赖配置
- ✅ 核心Service实现（ZhipuAiService）
- ✅ 对话补全API (Chat Completion)
- ✅ 应用场景实现
  - 智能客服助手
  - 访客通知生成
  - 数据分析报告生成

### 📗 第二部分：高级功能

**文件**：[文档/05-智谱AI集成指南-Part2.md](./文档/05-智谱AI集成指南-Part2.md)

**内容**：
- ✅ Embeddings文本向量化
- ✅ 语义搜索实现
- ✅ 函数调用 (Function Call)
- ✅ 最佳实践
  - 提示词工程
  - 错误处理与重试
  - 费用控制
  - 缓存策略
- ✅ 安全建议
- ✅ 完整示例代码
- ✅ 监控与日志
- ✅ 常见问题解答

---

## 🎯 快速开始

### 1. 获取API密钥

```
1. 访问：https://open.bigmodel.cn/
2. 注册/登录账号
3. 创建API Key
```

### 2. 配置项目

在 `application.yml` 中添加：

```yaml
zhipu:
  api:
    key: your_api_key_here
    base-url: https://open.bigmodel.cn/api/paas/v4
    timeout: 30000
    model: glm-4
```

### 3. 创建Service类

复制 `ZhipuAiService.java` 代码到项目中：
```
src/main/java/com/parkingmanage/service/ZhipuAiService.java
```

### 4. 开始使用

```java
@Resource
private ZhipuAiService zhipuAiService;

// 简单对话
String answer = zhipuAiService.chatCompletion("如何预约停车位？");

// 带历史的对话
JSONArray history = new JSONArray();
// ... 添加历史消息
String answer = zhipuAiService.chatCompletion("继续刚才的话题", history);
```

---

## 💡 主要功能

### 1️⃣ 智能客服

**场景**：访客咨询停车规则、预约流程

```java
@PostMapping("/api/assistant/chat")
public ResponseEntity<Result> chat(@RequestBody ChatRequest request) {
    String answer = zhipuAiService.chatCompletion(request.getQuestion());
    return ResponseEntity.ok(Result.success(answer));
}
```

**示例对话**：
- 用户："我想预约明天上午的停车位"
- AI："好的，请提供您的车牌号和具体到访时间，我将为您办理预约。"

### 2️⃣ 文本生成

**场景**：自动生成访客通知、报告

```java
String prompt = "为访客张三生成预约成功通知，车牌京A12345，时间2025-01-20";
String notice = zhipuAiService.chatCompletion(prompt);
```

**生成示例**：
> "尊敬的张三先生，您已成功预约2025年1月20日的停车位，车牌号：京A12345。请准时到访，祝您出行愉快！"

### 3️⃣ 语义搜索

**场景**：智能搜索相似访客记录

```java
// 获取向量
float[] queryVector = zhipuAiService.getEmbeddings("查找上周来访的教授");

// 计算相似度
double similarity = zhipuAiService.cosineSimilarity(vec1, vec2);
```

### 4️⃣ 函数调用

**场景**：自然语言查询数据

```java
// 用户："查询昨天的车流量"
// AI自动调用：getVehicleFlow("2025-01-16")
```

---

## 🔥 应用场景示例

| 场景 | 功能 | 实现方式 |
|-----|------|---------|
| **智能客服** | 24小时在线回答访客问题 | Chat Completion |
| **访客通知** | 自动生成预约成功/提醒短信 | 文本生成 |
| **数据报告** | 自动生成车流分析报告 | 文本生成 |
| **智能搜索** | 语义化搜索历史记录 | Embeddings |
| **事件总结** | 自动总结异常事件 | Chat Completion |
| **违规提醒** | 生成个性化违规通知 | 文本生成 |
| **数据查询** | 自然语言查询数据 | Function Call |

---

## 📊 技术优势

### ✨ 为什么选择智谱AI？

1. **国产大模型**
   - 符合国内合规要求
   - 中文理解能力强
   - 数据安全有保障

2. **性价比高**
   - GLM-3-Turbo：经济实惠
   - GLM-4：性能强大
   - 按需付费，灵活控制

3. **功能丰富**
   - 对话、生成、理解
   - Embeddings向量化
   - Function Call
   - 图像理解

4. **易于集成**
   - 标准RESTful API
   - 完善的文档
   - 活跃的社区

---

## 🛡️ 安全与最佳实践

### 安全建议

- ✅ API密钥使用环境变量
- ✅ 输入内容过滤敏感信息
- ✅ 实现请求频率限制
- ✅ 记录完整的调用日志

### 成本控制

- ✅ 实现缓存机制
- ✅ 限制每日调用次数
- ✅ 优化提示词长度
- ✅ 选择合适的模型

### 性能优化

- ✅ 异步调用
- ✅ 批量处理
- ✅ 重试机制
- ✅ 超时控制

---

## 📈 集成效果预期

### 用户体验提升

- **响应速度**：1-3秒内获得智能回答
- **准确率**：常见问题准确率 >90%
- **满意度**：用户满意度提升 30%+

### 运营效率提升

- **人工成本**：减少客服工作量 60%
- **响应时间**：7×24小时即时响应
- **错误率**：自动化生成，错误率 <1%

### 数据洞察增强

- **报告生成**：从手动2小时 → 自动30秒
- **趋势分析**：智能发现数据规律
- **决策支持**：AI辅助运营决策

---

## 🔗 相关链接

### 官方资源

- **API文档**：https://docs.bigmodel.cn/api-reference
- **控制台**：https://open.bigmodel.cn/
- **定价说明**：https://open.bigmodel.cn/pricing

### 项目文档

- [01-项目概述与架构](./文档/01-项目概述与架构.md)
- [02-核心功能模块详解](./文档/02-核心功能模块详解.md)
- [03-API接口文档](./文档/03-API接口文档.md)
- [04-配置与部署指南](./文档/04-配置与部署指南.md)

---

## ❓ 常见问题

**Q: 如何选择模型？**

A: 
- GLM-3-Turbo：日常对话、简单生成（经济）
- GLM-4：复杂分析、专业内容（性能）

**Q: 费用大概多少？**

A: 
- 估算：1000次对话/月 ≈ 50-100元
- 具体以实际调用量为准

**Q: 如何保证服务稳定？**

A:
- 实现重试机制
- 添加本地缓存
- 设置降级方案

---

## 📞 技术支持

如有问题，请参考：
1. 详细文档（Part1 + Part2）
2. 官方API文档
3. 项目团队技术支持

---

**开始阅读**：[第一部分 - 基础集成](./文档/05-智谱AI集成指南-Part1.md)
