-- ============================================
-- 添加 blacklist_valid_days 字段到 violation_config 表
-- 用于存储临时拉黑的有效天数（从最后一次违规时间开始计算）
-- ============================================

-- 添加字段
ALTER TABLE `violation_config`
ADD COLUMN `blacklist_valid_days` INT(11) DEFAULT 30 COMMENT '临时拉黑有效天数（从最后一次违规时间开始计算，单位：天）'
AFTER `is_permanent`;

-- 说明：
-- 1. 该字段仅在 is_permanent=0（临时拉黑）时生效
-- 2. 默认值为30天
-- 3. 触发自动拉黑时，系统会根据最后一次违规时间 + blacklist_valid_days 计算拉黑结束时间
-- 4. blacklist_start_time 和 blacklist_end_time 字段会在触发拉黑时自动计算并保存

