# 海康威视园区卡口违章事件查询接口对接指南

## 概述

本文档说明如何使用新增的海康威视园区卡口违章事件查询功能。系统已集成海康威视开放平台的违章事件查询接口，支持根据车牌号、违章类型、时间范围等条件查询违章事件。

## 功能特性

1. **多条件查询**：支持车牌号、违章类型、测速类型、时间范围等多种查询条件
2. **分页查询**：支持分页查询，每页最多1000条记录
3. **违章类型**：支持超速、逆行、黑名单、违停等多种违章类型
4. **时间范围**：支持按过车时间或入库时间查询
5. **完整信息**：返回包括车牌号、违章类型、车速、照片URI等完整违章数据

## 服务器配置

### 违章查询服务器（新增）
- **服务器地址**：`https://10.100.110.82:443`
- **合作方Key**：`28227294`
- **合作方Secret**：`SD5VWH0LUuVHRxAjdAhQ`

### 人员查询服务器（现有）
- **服务器地址**：`https://10.100.111.5:443`
- **合作方Key**：`22668058`
- **合作方Secret**：`T09WZsuZyne1guzhZ4Gc`

## 配置说明

配置文件 `application.yml` 已更新：

```yaml
hikvision:
  # 人员查询服务器（现有）
  api:
    base-url: https://10.100.111.5:443
    app-key: 22668058
    app-secret: T09WZsuZyne1guzhZ4Gc
    timeout: 30000
  
  # 违章查询服务器（新增）
  traffic:
    base-url: https://10.100.110.82:443
    app-key: 28227294
    app-secret: SD5VWH0LUuVHRxAjdAhQ
    timeout: 30000
```

## API 接口说明

### 1. 查询违章事件（完整接口）

**端点**：`POST /parking/hikvision/traffic/violations/search`

**功能**：根据多种条件查询违章事件

**请求体**：
```json
{
  "pageSize": 20,
  "pageNo": 1,
  "plateNo": "浙A12345",
  "speedType": "-1",
  "illegalType": "1",
  "monitoringId": "-1",
  "beginTime": "2019-07-26T15:00:00.000+08:00",
  "endTime": "2019-07-26T17:00:00.000+08:00",
  "alarmReason": "-1",
  "eventId": ""
}
```

**请求参数说明**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageSize | Number | 是 | 每页记录数，范围 (0, 1000] |
| pageNo | Number | 是 | 目标页码，范围 (0, ~) |
| plateNo | String | 否 | 车牌号，支持模糊查询 |
| speedType | String | 否 | 测速类型：-1-全部，1-点位测速，2-区间测速 |
| illegalType | String | 否 | 违章类型：-1-全部，1-超速，2-逆行，3-黑名单，5-违停 |
| monitoringId | String | 否 | 卡口点id，不传代表查询全部 |
| beginTime | String | 否 | 过车开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00） |
| endTime | String | 否 | 过车结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00） |
| createBeginTime | String | 否 | 入库开始时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00） |
| createEndTime | String | 否 | 入库结束时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00） |
| alarmReason | String | 否 | 布防原因：-1-全部，1-被盗车，2-被抢车，3-嫌疑车，4-交通违法车，5-紧急查控车 |
| eventId | String | 否 | 事件的唯一编号 |

**注意**：
- 【过车开始、结束时间】和【入库开始时间、结束时间】只能配套使用，不能同时传值

