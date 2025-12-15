-- 停车超时提醒系统数据库脚本
-- 执行顺序：先创建表结构，再插入初始数据，最后修改现有表

-- ====================================
-- 1. 创建停车超时时间配置表
-- ====================================

DROP TABLE IF EXISTS `parking_timeout_config`;
CREATE TABLE `parking_timeout_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `yard_code` varchar(50) DEFAULT NULL COMMENT '车场编码',
  `yard_name` varchar(100) DEFAULT NULL COMMENT '车场名称',
  `vehicle_type` varchar(50) DEFAULT NULL COMMENT '车辆类型（临时、访客、业主等）',
  `timeout_minutes` int(11) DEFAULT NULL COMMENT '超时时间（分钟）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_yard_code` (`yard_code`),
  KEY `idx_vehicle_type` (`vehicle_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车超时时间配置表';

-- ====================================
-- 2. 创建消息提醒记录表
-- ====================================

DROP TABLE IF EXISTS `message_notification_log`;
CREATE TABLE `message_notification_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reservation_id` int(11) DEFAULT NULL COMMENT '预约记录ID',
  `plate_number` varchar(20) DEFAULT NULL COMMENT '车牌号码',
  `openid` varchar(100) DEFAULT NULL COMMENT '微信OpenID',
  `message_type` varchar(50) DEFAULT NULL COMMENT '消息类型（timeout_warning）',
  `message_content` text COMMENT '消息内容',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `send_status` tinyint(1) DEFAULT '0' COMMENT '发送状态：0-待发送，1-已发送，2-发送失败',
  `error_message` text COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_reservation_id` (`reservation_id`),
  KEY `idx_plate_number` (`plate_number`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息提醒记录表';

-- ====================================
-- 3. 修改现有appointment表（添加新字段）
-- ====================================

-- 为appointment表添加停车超时提醒相关字段
ALTER TABLE `appointment` 
ADD COLUMN `expected_leave_time` datetime DEFAULT NULL COMMENT '预计离场时间',
ADD COLUMN `timeout_notified` tinyint(1) DEFAULT '0' COMMENT '是否已发送超时提醒：0-未发送，1-已发送',
ADD COLUMN `yard_code` varchar(50) DEFAULT NULL COMMENT '车场编码',
ADD COLUMN `yard_name` varchar(100) DEFAULT NULL COMMENT '车场名称',
ADD COLUMN `vehicle_type` varchar(50) DEFAULT NULL COMMENT '车辆类型',
ADD COLUMN `enter_time` datetime DEFAULT NULL COMMENT '进场时间',
ADD COLUMN `leave_time` datetime DEFAULT NULL COMMENT '离场时间';

-- 为新增字段添加索引
ALTER TABLE `appointment` ADD INDEX `idx_enter_time` (`enter_time`);
ALTER TABLE `appointment` ADD INDEX `idx_leave_time` (`leave_time`);
ALTER TABLE `appointment` ADD INDEX `idx_yard_code` (`yard_code`);
ALTER TABLE `appointment` ADD INDEX `idx_timeout_notified` (`timeout_notified`);

-- ====================================
-- 4. 插入初始配置数据
-- ====================================

-- 插入不同车辆类型的超时时间配置
INSERT INTO `parking_timeout_config` (`yard_code`, `yard_name`, `vehicle_type`, `timeout_minutes`, `is_active`) VALUES
('YARD001', '金域华府停车场', '临时车辆', 120, 1),
('YARD001', '金域华府停车场', '访客车辆', 180, 1),
('YARD001', '金域华府停车场', '业主车辆', 720, 1),
('YARD002', '蓝调国际停车场', '临时车辆', 120, 1),
('YARD002', '蓝调国际停车场', '访客车辆', 180, 1),
('YARD002', '蓝调国际停车场', '业主车辆', 720, 1),
('YARD003', '万象城停车场', '临时车辆', 180, 1),
('YARD003', '万象城停车场', '访客车辆', 240, 1),
('YARD003', '万象城停车场', '业主车辆', 480, 1);

-- ====================================
-- 5. 数据修复和更新脚本
-- ====================================

-- 将现有appointment表的arrivedate字段数据同步到enter_time
UPDATE `appointment` 
SET `enter_time` = STR_TO_DATE(`arrivedate`, '%Y-%m-%d %H:%i:%s') 
WHERE `arrivedate` IS NOT NULL AND `arrivedate` != '';

-- 将现有appointment表的leavedate字段数据同步到leave_time
UPDATE `appointment` 
SET `leave_time` = STR_TO_DATE(`leavedate`, '%Y-%m-%d %H:%i:%s') 
WHERE `leavedate` IS NOT NULL AND `leavedate` != '';

-- 根据小区信息设置默认的车场编码和名称
UPDATE `appointment` 
SET `yard_code` = 'YARD001', `yard_name` = '金域华府停车场' 
WHERE `community` = '金域华府' AND `yard_code` IS NULL;

UPDATE `appointment` 
SET `yard_code` = 'YARD002', `yard_name` = '蓝调国际停车场' 
WHERE `community` = '蓝调国际' AND `yard_code` IS NULL;

UPDATE `appointment` 
SET `yard_code` = 'YARD003', `yard_name` = '万象城停车场' 
WHERE `community` = '万象城' AND `yard_code` IS NULL;

-- 根据预约类型设置车辆类型
UPDATE `appointment` 
SET `vehicle_type` = '访客车辆' 
WHERE `appointtype` = '访客预约' AND `vehicle_type` IS NULL;

UPDATE `appointment` 
SET `vehicle_type` = '业主车辆' 
WHERE `appointtype` = '业主预约' AND `vehicle_type` IS NULL;

UPDATE `appointment` 
SET `vehicle_type` = '临时车辆' 
WHERE `appointtype` = '临时预约' AND `vehicle_type` IS NULL;

-- 为没有设置车辆类型的记录设置默认值
UPDATE `appointment` 
SET `vehicle_type` = '临时车辆' 
WHERE `vehicle_type` IS NULL OR `vehicle_type` = '';

-- ====================================
-- 6. 数据完整性检查脚本
-- ====================================

-- 检查appointment表中需要超时提醒但缺少必要字段的记录
SELECT 
    id,
    platenumber,
    openid,
    enter_time,
    leave_time,
    yard_code,
    yard_name,
    vehicle_type,
    timeout_notified,
    '缺少OpenID' as issue
FROM `appointment` 
WHERE `enter_time` IS NOT NULL 
  AND `leave_time` IS NULL 
  AND (`openid` IS NULL OR `openid` = '')
LIMIT 10;

-- 检查超时配置表
SELECT 
    yard_code,
    yard_name,
    vehicle_type,
    timeout_minutes,
    is_active
FROM `parking_timeout_config` 
WHERE `is_active` = 1
ORDER BY `yard_code`, `vehicle_type`;

-- 检查消息日志表
SELECT 
    DATE(send_time) as send_date,
    message_type,
    send_status,
    COUNT(*) as message_count
FROM `message_notification_log` 
WHERE `send_time` >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(send_time), message_type, send_status
ORDER BY send_date DESC;

-- ====================================
-- 7. 性能优化建议
-- ====================================

-- 添加复合索引以提高查询性能
ALTER TABLE `appointment` 
ADD INDEX `idx_timeout_check` (`enter_time`, `leave_time`, `timeout_notified`, `openid`);

-- 定期清理过期的消息日志（保留最近3个月的数据）
-- 注意：这是一个示例脚本，实际使用时请根据业务需要调整
/*
DELETE FROM `message_notification_log` 
WHERE `create_time` < DATE_SUB(NOW(), INTERVAL 3 MONTH);
*/

-- ====================================
-- 8. 应用配置参考
-- ====================================

-- 在application.yml中添加以下配置：
/*
wechat:
  appid: wx_your_appid
  secret: your_app_secret
  timeout:
    template:
      id: 45414  # 您的微信模板消息ID

spring:
  task:
    scheduling:
      pool:
        size: 2
  # 启用定时任务
  scheduling:
    enabled: true
*/

-- ====================================
-- 9. 测试数据插入（可选）
-- ====================================

-- 插入测试预约数据（用于测试超时提醒功能）
/*
INSERT INTO `appointment` (
    `platenumber`, 
    `openid`, 
    `enter_time`, 
    `yard_code`, 
    `yard_name`, 
    `vehicle_type`, 
    `status`, 
    `timeout_notified`,
    `visitdate`,
    `recorddate`
) VALUES (
    '粤B12345', 
    'test_openid_123', 
    NOW() - INTERVAL 1 HOUR, 
    'YARD001', 
    '金域华府停车场', 
    '临时车辆', 
    '1', 
    0,
    CONCAT(DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), ' - ', DATE_FORMAT(NOW() + INTERVAL 2 HOUR, '%Y-%m-%d %H:%i:%s')),
    NOW()
);
*/

-- ====================================
-- 10. 常用查询脚本
-- ====================================

-- 查询当前在场且即将超时的车辆
SELECT 
    a.id,
    a.platenumber,
    a.yard_name,
    a.enter_time,
    a.vehicle_type,
    a.timeout_notified,
    p.timeout_minutes,
    TIMESTAMPDIFF(MINUTE, a.enter_time, NOW()) as parked_minutes,
    (p.timeout_minutes - TIMESTAMPDIFF(MINUTE, a.enter_time, NOW())) as remaining_minutes
FROM `appointment` a
LEFT JOIN `parking_timeout_config` p ON a.yard_code = p.yard_code AND a.vehicle_type = p.vehicle_type
WHERE a.enter_time IS NOT NULL 
  AND a.leave_time IS NULL 
  AND a.timeout_notified = 0
  AND a.openid IS NOT NULL
  AND p.is_active = 1
  AND (p.timeout_minutes - TIMESTAMPDIFF(MINUTE, a.enter_time, NOW())) BETWEEN 0 AND 15
ORDER BY remaining_minutes ASC;

-- 查询今日消息发送统计
SELECT 
    send_status,
    CASE 
        WHEN send_status = 0 THEN '待发送'
        WHEN send_status = 1 THEN '已发送'
        WHEN send_status = 2 THEN '发送失败'
    END as status_name,
    COUNT(*) as count
FROM `message_notification_log` 
WHERE DATE(create_time) = CURDATE()
GROUP BY send_status;

-- 查询超时提醒效果统计
SELECT 
    yard_name,
    vehicle_type,
    COUNT(*) as total_notifications,
    SUM(CASE WHEN send_status = 1 THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN send_status = 2 THEN 1 ELSE 0 END) as failed_count,
    ROUND(SUM(CASE WHEN send_status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM `message_notification_log` m
WHERE DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY yard_name, vehicle_type
ORDER BY yard_name, vehicle_type;

-- ====================================
-- 执行完成提示
-- ====================================

SELECT 
    '停车超时提醒系统数据库初始化完成！' as message,
    NOW() as completion_time; 