-- 临时车预进场数据表
-- 用于存储enterVipType=1且enterChannelCode=520243的临时车预进场数据

CREATE TABLE IF NOT EXISTS `temp_car_pre_entry` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号',
  `park_code` varchar(50) NOT NULL COMMENT '车场编码',
  `park_name` varchar(100) DEFAULT NULL COMMENT '车场名称',
  `enter_channel_code` varchar(50) DEFAULT NULL COMMENT '进场通道编码',
  `enter_channel_id` int(11) DEFAULT NULL COMMENT '进场通道ID',
  `enter_vip_type` int(11) DEFAULT NULL COMMENT '进场VIP类型',
  `pre_enter_time` varchar(50) NOT NULL COMMENT '预进场时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `used` int(1) NOT NULL DEFAULT '0' COMMENT '是否已使用 (0-未使用, 1-已使用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_plate_park` (`plate_number`, `park_code`),
  KEY `idx_used` (`used`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='临时车预进场数据记录表';

-- 创建索引以提高查询性能
-- 主要查询场景：根据车牌号和车场编码查询未使用的预进场记录
CREATE INDEX idx_plate_park_used ON temp_car_pre_entry(plate_number, park_code, used);
CREATE INDEX idx_pre_enter_time ON temp_car_pre_entry(pre_enter_time); 