**响应示例**：
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
        "crossTime": "2019-07-28T15:00:00.000+08:00",
        "eventId": "cc2c881339ed4222839cb34a974655fd",
        "plateNo": "浙59SVN",
        "illegalType": 1,
        "speedType": 1,
        "monitoringId": "24522978916a4f0aa432508f01a12223",
        "monitoringName": "Channel-007",
        "platePicUri": "/pic?=d7ei703i10cd*73a-d5108a--22cd0c9d6592aiid=",
        "carPicUri": "/pic?=d7ei703i10cd*73a-d5108a--22cd0c9d6592aiid=",
        "aswSyscode": "h4h45y13ty23hg24h",
        "reason": 6,
        "uuid": "642C8DC7-259E-4276-9FD8-A70652468B1C",
        "pointIntervalName": "点位6000",
        "speed": 100
      }
    ]
  }
}
```

### 2. 根据车牌号查询违章（简化接口）

**端点**：`GET /parking/hikvision/traffic/violations/by-plate`

**功能**：根据车牌号快速查询违章事件

**请求参数**：
- `plateNo`（必填）：车牌号
- `pageNo`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页大小

**示例**：
```
GET /parking/hikvision/traffic/violations/by-plate?plateNo=浙A12345&pageNo=1&pageSize=20
```

### 3. 查询最近违章事件（简化接口）

**端点**：`GET /parking/hikvision/traffic/violations/recent`

**功能**：查询最近N天的违章事件

**请求参数**：
- `days`（可选，默认7）：查询最近几天
- `pageNo`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页大小

**示例**：
```
GET /parking/hikvision/traffic/violations/recent?days=7&pageNo=1&pageSize=20
```

### 4. 根据违章类型查询（简化接口）

**端点**：`GET /parking/hikvision/traffic/violations/by-type`

**功能**：根据违章类型查询违章事件

**请求参数**：
- `illegalType`（必填）：违章类型（1-超速，2-逆行，3-黑名单，5-违停）
- `pageNo`（可选，默认1）：页码
- `pageSize`（可选，默认20）：每页大小

**示例**：
```
GET /parking/hikvision/traffic/violations/by-type?illegalType=1&pageNo=1&pageSize=20
```

## 数据字段说明

### 违章事件字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| crossTime | String | 过车时间（格式：yyyy-MM-ddTHH:mm:ss.sss+08:00） |
| eventId | String | 事件的唯一标识 |
| plateNo | String | 车牌号 |
| illegalType | Integer | 违章类型：1-超速，2-逆行，3-黑名单，5-违停 |
| speedType | Integer | 测速类型：1-点位测速，2-区间测速 |
| monitoringId | String | 事件源(卡口点的编号) |
| monitoringName | String | 事件源(卡口点的名称) |
| platePicUri | String | 车牌图片uri |
| carPicUri | String | 车辆图片uri |
| aswSyscode | String | 图片服务的唯一标识 |
| reason | Integer | 布防原因(只有黑名单事件才会有值)：1-被盗车，2-被抢车，3-嫌疑车，4-交通违法车，5-紧急查控车 |
| uuid | String | 事件唯一标识 |
| pointIntervalName | String | 区间/点位测速名称 |
| speed | Integer | 车速(单位km/h)，大于0 |

## 违章类型说明

| 类型值 | 说明 |
|--------|------|
| -1 | 全部类型 |
| 1 | 超速 |
| 2 | 逆行 |
| 3 | 黑名单 |
| 5 | 违停 |

## 测速类型说明

| 类型值 | 说明 |
|--------|------|
| -1 | 全部类型 |
| 1 | 点位测速 |
| 2 | 区间测速 |

## 布防原因说明

| 原因值 | 说明 |
|--------|------|
| -1 | 全部原因 |
| 1 | 被盗车 |
| 2 | 被抢车 |
| 3 | 嫌疑车 |
| 4 | 交通违法车 |
| 5 | 紧急查控车 |

## 错误处理

### 常见错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1 | 查询失败 |
| 400 | 参数错误 |
| 401 | 认证失败 |
| 500 | 服务器错误 |

### 错误响应示例

```json
{
  "code": 1,
  "msg": "分页大小必须在 (0, 1000] 范围内",
  "data": null
}
```

## 日志记录

系统会记录所有查询操作的日志，包括：

- 查询条件（车牌号、违章类型、时间范围等）
- 查询结果的成功/失败状态
- 返回的记录数量
- 异常信息

日志级别：
- `INFO`：正常操作日志
- `WARN`：警告信息（如参数错误、查询失败）
- `ERROR`：错误信息（如异常）

## 集成示例

### Java 调用示例

```java
// 注入服务
@Resource
private HikvisionTrafficViolationService hikvisionTrafficViolationService;

