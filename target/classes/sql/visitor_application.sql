-- 访客申请表
CREATE TABLE IF NOT EXISTS `visitor_application` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_no` varchar(50) NOT NULL COMMENT '申请编号',
  `nickname` varchar(50) NOT NULL COMMENT '访客姓名',
  `phone` varchar(20) NOT NULL COMMENT '访客手机号',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '业主姓名',
  `owner_phone` varchar(20) DEFAULT NULL COMMENT '业主手机号',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `id_card` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `reason` text NOT NULL COMMENT '申请原因',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `community` varchar(100) DEFAULT NULL COMMENT '小区',
  `building` varchar(20) DEFAULT NULL COMMENT '栋',
  `units` int(11) DEFAULT NULL COMMENT '单元',
  `floor` int(11) DEFAULT NULL COMMENT '楼层',
  `roomnumber` int(11) DEFAULT NULL COMMENT '房间号',
  `full_address` varchar(500) NOT NULL COMMENT '完整地址',
  `userkind` varchar(20) DEFAULT '访客' COMMENT '用户类型',
  `auditstatus` varchar(20) DEFAULT '待审批' COMMENT '审批状态：待审批、已通过、未通过',
  `applydate` datetime NOT NULL COMMENT '申请时间',
  `auditusername` varchar(50) DEFAULT NULL COMMENT '审批人',
  `auditdate` datetime DEFAULT NULL COMMENT '审批时间',
  `refusereason` text DEFAULT NULL COMMENT '审批意见/拒绝原因',
  `openid` varchar(100) DEFAULT NULL COMMENT '微信OpenID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_phone` (`phone`),
  KEY `idx_owner_phone` (`owner_phone`),
  KEY `idx_auditstatus` (`auditstatus`),
  KEY `idx_applydate` (`applydate`),
  KEY `idx_community` (`community`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客申请表';

-- 插入测试数据（可选）
INSERT INTO `visitor_application` (
  `application_no`, `nickname`, `phone`, `owner_name`, `owner_phone`, `gender`, `reason`, 
  `province`, `city`, `district`, `community`, `building`, 
  `units`, `floor`, `roomnumber`, `full_address`, `userkind`, 
  `auditstatus`, `applydate`
) VALUES 
(
  'VA20250625140001', '张三', '13800138001', '李业主', '13900139001', '男', '快递送货', 
  '北京市', '北京市', '朝阳区', '四季上东', '1', 
  1, 10, 1001, '北京市北京市朝阳区四季上东1栋1单元10楼1001室', '访客', 
  '待审批', NOW()
),
(
  'VA20250625140002', '李四', '13800138002', '王业主', '13900139002', '女', '维修服务', 
  '北京市', '北京市', '朝阳区', '万象上东', '2', 
  2, 5, 502, '北京市北京市朝阳区万象上东2栋2单元5楼502室', '访客', 
  '已通过', NOW()
); 