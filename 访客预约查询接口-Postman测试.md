# 访客预约查询接口 - Postman 测试指南

## 测试前准备

1. **确认后端服务已启动**
   - 默认地址：`http://localhost:8080`
   - 如果端口不同，请替换为实际端口

2. **准备测试数据**
   - 需要一个已存在的车牌号，例如：`京A12345`
   - 可以先通过定时任务同步一些数据到表中

---

## 接口1：根据车牌号分页查询预约记录

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-by-car-number`

### 请求参数 (Params)
```
carNumber: 京A12345        (必填)
pageNum: 1                 (可选，默认1)
pageSize: 10               (可选，默认10)
```

### Postman 配置步骤

1. **创建新请求**
   - 点击 "New" → "Request"
   - 名称：`查询车牌号预约记录（分页）`
   - 保存到集合

2. **设置请求方法**
   - 选择 `GET`

3. **设置请求URL**
   ```
   http://localhost:8080/parking/visitor-reservation-sync/query-by-car-number
   ```

4. **设置查询参数 (Params 标签页)**
   | KEY | VALUE | DESCRIPTION |
   |-----|-------|-------------|
   | carNumber | 京A12345 | 车牌号（必填） |
   | pageNum | 1 | 页码（可选） |
   | pageSize | 10 | 每页数量（可选） |

5. **发送请求**
   - 点击 `Send` 按钮

### 预期响应示例
```json
{
    "code": "0",
    "msg": "成功",
    "data": {
        "records": [
            {
                "id": 1,
                "reservationId": "R20251216001",
                "carNumber": "京A12345",
                "visitorName": "张三",
                "visitorPhone": "13800138000",
                "visitorIdCard": "110101199001011234",
                "startTime": "2025-12-16 08:00:00",
                "endTime": "2025-12-16 18:00:00",
                "vipTypeName": "访客通行证",
                "passDep": "技术部",
                "passName": "李四",
                "applyStateName": "已审核",
                "personVisitStatus": "人未来访",
                "carVisitStatus": "车未来访",
                "createTime": "2025-12-16 07:30:00"
            }
        ],
        "total": 5,
        "pageNum": 1,
        "pageSize": 10,
        "pages": 1
    }
}
```

---

## 接口2：查询车牌号最新一条预约

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-latest-by-car-number`

### 请求参数 (Params)
```
carNumber: 京A12345        (必填)
```

### Postman 配置步骤

1. **创建新请求**
   - 名称：`查询车牌号最新预约`

2. **设置请求URL**
   ```
   http://localhost:8080/parking/visitor-reservation-sync/query-latest-by-car-number
   ```

3. **设置查询参数**
   | KEY | VALUE |
   |-----|-------|
   | carNumber | 京A12345 |

4. **发送请求**

### 预期响应示例
```json
{
    "code": "0",
    "msg": "成功",
    "data": {
        "id": 1,
        "reservationId": "R20251216001",
        "carNumber": "京A12345",
        "visitorName": "张三",
        "visitorPhone": "13800138000",
        "startTime": "2025-12-16 08:00:00",
        "endTime": "2025-12-16 18:00:00",
        "vipTypeName": "访客通行证",
        "createTime": "2025-12-16 07:30:00"
    }
}
```

---

## 接口3：查询车牌号当前有效预约 ⭐推荐

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-valid-by-car-number`

### 请求参数 (Params)
```
carNumber: 京A12345        (必填)
```

### Postman 配置步骤

1. **创建新请求**
   - 名称：`查询车牌号有效预约`

2. **设置请求URL**
   ```
   http://localhost:8080/parking/visitor-reservation-sync/query-valid-by-car-number
   ```

3. **设置查询参数**
   | KEY | VALUE |
   |-----|-------|
   | carNumber | 京A12345 |

4. **发送请求**

### 功能说明
此接口会查询当前时间在 `start_time` 和 `end_time` 之间的有效预约记录。

### 预期响应示例
```json
{
    "code": "0",
    "msg": "成功",
    "data": {
        "records": [
            {
                "id": 1,
                "reservationId": "R20251216001",
                "carNumber": "京A12345",
                "visitorName": "张三",
                "startTime": "2025-12-16 08:00:00",
                "endTime": "2025-12-16 18:00:00",
                "vipTypeName": "访客通行证"
            }
        ],
        "total": 1
    }
}
```

---

## 接口4：根据预约ID查询详情

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-by-id`

### 请求参数 (Params)
```
reservationId: R20251216001    (必填)
```

### Postman 配置
```
GET http://localhost:8080/parking/visitor-reservation-sync/query-by-id?reservationId=R20251216001
```

---

## 接口5：根据访客姓名查询

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-by-visitor-name`

### 请求参数 (Params)
```
visitorName: 张三           (必填)
pageNum: 1                 (可选)
pageSize: 10               (可选)
```

### Postman 配置
```
GET http://localhost:8080/parking/visitor-reservation-sync/query-by-visitor-name?visitorName=张三&pageNum=1&pageSize=10
```

---

## 接口6：分页查询所有预约

### 基本信息
- **请求方法**：`GET`
- **请求URL**：`http://localhost:8080/parking/visitor-reservation-sync/query-all`

