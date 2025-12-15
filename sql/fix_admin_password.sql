-- ========================================
-- 修复admin用户密码问题
-- 创建时间：2024-12-06
-- ========================================

-- 问题描述：
-- 数据库中的BCrypt密码哈希与123456不匹配
-- 需要重新生成正确的密码哈希

-- ========================================
-- 方案1：使用新生成的BCrypt密码（推荐）
-- ========================================

-- 步骤1：先查看当前密码
SELECT 
    id,
    username,
    password,
    role,
    status
FROM sys_user
WHERE username = 'admin';

-- 步骤2：更新为新的BCrypt密码
-- 注意：执行前请先访问 http://localhost:8675/api/test/generate-password?password=123456
-- 获取新的密码哈希，然后替换下面的密码

-- 临时方案：使用重新生成的密码（密码：123456）
UPDATE sys_user 
SET password = '$2a$10$N.zmdr9k7uObo8XbreuN4OFWuiUZbFvbK.zsKIGCxRBqjjTyOb.K2',
    update_time = NOW()
WHERE username = 'admin';

-- 步骤3：验证更新
SELECT 
    username,
    LEFT(password, 30) as password_preview,
    update_time
FROM sys_user
WHERE username = 'admin';

-- ========================================
-- 方案2：如果上述密码仍然不工作
-- ========================================

-- 访问测试接口找出正确的密码：
-- http://localhost:8675/api/test/test-db-password

-- 这个接口会测试常见密码，找出数据库中密码对应的原始密码

-- ========================================
-- 验证步骤
-- ========================================

/*
1. 执行上面的UPDATE语句
2. 重启后端服务（可选）
3. 使用以下凭据登录：
   - 用户名: admin
   - 密码: 123456
4. 如果仍然失败，访问测试接口获取新密码：
   http://localhost:8675/api/test/generate-password?password=123456
*/

-- ========================================
-- 注意事项
-- ========================================

/*
BCrypt密码特性：
1. 每次生成的哈希值都不同（包含随机盐）
2. 但都能验证同一个原始密码
3. 格式：$2a$[cost]$[salt][hash]
4. 示例：$2a$10$XcY5rSqQRKdcBqV/VLnvMe... 

如果密码验证仍然失败：
1. 检查是否有特殊字符编码问题
2. 确认BCryptPasswordEncoder版本一致
3. 使用测试接口生成全新的密码哈希
*/
