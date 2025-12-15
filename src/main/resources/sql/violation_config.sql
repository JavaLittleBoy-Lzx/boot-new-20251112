-- ============================================
-- 违规配置表（violation_config）
-- 用于存储违规自动拉黑等配置信息
-- ============================================

CREATE TABLE IF NOT EXISTS `violation_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `park_name` VARCHAR(100) DEFAULT NULL COMMENT '车场名称',
  `park_code` VARCHAR(100) DEFAULT NULL COMMENT '车场编码',
  `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型：NEBU_AUTO_BLACKLIST-东北林大自动拉黑',
  `max_violation_count` INT(11) DEFAULT NULL COMMENT '最大违规次数，达到此次数后自动拉黑',
  `blacklist_type` VARCHAR(50) DEFAULT NULL COMMENT '黑名单类型：黑名单/白名单/免费通行',
  `is_permanent` TINYINT(1) DEFAULT 1 COMMENT '是否永久拉黑：1-永久，0-临时',
  `blacklist_valid_days` INT(11) DEFAULT 30 COMMENT '临时拉黑有效天数（从最后一次违规时间开始计算，单位：天）',
  `blacklist_start_time` VARCHAR(30) DEFAULT NULL COMMENT '临时拉黑开始时间（格式：yyyy-MM-dd HH:mm:ss）',
  `blacklist_end_time` VARCHAR(30) DEFAULT NULL COMMENT '临时拉黑结束时间（格式：yyyy-MM-dd HH:mm:ss）',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置说明',
  `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_config` (`park_name`, `config_type`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_config_type` (`config_type`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规配置表';

-- ============================================
-- 示例数据：东北林业大学自动拉黑配置
-- ============================================
INSERT INTO `violation_config` (`park_name`, `park_code`, `config_type`, `max_violation_count`, `blacklist_type`, `is_permanent`, `blacklist_start_time`, `blacklist_end_time`, `description`, `created_by`)
VALUES ('东北林业大学', NULL, 'NEBU_AUTO_BLACKLIST', 5, '黑名单', 1, NULL, NULL, '违规5次自动拉黑，黑名单类型：黑名单，永久拉黑', 'system')
ON DUPLICATE KEY UPDATE
  `max_violation_count` = 5,
  `blacklist_type` = '黑名单',
  `is_permanent` = 1,
  `updated_at` = CURRENT_TIMESTAMP;