### 请求参数 (Params)
```
pageNum: 1                     (可选)
pageSize: 10                   (可选)
startTime: 2025-12-01 00:00:00 (可选)
endTime: 2025-12-31 23:59:59   (可选)
```

### Postman 配置
```
GET http://localhost:8080/parking/visitor-reservation-sync/query-all?pageNum=1&pageSize=10
```

---

## 快速导入 Postman Collection

复制以下 JSON 内容，在 Postman 中选择 `Import` → `Raw text` 粘贴：

```json
{
    "info": {
        "name": "访客预约查询接口",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "根据车牌号分页查询",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-by-car-number?carNumber=京A12345&pageNum=1&pageSize=10",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-by-car-number"],
                    "query": [
                        {"key": "carNumber", "value": "京A12345"},
                        {"key": "pageNum", "value": "1"},
                        {"key": "pageSize", "value": "10"}
                    ]
                }
            }
        },
        {
            "name": "查询车牌号最新预约",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-latest-by-car-number?carNumber=京A12345",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-latest-by-car-number"],
                    "query": [
                        {"key": "carNumber", "value": "京A12345"}
                    ]
                }
            }
        },
        {
            "name": "查询车牌号有效预约",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-valid-by-car-number?carNumber=京A12345",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-valid-by-car-number"],
                    "query": [
                        {"key": "carNumber", "value": "京A12345"}
                    ]
                }
            }
        },
        {
            "name": "根据预约ID查询",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-by-id?reservationId=R20251216001",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-by-id"],
                    "query": [
                        {"key": "reservationId", "value": "R20251216001"}
                    ]
                }
            }
        },
        {
            "name": "根据访客姓名查询",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-by-visitor-name?visitorName=张三&pageNum=1&pageSize=10",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-by-visitor-name"],
                    "query": [
                        {"key": "visitorName", "value": "张三"},
                        {"key": "pageNum", "value": "1"},
                        {"key": "pageSize", "value": "10"}
                    ]
                }
            }
        },
        {
            "name": "分页查询所有预约",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "http://localhost:8080/parking/visitor-reservation-sync/query-all?pageNum=1&pageSize=10",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["parking", "visitor-reservation-sync", "query-all"],
                    "query": [
                        {"key": "pageNum", "value": "1"},
                        {"key": "pageSize", "value": "10"},
                        {"key": "startTime", "value": "2025-12-01 00:00:00", "disabled": true},
                        {"key": "endTime", "value": "2025-12-31 23:59:59", "disabled": true}
                    ]
                }
            }
        }
    ]
}
```

---

## 测试技巧

### 1. 设置环境变量
在 Postman 中创建环境变量，方便切换不同环境：
- `base_url`: `http://localhost:8080`
- `car_number`: `京A12345`

使用方式：`{{base_url}}/parking/visitor-reservation-sync/query-by-car-number?carNumber={{car_number}}`

### 2. 测试脚本 (Tests 标签页)
在 Postman 的 Tests 标签页添加以下脚本，自动验证响应：

```javascript
// 验证状态码
pm.test("状态码为200", function () {
    pm.response.to.have.status(200);
});

// 验证响应格式
pm.test("响应包含code和msg字段", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData).to.have.property('msg');
});

// 验证成功响应
pm.test("业务响应成功", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("0");
});

// 验证data字段
pm.test("包含data数据", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('data');
});
```

### 3. 常见错误排查

**错误1：连接被拒绝**
```
Error: connect ECONNREFUSED 127.0.0.1:8080
```
解决：检查后端服务是否启动

**错误2：404 Not Found**
```
{
    "timestamp": "2025-12-16T09:00:00.000+00:00",
    "status": 404,
    "error": "Not Found"
}
```
解决：检查URL路径是否正确，确认Controller是否被扫描到

**错误3：车牌号为空**
```json
{
    "code": "1",
    "msg": "车牌号不能为空"
}
```
解决：检查参数名是否为 `carNumber`，注意大小写

---

## 测试数据准备

如果数据库中没有测试数据，可以通过以下方式准备：

1. **等待定时任务同步**
   - 定时任务每10秒会从外部接口同步数据
   - 查看后端日志确认同步成功

2. **手动插入测试数据**（可选）
   ```sql
   INSERT INTO visitor_reservation_sync 
   (reservation_id, car_number, visitor_name, visitor_phone, 
    start_time, end_time, vip_type_name, create_time, deleted)
   VALUES 
   ('TEST001', '京A12345', '测试访客', '13800138000',
    NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), '访客通行证', NOW(), 0);
   ```

---

## 完整测试流程示例

1. **启动后端服务**
2. **打开Postman**
3. **导入上面的Collection JSON**
4. **修改车牌号参数为实际存在的车牌**
5. **依次测试6个接口**
6. **检查响应数据是否正确**

测试完成后，你就可以在前端调用这些接口了！
