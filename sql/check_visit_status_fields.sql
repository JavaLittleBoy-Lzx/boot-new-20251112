-- =====================================================
-- 检查 visitor_reservation_sync 表中的字段是否存在
-- =====================================================

-- 检查新添加的4个字段是否存在
SELECT 
    COLUMN_NAME AS '字段名',
    DATA_TYPE AS '数据类型',
    CHARACTER_MAXIMUM_LENGTH AS '最大长度',
    COLUMN_COMMENT AS '字段说明',
    IS_NULLABLE AS '是否可空',
    COLUMN_DEFAULT AS '默认值',
    ORDINAL_POSITION AS '字段位置'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'visitor_reservation_sync'
  AND COLUMN_NAME IN ('person_visit_status', 'person_visit_times', 'car_visit_status', 'car_visit_times')
ORDER BY ORDINAL_POSITION;

-- 如果查询结果为空，说明字段不存在，需要执行 add_visit_status_fields_simple.sql
-- 如果查询结果有4条记录，说明字段已存在

