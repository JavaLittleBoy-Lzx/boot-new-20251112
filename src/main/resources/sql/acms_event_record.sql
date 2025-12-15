-- ACMS数据推送事件记录表
-- 用于存储ACMS系统推送的进出事件数据
-- 包括：人证比对(197162)、人脸识别(196893)、刷校园卡(198914)三类事件

CREATE TABLE IF NOT EXISTS `acms_event_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` varchar(100) DEFAULT NULL COMMENT '事件ID（外部系统事件唯一标识）',
  `event_type` int(11) NOT NULL COMMENT '事件类型：197162-人证比对，196893-人脸识别，198914-刷校园卡',
  `recognition_type` varchar(50) DEFAULT NULL COMMENT '识别类型：人证比对/人脸识别/刷校园卡',
  
  -- 人员基本信息
  `person_id` varchar(100) DEFAULT NULL COMMENT '人员ID（ExtEventPersonNoj）',
  `person_name` varchar(100) DEFAULT NULL COMMENT '姓名',
  `job_no` varchar(50) DEFAULT NULL COMMENT '工号/学号',
  `phone_no` varchar(20) DEFAULT NULL COMMENT '手机号',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别：1-男，2-女，0-未知',
  `id_card` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `organization` varchar(200) DEFAULT NULL COMMENT '所属单位',
  
  -- 进出信息
  `channel_name` varchar(200) DEFAULT NULL COMMENT '进出通道名称',
  `direction` varchar(20) DEFAULT NULL COMMENT '进出方向：进/出',
  
  -- 访客信息
  `plate_number` varchar(20) DEFAULT NULL COMMENT '车牌号码（只有访客会有）',
  `vip_type_name` varchar(100) DEFAULT NULL COMMENT 'VIP类型名称（从访客预约记录中获取）',
  `is_unreserved_visitor` tinyint(1) DEFAULT 0 COMMENT '是否未预约纯访客：0-否（已预约或有预约记录），1-是（未预约的纯访客）。仅针对刷身份证进出（event_type=197162）的记录',
  `reservation_time_range` varchar(100) DEFAULT NULL COMMENT '预约时间段（格式：开始时间-结束时间），例如：2025-01-15 10:00:00-2025-01-15 12:00:00。仅针对已预约的访客',
  
  -- 照片信息
  `photo_url` varchar(500) DEFAULT NULL COMMENT '照片URL（人脸照片或身份证照片）',
  
  -- 事件时间
  `event_time` datetime DEFAULT NULL COMMENT '事件发生时间',
  
  -- 原始数据（用于调试和追溯）
  `raw_data` text COMMENT '原始推送数据（JSON格式）',
  
  -- 系统字段
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_person_id` (`person_id`),
  KEY `idx_person_name` (`person_name`),
  KEY `idx_job_no` (`job_no`),
  KEY `idx_phone_no` (`phone_no`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_event_time` (`event_time`),
  KEY `idx_channel_name` (`channel_name`),
  KEY `idx_direction` (`direction`),
  KEY `idx_plate_number` (`plate_number`),
  KEY `idx_vip_type_name` (`vip_type_name`),
  KEY `idx_is_unreserved_visitor` (`is_unreserved_visitor`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ACMS数据推送事件记录表';

-- 添加表注释
ALTER TABLE `acms_event_record` COMMENT = 'ACMS数据推送事件记录表，存储人证比对、人脸识别、刷校园卡等进出事件数据';

