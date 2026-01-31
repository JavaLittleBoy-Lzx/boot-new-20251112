-- ============================================
-- 关注追踪系统数据库表
-- 创建时间: 2025-12-16
-- 说明: 用于实现关注对象的智能追踪和提醒功能
-- ============================================

-- ============================================
-- 1. 关注列表表
-- ============================================
CREATE TABLE IF NOT EXISTS `focus_watch_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `watch_type` enum('idcard','plate') NOT NULL COMMENT '关注类型：idcard-身份证号，plate-车牌号',
  `watch_value` varchar(50) NOT NULL COMMENT '关注值：身份证号或车牌号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注信息',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type_value` (`user_id`, `watch_type`, `watch_value`) COMMENT '防止同一用户重复添加相同对象',
  KEY `idx_watch_value` (`watch_value`) COMMENT '快速匹配进出场数据',
  KEY `idx_user_id` (`user_id`) COMMENT '快速查询某个用户的关注列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注监控列表表';

-- ============================================
-- 2. 提醒记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `focus_alert_records` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `watch_id` int(11) NOT NULL COMMENT '关注对象ID（关联focus_watch_list.id）',
  `alert_type` enum('person','vehicle') NOT NULL COMMENT '提醒类型：person-人员，vehicle-车辆',
  `watch_value` varchar(50) NOT NULL COMMENT '身份证号或车牌号',
  `event_type` enum('entry','exit') NOT NULL COMMENT '事件类型：entry-进场，exit-出场',
  `event_time` datetime NOT NULL COMMENT '事件发生时间',
  `channel_name` varchar(200) DEFAULT NULL COMMENT '通道名称',
  
  -- 人员相关字段
  `person_name` varchar(100) DEFAULT NULL COMMENT '人员姓名（人员类型时）',
  `department` varchar(200) DEFAULT NULL COMMENT '部门/学院（人员类型时）',
  `phone_no` varchar(50) DEFAULT NULL COMMENT '手机号（人员类型时）',
  `photo_url` varchar(500) DEFAULT NULL COMMENT '照片URL（人员为人脸照片，车辆为车辆照片）',
  
  -- 车辆出场额外字段
  `enter_channel_name` varchar(200) DEFAULT NULL COMMENT '进场通道（车辆出场时）',
  `stopping_time` varchar(100) DEFAULT NULL COMMENT '停车时长（车辆出场时）',
  
  -- 车辆预约信息
  `reservation_person` varchar(100) DEFAULT NULL COMMENT '预约人（车辆预约信息）',
  `reservation_phone` varchar(50) DEFAULT NULL COMMENT '预约联系电话（车辆预约信息）',
  `reservation_reason` varchar(200) DEFAULT NULL COMMENT '预约事由（车辆预约信息）',
  `reservation_time_range` varchar(100) DEFAULT NULL COMMENT '预约时段（车辆预约信息）',
  
  -- 人员预约信息
  `visitor_pass_name` varchar(255) DEFAULT NULL COMMENT '通行证名称（人员预约信息）',
  `visitor_vip_type` varchar(100) DEFAULT NULL COMMENT 'VIP类型（人员预约信息）',
  `visitor_park_name` varchar(200) DEFAULT NULL COMMENT '园区名称（人员预约信息）',
  `visitor_reservation_time_range` varchar(100) DEFAULT NULL COMMENT '访客预约时段（人员预约信息）',
  
  -- 确认状态
  `is_confirmed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已确认：0-未确认，1-已确认',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  
  `remark` varchar(255) DEFAULT NULL COMMENT '关注备注（冗余字段，方便查询）',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_user_confirmed` (`user_id`, `is_confirmed`) COMMENT '快速查询某用户的未确认/已确认记录',
  KEY `idx_watch_id` (`watch_id`) COMMENT '关联关注对象',
  KEY `idx_event_time` (`event_time`) COMMENT '按时间排序',
  KEY `idx_alert_type` (`alert_type`) COMMENT '区分人员/车辆类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注提醒记录表';

-- ============================================
-- 示例数据（可选，用于测试）
-- ============================================

-- 插入示例关注对象
-- INSERT INTO `focus_watch_list` (`user_id`, `watch_type`, `watch_value`, `remark`) VALUES
-- (1, 'idcard', '230102199001011234', '重点关注人员'),
-- (1, 'plate', '黑A12345', '领导用车');

-- ============================================
-- 使用说明
-- ============================================
/*
业务流程：
1. 用户通过前端界面添加关注对象 → 保存到 focus_watch_list 表
2. 系统检测到进出场事件 → 查询 focus_watch_list 表匹配
3. 匹配成功 → 推送WebSocket提醒 + 保存到 focus_alert_records 表（is_confirmed=0）
4. 用户查看未确认提醒 → 点击确认 → 更新 is_confirmed=1
5. 已确认记录 → 显示在历史记录Tab中

索引说明：
- uk_user_type_value: 防止同一用户重复添加相同的身份证号或车牌号
- idx_watch_value: 快速匹配进出场数据
- idx_user_confirmed: 快速查询未确认/已确认记录
- idx_event_time: 按时间排序查询
*/
