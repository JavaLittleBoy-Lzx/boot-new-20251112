# 东北林业大学智慧车行数据可视化平台 - API接口文档

> **版本**：V1.0 | **更新**：2025-11-17 | **基础URL**：http://www.xuerparking.cn:8675

---

## 📌 接口概览

### 控制器列表

| 控制器 | 路径前缀 | 接口数 | 说明 |
|-------|---------|-------|------|
| AcmsVipController | /parking/acms/vip | 15+ | ACMS数据接收、VIP查询 |
| VisitorVipAutoController | /api/visitor-vip-auto | 8+ | VIP自动开通管理 |
| VehicleFlowAnalysisController | /api/vehicle-flow | 10+ | 车流分析统计 |
| VehicleReservationController | /api/vehicle-reservation | 6+ | 车辆预约 |
| VisitorVipAnalysisController | /api/visitor-vip-analysis | 8+ | 访客分析 |
| ActivityLogController | /api/activity-log | 5+ | 活动日志 |
| FileUploadController | /api/upload | 3+ | 文件上传 |

---

## 一、ACMS VIP接口

### 1.1 接收ACMS推送数据

**接口地址**：`POST /parking/acms/vip/eventRcv`

**功能说明**：接收ACMS系统主动推送的人员进出场事件数据

**请求头**：
```
Content-Type: application/json
```

**请求体**（新格式）：
```json
{
  "method": "OnEventNotify",
  "params": {
    "ability": "event_acs",
    "events": [
      {
        "eventType": 197162,
        "eventId": "event-001",
        "happenTime": "2025-01-15T10:30:00.000+08:00",
        "srcName": "图书馆1",
        "data": {
          "ExtEventPersonNo": "person-id-001",
          "ExtEventIdentityCardInfo": {
            "Name": "张三",
            "IdNum": "110101199001011234",
            "Sex": 1,
            "Address": "北京市朝阳区"
          }
        }
      }
    ]
  }
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "批量处理完成",
  "data": {
    "totalCount": 1,
    "successCount": 1,
    "skipCount": 0,
    "failCount": 0,
    "processResults": [
      {
        "eventId": "event-001",
        "eventType": 197162,
        "status": "success",
        "personInfo": {
          "personName": "张三",
          "idCard": "110101199001011234",
          "gender": "男"
        }
      }
    ]
  }
}
```

### 1.2 查询车主信息

**接口地址**：`POST /parking/acms/vip/owner-info`

**请求体**：
```json
{
  "plateNumber": "京A12345",
  "parkName": "东北林业大学"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "ownerName": "张三",
    "ownerPhone": "13800138000",
    "ownerAddress": "教职工住宅区",
    "plateNumber": "京A12345",
    "parkName": "东北林业大学"
  }
}
```

### 1.3 查询VIP票信息

**接口地址**：`POST /parking/acms/vip/vip-ticket-info`

**请求体**：
```json
{
  "plateNumber": "京A12345",
  "parkName": "东北林业大学"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "vipTypeName": "教职工月票",
    "ownerName": "张三",
    "ownerPhone": "13800138000",
    "plateNumber": "京A12345",
    "parkName": "东北林业大学"
  }
}
```

### 1.4 融合查询VIP和车主信息

**接口地址**：`POST /parking/acms/vip/merged-info`

**请求体**：
```json
{
  "plateNumber": "京A12345",
  "parkName": "东北林业大学"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "plateNumber": "京A12345",
    "parkName": "东北林业大学",
    "vipTypeName": "教职工月票",
    "ownerName": "张三",
    "ownerPhone": "13800138000",
    "ownerAddress": "教职工住宅区",
    "ownerCategory": "教职工",
    "customerCompany": "林学院",
    "customerRoomNumber": "A301"
  }
}
```

### 1.5 获取黑名单类型列表

**接口地址**：`POST /parking/acms/vip/blacklist-types`

或

**接口地址**：`GET /parking/acms/vip/blacklist-types?parkName=东北林业大学`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "blacklistTypes": [
      {
        "typeId": "1",
        "typeName": "违章车辆",
        "description": "多次违章停车"
      },
      {
        "typeId": "2",
        "typeName": "欠费车辆",
        "description": "长期欠费未缴"
      }
    ],
    "isDefault": false,
    "count": 2
  }
}
```

### 1.6 查询海康威视人员信息

**接口地址**：`POST /parking/acms/vip/hikvision/person-info`

**请求体**：
```json
{
  "personIds": "32fb3b91-f823-42b6-8fca-137bff553857"
}
```

或多个ID：
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
        "orgPathName": "林学院",
        "certificateType": 111,
        "certificateNo": "110101199001011234",
        "phoneNo": "13800138000",
        "jobNo": "T001",
        "createTime": "2025-01-01T00:00:00.000+08:00",
        "updateTime": "2025-01-15T10:00:00.000+08:00"
      }
    ]
  }
}
```

---

## 二、访客VIP自动化接口

### 2.1 获取服务状态

