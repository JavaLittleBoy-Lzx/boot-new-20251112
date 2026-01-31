# 海康威视违章查询接口 - 快速上手指南

## 🚀 最简单的测试方式

### 方式1：完全不需要参数（推荐）

```bash
GET /parking/hikvision/traffic/violations/search-simple
```

**直接访问**：
```
http://localhost:8675/parking/hikvision/traffic/violations/search-simple
```

**说明**：自动查询最近24小时的违章记录，第1页，每页20条

---

### 方式2：只传4个必要参数

```bash
POST /parking/hikvision/traffic/violations/search
```

**最简请求体**（只需要这4个参数）：
```json
{
  "pageSize": 20,
  "pageNo": 1,
  "beginTime": "2025-01-28T00:00:00.000+08:00",
  "endTime": "2025-01-28T23:59:59.000+08:00"
}
```

**cURL 测试**：
```bash
curl -X POST http://localhost:8675/parking/hikvision/traffic/violations/search \
  -H "Content-Type: application/json" \
  -d '{
    "pageSize": 20,
    "pageNo": 1,
    "beginTime": "2025-01-28T00:00:00.000+08:00",
    "endTime": "2025-01-28T23:59:59.000+08:00"
  }'
```

---

## 📋 核心参数说明

### 必填参数（4个）

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| pageSize | Integer | 每页记录数，范围 (0, 1000] | 20 |
| pageNo | Integer | 页码，从1开始 | 1 |
| beginTime | String | 开始时间 | "2025-01-28T00:00:00.000+08:00" |
| endTime | String | 结束时间 | "2025-01-28T23:59:59.000+08:00" |

### 可选参数（如果需要更多筛选）

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| plateNo | String | 车牌号（模糊查询） | "浙A12345" |
| illegalType | String | 违章类型：1-超速，2-逆行，3-黑名单，5-违停 | "1" |
| speedType | String | 测速类型：1-点位测速，2-区间测速 | "1" |
| monitoringId | String | 卡口点id | "xxx" |

---

## 🎯 常用场景示例

### 场景1：查询今天的所有违章

```json
{
  "pageSize": 100,
  "pageNo": 1,
  "beginTime": "2025-01-28T00:00:00.000+08:00",
  "endTime": "2025-01-28T23:59:59.000+08:00"
}
```

### 场景2：查询最近1小时的违章

```json
{
  "pageSize": 50,
  "pageNo": 1,
  "beginTime": "2025-01-28T14:00:00.000+08:00",
  "endTime": "2025-01-28T15:00:00.000+08:00"
}
```

### 场景3：查询指定车牌的违章

```json
{
  "pageSize": 20,
  "pageNo": 1,
  "beginTime": "2025-01-28T00:00:00.000+08:00",
  "endTime": "2025-01-28T23:59:59.000+08:00",
  "plateNo": "浙A12345"
}
```

### 场景4：只查询超速违章

```json
{
  "pageSize": 20,
  "pageNo": 1,
  "beginTime": "2025-01-28T00:00:00.000+08:00",
  "endTime": "2025-01-28T23:59:59.000+08:00",
  "illegalType": "1"
}
```

---

## 📊 响应数据格式

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 100,
    "pageSize": 20,
    "pageNo": 1,
    "list": [
      {
        "crossTime": "2025-01-28T15:00:00.000+08:00",
        "eventId": "cc2c881339ed4222839cb34a974655fd",
        "plateNo": "浙59SVN",
        "illegalType": 1,
        "speedType": 1,
        "monitoringId": "24522978916a4f0aa432508f01a12223",
        "monitoringName": "Channel-007",
        "platePicUri": "/pic?=xxx",
        "carPicUri": "/pic?=xxx",
        "speed": 100,
        "pointIntervalName": "点位6000"
      }
    ]
  }
}
```

### 响应字段说明

| 字段名 | 说明 |
|--------|------|
| crossTime | 过车时间 |
| eventId | 事件唯一标识 |
| plateNo | 车牌号 |
| illegalType | 违章类型：1-超速，2-逆行，3-黑名单，5-违停 |
| speedType | 测速类型：1-点位测速，2-区间测速 |
| monitoringName | 卡口点名称 |
| speed | 车速(km/h) |
| platePicUri | 车牌图片URI |
| carPicUri | 车辆图片URI |

---

## ⏰ 时间格式说明

**格式**：`yyyy-MM-ddTHH:mm:ss.SSS+08:00`

**示例**：
- `2025-01-28T00:00:00.000+08:00` - 今天凌晨0点
- `2025-01-28T23:59:59.000+08:00` - 今天晚上23:59:59
- `2025-01-28T14:30:00.000+08:00` - 今天下午2点30分

**快速生成时间的方法**：

### Java代码
```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
String beginTime = LocalDateTime.now().minusHours(1).format(formatter) + "+08:00";
String endTime = LocalDateTime.now().format(formatter) + "+08:00";
```

### JavaScript代码
```javascript
function formatTime(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  const ms = String(date.getMilliseconds()).padStart(3, '0');
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}.${ms}+08:00`;
}

const endTime = formatTime(new Date());
const beginTime = formatTime(new Date(Date.now() - 3600000)); // 1小时前
```

---

## 🔧 Postman 测试配置

### 1. 创建请求
- Method: `POST`
- URL: `http://localhost:8675/parking/hikvision/traffic/violations/search`
- Headers: `Content-Type: application/json`

### 2. Body（选择 raw - JSON）
```json
{
  "pageSize": 20,
  "pageNo": 1,
  "beginTime": "2025-01-28T00:00:00.000+08:00",
  "endTime": "2025-01-28T23:59:59.000+08:00"
}
```

### 3. 点击 Send

---

## ❓ 常见问题

### Q1: 时间格式错误怎么办？
**A**: 确保格式为 `yyyy-MM-ddTHH:mm:ss.SSS+08:00`，注意：
- 日期和时间之间用 `T` 分隔
- 毫秒部分是3位数 `.000`
- 时区固定为 `+08:00`

### Q2: 查询不到数据？
**A**: 检查：
1. 时间范围是否正确
2. 该时间段内是否真的有违章记录
3. 服务器是否能连接到海康服务器（10.100.110.82:443）

### Q3: pageSize 设置多少合适？
**A**: 
- 测试：20-50条
- 正常使用：50-100条
- 批量查询：100-500条
- 最大值：1000条

---

## 📞 技术支持

如有问题，请查看详细文档：
- [完整API文档](./HIKVISION_TRAFFIC_VIOLATION_GUIDE.md)
- [海康威视人员查询](./HIKVISION_INTEGRATION_GUIDE.md)

---

**更新日期**：2025-01-28  
**版本**：V1.0
