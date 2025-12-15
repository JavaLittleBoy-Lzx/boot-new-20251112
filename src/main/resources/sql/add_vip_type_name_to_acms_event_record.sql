-- 为acms_event_record表添加vip_type_name字段
-- 如果表已存在，使用此SQL添加字段

ALTER TABLE `acms_event_record` 
ADD COLUMN `vip_type_name` varchar(100) DEFAULT NULL COMMENT 'VIP类型名称（从访客预约记录中获取）' 
AFTER `plate_number`;

-- 添加索引（可选，如果需要根据VIP类型查询）
-- ALTER TABLE `acms_event_record` ADD INDEX `idx_vip_type_name` (`vip_type_name`);

