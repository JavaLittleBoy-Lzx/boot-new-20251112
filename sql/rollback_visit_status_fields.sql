-- =====================================================
-- 回滚脚本：删除 visitor_reservation_sync 表中的新增字段
-- 警告：执行此脚本将删除所有进出场状态和时间数据，请谨慎操作！
-- =====================================================

-- 删除4个新添加的字段
ALTER TABLE visitor_reservation_sync 
DROP COLUMN person_visit_status,
DROP COLUMN person_visit_times,
DROP COLUMN car_visit_status,
DROP COLUMN car_visit_times;

-- 注意：如果MySQL版本支持 IF EXISTS，可以使用以下语法：
-- ALTER TABLE visitor_reservation_sync 
-- DROP COLUMN IF EXISTS person_visit_status,
-- DROP COLUMN IF EXISTS person_visit_times,
-- DROP COLUMN IF EXISTS car_visit_status,
-- DROP COLUMN IF EXISTS car_visit_times;

