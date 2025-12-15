-- ========================================
-- 系统用户表
-- 用于智慧停车场管理系统的用户认证和管理
-- 创建时间：2024-12-06
-- ========================================

-- 删除表（如果存在）
DROP TABLE IF EXISTS `sys_user`;

-- 创建系统用户表（简化版）
CREATE TABLE `sys_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：admin-管理员，user-普通用户，guest-访客',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户状态：0-禁用，1-启用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` INT(11) DEFAULT 0 COMMENT '登录次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ========================================
-- 插入初始数据
-- 密码使用BCrypt加密，原始密码都是：123456
-- ========================================

-- 管理员账号
INSERT INTO `sys_user` (`username`, `password`, `role`, `status`, `login_count`, `remark`)
VALUES 
('admin', '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe', 'admin', 1, 0, '超级管理员账号');

-- 普通用户账号
INSERT INTO `sys_user` (`username`, `password`, `role`, `status`, `login_count`, `remark`)
VALUES 
('user', '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe', 'user', 1, 0, '普通用户账号');

-- 访客账号
INSERT INTO `sys_user` (`username`, `password`, `role`, `status`, `login_count`, `remark`)
VALUES 
('guest', '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe', 'guest', 1, 0, '访客账号，只读权限');

-- ========================================
-- 查询验证
-- ========================================

-- 查看所有用户
SELECT id, username, role, status, login_count, create_time FROM sys_user WHERE is_deleted = 0;

-- 查看管理员用户
SELECT id, username, role, status FROM sys_user WHERE role = 'admin' AND is_deleted = 0;

-- ========================================
-- 密码生成说明
-- ========================================

-- 所有初始用户的密码都是：123456
-- 使用BCrypt加密后的密码：$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe

-- 如需生成新的BCrypt密码，可以使用以下Java代码：
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String encodedPassword = encoder.encode("你的密码");
-- System.out.println(encodedPassword);

-- ========================================
-- 表结构说明
-- ========================================

/*
简化版系统用户表，包含12个必要字段：

1. 主键：id（用户ID），自增BIGINT类型
2. 唯一索引：username（用户名）
3. 普通索引：role（角色）、status（状态）、create_time（创建时间）、is_deleted（删除标记）
4. 密码加密：使用BCrypt算法加密存储
5. 软删除：is_deleted字段标记删除状态，实际数据不删除
6. 角色管理：role字段区分admin/user/guest三种角色
7. 登录统计：记录最后登录时间、IP和登录次数
8. 审计字段：create_time、update_time自动维护
*/

-- ========================================
-- 使用示例
-- ========================================

-- 1. 创建新用户
-- INSERT INTO sys_user (username, password, role, status, remark)
-- VALUES ('newuser', '$2a$10$...', 'user', 1, '新用户备注');

-- 2. 更新用户备注
-- UPDATE sys_user SET remark = '更新备注', update_time = NOW() WHERE username = 'user';

-- 3. 禁用用户
-- UPDATE sys_user SET status = 0, update_time = NOW() WHERE username = 'user';

-- 4. 启用用户
-- UPDATE sys_user SET status = 1, update_time = NOW() WHERE username = 'user';

-- 5. 软删除用户
-- UPDATE sys_user SET is_deleted = 1, update_time = NOW() WHERE username = 'user';

-- 6. 重置密码
-- UPDATE sys_user SET password = '$2a$10$...', update_time = NOW() WHERE username = 'user';

-- 7. 更新登录信息
-- UPDATE sys_user SET last_login_time = NOW(), last_login_ip = '192.168.1.100', 
--        login_count = login_count + 1, update_time = NOW() WHERE id = 1;

-- 8. 查询活跃用户
-- SELECT * FROM sys_user WHERE status = 1 AND is_deleted = 0;

-- 9. 查询管理员
-- SELECT * FROM sys_user WHERE role = 'admin' AND status = 1 AND is_deleted = 0;

-- 10. 统计用户数量
-- SELECT role, COUNT(*) as count FROM sys_user WHERE is_deleted = 0 GROUP BY role;
