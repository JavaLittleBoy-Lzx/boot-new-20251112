-- =====================================================
-- 预约记录与进出场数据整合 - 数据库字段扩展（简化版）
-- 功能：在 visitor_reservation_sync 表中添加人员进出场和车辆进出场相关字段
-- 说明：此版本适用于所有MySQL版本，执行前请确保字段不存在
-- =====================================================

-- 添加4个新字段
ALTER TABLE visitor_reservation_sync 
ADD COLUMN person_visit_status VARCHAR(50) 
    COMMENT '人员来访状态：人未来访/人已进场/人已离场/来访中' 
    DEFAULT NULL
    AFTER deleted,
ADD COLUMN person_visit_times TEXT 
    COMMENT '人员进出场时间记录，JSON格式，格式：[{"enterTime":"2025-01-15 10:30:00","leaveTime":"2025-01-15 12:00:00"},...]' 
    DEFAULT NULL
    AFTER person_visit_status,
ADD COLUMN car_visit_status VARCHAR(50) 
    COMMENT '车辆来访状态：车未来访/已进场/已离场' 
    DEFAULT NULL
    AFTER person_visit_times,
ADD COLUMN car_visit_times TEXT 
    COMMENT '车辆进出场时间记录，JSON格式，格式：[{"enterTime":"2025-01-15 10:25:00","leaveTime":"2025-01-15 12:05:00"},...]' 
    DEFAULT NULL
    AFTER car_visit_status;

