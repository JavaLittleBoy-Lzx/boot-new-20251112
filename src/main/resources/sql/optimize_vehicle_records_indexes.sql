-- ============================================================
-- 进出场记录查询性能优化 - 索引优化脚本
-- ============================================================
-- 说明：本脚本为进出场记录表创建索引，优化查询性能
-- 创建日期：2024-10-25
-- 版本：1.0.0
-- ============================================================

-- 检查索引是否存在的辅助查询（执行前可以先检查）
-- SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME 
-- FROM INFORMATION_SCHEMA.STATISTICS 
-- WHERE TABLE_SCHEMA = DATABASE() 
--   AND TABLE_NAME IN ('report_car_in', 'report_car_out')
-- ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================
-- 1. 进场记录表（report_car_in）索引优化
-- ============================================================

-- 索引1：按创建时间和删除标识查询（最重要的索引）
-- 用于: SELECT * FROM report_car_in WHERE create_time >= ? AND deleted = 0 ORDER BY create_time DESC LIMIT ?
DROP INDEX IF EXISTS idx_report_car_in_create_time ON report_car_in;
CREATE INDEX idx_report_car_in_create_time 
ON report_car_in(create_time DESC, deleted);

-- 索引2：按车牌号和进场时间查询
-- 用于: 查询特定车辆的进场记录
DROP INDEX IF EXISTS idx_report_car_in_plate_time ON report_car_in;
CREATE INDEX idx_report_car_in_plate_time 
ON report_car_in(car_license_number, enter_time);

-- 索引3：按删除标识查询（辅助索引）
-- 用于: WHERE deleted = 0 的查询条件
DROP INDEX IF EXISTS idx_report_car_in_deleted ON report_car_in;
CREATE INDEX idx_report_car_in_deleted 
ON report_car_in(deleted);

-- 索引4：按进场时间查询
-- 用于: 按时间范围查询
DROP INDEX IF EXISTS idx_report_car_in_enter_time ON report_car_in;
CREATE INDEX idx_report_car_in_enter_time 
ON report_car_in(enter_time DESC);

-- ============================================================
-- 2. 离场记录表（report_car_out）索引优化
-- ============================================================

-- 索引1：按创建时间和删除标识查询（最重要的索引）
-- 用于: SELECT * FROM report_car_out WHERE create_time >= ? AND deleted = 0 ORDER BY create_time DESC LIMIT ?
DROP INDEX IF EXISTS idx_report_car_out_create_time ON report_car_out;
CREATE INDEX idx_report_car_out_create_time 
ON report_car_out(create_time DESC, deleted);

-- 索引2：按车牌号和离场时间查询
-- 用于: 查询特定车辆的离场记录
DROP INDEX IF EXISTS idx_report_car_out_plate_time ON report_car_out;
CREATE INDEX idx_report_car_out_plate_time 
ON report_car_out(car_license_number, leave_time);

-- 索引3：按删除标识查询（辅助索引）
-- 用于: WHERE deleted = 0 的查询条件
DROP INDEX IF EXISTS idx_report_car_out_deleted ON report_car_out;
CREATE INDEX idx_report_car_out_deleted 
ON report_car_out(deleted);

-- 索引4：按离场时间查询
-- 用于: 按时间范围查询
DROP INDEX IF EXISTS idx_report_car_out_leave_time ON report_car_out;
CREATE INDEX idx_report_car_out_leave_time 
ON report_car_out(leave_time DESC);

-- 索引5：按进场时间查询（用于关联查询）
DROP INDEX IF EXISTS idx_report_car_out_enter_time ON report_car_out;
CREATE INDEX idx_report_car_out_enter_time 
ON report_car_out(enter_time DESC);

-- ============================================================
-- 3. 验证索引创建结果
-- ============================================================

-- 查看进场记录表的索引
SHOW INDEX FROM report_car_in;

-- 查看离场记录表的索引
SHOW INDEX FROM report_car_out;

-- ============================================================
-- 4. 测试查询性能
-- ============================================================

-- 测试1：查询当天最新50条进场记录
EXPLAIN SELECT * FROM report_car_in 
WHERE create_time >= DATE_FORMAT(NOW(), '%Y-%m-%d 00:00:00') 
  AND deleted = 0 
ORDER BY create_time DESC 
LIMIT 50;
-- 预期结果：type应该是range或index，key应该是idx_report_car_in_create_time

