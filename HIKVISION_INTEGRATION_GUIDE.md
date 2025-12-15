# 海康威视人员信息查询接口对接指南

## 概述

本文档说明如何使用新增的海康威视人员信息查询功能。系统已集成海康威视开放平台的人员查询接口，支持在 ACMS 推送数据时自动查询关联的人员信息。

## 功能特性

1. **自动人员查询**：在接收 ACMS 推送数据时，自动提取 `ExtEventPersonNoj` 字段并查询人员信息
2. **独立查询接口**：提供独立的 HTTP 接口用于按需查询人员信息
3. **完整人员信息**：返回包括姓名、性别、证件信息、照片等完整人员数据
4. **错误处理**：完善的异常处理和日志记录

## 配置步骤

### 1. 获取海康威视 API 凭证

访问 [海康威视开放平台](https://open.hikvision.com)：
- 注册开发者账号
- 创建应用
- 获取 `App Key` 和 `App Secret`

### 2. 配置应用参数

编辑 `application-hikvision.yml` 文件：

```yaml
hikvision:
  api:
    base-url: https://open.hikvision.com
    app-key: your_app_key_here
    app-secret: your_app_secret_here
    timeout: 30000
```

### 3. 激活配置文件

在 `application.yml` 中添加：

```yaml
spring:
  profiles:
    include: hikvision
```

## API 接口说明

### 1. 接收 ACMS 推送数据（自动查询人员）

**端点**：`POST /parking/acms/vip/eventRcv`

**功能**：接收 ACMS 系统推送的事件数据，自动提取 `ExtEventPersonNoj` 字段并查询人员信息

**请求示例**：
```json
{
  "eventType": "person_event",
  "ExtEventPersonNoj": "32fb3b91-f823-42b6-8fca-137bff553857",
  "eventTime": "2025-01-14 10:30:00"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "receiveTime": "2025-01-14 10:30:00",
    "rawData": { ... },
    "personInfo": [
      {
        "personId": "32fb3b91-f823-42b6-8fca-137bff553857",
        "personName": "张三",
        "gender": 1,
        "phoneNo": "13800138000",
        "certificateType": 111,
        "certificateNo": "123253565464",
        "createTime": "2018-08-24T11:57:02.000+08:00",
        "updateTime": "2018-08-24T11:57:02.000+08:00"
      }
    ],
    "personQueryStatus": "success"
  }
}
```

### 2. 查询海康威视人员信息

**端点**：`POST /parking/acms/vip/hikvision/person-info`

**功能**：根据人员 ID 查询人员详细信息

**请求体**：
```json
{
  "personIds": "32fb3b91-f823-42b6-8fca-137bff553857"
}
```

多个 ID 用逗号分隔：
```json
{
  "personIds": "id1,id2,id3"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 1,
    "pageNo": 1,
    "pageSize": 1000,
    "personList": [
      {
        "personId": "32fb3b91-f823-42b6-8fca-137bff553857",
        "personName": "张三",
        "gender": 1,
        "orgPath": "@root0000008",
        "orgIndexCode": "ac7114be-c23a-49a2-96bf-52e2f1231165",
        "orgPathName": "2222",
        "certificateType": 111,
        "certificateNo": "123253565464",
        "cardNo": "12131",
        "plateNo": "京A12131",
        "createTime": "2018-08-24T11:57:02.000+08:00",
        "updateTime": "2018-08-24T11:57:02.000+08:00",
        "phoneNo": "13512424214",
        "jobNo": "111",
        "personPhoto": {
          "picId": "12574bdf-37f6-4b93-ac0c-e90b5d5b0626",
          "picUri": "/pic/8d2851f-..."
        }
      }
    ]
  }
}
```

## 数据字段说明

### 人员信息字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| personId | String | 人员ID |
| personName | String | 人员名称 |
| gender | Integer | 性别：1-男，2-女，0-未知 |
| orgPath | String | 所属组织目录 |
| orgIndexCode | String | 所属组织唯一标识 |
| orgPathName | String | 所属组织名称 |
| certificateType | Integer | 证件类型 |
| certificateNo | String | 证件号码 |
| cardNo | String | 卡号 |
| plateNo | String | 车牌号 |
| phoneNo | String | 联系电话 |
| jobNo | String | 工号 |
| createTime | String | 创建时间 |
| updateTime | String | 更新时间 |
| personPhoto | Object | 人员照片信息 |

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
  "msg": "查询失败: 人员ID不能为空",
  "data": null
}
```

## 日志记录

系统会记录所有查询操作的日志，包括：

- 接收推送数据的时间和内容
- 人员ID 的提取情况
- 查询结果的成功/失败状态
- 异常信息

日志级别：
- `INFO`：正常操作日志
- `WARN`：警告信息（如查询失败）
- `ERROR`：错误信息（如异常）
- `DEBUG`：调试信息（如格式化的 JSON 数据）

## 集成示例

### Java 调用示例

```java
// 注入服务
@Resource
private HikvisionPersonService hikvisionPersonService;

// 查询单个人员
HikvisionPersonService.PersonInfo personInfo = 
    hikvisionPersonService.queryPersonInfo("32fb3b91-f823-42b6-8fca-137bff553857");

// 查询多个人员
HikvisionPersonService.PersonListResponse response = 
    hikvisionPersonService.queryPersonList("id1,id2,id3");

if ("0".equals(response.getCode())) {
    List<HikvisionPersonService.PersonInfo> personList = response.getData();
    // 处理人员列表
}
```

### cURL 调用示例

```bash
# 查询人员信息
curl -X POST http://www.xuerparking.cn:8080/parking/acms/vip/hikvision/person-info \
  -H "Content-Type: application/json" \
  -d '{
    "personIds": "32fb3b91-f823-42b6-8fca-137bff553857"
  }'
```

## 性能优化建议

1. **批量查询**：尽量使用逗号分隔的多个 ID 进行批量查询，而不是多次单个查询
2. **缓存**：考虑缓存查询结果，避免重复查询相同的人员信息
3. **异步处理**：对于大量查询，考虑使用异步处理方式
4. **超时设置**：根据网络情况调整 `timeout` 参数

## 故障排查

### 问题 1：无法连接到海康威视 API

**原因**：
- 网络连接问题
- API 地址错误
- 防火墙限制

**解决方案**：
- 检查网络连接
- 验证 `base-url` 配置
- 检查防火墙设置

### 问题 2：认证失败

**原因**：
- App Key 或 App Secret 错误
- 凭证已过期

**解决方案**：
- 重新检查凭证
- 在海康威视平台重新生成凭证

### 问题 3：查询返回空结果

**原因**：
- 人员 ID 不存在
- 人员已被删除

**解决方案**：
- 验证人员 ID 的正确性
- 检查人员是否存在于海康威视系统中

## 相关文档

- [海康威视开放平台文档](https://open.hikvision.com/docs/docId?productId=5c67f1e2f05948198c909700&version=%2Ff95e951cefc54578b523d1738f65f0a1&tagPath=%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97)
- [ACMS 推送数据格式](./ACMS_PUSH_FORMAT.md)

## 支持

如有问题，请联系系统管理员。
