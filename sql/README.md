# 数据库迁移脚本说明

## 概述

这些SQL脚本用于在 `visitor_reservation_sync` 表中添加人员进出场和车辆进出场相关的字段，以支持预约记录与进出场数据整合功能。

## 文件说明

### 1. `add_visit_status_fields_simple.sql` ⭐ 推荐使用
- **用途**：添加4个新字段的简化版本
- **适用场景**：适用于所有MySQL版本
- **执行前**：请先执行 `check_visit_status_fields.sql` 确认字段不存在
- **说明**：如果字段已存在，执行会报错，需要先检查

### 2. `add_visit_status_fields.sql`
- **用途**：添加4个新字段的完整版本（包含IF NOT EXISTS语法）
- **适用场景**：MySQL 5.7+ 版本
- **说明**：如果MySQL版本不支持 IF NOT EXISTS，请使用简化版本

### 3. `check_visit_status_fields.sql`
- **用途**：检查字段是否已存在
- **执行时机**：在执行添加字段脚本之前
- **结果说明**：
  - 如果查询结果为空：字段不存在，可以执行添加脚本
  - 如果查询结果有4条记录：字段已存在，无需重复执行

### 4. `rollback_visit_status_fields.sql`
- **用途**：回滚脚本，删除新添加的字段
- **警告**：执行此脚本将删除所有进出场状态和时间数据，请谨慎操作！
- **使用场景**：需要回滚迁移时使用

## 新增字段说明

### 1. person_visit_status (VARCHAR(50))
- **说明**：人员来访状态
- **取值**：
  - `人未来访`：初始状态，人员尚未进入
  - `人已进场`：人员已进入
  - `人已离场`：人员已离开
  - `来访中`：人员在场内（有进场记录但无离场记录）

### 2. person_visit_times (TEXT)
- **说明**：人员进出场时间记录，JSON格式
- **格式示例**：
  ```json
  [
    {
      "enterTime": "2025-01-15 10:30:00",
      "leaveTime": "2025-01-15 12:00:00"
    },
    {
      "enterTime": "2025-01-15 14:00:00",
      "leaveTime": null
    }
  ]
  ```

### 3. car_visit_status (VARCHAR(50))
- **说明**：车辆来访状态
- **取值**：
  - `车未来访`：初始状态，车辆尚未进入
  - `已进场`：车辆已进入
  - `已离场`：车辆已离开

### 4. car_visit_times (TEXT)
- **说明**：车辆进出场时间记录，JSON格式
- **格式示例**：
  ```json
  [
    {
      "enterTime": "2025-01-15 10:25:00",
      "leaveTime": "2025-01-15 12:05:00"
    },
    {
      "enterTime": "2025-01-15 14:10:00",
      "leaveTime": null
    }
  ]
  ```

## 执行步骤

### 步骤1：检查字段是否存在
```sql
-- 执行 check_visit_status_fields.sql
-- 或者直接执行以下SQL
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'visitor_reservation_sync'
  AND COLUMN_NAME IN ('person_visit_status', 'person_visit_times', 'car_visit_status', 'car_visit_times');
```

### 步骤2：执行添加字段脚本
```bash
# 方式1：使用简化版本（推荐）
mysql -u用户名 -p数据库名 < add_visit_status_fields_simple.sql

# 方式2：使用完整版本（MySQL 5.7+）
mysql -u用户名 -p数据库名 < add_visit_status_fields.sql
```

### 步骤3：验证字段添加成功
```sql
-- 再次执行 check_visit_status_fields.sql
-- 应该能看到4条记录
```

## 注意事项

1. **备份数据**：执行任何数据库结构变更前，请先备份数据库
2. **测试环境**：建议先在测试环境执行，验证无误后再在生产环境执行
3. **执行时机**：建议在业务低峰期执行，避免影响业务
4. **字段位置**：新字段添加在 `deleted` 字段之后
5. **默认值**：所有新字段默认值为 `NULL`，允许为空

## 回滚操作

如果需要回滚迁移，执行以下脚本：

```bash
mysql -u用户名 -p数据库名 < rollback_visit_status_fields.sql
```

**警告**：回滚操作将删除所有进出场状态和时间数据，请确保已备份重要数据！

## 常见问题

### Q1: 执行时提示字段已存在？
**A**: 说明字段已经添加过了，无需重复执行。可以使用 `check_visit_status_fields.sql` 确认。

### Q2: MySQL版本不支持 IF NOT EXISTS？
**A**: 使用 `add_visit_status_fields_simple.sql` 简化版本，执行前先检查字段是否存在。

### Q3: 执行后如何验证？
**A**: 执行 `check_visit_status_fields.sql` 或使用以下SQL：
```sql
DESC visitor_reservation_sync;
```

### Q4: 字段添加后，旧数据如何处理？
**A**: 旧数据的4个新字段值默认为 `NULL`，系统会在后续同步或更新时自动填充。

## 技术支持

如有问题，请参考需求文档：`需求文档-预约记录与进出场数据整合.md`