-- 测试2：查询当天最新50条离场记录
EXPLAIN SELECT * FROM report_car_out 
WHERE create_time >= DATE_FORMAT(NOW(), '%Y-%m-%d 00:00:00') 
  AND deleted = 0 
ORDER BY create_time DESC 
LIMIT 50;
-- 预期结果：type应该是range或index，key应该是idx_report_car_out_create_time

-- 测试3：增量查询（查询某个时间点之后的数据）
EXPLAIN SELECT * FROM report_car_in 
WHERE create_time >= '2024-10-25 10:00:00' 
  AND deleted = 0 
ORDER BY create_time DESC 
LIMIT 50;
-- 预期结果：type应该是range，key应该是idx_report_car_in_create_time

-- 测试4：按车牌号查询
EXPLAIN SELECT * FROM report_car_in 
WHERE car_license_number = '京A12345' 
  AND deleted = 0;
-- 预期结果：type应该是ref，key应该是idx_report_car_in_plate_time

-- ============================================================
-- 5. 性能对比查询（可选）
-- ============================================================

-- 统计进场记录表大小
SELECT 
  COUNT(*) AS total_records,
  SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) AS active_records,
  SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END) AS deleted_records,
  MIN(create_time) AS earliest_record,
  MAX(create_time) AS latest_record
FROM report_car_in;

-- 统计离场记录表大小
SELECT 
  COUNT(*) AS total_records,
  SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) AS active_records,
  SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END) AS deleted_records,
  MIN(create_time) AS earliest_record,
  MAX(create_time) AS latest_record
FROM report_car_out;

-- ============================================================
-- 6. 索引维护建议
-- ============================================================

-- 定期分析表，优化索引
-- ANALYZE TABLE report_car_in;
-- ANALYZE TABLE report_car_out;

-- 定期优化表（清理碎片）
-- OPTIMIZE TABLE report_car_in;
-- OPTIMIZE TABLE report_car_out;

-- 定期检查慢查询日志
-- SHOW VARIABLES LIKE 'slow_query_log%';
-- SHOW VARIABLES LIKE 'long_query_time';

-- ============================================================
-- 7. 数据清理建议（可选）
-- ============================================================

-- 清理超过3个月的旧数据（谨慎操作！建议先备份）
-- 注意：这是物理删除，不是逻辑删除
-- DELETE FROM report_car_in WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
-- DELETE FROM report_car_out WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);

-- 或者使用逻辑删除
-- UPDATE report_car_in SET deleted = 1 WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH) AND deleted = 0;
-- UPDATE report_car_out SET deleted = 1 WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH) AND deleted = 0;

-- ============================================================
-- 8. 监控查询
-- ============================================================

-- 查询今天的进出场统计
SELECT 
  '进场' AS type,
  COUNT(*) AS count,
  COUNT(DISTINCT car_license_number) AS unique_vehicles
FROM report_car_in 
WHERE DATE(create_time) = CURDATE() 
  AND deleted = 0

UNION ALL

SELECT 
  '离场' AS type,
  COUNT(*) AS count,
  COUNT(DISTINCT car_license_number) AS unique_vehicles
FROM report_car_out 
WHERE DATE(create_time) = CURDATE() 
  AND deleted = 0;

-- 查询最近1小时的进出场记录数
SELECT 
  '进场' AS type,
  COUNT(*) AS count
FROM report_car_in 
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) 
  AND deleted = 0

UNION ALL

SELECT 
  '离场' AS type,
  COUNT(*) AS count
FROM report_car_out 
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) 
  AND deleted = 0;

-- ============================================================
-- 9. 索引大小查询
-- ============================================================

-- 查看表和索引的大小
SELECT 
  TABLE_NAME AS '表名',
  ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)',
  ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
  ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
  TABLE_ROWS AS '行数'
FROM 
  INFORMATION_SCHEMA.TABLES
WHERE 
  TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('report_car_in', 'report_car_out')
ORDER BY 
  (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ============================================================
-- 执行完成
-- ============================================================

SELECT 
  '✅ 索引优化完成！' AS status,
  '请执行上面的验证查询，确认索引已正确创建。' AS next_step;

-- ============================================================
-- 注意事项
-- ============================================================
-- 1. 创建索引会锁表，建议在业务低峰期执行
-- 2. 如果表数据量很大（>100万行），索引创建可能需要较长时间
-- 3. 创建索引后，表空间会增加（索引也占用磁盘空间）
-- 4. 定期监控索引使用情况，删除不常用的索引
-- 5. 定期执行ANALYZE TABLE和OPTIMIZE TABLE维护表性能
-- ============================================================