**接口地址**：`GET /api/visitor-vip-auto/status`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "enabled": true,
    "mode": "dynamic",
    "currentInterval": 2000,
    "lastSyncTime": "2025-01-15 10:30:00",
    "totalSynced": 1250,
    "circuitBreakerOpen": false
  }
}
```

### 2.2 启用服务

**接口地址**：`POST /api/visitor-vip-auto/enable`

**响应示例**：
```json
{
  "code": 0,
  "msg": "服务已启用",
  "data": null
}
```

### 2.3 禁用服务

**接口地址**：`POST /api/visitor-vip-auto/disable`

**响应示例**：
```json
{
  "code": 0,
  "msg": "服务已禁用",
  "data": null
}
```

### 2.4 手动触发同步

**接口地址**：`POST /api/visitor-vip-auto/trigger-sync`

**响应示例**：
```json
{
  "code": 0,
  "msg": "同步任务已触发",
  "data": {
    "syncCount": 50,
    "duration": 1250
  }
}
```

---

## 三、车辆预约接口

### 3.1 创建预约

**接口地址**：`POST /api/vehicle-reservation`

**请求体**：
```json
{
  "visitorName": "李四",
  "visitorPhone": "13900139000",
  "carNumber": "京B88888",
  "visitDate": "2025-01-20",
  "visitPurpose": "学术交流"
}
```

**响应示例**：
```json
{
  "code": 0,
  "msg": "预约创建成功",
  "data": {
    "reservationId": 12345,
    "reservationNo": "RES20250115001"
  }
}
```

### 3.2 查询预约列表

**接口地址**：`GET /api/vehicle-reservation/list`

**请求参数**：
- `pageNo`：页码（默认1）
- `pageSize`：每页数量（默认10）
- `status`：预约状态（可选）
- `startDate`：开始日期（可选）
- `endDate`：结束日期（可选）

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 100,
    "pageNo": 1,
    "pageSize": 10,
    "records": [
      {
        "reservationId": 12345,
        "reservationNo": "RES20250115001",
        "visitorName": "李四",
        "carNumber": "京B88888",
        "status": "待来访",
        "createTime": "2025-01-15 09:00:00"
      }
    ]
  }
}
```

### 3.3 查询预约详情

**接口地址**：`GET /api/vehicle-reservation/{id}`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "reservationId": 12345,
    "reservationNo": "RES20250115001",
    "visitorName": "李四",
    "visitorPhone": "13900139000",
    "carNumber": "京B88888",
    "visitDate": "2025-01-20",
    "visitPurpose": "学术交流",
    "status": "待来访",
    "personVisitStatus": "人未来访",
    "carVisitStatus": "车未来访",
    "personVisitTimes": [],
    "carVisitTimes": [],
    "createTime": "2025-01-15 09:00:00"
  }
}
```

---

## 四、车流分析接口

### 4.1 实时车流统计

**接口地址**：`GET /api/vehicle-flow/realtime`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "currentInPark": 285,
    "todayIn": 1520,
    "todayOut": 1235,
    "averageStayTime": 45
  }
}
```

### 4.2 车流趋势分析

**接口地址**：`GET /api/vehicle-flow/trend`

**请求参数**：
- `startDate`：开始日期（YYYY-MM-DD）
- `endDate`：结束日期（YYYY-MM-DD）
- `granularity`：粒度（hour/day/week/month）

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "trend": [
      {
        "time": "2025-01-15 08:00",
        "inCount": 120,
        "outCount": 50
      },
      {
        "time": "2025-01-15 09:00",
        "inCount": 180,
        "outCount": 90
      }
    ]
  }
}
```

### 4.3 高峰时段分析

**接口地址**：`GET /api/vehicle-flow/peak-hours`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "peakHours": [
      {
        "hour": 8,
        "avgCount": 200,
        "label": "上班高峰"
      },
      {
        "hour": 17,
        "avgCount": 180,
        "label": "下班高峰"
      }
    ]
  }
}
```

---

## 五、访客分析接口

### 5.1 访客统计

**接口地址**：`GET /api/visitor-vip-analysis/statistics`

**请求参数**：
- `startDate`：开始日期
- `endDate`：结束日期

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalVisitors": 350,
    "vipVisitors": 120,
    "regularVisitors": 230,
    "avgVisitDuration": 65
  }
}
```

### 5.2 来访频次统计

**接口地址**：`GET /api/visitor-vip-analysis/frequency`

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "frequency": [
      {
        "visitorName": "李四",
        "visitCount": 15,
        "lastVisitTime": "2025-01-15 14:00:00"
      }
    ]
  }
}
```

---

## 六、活动日志接口

### 6.1 查询日志列表

**接口地址**：`GET /api/activity-log/list`

**请求参数**：
- `pageNo`：页码
- `pageSize`：每页数量
- `userId`：用户ID（可选）
- `action`：操作类型（可选）
- `startDate`：开始日期（可选）
- `endDate`：结束日期（可选）

**响应示例**：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 500,
    "records": [
      {
        "id": 1001,
        "userId": "admin",
        "userName": "管理员",
        "action": "LOGIN",
        "description": "用户登录",
        "ipAddress": "192.168.1.100",
        "createTime": "2025-01-15 09:00:00"
      }
    ]
  }
}
```

---

## 七、文件上传接口

### 7.1 上传文件

**接口地址**：`POST /api/upload/file`

**请求方式**：multipart/form-data

**请求参数**：
- `file`：文件（必填）
- `type`：文件类型（可选）

**响应示例**：
```json
{
  "code": 0,
  "msg": "上传成功",
  "data": {
    "fileId": "file-20250115-001",
    "fileName": "document.pdf",
    "fileUrl": "/uploads/2025/01/15/document.pdf",
    "fileSize": 1024000
  }
}
```

---

## 八、统一响应格式

### 8.1 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

### 8.2 失败响应

```json
{
  "code": 1,
  "msg": "错误信息描述",
  "data": null
}
```

### 8.3 常见错误码

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| 1 | 业务错误 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 九、Swagger文档

**访问地址**：http://www.xuerparking.cn:8675/doc.html

**Knife4j增强文档**，提供：
- 接口在线测试
- 参数自动填充
- 响应示例展示
- 接口调试工具

---

**相关文档**：
- [01-项目概述与架构](./01-项目概述与架构.md)
- [02-核心功能模块详解](./02-核心功能模块详解.md)
- [04-配置与部署指南](./04-配置与部署指南.md)
