-- 二维码使用记录表
-- 用于记录管家生成的二维码使用情况，确保每个二维码只能使用一次

CREATE TABLE IF NOT EXISTS `qr_code_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `qr_id` VARCHAR(64) NOT NULL COMMENT '二维码唯一标识',
    `butler_phone` VARCHAR(20) NOT NULL COMMENT '管家手机号',
    `butler_name` VARCHAR(50) COMMENT '管家姓名',
    `community` VARCHAR(100) COMMENT '小区名称',
    `building` VARCHAR(50) COMMENT '楼栋',
    `unit` VARCHAR(50) COMMENT '单元',
    `floor` VARCHAR(50) COMMENT '楼层',
    `room` VARCHAR(50) COMMENT '房间号',
    `created_time` DATETIME NOT NULL COMMENT '创建时间',
    `used_time` DATETIME COMMENT '使用时间',
    `is_used` TINYINT DEFAULT 0 COMMENT '是否已使用 0-未使用 1-已使用',
    `visitor_openid` VARCHAR(100) COMMENT '访客openid',
    `visitor_phone` VARCHAR(20) COMMENT '访客手机号',
    `qr_type` VARCHAR(20) DEFAULT 'visitor_invitation' COMMENT '二维码类型',
    `expire_time` DATETIME COMMENT '有效期至',
    `remark` VARCHAR(255) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_qr_id` (`qr_id`),
    KEY `idx_butler_phone` (`butler_phone`),
    KEY `idx_created_time` (`created_time`),
    KEY `idx_is_used` (`is_used`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二维码使用记录表';

-- 插入测试数据（可选）
-- INSERT INTO `qr_code_usage` (
--     `qr_id`, `butler_phone`, `butler_name`, `community`, `building`, 
--     `created_time`, `is_used`, `qr_type`, `expire_time`
-- ) VALUES (
--     'QR_TEST_001', '13800138000', '测试管家', '四季上东', '1号楼',
--     NOW(), 0, 'visitor_invitation', DATE_ADD(NOW(), INTERVAL 24 HOUR)
-- );

-- 创建定时清理过期二维码的存储过程（可选）
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS `CleanExpiredQrCodes`()
BEGIN
    DECLARE cleaned_count INT DEFAULT 0;
    
    -- 标记过期的二维码为已使用
    UPDATE `qr_code_usage` 
    SET `is_used` = 1, `remark` = '已过期' 
    WHERE `expire_time` < NOW() AND `is_used` = 0;
    
    -- 获取清理的记录数
    SET cleaned_count = ROW_COUNT();
    
    -- 记录清理日志
    INSERT INTO `qr_code_usage` (
        `qr_id`, `butler_phone`, `butler_name`, `community`, 
        `created_time`, `is_used`, `qr_type`, `remark`
    ) VALUES (
        CONCAT('CLEAN_LOG_', UNIX_TIMESTAMP()), 'SYSTEM', '系统清理', '系统操作',
        NOW(), 1, 'system_log', CONCAT('清理过期二维码数量: ', cleaned_count)
    );
    
    SELECT cleaned_count AS cleaned_records;
END$$

DELIMITER ;

-- 创建定时事件来自动清理过期二维码（每天凌晨2点执行）
-- 注意：需要确保MySQL的事件调度器已启用 (SET GLOBAL event_scheduler = ON;)
/*
CREATE EVENT IF NOT EXISTS `auto_clean_expired_qr_codes`
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '02:00:00')
DO
  CALL CleanExpiredQrCodes();
*/
