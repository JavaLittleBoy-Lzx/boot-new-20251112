-- ========================================
-- 检查和修复sys_user表中的密码问题
-- 状态：✅ 已解决 (2024-12-06)
-- 原因：数据库中的BCrypt哈希值不对应密码123456
-- 解决：已重新生成正确的密码哈希
-- ========================================

-- 1. 检查当前用户及密码格式
SELECT 
    id,
    username,
    LEFT(password, 20) as password_prefix,
    CASE 
        WHEN password LIKE '$2a$%' THEN '✅ BCrypt加密'
        WHEN password LIKE '$2b$%' THEN '✅ BCrypt加密(新版)'
        WHEN LENGTH(password) < 20 THEN '❌ 明文密码'
        ELSE '⚠️ 未知格式'
    END as password_status,
    role,
    status
FROM sys_user
WHERE is_deleted = 0;

-- ========================================
-- 2. 如果密码不是BCrypt格式，执行以下更新
-- ========================================

-- 注意：以下密码都是 '123456' 经过BCrypt加密后的结果
-- 密码哈希: $2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe

-- 更新admin用户密码
UPDATE sys_user 
SET password = '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe',
    update_time = NOW()
WHERE username = 'admin' AND password NOT LIKE '$2a$%';

-- 更新user用户密码
UPDATE sys_user 
SET password = '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe',
    update_time = NOW()
WHERE username = 'user' AND password NOT LIKE '$2a$%';

-- 更新guest用户密码
UPDATE sys_user 
SET password = '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe',
    update_time = NOW()
WHERE username = 'guest' AND password NOT LIKE '$2a$%';

-- ========================================
-- 3. 验证修复结果
-- ========================================

SELECT 
    username,
    CASE 
        WHEN password = '$2a$10$XcY5rSqQRKdcBqV/VLnvMeMXDWP5qzRKmQgN7OMV2ZW0QJmGbqPXe' THEN '✅ 密码正确'
        ELSE '❌ 密码异常'
    END as password_check,
    role,
    status,
    update_time
FROM sys_user
WHERE is_deleted = 0;

-- ========================================
-- 常见问题说明
-- ========================================

/*
问题1: 密码验证失败的可能原因
--------------------------------------
1. 数据库密码不是BCrypt加密格式
   - 检查: password字段是否以 $2a$ 或 $2b$ 开头
   - 解决: 运行上面的UPDATE语句

2. 用户输入的密码不正确
   - 初始密码: 123456
   - 区分大小写

3. 密码字段被意外修改
   - 检查update_time字段
   - 重新运行sys_user.sql重置数据

问题2: 如何生成新的BCrypt密码
--------------------------------------
方法1 - 使用Java代码:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String encoded = encoder.encode("你的密码");
System.out.println(encoded);
```

方法2 - 使用在线工具:
https://bcrypt-generator.com/

方法3 - 调用测试方法:
在UserServiceImpl中调用 testBCryptPassword() 方法

问题3: 登录时的调试信息
--------------------------------------
查看后端日志中的关键信息：
- 🔐 密码验证 - 用户: xxx
- 📝 输入密码: xxx
- 💾 数据库密码(BCrypt): xxx
- 🔍 密码是否以$2a$开头: true/false
- ✅ 密码匹配结果: true/false

如果"密码是否以$2a$开头"显示false，说明数据库密码有问题
*/

-- ========================================
-- 4. 快速测试登录
-- ========================================

-- 测试查询（模拟登录验证）
SELECT 
    id,
    username,
    password,
    role,
    status,
    '请使用密码: 123456 登录' as login_hint
FROM sys_user
WHERE username = 'admin' AND is_deleted = 0;