// 构建查询请求
HikvisionTrafficViolationService.ViolationEventRequest request = 
    new HikvisionTrafficViolationService.ViolationEventRequest();
request.setPageSize(20);
request.setPageNo(1);
request.setPlateNo("浙A12345");
request.setIllegalType("1"); // 超速
request.setBeginTime("2019-07-26T15:00:00.000+08:00");
request.setEndTime("2019-07-26T17:00:00.000+08:00");

// 查询违章事件
HikvisionTrafficViolationService.ViolationEventResponse response = 
    hikvisionTrafficViolationService.queryViolationEvents(request);

if ("0".equals(response.getCode())) {
    List<HikvisionTrafficViolationService.ViolationEvent> violations = 
        response.getData().getList();
    // 处理违章事件列表
}
```

### cURL 调用示例

```bash
# 完整查询接口
curl -X POST http://localhost:8675/parking/hikvision/traffic/violations/search \
  -H "Content-Type: application/json" \
  -d '{
    "pageSize": 20,
    "pageNo": 1,
    "plateNo": "浙A12345",
    "illegalType": "1",
    "beginTime": "2019-07-26T15:00:00.000+08:00",
    "endTime": "2019-07-26T17:00:00.000+08:00"
  }'

# 根据车牌号查询
curl -X GET "http://localhost:8675/parking/hikvision/traffic/violations/by-plate?plateNo=浙A12345&pageNo=1&pageSize=20"

# 查询最近7天违章
curl -X GET "http://localhost:8675/parking/hikvision/traffic/violations/recent?days=7&pageNo=1&pageSize=20"

# 根据违章类型查询（超速）
curl -X GET "http://localhost:8675/parking/hikvision/traffic/violations/by-type?illegalType=1&pageNo=1&pageSize=20"
```

## 性能优化建议

1. **合理设置分页大小**：建议每页20-100条记录，避免一次查询过多数据
2. **使用时间范围**：尽量指定时间范围，避免查询全部数据
3. **缓存查询结果**：对于频繁查询的数据，考虑使用缓存
4. **异步处理**：对于大量查询，考虑使用异步处理方式

## 故障排查

### 问题 1：无法连接到海康威视 API

**原因**：
- 网络连接问题
- API 地址错误
- 防火墙限制

**解决方案**：
- 检查网络连接
- 验证 `base-url` 配置（`https://10.100.110.82:443`）
- 检查防火墙设置

### 问题 2：认证失败

**原因**：
- App Key 或 App Secret 错误
- 凭证已过期

**解决方案**：
- 重新检查凭证（Key: 28227294, Secret: SD5VWH0LUuVHRxAjdAhQ）
- 在海康威视平台重新生成凭证

### 问题 3：查询返回空结果

**原因**：
- 查询条件不匹配
- 时间范围内没有违章记录
- 车牌号不存在

**解决方案**：
- 验证查询条件的正确性
- 扩大时间范围
- 检查车牌号是否正确

### 问题 4：分页参数错误

**原因**：
- pageSize 超出范围 (0, 1000]
- pageNo 小于等于0

**解决方案**：
- 确保 pageSize 在 1-1000 之间
- 确保 pageNo 大于0

## 相关文档

- [海康威视开放平台文档](https://open.hikvision.com/docs/)
- [海康威视人员查询接口](./HIKVISION_INTEGRATION_GUIDE.md)
- [园区卡口错误码说明](https://open.hikvision.com/docs/)

## 支持

如有问题，请联系系统管理员。

---

**更新日期**：2025-01-28  
**版本**：V1.0
