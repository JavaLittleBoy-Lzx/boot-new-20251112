-- =====================================================
-- 预约记录与进出场数据整合 - 数据库字段扩展
-- 功能：在 visitor_reservation_sync 表中添加人员进出场和车辆进出场相关字段
-- 创建时间：2025-01-XX
-- =====================================================

-- 检查字段是否已存在，如果不存在则添加
-- 使用 IF NOT EXISTS 语法（MySQL 5.7+）

-- 1. 添加人员进出状态字段
ALTER TABLE visitor_reservation_sync 
ADD COLUMN IF NOT EXISTS person_visit_status VARCHAR(50) 
    COMMENT '人员来访状态：人未来访/人已进场/人已离场/来访中' 
    DEFAULT NULL
    AFTER deleted;

-- 2. 添加人员进出时间字段
ALTER TABLE visitor_reservation_sync 
ADD COLUMN IF NOT EXISTS person_visit_times TEXT 
    COMMENT '人员进出场时间记录，JSON格式，格式：[{"enterTime":"2025-01-15 10:30:00","leaveTime":"2025-01-15 12:00:00"},...]' 
    DEFAULT NULL
    AFTER person_visit_status;

-- 3. 添加车辆进出状态字段
ALTER TABLE visitor_reservation_sync 
ADD COLUMN IF NOT EXISTS car_visit_status VARCHAR(50) 
    COMMENT '车辆来访状态：车未来访/已进场/已离场' 
    DEFAULT NULL
    AFTER person_visit_times;

-- 4. 添加车辆进出时间字段
ALTER TABLE visitor_reservation_sync 
ADD COLUMN IF NOT EXISTS car_visit_times TEXT 
    COMMENT '车辆进出场时间记录，JSON格式，格式：[{"enterTime":"2025-01-15 10:25:00","leaveTime":"2025-01-15 12:05:00"},...]' 
    DEFAULT NULL
    AFTER car_visit_status;

-- =====================================================
-- 如果 MySQL 版本不支持 IF NOT EXISTS，可以使用以下方式：
-- =====================================================

-- 方式1：先检查再添加（适用于存储过程或脚本）
-- SET @dbname = DATABASE();
-- SET @tablename = 'visitor_reservation_sync';
-- SET @columnname = 'person_visit_status';
-- SET @preparedStatement = (SELECT IF(
--   (
--     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE
--       (table_name = @tablename)
--       AND (table_schema = @dbname)
--       AND (column_name = @columnname)
--   ) > 0,
--   'SELECT 1',
--   CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(50) COMMENT ''人员来访状态：人未来访/人已进场/人已离场/来访中'' DEFAULT NULL AFTER deleted')
-- ));
-- PREPARE alterIfNotExists FROM @preparedStatement;
-- EXECUTE alterIfNotExists;
-- DEALLOCATE PREPARE alterIfNotExists;

-- =====================================================
-- 方式2：直接执行（如果确定字段不存在）
-- =====================================================

-- ALTER TABLE visitor_reservation_sync 
-- ADD COLUMN person_visit_status VARCHAR(50) 
--     COMMENT '人员来访状态：人未来访/人已进场/人已离场/来访中' 
--     DEFAULT NULL
--     AFTER deleted,
-- ADD COLUMN person_visit_times TEXT 
--     COMMENT '人员进出场时间记录，JSON格式' 
--     DEFAULT NULL
--     AFTER person_visit_status,
-- ADD COLUMN car_visit_status VARCHAR(50) 
--     COMMENT '车辆来访状态：车未来访/已进场/已离场' 
--     DEFAULT NULL
--     AFTER person_visit_times,
-- ADD COLUMN car_visit_times TEXT 
--     COMMENT '车辆进出场时间记录，JSON格式' 
--     DEFAULT NULL
--     AFTER car_visit_status;

-- =====================================================
-- 验证字段是否添加成功
-- =====================================================

-- SELECT 
--     COLUMN_NAME,
--     DATA_TYPE,
--     CHARACTER_MAXIMUM_LENGTH,
--     COLUMN_COMMENT,
--     IS_NULLABLE,
--     COLUMN_DEFAULT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'visitor_reservation_sync'
--   AND COLUMN_NAME IN ('person_visit_status', 'person_visit_times', 'car_visit_status', 'car_visit_times')
-- ORDER BY ORDINAL_POSITION;

-- =====================================================
-- 回滚脚本（如果需要删除字段）
-- =====================================================

-- ALTER TABLE visitor_reservation_sync 
-- DROP COLUMN IF EXISTS person_visit_status,
-- DROP COLUMN IF EXISTS person_visit_times,
-- DROP COLUMN IF EXISTS car_visit_status,
-- DROP COLUMN IF EXISTS car_visit_times;

