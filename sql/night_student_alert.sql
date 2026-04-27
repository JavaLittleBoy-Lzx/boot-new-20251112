-- ============================================
-- 夜间学生出校提醒功能数据库表
-- 创建时间: 2025-04-22
-- ============================================

-- ============================================
-- 1. 夜间提醒配置表
-- ============================================
CREATE TABLE IF NOT EXISTS `night_student_alert_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `night_start_time` varchar(10) NOT NULL DEFAULT '22:00' COMMENT '夜间开始时间（如22:00）',
  `night_end_time` varchar(10) NOT NULL DEFAULT '06:00' COMMENT '夜间结束时间（如06:00），支持跨天',
  `alert_channels` varchar(500) DEFAULT NULL COMMENT '需要提醒的出口通道（逗号分隔，为空则全部通道）',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='夜间学生出校提醒配置表';

-- ============================================
-- 2. 夜间学生出校提醒记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `night_student_alert_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `person_name` varchar(100) NOT NULL COMMENT '学生姓名',
  `id_card` varchar(50) DEFAULT NULL COMMENT '身份证号（脱敏存储）',
  `job_no` varchar(50) DEFAULT NULL COMMENT '学号',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别：男/女',
  `college` varchar(200) DEFAULT NULL COMMENT '学院/部门',
  `channel_name` varchar(200) NOT NULL COMMENT '出校通道名称',
  `event_time` datetime NOT NULL COMMENT '出校时间',
  `photo_url` varchar(500) DEFAULT NULL COMMENT '照片URL',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_time` (`event_time`),
  KEY `idx_channel_name` (`channel_name`),
  KEY `idx_gender` (`gender`),
  KEY `idx_college` (`college`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='夜间学生出校提醒记录表';

-- ============================================
-- 3. 插入默认配置
-- ============================================
INSERT INTO `night_student_alert_config` (`id`, `enabled`, `night_start_time`, `night_end_time`, `alert_channels`)
VALUES (1, 1, '22:00', '06:00', NULL)
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `night_start_time` = VALUES(`night_start_time`),
  `night_end_time` = VALUES(`night_end_time`),
  `alert_channels` = VALUES(`alert_channels`);
